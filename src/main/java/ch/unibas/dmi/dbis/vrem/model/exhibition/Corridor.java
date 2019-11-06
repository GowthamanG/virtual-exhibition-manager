package ch.unibas.dmi.dbis.vrem.model.exhibition;

import ch.unibas.dmi.dbis.vrem.model.Vector3f;
import ch.unibas.dmi.dbis.vrem.model.objects.CulturalHeritageObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A Corridor is a Room with only 2 walls
 */
public class Corridor extends Room {


    /**
     * List of walls (2 max).
     */
    private List<Wall> walls = new ArrayList<>(2);

    public Corridor(String text, Texture floor, Texture ceiling, Vector3f size, Vector3f position, Vector3f entrypoint) {
        this(text, new ArrayList<>(2), floor.name(), ceiling.name(), size, position, entrypoint, null);
    }

    public Corridor(String text, List<Wall> walls, Texture floor, Texture ceiling, Vector3f size, Vector3f position, Vector3f entrypoint) {
        this(text, walls, floor.toString(), ceiling.toString(), size, position, entrypoint, null);
    }

    public Corridor(String text, List<Wall> walls, String floor, String ceiling, Vector3f size, Vector3f position, Vector3f entrypoint, String ambient) {
        this.floor = floor;
        this.ceiling = ceiling;
        this.size = size;
        this.text = text;
        this.position = position;
        this.entrypoint = entrypoint;
        this.walls.addAll(walls);
        this.ambient = ambient;
    }
}
