package se.kth.id2203.overlay.lookuptable;

/**
 * @author Kim Hammar on 2017-02-18.
 */
public class PartitionAssignmentException extends Exception {

    /**
     * Constructs a new exception with the specified detail message.  The
     * cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public PartitionAssignmentException(String message) {
        super(message);
    }
}
