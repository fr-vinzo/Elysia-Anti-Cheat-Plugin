package fr.elysia.anticheat.checks;

public enum CheckCategory {
    MOVEMENT("Mouvement"),
    COMBAT("Combat"),
    WORLD("Monde");

    private final String displayName;

    CheckCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() { return displayName; }
}
