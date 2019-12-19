package ch.unibas.dmi.dbis.vrem.model.exhibition.polygonal;

import ch.unibas.dmi.dbis.vrem.model.Vector3f;
import ch.unibas.dmi.dbis.vrem.model.exhibition.Exhibit;
import ch.unibas.dmi.dbis.vrem.model.exhibition.Texture;
import ch.unibas.dmi.dbis.vrem.model.objects.CulturalHeritageObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Room {

    public final String text;

    public final String floor;

    public final String ceiling;

    public double height;
    public final String ambient;
    /**
     * List of exhibits (only 3D models valid).
     */
    private List<Exhibit> exhibits = new ArrayList<>();
    public Vector3f position;

    public double ceiling_scale;

    /**
     * List of walls.
     */
    private List<Wall> walls = new ArrayList<>();

    public Room(String text, Texture floor, Texture ceiling, double height, Vector3f position, double ceiling_scale) {
        this(text, new ArrayList<>(), floor.name(), ceiling.name(), height, position, ceiling_scale, null);

    }

    public Room(String text, List<Wall> walls, Texture floor, Texture ceiling, double height, Vector3f position, double ceiling_scale) {
        this(text, walls, floor.toString(), ceiling.toString(), height, position, ceiling_scale, null);
    }

    public Room(String text, List<Wall> walls, String floor, String ceiling, double height, Vector3f position, double ceiling_scale, String ambient) {
        this.floor = floor;
        this.ceiling = ceiling;
        this.text = text;
        this.height = height;
        this.position = position;
        this.ceiling_scale = ceiling_scale;
        this.walls.addAll(walls);
        this.ambient = ambient;
    }

    public void addWall(Wall wall) {
        if (this.walls == null) {
            this.walls = new ArrayList<>();
        }

        this.walls.add(wall);
    }

    public List<Wall> getWalls() {
        return this.walls;
    }

    public boolean placeExhibit(Exhibit exhibit) {
        if (exhibit.type != CulturalHeritageObject.CHOType.MODEL) {
            throw new IllegalArgumentException("Only 3D objects can be placed in a room.");
        }
        if (!this.exhibits.contains(exhibit)) {
            this.exhibits.add(exhibit);
            return true;
        }
        return false;
    }


    private void setWall(Wall w) {
        //this.walls.add(dir.ordinal(),w);
        if (this.walls == null) {
            this.walls = new ArrayList<>();
        }
        this.walls.add(w);
    }


    @Override
    public String toString() {
        return "Room{" + "text='" + text + '\'' + ", floor='" + floor + '\'' + ", ceiling='" + ceiling + '\'' + ", ambient='" + ambient + '\'' + ", exhibits=" + exhibits + ", position=" + position + ", walls=" + walls + '}';
    }

    public List<Exhibit> getExhibits() {
        if (this.exhibits == null) {
            exhibits = new ArrayList<>();
        }
        return Collections.unmodifiableList(this.exhibits);
    }
}
