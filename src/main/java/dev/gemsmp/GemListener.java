package dev.gemsmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class GemListener implements Listener {
    private final GemsPlugin plugin;

    public GemListener(GemsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String mode = plugin.getConfig().getString("first-join-gem", "random");
        if (!"random".equalsIgnoreCase(mode)) {
            return;
        }
        if (plugin.data().hasReceivedFirstGem(player.getUniqueId())) {
            return;
        }
        plugin.data().markFirstGem(player.getUniqueId());

        Gem[] all = Gem.values();
        Gem gem = all[ThreadLocalRandom.current().nextInt(all.length)];
        plugin.giveGem(player, gem, 1);
        player.sendMessage(Component.text("You received the " + gem.displayName()
                + " Gem! Hold it in your offhand, then sneak + right-click with an empty main hand.", gem.color()));
    }

    /** Sneak + right-click with an empty main hand while holding a gem in the offhand. */
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.isSneaking()) {
            return;
        }
        PlayerInventory inv = player.getInventory();
        if (!inv.getItemInMainHand().getType().isAir()) {
            return;
        }
        ItemStack off = inv.getItemInOffHand();
        if (!plugin.items().isGem(off)) {
            return;
        }
        UUID id = plugin.items().getId(off);
        if (id == null || !plugin.data().isRegistered(id)) {
            return;
        }
        event.setCancelled(true);
        plugin.abilities().use(player, off);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (plugin.items().isGem(event.getItemInHand())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCraft(PrepareItemCraftEvent event) {
        for (ItemStack stack : event.getInventory().getMatrix()) {
            if (plugin.items().isGem(stack)) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        FileConfiguration config = plugin.getConfig();
        boolean keep = config.getBoolean("keep-gem-on-death", true);
        boolean loseTier = config.getBoolean("lose-tier-on-death", true);
        boolean upgrade = config.getBoolean("upgrade-on-kill", true);

        boolean victimHadGem = false;
        if (event.getKeepInventory()) {
            victimHadGem = hasGem(victim);
        } else {
            for (ItemStack stack : new ArrayList<>(event.getDrops())) {
                if (!plugin.items().isGem(stack)) {
                    continue;
                }
                victimHadGem = true;
                if (loseTier && plugin.items().getTier(stack) > 1) {
                    plugin.items().setTier(stack, 1);
                }
                if (keep) {
                    event.getDrops().remove(stack);
                    event.getItemsToKeep().add(stack);
                }
            }
        }

        Player killer = victim.getKiller();
        if (upgrade && victimHadGem && killer != null && !killer.equals(victim)) {
            upgradeGems(killer);
        }
    }

    private boolean hasGem(Player player) {
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            if (plugin.items().isGem(inv.getItem(i))) {
                return true;
            }
        }
        return false;
    }

    private void upgradeGems(Player killer) {
        PlayerInventory inv = killer.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!plugin.items().isGem(stack) || plugin.items().getTier(stack) >= 2) {
                continue;
            }
            Gem gem = plugin.items().getGem(stack);
            if (gem == null) {
                continue;
            }
            plugin.items().setTier(stack, 2);
            inv.setItem(i, stack);
            killer.sendMessage(Component.text("Your " + gem.displayName() + " Gem ascended to Tier 2!",
                    NamedTextColor.LIGHT_PURPLE));
            killer.playSound(killer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        }
    }
}
