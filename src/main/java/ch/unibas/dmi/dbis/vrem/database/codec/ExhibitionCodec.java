package ch.unibas.dmi.dbis.vrem.database.codec;

import ch.unibas.dmi.dbis.vrem.model.exhibition.Exhibition;
import ch.unibas.dmi.dbis.vrem.model.exhibition.polygonal.Room;
import java.util.LinkedList;
import java.util.List;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.types.ObjectId;


public class ExhibitionCodec implements Codec<Exhibition> {

    public static final String FIELD_NAME_ID = "_id";
    public static final String FIELD_NAME_NAME = "name";
    public static final String FIELD_NAME_DESCRIPTION = "description";
    public static final String FIELD_NAME_ROOMS = "rooms";

    private final Codec<Room> codec;

    public ExhibitionCodec(CodecRegistry registry) {
        this.codec = registry.get(Room.class);
    }

    @Override
    public Exhibition decode(BsonReader reader, DecoderContext decoderContext) {
        reader.readStartDocument();
        ObjectId id = null;
        String name = null;
        String description = null;
        List<Room> rooms = new LinkedList<>();

        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            switch (reader.readName()) {
                case FIELD_NAME_ID:
                    id = reader.readObjectId();
                    break;
                case FIELD_NAME_NAME:
                    name = reader.readString();
                    break;
                case FIELD_NAME_DESCRIPTION:
                    description = reader.readString();
                    break;
                case FIELD_NAME_ROOMS:
                    reader.readStartArray();
                    while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                        rooms.add(this.codec.decode(reader, decoderContext));
                    }
                    reader.readEndArray();
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.readEndDocument();
        final Exhibition exhibition = new Exhibition(id, name, description);
        for (Room room : rooms) {
            exhibition.addRoom(room);
        }
        return exhibition;
    }

    @Override
    public void encode(BsonWriter writer, Exhibition value, EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeObjectId(FIELD_NAME_ID, value.id);
        writer.writeString(FIELD_NAME_NAME, value.name);
        writer.writeString(FIELD_NAME_DESCRIPTION, value.description);
        writer.writeName(FIELD_NAME_ROOMS);
        writer.writeStartArray();
        for (Room room : value.getRooms()) {
            this.codec.encode(writer, room, encoderContext);
        }
        writer.writeEndArray();
        writer.writeEndDocument();
    }

    @Override
    public Class<Exhibition> getEncoderClass() {
        return Exhibition.class;
    }
}
