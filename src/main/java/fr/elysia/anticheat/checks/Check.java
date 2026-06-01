package fr.elysia.anticheat.checks;

import fr.elysia.anticheat.ElysiaAntiCheat;

public abstract class Check {

    protected final ElysiaAntiCheat plugin;
    private final String name;
    private final CheckCategory category;
    private boolean enabled;

    protected Check(ElysiaAntiCheat plugin, String name, CheckCategory category) {
        this.plugin = plugin;
        this.name = name;
        this.category = category;
        this.enabled = plugin.getConfig().getBoolean("checks." + name.toLowerCase().replace(" ", "-") + ".enabled", true);
    }

    public String getName() { return name; }
    public CheckCategory getCategory() { return category; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    protected int getAlertVL() {
        return plugin.getConfig().getInt("checks." + getConfigKey() + ".alert-vl", 5);
    }

    protected int getKickVL() {
        return plugin.getConfig().getInt("checks." + getConfigKey() + ".kick-vl", 15);
    }

    private String getConfigKey() {
        return name.toLowerCase().replace(" ", "-");
    }

    @Override
    public String toString() {
        return "[" + category.getDisplayName() + "] " + name;
    }
}
