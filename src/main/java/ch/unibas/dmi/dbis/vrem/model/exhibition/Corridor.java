package ch.unibas.dmi.dbis.vrem.model.exhibition;

import ch.unibas.dmi.dbis.vrem.model.Vector3f;
import ch.unibas.dmi.dbis.vrem.model.exhibition.polygonal.Wall;
import ch.unibas.dmi.dbis.vrem.model.exhibition.polygonal.Room;
import ch.unibas.dmi.dbis.vrem.model.objects.CulturalHeritageObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A Corridor is a Room with only 2 walls
 */
public class Corridor {


    public final String text;

    public final String floor;

    public final String ceiling;

    public Vector3f size;
    public Vector3f entrypoint;
    public final String ambient;
    /**
     * List of exhibits (only 3D models valid).
     */
    private List<Exhibit> exhibits = new ArrayList<>();
    public Vector3f position;
    /**
     *
     * List of walls (4 max).
     */
    private List<Wall> walls = new ArrayList<>(2);

    public List<Room> connects = new ArrayList<>(2);

    public Corridor(String text, Texture floor, Texture ceiling, Vector3f size, Vector3f position, Vector3f entrypoint, List<Room> connects) {
        this(text, new ArrayList<>(2), floor.name(), ceiling.name(), size, position, entrypoint, null, connects);
    }

    public Corridor(String text, List<Wall> walls, Texture floor, Texture ceiling, Vector3f size, Vector3f position, Vector3f entrypoint, List<Room> connects) {
        this(text, walls, floor.toString(), ceiling.toString(), size, position, entrypoint, null, connects);
    }

    public Corridor(String text, List<Wall> walls, String floor, String ceiling, Vector3f size, Vector3f position, Vector3f entrypoint, String ambient, List<Room> connects) {
        this.floor = floor;
        this.ceiling = ceiling;
        this.size = size;
        this.text = text;
        this.position = position;
        this.entrypoint = entrypoint;
        this.walls.addAll(walls);
        this.ambient = ambient;
        this.connects = connects;
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
        return "Corridor{" + "text='" + this.text + '\'' + ", floor='" + this.floor + '\'' + ", ceiling='" + this.ceiling + '\'' + ", size=" + this.size + ", entrypoint=" + this.entrypoint + ", ambient='" + this.ambient + '\'' + ", exhibits=" + this.exhibits + ", position=" + this.position + ", walls=" + this.walls + '}';
    }

    public List<Exhibit> getExhibits() {
        if (this.exhibits == null) {
            exhibits = new ArrayList<>();
        }
        return Collections.unmodifiableList(this.exhibits);
    }
}
