package fr.elysia.anticheat.api;

public class CheckResult {

    public static final CheckResult PASS = new CheckResult(false, "");

    private final boolean flagged;
    private final String details;

    private CheckResult(boolean flagged, String details) {
        this.flagged = flagged;
        this.details = details;
    }

    public static CheckResult fail(String details) {
        return new CheckResult(true, details);
    }

    public static CheckResult pass() {
        return PASS;
    }

    public boolean isFlagged() { return flagged; }
    public String getDetails() { return details; }
}
