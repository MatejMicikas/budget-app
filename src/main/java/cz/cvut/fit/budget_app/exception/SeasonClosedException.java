package cz.cvut.fit.budget_app.exception;

public class SeasonClosedException extends RuntimeException {
    public SeasonClosedException(Long seasonId) {
        super("Season " + seasonId + " is closed and cannot be modified");
    }
}
