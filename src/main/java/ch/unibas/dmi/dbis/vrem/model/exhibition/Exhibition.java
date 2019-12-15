package ch.unibas.dmi.dbis.vrem.model.exhibition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import ch.unibas.dmi.dbis.vrem.model.exhibition.polygonal.Room;
import ch.unibas.dmi.dbis.vrem.model.objects.CulturalHeritageObject;
import org.bson.types.ObjectId;

public class Exhibition {


    public final ObjectId id;

    public final String name;

    public final String description;

    private final List<Room> rooms = new ArrayList<>();

    private final List<Corridor> corridors = new ArrayList<>();

    public Exhibition(ObjectId id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public Exhibition(String name, String description) {
        this(new ObjectId(), name, description);
    }

    public boolean addRoom(Room room) {
        if (!this.rooms.contains(room)) {
            this.rooms.add(room);
            return true;
        } else {
            return false;
        }
    }

    public boolean addCorridor(Corridor corridor) {
        if (!this.corridors.contains(corridor)) {
            this.corridors.add(corridor);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "Exhibition{" + "id='" + id + '\'' + ", name='" + name + '\'' + ", description='" + description + '\'' + ", rooms=" + rooms + '\'' + ", corridors=" + corridors +'}';
    }

    public List<Room> getRooms() {
        if (rooms == null) {
            return Collections.unmodifiableList(new ArrayList<>());
        }
        return Collections.unmodifiableList(this.rooms);
    }

    public List<Corridor> getCorridors() {
        if (corridors == null) {
            return Collections.unmodifiableList(new ArrayList<>());
        }
        return Collections.unmodifiableList(this.corridors);
    }

    public List<Exhibit> getExhibits(){
        final List<Exhibit> list = new ArrayList<>();

        if(rooms == null){
            return Collections.unmodifiableList(new ArrayList<>());
        }else{
            this.rooms.forEach(r -> {
                list.addAll(r.getExhibits());
                r.getWalls().forEach(w -> {
                    list.addAll(w.getExhibits());
                });
            });
        }

        if(corridors == null){
            return Collections.unmodifiableList(new ArrayList<>());
        }else{
            this.corridors.forEach(r -> {
                list.addAll(r.getExhibits());
                list.addAll(r.getNorth().getExhibits());
                list.addAll(r.getSouth().getExhibits());
            });
        }

        return Collections.unmodifiableList(list);
    }

    public List<Exhibit> getExhibits(CulturalHeritageObject.CHOType type){
        return Collections.unmodifiableList(getExhibits().stream().filter(e -> e.type.equals(type)).collect(Collectors.toList()));
    }
}
