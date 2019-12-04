package ch.unibas.dmi.dbis.vrem.importer;

import static ch.unibas.dmi.dbis.vrem.model.exhibition.cuboid.Direction.EAST;
import static ch.unibas.dmi.dbis.vrem.model.exhibition.cuboid.Direction.NORTH;
import static ch.unibas.dmi.dbis.vrem.model.exhibition.cuboid.Direction.SOUTH;
import static ch.unibas.dmi.dbis.vrem.model.exhibition.cuboid.Direction.WEST;
import static java.nio.charset.StandardCharsets.UTF_8;

import ch.unibas.dmi.dbis.vrem.config.Config;
import ch.unibas.dmi.dbis.vrem.database.codec.VREMCodecProvider;
import ch.unibas.dmi.dbis.vrem.database.dao.VREMReader;
import ch.unibas.dmi.dbis.vrem.database.dao.VREMWriter;
import ch.unibas.dmi.dbis.vrem.model.Vector3f;
import ch.unibas.dmi.dbis.vrem.model.exhibition.cuboid.Direction;
import ch.unibas.dmi.dbis.vrem.model.exhibition.Exhibit;
import ch.unibas.dmi.dbis.vrem.model.exhibition.Exhibition;
import ch.unibas.dmi.dbis.vrem.model.exhibition.polygonal.Room;
import ch.unibas.dmi.dbis.vrem.model.exhibition.Texture;
import ch.unibas.dmi.dbis.vrem.model.exhibition.polygonal.Wall;
import ch.unibas.dmi.dbis.vrem.model.objects.CulturalHeritageObject.CHOType;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.restrictions.Required;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.FileUtils;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import spark.utils.StringUtils;

/**
 * Imports an exhibition which is represented as folders. The exhibition's root folder is passed via the --path (-p) option. Each folder there is considered a room. They are sorted the way they are present. Each room may contain a {@code room-config.json} file, with settings of the room itself. Furthermore such a room folder contains image files, which are its exhibits.
 * <p>
 * There are options for either specifying a configuration file for the database connection or a path for the output as json.
 *
 * @author loris.sauter
 */
@Command(name = "import-folder", description = "Imports a folder-based exhibition")
public class ExhibitionImporter implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(ExhibitionImporter.class);

    public static final String NORTH_WALL_NAME = "north";
    public static final String EAST_WALL_NAME = "east";
    public static final String SOUTH_WALL_NAME = "south";
    public static final String WEST_WALL_NAME = "west";
    public static final String ROOM_CONFIG_FILE = "room-config.json";
    public static final String WALL_CONFIG_FILE = "wall-config.json";
    public static final String PNG_EXTENSION = "png";
    public static final String JPG_EXTENSION = "jpg";
    public static final String JSON_EXTENSION = "json";
    //public static final Vector3f ROOM_SIZE = new Vector3f(10, 5, 10);
    public static final Vector3f ENTRYPOINT = Vector3f.ORIGIN;

    public static final float ROOM_BORDER = 0.5f;
    public static final float EXHIBIT_PADDING = 1f;
    public static final float EXHIBIT_DEFAULT_HEIGHT = 1.5f;

    @Required
    @Option(title = "Exhibition-Path", name = {"--path", "-p"}, description = "Path to the exhibition root folder")
    private String exhibitionPath;

    @Option(title = "Configuration", name = {"--config", "-c"}, description = "Path to configuration file")
    @Required
    private String config;

    @Option(title = "Remove Old Exhibition", name = {"--clean"}, description = "Remove old exhibitions with the same key")
    private boolean clean = false;


    @Option(title = "Exhibition-Description", name = {"--description"}, description = "Description of the exhibition to be imported")
    private String exhibitionDescription = "";

    @Option(title = "Exhibition-Name", name = {"--name"}, description = "Name of the exhibition to be imported")
    private String name = "default-name";

    private Gson gson;
    private Exhibition reference = null;
    private Comparator<Exhibit> pathExhibitComparator = Comparator.comparing(e -> e.path);


    @Override
    public void run() {
        try {
            gson = new GsonBuilder().setPrettyPrinting().create();
            Config config = null;
            VREMWriter writer = null;
            MongoDatabase db = null;

            String json = new String(Files.readAllBytes(Paths.get(this.config)), UTF_8);
            config = gson.fromJson(json, Config.class);

            final Path exhibitionRoot = Paths.get(exhibitionPath);
            if (!exhibitionRoot.toFile().isDirectory()) {
                System.err.println("--path argument has to point to a folder");
            }

            /* Prepare database & DAO. */
            final CodecRegistry registry = CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), CodecRegistries.fromProviders(new VREMCodecProvider()));
            final ConnectionString connectionString = config.database.getConnectionString();
            final MongoClientSettings settings = MongoClientSettings.builder().codecRegistry(registry).applyConnectionString(connectionString).applicationName("VREM").build();

            final MongoClient client = MongoClients.create(settings);
            db = client.getDatabase(config.database.database);
            writer = new VREMWriter(db);

            VREMReader reader = new VREMReader(db);
            if (reader.getExhibition(name) != null) {
                if (!clean) {
                    LOGGER.warn("An exhibition with name {} already exists. Please remove this exhibition from your database. Exiting.", name);
                    return;
                }
                writer.deleteExhibition(name);
            }
            Exhibition exhibition = new Exhibition(name, exhibitionDescription);

            LOGGER.info("Starting to import exhibition at {}", exhibitionRoot);
            Arrays.stream(Objects.requireNonNull(exhibitionRoot.toFile().listFiles(File::isDirectory))).forEach(f -> {
                if (f.getName().startsWith("__")) { // TODO Extract const
                    return;
                }
                try {
                    exhibition.addRoom(importRoom(exhibitionRoot.getParent(), f, exhibition.getRooms()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            writer.saveExhibition(exhibition);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Room importRoom(Path root, File room, List<Room> siblings) throws IOException {
        LOGGER.info("Importing room {}", room);
        if (!room.isDirectory()) {
            throw new IllegalArgumentException("Cannot import file-based rooms. Only folder-based rooms are supported.");
        }

        Room roomConfig;
        if (Paths.get(room.getPath(), ROOM_CONFIG_FILE).toFile().exists()) {
            String configJson = new String(Files.readAllBytes(Paths.get(room.getPath(), ROOM_CONFIG_FILE)), UTF_8);
            roomConfig = gson.fromJson(configJson, Room.class);
            LOGGER.trace("Loaded room config:\n{}", gson.toJson(roomConfig));
        } else {
            roomConfig = new Room(room.getName(), Texture.NONE, Texture.NONE, Vector3f.ORIGIN, ENTRYPOINT);
            LOGGER.debug("Created new room without room config");
        }
        roomConfig.entrypoint = ENTRYPOINT;
        File north = Paths.get(room.getPath(), NORTH_WALL_NAME).toFile();
        File east = Paths.get(room.getPath(), EAST_WALL_NAME).toFile();
        File south = Paths.get(room.getPath(), SOUTH_WALL_NAME).toFile();
        File west = Paths.get(room.getPath(), WEST_WALL_NAME).toFile();

        List<File> wallFiles = new ArrayList<>();
        boolean wallAvailable = false;
        int wallNumber = 0;

        do {
            wallFiles.add(Paths.get(room.getPath(), String.valueOf(wallNumber)).toFile());

            if(Files.exists(Paths.get(room.getPath(), String.valueOf(wallNumber + 1))))
                wallAvailable = true;
            else
                wallAvailable = false;

        } while (wallAvailable);

        for (int i = 0; i < wallFiles.size(); i++) {
            roomConfig.addWall(importWall(i, wallFiles.get(i), root));
        }

        roomConfig.position = calculatePosition(roomConfig, siblings);

        return roomConfig;
    }

    private Wall importWall(int wallNumber, File wallFolder, Path root) throws IOException {
        LOGGER.trace("Importing wall {}", wallFolder);
        Wall wallConfig;
        if (Paths.get(wallFolder.getPath(), WALL_CONFIG_FILE).toFile().exists()) {
            String json = new String(Files.readAllBytes(Paths.get(wallFolder.getPath(), WALL_CONFIG_FILE)), UTF_8);
            wallConfig = gson.fromJson(json, Wall.class);
            wallConfig.wallNumber = String.valueOf(wallNumber);
            LOGGER.trace("Loaded wall config:\n{}", gson.toJson(wallConfig));
        } else {
            wallConfig = new Wall(String.valueOf(wallNumber), Texture.NONE.name());
            LOGGER.debug("Created new wall with default config");
        }

        // Import exhibits
        Arrays.stream(Objects.requireNonNull(wallFolder.listFiles(f -> {
            String extension = FileUtils.getFileExtension(f);
            if (extension == null) {
                LOGGER.error("No extension found for {}", f.toString());
                return false;
            }
            if (extension.equalsIgnoreCase(JSON_EXTENSION)) {
                return false;
            }
            if (extension.equalsIgnoreCase(PNG_EXTENSION) || extension.equalsIgnoreCase(JPG_EXTENSION)) {
                return true;
            }
            LOGGER.debug("ignoring file {} because it has extension {}", f, extension);
            return false;
        }))).forEach(f -> {
            wallConfig.placeExhibit(importExhibit(root.toFile(), f, wallConfig.getExhibits()));
        });

        return wallConfig;
    }

    private Exhibit importExhibit(File exhibitionRoot, File exhibitFile, List<Exhibit> siblings) {
        Exhibit exhibit = null;
        LOGGER.trace("Importing {}", exhibitFile.getName());
        String fileName = exhibitFile.getName().substring(0, exhibitFile.getName().lastIndexOf('.'));
        Path configPath = Paths.get(exhibitFile.toURI()).getParent().resolve(fileName + "." + JSON_EXTENSION);
        if (configPath.toFile().exists()) {
            try {
                String exhibitJson = new String(Files.readAllBytes(configPath), UTF_8);
                try {
                    exhibit = gson.fromJson(exhibitJson, Exhibit.class);
                } catch (Exception err) {
                    LOGGER.error("Error while parsing json from {}", configPath);
                    err.printStackTrace();
                    System.exit(-100);
                }
                Path path = Paths.get(exhibitionRoot.toURI()).relativize(Paths.get(exhibitFile.toURI()));
                exhibit.path = path.toString().replace('\\', '/');
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Path path = Paths.get(exhibitionRoot.toURI()).relativize(Paths.get(exhibitFile.toURI()));
            exhibit = new Exhibit("", "", path.toString().replace('\\', '/'), CHOType.IMAGE, Vector3f.ORIGIN, Vector3f.ORIGIN);
            LOGGER.debug("Creating empty exhibit for path {}", path);
        }
        try {
            BufferedImage img = ImageIO.read(exhibitFile);
            float aspectRatio = (float) img.getHeight() / (float) img.getWidth();
            float width = 2, height = 2; // 2m
            if (img.getWidth() > img.getHeight()) {
                height = (aspectRatio * 200f) / 100f; // in cm for precision
            } else {
                width = (200f / aspectRatio) / 100f;
            }
            if (exhibit.size == null || (exhibit.size.isNaN() || exhibit.size.equals(Vector3f.ORIGIN))) {
                exhibit.size = new Vector3f(width, height, 0);
            }
            if (exhibit.position == null || (exhibit.position.isNaN() || exhibit.position.equals(Vector3f.ORIGIN))) {
                exhibit.position = calculatePosition(exhibit, siblings);
            }
            Optional<Exhibit> reference = findExhibitForPath(this.reference, exhibit.path);
            if (reference.isPresent()) {
                LOGGER.debug("Found reference {}", reference.get());
                mergeDescription(reference.get(), exhibit);
                mergeName(reference.get(), exhibit);
            }
            return exhibit;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    private Vector3f calculatePosition(Exhibit e, List<Exhibit> siblings) {
        if (siblings.isEmpty()) {
            return new Vector3f(ROOM_BORDER + (e.size.x / 2f), EXHIBIT_DEFAULT_HEIGHT, 0);
        } else {
            float dist = (float) siblings.stream().mapToDouble(exhibit -> exhibit.size.x + EXHIBIT_PADDING).sum();
            return new Vector3f(ROOM_BORDER + dist + (e.size.x / 2f), EXHIBIT_DEFAULT_HEIGHT, 0);
        }
    }

    private Vector3f calculatePosition(Room r, List<Room> siblings) {
        // Totally arbitrary
        return new Vector3f(siblings.size(), 0, 0);
    }

    /**
     * Returns the first exhibit with that path
     */
    private Optional<Exhibit> findExhibitForPath(Exhibition exhibition, String path) {
        Optional<Exhibit> out = Optional.empty();
        LOGGER.trace("Finding exhibit for path {}", path);
        if (exhibition != null) {
            for (Room r : exhibition.getRooms()) {
                List<Exhibit> exhibits = new ArrayList<>(r.getExhibits());
                for (Wall w: r.getWalls()) {
                    exhibits.addAll(w.getExhibits());
                }
                for (Exhibit e : exhibits) {
                    LOGGER.trace("Probe: {}, needle: {}", e.path, path);
                    if (path.equals(e.path)) {
                        out = Optional.of(e);
                    }
                }
            }
            LOGGER.trace("Exhibit for path: {}", out);

        } else {
            LOGGER.trace("Couldn't find exhibit for path, no exhibition provided");
        }
        return out;
    }

    private void mergeDescription(Exhibit src, Exhibit dest) {
        if (StringUtils.isNotBlank(src.description) && StringUtils.isBlank(dest.description)) {
            dest.description = src.description;
        }
    }

    private void mergeName(Exhibit src, Exhibit dest) {
        if (StringUtils.isNotBlank(src.name) && StringUtils.isBlank(dest.name)) {
            dest.name = src.name;
        }
    }


}
