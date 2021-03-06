package ch.unibas.dmi.dbis.vrem.database.codec.polygonalCodec;

import ch.unibas.dmi.dbis.vrem.model.Vector3f;
import ch.unibas.dmi.dbis.vrem.model.exhibition.Exhibit;
import ch.unibas.dmi.dbis.vrem.model.exhibition.polygonal.Room;
import ch.unibas.dmi.dbis.vrem.model.exhibition.Texture;
import ch.unibas.dmi.dbis.vrem.model.exhibition.polygonal.Wall;
import java.util.ArrayList;
import java.util.List;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

public class RoomCodec implements Codec<Room> {

    private final String FIELD_NAME_TEXT = "text";
    private final String FIELD_NAME_FLOOR = "floor";
    private final String FIELD_NAME_CEILING = "ceiling";
    //private final String FIELD_NAME_SIZE = "size";
    private final String FIELD_NAME_CEILING_SCALE = "ceiling_scale";
    private final String FIELD_NAME_HEIGHT = "height";
    private final String FIELD_NAME_POSITION = "position";
    //private final String FIELD_NAME_ENTRYPOINT = "entrypoint";
    private final String FIELD_NAME_WALLS = "walls";
    private final String FIELD_NAME_EXHIBITS = "exhibits";
    private final String FIELD_NAME_AMBIENT = "ambient";

    private final Codec<Exhibit> exhibitCodec;

    private final Codec<Wall> wallCodec;

    private final Codec<Vector3f> vectorCodec;

    /**
     *
     */
    public RoomCodec(CodecRegistry registry) {
        this.exhibitCodec = registry.get(Exhibit.class);
        this.wallCodec = registry.get(Wall.class);
        this.vectorCodec = registry.get(Vector3f.class);
    }

    /**
     *
     */
    @Override
    public Room decode(BsonReader reader, DecoderContext decoderContext) {
        reader.readStartDocument();
        String text = null;
        String floor = Texture.WOOD1.name();
        String ceiling = Texture.CONCRETE.name();
        //Vector3f size = null;
        double ceiling_scale = 1.0;
        double height = 5.0;
        Vector3f position = null;
        //Vector3f entrypoint = null;
        List<Wall> walls = new ArrayList<>();
        List<Exhibit> exhibits = new ArrayList<>();
        String ambient = null;

        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            switch (reader.readName()) {
                case FIELD_NAME_TEXT:
                    text = reader.readString();
                    break;
                case FIELD_NAME_FLOOR:
                    floor = reader.readString();
                    break;
                case FIELD_NAME_CEILING:
                    ceiling = reader.readString();
                    break;
                /*case FIELD_NAME_SIZE:
                    size = this.vectorCodec.decode(reader, decoderContext);
                    break;*/
                case FIELD_NAME_CEILING_SCALE:
                    ceiling_scale = reader.readDouble();
                    break;
                case FIELD_NAME_HEIGHT:
                    height = reader.readDouble();
                    break;
                case FIELD_NAME_POSITION:
                    position = this.vectorCodec.decode(reader, decoderContext);
                    break;
                /*case FIELD_NAME_ENTRYPOINT:
                    entrypoint = this.vectorCodec.decode(reader, decoderContext);
                    break;*/
                case FIELD_NAME_WALLS:
                    reader.readStartArray();
                    while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                        walls.add(this.wallCodec.decode(reader, decoderContext));
                    }
                    reader.readEndArray();
                    break;
                case FIELD_NAME_EXHIBITS:
                    reader.readStartArray();
                    while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                        exhibits.add(this.exhibitCodec.decode(reader, decoderContext));
                    }
                    reader.readEndArray();
                    break;
                case FIELD_NAME_AMBIENT:
                    ambient = reader.readString();
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.readEndDocument();
        final Room room = new Room(text, walls, floor, ceiling, height, position, ceiling_scale, ambient);
        for (Exhibit exhibit : exhibits) {
            room.placeExhibit(exhibit);
        }
        return room;
    }

    @Override
    public void encode(BsonWriter writer, Room value, EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeString(FIELD_NAME_TEXT, value.text);
        writer.writeString(FIELD_NAME_FLOOR, value.floor);
        writer.writeString(FIELD_NAME_CEILING, value.ceiling);
        /*writer.writeName(FIELD_NAME_SIZE);
        this.vectorCodec.encode(writer, value.size, encoderContext);*/
        writer.writeDouble(FIELD_NAME_HEIGHT, value.height);
        writer.writeName(FIELD_NAME_POSITION);
        this.vectorCodec.encode(writer, value.position, encoderContext);
        writer.writeDouble(FIELD_NAME_CEILING_SCALE, value.ceiling_scale);
        /*writer.writeName(FIELD_NAME_ENTRYPOINT);
        this.vectorCodec.encode(writer, value.entrypoint, encoderContext);*/
        writer.writeName(FIELD_NAME_WALLS);
        writer.writeStartArray();
        for (Wall wall: value.getWalls())
            this.wallCodec.encode(writer, wall, encoderContext);
        writer.writeEndArray();
        writer.writeName(FIELD_NAME_EXHIBITS);
        writer.writeStartArray();
        for (Exhibit exhibit : value.getExhibits()) {
            this.exhibitCodec.encode(writer, exhibit, encoderContext);
        }
        writer.writeEndArray();
        if (value.ambient != null) {
            writer.writeString(FIELD_NAME_AMBIENT, value.ambient);
        }
        writer.writeEndDocument();
    }

    @Override
    public Class<Room> getEncoderClass() {
        return Room.class;
    }
}
