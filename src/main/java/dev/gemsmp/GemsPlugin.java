package dev.gemsmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class GemsPlugin extends JavaPlugin {
    private DataStore data;
    private GemItems items;
    private AbilityManager abilities;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.data = new DataStore(this);
        this.items = new GemItems(this);
        this.abilities = new AbilityManager(this);

        getServer().getPluginManager().registerEvents(new GemListener(this), this);

        GemsCommand command = new GemsCommand(this);
        PluginCommand pluginCommand = getCommand("gems");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }

        // Once a second: remove duplicate gems and apply passive effects.
        getServer().getScheduler().runTaskTimer(this, this::tick, 20L, 20L);
        getLogger().info("GemsSMP enabled.");
    }

    public DataStore data() {
        return data;
    }

    public GemItems items() {
        return items;
    }

    public AbilityManager abilities() {
        return abilities;
    }

    /** Creates a brand new registered gem and puts it in the player's inventory. */
    public ItemStack giveGem(Player player, Gem gem, int tier) {
        UUID id = UUID.randomUUID();
        ItemStack item = items.create(gem, tier, id);
        data.registerGem(id, gem);
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
        for (ItemStack extra : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), extra);
        }
        return item;
    }

    private void tick() {
        Set<UUID> seen = new HashSet<>();

        for (Player player : getServer().getOnlinePlayers()) {
            PlayerInventory inv = player.getInventory();

            // Every gem has a unique id. If the same id shows up twice, the extra copy is deleted.
            boolean removed = false;
            for (int i = 0; i < inv.getSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (!items.isGem(stack)) {
                    continue;
                }
                UUID gemId = items.getId(stack);
                if (gemId == null || !data.isRegistered(gemId)) {
                    continue;
                }
                if (!seen.add(gemId)) {
                    inv.setItem(i, null);
                    removed = true;
                }
            }
            if (removed) {
                player.sendMessage(Component.text("A duplicate gem was removed from your inventory.", NamedTextColor.RED));
            }

            // Passive effect from the gem in the offhand.
            ItemStack off = inv.getItemInOffHand();
            if (!items.isGem(off)) {
                continue;
            }
            UUID offId = items.getId(off);
            if (offId == null || !data.isRegistered(offId)) {
                continue;
            }
            Gem gem = items.getGem(off);
            if (gem == null) {
                continue;
            }
            int tier = items.getTier(off);
            int amplifier = tier >= 2 ? gem.ampTier2() : gem.ampTier1();
            int duration = gem.passiveEffect() == PotionEffectType.NIGHT_VISION ? 260 : 60;
            player.addPotionEffect(new PotionEffect(gem.passiveEffect(), duration, amplifier, true, false, true));
        }
    }
}
