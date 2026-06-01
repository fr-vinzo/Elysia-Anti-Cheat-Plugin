package fr.elysia.anticheat.api;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Événement lancé chaque fois qu'un joueur déclenche une vérification de l'anti-cheat.
 * D'autres plugins peuvent annuler cet événement pour empêcher l'action.
 */
public class ViolationEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String checkName;
    private final int violationLevel;
    private final String details;
    private boolean cancelled;

    public ViolationEvent(Player player, String checkName, int violationLevel, String details) {
        this.player = player;
        this.checkName = checkName;
        this.violationLevel = violationLevel;
        this.details = details;
    }

    public Player getPlayer() { return player; }
    public String getCheckName() { return checkName; }
    public int getViolationLevel() { return violationLevel; }
    public String getDetails() { return details; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
