package no.ion.jhms.modularizer;

public class ErrorException extends MainException {
    public ErrorException(String message) {
        super("error: " + message);
    }
}
