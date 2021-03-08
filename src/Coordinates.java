/**
 * class describing coordinates
 */
public class Coordinates {
    /**
     * standart constructor
     * @param x coordinate
     * @param y coordinate
     */
    public Coordinates(double x, Double y) {
        this.x = x;
        this.y = y;
    }

    private double x;

    @Override
    public String toString() {
        return String.format("(%.0f, %.0f)", x, y);
    }

    private Double y; //Поле не может быть null

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public Double getY() {
        return y;
    }

    public void setY(Double y) {
        this.y = y;
    }
}