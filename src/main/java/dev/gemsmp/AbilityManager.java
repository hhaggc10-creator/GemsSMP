package dev.gemsmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Cooldowns and the six gem abilities. */
public final class AbilityManager {
    private final GemsPlugin plugin;
    private final Map<UUID, Map<Gem, Long>> cooldowns = new HashMap<>();
    private final Map<UUID, Integer> lastTriggerTick = new HashMap<>();

    public AbilityManager(GemsPlugin plugin) {
        this.plugin = plugin;
    }

    public void use(Player player, ItemStack gemItem) {
        // Minecraft can fire the click event for both hands in the same tick; only act once.
        int tick = Bukkit.getCurrentTick();
        Integer last = lastTriggerTick.put(player.getUniqueId(), tick);
        if (last != null && last == tick) {
            return;
        }

        Gem gem = plugin.items().getGem(gemItem);
        if (gem == null) {
            return;
        }
        int tier = plugin.items().getTier(gemItem);

        long now = System.currentTimeMillis();
        Map<Gem, Long> map = cooldowns.computeIfAbsent(player.getUniqueId(), k -> new EnumMap<>(Gem.class));
        long readyAt = map.getOrDefault(gem, 0L);
        if (now < readyAt) {
            long secs = (readyAt - now + 999) / 1000;
            player.sendActionBar(Component.text(gem.abilityName() + " ready in " + secs + "s", NamedTextColor.RED));
            return;
        }

        boolean used;
        switch (gem) {
            case FIRE -> used = fire(player, tier);
            case SPEED -> used = speed(player, tier);
            case STRENGTH -> used = strength(player, tier);
            case LIFE -> used = life(player, tier);
            case SHADOW -> used = shadow(player, tier);
            case STORM -> used = storm(player, tier);
            default -> used = false;
        }

        if (used) {
            map.put(gem, now + (long) (cooldownSeconds(gem, tier) * 1000));
            player.sendActionBar(Component.text(gem.abilityName() + "!", gem.color()));
        }
    }

    // ---------------------------------------------------------------- config helpers

    private static String key(Gem gem) {
        return gem.name().toLowerCase(Locale.ROOT);
    }

    private double num(Gem gem, String path, double def) {
        return plugin.getConfig().getDouble("gems." + key(gem) + "." + path, def);
    }

    private double cooldownSeconds(Gem gem, int tier) {
        String path = tier >= 2 ? "cooldown-tier2" : "cooldown-tier1";
        return num(gem, path, tier >= 2 ? 40 : 60);
    }

    // ---------------------------------------------------------------- targeting

    /** Enemies near the caster: players who aren't trusted by them, plus hostile mobs. */
    private List<LivingEntity> enemies(Player caster, double radius) {
        List<LivingEntity> out = new ArrayList<>();
        Location origin = caster.getLocation();
        for (Entity e : caster.getNearbyEntities(radius, radius, radius)) {
            if (!(e instanceof LivingEntity living)) {
                continue;
            }
            if (e instanceof Player other) {
                GameMode mode = other.getGameMode();
                if (mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR) {
                    continue;
                }
                if (plugin.data().isTrusted(caster.getUniqueId(), other.getUniqueId())) {
                    continue;
                }
            } else if (!(e instanceof Monster)) {
                continue;
            }
            if (living.getLocation().distanceSquared(origin) <= radius * radius) {
                out.add(living);
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- abilities

    private boolean fire(Player p, int tier) {
        double radius = num(Gem.FIRE, "radius", 7);
        int burnSeconds = tier >= 2 ? 8 : 5;
        Location origin = p.getLocation();
        World world = p.getWorld();

        world.spawnParticle(Particle.FLAME, origin.clone().add(0, 1, 0), 120, 1.5, 0.8, 1.5, 0.08);
        world.playSound(origin, Sound.ENTITY_BLAZE_SHOOT, 1.2f, 0.7f);

        for (LivingEntity e : enemies(p, radius)) {
            e.setFireTicks(burnSeconds * 20);
            Vector push = e.getLocation().toVector().subtract(origin.toVector()).setY(0);
            if (push.lengthSquared() > 0.0001) {
                e.setVelocity(push.normalize().multiply(0.9).setY(0.35));
            }
        }
        return true;
    }

    private boolean speed(Player p, int tier) {
        Vector dir = p.getLocation().getDirection().setY(0.15);
        if (dir.lengthSquared() < 0.0001) {
            return false;
        }
        p.setVelocity(dir.normalize().multiply(tier >= 2 ? 2.2 : 1.8));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 5, 2, true, true, true));
        p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation(), 40, 0.4, 0.2, 0.4, 0.05);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 1.4f);
        return true;
    }

    private boolean strength(Player p, int tier) {
        double radius = num(Gem.STRENGTH, "radius", 6);
        for (LivingEntity e : enemies(p, radius)) {
            e.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20 * 8, tier >= 2 ? 1 : 0));
            e.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 20 * 4, 0));
            e.getWorld().spawnParticle(Particle.CRIT, e.getLocation().add(0, 1, 0), 25, 0.4, 0.5, 0.4, 0.2);
        }
        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 8, tier >= 2 ? 1 : 0));
        p.getWorld().spawnParticle(Particle.CRIT, p.getLocation().add(0, 1, 0), 60, 1.2, 0.6, 1.2, 0.3);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.2f, 0.6f);
        return true;
    }

    private boolean life(Player p, int tier) {
        double heal = tier >= 2 ? 10 : 6;
        double radius = num(Gem.LIFE, "radius", 6);

        healPlayer(p, heal, tier);
        for (Entity e : p.getNearbyEntities(radius, radius, radius)) {
            if (e instanceof Player ally && plugin.data().isTrusted(p.getUniqueId(), ally.getUniqueId())) {
                healPlayer(ally, heal / 2, tier);
            }
        }
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        return true;
    }

    private void healPlayer(Player target, double amount, int tier) {
        AttributeInstance attr = target.getAttribute(Attribute.MAX_HEALTH);
        double max = attr == null ? 20.0 : attr.getValue();
        target.setHealth(Math.min(max, target.getHealth() + amount));
        target.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 20 * 10, tier >= 2 ? 1 : 0));
        target.getWorld().spawnParticle(Particle.HEART, target.getLocation().add(0, 1.5, 0), 8, 0.4, 0.4, 0.4, 0);
    }

    private boolean shadow(Player p, int tier) {
        double range = num(Gem.SHADOW, "range", 14) * (tier >= 2 ? 1.4 : 1.0);
        Location origin = p.getLocation();

        Vector dir = p.getEyeLocation().getDirection();
        dir.setY(Math.max(-0.3, dir.getY()));
        dir.normalize();

        // Walk forward in half-block steps and stop before anything solid or liquid.
        Location last = origin.clone();
        for (double d = 0.5; d <= range; d += 0.5) {
            Location probe = origin.clone().add(dir.clone().multiply(d));
            Block feet = probe.getBlock();
            Block head = probe.clone().add(0, 1, 0).getBlock();
            if (!feet.isPassable() || !head.isPassable() || feet.isLiquid() || head.isLiquid()) {
                break;
            }
            last = probe;
        }

        if (last.distanceSquared(origin) < 1.0) {
            p.sendActionBar(Component.text("No room to blink that way", NamedTextColor.RED));
            return false;
        }

        World world = p.getWorld();
        world.spawnParticle(Particle.PORTAL, origin.clone().add(0, 1, 0), 60, 0.4, 0.8, 0.4, 0.5);
        world.playSound(origin, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        p.teleport(last);
        world.spawnParticle(Particle.PORTAL, last.clone().add(0, 1, 0), 60, 0.4, 0.8, 0.4, 0.5);
        world.playSound(last, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.3f);

        p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20 * (tier >= 2 ? 8 : 5), 0, true, false, true));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20 * 4, 0, true, false, true));
        return true;
    }

    private boolean storm(Player p, int tier) {
        double radius = num(Gem.STORM, "radius", 14);
        int max = tier >= 2 ? 5 : 3;
        Location here = p.getLocation();

        List<LivingEntity> targets = enemies(p, radius);
        if (targets.isEmpty()) {
            p.sendActionBar(Component.text("No targets nearby", NamedTextColor.RED));
            return false;
        }
        targets.sort(Comparator.comparingDouble((LivingEntity e) -> e.getLocation().distanceSquared(here)));

        int struck = 0;
        for (LivingEntity target : targets) {
            if (struck >= max) {
                break;
            }
            target.getWorld().strikeLightning(target.getLocation());
            struck++;
        }
        p.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, here.clone().add(0, 1, 0), 60, 0.6, 1.0, 0.6, 0.3);
        return true;
    }
}
