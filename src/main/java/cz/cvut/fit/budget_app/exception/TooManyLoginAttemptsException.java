package cz.cvut.fit.budget_app.exception;

public class TooManyLoginAttemptsException extends RuntimeException {
    public TooManyLoginAttemptsException() {
        super("Too many failed login attempts. Try again later.");
    }
}
