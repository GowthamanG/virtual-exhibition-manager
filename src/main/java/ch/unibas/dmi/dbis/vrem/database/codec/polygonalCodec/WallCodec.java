package ch.unibas.dmi.dbis.vrem.database.codec.polygonalCodec;

import ch.unibas.dmi.dbis.vrem.model.Vector3f;
import ch.unibas.dmi.dbis.vrem.model.exhibition.cuboid.Direction;
import ch.unibas.dmi.dbis.vrem.model.exhibition.Exhibit;
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

public class WallCodec implements Codec<Wall> {

    private final String FIELD_NAME_WALLNUMBER = "wallNumber";
    private final String FIELD_NAME_WALLCOORDINATES = "wallCoordinates";
    private final String FIELD_NAME_TEXTURE = "texture";
    private final String FIELD_NAME_COLOR = "color";
    private final String FIELD_NAME_EXHIBITS = "exhibits";

    private final Codec<Vector3f> vectorCodec;

    private final Codec<Exhibit> exhibitCodec;


    public WallCodec(CodecRegistry registry) {
        this.vectorCodec = registry.get(Vector3f.class);
        this.exhibitCodec = registry.get(Exhibit.class);
    }


    @Override
    public Wall decode(BsonReader reader, DecoderContext decoderContext) {
        reader.readStartDocument();
        String texture = null;
        String wallNumber = null;
        Vector3f position = null;
        Vector3f color = null;
        List<Exhibit> exhibits = new ArrayList<>();
        List<Vector3f> wallCoordinates = new ArrayList<>();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            switch (reader.readName()) {
                case FIELD_NAME_WALLNUMBER:
                     wallNumber = reader.readString();
                    break;
                case FIELD_NAME_WALLCOORDINATES:
                    reader.readStartArray();
                    while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                        wallCoordinates.add(this.vectorCodec.decode(reader, decoderContext));
                    }
                    reader.readEndArray();
                    break;
                case FIELD_NAME_TEXTURE:
                    //texture = Texture.valueOf(reader.readString());
                    texture = reader.readString();
                    break;
                case FIELD_NAME_COLOR:
                    color = this.vectorCodec.decode(reader, decoderContext);
                    break;
                case FIELD_NAME_EXHIBITS:
                    reader.readStartArray();
                    while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                        exhibits.add(this.exhibitCodec.decode(reader, decoderContext));
                    }
                    reader.readEndArray();
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.readEndDocument();

        /* Make final assembly. */
        Wall wall;
        if (texture == null) {
            wall = new Wall(wallNumber, color);
        } else {
            wall = new Wall(wallNumber, texture);
        }

        wall.wallCoordinates.addAll(wallCoordinates);

        for (Exhibit exhibit : exhibits) {
            wall.placeExhibit(exhibit);
        }
        return wall;
    }

    @Override
    public void encode(BsonWriter writer, Wall value, EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeString(FIELD_NAME_WALLNUMBER, String.valueOf(value.wallNumber));
        writer.writeName(FIELD_NAME_WALLCOORDINATES);
        writer.writeStartArray();
        for (Vector3f coordinate : value.wallCoordinates) {
            this.vectorCodec.encode(writer, coordinate, encoderContext);
        }
        writer.writeEndArray();
        writer.writeString(FIELD_NAME_TEXTURE, value.texture);
        writer.writeName(FIELD_NAME_COLOR);
        this.vectorCodec.encode(writer, value.color, encoderContext);
        writer.writeName(FIELD_NAME_EXHIBITS);
        writer.writeStartArray();
        for (Exhibit exhibit : value.getExhibits()) {
            this.exhibitCodec.encode(writer, exhibit, encoderContext);
        }
        writer.writeEndArray();
        writer.writeEndDocument();
    }

    @Override
    public Class<Wall> getEncoderClass() {
        return Wall.class;
    }
}
