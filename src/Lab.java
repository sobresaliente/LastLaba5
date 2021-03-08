import java.io.*;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * class of interactive work with laba
 */
public class Lab {

    private Map<Long, SpaceMarine> marines = new HashMap<>();

    private final String saveFilePath = System.getenv("FILE");

    private long maxid = 0;

    private int scriptDepth = 0;

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yy");

    /**
     * method of reading file
     * @return exception if you have them
     * @throws CustomFileException
     */
    public Map<Integer, String> readFile() throws CustomFileException {
        if (saveFilePath == null) {
            throw CustomFileException.envVarNotSet();
        }
        InputStream stream;
        try {
            stream = new FileInputStream(saveFilePath);
        } catch (FileNotFoundException e) {
            throw CustomFileException.notFound();
        }
        InputStreamReader reader = new InputStreamReader(stream);
        String line;
        int i = 1;
        Map<Integer, String> errors = new HashMap<>();
        while (true) {
            try {
                if ((line = readLine(reader)).isEmpty()) break;
            } catch (IOException e) {
                throw CustomFileException.readProblem();
            }
            String[] ogFields = line.split(" *, *");
            if (Arrays.stream(ogFields).anyMatch(s -> !(s.startsWith("\"") == s.endsWith("\"")))) {
                errors.put(i++, "bad quoting");
                continue;
            }
            String[] fields = Arrays.stream(ogFields).map(s -> {
                if (s.equals("null")) {
                    return null;
                }
                return s.replaceAll("^\"|\"$", "");
            }).toArray(String[]::new);

            if (fields.length != 12) {
                errors.put(i++, "only " + fields.length + "fields");
                continue;
            }
            long key;
            try {
                key = Long.parseLong(fields[0]);
            } catch (NumberFormatException e) {
                errors.put(i++, "invalid key");
                continue;
            }
            long id;
            try {
                id = Long.parseLong(fields[1]);
            } catch (NumberFormatException e) {
                errors.put(i++, "invalid id");
                continue;
            }
            String name = fields[2];
            if (name.isEmpty()) {
                errors.put(i++, "invalid name");
                continue;
            }
            double x;
            double y;
            try {
                x = Double.parseDouble(fields[3]);
            } catch (NumberFormatException e) {
                errors.put(i++, "invalid x coordinate");
                continue;
            }
            try {
                y = Double.parseDouble(fields[4]);
            } catch (NumberFormatException e) {
                errors.put(i++, "invalid y coordinate");
                continue;
            }
            Coordinates coordinates = new Coordinates(x, y);
            Date creationDate;
            try {
                 creationDate = dateFormat.parse(fields[5]);
            } catch (ParseException e) {
                errors.put(i++, "invalid date");
                continue;
            }
            float health;
            try {
                health = Float.parseFloat(fields[6]);
                if (health <= 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                errors.put(i++, "invalid health");
                continue;
            }
            AstartesCategory category;
            if (fields[7] == null) {
                category = null;
            } else {
                try {
                    category = AstartesCategory.valueOf(fields[7]);
                } catch (IllegalArgumentException e) {
                    errors.put(i++, "invalid category");
                    continue;
                }
            }
            Weapon weaponType;
            try {
                weaponType = Weapon.valueOf(fields[8]);
            } catch (IllegalArgumentException e) {
                errors.put(i++, "invalid weapon type");
                continue;
            }
            MeleeWeapon meleeWeapon;
            try {
                meleeWeapon = MeleeWeapon.valueOf(fields[9]);
            } catch (IllegalArgumentException e) {
                errors.put(i++, "invalid melee weapon type");
                continue;
            }
            Chapter chapter;
            if (fields[10] == null) {
                chapter = null;
            } else {
                String chapterName = fields[10];
                String world = fields[11];
                chapter = new Chapter(chapterName, world);
            }
            marines.put(key, new SpaceMarine(id, name, coordinates, creationDate, health, category, weaponType, meleeWeapon, chapter));
            i++;
        }
        maxid = marines.values().stream().map(SpaceMarine::getId).max(Long::compare).orElse(0L);
        return errors;
    }

    /**
     * custom class of reading line
     * @param reader - input stream reader
     * @return string which we read
     * @throws IOException
     */
    private static String readLine(InputStreamReader reader) throws IOException {
        StringBuilder builder = new StringBuilder();
        int ci;
        while ((ci = reader.read()) != -1) {
            char c = (char) ci;
            if (c == '\n') {
                break;
            }
            builder.append(c);
        }
        return builder.toString();
    }

    /**
     * method of work with singlearg commands
     * @param args your args
     * @param parse function parse
     * @param isValid - predicate form math
     * @param commandName
     * @param argName
     * @param validityErrorMessage - error if you have them
     * @param action
     * @param <T>
     */
    private <T> void simpleSingleArg(String[] args, Function<String, T> parse, Predicate<T> isValid, String commandName, String argName, String validityErrorMessage, Consumer<T> action) {
        if (args.length == 1) {
            System.out.println(argName + " required");
        }
        else if (args.length > 2) {
            System.out.println(commandName + " only takes 1 same-line argument");
        }
        else {
            try {
                T t = parse.apply(args[1]);
                if (isValid.test(t)) {
                    action.accept(t);
                }
                else {
                    System.out.println(validityErrorMessage);
                }
            } catch (Exception e) {
                    System.out.println("invalid " + argName);
            }
        }
    }

    /**
     * method for parse
     * @param o - line
     * @return string with parsed line
     */
    private static String quotedToString(Object o) {
        if (o == null) {
            return "null";
        }
        return "\"" + o + "\"";
    }

    /**
     * method for interactive work
     * @param scanner
     * @param quiet boolean for hiding console
     */
    public void interact(Scanner scanner, boolean quiet) {
        if (!quiet) {
            System.out.print("> ");
        }
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.length() > 256) {
                System.out.println("input too long");
                continue;
            }
            String[] args = line.trim().split(" +");
            if (args.length > 0) {
                String command = args[0];
                if (command.equals("help")) {
                    help();
                }
                else if (command.equals("info")) {
                    info();
                }
                else if (command.equals("show")) {
                    show();
                }
                else if (command.equals("insert")) {
                    simpleSingleArg(args,
                            Long::parseLong,
                            k -> !marines.containsKey(k),
                            "insert",
                            "key",
                            "key already present",
                            k -> insert(k, readMarine(scanner, quiet)));
                }
                else if (command.equals("update")) {
                    simpleSingleArg(args,
                            Long::parseLong,
                            id -> marines.values().stream().anyMatch(m -> m.getId().equals(id)),
                            "update",
                            "id",
                            "id not found",
                            id -> update(id, readMarine(scanner, quiet)));
                }
                else if (command.equals("remove_key")) {
                    simpleSingleArg(args,
                            Long::parseLong,
                            marines::containsKey,
                            "remove_key",
                            "key",
                            "key not found",
                            this::removeKey);
                }
                else if (command.equals("clear")) {
                    clear();
                }
                else if (command.equals("save")) {
                    File file = new File(saveFilePath);
                    if (!file.exists()) {
                        try {
                            file.createNewFile();
                        } catch (IOException e) {
                            System.out.println("can't create file");
                            continue;
                        }
                    }
                    if (file.canWrite()) {
                        try {
                            PrintWriter writer = new PrintWriter(saveFilePath);
                            marines.forEach((key, m) -> writer.println(
                                    Stream.of(key, m.getId(), m.getName(), m.getCoordinates().getX(),
                                            m.getCoordinates().getY(), dateFormat.format(m.getCreationDate()),
                                            m.getHealth(), m.getCategory(), m.getWeaponType(), m.getMeleeWeapon(),
                                            m.getChapter() == null ? null : m.getChapter().getName(),
                                            m.getChapter() == null ? null : m.getChapter().getWorld())
                                            .map(Lab::quotedToString).collect(Collectors.joining(", "))
                            ));
                            writer.close();
                        } catch (FileNotFoundException ignored) {}
                    }
                    else {
                        System.out.println("bad permissions");
                    }
                }
                else if (command.equals("execute_script")) {
                    if (args.length == 1) {
                        System.out.println("file required");
                    }
                    else if (args.length > 2) {
                        System.out.println("execute_script only takes 1 argument");
                    }
                    else if (scriptDepth == 1) {
                        System.out.println("maximum recursion depth reached, skipping execute_script");
                    }
                    else {
                        File scriptFile = new File(args[1]);
                        try {
                            if (Files.isReadable(scriptFile.toPath())) {
                                Scanner scriptScanner = new Scanner(new FileInputStream(scriptFile));
                                scriptDepth++;
                                interact(scriptScanner, true);
                                scriptDepth--;
                            }
                            else {
                                System.out.println("file not readable");
                            }
                        } catch (FileNotFoundException e) {
                            System.out.println("file not found");
                        }
                    }
                }
                else if (command.equals("exit")) {
                    break;
                }
                else if (command.equals("remove_lower")) {
                    if (args.length > 1) {
                        System.out.println("remove_lower doesn't take any same-line arguments");
                    }
                    else {
                        removeLower(readMarine(scanner, quiet));
                    }
                }
                else if (command.equals("replace_if_lower")) {
                    simpleSingleArg(args,
                            Long::parseLong,
                            marines::containsKey,
                            "replace_if_lower",
                            "key",
                            "key not found",
                            k -> replaceIfLower(k, readMarine(scanner, quiet)));
                }
                else if (command.equals("remove_lower_key")) {
                    simpleSingleArg(args,
                            Long::parseLong,
                            k -> true,
                            "remove_lower_key",
                            "key",
                            "",
                            this::removeLowerKey);
                }
                else if (command.equals("group_counting_by_creation_date")) {
                    groupCountingByCreationDate();
                }
                else if (command.equals("filter_greater_than_category")) {
                    if (args.length > 1) {
                        System.out.println("filter_greater_than_category doesn't take any same-line arguments");
                    }
                    else {
                        AstartesCategory category = readObject(scanner,
                                AstartesCategory::valueOf,
                                c -> true,
                                "Enter category (one of [" +
                                        Arrays.stream(AstartesCategory.values()).map(AstartesCategory::toString)
                                                .collect(Collectors.joining(", ")) + "]): ",
                                "invalid category",
                                false,
                                quiet);
                        filterGreaterThanCategory(category);
                    }
                }
                else if (command.equals("print_ascending")) {
                    printAscending();
                }
                else {
                    System.out.println("unknown command");
                }
            }
            if (!quiet) {
                System.out.print("> ");
            }
        }
    }

    /**
     * show help
     */
    private static void help() {
        System.out.println("all args written as {arg} must be specified on further lines");
        System.out.println("help print help");
        System.out.println("info print info about current state of marines");
        System.out.println("show print all marines");
        System.out.println("insert key {marine} add new marine with given key");
        System.out.println("update id {marine} update marine with given id");
        System.out.println("remove_key key delete marine with given key");
        System.out.println("clear delete all marines");
        System.out.println("save save marines to file");
        System.out.println("execute_script file_name execute script");
        System.out.println("exit end execution");
        System.out.println("remove_lower {marine} delete all marines with health lower than the one given");
        System.out.println("replace_if_lower key {marine} replace marine with key with given one if the new health is lower than the old");
        System.out.println("remove_lower_key key delete all marines with key lower than given");
        System.out.println("group_counting_by_creation_date print number of marines with each creation date");
        System.out.println("filter_greater_than_category {category} print marines with categories higher than the one given");
        System.out.println("print_ascending print all marines sorted by health");
    }

    /**
     * show info
     */
    private void info() {
        System.out.println("type: HashMap<Long, SpaceMarine>");
        System.out.println("number of elements: " + marines.size());
        if (!marines.isEmpty()) {
            System.out.println("newest marine created on " + marines.values().stream().max(Comparator.comparing(SpaceMarine::getCreationDate)));
        }
    }

    /**
     * print information
     * @param key from hasmap
     * @param marine elemnt of collection
     */
    private static void printMarine(Long key, SpaceMarine marine) {
        System.out.println("Key: " + key);
        System.out.println("ID: " + marine.getId());
        System.out.println("Name: " + marine.getName());
        System.out.println("Coordinates: " + marine.getCoordinates());
        System.out.println("Creation date: " + dateFormat.format(marine.getCreationDate()));
        System.out.println("Health: " + marine.getHealth());
        System.out.println("Category: " + marine.getCategory());
        System.out.println("Weapon type: " + marine.getWeaponType());
        System.out.println("Melee weapon: " + marine.getMeleeWeapon());
        if (marine.getChapter() == null) {
            System.out.println("Chapter: null");
        }
        else {
            System.out.println("Chapter name: " + marine.getChapter().getName());
            System.out.println("Chapter world: " + marine.getChapter().getWorld());
        }
    }

    /**
     * reading the object - the same with simple arg
     * @param scanner
     * @param conv
     * @param isValid
     * @param promptMessage
     * @param errorMessage
     * @param canBeEmpty
     * @param quiet
     * @param <T>
     * @return
     */
    private static <T> T readObject(Scanner scanner, Function<String, T> conv, Predicate<T> isValid, String promptMessage, String errorMessage, boolean canBeEmpty, boolean quiet) {
        while (true) {
            if (!quiet) {
                System.out.print(promptMessage);
            }
            String line = scanner.nextLine();
            if (line.length() > 256) {
                System.out.println("input too long");
                continue;
            }
            else if (canBeEmpty && line.isEmpty()) {
                return null;
            }
            try {
                T t = conv.apply(line);
                if (isValid.test(t)) {
                    return t;
                }
            } catch (Exception ignored) {}
            System.out.println(errorMessage);
        }
    }

    /**
     *  read marine to interact
     * @param scanner the same with interact
     * @param quiet the same with interact
     * @return
     */
    private SpaceMarine readMarine(Scanner scanner, boolean quiet) {
        System.out.println("Note: all decimal fractions are stored with limited precision and may be rounded from the value given");
        String name = readObject(scanner,
                s -> s,
                s -> !s.isEmpty(),
                "Enter name: ",
                "name can't be empty",
                false,
                quiet);

        double x = readObject(scanner,
                Double::parseDouble,
                d -> true,
                "Enter x coordinate (decimal fraction): ",
                "not a valid coordinate",
                false,
                quiet);
        Double y = readObject(scanner,
                Double::parseDouble,
                d -> true,
                "Enter y coordinate (decimal fraction): ",
                "not a valid coordinate",
                false,
                quiet);

        Coordinates coordinates = new Coordinates(x, y);

        Date creationDate = new Date();

        Float health = readObject(scanner,
                Float::parseFloat,
                f -> f > 0,
                "Enter health (decimal fraction, must be >0): ",
                "not a valid health value",
                false,
                quiet);

        AstartesCategory category = readObject(scanner,
                AstartesCategory::valueOf,
                c -> true,
                "Enter a category (one of [" + Arrays.stream(AstartesCategory.values())
                        .map(AstartesCategory::toString)
                        .collect(Collectors.joining(", ")) + "]) or leave empty: ",
                "not a valid category",
                true,
                quiet);

        Weapon weaponType = readObject(scanner,
                Weapon::valueOf,
                w -> true,
                "Enter a weapong type (one of [" + Arrays.stream(Weapon.values())
                        .map(Weapon::toString)
                        .collect(Collectors.joining(", ")) + "]): ",
                "not a valid weapon type",
                false,
                quiet);

        MeleeWeapon meleeWeapon = readObject(scanner,
                MeleeWeapon::valueOf,
                mw -> true,
                "Enter a melee weapon type (one of [" + Arrays.stream(MeleeWeapon.values())
                        .map(MeleeWeapon::toString)
                        .collect(Collectors.joining(", ")) + "]): ",
                "not a valid melee weapon type",
                false,
                quiet);

        boolean needChapter = readObject(scanner,
                s -> {
                    if (s.equals("y")) {
                        return true;
                    } else if (s.equals("n")) {
                        return false;
                    } else {
                        throw new IllegalArgumentException();
                    }
                },
                nc -> true,
                "Do you want to add a chapter (y/n): ",
                "enter 'y' or 'n'",
                false,
                quiet);
        Chapter chapter = null;
        if (needChapter) {
            String chapterName = readObject(scanner,
                    cn -> cn,
                    s -> !s.isEmpty(),
                    "Enter chapter name: ",
                    "chapter name can't be empty",
                    false,
                    quiet);

            String world = readObject(scanner,
                    w -> w,
                    w -> true,
                    "Enter world name or leave empty: ",
                    "",
                    true,
                    quiet);
            chapter = new Chapter(chapterName, world);
        }
        return new SpaceMarine(++maxid, name, coordinates, creationDate, health,
                category, weaponType, meleeWeapon, chapter);
    }


    private void show() {
        marines.forEach((key, marine) -> {
            printMarine(key, marine);
            System.out.println();
        });
    }

    private void insert(Long key, SpaceMarine marine) {
        marines.put(key, marine);
    }

    /**
     * update marine
     * @param id of marine
     * @param marine whick we want update
     */
    private void update(Long id, SpaceMarine marine) {
        marine.setId(id);
        marines.keySet().stream()
                .filter(k -> marines.get(k).getId().equals(id))
                .forEach(k -> marines.put(k, marine));
    }

    private void removeKey(Long key) {
        marines.remove(key);
    }

    private void clear() {
        marines.clear();
    }

    private void removeLower(SpaceMarine marine) {
        marines.keySet().stream()
                .filter(k -> marines.get(k).getHealth() < marine.getHealth())
                .forEach(marines::remove);
    }

    private void replaceIfLower(Long key, SpaceMarine marine) {
        if (marine.getHealth() < marines.get(key).getHealth()) {
            marines.put(key, marine);
        }
    }

    private void removeLowerKey(Long key) {
        marines.keySet().stream()
                .filter(k -> k < key)
                .forEach(marines::remove);
    }

    private void groupCountingByCreationDate() {
        Map<Date, Long> groups = marines.values().stream()
                .collect(Collectors.groupingBy(SpaceMarine::getCreationDate
                        , Collectors.counting()));
        groups.forEach((date, number)
                -> System.out.println(dateFormat.format(date) + ": " + number));
    }

    private void filterGreaterThanCategory(AstartesCategory category) {
        for (Map.Entry<Long, SpaceMarine> e : marines.entrySet()) {
            AstartesCategory marineCat = e.getValue().getCategory();
            if (marineCat!= null && marineCat.ordinal() > category.ordinal()) {
                printMarine(e.getKey(), e.getValue());
                System.out.println();
            }
        }
    }

    private void printAscending() {
        marines.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(e -> {
                    printMarine(e.getKey(), e.getValue());
                    System.out.println();
                });
    }
}
