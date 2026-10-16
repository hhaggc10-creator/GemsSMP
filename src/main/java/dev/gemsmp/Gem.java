package dev.gemsmp;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

public enum Gem {
    FIRE("Fire", NamedTextColor.GOLD, Material.BLAZE_POWDER,
            PotionEffectType.FIRE_RESISTANCE, "Fire Resistance", 0, 0,
            "Inferno Burst", "Ignites and blasts back nearby enemies."),
    SPEED("Speed", NamedTextColor.AQUA, Material.SUGAR,
            PotionEffectType.SPEED, "Speed", 0, 1,
            "Dash", "Launch yourself forward with a burst of speed."),
    STRENGTH("Strength", NamedTextColor.RED, Material.RED_DYE,
            PotionEffectType.STRENGTH, "Strength", 0, 1,
            "Shockwave", "Weakens and withers nearby enemies while you shrug off damage."),
    LIFE("Life", NamedTextColor.GREEN, Material.GLISTERING_MELON_SLICE,
            PotionEffectType.REGENERATION, "Regeneration", 0, 1,
            "Healing Surge", "Heals you and trusted allies and grants absorption hearts."),
    SHADOW("Shadow", NamedTextColor.DARK_PURPLE, Material.ECHO_SHARD,
            PotionEffectType.NIGHT_VISION, "Night Vision", 0, 0,
            "Shadow Step", "Blink forward and turn briefly invisible."),
    STORM("Storm", NamedTextColor.YELLOW, Material.PRISMARINE_CRYSTALS,
            PotionEffectType.HASTE, "Haste", 0, 1,
            "Thunderstrike", "Call lightning down on nearby enemies.");

    private final String displayName;
    private final TextColor color;
    private final Material material;
    private final PotionEffectType passiveEffect;
    private final String passiveName;
    private final int ampTier1;
    private final int ampTier2;
    private final String abilityName;
    private final String abilityDescription;

    Gem(String displayName, TextColor color, Material material,
        PotionEffectType passiveEffect, String passiveName, int ampTier1, int ampTier2,
        String abilityName, String abilityDescription) {
        this.displayName = displayName;
        this.color = color;
        this.material = material;
        this.passiveEffect = passiveEffect;
        this.passiveName = passiveName;
        this.ampTier1 = ampTier1;
        this.ampTier2 = ampTier2;
        this.abilityName = abilityName;
        this.abilityDescription = abilityDescription;
    }

    public String displayName() { return displayName; }
    public TextColor color() { return color; }
    public Material material() { return material; }
    public PotionEffectType passiveEffect() { return passiveEffect; }
    public String passiveName() { return passiveName; }
    public int ampTier1() { return ampTier1; }
    public int ampTier2() { return ampTier2; }
    public String abilityName() { return abilityName; }
    public String abilityDescription() { return abilityDescription; }

    public static Gem fromName(String name) {
        for (Gem gem : values()) {
            if (gem.name().equalsIgnoreCase(name)) {
                return gem;
            }
        }
        return null;
    }
}
