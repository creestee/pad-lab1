package at.lab1.rides.exception;

public class EntryNotFoundException extends RuntimeException {
    private String entry;
    private String id;

    public EntryNotFoundException(String entry) {
        super("Entry not found: " + entry);
        this.entry = entry;
    }

    public EntryNotFoundException(String entry, String id) {
        super("Entry not found: " + entry + ", ID: " + id);
        this.entry = entry;
        this.id = id;
    }

    public EntryNotFoundException(String entry, Throwable cause) {
        super(entry, cause);
    }

    public String getEntry() {
        return entry;
    }

    public String getId() { return id; }
}
