import java.util.Comparator;
import java.util.Map;
import java.util.Scanner;

/**
 * main class
 */
public class Main {
    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        Lab lab = new Lab();

        try {
            Map<Integer, String> errors =  lab.readFile();
            if (!errors.isEmpty()) {
                System.out.println("Errors reading file:");
                errors.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e ->
                    System.out.println("line " + e.getKey() + ": " + e.getValue()));
            }
        } catch (CustomFileException e) {
            System.out.println("Error reading file: " + e.getMessage());
            return;
        }

        lab.interact(scanner, false);
    }
}
