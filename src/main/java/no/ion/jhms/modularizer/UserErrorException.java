package no.ion.jhms.modularizer;

public class UserErrorException extends MainException {
    public UserErrorException(String message) { super(message + "\ntry --help to get more information"); }
}
