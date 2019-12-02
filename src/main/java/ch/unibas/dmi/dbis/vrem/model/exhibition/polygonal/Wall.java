package ch.unibas.dmi.dbis.vrem.model.exhibition.polygonal;

import ch.unibas.dmi.dbis.vrem.model.Vector3f;
import ch.unibas.dmi.dbis.vrem.model.exhibition.Exhibit;
import ch.unibas.dmi.dbis.vrem.model.exhibition.Texture;
import ch.unibas.dmi.dbis.vrem.model.objects.CulturalHeritageObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Wall {

    /**
     *
     */
    public final Vector3f color;

    /**
     *
     */
    public final String texture;

    /**
     *
     */
    public List<Vector3f> wallCoordinates = new ArrayList<>(4);

    /**
     *
     */
    public int wallNumber;

    /**
     *
     */
    private List<Exhibit> exhibits = new ArrayList<>();

    /**
     *
     */
    public Wall(int wallNumber, Vector3f color) {
        this.wallNumber = wallNumber;
        this.color = color;
        this.texture = Texture.NONE.toString();
    }

    /**
     *
     */
    public Wall(int wallNumber, String texture) {
        this.wallNumber = wallNumber;
        this.color = Vector3f.UNIT;
        this.texture = texture;
    }

    /**
     *
     */
    public boolean placeExhibit(Exhibit exhibit) {
        if (exhibit.type != CulturalHeritageObject.CHOType.IMAGE) {
            throw new IllegalArgumentException("Only images can be placed on walls.");
        }
        if (!this.exhibits.contains(exhibit)) {
            this.exhibits.add(exhibit);
            return true;
        } else {
            return false;
        }
    }

    /**
     *
     */
    public List<Exhibit> getExhibits() {
        if (this.exhibits == null) {
            exhibits = new ArrayList<>();
        }
        return Collections.unmodifiableList(this.exhibits);
    }
}
