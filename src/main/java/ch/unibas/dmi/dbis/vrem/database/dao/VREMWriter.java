package ch.unibas.dmi.dbis.vrem.database.dao;

import ch.unibas.dmi.dbis.vrem.database.codec.ExhibitionCodec;
import ch.unibas.dmi.dbis.vrem.model.collection.ArtCollection;
import ch.unibas.dmi.dbis.vrem.model.collection.ExhibitUpload;
import ch.unibas.dmi.dbis.vrem.model.exhibition.Exhibit;
import ch.unibas.dmi.dbis.vrem.model.exhibition.Exhibition;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

public class VREMWriter extends VREMDao {

    private static final Logger LOGGER = LogManager.getLogger(VREMWriter.class);

    /**
     *
     */
    public VREMWriter(MongoDatabase database) {
        super(database);
    }

    /**
     *
     */
    public boolean saveExhibition(Exhibition exhibition) {
        final MongoCollection<Exhibition> collection = this.database.getCollection(EXHIBITION_COLLECTION, Exhibition.class);
        final UpdateResult result = collection.replaceOne(Filters.eq("_id", exhibition.id), exhibition);
        if (result.getMatchedCount() == 0) {
            collection.insertOne(exhibition);
        } else if (result.getModifiedCount() == 0) {
            return false;
        }
        return true;
    }

    public String uploadExhibit(ExhibitUpload exhibitUpload) {
        final MongoCollection<ArtCollection> mongoCollection = this.database.getCollection(CORPUS_COLLECTION, ArtCollection.class);

        // Construct new Exhibit to generate ID
        Exhibit to_add = new Exhibit(exhibitUpload.exhibit.name, exhibitUpload.exhibit.description, exhibitUpload.exhibit.path, exhibitUpload.exhibit.type, exhibitUpload.exhibit.position, exhibitUpload.exhibit.size, exhibitUpload.exhibit.audio, exhibitUpload.exhibit.light);

        // Construct Path with ID
        to_add.path = exhibitUpload.artCollection + "/" + to_add.id + "." + exhibitUpload.fileExtension;

        if(mongoCollection.countDocuments() == 0){
            LOGGER.debug("There is no previous document in {} collection. We're adding one", CORPUS_COLLECTION);
            ArtCollection artCollection = new ArtCollection("classics", Arrays.asList(to_add));
            mongoCollection.insertOne(artCollection);
            LOGGER.debug("Successfully inserted a new artcollection");
        }else{
            UpdateResult result = mongoCollection.updateOne(Filters.eq("name", exhibitUpload.artCollection), Updates.push("exhibits", to_add));
            if(result.getMatchedCount() == 0){
                LOGGER.error("Could not update the art collection");
            }
        }
        return to_add.path;

    }

    /**
     * @param key {@link ExhibitionCodec#FIELD_NAME_NAME} of the exhibition to be deleted. all exhibitions with said key will be removed.
     */
    public void deleteExhibition(String key) {
        this.database.getCollection(EXHIBITION_COLLECTION).deleteMany(Filters.eq(ExhibitionCodec.FIELD_NAME_NAME, key));
    }
}
