/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Color
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.NamespacedKey
 *  org.bukkit.Particle
 *  org.bukkit.Particle$DustOptions
 *  org.bukkit.Sound
 *  org.bukkit.attribute.Attribute
 *  org.bukkit.attribute.AttributeInstance
 *  org.bukkit.attribute.AttributeModifier
 *  org.bukkit.attribute.AttributeModifier$Operation
 *  org.bukkit.block.Block
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.EntityType
 *  org.bukkit.entity.FallingBlock
 *  org.bukkit.entity.Fireball
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.entity.Vindicator
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.metadata.FixedMetadataValue
 *  org.bukkit.metadata.MetadataValue
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 *  org.bukkit.projectiles.ProjectileSource
 *  org.bukkit.scheduler.BukkitRunnable
 *  org.bukkit.util.RayTraceResult
 *  org.bukkit.util.Vector
 */
package com.daggersmp.managers;

import com.daggersmp.DaggerSMP;
import com.daggersmp.daggers.DaggerType;
import com.daggersmp.managers.CooldownManager;
import com.daggersmp.managers.VoidStateManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vindicator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class AbilityManager {
    private final DaggerSMP plugin;
    private final Random random = new Random();
    private final Map<UUID, Set<Entity>> mafiaVindicators = new HashMap<UUID, Set<Entity>>();
    private final Map<UUID, List<Block>> earthWalls = new HashMap<UUID, List<Block>>();
    private final Map<UUID, BukkitRunnable> guardianBeamTasks = new HashMap<UUID, BukkitRunnable>();
    private final Set<UUID> ghostFormActive = new HashSet<UUID>();
    private final Set<UUID> windFallDamageImmune = new HashSet<UUID>();
    private final Map<UUID, DaggerType> chanceActiveDagger = new HashMap<UUID, DaggerType>();
    private final Map<UUID, Long> chanceEndTime = new HashMap<UUID, Long>();
    private final Map<UUID, Long> chancePassiveNextRoll = new HashMap<UUID, Long>();
    private final Map<UUID, Map<UUID, Long>> lifeStealActiveBonus = new HashMap<UUID, Map<UUID, Long>>();
    private static final NamespacedKey LIFE_HP_KEY = new NamespacedKey("daggersmp", "life_hearts");
    private static final NamespacedKey VAMPIRE_HP_KEY = new NamespacedKey("daggersmp", "vampire_hearts");
    private static final NamespacedKey LIFE_STEAL_KEY = new NamespacedKey("daggersmp", "life_steal_bonus");
    private static final NamespacedKey SPEED_AS_KEY = new NamespacedKey("daggersmp", "speed_attack_speed");

    public AbilityManager(DaggerSMP plugin) {
        this.plugin = plugin;
        this.startPassiveTasks();
    }

    public void shutdown() {
    }

    private double cfgD(String p, double d) {
        return this.plugin.getConfig().getDouble(p, d);
    }

    private int cfgI(String p, int d) {
        return this.plugin.getConfig().getInt(p, d);
    }

    private long cfgL(String p, long d) {
        return this.plugin.getConfig().getLong(p, d);
    }

    private boolean cfgB(String p, boolean d) {
        return this.plugin.getConfig().getBoolean(p, d);
    }

    private int cfgTicks(String p, double defSec) {
        return (int)Math.round(this.plugin.getConfig().getDouble(p, defSec) * 20.0);
    }

    private void startPassiveTasks() {
        new BukkitRunnable(){

            public void run() {
                for (Player p : AbilityManager.this.plugin.getServer().getOnlinePlayers()) {
                    AbilityManager.this.applyPassives(p);
                    AbilityManager.this.tickMafiaVindicators(p);
                    AbilityManager.this.tickChanceDagger(p);
                    AbilityManager.this.tickChancePassive(p);
                    AbilityManager.this.tickLifeStealBonus(p);
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 20L);
        // Cobweb destroyer: per-tick safety net that breaks any cobweb the player is currently
        // standing in, in case PlayerMoveEvent didn't fire (e.g. webs spawned around them).
        new BukkitRunnable(){

            public void run() {
                for (Player p : AbilityManager.this.plugin.getServer().getOnlinePlayers()) {
                    if (!AbilityManager.this.hasArachnidHeld(p)) continue;
                    Location feet = p.getLocation().getBlock().getLocation();
                    int[][] offsets = new int[][] { {0,0,0}, {0,1,0} };
                    for (int[] o : offsets) {
                        org.bukkit.block.Block b = feet.getWorld().getBlockAt(feet.getBlockX() + o[0], feet.getBlockY() + o[1], feet.getBlockZ() + o[2]);
                        if (b.getType() == Material.COBWEB) {
                            b.breakNaturally();
                        }
                    }
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 2L);
        new BukkitRunnable(){

            public void run() {
                for (Player p : AbilityManager.this.plugin.getServer().getOnlinePlayers()) {
                    if (!AbilityManager.this.hasArachnidHeld(p) || !AbilityManager.this.isAgainstWall(p.getLocation())) continue;
                    if (p.isSneaking() || (p.getVelocity().getY() > 0.05 && !p.isOnGround())) {
                        // Slower wall-climb (~0.22 instead of vanilla-jump 0.42) so it feels like climbing,
                        // not jumping straight up.
                        double y = AbilityManager.this.cfgD("daggers.arachnid.passive.wallclimb-y-velocity", 0.22);
                        Vector v = p.getVelocity();
                        p.setVelocity(new Vector(v.getX() * 0.2, y, v.getZ() * 0.2));
                        p.setFallDistance(0.0f);
                        continue;
                    }
                    if (p.isOnGround() || !(p.getVelocity().getY() < 0.0)) continue;
                    Vector v = p.getVelocity();
                    p.setVelocity(new Vector(v.getX(), Math.max(v.getY(), -0.08), v.getZ()));
                    p.setFallDistance(0.0f);
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L);
    }

    private boolean isAgainstWall(Location loc) {
        int[][] dirs;
        for (int[] d : dirs = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            if (loc.clone().add((double)d[0], 0.0, (double)d[1]).getBlock().getType().isSolid()) {
                return true;
            }
            if (!loc.clone().add((double)d[0], 1.0, (double)d[1]).getBlock().getType().isSolid()) continue;
            return true;
        }
        return false;
    }

    public boolean isInCobweb(Player p) {
        Block at = p.getLocation().getBlock();
        Block above = p.getLocation().add(0.0, 1.0, 0.0).getBlock();
        return at.getType() == Material.COBWEB || above.getType() == Material.COBWEB;
    }

    public boolean hasArachnidHeld(Player p) {
        return DaggerType.fromItem(p.getInventory().getItemInMainHand()) == DaggerType.ARACHNID || DaggerType.fromItem(p.getInventory().getItemInOffHand()) == DaggerType.ARACHNID;
    }

    private void addPerm(Player p, PotionEffectType type, int amp) {
        if (!p.hasPotionEffect(type) || p.getPotionEffect(type).getAmplifier() < amp || p.getPotionEffect(type).getDuration() < 40) {
            p.addPotionEffect(new PotionEffect(type, 60, amp, true, false, false));
        }
    }

    private void applyPassives(Player p) {
        if (this.hasDaggerAnywhere(p, DaggerType.STRENGTH)) {
            this.addPerm(p, PotionEffectType.STRENGTH, this.cfgI("daggers.strength.passive.strength-amplifier", 0));
        }
        if (this.hasDaggerAnywhere(p, DaggerType.SPEED)) {
            this.addPerm(p, PotionEffectType.SPEED, this.cfgI("daggers.speed.passive.speed-amplifier", 2));
        }
        if (this.hasDaggerAnywhere(p, DaggerType.WIND)) {
            this.addPerm(p, PotionEffectType.SPEED, this.cfgI("daggers.wind.passive.speed-amplifier", 0));
        }
        this.applyLifeMaxHealthBonus(p, this.hasDaggerAnywhere(p, DaggerType.LIFE));
        this.applyVampireMaxHealthBonus(p, this.hasDaggerAnywhere(p, DaggerType.VAMPIRE));
        if (this.hasDaggerAnywhere(p, DaggerType.CRIMSON)) {
            this.addPerm(p, PotionEffectType.FIRE_RESISTANCE, this.cfgI("daggers.crimson.passive.fire-resistance-amplifier", 0));
            this.addPerm(p, PotionEffectType.HASTE, this.cfgI("daggers.crimson.passive.haste-amplifier", 0));
        }
        if (this.hasDaggerAnywhere(p, DaggerType.DARKNESS)) {
            this.addPerm(p, PotionEffectType.STRENGTH, this.cfgI("daggers.darkness.passive.strength-amplifier", 0));
            this.addPerm(p, PotionEffectType.SLOWNESS, this.cfgI("daggers.darkness.passive.slowness-amplifier", 0));
        }
        if (this.hasDaggerAnywhere(p, DaggerType.HACK)) {
            this.addPerm(p, PotionEffectType.HASTE, this.cfgI("daggers.hack.passive.haste-amplifier", 0));
            this.addPerm(p, PotionEffectType.SPEED, this.cfgI("daggers.hack.passive.speed-amplifier", 0));
        }
        if (this.hasDaggerAnywhere(p, DaggerType.MAFIA)) {
            this.addPerm(p, PotionEffectType.INVISIBILITY, this.cfgI("daggers.mafia.passive.invisibility-amplifier", 0));
        }
        if (this.hasDaggerAnywhere(p, DaggerType.PIRATE)) {
            this.addPerm(p, PotionEffectType.WATER_BREATHING, this.cfgI("daggers.pirate.passive.water-breathing-amplifier", 0));
        }
        boolean hasVoid = this.hasDaggerAnywhere(p, DaggerType.VOID);
        if (hasVoid) {
            this.addPerm(p, PotionEffectType.SPEED, this.cfgI("daggers.void.passive.speed-amplifier", 1));
            this.addPerm(p, PotionEffectType.STRENGTH, this.cfgI("daggers.void.passive.strength-amplifier", 0));
        } else if (p.hasMetadata("dagger_void_had_passive")) {
            p.removePotionEffect(PotionEffectType.SPEED);
            p.removePotionEffect(PotionEffectType.STRENGTH);
            p.removeMetadata("dagger_void_had_passive", (Plugin)this.plugin);
            if (p.hasMetadata("dagger_void_passive_cd")) p.removeMetadata("dagger_void_passive_cd", (Plugin)this.plugin);
        }
        if (hasVoid && !p.hasMetadata("dagger_void_had_passive")) {
            p.setMetadata("dagger_void_had_passive", (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)true));
        }
        if (this.hasDaggerAnywhere(p, DaggerType.LUCKY)) {
            this.addPerm(p, PotionEffectType.HERO_OF_THE_VILLAGE, this.cfgI("daggers.lucky.passive.hero-amplifier", 0));
            this.addPerm(p, PotionEffectType.LUCK, this.cfgI("daggers.lucky.passive.luck-amplifier", 2));
        }
        if (this.hasDaggerAnywhere(p, DaggerType.JUNGLE)) {
            this.addPerm(p, PotionEffectType.RESISTANCE, this.cfgI("daggers.jungle.passive.resistance-amplifier", 0));
        }
        if (this.hasDaggerAnywhere(p, DaggerType.EARTH)) {
            this.addPerm(p, PotionEffectType.HASTE, this.cfgI("daggers.earth.passive.haste-amplifier", 1));
        }
        if (this.hasDaggerAnywhere(p, DaggerType.TITAN)) {
            this.addPerm(p, PotionEffectType.RESISTANCE, this.cfgI("daggers.titan.passive.resistance-amplifier", 0));
        }
        if (this.hasDaggerAnywhere(p, DaggerType.STORM)) {
            this.addPerm(p, PotionEffectType.SPEED, this.cfgI("daggers.storm.passive.speed-amplifier", 0));
        }
    }

    private void applyLifeMaxHealthBonus(Player p, boolean shouldHave) {
        AttributeInstance attr = p.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) {
            return;
        }
        AttributeModifier existing = null;
        for (AttributeModifier m : attr.getModifiers()) {
            if (!m.getKey().equals((Object)LIFE_HP_KEY)) continue;
            existing = m;
            break;
        }
        double bonus = this.cfgD("daggers.life.passive.max-health-bonus", 10.0);
        if (shouldHave) {
            if (existing == null) {
                attr.addModifier(new AttributeModifier(LIFE_HP_KEY, bonus, AttributeModifier.Operation.ADD_NUMBER));
            } else if (Math.abs(existing.getAmount() - bonus) > 0.01) {
                attr.removeModifier(existing);
                attr.addModifier(new AttributeModifier(LIFE_HP_KEY, bonus, AttributeModifier.Operation.ADD_NUMBER));
            }
        } else if (existing != null) {
            attr.removeModifier(existing);
            if (p.getHealth() > attr.getValue()) {
                p.setHealth(attr.getValue());
            }
        }
    }

    private void applyVampireMaxHealthBonus(Player p, boolean shouldHave) {
        AttributeInstance attr = p.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) {
            return;
        }
        AttributeModifier existing = null;
        for (AttributeModifier m : attr.getModifiers()) {
            if (!m.getKey().equals(VAMPIRE_HP_KEY)) continue;
            existing = m;
            break;
        }
        double bonus = this.cfgD("daggers.vampire.passive.max-health-bonus", 4.0);
        if (shouldHave) {
            if (existing == null) {
                attr.addModifier(new AttributeModifier(VAMPIRE_HP_KEY, bonus, AttributeModifier.Operation.ADD_NUMBER));
            } else if (Math.abs(existing.getAmount() - bonus) > 0.01) {
                attr.removeModifier(existing);
                attr.addModifier(new AttributeModifier(VAMPIRE_HP_KEY, bonus, AttributeModifier.Operation.ADD_NUMBER));
            }
        } else if (existing != null) {
            attr.removeModifier(existing);
            if (p.getHealth() > attr.getValue()) {
                p.setHealth(attr.getValue());
            }
        }
    }

    private void tickMafiaVindicators(Player player) {
        UUID uuid = player.getUniqueId();
        Set<Entity> vinds = this.mafiaVindicators.get(uuid);
        if (vinds == null || vinds.isEmpty()) {
            return;
        }
        vinds.removeIf(ex -> !ex.isValid() || ex.isDead());
        if (vinds.isEmpty()) {
            this.mafiaVindicators.remove(uuid);
            return;
        }
        double idleTp = this.cfgD("daggers.mafia.ability2.teleport-distance", 16.0);
        double combatTp = this.cfgD("daggers.mafia.ability2.combat-teleport-distance", 24.0);
        for (Entity e : vinds) {
            Player tp;
            if (!(e instanceof Vindicator)) continue;
            Vindicator v = (Vindicator)e;
            LivingEntity tgt = v.getTarget();
            if (tgt instanceof Player && !this.isPlayerHostileToOwner(tp = (Player)tgt, player)) {
                v.setTarget(null);
                tgt = null;
            }
            boolean inCombat = tgt != null;
            double dist = v.getLocation().distance(player.getLocation());
            double d = inCombat ? combatTp : idleTp;
            double maxDist = d;
            if (!(dist > maxDist)) continue;
            v.teleport(player.getLocation().add((Math.random() - 0.5) * 3.0, 0.0, (Math.random() - 0.5) * 3.0));
        }
    }

    private boolean isPlayerHostileToOwner(Player suspect, Player owner) {
        return suspect.hasMetadata("dagger_mafia_threat_" + String.valueOf(owner.getUniqueId()));
    }

    public void notifyMafiaThreat(final Player owner, final Player attacker) {
        attacker.setMetadata("dagger_mafia_threat_" + String.valueOf(owner.getUniqueId()), (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)true));
        new BukkitRunnable(){

            public void run() {
                if (attacker.isValid()) {
                    attacker.removeMetadata("dagger_mafia_threat_" + String.valueOf(owner.getUniqueId()), (Plugin)AbilityManager.this.plugin);
                }
            }
        }.runTaskLater((Plugin)this.plugin, 200L);
        Set<Entity> vinds = this.mafiaVindicators.get(owner.getUniqueId());
        if (vinds != null) {
            for (Entity e : vinds) {
                Vindicator v;
                if (!(e instanceof Vindicator) || !(v = (Vindicator)e).isValid()) continue;
                v.setTarget((LivingEntity)attacker);
            }
        }
    }

    public void notifyMafiaTargetMob(Player owner, LivingEntity mob) {
        Set<Entity> vinds = this.mafiaVindicators.get(owner.getUniqueId());
        if (vinds == null) {
            return;
        }
        for (Entity e : vinds) {
            Vindicator v;
            if (!(e instanceof Vindicator) || !(v = (Vindicator)e).isValid() || v.getTarget() != null) continue;
            v.setTarget(mob);
        }
    }

    private void tickChanceDagger(Player player) {
        UUID uuid = player.getUniqueId();
        if (this.chanceActiveDagger.containsKey(uuid) && System.currentTimeMillis() > this.chanceEndTime.getOrDefault(uuid, 0L)) {
            this.chanceActiveDagger.remove(uuid);
            this.chanceEndTime.remove(uuid);
        }
    }

    private void tickChancePassive(Player p) {
        if (!this.hasDaggerAnywhere(p, DaggerType.CHANCE)) {
            return;
        }
        UUID uuid = p.getUniqueId();
        long intervalMs = (long)(this.cfgD("daggers.chance.passive.interval-seconds", 30.0) * 1000.0);
        long next = this.chancePassiveNextRoll.getOrDefault(uuid, 0L);
        long now = System.currentTimeMillis();
        if (now < next) {
            return;
        }
        this.chancePassiveNextRoll.put(uuid, now + intervalMs);
        int durTicks = this.cfgTicks("daggers.chance.passive.effect-duration-seconds", 10.0);
        int amp = this.cfgI("daggers.chance.passive.effect-amplifier", 1);
        PotionEffectType[] pool = new PotionEffectType[]{PotionEffectType.SPEED, PotionEffectType.STRENGTH, PotionEffectType.RESISTANCE, PotionEffectType.REGENERATION, PotionEffectType.HASTE, PotionEffectType.FIRE_RESISTANCE, PotionEffectType.NIGHT_VISION, PotionEffectType.ABSORPTION, PotionEffectType.HEALTH_BOOST};
        PotionEffectType chosen = pool[this.random.nextInt(pool.length)];
        p.addPotionEffect(new PotionEffect(chosen, durTicks, amp));
    }

    private void tickLifeStealBonus(Player p) {
        UUID uuid = p.getUniqueId();
        Map<UUID, Long> sources = this.lifeStealActiveBonus.get(uuid);
        if (sources == null || sources.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        boolean changed = sources.values().removeIf(t -> t <= now);
        if (changed) {
            this.updateLifeStealAttribute(p);
        }
    }

    private void updateLifeStealAttribute(Player p) {
        Map<UUID, Long> srcs;
        AttributeInstance attr = p.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) {
            return;
        }
        AttributeModifier existing = null;
        for (AttributeModifier m : attr.getModifiers()) {
            if (!m.getKey().equals((Object)LIFE_STEAL_KEY)) continue;
            existing = m;
            break;
        }
        if (existing != null) {
            attr.removeModifier(existing);
        }
        if ((srcs = this.lifeStealActiveBonus.get(p.getUniqueId())) != null && !srcs.isEmpty()) {
            double per = this.cfgD("daggers.life.ability1.bonus-hearts-per-player", 6.0);
            attr.addModifier(new AttributeModifier(LIFE_STEAL_KEY, per * (double)srcs.size(), AttributeModifier.Operation.ADD_NUMBER));
        } else if (p.getHealth() > attr.getValue()) {
            p.setHealth(attr.getValue());
        }
    }

    public void tryActivate(Player p, DaggerType t, int n) {
        boolean defer;
        CooldownManager cm = this.plugin.getCooldownManager();
        if (cm.isOnCooldown(p.getUniqueId(), t, n)) {
            long rem = cm.getRemainingSeconds(p.getUniqueId(), t, n);
            return;
        }
        boolean bl = defer = t == DaggerType.VOID && n == 2;
        if (!defer) {
            cm.setCooldown(p.getUniqueId(), t, n);
        }
        if (n == 1) {
            this.useAbility1(p, t);
        } else {
            this.useAbility2(p, t);
        }
    }

    public void useAbility1(Player p, DaggerType t) {
        switch (t) {
            case WIND: {
                this.windAbility1(p);
                break;
            }
            case LIFE: {
                this.lifeAbility1(p);
                break;
            }
            case STRENGTH: {
                this.strengthAbility1(p);
                break;
            }
            case SPEED: {
                this.speedAbility1(p);
                break;
            }
            case CRIMSON: {
                this.crimsonAbility1(p);
                break;
            }
            case DARKNESS: {
                this.darknessAbility1(p);
                break;
            }
            case HACK: {
                this.hackAbility1(p);
                break;
            }
            case FROST: {
                this.frostAbility1(p);
                break;
            }
            case MAFIA: {
                this.mafiaAbility1(p);
                break;
            }
            case PIRATE: {
                this.pirateAbility1(p);
                break;
            }
            case VOID: {
                this.voidAbility1(p);
                break;
            }
            case LUCKY: {
                this.luckyAbility1(p);
                break;
            }
            case MIRROR: {
                this.mirrorAbility1(p);
                break;
            }
            case JUNGLE: {
                this.jungleAbility1(p);
                break;
            }
            case MIDAS: {
                this.midasAbility1(p);
                break;
            }
            case TOXIC: {
                this.toxicAbility1(p);
                break;
            }
            case ARACHNID: {
                this.arachnidAbility1(p);
                break;
            }
            case VAMPIRE: {
                this.vampireAbility1(p);
                break;
            }
            case GRAVITY: {
                this.gravityAbility1(p);
                break;
            }
            case EARTH: {
                this.earthAbility1(p);
                break;
            }
            case TITAN: {
                this.titanAbility1(p);
                break;
            }
            case GUARDIAN: {
                this.guardianAbility1(p);
                break;
            }
            case GHOST: {
                this.ghostAbility1(p);
                break;
            }
            case CHANCE: {
                this.chanceAbility1(p);
                break;
            }
            case STORM: {
                this.stormAbility1(p);
                break;
            }
            default: {
            }
        }
    }

    public void useAbility2(Player p, DaggerType t) {
        switch (t) {
            case WIND: {
                this.windAbility2(p);
                break;
            }
            case LIFE: {
                this.lifeAbility2(p);
                break;
            }
            case STRENGTH: {
                this.strengthAbility2(p);
                break;
            }
            case SPEED: {
                this.speedAbility2(p);
                break;
            }
            case CRIMSON: {
                this.crimsonAbility2(p);
                break;
            }
            case DARKNESS: {
                this.darknessAbility2(p);
                break;
            }
            case HACK: {
                this.hackAbility2(p);
                break;
            }
            case FROST: {
                this.frostAbility2(p);
                break;
            }
            case MAFIA: {
                this.mafiaAbility2(p);
                break;
            }
            case PIRATE: {
                this.pirateAbility2(p);
                break;
            }
            case VOID: {
                this.voidAbility2(p);
                break;
            }
            case MIRROR: {
                this.mirrorAbility2(p);
                break;
            }
            case JUNGLE: {
                this.jungleAbility2(p);
                break;
            }
            case MIDAS: {
                this.midasAbility2(p);
                break;
            }
            case TOXIC: {
                this.toxicAbility2(p);
                break;
            }
            case GRAVITY: {
                this.gravityAbility2(p);
                break;
            }
            case EARTH: {
                this.earthAbility2(p);
                break;
            }
            case TITAN: {
                this.titanAbility2(p);
                break;
            }
            case GUARDIAN: {
                this.guardianAbility2(p);
                break;
            }
            case GHOST: {
                this.ghostAbility2(p);
                break;
            }
            case STORM: {
                this.stormAbility2(p);
                break;
            }
            default: {
            }
        }
    }

    private void strengthAbility1(Player p) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, this.cfgTicks("daggers.strength.ability1.duration-seconds", 3.0), this.cfgI("daggers.strength.ability1.amplifier", 2)));
        // === POWER SURGE: red energy burst + pulsing aura ===
        Location loc = p.getLocation();
        for (int i = 0; i < 40; i++) {
            double a = i * Math.PI * 2.0 / 40.0;
            double r = 0.7 + this.random.nextDouble() * 0.3;
            Location pt = loc.clone().add(Math.cos(a) * r, 0.8 + this.random.nextDouble(), Math.sin(a) * r);
            loc.getWorld().spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, 0,
                (Object) new Particle.DustOptions(Color.fromRGB(220, 30, 30), 1.8f));
        }
        loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 1, 0), 30, 0.4, 0.6, 0.4, 0,
            (Object) new Particle.DustOptions(Color.fromRGB(255, 80, 0), 2.0f));
        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc.clone().add(0, 0.5, 0), 5, 0.3, 0.1, 0.3, 0.0);
    }

    private void strengthAbility2(Player p) {
        p.setMetadata("dagger_strength_armor_break", (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)true));
        // === ARMOR CRACK: dark fracture particles + ominous shimmer ===
        Location loc = p.getLocation();
        loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 1.2, 0), 30, 0.35, 0.5, 0.35, 0,
            (Object) new Particle.DustOptions(Color.fromRGB(140, 0, 0), 1.6f));
        for (int i = 0; i < 24; i++) {
            double a = i * Math.PI * 2.0 / 24.0;
            Location pt = loc.clone().add(Math.cos(a) * 0.6, 1.0 + this.random.nextDouble() * 0.8, Math.sin(a) * 0.6);
            loc.getWorld().spawnParticle(Particle.SQUID_INK, pt, 1, 0.05, 0.1, 0.05, 0.0);
        }
        loc.getWorld().spawnParticle(Particle.CRIT, loc.clone().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.1);
    }

    private void speedAbility1(Player p) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, this.cfgTicks("daggers.speed.ability1.duration-seconds", 7.0), this.cfgI("daggers.speed.ability1.amplifier", 4)));
        // === SPEED BURST: cyan motion-blur streak + ring ===
        Location loc = p.getLocation();
        Vector fwd = p.getLocation().getDirection().setY(0).normalize();
        for (int i = 0; i < 36; i++) {
            double a = i * Math.PI * 2.0 / 36.0;
            Location pt = loc.clone().add(Math.cos(a) * 0.75, 0.6 + this.random.nextDouble() * 1.0, Math.sin(a) * 0.75);
            loc.getWorld().spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, 0,
                (Object) new Particle.DustOptions(Color.fromRGB(100, 220, 255), 1.5f));
        }
        loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, 1, 0), 10, 0.3, 0.4, 0.3, 0.08);
        // Speed lines in facing direction
        for (int k = 1; k <= 8; k++) {
            Location streak = loc.clone().add(fwd.clone().multiply(-k * 0.5)).add(0, 1.0 + (this.random.nextDouble() - 0.5) * 0.6, 0);
            streak.getWorld().spawnParticle(Particle.DUST, streak, 1, 0.05, 0.15, 0.05, 0,
                (Object) new Particle.DustOptions(Color.fromRGB(140, 240, 255), 1.1f));
        }
    }

    private void speedAbility2(final Player p) {
        final AttributeInstance attr = p.getAttribute(Attribute.ATTACK_SPEED);
        if (attr == null) {
            return;
        }
        for (AttributeModifier m : new ArrayList<AttributeModifier>(attr.getModifiers())) {
            if (!m.getKey().equals((Object)SPEED_AS_KEY)) continue;
            attr.removeModifier(m);
        }
        double base = attr.getBaseValue();
        double mult = this.cfgD("daggers.speed.ability2.attack-speed-bonus", 0.5);
        attr.addModifier(new AttributeModifier(SPEED_AS_KEY, base * mult, AttributeModifier.Operation.ADD_NUMBER));
        long ticks = this.cfgTicks("daggers.speed.ability2.duration-seconds", 7.0);
        // === ATTACK FRENZY: yellow strike sparks spinning around hands ===
        Location loc = p.getLocation();
        for (int i = 0; i < 32; i++) {
            double a = i * Math.PI * 2.0 / 32.0;
            Location pt = loc.clone().add(Math.cos(a) * 0.6, 1.3 + this.random.nextDouble() * 0.4, Math.sin(a) * 0.6);
            loc.getWorld().spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, 0,
                (Object) new Particle.DustOptions(Color.fromRGB(255, 220, 50), 1.4f));
        }
        loc.getWorld().spawnParticle(Particle.CRIT, loc.clone().add(0, 1.2, 0), 24, 0.25, 0.4, 0.25, 0.15);
        loc.getWorld().spawnParticle(Particle.FLASH, loc.clone().add(0, 1.2, 0), 1, 0, 0, 0, 0);
        new BukkitRunnable(){
            public void run() {
                if (!p.isOnline()) {
                    return;
                }
                for (AttributeModifier m : new ArrayList<AttributeModifier>(attr.getModifiers())) {
                    if (!m.getKey().equals((Object)SPEED_AS_KEY)) continue;
                    attr.removeModifier(m);
                }
            }
        }.runTaskLater((Plugin)this.plugin, ticks);
    }

    private void windAbility1(final Player p) {
        double dist = this.cfgD("daggers.wind.ability1.dash-blocks", 20.0);
        Vector dir = p.getLocation().getDirection().normalize().multiply(Math.max(1.5, dist * 0.12));
        p.setVelocity(dir);
        this.windFallDamageImmune.add(p.getUniqueId());
        p.playSound(p.getLocation(), "daggersmp:ability.wind.dash", 1.0f, 1.5f);
        // Dash burst: ring of wind particles at launch point
        Location launchLoc = p.getLocation();
        for (int i = 0; i < 24; i++) {
            double a = i * Math.PI * 2.0 / 24.0;
            double rx = Math.cos(a) * 0.8;
            double rz = Math.sin(a) * 0.8;
            launchLoc.getWorld().spawnParticle(Particle.CLOUD, launchLoc.clone().add(rx, 0.5, rz), 2, 0.05, 0.1, 0.05, 0.04);
            launchLoc.getWorld().spawnParticle(
                Particle.DUST, launchLoc.clone().add(rx, 0.5, rz), 1, 0, 0, 0, 0,
                (Object) new Particle.DustOptions(Color.fromRGB(200, 240, 255), 1.4f)
            );
        }
        // Particle trail during the dash
        new BukkitRunnable(){
            int t = 0;
            Location prevLoc = p.getLocation().clone();
            public void run() {
                if (++t > 20 || !p.isOnline()) { this.cancel(); return; }
                Location cur = p.getLocation();
                for (int i = 0; i < 3; i++) {
                    double frac = i / 3.0;
                    double tx = prevLoc.getX() + (cur.getX() - prevLoc.getX()) * frac;
                    double ty = prevLoc.getY() + (cur.getY() - prevLoc.getY()) * frac + 1.0;
                    double tz = prevLoc.getZ() + (cur.getZ() - prevLoc.getZ()) * frac;
                    Location tpt = new Location(cur.getWorld(), tx, ty, tz);
                    cur.getWorld().spawnParticle(Particle.CLOUD, tpt, 3, 0.15, 0.15, 0.15, 0.04);
                    cur.getWorld().spawnParticle(
                        Particle.DUST, tpt, 1, 0.1, 0.1, 0.1, 0,
                        (Object) new Particle.DustOptions(Color.fromRGB(210, 245, 255), 1.2f)
                    );
                }
                prevLoc = cur.clone();
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L);
        new BukkitRunnable(){
            public void run() {
                AbilityManager.this.windFallDamageImmune.remove(p.getUniqueId());
            }
        }.runTaskLater((Plugin)this.plugin, (long)this.cfgTicks("daggers.wind.ability1.fall-immune-seconds", 10.0));
    }

    private void windAbility2(final Player p) {
        this.windFallDamageImmune.add(p.getUniqueId());
        double upward = this.cfgD("daggers.wind.ability2.launch-upward", 2.6);
        Vector dir = p.getLocation().getDirection();
        p.setVelocity(new Vector(dir.getX() * 0.6, upward, dir.getZ() * 0.6));
        p.playSound(p.getLocation(), "daggersmp:ability.wind.leap", 1.0f, 1.5f);
        // === LAUNCH BURST: updraft of cloud/ice particles ===
        Location launchLoc = p.getLocation().clone();
        for (int i = 0; i < 36; i++) {
            double a = i * Math.PI * 2.0 / 36.0;
            double r = 0.5 + this.random.nextDouble() * 0.5;
            Location pt = launchLoc.clone().add(Math.cos(a) * r, this.random.nextDouble() * 0.5, Math.sin(a) * r);
            pt.getWorld().spawnParticle(Particle.CLOUD, pt, 2, 0.05, 0.1, 0.05, 0.06);
            pt.getWorld().spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, 0,
                (Object) new Particle.DustOptions(Color.fromRGB(200, 240, 255), 1.3f));
        }
        final UUID uuid = p.getUniqueId();
        new BukkitRunnable(){
            int ticks = 0;
            boolean wasAir = false;

            public void run() {
                ++this.ticks;
                if (this.ticks > 200 || !p.isOnline()) {
                    AbilityManager.this.windFallDamageImmune.remove(uuid);
                    this.cancel();
                    return;
                }
                // Air trail during ascent
                if (this.ticks <= 20 && !p.isOnGround()) {
                    p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation().clone().add(0, 0.5, 0), 3, 0.2, 0.2, 0.2, 0.03);
                    p.getWorld().spawnParticle(Particle.DUST, p.getLocation().clone().add(0, 1, 0), 1, 0.1, 0.2, 0.1, 0,
                        (Object) new Particle.DustOptions(Color.fromRGB(180, 235, 255), 1.0f));
                }
                if (!p.isOnGround() && this.ticks > 5) {
                    this.wasAir = true;
                }
                if (this.wasAir && p.isOnGround()) {
                    AbilityManager.this.windFallDamageImmune.remove(uuid);
                    double dmg = AbilityManager.this.cfgD("daggers.wind.ability2.shockwave-damage", 6.0);
                    double radius = AbilityManager.this.cfgD("daggers.wind.ability2.shockwave-radius", 5.0);
                    Location loc = p.getLocation();
                    // Landing shockwave ring
                    for (int i = 0; i < 48; i++) {
                        double a = i * Math.PI * 2.0 / 48.0;
                        Location rpt = loc.clone().add(Math.cos(a) * radius * 0.9, 0.2, Math.sin(a) * radius * 0.9);
                        rpt.getWorld().spawnParticle(Particle.CLOUD, rpt, 2, 0.1, 0.1, 0.1, 0.04);
                        rpt.getWorld().spawnParticle(Particle.DUST, rpt, 1, 0, 0, 0, 0,
                            (Object) new Particle.DustOptions(Color.fromRGB(190, 240, 255), 1.4f));
                    }
                    loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 30, radius * 0.4, 0.5, radius * 0.4, 0.0);
                    loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 3, 0.5, 0.1, 0.5, 0.0);
                    loc.getWorld().playSound(loc, "daggersmp:ability.wind.leap", 1.0f, 1.5f);
                    for (Entity e : p.getNearbyEntities(radius, 3.0, radius)) {
                        LivingEntity le;
                        if (!(e instanceof LivingEntity) || (le = (LivingEntity)e) == p || AbilityManager.this.isTrustedEntity(p, e)) continue;
                        le.damage(dmg, (Entity)p);
                    }
                    this.cancel();
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L);
    }

    public boolean isWindFallImmune(UUID uuid) {
        return this.windFallDamageImmune.contains(uuid);
    }

    private void lifeAbility1(Player p) {
        double hearts = this.cfgD("daggers.life.ability1.steal-hearts", 3.0);
        double radius = this.cfgD("daggers.life.ability1.radius", 5.0);
        long durMs = (long)(this.cfgD("daggers.life.ability1.extra-health-duration-seconds", 10.0) * 1000.0);
        double dmg = hearts * 2.0;
        int hits = 0;
        UUID uuid = p.getUniqueId();
        Map sources = this.lifeStealActiveBonus.computeIfAbsent(uuid, k -> new HashMap());
        for (Entity e : p.getNearbyEntities(radius, radius, radius)) {
            if (!(e instanceof Player)) continue;
            Player tp = (Player)e;
            if (this.isTrustedEntity(p, e)) continue;
            tp.setNoDamageTicks(0);
            double before = tp.getHealth();
            tp.damage(dmg, (Entity)p);
            if (tp.getHealth() >= before - 0.01) {
                tp.setHealth(Math.max(0.0, before - dmg));
            }
            // Hearts burst from victim
            tp.getWorld().spawnParticle(Particle.HEART, tp.getLocation().clone().add(0.0, 1.5, 0.0), 18, 0.35, 0.35, 0.35, 0.0);
            // Red drain beam — particles flowing from victim to caster
            Vector toward = p.getLocation().toVector().subtract(tp.getLocation().toVector());
            double dist = toward.length();
            if (dist > 0.5) {
                Vector unit = toward.clone().normalize();
                for (double d = 0.3; d <= dist; d += 0.4) {
                    Location sp = tp.getLocation().clone().add(0, 1.4, 0).add(unit.clone().multiply(d));
                    sp.getWorld().spawnParticle(Particle.DUST, sp, 1, 0.04, 0.04, 0.04, 0,
                        (Object) new Particle.DustOptions(Color.fromRGB(255, 30, 30), 1.0f));
                }
            }
            sources.put(tp.getUniqueId(), System.currentTimeMillis() + durMs);
            ++hits;
        }
        if (hits == 0) {
            return;
        }
        this.updateLifeStealAttribute(p);
        AttributeInstance attr = p.getAttribute(Attribute.MAX_HEALTH);
        if (attr != null) {
            p.setHealth(Math.min(attr.getValue(), p.getHealth() + this.cfgD("daggers.life.ability1.bonus-hearts-per-player", 6.0)));
        }
        // Caster absorbs health — radiant green/pink burst
        p.getWorld().spawnParticle(Particle.HEART, p.getLocation().clone().add(0, 2.2, 0), 12, 0.4, 0.3, 0.4, 0.02);
        p.getWorld().spawnParticle(Particle.DUST, p.getLocation().clone().add(0, 1, 0), 28, 0.4, 0.6, 0.4, 0,
            (Object) new Particle.DustOptions(Color.fromRGB(255, 60, 80), 1.8f));
    }

    private void lifeAbility2(Player p) {
        int dur = this.cfgTicks("daggers.life.ability2.duration-seconds", 5.0);
        int amp = this.cfgI("daggers.life.ability2.regen-amplifier", 1);
        double radius = this.cfgD("daggers.life.ability2.radius", 10.0);
        // === HEALING PULSE: green expanding ring ===
        new BukkitRunnable() {
            int t = 0;
            public void run() {
                if (++t > 16) { this.cancel(); return; }
                double waveR = radius * t / 16.0;
                Location loc = p.getLocation();
                for (int i = 0; i < 48; i++) {
                    double a = i * Math.PI * 2.0 / 48.0;
                    Location pt = loc.clone().add(Math.cos(a) * waveR, 0.3, Math.sin(a) * waveR);
                    pt.getWorld().spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, 0,
                        (Object) new Particle.DustOptions(Color.fromRGB(80, 220, 80), 1.6f));
                    if (i % 6 == 0) pt.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, pt, 1, 0.05, 0.1, 0.05, 0.0);
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L);
        p.getWorld().spawnParticle(Particle.HEART, p.getLocation().clone().add(0, 2, 0), 8, 0.4, 0.2, 0.4, 0.0);
        for (Entity e : p.getNearbyEntities(radius, radius, radius)) {
            Player ally;
            if (!(e instanceof Player) || (ally = (Player)e) == p || !this.plugin.getTrustManager().isTrusted(p.getUniqueId(), ally.getUniqueId())) continue;
            ally.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, dur, amp));
            ally.getWorld().spawnParticle(Particle.HEART, ally.getLocation().clone().add(0, 2, 0), 6, 0.3, 0.2, 0.3, 0.0);
        }
        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, dur, amp));
    }

    private void crimsonAbility1(Player p) {
        p.setMetadata("dagger_crimson_wither_next", (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)true));
        // === WITHER CURSE: dark necrotic aura charge ===
        Location loc = p.getLocation();
        for (int i = 0; i < 36; i++) {
            double a = i * Math.PI * 2.0 / 36.0;
            double r = 0.7 + this.random.nextDouble() * 0.3;
            Location pt = loc.clone().add(Math.cos(a) * r, 0.9 + this.random.nextDouble() * 0.8, Math.sin(a) * r);
            pt.getWorld().spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, 0,
                (Object) new Particle.DustOptions(Color.fromRGB(40, 0, 0), 1.5f));
        }
        loc.getWorld().spawnParticle(Particle.SQUID_INK, loc.clone().add(0, 1.2, 0), 14, 0.3, 0.5, 0.3, 0.02);
        loc.getWorld().spawnParticle(Particle.WITCH, loc.clone().add(0, 2.0, 0), 10, 0.3, 0.3, 0.3, 0.05);
    }

    private void crimsonAbility2(Player p) {
        Location loc = p.getEyeLocation();
        Vector dir = loc.getDirection().normalize();
        // === FIREBALL LAUNCH: muzzle flame burst ===
        p.getWorld().spawnParticle(Particle.FLAME, loc.clone(), 20, 0.2, 0.2, 0.2, 0.08);
        p.getWorld().spawnParticle(Particle.DUST, loc.clone(), 10, 0.15, 0.15, 0.15, 0,
            (Object) new Particle.DustOptions(Color.fromRGB(255, 100, 0), 1.8f));
        p.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc.clone(), 5, 0.1, 0.1, 0.1, 0.04);
        Fireball fb = (Fireball)p.getWorld().spawn(loc.add(dir), Fireball.class);
        fb.setDirection(dir.multiply(2));
        fb.setShooter((ProjectileSource)p);
        fb.setYield((float)this.cfgD("daggers.crimson.ability2.explosion-power", 2.0));
        fb.setMetadata("dagger_crimson_pierce_fireres", (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)p.getUniqueId().toString()));
        fb.setMetadata("dagger_crimson_damage", (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)this.cfgD("daggers.crimson.ability2.damage", 6.0)));
    }

    private void darknessAbility1(Player p) {
        double radius = this.cfgD("daggers.darkness.ability1.radius", 5.0);
        int dur = this.cfgTicks("daggers.darkness.ability1.duration-seconds", 5.0);
        int slow = this.cfgI("daggers.darkness.ability1.slowness-amplifier", 2);
        int dark = this.cfgI("daggers.darkness.ability1.darkness-amplifier", 0);
        for (Entity e : p.getNearbyEntities(radius, radius, radius)) {
            if (!(e instanceof Player)) continue;
            Player tp = (Player)e;
            if (this.isTrustedEntity(p, e)) continue;
            tp.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, dur, slow));
            tp.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, dur, dark));
        }

        // === SHADOW PULSE: expanding dark sphere + spiraling tendrils ===
        final double finalRadius = radius;
        new BukkitRunnable() {
            int t = 0;
            public void run() {
                if (++t > 22) { this.cancel(); return; }
                double waveR = finalRadius * t / 22.0;
                Location loc = p.getLocation();
                int steps = 60;
                // Expanding ring at multiple heights
                for (int i = 0; i < steps; i++) {
                    double a = i * Math.PI * 2.0 / steps;
                    for (int yy = 0; yy < 3; yy++) {
                        double y = yy * 0.85;
                        Location pt = loc.clone().add(Math.cos(a) * waveR, y, Math.sin(a) * waveR);
                        pt.getWorld().spawnParticle(Particle.SQUID_INK, pt, 1, 0.02, 0.02, 0.02, 0.0);
                        if (i % 5 == 0) {
                            pt.getWorld().spawnParticle(
                                Particle.DUST, pt, 1, 0, 0, 0, 0,
                                (Object) new Particle.DustOptions(Color.fromRGB(20, 0, 45), 2.0f)
                            );
                        }
                    }
                }
                // Shadow tendrils spiraling inward to caster position
                for (int i = 0; i < 8; i++) {
                    double a = (i * Math.PI * 2.0 / 8.0) + (t * 0.35);
                    double r = waveR * 0.65;
                    double y = 0.5 + (t * 0.06);
                    Location sp = loc.clone().add(Math.cos(a) * r, y, Math.sin(a) * r);
                    sp.getWorld().spawnParticle(Particle.SQUID_INK, sp, 2, 0.06, 0.12, 0.06, 0.0);
                }
                // Dense dark center cloud
                if (t <= 8) {
                    loc.getWorld().spawnParticle(Particle.SQUID_INK, loc.clone().add(0, 1.0, 0), 6, 0.3, 0.4, 0.3, 0.02);
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L);
    }

    private void darknessAbility2(final Player p) {
        final Location start = p.getEyeLocation();
        final Vector dir = start.getDirection().normalize();
        final double dmg = this.cfgD("daggers.darkness.ability2.beam-damage", 6.0);
        final double maxDist = this.cfgD("daggers.darkness.ability2.max-distance", 30.0);
        // Pre-cast muzzle flash at the caster's eye
        p.getWorld().spawnParticle(Particle.SQUID_INK, start.clone().add(0, 0, 0), 12, 0.15, 0.15, 0.15, 0.02);
        p.getWorld().spawnParticle(
            Particle.DUST, start, 8, 0.1, 0.1, 0.1, 0,
            (Object) new Particle.DustOptions(Color.fromRGB(60, 0, 130), 1.8f)
        );
        p.getWorld().playSound(start, "daggersmp:ability.darkness.sonic", 2.0f, 1.0f);
        // Precompute perpendicular vectors for shockwave rings
        final Vector perp1 = (Math.abs(dir.getX()) < 0.9
            ? new Vector(1, 0, 0) : new Vector(0, 1, 0))
            .crossProduct(dir).normalize();
        final Vector perp2 = dir.clone().crossProduct(perp1).normalize();
        new BukkitRunnable(){
            double dist = 0.0;
            final Set<Entity> hit = new HashSet<Entity>();
            int step = 0;
            public void run() {
                for (int i = 0; i < 5; ++i) {
                    this.dist += 0.5;
                    this.step++;
                    if (this.dist > maxDist) {
                        this.cancel();
                        return;
                    }
                    Location pt = start.clone().add(dir.clone().multiply(this.dist));
                    // Core sonic distortion
                    pt.getWorld().spawnParticle(Particle.SONIC_BOOM, pt, 0);
                    // Dense dark ink core — the visible "beam"
                    pt.getWorld().spawnParticle(Particle.SQUID_INK, pt, 2, 0.04, 0.04, 0.04, 0.0);
                    // Purple-violet outer glow halo
                    pt.getWorld().spawnParticle(
                        Particle.DUST, pt, 1, 0.1, 0.1, 0.1, 0,
                        (Object) new Particle.DustOptions(Color.fromRGB(55, 0, 120), 1.6f)
                    );
                    // Perpendicular shockwave rings every ~1 block
                    if (this.step % 4 == 0) {
                        for (int j = 0; j < 14; j++) {
                            double a = j * Math.PI * 2.0 / 14.0;
                            double rr = 0.45 + AbilityManager.this.random.nextDouble() * 0.25;
                            Location rpt = pt.clone().add(
                                perp1.clone().multiply(Math.cos(a) * rr)
                                     .add(perp2.clone().multiply(Math.sin(a) * rr))
                            );
                            rpt.getWorld().spawnParticle(
                                Particle.DUST, rpt, 1, 0, 0, 0, 0,
                                (Object) new Particle.DustOptions(Color.fromRGB(100, 20, 210), 1.0f)
                            );
                        }
                    }
                    for (Entity e : pt.getWorld().getNearbyEntities(pt, 1.0, 1.0, 1.0)) {
                        LivingEntity le;
                        if (!(e instanceof LivingEntity) || (le = (LivingEntity)e) == p || this.hit.contains(e) || AbilityManager.this.isTrustedEntity(p, e)) continue;
                        le.damage(dmg, (Entity)p);
                        this.hit.add(e);
                        // Impact burst on target
                        le.getWorld().spawnParticle(Particle.SQUID_INK, le.getLocation().clone().add(0, 1.1, 0), 18, 0.3, 0.45, 0.3, 0.04);
                        le.getWorld().spawnParticle(
                            Particle.DUST, le.getLocation().clone().add(0, 1.1, 0), 14, 0.3, 0.45, 0.3, 0,
                            (Object) new Particle.DustOptions(Color.fromRGB(80, 0, 170), 1.6f)
                        );
                    }
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L);
    }

    private void hackAbility1(Player p) {
        double radius = this.cfgD("daggers.hack.ability1.reveal-radius", 50.0);
        int dur = this.cfgTicks("daggers.hack.ability1.glow-duration-seconds", 30.0);
        int count = 0;
        for (Entity e : p.getNearbyEntities(radius, radius, radius)) {
            if (!(e instanceof LivingEntity)) continue;
            LivingEntity le = (LivingEntity)e;
            le.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, dur, 0));
            // Green scan pulse on each revealed entity
            le.getWorld().spawnParticle(Particle.DUST, le.getLocation().clone().add(0, 1.0, 0), 12, 0.3, 0.5, 0.3, 0,
                (Object) new Particle.DustOptions(Color.fromRGB(0, 255, 100), 1.4f));
            ++count;
        }
        // Scan wave expanding outward from caster
        final double scanRadius = radius;
        new BukkitRunnable() {
            int t = 0;
            public void run() {
                if (++t > 18) { this.cancel(); return; }
                double waveR = Math.min(scanRadius, t * 3.0);
                Location loc = p.getLocation();
                for (int i = 0; i < 48; i++) {
                    double a = i * Math.PI * 2.0 / 48.0;
                    Location pt = loc.clone().add(Math.cos(a) * waveR, 1.0, Math.sin(a) * waveR);
                    pt.getWorld().spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, 0,
                        (Object) new Particle.DustOptions(Color.fromRGB(0, 200 + AbilityManager.this.random.nextInt(55), 80), 1.2f));
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L);
    }

    private void hackAbility2(final Player p) {
        long ticks = this.cfgTicks("daggers.hack.ability2.hitbox-duration-seconds", 5.0);
        final double bonus = this.cfgD("daggers.hack.ability2.reach-bonus", 3.0);
        final NamespacedKey key = new NamespacedKey("daggersmp", "hack_reach");
        final NamespacedKey key2 = new NamespacedKey("daggersmp", "hack_reach_block");
        AttributeInstance reach = p.getAttribute(Attribute.ENTITY_INTERACTION_RANGE);
        AttributeInstance breach = p.getAttribute(Attribute.BLOCK_INTERACTION_RANGE);
        if (reach != null) {
            for (AttributeModifier m : new ArrayList<AttributeModifier>(reach.getModifiers())) {
                if (m.getKey().equals((Object)key)) reach.removeModifier(m);
            }
            reach.addModifier(new AttributeModifier(key, bonus, AttributeModifier.Operation.ADD_NUMBER));
        }
        if (breach != null) {
            for (AttributeModifier m : new ArrayList<AttributeModifier>(breach.getModifiers())) {
                if (m.getKey().equals((Object)key2)) breach.removeModifier(m);
            }
            breach.addModifier(new AttributeModifier(key2, bonus, AttributeModifier.Operation.ADD_NUMBER));
        }
        p.setMetadata("dagger_hack_hitbox", (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)true));
        // === REACH HACK: electric arc cone in facing direction ===
        Location eye = p.getEyeLocation();
        Vector fwd = eye.getDirection().normalize();
        for (int k = 1; k <= (int)(bonus * 2); k++) {
            Location pt = eye.clone().add(fwd.clone().multiply(k * 0.5));
            pt.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, pt, 3, 0.15, 0.15, 0.15, 0.08);
            pt.getWorld().spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, 0,
                (Object) new Particle.DustOptions(Color.fromRGB(0, 230, 130), 1.2f));
        }
        p.getWorld().spawnParticle(Particle.FLASH, eye.clone().add(fwd.clone().multiply(bonus)), 1, 0, 0, 0, 0);
        new BukkitRunnable(){
            public void run() {
                if (p.isOnline()) {
                    p.removeMetadata("dagger_hack_hitbox", (Plugin)AbilityManager.this.plugin);
                }
                AttributeInstance r = p.getAttribute(Attribute.ENTITY_INTERACTION_RANGE);
                AttributeInstance b = p.getAttribute(Attribute.BLOCK_INTERACTION_RANGE);
                if (r != null) for (AttributeModifier m : new ArrayList<AttributeModifier>(r.getModifiers())) if (m.getKey().equals((Object)key)) r.removeModifier(m);
                if (b != null) for (AttributeModifier m : new ArrayList<AttributeModifier>(b.getModifiers())) if (m.getKey().equals((Object)key2)) b.removeModifier(m);
            }
        }.runTaskLater((Plugin)this.plugin, ticks);
    }

    private void frostAbility1(final Player p) {
        double radius = this.cfgD("daggers.frost.ability1.radius", 5.0);
        final double frozenRadius = radius;
        final int dur = this.cfgTicks("daggers.frost.ability1.freeze-duration-seconds", 5.0);
        final java.util.List<LivingEntity> frozen = new ArrayList<LivingEntity>();
        final java.util.Map<java.util.UUID, Location> anchors = new java.util.HashMap<java.util.UUID, Location>();
        for (Entity e : p.getNearbyEntities(radius, radius, radius)) {
            LivingEntity le;
            if (!(e instanceof LivingEntity) || (le = (LivingEntity)e) == p || this.isTrustedEntity(p, e)) continue;
            le.setFreezeTicks(dur + 60);
            anchors.put(le.getUniqueId(), le.getLocation().clone());
            frozen.add(le);
            le.getWorld().spawnParticle(Particle.SNOWFLAKE, le.getLocation().add(0.0, 1.0, 0.0), 50, 0.3, 0.6, 0.3, 0.02);
            // Ice encasement burst on each frozen entity
            le.getWorld().spawnParticle(
                Particle.DUST, le.getLocation().add(0, 1.0, 0), 20, 0.3, 0.5, 0.3, 0,
                (Object) new Particle.DustOptions(Color.fromRGB(190, 240, 255), 2.0f)
            );
        }
        // Freeze shockwave ring expanding outward
        new BukkitRunnable() {
            int t = 0;
            public void run() {
                if (++t > 20) { this.cancel(); return; }
                double waveR = frozenRadius * t / 20.0;
                Location loc = p.getLocation();
                int wsteps = 60;
                for (int i = 0; i < wsteps; i++) {
                    double a = i * Math.PI * 2.0 / wsteps;
                    Location pt = loc.clone().add(Math.cos(a) * waveR, 0.15, Math.sin(a) * waveR);
                    pt.getWorld().spawnParticle(Particle.SNOWFLAKE, pt, 2, 0.05, 0.15, 0.05, 0.0);
                    if (i % 5 == 0) {
                        pt.getWorld().spawnParticle(
                            Particle.DUST, pt, 1, 0, 0, 0, 0,
                            (Object) new Particle.DustOptions(Color.fromRGB(200, 245, 255), 1.8f)
                        );
                    }
                }
                // Ice crystals rising above frozen entities
                for (LivingEntity le : frozen) {
                    if (!le.isValid()) continue;
                    Location ep = le.getLocation().add(0, 0.4 + t * 0.08, 0);
                    ep.getWorld().spawnParticle(Particle.SNOWFLAKE, ep, 4, 0.2, 0.15, 0.2, 0.015);
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L);
        if (!frozen.isEmpty()) {
            new BukkitRunnable() {
                int t = 0;
                public void run() {
                    if (++t > dur) { this.cancel(); return; }
                    for (LivingEntity le : frozen) {
                        if (!le.isValid() || le.isDead()) continue;
                        Location anc = anchors.get(le.getUniqueId());
                        if (anc == null) continue;
                        le.setVelocity(new Vector(0, 0, 0));
                        Location cur = le.getLocation();
                        if (cur.distanceSquared(anc) > 0.04) {
                            Location back = anc.clone();
                            back.setYaw(cur.getYaw());
                            back.setPitch(cur.getPitch());
                            le.teleport(back);
                        }
                        le.setFreezeTicks(Math.max(le.getFreezeTicks(), 200));
                        // Dense ice aura around frozen entity
                        if (t % 4 == 0) {
                            le.getWorld().spawnParticle(Particle.SNOWFLAKE, le.getLocation().add(0.0, 1.0, 0.0), 12, 0.25, 0.45, 0.25, 0.01);
                            le.getWorld().spawnParticle(
                                Particle.DUST, le.getLocation().add(0.0, 1.0, 0.0), 4, 0.25, 0.4, 0.25, 0,
                                (Object) new Particle.DustOptions(Color.fromRGB(170, 230, 255), 1.2f)
                            );
                        }
                    }
                }
            }.runTaskTimer((Plugin)this.plugin, 0L, 1L);
        }
        p.getWorld().playSound(p.getLocation(), "daggersmp:ability.frost.field", 2.0f, 1.0f);
    }

    private void frostAbility2(Player p) {
        int ticks = this.cfgTicks("daggers.frost.ability2.debuff-duration-seconds", 5.0);
        double radius = this.cfgD("daggers.frost.ability2.radius", 5.0);
        // === FROST DEBUFF: icy burst on caster, cold snap particles ===
        Location casterLoc = p.getLocation();
        for (int i = 0; i < 32; i++) {
            double a = i * Math.PI * 2.0 / 32.0;
            Location pt = casterLoc.clone().add(Math.cos(a) * 0.8, 0.8 + this.random.nextDouble() * 0.8, Math.sin(a) * 0.8);
            pt.getWorld().spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, 0,
                (Object) new Particle.DustOptions(Color.fromRGB(170, 230, 255), 1.4f));
        }
        casterLoc.getWorld().spawnParticle(Particle.SNOWFLAKE, casterLoc.clone().add(0, 1.5, 0), 20, 0.4, 0.4, 0.4, 0.02);
        for (Entity e : p.getNearbyEntities(radius, radius, radius)) {
            if (!(e instanceof Player)) continue;
            Player tp = (Player)e;
            if (this.isTrustedEntity(p, e)) continue;
            tp.setFreezeTicks(ticks);
            tp.setMetadata("dagger_frost_debuff", (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)true));
            // Ice particles on each debuffed target
            tp.getWorld().spawnParticle(Particle.SNOWFLAKE, tp.getLocation().clone().add(0, 1.0, 0), 24, 0.25, 0.5, 0.25, 0.015);
            tp.getWorld().spawnParticle(Particle.DUST, tp.getLocation().clone().add(0, 1.0, 0), 12, 0.25, 0.4, 0.25, 0,
                (Object) new Particle.DustOptions(Color.fromRGB(190, 245, 255), 1.5f));
            final Player ftp = tp;
            new BukkitRunnable(){
                public void run() {
                    if (ftp.isValid()) {
                        ftp.removeMetadata("dagger_frost_debuff", (Plugin)AbilityManager.this.plugin);
                    }
                }
            }.runTaskLater((Plugin)this.plugin, (long)ticks);
        }
    }

    private void mafiaAbility1(Player p) {
        p.setMetadata("dagger_mafia_next_hit", (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)true));
        // === INTIMIDATION: smoke puff + dark menace ring ===
        Location loc = p.getLocation();
        loc.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc.clone().add(0, 1.2, 0), 20, 0.35, 0.4, 0.35, 0.04);
        for (int i = 0; i < 28; i++) {
            double a = i * Math.PI * 2.0 / 28.0;
            Location pt = loc.clone().add(Math.cos(a) * 0.7, 1.0 + this.random.nextDouble() * 0.5, Math.sin(a) * 0.7);
            pt.getWorld().spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, 0,
                (Object) new Particle.DustOptions(Color.fromRGB(30, 30, 30), 1.5f));
        }
        loc.getWorld().spawnParticle(Particle.CRIT, loc.clone().add(0, 1.5, 0), 14, 0.3, 0.4, 0.3, 0.1);
    }

    private void mafiaAbility2(Player player) {
        UUID uuid = player.getUniqueId();
        Set<Entity> old = this.mafiaVindicators.getOrDefault(uuid, new HashSet<Entity>());
        for (Entity e : old) {
            if (!e.isValid()) continue;
            e.remove();
        }
        old.clear();
        HashSet<Vindicator> vinds = new HashSet<Vindicator>();
        int count = this.cfgI("daggers.mafia.ability2.vindicator-count", 4);
        String name = this.plugin.getConfig().getString("daggers.mafia.ability2.vindicator-name", "\u00a76Johnny");
        for (int i = 0; i < count; ++i) {
            double angle = (double)i * (Math.PI * 2 / (double)count);
            Location spawnLoc = player.getLocation().clone().add(Math.cos(angle) * 2.0, 0.0, Math.sin(angle) * 2.0);
            // === SUMMONING PORTAL: dark smoke pillar at each spawn point ===
            spawnLoc.getWorld().spawnParticle(Particle.LARGE_SMOKE, spawnLoc.clone().add(0, 1, 0), 20, 0.2, 0.6, 0.2, 0.02);
            spawnLoc.getWorld().spawnParticle(Particle.DUST, spawnLoc.clone().add(0, 0.2, 0), 8, 0.3, 0.1, 0.3, 0,
                (Object) new Particle.DustOptions(Color.fromRGB(60, 30, 0), 1.8f));
            Vindicator v = (Vindicator)player.getWorld().spawnEntity(spawnLoc, EntityType.VINDICATOR);
            v.setCustomName(name);
            v.setCustomNameVisible(true);
            v.setMetadata("dagger_mafia_owner", (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)uuid.toString()));
            v.setTarget(null);
            vinds.add(v);
        }
        // Central summoning burst
        player.getWorld().spawnParticle(Particle.EXPLOSION, player.getLocation(), 6, 0.5, 0.1, 0.5, 0.0);
        this.mafiaVindicators.put(uuid, new HashSet(vinds));
    }

    public Set<Entity> getMafiaVindicators(UUID uuid) {
        return this.mafiaVindicators.getOrDefault(uuid, Collections.emptySet());
    }

    private void pirateAbility1(Player p) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, this.cfgTicks("daggers.pirate.ability1.duration-seconds", 15.0), this.cfgI("daggers.pirate.ability1.amplifier", 0)));
        // === DOLPHIN GRACE: water splash burst + bubble ring ===
        Location loc = p.getLocation();
        for (int i = 0; i < 36; i++) {
            double a = i * Math.PI * 2.0 / 36.0;
            double r = 0.7 + this.random.nextDouble() * 0.4;
            Location pt = loc.clone().add(Math.cos(a) * r, this.random.nextDouble() * 0.5, Math.sin(a) * r);
            pt.getWorld().spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, 0,
                (Object) new Particle.DustOptions(Color.fromRGB(60, 140, 255), 1.5f));
            if (i % 4 == 0) pt.getWorld().spawnParticle(Particle.BUBBLE_POP, pt, 2, 0.05, 0.1, 0.05, 0.0);
        }
        loc.getWorld().spawnParticle(Particle.SPLASH, loc.clone().add(0, 1, 0), 40, 0.5, 0.4, 0.5, 0.1);
        loc.getWorld().spawnParticle(Particle.FALLING_WATER, loc.clone().add(0, 1.8, 0), 20, 0.4, 0.3, 0.4, 0.0);
    }

    private void pirateAbility2(final Player p) {
        final double pushBlocks = this.cfgD("daggers.pirate.ability2.push-blocks", 25.0);
        final double radius = this.cfgD("daggers.pirate.ability2.radius", 8.0);
        for (Entity e : p.getNearbyEntities(radius, radius, radius)) {
            LivingEntity le;
            if (!(e instanceof LivingEntity) || (le = (LivingEntity)e) == p || this.isTrustedEntity(p, e)) continue;
            Vector away = e.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0).normalize().multiply(pushBlocks * 0.1);
            le.setVelocity(away.setY(0.4));
        }
        p.getWorld().playSound(p.getLocation(), "daggersmp:ability.pirate.wave", 1.6f, 0.7f);
        p.getWorld().playSound(p.getLocation(), "daggersmp:ability.pirate.wave", 1.0f, 1.4f);
        final Location origin = p.getLocation().clone();
        final Vector forward = p.getLocation().getDirection().setY(0).normalize();
        new BukkitRunnable() {
            int t = 0;
            public void run() {
                t++;
                if (t > 14) { this.cancel(); return; }
                double r = t * 0.6;
                int steps = (int) Math.max(20, r * 14);
                for (int i = 0; i < steps; i++) {
                    double a = (Math.PI * 2 * i) / steps;
                    Location pt = origin.clone().add(Math.cos(a) * r, 0.4 + Math.sin(t * 0.5) * 0.2, Math.sin(a) * r);
                    pt.getWorld().spawnParticle(Particle.SPLASH, pt, 6, 0.05, 0.2, 0.05, 0.0);
                    pt.getWorld().spawnParticle(Particle.DUST, pt, 5, 0.0, 0.0, 0.0, (Object) new Particle.DustOptions(Color.fromRGB(80, 160, 255), 1.6f));
                    if (i % 4 == 0) pt.getWorld().spawnParticle(Particle.BUBBLE_POP, pt, 5, 0.05, 0.1, 0.05, 0.0);
                }
                Location front = origin.clone().add(forward.clone().multiply(r * 0.6)).add(0, 1.0, 0);
                front.getWorld().spawnParticle(Particle.DRIPPING_WATER, front, 24, 0.6, 0.4, 0.6, 0.0);
                front.getWorld().spawnParticle(Particle.FALLING_WATER, front, 36, 0.8, 0.5, 0.8, 0.0);
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 2L);
    }

    private void voidAbility1(Player p) {
        double dist = this.cfgD("daggers.void.ability1.teleport-blocks", 20.0);
        Vector dir = p.getLocation().getDirection().normalize();
        Location origin = p.getLocation().clone();
        Location target = p.getLocation().clone();
        for (double d = 1.0; d <= dist; d += 0.5) {
            Location check = p.getLocation().clone().add(dir.clone().multiply(d));
            if (!check.getBlock().getType().isAir() && check.getBlock().getType().isSolid()) continue;
            target = check.clone();
        }
        // === DEPARTURE RIFT: space-time tear at origin ===
        for (int i = 0; i < 48; i++) {
            double a = i * Math.PI * 2.0 / 48.0;
            double r = 0.6 + this.random.nextDouble() * 0.4;
            Location rpt = origin.clone().add(Math.cos(a) * r, 1.0 + this.random.nextDouble() * 0.5, Math.sin(a) * r);
            origin.getWorld().spawnParticle(Particle.PORTAL, rpt, 2, 0.05, 0.1, 0.05, 0.5);
            if (i % 4 == 0) {
                origin.getWorld().spawnParticle(
                    Particle.DUST, rpt, 1, 0, 0, 0, 0,
                    (Object) new Particle.DustOptions(Color.fromRGB(80, 0, 180), 1.5f)
                );
            }
        }
        origin.getWorld().spawnParticle(Particle.FLASH, origin.clone().add(0, 1, 0), 1, 0, 0, 0, 0);
        p.teleport(target);
        // === ARRIVAL RIFT: implosion at destination ===
        for (int i = 0; i < 60; i++) {
            double a = this.random.nextDouble() * Math.PI * 2;
            double r = 0.3 + this.random.nextDouble() * 1.2;
            Location apt = target.clone().add(Math.cos(a) * r, 0.5 + this.random.nextDouble() * 1.5, Math.sin(a) * r);
            p.getWorld().spawnParticle(Particle.PORTAL, apt, 3, 0.1, 0.2, 0.1, 0.6);
        }
        for (int i = 0; i < 36; i++) {
            double a = i * Math.PI * 2.0 / 36.0;
            Location rpt = target.clone().add(Math.cos(a) * 0.8, 1.0, Math.sin(a) * 0.8);
            p.getWorld().spawnParticle(
                Particle.DUST, rpt, 1, 0, 0, 0, 0,
                (Object) new Particle.DustOptions(Color.fromRGB(120, 20, 230), 1.8f)
            );
        }
        p.getWorld().spawnParticle(Particle.END_ROD, target.clone().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.1);
    }

    private void voidAbility2(final Player p) {
        final VoidStateManager vsm = this.plugin.getVoidStateManager();
        if (vsm.isInVoid(p.getUniqueId())) {
            // === EXIT VOID: arrival flash at return location ===
            p.getWorld().spawnParticle(Particle.PORTAL, p.getLocation().clone().add(0, 1, 0), 60, 0.4, 0.8, 0.4, 0.4);
            p.getWorld().spawnParticle(Particle.END_ROD, p.getLocation().clone().add(0, 1, 0), 18, 0.3, 0.5, 0.3, 0.08);
            p.getWorld().spawnParticle(Particle.FLASH, p.getLocation().clone().add(0, 1, 0), 1, 0, 0, 0, 0);
            this.returnFromVoid(p);
            return;
        }
        // === ENTER VOID: black hole implosion at departure ===
        Location original = p.getLocation().clone();
        for (int i = 0; i < 72; i++) {
            double a = this.random.nextDouble() * Math.PI * 2;
            double r = 0.3 + this.random.nextDouble() * 1.5;
            Location pt = original.clone().add(Math.cos(a) * r, 0.5 + this.random.nextDouble() * 1.5, Math.sin(a) * r);
            pt.getWorld().spawnParticle(Particle.PORTAL, pt, 2, 0.05, 0.1, 0.05, 0.3);
        }
        for (int i = 0; i < 40; i++) {
            double a = i * Math.PI * 2.0 / 40.0;
            Location rpt = original.clone().add(Math.cos(a) * 0.8, 1.0, Math.sin(a) * 0.8);
            rpt.getWorld().spawnParticle(Particle.DUST, rpt, 1, 0, 0, 0, 0,
                (Object) new Particle.DustOptions(Color.fromRGB(20, 0, 60), 1.8f));
        }
        original.getWorld().spawnParticle(Particle.SQUID_INK, original.clone().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.02);
        original.getWorld().spawnParticle(Particle.FLASH, original.clone().add(0, 1, 0), 1, 0, 0, 0, 0);
        vsm.enterVoid(p.getUniqueId(), original);
        p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 99999, 0, false, false, false));
        p.setAllowFlight(true);
        p.setFlying(true);
        long returnSec = (long)this.cfgD("daggers.void.ability2.return-seconds", 300.0);
        new BukkitRunnable(){

            public void run() {
                if (!vsm.isInVoid(p.getUniqueId())) {
                    return;
                }
                vsm.exitVoid(p.getUniqueId());
                if (p.isOnline()) {
                    p.removePotionEffect(PotionEffectType.INVISIBILITY);
                    if (!p.isOp()) {
                        p.setAllowFlight(false);
                    }
                    p.setFlying(false);
                }
            }
        }.runTaskLater((Plugin)this.plugin, returnSec * 20L);
    }

    private void returnFromVoid(Player p) {
        VoidStateManager vsm = this.plugin.getVoidStateManager();
        VoidStateManager.VoidEntry entry = vsm.getEntry(p.getUniqueId());
        if (entry == null) {
            return;
        }
        vsm.exitVoid(p.getUniqueId());
        p.removePotionEffect(PotionEffectType.INVISIBILITY);
        if (!p.isOp()) {
            p.setAllowFlight(false);
        }
        p.setFlying(false);
        p.teleport(entry.returnLocation);
        this.plugin.getCooldownManager().setCooldown(p.getUniqueId(), DaggerType.VOID, 2);
    }

    private void luckyAbility1(Player p) {
        int count = this.cfgI("daggers.lucky.ability1.effect-count", 3);
        int dur = this.cfgTicks("daggers.lucky.ability1.duration-seconds", 10.0);
        int amp = this.cfgI("daggers.lucky.ability1.amplifier", 1);
        PotionEffectType[] pool = new PotionEffectType[]{PotionEffectType.SPEED, PotionEffectType.STRENGTH, PotionEffectType.RESISTANCE, PotionEffectType.REGENERATION, PotionEffectType.HASTE, PotionEffectType.FIRE_RESISTANCE, PotionEffectType.ABSORPTION, PotionEffectType.HEALTH_BOOST};
        HashSet<PotionEffectType> chosen = new HashSet<PotionEffectType>();
        while (chosen.size() < Math.min(count, pool.length)) {
            chosen.add(pool[this.random.nextInt(pool.length)]);
        }
        for (PotionEffectType t : chosen) {
            p.addPotionEffect(new PotionEffect(t, dur, amp));
        }
        // === LUCKY BURST: rainbow spiral of sparkles ===
        Location loc = p.getLocation();
        int[][] rainbow = {{255,50,50},{255,160,0},{255,255,0},{50,220,50},{50,150,255},{130,50,255},{255,100,200}};
        for (int i = 0; i < 56; i++) {
            double a = i * Math.PI * 2.0 / 56.0;
            double r = 0.6 + Math.sin(i * 0.3) * 0.3;
            double y = 0.4 + (i / 56.0) * 1.6;
            Location pt = loc.clone().add(Math.cos(a) * r, y, Math.sin(a) * r);
            int[] c = rainbow[i % rainbow.length];
            pt.getWorld().spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, 0,
                (Object) new Particle.DustOptions(Color.fromRGB(c[0], c[1], c[2]), 1.3f));
        }
        loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc.clone().add(0, 2.2, 0), 20, 0.4, 0.3, 0.4, 0.05);
        loc.getWorld().spawnParticle(Particle.FLASH, loc.clone().add(0, 1.5, 0), 1, 0, 0, 0, 0);
    }

    private void mirrorAbility1(final Player p) {
        long ticks = this.cfgTicks("daggers.mirror.ability1.full-reflect-duration-seconds", 10.0);
        p.setMetadata("dagger_mirror_full_reflect", (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)true));

        // === ACTIVATION BURST: silver mirror flash ===
        for (int i = 0; i < 48; i++) {
            double a = i * Math.PI * 2.0 / 48.0;
            double r = 0.7 + this.random.nextDouble() * 0.3;
            Location bpt = p.getLocation().clone().add(Math.cos(a) * r, 1.0 + this.random.nextDouble() * 0.8, Math.sin(a) * r);
            p.getWorld().spawnParticle(
                Particle.DUST, bpt, 1, 0, 0, 0, 0,
                (Object) new Particle.DustOptions(Color.fromRGB(220, 240, 255), 1.6f)
            );
        }
        p.getWorld().spawnParticle(Particle.END_ROD, p.getLocation().clone().add(0, 1.0, 0), 18, 0.4, 0.6, 0.4, 0.06);
        p.getWorld().spawnParticle(Particle.FLASH, p.getLocation().clone().add(0, 1.5, 0), 1, 0, 0, 0, 0);

        // === CONTINUOUS MIRROR SHIELD AURA ===
        final long endTick = ticks;
        new BukkitRunnable(){
            int t = 0;
            public void run() {
                if (++t > endTick || !p.isOnline() || !p.hasMetadata("dagger_mirror_full_reflect")) {
                    this.cancel(); return;
                }
                java.util.Random r = AbilityManager.this.random;
                Location base = p.getLocation();
                // Silver rotating ring of mirror shards
                for (int i = 0; i < 16; i++) {
                    double a = (i * Math.PI * 2.0 / 16.0) + (t * 0.2);
                    double rad = 0.85;
                    double y = 0.7 + Math.sin(t * 0.2 + i * 0.5) * 0.4;
                    Location rpt = base.clone().add(Math.cos(a) * rad, y, Math.sin(a) * rad);
                    rpt.getWorld().spawnParticle(
                        Particle.DUST, rpt, 1, 0, 0, 0, 0,
                        (Object) new Particle.DustOptions(Color.fromRGB(200 + r.nextInt(55), 220 + r.nextInt(35), 255), 1.1f)
                    );
                }
                // END_ROD glints periodically
                if (t % 6 == 0) {
                    base.getWorld().spawnParticle(Particle.END_ROD, base.clone().add(0, 1.2, 0), 3, 0.5, 0.5, 0.5, 0.04);
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L);

        new BukkitRunnable(){
            public void run() {
                if (p.isOnline()) {
                    p.removeMetadata("dagger_mirror_full_reflect", (Plugin)AbilityManager.this.plugin);
                }
            }
        }.runTaskLater((Plugin)this.plugin, ticks);
    }

    private void mirrorAbility2(Player p) {
        double radius = this.cfgD("daggers.mirror.ability2.swap-radius", 5.0);
        Player target = null;
        double best = Double.MAX_VALUE;
        for (Entity e : p.getNearbyEntities(radius, radius, radius)) {
            double d;
            if (!(e instanceof Player)) continue;
            Player tp = (Player)e;
            if (this.plugin.getTrustManager().isTrusted(p.getUniqueId(), tp.getUniqueId()) || !((d = e.getLocation().distanceSquared(p.getLocation())) < best)) continue;
            best = d;
            target = tp;
        }
        if (target == null) {
            return;
        }
        Location a = p.getLocation().clone();
        Location b = target.getLocation().clone();
        // === POSITION SWAP: silver mirror flash at both locations ===
        for (Location swapLoc : new Location[]{a, b}) {
            for (int i = 0; i < 40; i++) {
                double ang = i * Math.PI * 2.0 / 40.0;
                Location pt = swapLoc.clone().add(Math.cos(ang) * 0.6, 0.8 + this.random.nextDouble() * 0.8, Math.sin(ang) * 0.6);
                pt.getWorld().spawnParticle(Particle.END_ROD, pt, 1, 0.02, 0.02, 0.02, 0.02);
            }
            swapLoc.getWorld().spawnParticle(Particle.FLASH, swapLoc.clone().add(0, 1, 0), 1, 0, 0, 0, 0);
            swapLoc.getWorld().spawnParticle(Particle.DUST, swapLoc.clone().add(0, 1, 0), 16, 0.3, 0.5, 0.3, 0,
                (Object) new Particle.DustOptions(Color.fromRGB(220, 240, 255), 1.6f));
        }
        p.teleport(b);
        target.teleport(a);
    }

    private void jungleAbility1(final Player p) {
        Location targetLoc;
        Entity targetEnt;
        double range = this.cfgD("daggers.jungle.ability1.range", 20.0);
        final double speed = this.cfgD("daggers.jungle.ability1.pull-speed", 1.5);
        final long timeoutSec = (long)this.cfgD("daggers.jungle.ability1.timeout-seconds", 7.0);
        RayTraceResult entHit = p.getWorld().rayTraceEntities(p.getEyeLocation(), p.getLocation().getDirection(), range, 0.5, e -> e != p);
        Entity entity = targetEnt = entHit != null ? entHit.getHitEntity() : null;
        if (targetEnt != null) {
            targetLoc = null;
        } else {
            RayTraceResult blockHit = p.getWorld().rayTraceBlocks(p.getEyeLocation(), p.getLocation().getDirection(), range);
            if (blockHit == null || blockHit.getHitPosition() == null) {
                return;
            }
            targetLoc = blockHit.getHitPosition().toLocation(p.getWorld());
        }
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_FISHING_BOBBER_THROW, 1.0f, 1.2f);
        final Particle.DustOptions vineGreen = new Particle.DustOptions(Color.fromRGB(40, 160, 50), 0.9f);
        final Particle.DustOptions vineLeaf  = new Particle.DustOptions(Color.fromRGB(110, 210, 90), 0.7f);
        new BukkitRunnable(){
            int ticks = 0;

            public void run() {
                ++this.ticks;
                if (!p.isOnline() || (long)this.ticks > timeoutSec * 20L) {
                    this.cancel();
                    return;
                }
                Location dest = targetEnt != null ? targetEnt.getLocation().add(0.0, 0.5, 0.0) : targetLoc;
                // Draw a visible vine from the player's hand to the grapple target every tick.
                Location handLoc = p.getEyeLocation().add(p.getLocation().getDirection().multiply(0.4)).subtract(0.0, 0.3, 0.0);
                Vector full = dest.toVector().subtract(handLoc.toVector());
                double total = full.length();
                if (total > 0.001) {
                    Vector unit = full.clone().normalize();
                    double step = 0.35;
                    for (double d = 0.0; d <= total; d += step) {
                        Location pt = handLoc.clone().add(unit.clone().multiply(d));
                        pt.getWorld().spawnParticle(Particle.DUST, pt, 2, 0.02, 0.02, 0.02, (Object) vineGreen);
                        if (((int)(d * 4) + this.ticks) % 5 == 0) {
                            pt.getWorld().spawnParticle(Particle.DUST, pt, 1, 0.05, 0.05, 0.05, (Object) vineLeaf);
                        }
                        if (((int)(d * 4) + this.ticks) % 9 == 0) {
                            pt.getWorld().spawnParticle(Particle.COMPOSTER, pt, 1, 0.0, 0.0, 0.0, 0.0);
                        }
                    }
                    // Tip flair so the anchor point is obviously connected.
                    dest.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, dest, 2, 0.15, 0.15, 0.15, 0.0);
                }
                Vector to = dest.toVector().subtract(p.getLocation().toVector());
                double dist = to.length();
                if (dist < 1.5) {
                    p.setVelocity(new Vector(0.0, 0.2, 0.0));
                    this.cancel();
                    return;
                }
                Vector vel = to.normalize().multiply(speed);
                p.setVelocity(vel);
                p.setFallDistance(0.0f);
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L);
    }

    private void jungleAbility2(final Player p) {
        final double range = this.cfgD("daggers.jungle.ability2.range", 20.0);
        final int poisonDur = this.cfgTicks("daggers.jungle.ability2.poison-duration-seconds", 5.0);
        final int poisonAmp = this.cfgI("daggers.jungle.ability2.poison-amplifier", 0);
        final int idAmp = this.cfgI("daggers.jungle.ability2.instant-damage-amplifier", 0);
        final Location start = p.getEyeLocation();
        final Vector dir = start.getDirection().normalize();
        p.getWorld().playSound(start, Sound.BLOCK_GRASS_BREAK, 1.2f, 1.4f);
        new BukkitRunnable() {
            double dist = 0.0;
            public void run() {
                for (int i = 0; i < 4; i++) {
                    dist += 0.5;
                    if (dist > range) { this.cancel(); return; }
                    Location pt = start.clone().add(dir.clone().multiply(dist));
                    pt.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, pt, 5, 0.05, 0.05, 0.05, 0.0);
                    pt.getWorld().spawnParticle(Particle.COMPOSTER, pt, 6, 0.1, 0.1, 0.1, 0.0);
                    if (!pt.getBlock().isPassable()) { this.cancel(); return; }
                    for (Entity e : pt.getWorld().getNearbyEntities(pt, 1.2, 1.2, 1.2)) {
                        if (e == p || !(e instanceof LivingEntity)) continue;
                        if (AbilityManager.this.isTrustedEntity(p, e)) continue;
                        LivingEntity le = (LivingEntity) e;
                        Location dest = p.getLocation().clone();
                        Vector pull = p.getLocation().toVector().subtract(le.getLocation().toVector());
                        double len = pull.length();
                        if (len > 1.0) {
                            le.setVelocity(pull.normalize().multiply(Math.min(2.5, len * 0.3)).setY(0.4));
                        } else {
                            le.teleport(dest);
                        }
                        le.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, 1, idAmp));
                        le.addPotionEffect(new PotionEffect(PotionEffectType.POISON, poisonDur, poisonAmp));
                        le.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, le.getLocation().add(0.0, 1.0, 0.0), 45, 0.4, 0.5, 0.4, 0.0);
                        le.getWorld().playSound(le.getLocation(), Sound.BLOCK_VINE_BREAK, 1.0f, 1.0f);
                        this.cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L);
    }

    private void midasAbility1(Player p) {
        p.setMetadata("dagger_midas_next_hit", (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)true));
        // === GOLDEN TOUCH: golden gleam aura ready indicator ===
        Location loc = p.getLocation();
        for (int i = 0; i < 48; i++) {
            double a = i * Math.PI * 2.0 / 48.0;
            double r = 0.5 + this.random.nextDouble() * 0.3;
            double y = 0.2 + (i / 48.0) * 1.8;
            Location pt = loc.clone().add(Math.cos(a) * r, y, Math.sin(a) * r);
            pt.getWorld().spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, 0,
                (Object) new Particle.DustOptions(Color.fromRGB(255, 210, 0), 1.4f));
        }
        loc.getWorld().spawnParticle(Particle.WAX_ON, loc.clone().add(0, 2.0, 0), 16, 0.4, 0.2, 0.4, 0.05);
        loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc.clone().add(0, 1.0, 0), 8, 0.3, 0.5, 0.3, 0.04);
    }

    private void midasAbility2(Player p) {
        double radius = this.cfgD("daggers.midas.ability2.radius", 5.0);
        upgradeArmorToNetherite(p);
        for (Entity e : p.getNearbyEntities(radius, radius, radius)) {
            if (!(e instanceof Player)) continue;
            Player tp = (Player)e;
            if (!this.plugin.getTrustManager().isTrusted(p.getUniqueId(), tp.getUniqueId())) continue;
            upgradeArmorToNetherite(tp);
        }
    }

    private static void upgradeArmorToNetherite(Player p) {
        ItemStack[] armor = p.getInventory().getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            ItemStack piece = armor[i];
            if (piece == null || piece.getType() == Material.AIR) continue;
            Material newMat = netheriteEquivalent(piece.getType());
            if (newMat == null || newMat == piece.getType()) continue;
            ItemStack neu = new ItemStack(newMat);
            org.bukkit.inventory.meta.ItemMeta oldMeta = piece.getItemMeta();
            org.bukkit.inventory.meta.ItemMeta newMeta = neu.getItemMeta();
            if (oldMeta != null && newMeta != null) {
                for (java.util.Map.Entry<org.bukkit.enchantments.Enchantment, Integer> en : oldMeta.getEnchants().entrySet()) {
                    newMeta.addEnchant(en.getKey(), en.getValue(), true);
                }
                if (oldMeta.hasDisplayName()) newMeta.setDisplayName(oldMeta.getDisplayName());
                if (oldMeta.hasLore()) newMeta.setLore(oldMeta.getLore());
                newMeta.setUnbreakable(oldMeta.isUnbreakable());
                neu.setItemMeta(newMeta);
            }
            armor[i] = neu;
        }
        p.getInventory().setArmorContents(armor);
        p.getWorld().spawnParticle(Particle.WAX_ON, p.getLocation().add(0.0, 1.0, 0.0), 90, 0.5, 1.0, 0.5, 0.0);
        p.getWorld().playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1.2f, 1.0f);
    }

    private static Material netheriteEquivalent(Material m) {
        switch (m) {
            case LEATHER_HELMET: case CHAINMAIL_HELMET: case IRON_HELMET: case GOLDEN_HELMET: case DIAMOND_HELMET: return Material.NETHERITE_HELMET;
            case LEATHER_CHESTPLATE: case CHAINMAIL_CHESTPLATE: case IRON_CHESTPLATE: case GOLDEN_CHESTPLATE: case DIAMOND_CHESTPLATE: return Material.NETHERITE_CHESTPLATE;
            case LEATHER_LEGGINGS: case CHAINMAIL_LEGGINGS: case IRON_LEGGINGS: case GOLDEN_LEGGINGS: case DIAMOND_LEGGINGS: return Material.NETHERITE_LEGGINGS;
            case LEATHER_BOOTS: case CHAINMAIL_BOOTS: case IRON_BOOTS: case GOLDEN_BOOTS: case DIAMOND_BOOTS: return Material.NETHERITE_BOOTS;
            default: return null;
        }
    }

    private void toxicAbility1(final Player p) {
        final int durTicks = this.cfgTicks("daggers.toxic.ability1.cloud-duration-seconds", 5.0);
        final int amp = this.cfgI("daggers.toxic.ability1.poison-amplifier", 2);
        final double radius = this.cfgD("daggers.toxic.ability1.radius", 5.0);
        final Location center = p.getLocation().add(0.0, 0.5, 0.0);
        p.getWorld().playSound(center, "daggersmp:ability.toxic.cloud", 1.2f, 0.8f);

        // Spawn an AreaEffectCloud entity at the center for built-in poison application
        org.bukkit.entity.AreaEffectCloud cloud = (org.bukkit.entity.AreaEffectCloud) p.getWorld()
                .spawnEntity(center, EntityType.AREA_EFFECT_CLOUD);
        cloud.setRadius((float) radius);
        cloud.setRadiusOnUse(0.0f);
        cloud.setRadiusPerTick(0.0f);
        cloud.setDuration(durTicks);
        cloud.setReapplicationDelay(20);
        cloud.setWaitTime(0);
        cloud.setSource((ProjectileSource) p);
        cloud.setParticle(Particle.ENTITY_EFFECT, Color.fromRGB(60, 220, 50));
        try {
            cloud.addCustomEffect(new PotionEffect(PotionEffectType.POISON, 100, amp), true);
        } catch (Throwable ignored) {}

        // Realistic toxic cloud: volumetric fog, rising wisps, rotating vortex, drips, billow
        new BukkitRunnable(){
            int t = 0;
            public void run() {
                if (++this.t > durTicks || !cloud.isValid()) {
                    this.cancel();
                    return;
                }
                java.util.Random r = AbilityManager.this.random;
                Location c = cloud.getLocation();
                double phase = this.t * 0.04;

                // === VOLUMETRIC FILL: dense opaque color-varied fog sphere ===
                for (int i = 0; i < 200; i++) {
                    double u = r.nextDouble();
                    double dr = Math.cbrt(u) * radius;
                    double theta = r.nextDouble() * Math.PI * 2;
                    double phi = Math.acos(2.0 * r.nextDouble() - 1.0);
                    double dx = dr * Math.sin(phi) * Math.cos(theta);
                    double dy = dr * Math.cos(phi) * 0.55;
                    double dz = dr * Math.sin(phi) * Math.sin(theta);
                    Location pt = c.clone().add(dx, Math.abs(dy) + 0.1, dz);
                    int rg = 35 + r.nextInt(45);
                    int gg = 170 + r.nextInt(60);
                    int bg = 15 + r.nextInt(30);
                    pt.getWorld().spawnParticle(
                        Particle.DUST, pt, 1, 0.0, 0.0, 0.0, 0.0,
                        (Object) new Particle.DustOptions(Color.fromRGB(rg, gg, bg), 3.0f)
                    );
                }

                // === RISING WISPS: lazy upward-drifting tendrils ===
                for (int i = 0; i < 20; i++) {
                    double ang = r.nextDouble() * Math.PI * 2;
                    double dr = Math.sqrt(r.nextDouble()) * radius * 0.75;
                    double wy = 0.6 + r.nextDouble() * 1.8;
                    Location pt = c.clone().add(Math.cos(ang) * dr, wy, Math.sin(ang) * dr);
                    pt.getWorld().spawnParticle(
                        Particle.DUST, pt, 1, 0.0, 0.06, 0.0, 0.015,
                        (Object) new Particle.DustOptions(Color.fromRGB(90, 240, 40), 1.6f)
                    );
                }

                // === TOXIC VORTEX: spiral swirling toward center ===
                for (int i = 0; i < 24; i++) {
                    double a = phase * 2.8 + (i * Math.PI * 2.0 / 24.0);
                    double vr = radius * 0.38 * (1.0 - (i / 24.0) * 0.35);
                    double vy = 0.25 + i * 0.1;
                    Location vpt = c.clone().add(Math.cos(a) * vr, vy, Math.sin(a) * vr);
                    vpt.getWorld().spawnParticle(
                        Particle.DUST, vpt, 1, 0, 0, 0, 0,
                        (Object) new Particle.DustOptions(Color.fromRGB(110, 255, 55), 2.2f)
                    );
                }

                // === CLOUD BILLOW: large boundary particles for 3D depth ===
                for (int i = 0; i < 18; i++) {
                    double ang = r.nextDouble() * Math.PI * 2;
                    double br = radius * (0.82 + r.nextDouble() * 0.22);
                    double by = 0.2 + r.nextDouble() * 1.2;
                    Location bpt = c.clone().add(Math.cos(ang) * br, by, Math.sin(ang) * br);
                    bpt.getWorld().spawnParticle(
                        Particle.DUST, bpt, 1, 0.0, 0.0, 0.0, 0.0,
                        (Object) new Particle.DustOptions(Color.fromRGB(25, 140, 8), 5.0f)
                    );
                }

                // === DRIP ACCENTS: slime bubbles along the ground ===
                for (int i = 0; i < 10; i++) {
                    double ang = r.nextDouble() * Math.PI * 2;
                    double dr = Math.sqrt(r.nextDouble()) * radius;
                    Location pt = c.clone().add(Math.cos(ang) * dr, 0.05, Math.sin(ang) * dr);
                    pt.getWorld().spawnParticle(Particle.ITEM_SLIME, pt, 3, 0.18, 0.12, 0.18, 0.0);
                }

                // === TOXIC DROPS: sneeze splatter spray every 4 ticks ===
                if (this.t % 4 == 0) {
                    for (int i = 0; i < 6; i++) {
                        double ang = r.nextDouble() * Math.PI * 2;
                        double dr = Math.sqrt(r.nextDouble()) * radius;
                        Location pt = c.clone().add(Math.cos(ang) * dr, r.nextDouble() * 0.6, Math.sin(ang) * dr);
                        pt.getWorld().spawnParticle(Particle.SNEEZE, pt, 2, 0.12, 0.08, 0.12, 0.01);
                    }
                }

                if (this.t % 20 == 0) {
                    cloud.getWorld().playSound(cloud.getLocation(), Sound.ENTITY_SPIDER_HURT, 0.5f, 0.6f);
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L);
    }

    private void toxicAbility2(Player p) {
        p.setMetadata("dagger_toxic_lethal", (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)true));
        // === LETHAL DOSE: venomous ready-state pulse ===
        Location loc = p.getLocation();
        for (int i = 0; i < 40; i++) {
            double a = i * Math.PI * 2.0 / 40.0;
            double r = 0.6 + Math.sin(i * 0.4) * 0.2;
            double y = 0.3 + (i / 40.0) * 1.8;
            Location pt = loc.clone().add(Math.cos(a) * r, y, Math.sin(a) * r);
            pt.getWorld().spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, 0,
                (Object) new Particle.DustOptions(Color.fromRGB(60, 200 + this.random.nextInt(55), 20), 1.5f));
        }
        loc.getWorld().spawnParticle(Particle.SNEEZE, loc.clone().add(0, 2.0, 0), 12, 0.3, 0.2, 0.3, 0.04);
        loc.getWorld().spawnParticle(Particle.ITEM_SLIME, loc.clone().add(0, 1.5, 0), 10, 0.3, 0.4, 0.3, 0.04);
    }

    private void arachnidAbility1(Player p) {
        p.setMetadata("dagger_arachnid_next", (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)true));
        // === WEB TRAP: dark spider-silk charge indicator ===
        Location loc = p.getLocation();
        for (int i = 0; i < 32; i++) {
            double a = i * Math.PI * 2.0 / 32.0;
            double r = 0.5 + this.random.nextDouble() * 0.35;
            Location pt = loc.clone().add(Math.cos(a) * r, 0.5 + this.random.nextDouble() * 1.2, Math.sin(a) * r);
            pt.getWorld().spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, 0,
                (Object) new Particle.DustOptions(Color.fromRGB(200, 200, 200), 1.2f));
        }
        loc.getWorld().spawnParticle(Particle.SQUID_INK, loc.clone().add(0, 1.2, 0), 8, 0.25, 0.4, 0.25, 0.02);
        loc.getWorld().spawnParticle(Particle.CLOUD, loc.clone().add(0, 1.0, 0), 10, 0.4, 0.5, 0.4, 0.02);
    }

    private void vampireAbility1(final Player p) {
        int dur = this.cfgTicks("daggers.vampire.ability1.duration-seconds", 8.0);
        p.setMetadata("dagger_vampire_heal", (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)true));
        p.getWorld().playSound(p.getLocation(), "daggersmp:ability.vampire.heal", 1.0f, 1.5f);

        // === ACTIVATION BURST: blood surge ring ===
        for (int i = 0; i < 48; i++) {
            double a = i * Math.PI * 2.0 / 48.0;
            double r = 0.65 + this.random.nextDouble() * 0.4;
            Location bpt = p.getLocation().clone().add(Math.cos(a) * r, 1.0 + this.random.nextDouble() * 0.9, Math.sin(a) * r);
            p.getWorld().spawnParticle(
                Particle.DUST, bpt, 1, 0, 0, 0, 0,
                (Object) new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.8f)
            );
        }
        p.getWorld().spawnParticle(Particle.HEART, p.getLocation().clone().add(0, 2.2, 0), 6, 0.3, 0.2, 0.3, 0.0);

        // === CONTINUOUS BLOOD DRAIN AURA ===
        new BukkitRunnable(){
            int t = 0;
            public void run() {
                if (++t > dur || !p.isOnline()) { this.cancel(); return; }
                java.util.Random r = AbilityManager.this.random;
                Location base = p.getLocation();

                // Swirling red aura coiling around the caster
                for (int i = 0; i < 12; i++) {
                    double a = (t * 0.28) + (i * Math.PI * 2.0 / 12.0);
                    double rad = 0.75 + Math.sin(t * 0.18 + i) * 0.22;
                    double y = 0.7 + r.nextDouble() * 1.3;
                    Location apt = base.clone().add(Math.cos(a) * rad, y, Math.sin(a) * rad);
                    apt.getWorld().spawnParticle(
                        Particle.DUST, apt, 1, 0, 0, 0, 0,
                        (Object) new Particle.DustOptions(Color.fromRGB(150 + r.nextInt(80), 0, 0), 1.2f)
                    );
                }

                // Blood streams flowing from nearby enemies toward the caster
                if (t % 2 == 0) {
                    for (Entity e : p.getNearbyEntities(8, 8, 8)) {
                        if (!(e instanceof LivingEntity) || e == p || AbilityManager.this.isTrustedEntity(p, e)) continue;
                        Vector toward = base.toVector().subtract(e.getLocation().toVector());
                        double dist = toward.length();
                        if (dist < 0.5) continue;
                        for (int k = 1; k <= 4; k++) {
                            double frac = k / 5.0;
                            Location sp = e.getLocation().clone().add(0, 1.4, 0).add(toward.clone().multiply(frac));
                            sp.getWorld().spawnParticle(
                                Particle.DUST, sp, 1, 0.04, 0.04, 0.04, 0,
                                (Object) new Particle.DustOptions(Color.fromRGB(210, 10, 20), 0.85f)
                            );
                        }
                        if (t % 8 == 0) {
                            e.getWorld().spawnParticle(Particle.HEART, e.getLocation().clone().add(0, 2.1, 0), 1, 0.1, 0.1, 0.1, 0.0);
                        }
                        break;
                    }
                }

                // Pulsing blood ring expanding from caster every 10 ticks
                if (t % 10 == 0) {
                    for (int i = 0; i < 30; i++) {
                        double a = i * Math.PI * 2.0 / 30.0;
                        Location rpt = base.clone().add(Math.cos(a) * 1.6, 0.4, Math.sin(a) * 1.6);
                        rpt.getWorld().spawnParticle(
                            Particle.DUST, rpt, 1, 0, 0, 0, 0,
                            (Object) new Particle.DustOptions(Color.fromRGB(220, 10, 10), 1.5f)
                        );
                    }
                    p.getWorld().spawnParticle(Particle.HEART, base.clone().add(0, 2.6, 0), 2, 0.2, 0.1, 0.2, 0.0);
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L);

        new BukkitRunnable(){
            public void run() {
                if (p.isOnline()) {
                    p.removeMetadata("dagger_vampire_heal", (Plugin)AbilityManager.this.plugin);
                }
            }
        }.runTaskLater((Plugin)this.plugin, (long)dur);
    }

    private void gravityAbility1(final Player p) {
        // Spawn a real-looking black hole 5 blocks in front of the player.
        // It hovers in place, pulls entities toward its center, then collapses
        // outward in an explosion that flings everything away.
        final double pullRadius = this.cfgD("daggers.gravity.ability1.pull-radius", 8.0);
        final double dmg = this.cfgD("daggers.gravity.ability1.pull-damage", 6.0);
        final double flingY = this.cfgD("daggers.gravity.ability1.fling-y", 1.5);
        final int lifeTicks = this.cfgTicks("daggers.gravity.ability1.collapse-delay-seconds", 3.0);
        final double spawnDist = this.cfgD("daggers.gravity.ability1.spawn-distance", 5.0);
        final double coreRadius = this.cfgD("daggers.gravity.ability1.core-radius", 1.4);

        Vector dir = p.getEyeLocation().getDirection().normalize();
        final Location origin = p.getEyeLocation().add(dir.clone().multiply(spawnDist));
        // Snap above ground a bit so it floats
        if (origin.getBlock().getType().isSolid()) {
            origin.add(0, 1.5, 0);
        }

        p.getWorld().playSound(origin, "daggersmp:ability.gravity.blackhole", 2.0f, 0.4f);
        p.getWorld().playSound(origin, "daggersmp:ability.gravity.blackhole", 1.6f, 0.3f);

        new BukkitRunnable(){
            int t = 0;
            public void run() {
                this.t++;
                java.util.Random r = AbilityManager.this.random;

                // === BLACK HOLE CORE: dense pitch-black sphere of squid ink ===
                for (int i = 0; i < 90; i++) {
                    double u = r.nextDouble();
                    double cr = Math.cbrt(u) * coreRadius * 0.9;
                    double theta = r.nextDouble() * Math.PI * 2;
                    double phi = Math.acos(2.0 * r.nextDouble() - 1.0);
                    double dx = cr * Math.sin(phi) * Math.cos(theta);
                    double dy = cr * Math.cos(phi);
                    double dz = cr * Math.sin(phi) * Math.sin(theta);
                    origin.getWorld().spawnParticle(Particle.SQUID_INK, origin.clone().add(dx, dy, dz), 1, 0, 0, 0, 0);
                }

                // === PHOTON SPHERE: bright white-violet ring at event horizon ===
                double photonR = coreRadius * 1.2;
                for (int i = 0; i < 36; i++) {
                    double a = (i * Math.PI * 2 / 36.0) + (this.t * 0.22);
                    double yOff = Math.sin(a * 4 + this.t * 0.12) * 0.06;
                    Location pt = origin.clone().add(Math.cos(a) * photonR, yOff, Math.sin(a) * photonR);
                    int rv = 220 + r.nextInt(35);
                    origin.getWorld().spawnParticle(
                        Particle.DUST, pt, 1, 0, 0, 0, 0,
                        (Object) new Particle.DustOptions(Color.fromRGB(Math.min(255, rv), 180, 255), 1.0f)
                    );
                }

                // === INNER ACCRETION DISK: hot bright ring spinning fast ===
                double innerR = coreRadius * 1.75;
                for (int i = 0; i < 55; i++) {
                    double a = (this.t * 0.48) + (i * Math.PI * 2 / 55.0);
                    double yOff = Math.sin(a * 2.5 + this.t * 0.18) * 0.2;
                    Location pt = origin.clone().add(Math.cos(a) * innerR, yOff, Math.sin(a) * innerR);
                    int rv = 150 + r.nextInt(80);
                    int gv = 20 + r.nextInt(40);
                    int bv = 200 + r.nextInt(55);
                    origin.getWorld().spawnParticle(
                        Particle.DUST, pt, 1, 0, 0, 0, 0,
                        (Object) new Particle.DustOptions(Color.fromRGB(Math.min(255, rv), gv, Math.min(255, bv)), 2.0f)
                    );
                }

                // === OUTER ACCRETION DISK: wider slower counter-rotating ring ===
                double outerR = coreRadius * 2.7;
                for (int i = 0; i < 40; i++) {
                    double a = -(this.t * 0.20) + (i * Math.PI * 2 / 40.0);
                    double yOff = Math.sin(a * 1.5 + this.t * 0.07) * 0.38;
                    Location pt = origin.clone().add(Math.cos(a) * outerR, yOff, Math.sin(a) * outerR);
                    origin.getWorld().spawnParticle(
                        Particle.DUST, pt, 1, 0, 0, 0, 0,
                        (Object) new Particle.DustOptions(Color.fromRGB(70, 10, 150), 1.6f)
                    );
                }

                // === POLAR JETS: narrow beams shooting from top and bottom poles ===
                if (this.t % 2 == 0) {
                    for (int k = 0; k < 2; k++) {
                        double sign = (k == 0) ? 1.0 : -1.0;
                        for (int i = 0; i < 18; i++) {
                            double jetY = sign * (coreRadius * 0.9 + i * 0.28);
                            double spread = 0.06 + i * 0.012;
                            double jx = (r.nextDouble() - 0.5) * spread;
                            double jz = (r.nextDouble() - 0.5) * spread;
                            Location jpt = origin.clone().add(jx, jetY, jz);
                            int brightness = Math.max(0, 255 - i * 10);
                            int blue = 255;
                            int green = Math.max(0, 200 - i * 8);
                            origin.getWorld().spawnParticle(
                                Particle.DUST, jpt, 1, 0, 0, 0, 0,
                                (Object) new Particle.DustOptions(Color.fromRGB(brightness, green, blue), 0.75f)
                            );
                        }
                    }
                }

                // === HAWKING RADIATION: white sparks just outside event horizon ===
                if (this.t % 3 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double theta = r.nextDouble() * Math.PI * 2;
                        double phi = Math.acos(2.0 * r.nextDouble() - 1.0);
                        double hr = coreRadius * (1.05 + r.nextDouble() * 0.25);
                        Location hpt = origin.clone().add(
                            hr * Math.sin(phi) * Math.cos(theta),
                            hr * Math.cos(phi),
                            hr * Math.sin(phi) * Math.sin(theta)
                        );
                        origin.getWorld().spawnParticle(
                            Particle.DUST, hpt, 1, 0, 0, 0, 0,
                            (Object) new Particle.DustOptions(Color.fromRGB(255, 255, 255), 0.55f)
                        );
                    }
                }

                // === INFALLING MATERIAL: portal particles spiraling inward ===
                for (int i = 0; i < 14; i++) {
                    double a = r.nextDouble() * Math.PI * 2;
                    double dist = coreRadius * (3.5 + r.nextDouble() * 2.5);
                    Location pt = origin.clone().add(Math.cos(a) * dist, (r.nextDouble() - 0.5) * 1.8, Math.sin(a) * dist);
                    origin.getWorld().spawnParticle(Particle.PORTAL, pt, 1, 0, 0, 0, 0.35);
                }

                // === GRAVITATIONAL LENSING: shimmering arcs at large radius ===
                if (this.t % 4 == 0) {
                    double lensR = coreRadius * (2.2 + r.nextDouble() * 2.5);
                    double lensA = r.nextDouble() * Math.PI * 2;
                    int lensCount = 10;
                    for (int i = 0; i < lensCount; i++) {
                        double a = lensA + (i * Math.PI * 2.0 / lensCount);
                        Location pt = origin.clone().add(Math.cos(a) * lensR, (r.nextDouble() - 0.5) * 0.5, Math.sin(a) * lensR);
                        origin.getWorld().spawnParticle(
                            Particle.DUST, pt, 1, 0, 0, 0, 0,
                            (Object) new Particle.DustOptions(Color.fromRGB(190, 130, 255), 0.5f)
                        );
                    }
                }

                // --- Pull entities toward the core (each tick) ---
                for (Entity e : origin.getWorld().getNearbyEntities(origin, pullRadius, pullRadius, pullRadius)) {
                    if (!(e instanceof LivingEntity) || e == p || AbilityManager.this.isTrustedEntity(p, e)) continue;
                    Vector toCore = origin.toVector().subtract(e.getLocation().toVector());
                    double dist = toCore.length();
                    if (dist < 0.01) continue;
                    double strength = Math.min(1.2, 0.6 + (1.0 - dist / pullRadius) * 0.9);
                    Vector pull = toCore.normalize().multiply(strength);
                    Vector vel = e.getVelocity().multiply(0.55).add(pull);
                    e.setVelocity(vel);
                }

                if (this.t % 10 == 0) {
                    origin.getWorld().playSound(origin, "daggersmp:ability.gravity.blackhole", 1.5f, 0.4f);
                }

                if (this.t >= lifeTicks) {
                    this.cancel();
                    // === COLLAPSE: singularity shockwave explosion ===
                    origin.getWorld().playSound(origin, "daggersmp:ability.gravity.blackhole", 2.5f, 0.7f);
                    origin.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, origin, 3, 0.4, 0.4, 0.4, 0);
                    origin.getWorld().spawnParticle(Particle.FLASH, origin, 5, 0.3, 0.3, 0.3, 0);
                    // Expanding shockwave ring
                    for (int i = 0; i < 72; i++) {
                        double a = i * Math.PI * 2 / 72.0;
                        double er = pullRadius * 0.75;
                        Location ep = origin.clone().add(Math.cos(a) * er, 0.25, Math.sin(a) * er);
                        ep.getWorld().spawnParticle(
                            Particle.DUST, ep, 1, 0, 0, 0, 0,
                            (Object) new Particle.DustOptions(Color.fromRGB(255, 230, 255), 2.8f)
                        );
                    }
                    // Volumetric debris cloud
                    for (int i = 0; i < 200; i++) {
                        double a = AbilityManager.this.random.nextDouble() * Math.PI * 2;
                        double rr = AbilityManager.this.random.nextDouble() * pullRadius;
                        double ry = (AbilityManager.this.random.nextDouble() - 0.5) * 4.0;
                        Location pt = origin.clone().add(Math.cos(a) * rr, ry, Math.sin(a) * rr);
                        int rc = 120 + AbilityManager.this.random.nextInt(100);
                        int bc = 180 + AbilityManager.this.random.nextInt(75);
                        pt.getWorld().spawnParticle(
                            Particle.DUST, pt, 1, 0, 0, 0, 0,
                            (Object) new Particle.DustOptions(Color.fromRGB(Math.min(255, rc), 15, Math.min(255, bc)), 2.0f)
                        );
                    }
                    for (Entity e : origin.getWorld().getNearbyEntities(origin, pullRadius, pullRadius, pullRadius)) {
                        if (!(e instanceof LivingEntity) || e == p || AbilityManager.this.isTrustedEntity(p, e)) continue;
                        LivingEntity le = (LivingEntity) e;
                        le.damage(dmg, (Entity) p);
                        Vector out = le.getLocation().toVector().subtract(origin.toVector());
                        if (out.lengthSquared() < 0.01) {
                            out = new Vector(AbilityManager.this.random.nextDouble() - 0.5, 0, AbilityManager.this.random.nextDouble() - 0.5);
                        }
                        out = out.normalize().multiply(1.6).setY(flingY);
                        le.setVelocity(out);
                    }
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L);
    }

    private void gravityAbility2(final Player p) {
        final double radius = this.cfgD("daggers.gravity.ability2.radius", 6.0);
        final int dur = this.cfgTicks("daggers.gravity.ability2.duration-seconds", 5.0);
        final int amp = this.cfgI("daggers.gravity.ability2.levitation-amplifier", 9);

        p.getWorld().playSound(p.getLocation(), "daggersmp:ability.gravity.blackhole", 1.4f, 1.4f);

        // Apply levitation to all hostile/non-trusted entities (not just players) in radius
        for (Entity e : p.getNearbyEntities(radius, radius, radius)) {
            if (!(e instanceof LivingEntity) || e == p || this.isTrustedEntity(p, e)) continue;
            ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, dur, amp));
        }

        // Visible purple levitation field for the duration
        new BukkitRunnable(){
            int t = 0;
            public void run() {
                if (++this.t > dur || !p.isOnline()) {
                    this.cancel();
                    return;
                }
                Location base = p.getLocation();
                java.util.Random r = AbilityManager.this.random;
                double phase = this.t * 0.05;

                // === ACTIVATION BURST: expanding ring on first 12 ticks ===
                if (this.t <= 12) {
                    double burstR = radius * this.t / 12.0;
                    int bsteps = 120;
                    for (int i = 0; i < bsteps; i++) {
                        double a = i * Math.PI * 2.0 / bsteps;
                        Location pt = base.clone().add(Math.cos(a) * burstR, 0.05, Math.sin(a) * burstR);
                        pt.getWorld().spawnParticle(
                            Particle.DUST, pt, 1, 0, 0, 0, 0,
                            (Object) new Particle.DustOptions(Color.fromRGB(220, 100, 255), 2.2f)
                        );
                    }
                }

                // === OUTER GROUND RING: solid rotating main ring ===
                int steps = 180;
                for (int i = 0; i < steps; i++) {
                    double a = (i * Math.PI * 2.0 / steps) + phase;
                    Location pt = base.clone().add(Math.cos(a) * radius, 0.1, Math.sin(a) * radius);
                    pt.getWorld().spawnParticle(
                        Particle.DUST, pt, 1, 0, 0, 0, 0,
                        (Object) new Particle.DustOptions(Color.fromRGB(160, 50, 230), 1.5f)
                    );
                }

                // === INNER GROUND RING: counter-rotating inner ring ===
                double innerRad = radius * 0.45;
                for (int i = 0; i < 80; i++) {
                    double a = -(i * Math.PI * 2.0 / 80.0) - phase * 1.6;
                    Location pt = base.clone().add(Math.cos(a) * innerRad, 0.1, Math.sin(a) * innerRad);
                    pt.getWorld().spawnParticle(
                        Particle.DUST, pt, 1, 0, 0, 0, 0,
                        (Object) new Particle.DustOptions(Color.fromRGB(220, 120, 255), 1.0f)
                    );
                }

                // === CEILING RING: matching ring at the top of the field ===
                for (int i = 0; i < 120; i++) {
                    double a = (i * Math.PI * 2.0 / 120.0) - phase * 0.7;
                    Location pt = base.clone().add(Math.cos(a) * radius, 4.6, Math.sin(a) * radius);
                    pt.getWorld().spawnParticle(
                        Particle.DUST, pt, 1, 0, 0, 0, 0,
                        (Object) new Particle.DustOptions(Color.fromRGB(140, 40, 200), 1.2f)
                    );
                }

                // === ENERGY TENDRILS: vertical pillars connecting ground to ceiling ===
                for (int i = 0; i < 30; i++) {
                    double a = r.nextDouble() * Math.PI * 2;
                    double rr = radius * (0.88 + r.nextDouble() * 0.18);
                    double y = r.nextDouble() * 4.6;
                    Location pt = base.clone().add(Math.cos(a) * rr, y, Math.sin(a) * rr);
                    pt.getWorld().spawnParticle(
                        Particle.DUST, pt, 1, 0, 0, 0, 0,
                        (Object) new Particle.DustOptions(Color.fromRGB(190 + r.nextInt(40), 80 + r.nextInt(50), 255), 1.1f)
                    );
                }

                // === ELECTRIC SPARKS: END_ROD sparks crackling around the field ===
                if (this.t % 3 == 0) {
                    for (int i = 0; i < 7; i++) {
                        double a = r.nextDouble() * Math.PI * 2;
                        double rr = radius * (0.85 + r.nextDouble() * 0.3);
                        double y = r.nextDouble() * 4.6;
                        Location spt = base.clone().add(Math.cos(a) * rr, y, Math.sin(a) * rr);
                        spt.getWorld().spawnParticle(Particle.END_ROD, spt, 2, 0.04, 0.04, 0.04, 0.025);
                    }
                }

                // === LEVITATION PORTALS: inside field, float upward ===
                for (int i = 0; i < 22; i++) {
                    double a = r.nextDouble() * Math.PI * 2;
                    double rr = r.nextDouble() * radius * 0.85;
                    Location pt = base.clone().add(Math.cos(a) * rr, r.nextDouble() * 4.0, Math.sin(a) * rr);
                    pt.getWorld().spawnParticle(Particle.PORTAL, pt, 1, 0, 0.08, 0, 0.22);
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L);
    }

    private void earthAbility1(Player p) {
        Material mat;
        Location loc = p.getLocation();
        Vector dir = loc.getDirection();
        Vector facing = new Vector(dir.getX(), 0.0, dir.getZ()).normalize();
        Vector right = new Vector(-facing.getZ(), 0.0, facing.getX());
        final ArrayList<Block> wall = new ArrayList<Block>();
        Location base = loc.clone().add(facing.multiply(2));
        base.setY(loc.getY());
        int width = this.cfgI("daggers.earth.ability1.width", 4);
        int height = this.cfgI("daggers.earth.ability1.height", 3);
        try {
            mat = Material.valueOf((String)this.plugin.getConfig().getString("daggers.earth.ability1.material", "OBSIDIAN"));
        }
        catch (Exception ex) {
            mat = Material.OBSIDIAN;
        }
        int half = width / 2;
        for (int col = -half; col < width - half; ++col) {
            for (int row = 0; row < height; ++row) {
                Location bl = base.clone().add(right.clone().multiply(col)).add(0.0, (double)row, 0.0);
                Block b = bl.getBlock();
                if (!b.getType().isAir()) continue;
                b.setType(mat);
                wall.add(b);
                // Eruption burst at each block as it places
                Location bc = bl.clone().add(0.5, 0.5, 0.5);
                bc.getWorld().spawnParticle(Particle.BLOCK, bc, 10, 0.3, 0.3, 0.3, 0.08, mat.createBlockData());
                if (row == 0) {
                    bc.getWorld().spawnParticle(Particle.LARGE_SMOKE, bc, 4, 0.2, 0.1, 0.2, 0.015);
                }
            }
        }
        // Seismic ground crack at the wall base: dust + debris burst
        Location midWall = base.clone().add(right.clone().multiply((width / 2.0) - 0.5)).add(0, 0.3, 0);
        midWall.getWorld().spawnParticle(Particle.LARGE_SMOKE, midWall, 25, (width * 0.5), 0.2, 0.4, 0.04);
        midWall.getWorld().spawnParticle(Particle.BLOCK, midWall, 50, (width * 0.55), 0.4, 0.4, 0.12, mat.createBlockData());
        midWall.getWorld().spawnParticle(Particle.EXPLOSION, midWall, 4, (width * 0.3), 0.1, 0.3, 0.0);
        final UUID uuid = p.getUniqueId();
        for (Block b : this.earthWalls.getOrDefault(uuid, new ArrayList<Block>())) {
            b.setType(Material.AIR);
        }
        this.earthWalls.put(uuid, wall);
        int durTicks = this.cfgTicks("daggers.earth.ability1.duration-seconds", 10.0);
        new BukkitRunnable(){

            public void run() {
                for (Block b : wall) {
                    b.setType(Material.AIR);
                }
                AbilityManager.this.earthWalls.remove(uuid);
            }
        }.runTaskLater((Plugin)this.plugin, (long)durTicks);
    }

    private void earthAbility2(final Player p) {
        final Vector launchDir = p.getEyeLocation().getDirection().normalize();
        final Location spawnLoc = p.getEyeLocation().add(launchDir.clone().multiply(2));
        final double launchSpeed = this.cfgD("daggers.earth.ability2.launch-speed", 2.4);
        // Straight-line shot: no upward Y boost. Gravity will pull it down naturally over distance.
        Vector vel = launchDir.clone().multiply(launchSpeed);
        final double dmg = this.cfgD("daggers.earth.ability2.boulder-damage", 12.0);
        final double radius = this.cfgD("daggers.earth.ability2.radius", 7.0);
        final float boulderScale = (float) this.cfgD("daggers.earth.ability2.scale", 3.0);
        final FallingBlock boulder = p.getWorld().spawnFallingBlock(spawnLoc, Material.COBBLESTONE.createBlockData());
        boulder.setVelocity(vel);
        boulder.setDropItem(false);
        boulder.setHurtEntities(true);
        // Reduce gravity so the trajectory has only a SLIGHT downward arc instead of a high arc.
        try { boulder.setGravity(true); } catch (Throwable ignored) {}
        boulder.setMetadata("dagger_earth_boulder", (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)p.getUniqueId().toString()));
        final org.bukkit.entity.BlockDisplay display = (org.bukkit.entity.BlockDisplay) p.getWorld().spawnEntity(spawnLoc, EntityType.BLOCK_DISPLAY);
        display.setBlock(Material.COBBLESTONE.createBlockData());
        org.bukkit.util.Transformation tf = display.getTransformation();
        tf = new org.bukkit.util.Transformation(
            new org.joml.Vector3f(-boulderScale * 0.5f, -boulderScale * 0.5f, -boulderScale * 0.5f),
            tf.getLeftRotation(),
            new org.joml.Vector3f(boulderScale, boulderScale, boulderScale),
            tf.getRightRotation()
        );
        display.setTransformation(tf);
        p.getWorld().playSound(spawnLoc, "daggersmp:ability.earth.boulder", 1.4f, 0.7f);
        new BukkitRunnable(){
            int ticks = 0;
            public void run() {
                ++this.ticks;
                boolean entityHit = false;
                if (boulder.isValid()) {
                    Location bl = boulder.getLocation();
                    display.teleport(bl);
                    // Rock debris cloud trailing behind the boulder
                    bl.getWorld().spawnParticle(Particle.BLOCK, bl.clone().add(0, 0.5, 0), 30, boulderScale * 0.45, boulderScale * 0.45, boulderScale * 0.45, 0.05, Material.COBBLESTONE.createBlockData());
                    bl.getWorld().spawnParticle(Particle.BLOCK, bl.clone().add(0, 0.5, 0), 14, boulderScale * 0.35, boulderScale * 0.35, boulderScale * 0.35, 0.03, Material.GRAVEL.createBlockData());
                    bl.getWorld().spawnParticle(Particle.LARGE_SMOKE, bl, 12, 0.35, 0.3, 0.35, 0.025);
                    // Dust cloud billowing just behind the boulder
                    bl.getWorld().spawnParticle(Particle.DUST, bl.clone().add(0, 1.0, 0), 6, boulderScale * 0.3, boulderScale * 0.3, boulderScale * 0.3, 0,
                        (Object) new Particle.DustOptions(Color.fromRGB(140, 110, 80), 2.2f));
                    // Detonate on direct entity contact mid-flight.
                    for (Entity near : bl.getWorld().getNearbyEntities(bl, 1.5, 1.5, 1.5)) {
                        if (near == p || near == boulder || near == display || !(near instanceof LivingEntity)) continue;
                        if (AbilityManager.this.isTrustedEntity(p, near)) continue;
                        entityHit = true;
                        break;
                    }
                }
                if (this.ticks > 200 || !boulder.isValid() || entityHit) {
                    Location impact = boulder.isValid() ? boulder.getLocation() : spawnLoc;
                    boulder.remove();
                    display.remove();
                    impact.getWorld().createExplosion(impact, (float) Math.min(4.0, radius * 0.5), false, false, p);
                    // === IMPACT: multi-layer crater explosion ===
                    // Giant emitter + clustered smaller explosions
                    impact.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, impact, 12, 1.2, 0.6, 1.2, 0.0);
                    impact.getWorld().spawnParticle(Particle.EXPLOSION, impact, 120, radius * 0.45, 0.6, radius * 0.45, 0.0);
                    // Dense cobblestone + gravel debris burst
                    impact.getWorld().spawnParticle(Particle.BLOCK, impact, 280, radius * 0.55, 0.8, radius * 0.55, 0.15, Material.COBBLESTONE.createBlockData());
                    impact.getWorld().spawnParticle(Particle.BLOCK, impact, 120, radius * 0.4, 0.6, radius * 0.4, 0.12, Material.GRAVEL.createBlockData());
                    impact.getWorld().spawnParticle(Particle.BLOCK, impact, 80, radius * 0.35, 0.5, radius * 0.35, 0.10, Material.DIRT.createBlockData());
                    // Dirt dust billowing outward
                    impact.getWorld().spawnParticle(Particle.DUST, impact.clone().add(0, 0.5, 0), 50, radius * 0.5, 0.4, radius * 0.5, 0,
                        (Object) new Particle.DustOptions(Color.fromRGB(130, 100, 70), 3.0f));
                    // Shockwave ring of stones at ground level
                    for (int ri = 0; ri < 40; ri++) {
                        double ra = ri * Math.PI * 2.0 / 40.0;
                        Location rpt = impact.clone().add(Math.cos(ra) * radius * 0.7, 0.2, Math.sin(ra) * radius * 0.7);
                        rpt.getWorld().spawnParticle(Particle.BLOCK, rpt, 6, 0.15, 0.1, 0.15, 0.08, Material.COBBLESTONE.createBlockData());
                    }
                    // Smoke column rising from crater
                    impact.getWorld().spawnParticle(Particle.LARGE_SMOKE, impact, 60, radius * 0.3, 0.3, radius * 0.3, 0.05);
                    impact.getWorld().playSound(impact, "daggersmp:ability.earth.boulder", 2.0f, 0.7f);
                    for (Entity e : impact.getWorld().getNearbyEntities(impact, radius, radius, radius)) {
                        LivingEntity le;
                        if (!(e instanceof LivingEntity) || (le = (LivingEntity)e) == p || AbilityManager.this.isTrustedEntity(p, e)) continue;
                        le.damage(dmg, (Entity)p);
                        Vector kb = le.getLocation().toVector().subtract(impact.toVector()).normalize().multiply(1.2).setY(0.6);
                        le.setVelocity(kb);
                    }
                    this.cancel();
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L);
    }

    private void titanAbility1(final Player p) {
        p.setMetadata("dagger_titan_grow_next", (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)true));
        // === GIANT CHARGE: earth-shaking rumble ring + stone dust plume ===
        Location loc = p.getLocation();
        for (int i = 0; i < 40; i++) {
            double a = i * Math.PI * 2.0 / 40.0;
            double r = 0.8 + this.random.nextDouble() * 0.4;
            Location pt = loc.clone().add(Math.cos(a) * r, 0.15, Math.sin(a) * r);
            pt.getWorld().spawnParticle(Particle.BLOCK, pt, 3, 0.1, 0.2, 0.1, 0.05,
                (Object) Material.STONE.createBlockData());
        }
        loc.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc.clone().add(0, 0.2, 0), 16, 0.6, 0.1, 0.6, 0.03);
        loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 1.5, 0), 14, 0.4, 0.5, 0.4, 0,
            (Object) new Particle.DustOptions(Color.fromRGB(180, 140, 100), 1.8f));
        new BukkitRunnable(){
            int ticks = 0;

            public void run() {
                this.ticks += 10;
                if (!p.isOnline() || this.ticks > 600) {
                    p.removeMetadata("dagger_titan_grow_next", (Plugin)AbilityManager.this.plugin);
                    this.cancel();
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 10L, 10L);
    }

    private void titanAbility2(Player p) {
        long ticks = this.cfgTicks("daggers.titan.ability2.duration-seconds", 5.0);
        double scale = this.cfgD("daggers.titan.ability2.scale", 0.4);
        // === SHRINK: implosion of dust spiraling inward ===
        Location loc = p.getLocation();
        for (int i = 0; i < 60; i++) {
            double a = i * Math.PI * 2.0 / 60.0;
            double r = 1.2 + this.random.nextDouble() * 0.6;
            double y = 0.4 + this.random.nextDouble() * 1.6;
            Location pt = loc.clone().add(Math.cos(a) * r, y, Math.sin(a) * r);
            pt.getWorld().spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, 0,
                (Object) new Particle.DustOptions(Color.fromRGB(200, 220, 255), 1.2f));
        }
        loc.getWorld().spawnParticle(Particle.FLASH, loc.clone().add(0, 1, 0), 1, 0, 0, 0, 0);
        loc.getWorld().spawnParticle(Particle.POOF, loc.clone().add(0, 1, 0), 20, 0.4, 0.5, 0.4, 0.04);
        this.scalePlayer(p, scale, ticks);
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int)ticks, this.cfgI("daggers.titan.ability2.speed-amplifier", 2)));
    }

    private void scalePlayer(final Player player, double scale, long durationTicks) {
        final AttributeInstance attr = player.getAttribute(Attribute.SCALE);
        if (attr == null) {
            return;
        }
        final NamespacedKey key = new NamespacedKey("daggersmp", "titan_scale");
        for (AttributeModifier m : new ArrayList<AttributeModifier>(attr.getModifiers())) {
            if (!m.getKey().equals((Object)key)) continue;
            attr.removeModifier(m);
        }
        double base = attr.getBaseValue();
        attr.addModifier(new AttributeModifier(key, scale - base, AttributeModifier.Operation.ADD_NUMBER));
        new BukkitRunnable(){

            public void run() {
                if (!player.isOnline()) {
                    return;
                }
                for (AttributeModifier m : new ArrayList<AttributeModifier>(attr.getModifiers())) {
                    if (!m.getKey().equals((Object)key)) continue;
                    attr.removeModifier(m);
                }
            }
        }.runTaskLater((Plugin)this.plugin, durationTicks);
    }

    private void guardianAbility1(final Player p) {
        UUID uuid = p.getUniqueId();
        if (this.guardianBeamTasks.containsKey(uuid)) {
            return;
        }
        final double dmgPerSec = this.cfgD("daggers.guardian.ability1.beam-damage-per-tick", 2.0);
        final double knock = this.cfgD("daggers.guardian.ability1.knockback", 0.12);
        final long maxTicks = this.cfgTicks("daggers.guardian.ability1.duration-seconds", 8.0);
        final double maxRange = this.cfgD("daggers.guardian.ability1.range", 30.0);
        p.getWorld().playSound(p.getLocation(), "daggersmp:ability.guardian.beam", 2.0f, 1.0f);
        BukkitRunnable beam = new BukkitRunnable(){
            int ticks = 0;
            public void run() {
                ++this.ticks;
                if (!p.isOnline() || (long) this.ticks > maxTicks) {
                    AbilityManager.this.endGuardianBeam(p);
                    this.cancel();
                    return;
                }
                // Periodic re-trigger of the guardian "fire" sound for the locked-on feel.
                if (this.ticks % 30 == 1) {
                    p.getWorld().playSound(p.getLocation(), "daggersmp:ability.guardian.beam", 1.5f, 1.0f);
                }
                Location start = p.getEyeLocation();
                Vector dir = start.getDirection().normalize();
                // Walk the ray and draw a thick, vanilla-guardian-style beam (purple core + bubble trail).
                double traveled = 0.0;
                Set<Entity> hitThisTick = new HashSet<Entity>();
                Particle.DustOptions corePurple = new Particle.DustOptions(Color.fromRGB(190, 60, 255), 1.6f);
                Particle.DustOptions edgeCyan = new Particle.DustOptions(Color.fromRGB(120, 220, 255), 1.0f);
                for (double d = 0.0; d < maxRange; d += 0.25) {
                    Location pt = start.clone().add(dir.clone().multiply(d));
                    if (!pt.getBlock().isPassable()) break;
                    traveled = d;
                    // Dense purple core
                    pt.getWorld().spawnParticle(Particle.DUST, pt, 4, 0.04, 0.04, 0.04, (Object) corePurple);
                    // Cyan outer glow
                    pt.getWorld().spawnParticle(Particle.DUST, pt, 2, 0.10, 0.10, 0.10, (Object) edgeCyan);
                    // Vanilla guardian attack uses bubbles -- we trail them along the path
                    if ((this.ticks + (int)(d * 4)) % 2 == 0) {
                        pt.getWorld().spawnParticle(Particle.BUBBLE, pt, 2, 0.05, 0.05, 0.05, 0.0);
                    }
                    if ((this.ticks + (int)(d * 4)) % 6 == 0) {
                        pt.getWorld().spawnParticle(Particle.END_ROD, pt, 1, 0.0, 0.0, 0.0, 0.0);
                    }
                    if (this.ticks % 5 == 0) {
                        for (Entity e : pt.getWorld().getNearbyEntities(pt, 0.8, 0.8, 0.8)) {
                            if (e == p || hitThisTick.contains(e) || !(e instanceof LivingEntity)) continue;
                            if (AbilityManager.this.isTrustedEntity(p, e)) continue;
                            LivingEntity le = (LivingEntity) e;
                            le.damage(dmgPerSec, (Entity) p);
                            Vector cur = e.getVelocity();
                            Vector kb = e.getLocation().toVector().subtract(p.getLocation().toVector()).normalize().multiply(knock);
                            kb.setY(0.05);
                            e.setVelocity(cur.add(kb));
                            hitThisTick.add(e);
                        }
                    }
                }
                Location end = start.clone().add(dir.clone().multiply(traveled));
                end.getWorld().spawnParticle(Particle.FLASH, end, 1, 0.0, 0.0, 0.0, 0.0);
                end.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, end, 6, 0.2, 0.2, 0.2, 0.01);
            }
        };
        beam.runTaskTimer((Plugin)this.plugin, 0L, 1L);
        this.guardianBeamTasks.put(uuid, beam);
        p.setMetadata("dagger_guardian_beam_active", (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)true));
    }

    public void cancelGuardianBeamIfHit(Player p) {
        this.endGuardianBeam(p);
    }

    private void endGuardianBeam(Player p) {
        BukkitRunnable t = this.guardianBeamTasks.remove(p.getUniqueId());
        if (t != null) {
            t.cancel();
        }
        if (p.isOnline()) {
            p.removeMetadata("dagger_guardian_beam_active", (Plugin)this.plugin);
        }
    }

    private void guardianAbility2(Player p) {
        double radius = this.cfgD("daggers.guardian.ability2.radius", 5.0);
        int dur = this.cfgTicks("daggers.guardian.ability2.duration-seconds", 10.0);
        int amp = this.cfgI("daggers.guardian.ability2.mining-fatigue-amplifier", 1);
        // === FATIGUE PULSE: heavy stone crack shockwave ===
        Location loc = p.getLocation();
        new BukkitRunnable() {
            int t = 0;
            public void run() {
                if (++t > 16) { this.cancel(); return; }
                double waveR = radius * t / 16.0;
                for (int i = 0; i < 40; i++) {
                    double a = i * Math.PI * 2.0 / 40.0;
                    Location pt = loc.clone().add(Math.cos(a) * waveR, 0.15, Math.sin(a) * waveR);
                    pt.getWorld().spawnParticle(Particle.BLOCK, pt, 2, 0.05, 0.1, 0.05, 0.04,
                        (Object) Material.STONE.createBlockData());
                    if (i % 5 == 0) pt.getWorld().spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, 0,
                        (Object) new Particle.DustOptions(Color.fromRGB(120, 120, 140), 1.3f));
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L);
        for (Entity e : p.getNearbyEntities(radius, radius, radius)) {
            LivingEntity le;
            if (!(e instanceof LivingEntity) || (le = (LivingEntity)e) == p || this.isTrustedEntity(p, e)) continue;
            le.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, dur, amp));
            // Debuff puff on each hit target
            le.getWorld().spawnParticle(Particle.BLOCK, le.getLocation().clone().add(0, 1, 0), 16, 0.3, 0.5, 0.3, 0.04,
                (Object) Material.STONE.createBlockData());
        }
    }

    private void ghostAbility1(final Player p) {
        double dist = this.cfgD("daggers.ghost.ability1.dash-blocks", 10.0);
        final Vector dir = p.getLocation().getDirection().normalize();
        double power = Math.max(1.5, dist * 0.18);
        p.setVelocity(new Vector(dir.getX() * power, Math.max(0.25, p.getVelocity().getY()), dir.getZ() * power));
        p.setFallDistance(0.0f);
        this.windFallDamageImmune.add(p.getUniqueId());
        p.getWorld().playSound(p.getLocation(), "daggersmp:ability.ghost.dash", 1.2f, 1.5f);

        // === DASH LAUNCH BURST: soul ring at origin ===
        Location launchLoc = p.getLocation().clone();
        for (int i = 0; i < 40; i++) {
            double a = i * Math.PI * 2.0 / 40.0;
            double r = 0.6 + this.random.nextDouble() * 0.35;
            Location bpt = launchLoc.clone().add(Math.cos(a) * r, 0.9 + this.random.nextDouble() * 0.7, Math.sin(a) * r);
            launchLoc.getWorld().spawnParticle(Particle.SOUL, bpt, 1, 0.04, 0.08, 0.04, 0.015);
            if (i % 4 == 0) {
                launchLoc.getWorld().spawnParticle(
                    Particle.DUST, bpt, 1, 0, 0, 0, 0,
                    (Object) new Particle.DustOptions(Color.fromRGB(215, 238, 255), 1.3f)
                );
            }
        }
        launchLoc.getWorld().spawnParticle(Particle.END_ROD, launchLoc.clone().add(0, 1.2, 0), 6, 0.3, 0.4, 0.3, 0.06);

        new BukkitRunnable() {
            int t = 0;
            Location prevLoc = p.getLocation().clone();
            public void run() {
                if (++t > 18 || !p.isOnline()) {
                    AbilityManager.this.windFallDamageImmune.remove(p.getUniqueId());
                    this.cancel();
                    return;
                }
                Location cur = p.getLocation().clone();
                java.util.Random r = AbilityManager.this.random;

                // Dense spectral trail interpolated between previous and current position
                for (int k = 0; k < 4; k++) {
                    double frac = k / 4.0;
                    double tx = prevLoc.getX() + (cur.getX() - prevLoc.getX()) * frac;
                    double ty = prevLoc.getY() + (cur.getY() - prevLoc.getY()) * frac;
                    double tz = prevLoc.getZ() + (cur.getZ() - prevLoc.getZ()) * frac;
                    Location tpt = new Location(cur.getWorld(), tx, ty + 1.0, tz);
                    cur.getWorld().spawnParticle(Particle.SOUL, tpt, 3, 0.14, 0.2, 0.14, 0.012);
                    cur.getWorld().spawnParticle(Particle.CLOUD, tpt, 2, 0.1, 0.14, 0.1, 0.015);
                    cur.getWorld().spawnParticle(
                        Particle.DUST, tpt, 1, 0.08, 0.12, 0.08, 0,
                        (Object) new Particle.DustOptions(Color.fromRGB(200, 228, 255), 0.95f)
                    );
                }

                // Spectral afterimage ring left behind every 4 ticks
                if (t % 4 == 0) {
                    for (int i = 0; i < 20; i++) {
                        double a = i * Math.PI * 2.0 / 20.0;
                        Location rpt = cur.clone().add(Math.cos(a) * 0.5, 1.0, Math.sin(a) * 0.5);
                        rpt.getWorld().spawnParticle(
                            Particle.DUST, rpt, 1, 0, 0, 0, 0,
                            (Object) new Particle.DustOptions(Color.fromRGB(175, 218, 255), 0.8f)
                        );
                    }
                    cur.getWorld().spawnParticle(Particle.END_ROD, cur.clone().add(0, 1.6, 0), 4, 0.25, 0.3, 0.25, 0.04);
                }
                prevLoc = cur.clone();
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L);
    }

    private void ghostAbility2(final Player p) {
        long ticks = this.cfgTicks("daggers.ghost.ability2.flight-duration-seconds", 8.0);
        final UUID uuid = p.getUniqueId();
        final org.bukkit.GameMode prevMode = p.getGameMode();
        this.ghostFormActive.add(uuid);
        p.setGameMode(org.bukkit.GameMode.SPECTATOR);
        p.setInvisible(true);
        p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, (int) ticks + 20, 0, false, false, false));
        p.setMetadata("dagger_ghost_noclip", (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)true));
        p.getWorld().playSound(p.getLocation(), "daggersmp:ability.ghost.dash", 1.0f, 0.6f);
        final BukkitRunnable trail = new BukkitRunnable(){
            int t = 0;
            public void run() {
                if (!p.isOnline()) { this.cancel(); return; }
                this.t++;
                java.util.Random r = AbilityManager.this.random;
                Location loc = p.getLocation();
                // Dense soul particle aura orbiting the ghost form
                for (int i = 0; i < 8; i++) {
                    double a = r.nextDouble() * Math.PI * 2;
                    double rad = 0.4 + r.nextDouble() * 0.55;
                    double y = 0.4 + r.nextDouble() * 1.6;
                    Location sp = loc.clone().add(Math.cos(a) * rad, y, Math.sin(a) * rad);
                    sp.getWorld().spawnParticle(Particle.SOUL, sp, 1, 0.04, 0.08, 0.04, 0.008);
                }
                // Ice-blue shimmer wisps scattered around body
                for (int i = 0; i < 6; i++) {
                    Location sp = loc.clone().add(
                        (r.nextDouble() - 0.5) * 0.9, r.nextDouble() * 2.1, (r.nextDouble() - 0.5) * 0.9);
                    sp.getWorld().spawnParticle(
                        Particle.DUST, sp, 1, 0, 0, 0, 0,
                        (Object) new Particle.DustOptions(Color.fromRGB(205, 228, 255), 1.1f)
                    );
                }
                // Spectral shroud ring + END_ROD glints every 8 ticks
                if (this.t % 8 == 0) {
                    for (int i = 0; i < 24; i++) {
                        double a = i * Math.PI * 2.0 / 24.0;
                        Location rpt = loc.clone().add(Math.cos(a) * 0.72, 1.0, Math.sin(a) * 0.72);
                        rpt.getWorld().spawnParticle(
                            Particle.DUST, rpt, 1, 0, 0, 0, 0,
                            (Object) new Particle.DustOptions(Color.fromRGB(185, 218, 255), 0.9f)
                        );
                    }
                    loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, 1.6, 0), 5, 0.38, 0.38, 0.38, 0.05);
                }
            }
        };
        trail.runTaskTimer((Plugin)this.plugin, 0L, 2L);
        new BukkitRunnable(){
            public void run() {
                AbilityManager.this.ghostFormActive.remove(uuid);
                trail.cancel();
                if (!p.isOnline()) return;
                p.setInvisible(false);
                p.removePotionEffect(PotionEffectType.INVISIBILITY);
                p.setGameMode(prevMode);
                p.removeMetadata("dagger_ghost_noclip", (Plugin)AbilityManager.this.plugin);
            }
        }.runTaskLater((Plugin)this.plugin, ticks);
    }

    private void chanceAbility1(final Player player) {
        int slot = -1;
        for (int i = 0; i < player.getInventory().getSize(); ++i) {
            if (DaggerType.fromItem(player.getInventory().getItem(i)) != DaggerType.CHANCE) continue;
            slot = i;
            break;
        }
        if (slot < 0) {
            return;
        }
        long ms = (long)(this.cfgD("daggers.chance.ability1.duration-seconds", 20.0) * 1000.0);
        final int fslot = slot;
        player.getInventory().setItem(slot, null);
        ArrayList<DaggerType> valid = new ArrayList<DaggerType>();
        for (DaggerType t : DaggerType.values()) {
            if (t == DaggerType.CHANCE) continue;
            valid.add(t);
        }
        final DaggerType picked = (DaggerType)((Object)valid.get(this.random.nextInt(valid.size())));
        player.getInventory().setItem(slot, picked.createItem());
        // === CHANCE ROLL: rainbow explosion as dagger morphs ===
        Location cloc = player.getLocation();
        int[][] cols = {{255,50,50},{255,160,0},{255,255,0},{50,220,50},{50,150,255},{130,50,255},{255,100,200}};
        for (int ci = 0; ci < 72; ci++) {
            double a = ci * Math.PI * 2.0 / 72.0;
            double r = 0.5 + this.random.nextDouble() * 0.8;
            double y = this.random.nextDouble() * 2.0;
            Location cpt = cloc.clone().add(Math.cos(a) * r, y, Math.sin(a) * r);
            int[] c = cols[ci % cols.length];
            cpt.getWorld().spawnParticle(Particle.DUST, cpt, 1, 0, 0, 0, 0,
                (Object) new Particle.DustOptions(Color.fromRGB(c[0], c[1], c[2]), 1.4f));
        }
        cloc.getWorld().spawnParticle(Particle.FLASH, cloc.clone().add(0, 1.2, 0), 1, 0, 0, 0, 0);
        cloc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, cloc.clone().add(0, 1.2, 0), 20, 0.4, 0.5, 0.4, 0.08);
        final UUID uuid = player.getUniqueId();
        this.chanceActiveDagger.put(uuid, picked);
        this.chanceEndTime.put(uuid, System.currentTimeMillis() + ms);
        new BukkitRunnable(){

            public void run() {
                if (!player.isOnline()) {
                    return;
                }
                AbilityManager.this.chanceActiveDagger.remove(uuid);
                AbilityManager.this.chanceEndTime.remove(uuid);
                // Restore the original CHANCE dagger to the slot the transformed item is currently in.
                int restoreSlot = -1;
                ItemStack atOriginal = player.getInventory().getItem(fslot);
                if (DaggerType.fromItem(atOriginal) == picked) {
                    restoreSlot = fslot;
                } else {
                    // Player may have moved the transformed item; find it anywhere in the inventory.
                    for (int i = 0; i < player.getInventory().getSize(); ++i) {
                        if (DaggerType.fromItem(player.getInventory().getItem(i)) == picked) {
                            restoreSlot = i;
                            break;
                        }
                    }
                }
                if (restoreSlot >= 0) {
                    player.getInventory().setItem(restoreSlot, DaggerType.CHANCE.createItem());
                } else {
                    // Transformed dagger was lost (dropped/destroyed). Drop a Chance Dagger at the player so they don't lose it permanently.
                    player.getWorld().dropItemNaturally(player.getLocation(), DaggerType.CHANCE.createItem());
                }
            }
        }.runTaskLater((Plugin)this.plugin, ms / 50L);
    }

    private void stormAbility1(Player p) {
        Entity tgt = this.getNearestTarget(p, this.cfgD("daggers.storm.ability1.range", 30.0));
        if (tgt == null) {
            return;
        }
        double dmg = this.cfgD("daggers.storm.ability1.damage", 6.0);
        // === TARGETED BOLT: crackling arc traveling from caster eye to target ===
        Location src = p.getEyeLocation();
        Location dst = tgt.getLocation().clone().add(0, 1, 0);
        Vector step = dst.toVector().subtract(src.toVector());
        double dist = step.length();
        if (dist > 0.5) {
            step.normalize();
            for (double d = 0; d < dist; d += 0.5) {
                Location pt = src.clone().add(step.clone().multiply(d));
                // Jitter for electric feel
                pt.add(this.random.nextGaussian() * 0.12, this.random.nextGaussian() * 0.12, this.random.nextGaussian() * 0.12);
                pt.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, pt, 2, 0.04, 0.04, 0.04, 0.06);
                if ((int)(d * 2) % 2 == 0)
                    pt.getWorld().spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, 0,
                        (Object) new Particle.DustOptions(Color.fromRGB(200, 230, 255), 1.0f));
            }
        }
        // Impact flash at target
        tgt.getWorld().spawnParticle(Particle.FLASH, dst, 1, 0, 0, 0, 0);
        tgt.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, dst, 24, 0.5, 0.5, 0.5, 0.15);
        tgt.getWorld().strikeLightning(tgt.getLocation());
        if (tgt instanceof LivingEntity) {
            LivingEntity le = (LivingEntity)tgt;
            le.damage(dmg, (Entity)p);
        }
    }

    private void stormAbility2(final Player p) {
        final double dmg = this.cfgD("daggers.storm.ability2.damage", 4.0);
        final double radius = this.cfgD("daggers.storm.ability2.radius", 5.0);
        final double aoeRadius = this.cfgD("daggers.storm.ability2.aoe-radius", 3.5);
        final double durSec = this.cfgD("daggers.storm.ability2.duration-seconds", 5.0);
        final long boltIntervalTicks = (long) this.cfgD("daggers.storm.ability2.bolt-interval-ticks", 8.0);
        final Location anchor = p.getLocation().clone();
        p.getWorld().playSound(anchor, "daggersmp:ability.storm.bolt", 1.5f, 1.0f);

        // === STORM ACTIVATION: dramatic dark cloud formation ===
        Location stormCenter = p.getLocation().clone().add(0, 8, 0);
        for (int i = 0; i < 80; i++) {
            double a = this.random.nextDouble() * Math.PI * 2;
            double r = this.random.nextDouble() * radius;
            Location cpt = stormCenter.clone().add(Math.cos(a) * r, (this.random.nextDouble() - 0.5) * 2.0, Math.sin(a) * r);
            p.getWorld().spawnParticle(Particle.CLOUD, cpt, 3, 0.6, 0.3, 0.6, 0.02);
        }
        for (int i = 0; i < 36; i++) {
            double a = i * Math.PI * 2.0 / 36.0;
            Location rpt = stormCenter.clone().add(Math.cos(a) * radius, 0, Math.sin(a) * radius);
            p.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, rpt, 4, 0.2, 0.2, 0.2, 0.15);
        }

        final long maxTicks = (long)(durSec * 20.0);
        new BukkitRunnable() {
            int ticks = 0;
            public void run() {
                this.ticks += (int) boltIntervalTicks;
                if (this.ticks > maxTicks) {
                    this.cancel();
                    return;
                }
                Location center = p.isOnline() ? p.getLocation() : anchor;

                // === STORM CLOUD AMBIANCE: dark clouds above the storm area ===
                Location cloudBase = center.clone().add(0, 9, 0);
                for (int i = 0; i < 20; i++) {
                    double a = AbilityManager.this.random.nextDouble() * Math.PI * 2;
                    double r = AbilityManager.this.random.nextDouble() * radius;
                    Location cpt = cloudBase.clone().add(Math.cos(a) * r, (AbilityManager.this.random.nextDouble() - 0.5) * 1.5, Math.sin(a) * r);
                    center.getWorld().spawnParticle(Particle.CLOUD, cpt, 2, 0.4, 0.2, 0.4, 0.015);
                }

                // === PRE-BOLT CRACKLE: electrical arcing before strike ===
                for (int i = 0; i < 12; i++) {
                    double a = AbilityManager.this.random.nextDouble() * Math.PI * 2;
                    double r = AbilityManager.this.random.nextDouble() * radius;
                    double y = 2.0 + AbilityManager.this.random.nextDouble() * 5.0;
                    Location apt = center.clone().add(Math.cos(a) * r, y, Math.sin(a) * r);
                    center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, apt, 3, 0.1, 0.2, 0.1, 0.12);
                }

                double angle = AbilityManager.this.random.nextDouble() * Math.PI * 2.0;
                double dist = AbilityManager.this.random.nextDouble() * radius;
                Location strike = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
                int sy = strike.getBlockY();
                for (int y = sy + 6; y >= sy - 8; --y) {
                    Location probe = new Location(strike.getWorld(), strike.getX(), y, strike.getZ());
                    if (!probe.getBlock().isPassable()) {
                        strike = new Location(strike.getWorld(), strike.getX(), y + 1, strike.getZ());
                        break;
                    }
                }
                org.bukkit.entity.LightningStrike bolt = strike.getWorld().strikeLightning(strike);
                if (bolt != null) {
                    bolt.setSilent(false);
                }
                // === POST-STRIKE SCORCH: ground sparks after lightning impact ===
                strike.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, strike, 60, 0.8, 0.5, 0.8, 0.25);
                strike.getWorld().spawnParticle(Particle.FLASH, strike, 2, 0.05, 0.05, 0.05, 0.0);
                strike.getWorld().spawnParticle(Particle.SMOKE, strike, 15, 0.4, 0.3, 0.4, 0.05);
                for (int i = 0; i < 24; i++) {
                    double a = i * Math.PI * 2.0 / 24.0;
                    Location rpt = strike.clone().add(Math.cos(a) * 1.2, 0.1, Math.sin(a) * 1.2);
                    rpt.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, rpt, 2, 0.05, 0.1, 0.05, 0.1);
                }

                for (Entity e : strike.getWorld().getNearbyEntities(strike, aoeRadius, aoeRadius + 1.5, aoeRadius)) {
                    if (!(e instanceof LivingEntity)) continue;
                    LivingEntity le = (LivingEntity) e;
                    if (le == p) continue;
                    if (AbilityManager.this.isTrustedEntity(p, e)) continue;
                    if (le instanceof org.bukkit.entity.ArmorStand) continue;
                    if (le.isDead() || !le.isValid()) continue;
                    if (le.hasMetadata("dagger_mafia_owner")) {
                        String owner = ((MetadataValue) le.getMetadata("dagger_mafia_owner").get(0)).asString();
                        if (owner.equals(p.getUniqueId().toString())) continue;
                    }
                    le.setNoDamageTicks(0);
                    le.damage(dmg, (Entity) p);
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, boltIntervalTicks);
    }

    public boolean hasDaggerAnywhere(Player player, DaggerType type) {
        if (DaggerType.fromItem(player.getInventory().getItemInMainHand()) == type) {
            return true;
        }
        if (DaggerType.fromItem(player.getInventory().getItemInOffHand()) == type) {
            return true;
        }
        for (ItemStack it : player.getInventory().getContents()) {
            if (DaggerType.fromItem(it) != type) continue;
            return true;
        }
        return false;
    }

    public boolean isHighestPrioritySlot(Player player, DaggerType type) {
        if (DaggerType.fromItem(player.getInventory().getItemInMainHand()) == type) {
            return true;
        }
        if (DaggerType.fromItem(player.getInventory().getItemInMainHand()) != null) {
            return false;
        }
        if (DaggerType.fromItem(player.getInventory().getItemInOffHand()) == type) {
            return true;
        }
        if (DaggerType.fromItem(player.getInventory().getItemInOffHand()) != null) {
            return false;
        }
        for (ItemStack it : player.getInventory().getContents()) {
            DaggerType t = DaggerType.fromItem(it);
            if (t == null) continue;
            return t == type;
        }
        return false;
    }

    private Entity getNearestTarget(Player player, double range) {
        Entity closest = null;
        double minDist = Double.MAX_VALUE;
        for (Entity e : player.getNearbyEntities(range, range, range)) {
            double d;
            if (e == player || !(e instanceof LivingEntity) || !((d = e.getLocation().distanceSquared(player.getLocation())) < minDist)) continue;
            minDist = d;
            closest = e;
        }
        return closest;
    }

    public boolean isTrustedEntity(Player player, Entity e) {
        if (e instanceof Player) {
            Player tp = (Player)e;
            return this.plugin.getTrustManager().isTrusted(player.getUniqueId(), tp.getUniqueId());
        }
        return false;
    }
}

