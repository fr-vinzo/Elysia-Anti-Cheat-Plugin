package fr.elysia.anticheat.commands;

import fr.elysia.anticheat.ElysiaAntiCheat;
import fr.elysia.anticheat.checks.Check;
import fr.elysia.anticheat.data.PlayerData;
import fr.elysia.anticheat.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class AnticheatCommand implements CommandExecutor {

    private final ElysiaAntiCheat plugin;

    public AnticheatCommand(ElysiaAntiCheat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {

        if (!sender.hasPermission("elysiaac.use")) {
            MessageUtil.send(sender, "&cVous n'avez pas la permission d'utiliser cette commande.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "alerts" -> cmdAlerts(sender);
            case "info" -> cmdInfo(sender, args);
            case "reset" -> cmdReset(sender, args);
            case "kick" -> cmdKick(sender, args);
            case "violations", "vl" -> cmdViolations(sender);
            case "checks" -> cmdChecks(sender);
            case "reload" -> cmdReload(sender);
            case "verbose" -> cmdVerbose(sender, args);
            default -> sendHelp(sender);
        }

        return true;
    }



    private void cmdAlerts(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, "&cCette commande est réservée aux joueurs.");
            return;
        }
        boolean enabled = plugin.getAlertManager().toggleAlerts(player.getUniqueId());
        if (enabled) {
            MessageUtil.send(sender, "&aAlertes &aactivées&a. Tu recevras les notifications de l'anti-cheat.");
        } else {
            MessageUtil.send(sender, "&cAlertes &cdésactivées&c. Tu ne recevras plus les notifications.");
        }
    }

    private void cmdInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageUtil.send(sender, "&cUsage : /eac info <joueur>");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            MessageUtil.send(sender, "&cJoueur &e" + args[1] + " &cnon trouvé ou hors ligne.");
            return;
        }

        PlayerData data = plugin.getPlayerDataManager().get(target.getUniqueId());
        if (data == null) {
            MessageUtil.send(sender, "&cAucune donnée disponible pour ce joueur.");
            return;
        }

        sender.sendMessage(MessageUtil.line('━', 40));
        sender.sendMessage(MessageUtil.colorize(MessageUtil.prefix() + " &eInformations — &f" + target.getName()));
        sender.sendMessage(MessageUtil.line('━', 40));
        sender.sendMessage(MessageUtil.colorize(" &7Total violations &8: &c" + data.getTotalViolations()));
        sender.sendMessage(MessageUtil.colorize(" &7Suspect XRay     &8: " + (data.isXraySuspect() ? "&c✔" : "&a✘")));
        sender.sendMessage(MessageUtil.colorize(" &7Blocs minés       &8: &f" + data.getTotalBlocksMined()));
        sender.sendMessage(MessageUtil.colorize(" &7Minerais minés    &8: &f" + data.getOresMined()));
        sender.sendMessage(MessageUtil.colorize(" &7Ratio mines       &8: &f" + String.format("%.1f%%", data.getOreRatio() * 100)));

        if (!data.getAllViolations().isEmpty()) {
            sender.sendMessage(MessageUtil.colorize(" &7Violations par check &8:"));
            for (Map.Entry<String, Integer> e : data.getAllViolations().entrySet()) {
                sender.sendMessage(MessageUtil.colorize("   &8▸ &e" + e.getKey() + " &8: &c" + e.getValue() + " VL"));
            }
        }

        if (!data.getRecentAlerts().isEmpty()) {
            sender.sendMessage(MessageUtil.colorize(" &7Alertes récentes &8:"));
            int limit = Math.min(5, data.getRecentAlerts().size());
            for (int i = 0; i < limit; i++) {
                sender.sendMessage(MessageUtil.colorize("   &8" + (i + 1) + ". " + data.getRecentAlerts().get(i)));
            }
        }

        sender.sendMessage(MessageUtil.line('━', 40));
    }

    private void cmdReset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("elysiaac.admin")) {
            MessageUtil.send(sender, "&cPermission insuffisante.");
            return;
        }
        if (args.length < 2) {
            MessageUtil.send(sender, "&cUsage : /eac reset <joueur>");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            MessageUtil.send(sender, "&cJoueur &e" + args[1] + " &cnon trouvé.");
            return;
        }

        PlayerData data = plugin.getPlayerDataManager().get(target.getUniqueId());
        if (data != null) {
            data.resetAllViolations();
            data.setXraySuspect(false);
            data.resetMiningWindow();
        }

        MessageUtil.send(sender, "&aViolations de &e" + target.getName() + " &areinitialisées.");
        plugin.getLogger().info(sender.getName() + " a remis à zéro les violations de " + target.getName());
    }

    private void cmdKick(CommandSender sender, String[] args) {
        if (!sender.hasPermission("elysiaac.admin")) {
            MessageUtil.send(sender, "&cPermission insuffisante.");
            return;
        }
        if (args.length < 2) {
            MessageUtil.send(sender, "&cUsage : /eac kick <joueur> [raison]");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            MessageUtil.send(sender, "&cJoueur &e" + args[1] + " &cnon trouvé.");
            return;
        }

        String reason = args.length > 2
                ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length))
                : "Expulsé par le staff Elysia";

        String kickMsg = MessageUtil.colorize(
                "&c&lElysia Anti-Cheat\n&7" + reason + "\n&7Contacte le staff si nécessaire.");
        target.kickPlayer(kickMsg);

        MessageUtil.broadcastAlert(MessageUtil.formatKick(target.getName(), "Staff[" + sender.getName() + "]"));
        MessageUtil.send(sender, "&aJoueur &e" + target.getName() + " &aexpulsé.");
    }

    private void cmdViolations(CommandSender sender) {
        var all = plugin.getPlayerDataManager().getAll();
        boolean found = false;

        sender.sendMessage(MessageUtil.line('━', 40));
        sender.sendMessage(MessageUtil.colorize(MessageUtil.prefix() + " &eJoueurs avec violations actives"));
        sender.sendMessage(MessageUtil.line('━', 40));

        for (PlayerData data : all) {
            if (data.getTotalViolations() > 0) {
                found = true;
                String suspect = data.isXraySuspect() ? " &c[XRay]" : "";
                sender.sendMessage(MessageUtil.colorize(
                        " &8▸ &e" + data.getName() + " &8— &cVL total: " + data.getTotalViolations() + suspect));
            }
        }

        if (!found) {
            sender.sendMessage(MessageUtil.colorize(" &aAucune violation active en ce moment."));
        }
        sender.sendMessage(MessageUtil.line('━', 40));
    }

    private void cmdChecks(CommandSender sender) {
        sender.sendMessage(MessageUtil.line('━', 40));
        sender.sendMessage(MessageUtil.colorize(MessageUtil.prefix() + " &eChecks enregistrés"));
        sender.sendMessage(MessageUtil.line('━', 40));

        for (Check check : plugin.getCheckManager().getAll()) {
            String status = check.isEnabled() ? "&a✔ Activé" : "&c✘ Désactivé";
            sender.sendMessage(MessageUtil.colorize(
                    " &8▸ &f" + check.getName() + " &8[" + check.getCategory().getDisplayName() + "&8] " + status));
        }
        sender.sendMessage(MessageUtil.line('━', 40));
    }

    private void cmdReload(CommandSender sender) {
        if (!sender.hasPermission("elysiaac.admin")) {
            MessageUtil.send(sender, "&cPermission insuffisante.");
            return;
        }
        plugin.reloadConfig();
        plugin.getAntiXRayManager().reload();
        MessageUtil.send(sender, "&aConfiguration rechargée avec succès.");
    }

    @SuppressWarnings("deprecation")
    private void cmdVerbose(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageUtil.send(sender, "&cUsage : /eac verbose <joueur>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            MessageUtil.send(sender, "&cJoueur &e" + args[1] + " &cnon trouvé.");
            return;
        }

        PlayerData data = plugin.getPlayerDataManager().get(target.getUniqueId());
        if (data == null) {
            MessageUtil.send(sender, "&cAucune donnée disponible.");
            return;
        }

        sender.sendMessage(MessageUtil.line('━', 40));
        sender.sendMessage(MessageUtil.colorize(MessageUtil.prefix() + " &eMode verbose — &f" + target.getName()));
        sender.sendMessage(MessageUtil.line('━', 40));
        sender.sendMessage(MessageUtil.colorize(" &7Mode de jeu   &8: &f" + target.getGameMode().name()));
        sender.sendMessage(MessageUtil.colorize(" &7Sur le sol    &8: " + (target.isOnGround() ? "&a✔" : "&c✘")));
        sender.sendMessage(MessageUtil.colorize(" &7En vol        &8: " + (target.isFlying() ? "&c✔" : "&a✘")));
        sender.sendMessage(MessageUtil.colorize(" &7En planant    &8: " + (target.isGliding() ? "&e✔" : "&a✘")));
        sender.sendMessage(MessageUtil.colorize(" &7Airtime       &8: &f" + data.getAirtimeTicks() + " ticks"));
        sender.sendMessage(MessageUtil.colorize(" &7CPS combat    &8: &f" + data.getCPS()));
        sender.sendMessage(MessageUtil.colorize(" &7Cibles rés.   &8: &f" + data.getRecentTargetCount()));
        sender.sendMessage(MessageUtil.colorize(" &7Position      &8: &f"
                + String.format("%.1f, %.1f, %.1f", target.getLocation().getX(),
                target.getLocation().getY(), target.getLocation().getZ())));
        sender.sendMessage(MessageUtil.line('━', 40));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(MessageUtil.line('━', 40));
        sender.sendMessage(MessageUtil.colorize(MessageUtil.prefix() + " &eAide — Elysia Anti-Cheat"));
        sender.sendMessage(MessageUtil.line('━', 40));
        sender.sendMessage(MessageUtil.colorize(" &6/eac alerts          &8— &7Activer/désactiver tes alertes"));
        sender.sendMessage(MessageUtil.colorize(" &6/eac info <joueur>   &8— &7Voir les violations d'un joueur"));
        sender.sendMessage(MessageUtil.colorize(" &6/eac verbose <j>     &8— &7Données en temps réel d'un joueur"));
        sender.sendMessage(MessageUtil.colorize(" &6/eac violations      &8— &7Liste des joueurs suspects"));
        sender.sendMessage(MessageUtil.colorize(" &6/eac checks          &8— &7Liste des checks actifs"));
        if (sender.hasPermission("elysiaac.admin")) {
            sender.sendMessage(MessageUtil.colorize(" &6/eac reset <joueur>  &8— &7Remettre à zéro les violations"));
            sender.sendMessage(MessageUtil.colorize(" &6/eac kick <j> [msg] &8— &7Expulser un joueur"));
            sender.sendMessage(MessageUtil.colorize(" &6/eac reload         &8— &7Recharger la configuration"));
        }
        sender.sendMessage(MessageUtil.line('━', 40));
    }
}
