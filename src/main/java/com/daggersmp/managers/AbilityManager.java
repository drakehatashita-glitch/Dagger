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
        new BukkitRunnable(){

            public void run() {
                for (Player p : AbilityManager.this.plugin.getServer().getOnlinePlayers()) {
                    if (!AbilityManager.this.hasArachnidHeld(p)) continue;
                    if (!AbilityManager.this.isInCobweb(p)) continue;
                    Vector cur = p.getVelocity();
                    p.setVelocity(new Vector(cur.getX(), Math.max(cur.getY(), -0.05), cur.getZ()));
                    p.setFallDistance(0.0f);
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L);
        new BukkitRunnable(){

            public void run() {
                for (Player p : AbilityManager.this.plugin.getServer().getOnlinePlayers()) {
                    if (!AbilityManager.this.hasArachnidHeld(p) || !AbilityManager.this.isAgainstWall(p.getLocation())) continue;
                    if (p.isSneaking() || (p.getVelocity().getY() > 0.05 && !p.isOnGround())) {
                        double y = AbilityManager.this.cfgD("daggers.arachnid.passive.wallclimb-y-velocity", 0.42);
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
        if (this.hasDaggerAnywhere(p, DaggerType.VOID)) {
            this.addPerm(p, PotionEffectType.SPEED, this.cfgI("daggers.void.passive.speed-amplifier", 1));
            this.addPerm(p, PotionEffectType.STRENGTH, this.cfgI("daggers.void.passive.strength-amplifier", 0));
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
            player.sendMessage("\u00a7dYour Chance Dagger transformation has ended.");
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
        p.sendMessage("\u00a7dChance: \u00a77granted \u00a7d" + chosen.getKey().getKey() + " " + (amp + 1));
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
            p.sendMessage("\u00a7cAbility " + n + " on cooldown for \u00a7e" + rem + "s\u00a7c!");
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
                p.sendMessage("\u00a7cNo ability 1 for this dagger.");
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
                p.sendMessage("\u00a7cNo second ability for this dagger.");
            }
        }
    }

    private void strengthAbility1(Player p) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, this.cfgTicks("daggers.strength.ability1.duration-seconds", 3.0), this.cfgI("daggers.strength.ability1.amplifier", 2)));
        p.sendMessage("\u00a7cStrength activated!");
    }

    private void strengthAbility2(Player p) {
        p.setMetadata("dagger_strength_armor_break", (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)true));
        p.sendMessage("\u00a7cNext hit on a player will damage their armor durability!");
    }

    private void speedAbility1(Player p) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, this.cfgTicks("daggers.speed.ability1.duration-seconds", 7.0), this.cfgI("daggers.speed.ability1.amplifier", 4)));
        p.sendMessage("\u00a7bSpeed activated!");
    }

    private void speedAbility2(final Player p) {
        final AttributeInstance attr = p.getAttribute(Attribute.ATTACK_SPEED);
        if (attr == null) {
            p.sendMessage("\u00a7cAttack speed attribute unavailable.");
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
        p.sendMessage("\u00a7b+" + (int)(mult * 100.0) + "% attack speed for " + ticks / 20L + "s!");
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
        double dist = this.cfgD("daggers.wind.ability1.dash-blocks", 35.0);
        Vector dir = p.getLocation().getDirection().normalize().multiply(Math.max(2.0, dist * 0.12));
        p.setVelocity(new Vector(dir.getX(), Math.max(0.4, p.getVelocity().getY()), dir.getZ()));
        this.windFallDamageImmune.add(p.getUniqueId());
        p.playSound(p.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1.0f, 1.5f);
        p.sendMessage("\u00a7fWind dash!");
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
        p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1.0f, 1.5f);
        p.sendMessage("\u00a7fLeap! Shockwave on landing.");
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
                if (!p.isOnGround() && this.ticks > 5) {
                    this.wasAir = true;
                }
                if (this.wasAir && p.isOnGround()) {
                    AbilityManager.this.windFallDamageImmune.remove(uuid);
                    double dmg = AbilityManager.this.cfgD("daggers.wind.ability2.shockwave-damage", 6.0);
                    double radius = AbilityManager.this.cfgD("daggers.wind.ability2.shockwave-radius", 5.0);
                    Location loc = p.getLocation();
                    loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 10, radius * 0.4, 0.5, radius * 0.4, 0.0);
                    loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.5f);
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
        int hits = 0;
        UUID uuid = p.getUniqueId();
        Map sources = this.lifeStealActiveBonus.computeIfAbsent(uuid, k -> new HashMap());
        for (Entity e : p.getNearbyEntities(radius, radius, radius)) {
            if (!(e instanceof Player)) continue;
            Player tp = (Player)e;
            if (this.isTrustedEntity(p, e)) continue;
            tp.damage(hearts * 2.0, (Entity)p);
            sources.put(tp.getUniqueId(), System.currentTimeMillis() + durMs);
            ++hits;
        }
        if (hits == 0) {
            p.sendMessage("\u00a7cNo nearby players to steal from!");
            return;
        }
        this.updateLifeStealAttribute(p);
        AttributeInstance attr = p.getAttribute(Attribute.MAX_HEALTH);
        if (attr != null) {
            p.setHealth(Math.min(attr.getValue(), p.getHealth() + this.cfgD("daggers.life.ability1.bonus-hearts-per-player", 6.0)));
        }
        p.sendMessage("\u00a7aStole life from " + hits + " player(s)! +" + hits + " bonus hearts.");
    }

    private void lifeAbility2(Player p) {
        int dur = this.cfgTicks("daggers.life.ability2.duration-seconds", 5.0);
        int amp = this.cfgI("daggers.life.ability2.regen-amplifier", 1);
        double radius = this.cfgD("daggers.life.ability2.radius", 10.0);
        for (Entity e : p.getNearbyEntities(radius, radius, radius)) {
            Player ally;
            if (!(e instanceof Player) || (ally = (Player)e) == p || !this.plugin.getTrustManager().isTrusted(p.getUniqueId(), ally.getUniqueId())) continue;
            ally.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, dur, amp));
            ally.sendMessage("\u00a7a" + p.getName() + " granted you Regen " + (amp + 1) + "!");
        }
        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, dur, amp));
        p.sendMessage("\u00a7aGranted regen to nearby trusted allies!");
    }

    private void crimsonAbility1(Player p) {
        p.setMetadata("dagger_crimson_wither_next", (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)true));
        p.sendMessage("\u00a74Next hit will inflict Wither!");
    }

    private void crimsonAbility2(Player p) {
        Location loc = p.getEyeLocation();
        Vector dir = loc.getDirection().normalize();
        Fireball fb = (Fireball)p.getWorld().spawn(loc.add(dir), Fireball.class);
        fb.setDirection(dir.multiply(2));
        fb.setShooter((ProjectileSource)p);
        fb.setYield((float)this.cfgD("daggers.crimson.ability2.explosion-power", 2.0));
        fb.setMetadata("dagger_crimson_pierce_fireres", (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)p.getUniqueId().toString()));
        fb.setMetadata("dagger_crimson_damage", (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)this.cfgD("daggers.crimson.ability2.damage", 6.0)));
        p.sendMessage("\u00a74Fireball launched!");
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
        p.sendMessage("\u00a78Darkness erupts around you!");
    }

    private void darknessAbility2(final Player p) {
        final Location start = p.getEyeLocation();
        final Vector dir = start.getDirection().normalize();
        final double dmg = this.cfgD("daggers.darkness.ability2.beam-damage", 6.0);
        final double maxDist = this.cfgD("daggers.darkness.ability2.max-distance", 30.0);
        p.getWorld().playSound(start, Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 1.0f);
        new BukkitRunnable(){
            double dist = 0.0;
            final Set<Entity> hit = new HashSet<Entity>();

            public void run() {
                for (int i = 0; i < 5; ++i) {
                    this.dist += 0.5;
                    if (this.dist > maxDist) {
                        this.cancel();
                        return;
                    }
                    Location pt = start.clone().add(dir.clone().multiply(this.dist));
                    pt.getWorld().spawnParticle(Particle.SONIC_BOOM, pt, 0);
                    for (Entity e : pt.getWorld().getNearbyEntities(pt, 1.0, 1.0, 1.0)) {
                        LivingEntity le;
                        if (!(e instanceof LivingEntity) || (le = (LivingEntity)e) == p || this.hit.contains(e) || AbilityManager.this.isTrustedEntity(p, e)) continue;
                        le.damage(dmg, (Entity)p);
                        this.hit.add(e);
                    }
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L);
        p.sendMessage("\u00a78Sonic Beam fired!");
    }

    private void hackAbility1(Player p) {
        double radius = this.cfgD("daggers.hack.ability1.reveal-radius", 50.0);
        int dur = this.cfgTicks("daggers.hack.ability1.glow-duration-seconds", 30.0);
        int count = 0;
        for (Entity e : p.getNearbyEntities(radius, radius, radius)) {
            if (!(e instanceof LivingEntity)) continue;
            LivingEntity le = (LivingEntity)e;
            le.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, dur, 0));
            ++count;
        }
        p.sendMessage("\u00a72Revealed " + count + " entities (glowing for " + dur / 20 + "s)!");
    }

    private void hackAbility2(final Player p) {
        long ticks = this.cfgTicks("daggers.hack.ability2.hitbox-duration-seconds", 5.0);
        p.setMetadata("dagger_hack_hitbox", (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)true));
        p.sendMessage("\u00a72Player hitboxes effectively expanded for " + ticks / 20L + "s!");
        new BukkitRunnable(){

            public void run() {
                if (p.isOnline()) {
                    p.removeMetadata("dagger_hack_hitbox", (Plugin)AbilityManager.this.plugin);
                }
            }
        }.runTaskLater((Plugin)this.plugin, ticks);
    }

    private void frostAbility1(Player p) {
        double radius = this.cfgD("daggers.frost.ability1.radius", 5.0);
        int dur = this.cfgTicks("daggers.frost.ability1.freeze-duration-seconds", 5.0);
        for (Entity e : p.getNearbyEntities(radius, radius, radius)) {
            LivingEntity le;
            if (!(e instanceof LivingEntity) || (le = (LivingEntity)e) == p || this.isTrustedEntity(p, e)) continue;
            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, dur, this.cfgI("daggers.frost.ability1.slowness-amplifier", 6)));
            le.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, dur, this.cfgI("daggers.frost.ability1.mining-fatigue-amplifier", 2)));
            le.setFreezeTicks(dur);
            le.getWorld().spawnParticle(Particle.SNOWFLAKE, le.getLocation().add(0.0, 1.0, 0.0), 10, 0.3, 0.3, 0.3, 0.0);
        }
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_POWDER_SNOW_PLACE, 2.0f, 1.0f);
        p.sendMessage("\u00a73Nearby enemies frozen!");
    }

    private void frostAbility2(Player p) {
        int ticks = this.cfgTicks("daggers.frost.ability2.debuff-duration-seconds", 5.0);
        double radius = this.cfgD("daggers.frost.ability2.radius", 5.0);
        for (Entity e : p.getNearbyEntities(radius, radius, radius)) {
            if (!(e instanceof Player)) continue;
            Player tp = (Player)e;
            if (this.isTrustedEntity(p, e)) continue;
            tp.setFreezeTicks(ticks);
            tp.setMetadata("dagger_frost_debuff", (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)true));
            final Player ftp = tp;
            new BukkitRunnable(){

                public void run() {
                    if (ftp.isValid()) {
                        ftp.removeMetadata("dagger_frost_debuff", (Plugin)AbilityManager.this.plugin);
                    }
                }
            }.runTaskLater((Plugin)this.plugin, (long)ticks);
        }
        p.sendMessage("\u00a73Frost field deployed for " + ticks / 20 + "s!");
    }

    private void mafiaAbility1(Player p) {
        p.setMetadata("dagger_mafia_next_hit", (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)true));
        p.sendMessage("\u00a76Next hit applies poison, weakness, hunger!");
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
            Location spawnLoc = player.getLocation().add(Math.cos(angle) * 2.0, 0.0, Math.sin(angle) * 2.0);
            Vindicator v = (Vindicator)player.getWorld().spawnEntity(spawnLoc, EntityType.VINDICATOR);
            v.setCustomName(name);
            v.setCustomNameVisible(true);
            v.setMetadata("dagger_mafia_owner", (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)uuid.toString()));
            v.setTarget(null);
            vinds.add(v);
        }
        this.mafiaVindicators.put(uuid, new HashSet(vinds));
        player.sendMessage("\u00a76" + count + " " + name + "\u00a76s summoned \u2014 they only attack what you fight!");
    }

    public Set<Entity> getMafiaVindicators(UUID uuid) {
        return this.mafiaVindicators.getOrDefault(uuid, Collections.emptySet());
    }

    private void pirateAbility1(Player p) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, this.cfgTicks("daggers.pirate.ability1.duration-seconds", 15.0), this.cfgI("daggers.pirate.ability1.amplifier", 0)));
        p.sendMessage("\u00a79Dolphin's Grace activated!");
    }

    private void pirateAbility2(Player p) {
        double pushBlocks = this.cfgD("daggers.pirate.ability2.push-blocks", 25.0);
        double radius = this.cfgD("daggers.pirate.ability2.radius", 8.0);
        for (Entity e : p.getNearbyEntities(radius, radius, radius)) {
            LivingEntity le;
            if (!(e instanceof LivingEntity) || (le = (LivingEntity)e) == p || this.isTrustedEntity(p, e)) continue;
            Vector away = e.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0).normalize().multiply(pushBlocks * 0.1);
            le.setVelocity(away.setY(0.4));
        }
        p.getWorld().spawnParticle(Particle.SPLASH, p.getLocation(), 80, radius * 0.5, 0.5, radius * 0.5, 0.5);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_GENERIC_SPLASH, 1.5f, 0.8f);
        p.sendMessage("\u00a79Wave released!");
    }

    private void voidAbility1(Player p) {
        double dist = this.cfgD("daggers.void.ability1.teleport-blocks", 20.0);
        Vector dir = p.getLocation().getDirection().normalize();
        Location target = p.getLocation().clone();
        for (double d = 1.0; d <= dist; d += 0.5) {
            Location check = p.getLocation().clone().add(dir.clone().multiply(d));
            if (!check.getBlock().getType().isAir() && check.getBlock().getType().isSolid()) continue;
            target = check.clone();
        }
        p.teleport(target);
        p.getWorld().spawnParticle(Particle.PORTAL, p.getLocation(), 30, 0.5, 1.0, 0.5, 0.5);
        p.sendMessage("\u00a75Phased through!");
    }

    private void voidAbility2(final Player p) {
        final VoidStateManager vsm = this.plugin.getVoidStateManager();
        if (vsm.isInVoid(p.getUniqueId())) {
            this.returnFromVoid(p);
            return;
        }
        Location original = p.getLocation().clone();
        vsm.enterVoid(p.getUniqueId(), original);
        p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 99999, 0, false, false, false));
        p.setAllowFlight(true);
        p.setFlying(true);
        long returnSec = (long)this.cfgD("daggers.void.ability2.return-seconds", 300.0);
        p.sendMessage("\u00a75Saved position. Use Ability 2 again within " + returnSec + "s to return.");
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
                    p.sendMessage("\u00a75Your saved Void position has expired. (No teleport, no cooldown.)");
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
        p.sendMessage("\u00a75Returned to your saved state.");
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
        p.sendMessage("\u00a7eLucky! " + chosen.size() + " random buffs applied.");
    }

    private void mirrorAbility1(final Player p) {
        long ticks = this.cfgTicks("daggers.mirror.ability1.full-reflect-duration-seconds", 10.0);
        p.setMetadata("dagger_mirror_full_reflect", (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)true));
        p.sendMessage("\u00a77Damage reflection active for " + ticks / 20L + "s (" + (int)(this.cfgD("daggers.mirror.ability1.reflect-percent", 0.25) * 100.0) + "% reflected)!");
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
            p.sendMessage("\u00a7cNo untrusted player nearby!");
            return;
        }
        Location a = p.getLocation().clone();
        Location b = target.getLocation().clone();
        p.teleport(b);
        target.teleport(a);
        p.sendMessage("\u00a77Swapped positions with " + target.getName() + "!");
        target.sendMessage("\u00a77You were swapped by " + p.getName() + "!");
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
                p.sendMessage("\u00a7cNo grapple target within " + (int)range + " blocks!");
                return;
            }
            targetLoc = blockHit.getHitPosition().toLocation(p.getWorld());
        }
        p.sendMessage("\u00a72Grappling!");
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_FISHING_BOBBER_THROW, 1.0f, 1.2f);
        new BukkitRunnable(){
            int ticks = 0;

            public void run() {
                ++this.ticks;
                if (!p.isOnline() || (long)this.ticks > timeoutSec * 20L) {
                    p.sendMessage("\u00a7cGrapple cancelled.");
                    this.cancel();
                    return;
                }
                Location dest = targetEnt != null ? targetEnt.getLocation().add(0.0, 0.5, 0.0) : targetLoc;
                Vector to = dest.toVector().subtract(p.getLocation().toVector());
                double dist = to.length();
                if (dist < 1.5) {
                    p.setVelocity(new Vector(0.0, 0.2, 0.0));
                    p.sendMessage("\u00a72Grapple landed!");
                    this.cancel();
                    return;
                }
                Vector vel = to.normalize().multiply(speed);
                p.setVelocity(vel);
                p.setFallDistance(0.0f);
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L);
    }

    private void jungleAbility2(Player p) {
        double range = this.cfgD("daggers.jungle.ability2.range", 10.0);
        int poisonDur = this.cfgTicks("daggers.jungle.ability2.poison-duration-seconds", 5.0);
        int poisonAmp = this.cfgI("daggers.jungle.ability2.poison-amplifier", 0);
        int idAmp = this.cfgI("daggers.jungle.ability2.instant-damage-amplifier", 0);
        Entity target = this.getNearestTarget(p, range);
        if (target == null) {
            p.sendMessage("\u00a7cNo target!");
            return;
        }
        if (target instanceof LivingEntity) {
            LivingEntity le = (LivingEntity)target;
            le.teleport(p.getLocation());
            le.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, 1, idAmp));
            le.addPotionEffect(new PotionEffect(PotionEffectType.POISON, poisonDur, poisonAmp));
            p.sendMessage("\u00a72Vine pulled " + le.getName() + "!");
        }
    }

    private void midasAbility1(Player p) {
        p.setMetadata("dagger_midas_next_hit", (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)true));
        p.sendMessage("\u00a76Midas's touch primed \u2014 next hit alters armor protection!");
    }

    private void midasAbility2(Player p) {
        double radius = this.cfgD("daggers.midas.ability2.radius", 5.0);
        int dur = this.cfgTicks("daggers.midas.ability2.duration-seconds", 5.0);
        this.applyTempProtection(p, dur);
        for (Entity e : p.getNearbyEntities(radius, radius, radius)) {
            if (!(e instanceof Player)) continue;
            Player tp = (Player)e;
            if (!this.plugin.getTrustManager().isTrusted(p.getUniqueId(), tp.getUniqueId())) continue;
            this.applyTempProtection(tp, dur);
            tp.sendMessage("\u00a76" + p.getName() + " gave you Resistance for " + dur / 20 + "s!");
        }
        p.sendMessage("\u00a76Protection aura activated!");
    }

    private void applyTempProtection(Player p, int durTicks) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, durTicks, 1));
    }

    private void toxicAbility1(final Player p) {
        Location loc = p.getLocation();
        final int dur = this.cfgTicks("daggers.toxic.ability1.cloud-duration-seconds", 5.0);
        final int amp = this.cfgI("daggers.toxic.ability1.poison-amplifier", 2);
        final double radius = this.cfgD("daggers.toxic.ability1.radius", 5.0);
        loc.getWorld().spawnParticle(Particle.SNEEZE, loc.add(0.0, 1.0, 0.0), 60, radius * 0.5, 1.0, radius * 0.5, 0.0);
        new BukkitRunnable(){
            int t = 0;

            public void run() {
                if (++this.t > dur || !p.isOnline()) {
                    this.cancel();
                    return;
                }
                if (this.t % 10 == 0) {
                    for (Entity e : p.getNearbyEntities(radius, 2.0, radius)) {
                        LivingEntity le;
                        if (!(e instanceof LivingEntity) || (le = (LivingEntity)e) == p || AbilityManager.this.isTrustedEntity(p, e)) continue;
                        le.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, amp));
                    }
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L);
        p.sendMessage("\u00a7aPoison cloud released!");
    }

    private void toxicAbility2(Player p) {
        p.setMetadata("dagger_toxic_lethal", (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)true));
        p.sendMessage("\u00a7aNext hit injects lethal poison!");
    }

    private void arachnidAbility1(Player p) {
        p.setMetadata("dagger_arachnid_next", (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)true));
        p.sendMessage("\u00a78The next attack you do will paralyze!");
    }

    private void vampireAbility1(final Player p) {
        int dur = this.cfgTicks("daggers.vampire.ability1.duration-seconds", 8.0);
        p.setMetadata("dagger_vampire_heal", (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)true));
        p.sendMessage("\u00a74Bloodthirst! \u00a77" + (int)(this.cfgD("daggers.vampire.ability1.heal-percent", 0.25) * 100.0) + "% lifesteal for " + dur / 20 + "s.");
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1.0f, 1.5f);
        new BukkitRunnable(){

            public void run() {
                if (p.isOnline()) {
                    p.removeMetadata("dagger_vampire_heal", (Plugin)AbilityManager.this.plugin);
                    p.sendMessage("\u00a74Bloodthirst ended.");
                }
            }
        }.runTaskLater((Plugin)this.plugin, (long)dur);
    }

    private void gravityAbility1(final Player p) {
        Location loc = p.getLocation();
        double radius = this.cfgD("daggers.gravity.ability1.pull-radius", 10.0);
        final double dmg = this.cfgD("daggers.gravity.ability1.pull-damage", 6.0);
        final double flingY = this.cfgD("daggers.gravity.ability1.fling-y", 1.5);
        p.getWorld().spawnParticle(Particle.PORTAL, loc, 80, 4.0, 4.0, 4.0, 0.5);
        p.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 2.0f, 0.5f);
        final ArrayList<LivingEntity> hit = new ArrayList<LivingEntity>();
        for (Entity e : p.getNearbyEntities(radius, radius, radius)) {
            LivingEntity le;
            if (!(e instanceof LivingEntity) || (le = (LivingEntity)e) == p || this.isTrustedEntity(p, e)) continue;
            Vector pull = p.getLocation().toVector().subtract(e.getLocation().toVector()).normalize().multiply(2);
            e.setVelocity(pull);
            hit.add(le);
        }
        new BukkitRunnable(){

            public void run() {
                for (LivingEntity le : hit) {
                    if (!le.isValid()) continue;
                    le.damage(dmg, (Entity)p);
                    Vector out = le.getLocation().toVector().subtract(p.getLocation().toVector()).normalize().multiply(1.5).setY(flingY);
                    le.setVelocity(out);
                }
            }
        }.runTaskLater((Plugin)this.plugin, (long)this.cfgTicks("daggers.gravity.ability1.collapse-delay-seconds", 1.5));
        p.sendMessage("\u00a75Black hole created!");
    }

    private void gravityAbility2(Player p) {
        double radius = this.cfgD("daggers.gravity.ability2.radius", 6.0);
        int dur = this.cfgTicks("daggers.gravity.ability2.duration-seconds", 5.0);
        int amp = this.cfgI("daggers.gravity.ability2.levitation-amplifier", 9);
        for (Entity e : p.getNearbyEntities(radius, radius, radius)) {
            if (!(e instanceof Player)) continue;
            Player tp = (Player)e;
            if (this.isTrustedEntity(p, e)) continue;
            tp.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, dur, amp));
        }
        p.sendMessage("\u00a75Levitation applied to nearby untrusted players!");
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
            mat = Material.valueOf((String)this.plugin.getConfig().getString("daggers.earth.ability1.material", "STONE"));
        }
        catch (Exception ex) {
            mat = Material.STONE;
        }
        int half = width / 2;
        for (int col = -half; col < width - half; ++col) {
            for (int row = 0; row < height; ++row) {
                Location bl = base.clone().add(right.clone().multiply(col)).add(0.0, (double)row, 0.0);
                Block b = bl.getBlock();
                if (!b.getType().isAir()) continue;
                b.setType(mat);
                wall.add(b);
            }
        }
        final UUID uuid = p.getUniqueId();
        for (Block b : this.earthWalls.getOrDefault(uuid, new ArrayList<Block>())) {
            b.setType(Material.AIR);
        }
        this.earthWalls.put(uuid, wall);
        p.sendMessage("\u00a76Wall raised (" + width + "\u00d7" + height + ")!");
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
        final Location spawnLoc = p.getEyeLocation().add(p.getLocation().getDirection().normalize().multiply(2));
        Vector vel = p.getLocation().getDirection().normalize().multiply(1.5).setY(0.5);
        final double dmg = this.cfgD("daggers.earth.ability2.boulder-damage", 8.0);
        final double radius = this.cfgD("daggers.earth.ability2.radius", 4.0);
        final FallingBlock boulder = p.getWorld().spawnFallingBlock(spawnLoc, Material.COBBLESTONE.createBlockData());
        boulder.setVelocity(vel);
        boulder.setDropItem(false);
        boulder.setHurtEntities(true);
        boulder.setMetadata("dagger_earth_boulder", (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)p.getUniqueId().toString()));
        p.sendMessage("\u00a76Boulder hurled!");
        new BukkitRunnable(){
            int ticks = 0;

            public void run() {
                ++this.ticks;
                if (this.ticks > 200 || !boulder.isValid()) {
                    Location impact = boulder.isValid() ? boulder.getLocation() : spawnLoc;
                    boulder.remove();
                    impact.getWorld().spawnParticle(Particle.EXPLOSION, impact, 15, 1.0, 0.5, 1.0, 0.0);
                    impact.getWorld().playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
                    for (Entity e : impact.getWorld().getNearbyEntities(impact, radius, radius, radius)) {
                        LivingEntity le;
                        if (!(e instanceof LivingEntity) || (le = (LivingEntity)e) == p || AbilityManager.this.isTrustedEntity(p, e)) continue;
                        le.damage(dmg, (Entity)p);
                    }
                    this.cancel();
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 1L);
    }

    private void titanAbility1(final Player p) {
        p.setMetadata("dagger_titan_grow_next", (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)true));
        p.sendMessage("\u00a7cNext hit player will grow giant!");
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
        this.scalePlayer(p, scale, ticks);
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int)ticks, this.cfgI("daggers.titan.ability2.speed-amplifier", 2)));
        p.sendMessage("\u00a7cYou shrunk to " + scale + "x with Speed!");
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
        final double dmgPerSec = this.cfgD("daggers.guardian.ability1.beam-damage-per-tick", 1.0);
        final double knock = this.cfgD("daggers.guardian.ability1.knockback", 0.5);
        final long maxTicks = this.cfgTicks("daggers.guardian.ability1.duration-seconds", 8.0);
        final Entity[] tgt = new Entity[]{this.getNearestTarget(p, 30.0)};
        if (tgt[0] == null) {
            p.sendMessage("\u00a7cNo target!");
            return;
        }
        p.sendMessage("\u00a7bGuardian beam locked on " + tgt[0].getName());
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_GUARDIAN_ATTACK, 2.0f, 1.0f);
        BukkitRunnable beam = new BukkitRunnable(){
            int ticks = 0;

            public void run() {
                Entity entity;
                ++this.ticks;
                if (!p.isOnline() || tgt[0] == null || !tgt[0].isValid() || tgt[0].isDead() || (long)this.ticks > maxTicks) {
                    AbilityManager.this.endGuardianBeam(p);
                    this.cancel();
                    return;
                }
                Location start = p.getEyeLocation();
                Location end = tgt[0].getLocation().add(0.0, 1.0, 0.0);
                double dist = start.distance(end);
                Vector dir = end.subtract(start).toVector().normalize();
                for (double d = 0.0; d < dist; d += 0.5) {
                    Location pt = start.clone().add(dir.clone().multiply(d));
                    pt.getWorld().spawnParticle(Particle.DUST, pt, 0, (Object)new Particle.DustOptions(Color.fromRGB((int)100, (int)200, (int)255), 0.5f));
                }
                if (this.ticks % 20 == 0 && (entity = tgt[0]) instanceof LivingEntity) {
                    LivingEntity le = (LivingEntity)entity;
                    le.damage(dmgPerSec, (Entity)p);
                    Vector kb = tgt[0].getLocation().toVector().subtract(p.getLocation().toVector()).normalize().multiply(knock).setY(0.2);
                    tgt[0].setVelocity(kb);
                }
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
        for (Entity e : p.getNearbyEntities(radius, radius, radius)) {
            LivingEntity le;
            if (!(e instanceof LivingEntity) || (le = (LivingEntity)e) == p || this.isTrustedEntity(p, e)) continue;
            le.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, dur, amp));
        }
        p.sendMessage("\u00a7bMining fatigue applied nearby!");
    }

    private void ghostAbility1(Player p) {
        double dist = this.cfgD("daggers.ghost.ability1.dash-blocks", 10.0);
        Vector dir = p.getLocation().getDirection().normalize().multiply(dist);
        Location target = p.getLocation().clone().add(dir);
        target.setPitch(p.getLocation().getPitch());
        target.setYaw(p.getLocation().getYaw());
        p.teleport(target);
        p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation(), 30, 0.5, 1.0, 0.5, 0.05);
        p.sendMessage("\u00a77Ghost dash!");
    }

    private void ghostAbility2(final Player p) {
        long ticks = this.cfgTicks("daggers.ghost.ability2.flight-duration-seconds", 8.0);
        final UUID uuid = p.getUniqueId();
        this.ghostFormActive.add(uuid);
        p.setAllowFlight(true);
        p.setFlying(true);
        p.setMetadata("dagger_ghost_noclip", (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)true));
        p.sendMessage("\u00a77Ghost flight for " + ticks / 20L + "s!");
        final BukkitRunnable trail = new BukkitRunnable(){

            public void run() {
                if (!p.isOnline()) {
                    this.cancel();
                    return;
                }
                p.getWorld().spawnParticle(Particle.DUST, p.getLocation().add(0.0, 1.0, 0.0), 5, 0.2, 0.4, 0.2, (Object)new Particle.DustOptions(Color.WHITE, 1.0f));
            }
        };
        trail.runTaskTimer((Plugin)this.plugin, 0L, 4L);
        new BukkitRunnable(){

            public void run() {
                AbilityManager.this.ghostFormActive.remove(uuid);
                trail.cancel();
                if (!p.isOnline()) {
                    return;
                }
                if (!p.isOp()) {
                    p.setAllowFlight(false);
                }
                p.setFlying(false);
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
            player.sendMessage("\u00a7cChance Dagger not found!");
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
        final UUID uuid = player.getUniqueId();
        this.chanceActiveDagger.put(uuid, picked);
        this.chanceEndTime.put(uuid, System.currentTimeMillis() + ms);
        player.sendMessage("\u00a7dTransformed into: " + picked.getDisplayName() + " for " + ms / 1000L + "s!");
        new BukkitRunnable(){

            public void run() {
                if (!player.isOnline()) {
                    return;
                }
                AbilityManager.this.chanceActiveDagger.remove(uuid);
                AbilityManager.this.chanceEndTime.remove(uuid);
                ItemStack held = player.getInventory().getItem(fslot);
                if (DaggerType.fromItem(held) == picked) {
                    player.getInventory().setItem(fslot, null);
                }
                player.sendMessage("\u00a7dTransformation ended.");
            }
        }.runTaskLater((Plugin)this.plugin, ms / 50L);
    }

    private void stormAbility1(Player p) {
        Entity tgt = this.getNearestTarget(p, this.cfgD("daggers.storm.ability1.range", 30.0));
        if (tgt == null) {
            p.sendMessage("\u00a7cNo target!");
            return;
        }
        double dmg = this.cfgD("daggers.storm.ability1.damage", 6.0);
        tgt.getWorld().strikeLightning(tgt.getLocation());
        if (tgt instanceof LivingEntity) {
            LivingEntity le = (LivingEntity)tgt;
            le.damage(dmg, (Entity)p);
        }
        p.sendMessage("\u00a7eLightning struck " + tgt.getName() + "!");
    }

    private void stormAbility2(Player p) {
        double dmg = this.cfgD("daggers.storm.ability2.damage", 4.0);
        double radius = this.cfgD("daggers.storm.ability2.radius", 5.0);
        for (Entity e : p.getNearbyEntities(radius, radius, radius)) {
            LivingEntity le;
            if (!(e instanceof LivingEntity) || (le = (LivingEntity)e) == p || this.isTrustedEntity(p, e)) continue;
            e.getWorld().strikeLightning(e.getLocation());
            le.damage(dmg, (Entity)p);
        }
        p.sendMessage("\u00a7eStorm bolts unleashed!");
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

