package at.lab1.drivers.exception;

public class NotAvailableDriverException extends RuntimeException {

    public NotAvailableDriverException(String message) {
        super(message);
    }

    public NotAvailableDriverException(String entry, Throwable cause) {
        super(entry, cause);
    }

}
