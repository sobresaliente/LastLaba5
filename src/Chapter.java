/**
 * class describing chapter of astartes
 */
public class Chapter {
    private String name; //Поле не может быть null, Строка не может быть пустой
    private String world; //Поле может быть null


    @Override
    public String toString() {
        return "Chapter{" +
                "name='" + name + '\'' +
                ", world='" + world + '\'' +
                '}';
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWorld() {
        return world;
    }

    public void setWorld(String world) {
        this.world = world;
    }

    /**
     * standart constructor
     * @param name  of chapter
     * @param world of chapter
     */
    public Chapter(String name, String world) {
        this.name = name;
        this.world = world;
    }
}
