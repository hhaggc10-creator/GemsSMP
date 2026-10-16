package dev.gemsmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Builds gem items and reads their hidden data (type, tier, unique id). */
public final class GemItems {
    private final NamespacedKey typeKey;
    private final NamespacedKey tierKey;
    private final NamespacedKey idKey;

    public GemItems(JavaPlugin plugin) {
        this.typeKey = new NamespacedKey(plugin, "gem_type");
        this.tierKey = new NamespacedKey(plugin, "gem_tier");
        this.idKey = new NamespacedKey(plugin, "gem_id");
    }

    public ItemStack create(Gem gem, int tier, UUID id) {
        ItemStack item = new ItemStack(gem.material());
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(typeKey, PersistentDataType.STRING, gem.name());
        pdc.set(tierKey, PersistentDataType.INTEGER, tier);
        pdc.set(idKey, PersistentDataType.STRING, id.toString());
        applyDisplay(meta, gem, tier);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isGem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(typeKey, PersistentDataType.STRING);
    }

    public Gem getGem(ItemStack item) {
        if (!isGem(item)) {
            return null;
        }
        String name = item.getItemMeta().getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
        return Gem.fromName(name);
    }

    public int getTier(ItemStack item) {
        if (!isGem(item)) {
            return 1;
        }
        Integer tier = item.getItemMeta().getPersistentDataContainer().get(tierKey, PersistentDataType.INTEGER);
        return tier == null ? 1 : tier;
    }

    public UUID getId(ItemStack item) {
        if (!isGem(item)) {
            return null;
        }
        String raw = item.getItemMeta().getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public void setTier(ItemStack item, int tier) {
        Gem gem = getGem(item);
        if (gem == null) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(tierKey, PersistentDataType.INTEGER, tier);
        applyDisplay(meta, gem, tier);
        item.setItemMeta(meta);
    }

    private void applyDisplay(ItemMeta meta, Gem gem, int tier) {
        meta.displayName(text(gem.displayName() + " Gem", gem.color()).decoration(TextDecoration.BOLD, true));

        List<Component> lore = new ArrayList<>();
        lore.add(text("Tier " + tier + (tier >= 2 ? "  ★" : ""),
                tier >= 2 ? NamedTextColor.LIGHT_PURPLE : NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(text("Passive (offhand): " + gem.passiveName(), NamedTextColor.GRAY));
        lore.add(text("Ability: " + gem.abilityName(), gem.color()));
        lore.add(text(gem.abilityDescription(), NamedTextColor.DARK_GRAY));
        lore.add(Component.empty());
        lore.add(text("Sneak + right-click with an empty", NamedTextColor.DARK_GRAY));
        lore.add(text("main hand to use your ability.", NamedTextColor.DARK_GRAY));
        meta.lore(lore);
        meta.setEnchantmentGlintOverride(true);
    }

    private static Component text(String content, TextColor color) {
        return Component.text(content, color).decoration(TextDecoration.ITALIC, false);
    }
}
