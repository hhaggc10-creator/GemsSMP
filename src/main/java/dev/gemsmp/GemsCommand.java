package dev.gemsmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class GemsCommand implements CommandExecutor, TabCompleter {
    private final GemsPlugin plugin;

    public GemsCommand(GemsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            help(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "list" -> list(sender);
            case "use" -> use(sender);
            case "give" -> give(sender, args);
            case "trust" -> trust(sender, args, true);
            case "untrust" -> trust(sender, args, false);
            case "trusted" -> trusted(sender);
            case "reload" -> reload(sender);
            default -> help(sender);
        }
        return true;
    }

    private static void msg(CommandSender sender, String text, NamedTextColor color) {
        sender.sendMessage(Component.text(text, color));
    }

    private void help(CommandSender sender) {
        msg(sender, "GemsSMP commands:", NamedTextColor.GOLD);
        msg(sender, "/gems list - show all gems and what they do", NamedTextColor.GRAY);
        msg(sender, "/gems use - use your offhand gem's ability", NamedTextColor.GRAY);
        msg(sender, "/gems trust <player> - your abilities won't hit them", NamedTextColor.GRAY);
        msg(sender, "/gems untrust <player>", NamedTextColor.GRAY);
        msg(sender, "/gems trusted - list who you trust", NamedTextColor.GRAY);
        if (sender.hasPermission("gems.admin")) {
            msg(sender, "/gems give <player> <gem> [tier] - give a gem (admin)", NamedTextColor.GRAY);
            msg(sender, "/gems reload - reload config.yml (admin)", NamedTextColor.GRAY);
        }
    }

    private void list(CommandSender sender) {
        msg(sender, "Gems:", NamedTextColor.GOLD);
        for (Gem gem : Gem.values()) {
            sender.sendMessage(Component.text(gem.displayName() + ": ", gem.color())
                    .append(Component.text(gem.passiveName() + " passive. " + gem.abilityName()
                            + " - " + gem.abilityDescription(), NamedTextColor.GRAY)));
        }
    }

    private void use(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            msg(sender, "Only players can use gems.", NamedTextColor.RED);
            return;
        }
        ItemStack off = player.getInventory().getItemInOffHand();
        if (!plugin.items().isGem(off)) {
            msg(player, "Put a gem in your offhand first.", NamedTextColor.RED);
            return;
        }
        UUID id = plugin.items().getId(off);
        if (id == null || !plugin.data().isRegistered(id)) {
            msg(player, "That gem isn't valid.", NamedTextColor.RED);
            return;
        }
        plugin.abilities().use(player, off);
    }

    private void give(CommandSender sender, String[] args) {
        if (!sender.hasPermission("gems.admin")) {
            msg(sender, "You don't have permission to do that.", NamedTextColor.RED);
            return;
        }
        if (args.length < 3) {
            msg(sender, "Usage: /gems give <player> <gem> [tier]", NamedTextColor.RED);
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            msg(sender, "That player isn't online.", NamedTextColor.RED);
            return;
        }
        Gem gem = Gem.fromName(args[2]);
        if (gem == null) {
            msg(sender, "Unknown gem. Use /gems list.", NamedTextColor.RED);
            return;
        }
        int tier = 1;
        if (args.length >= 4) {
            try {
                tier = Math.max(1, Math.min(2, Integer.parseInt(args[3])));
            } catch (NumberFormatException ex) {
                msg(sender, "Tier must be 1 or 2.", NamedTextColor.RED);
                return;
            }
        }
        plugin.giveGem(target, gem, tier);
        msg(sender, "Gave " + target.getName() + " a Tier " + tier + " " + gem.displayName() + " Gem.", NamedTextColor.GREEN);
    }

    private void trust(CommandSender sender, String[] args, boolean add) {
        if (!(sender instanceof Player player)) {
            msg(sender, "Only players can do that.", NamedTextColor.RED);
            return;
        }
        if (args.length < 2) {
            msg(player, "Usage: /gems " + (add ? "trust" : "untrust") + " <player>", NamedTextColor.RED);
            return;
        }
        OfflinePlayer target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            target = Bukkit.getOfflinePlayerIfCached(args[1]);
        }
        if (target == null) {
            msg(player, "I don't know a player called " + args[1] + ".", NamedTextColor.RED);
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            msg(player, "You can't trust yourself - you're already safe from your own gems.", NamedTextColor.RED);
            return;
        }
        String name = target.getName() != null ? target.getName() : args[1];
        if (add) {
            plugin.data().trust(player.getUniqueId(), target.getUniqueId());
            msg(player, name + " is now trusted. Your abilities won't hit them.", NamedTextColor.GREEN);
        } else {
            plugin.data().untrust(player.getUniqueId(), target.getUniqueId());
            msg(player, name + " is no longer trusted.", NamedTextColor.YELLOW);
        }
    }

    private void trusted(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            msg(sender, "Only players can do that.", NamedTextColor.RED);
            return;
        }
        List<UUID> list = plugin.data().trusted(player.getUniqueId());
        if (list.isEmpty()) {
            msg(player, "You don't trust anyone yet. Use /gems trust <player>.", NamedTextColor.GRAY);
            return;
        }
        List<String> names = new ArrayList<>();
        for (UUID id : list) {
            String name = Bukkit.getOfflinePlayer(id).getName();
            names.add(name != null ? name : id.toString());
        }
        msg(player, "Trusted: " + String.join(", ", names), NamedTextColor.GREEN);
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("gems.admin")) {
            msg(sender, "You don't have permission to do that.", NamedTextColor.RED);
            return;
        }
        plugin.reloadConfig();
        msg(sender, "GemsSMP config reloaded.", NamedTextColor.GREEN);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("list", "use", "trust", "untrust", "trusted"));
            if (sender.hasPermission("gems.admin")) {
                subs.add("give");
                subs.add("reload");
            }
            return filter(subs, args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && (sub.equals("give") || sub.equals("trust") || sub.equals("untrust"))) {
            List<String> names = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                names.add(online.getName());
            }
            return filter(names, args[1]);
        }
        if (args.length == 3 && sub.equals("give")) {
            List<String> gems = new ArrayList<>();
            for (Gem gem : Gem.values()) {
                gems.add(gem.name().toLowerCase(Locale.ROOT));
            }
            return filter(gems, args[2]);
        }
        if (args.length == 4 && sub.equals("give")) {
            return filter(List.of("1", "2"), args[3]);
        }
        return List.of();
    }

    private static List<String> filter(List<String> options, String typed) {
        List<String> out = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(typed.toLowerCase(Locale.ROOT))) {
                out.add(option);
            }
        }
        return out;
    }
}
