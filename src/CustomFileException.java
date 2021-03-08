/**
 * class of exceptions
 */
public class CustomFileException extends Exception {

    /**
     * standart constructor
     * @param message of exception
     */
    public CustomFileException(String message) {
        super(message);
    }

    /**
     * not found exception
     * @return exception
     */
    public static CustomFileException notFound() {
        return new CustomFileException("file not found");
    }

    /**
     * exception with reading file
     * @return exception
     */
    public static CustomFileException readProblem() {
        return new CustomFileException("problem reading file");
    }

    /**
     * exception with environment variable
     * @return exception
     */
    public static CustomFileException envVarNotSet() {
        return new CustomFileException("the FILE env variable that should point to a save file is not set");
    }
}