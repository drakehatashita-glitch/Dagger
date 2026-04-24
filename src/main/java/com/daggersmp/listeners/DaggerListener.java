/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.NamespacedKey
 *  org.bukkit.Sound
 *  org.bukkit.attribute.Attribute
 *  org.bukkit.attribute.AttributeInstance
 *  org.bukkit.attribute.AttributeModifier
 *  org.bukkit.attribute.AttributeModifier$Operation
 *  org.bukkit.enchantments.Enchantment
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Fireball
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.entity.Vindicator
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.EntityDamageByEntityEvent
 *  org.bukkit.event.entity.EntityDamageEvent
 *  org.bukkit.event.entity.EntityDamageEvent$DamageCause
 *  org.bukkit.event.entity.EntityTargetLivingEntityEvent
 *  org.bukkit.event.inventory.CraftItemEvent
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.PrepareItemCraftEvent
 *  org.bukkit.event.player.PlayerSwapHandItemsEvent
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.Damageable
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.metadata.FixedMetadataValue
 *  org.bukkit.metadata.MetadataValue
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 *  org.bukkit.scheduler.BukkitRunnable
 *  org.bukkit.util.Vector
 */
package com.daggersmp.listeners;

import com.daggersmp.DaggerSMP;
import com.daggersmp.commands.DaggerCommand;
import com.daggersmp.daggers.DaggerType;
import com.daggersmp.managers.AbilityManager;
import com.daggersmp.managers.CooldownManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vindicator;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Boat;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class DaggerListener
implements Listener {
    private final DaggerSMP plugin;

    public DaggerListener(DaggerSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled=true)
    public void onSwapHands(PlayerSwapHandItemsEvent e) {
        Player p = e.getPlayer();
        ItemStack main = e.getMainHandItem();
        ItemStack off = e.getOffHandItem();
        DaggerType type = DaggerType.fromItem(p.getInventory().getItemInMainHand());
        if (type == null) {
            type = DaggerType.fromItem(p.getInventory().getItemInOffHand());
        }
        if (type == null) {
            return;
        }
        if (!this.plugin.getToggleControlsManager().isEnabled(p.getUniqueId())) {
            return;
        }
        e.setCancelled(true);
        int abilityNum = p.isSneaking() ? 2 : 1;
        this.plugin.getAbilityManager().tryActivate(p, type, abilityNum);
    }

    @EventHandler
    public void onRecipeGuiClick(InventoryClickEvent e) {
        HumanEntity humanEntity;
        InventoryHolder inventoryHolder = e.getInventory().getHolder();
        if (!(inventoryHolder instanceof DaggerCommand.RecipeMenuHolder)) {
            return;
        }
        DaggerCommand.RecipeMenuHolder holder = (DaggerCommand.RecipeMenuHolder)inventoryHolder;
        e.setCancelled(true);
        if (holder.type != null) {
            return;
        }
        ItemStack clicked = e.getCurrentItem();
        final DaggerType type = DaggerType.fromItem(clicked);
        if (type == null || !((humanEntity = e.getWhoClicked()) instanceof Player)) {
            return;
        }
        final Player p = (Player)humanEntity;
        new BukkitRunnable(){

            public void run() {
                DaggerCommand.openRecipeFor(p, type, DaggerListener.this.plugin);
            }
        }.runTaskLater((Plugin)this.plugin, 1L);
    }

    @EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
    public void onDamageByEntity(EntityDamageByEntityEvent e) {
        org.bukkit.util.Vector targetFacing;
        org.bukkit.util.Vector toTarget;
        Player voidTp;
        double chance;
        double thr;
        org.bukkit.util.Vector targetFacing2;
        org.bukkit.util.Vector toAttacker;
        double dot;
        Entity entity = e.getDamager();
        if (!(entity instanceof Player)) {
            return;
        }
        Player p = (Player)entity;
        Entity entity2 = e.getEntity();
        if (!(entity2 instanceof LivingEntity)) {
            return;
        }
        LivingEntity target = (LivingEntity)entity2;
        if (target == p) {
            return;
        }
        AbilityManager am = this.plugin.getAbilityManager();
        boolean charged = p.getAttackCooldown() >= 0.9f;
        DaggerType held = DaggerType.fromItem(p.getInventory().getItemInMainHand());
        if (target.hasMetadata("dagger_frost_debuff")) {
            double mult = this.plugin.getConfig().getDouble("daggers.frost.ability2.incoming-damage-multiplier", 1.15);
            e.setDamage(e.getDamage() * mult);
        }
        if (target instanceof Player) {
            Player tp = (Player)target;
            if (p.hasMetadata("dagger_strength_armor_break")) {
                p.removeMetadata("dagger_strength_armor_break", (Plugin)this.plugin);
                double pct = this.plugin.getConfig().getDouble("daggers.strength.ability2.armor-durability-percent", 0.15);
                ItemStack[] armor = tp.getInventory().getArmorContents();
                int idx = (int)(Math.random() * (double)armor.length);
                for (int i = 0; i < armor.length; ++i) {
                    int s = (idx + i) % armor.length;
                    ItemStack piece = armor[s];
                    if (piece == null || piece.getType() == Material.AIR || piece.getType().getMaxDurability() == 0) continue;
                    int dmg = (int)((double)piece.getType().getMaxDurability() * pct);
                    ItemMeta itemMeta = piece.getItemMeta();
                    if (itemMeta instanceof Damageable) {
                        Damageable dm = (Damageable)itemMeta;
                        dm.setDamage(Math.min(piece.getType().getMaxDurability() - 1, dm.getDamage() + dmg));
                        piece.setItemMeta((ItemMeta)dm);
                    }
                    armor[s] = piece;
                    tp.getInventory().setArmorContents(armor);
                    break;
                }
                tp.sendMessage("\u00a7cYour armor took heavy damage!");
            }
        }
        if (p.hasMetadata("dagger_crimson_wither_next")) {
            p.removeMetadata("dagger_crimson_wither_next", (Plugin)this.plugin);
            target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, (int)(this.plugin.getConfig().getDouble("daggers.crimson.ability1.wither-duration-seconds", 4.0) * 20.0), this.plugin.getConfig().getInt("daggers.crimson.ability1.wither-amplifier", 2)));
        }
        if (p.hasMetadata("dagger_mafia_next_hit")) {
            p.removeMetadata("dagger_mafia_next_hit", (Plugin)this.plugin);
            int dur = (int)(this.plugin.getConfig().getDouble("daggers.mafia.ability1.debuff-duration-seconds", 3.0) * 20.0);
            target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, dur, this.plugin.getConfig().getInt("daggers.mafia.ability1.poison-amplifier", 0)));
            target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, dur, this.plugin.getConfig().getInt("daggers.mafia.ability1.weakness-amplifier", 0)));
            target.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, dur, this.plugin.getConfig().getInt("daggers.mafia.ability1.hunger-amplifier", 0)));
        }
        if (target instanceof Player) {
            Player tp = (Player)target;
            am.notifyMafiaThreat(tp, p);
        } else {
            am.notifyMafiaTargetMob(p, target);
        }
        if (p.hasMetadata("dagger_midas_next_hit") && target instanceof Player) {
            Player midasTp = (Player)target;
            p.removeMetadata("dagger_midas_next_hit", (Plugin)this.plugin);
            boolean trusted = this.plugin.getTrustManager().isTrusted(p.getUniqueId(), midasTp.getUniqueId());
            int delta = trusted ? this.plugin.getConfig().getInt("daggers.midas.ability1.trusted-protection-increase", 1) : -this.plugin.getConfig().getInt("daggers.midas.ability1.enemy-protection-decrease", 2);
            int dur = (int)(this.plugin.getConfig().getDouble("daggers.midas.ability1." + (trusted ? "trusted" : "enemy") + "-duration-seconds", 3.0) * 20.0);
            this.modifyArmorProtection(midasTp, delta, dur);
            midasTp.sendMessage(trusted ? "\u00a76Midas blessing: +" + delta + " protection for " + dur / 20 + "s!" : "\u00a76Midas curse: " + delta + " protection for " + dur / 20 + "s!");
        }
        if (p.hasMetadata("dagger_toxic_lethal")) {
            p.removeMetadata("dagger_toxic_lethal", (Plugin)this.plugin);
            int amp = this.plugin.getConfig().getInt("daggers.toxic.ability2.poison-amplifier", 4);
            int dur = (int)(this.plugin.getConfig().getDouble("daggers.toxic.ability2.poison-duration-seconds", 3.0) * 20.0);
            target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, dur, amp));
        }
        if (p.hasMetadata("dagger_arachnid_next")) {
            p.removeMetadata("dagger_arachnid_next", (Plugin)this.plugin);
            int dur = (int)(this.plugin.getConfig().getDouble("daggers.arachnid.ability1.duration-seconds", 2.0) * 20.0);
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, dur, this.plugin.getConfig().getInt("daggers.arachnid.ability1.paralyze-amplifier", 6)));
            target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, dur, 6));
            target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, dur, 4));
            int sd = (int)(this.plugin.getConfig().getDouble("daggers.arachnid.ability1.strength-duration-seconds", 2.0) * 20.0);
            p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, sd, this.plugin.getConfig().getInt("daggers.arachnid.ability1.strength-amplifier", 0)));
        }
        if (p.hasMetadata("dagger_titan_grow_next") && target instanceof Player) {
            Player titanTp = (Player)target;
            p.removeMetadata("dagger_titan_grow_next", (Plugin)this.plugin);
            this.applyScale(titanTp, this.plugin.getConfig().getDouble("daggers.titan.ability1.grow-scale", 2.5), (int)(this.plugin.getConfig().getDouble("daggers.titan.ability1.grow-duration-seconds", 5.0) * 20.0));
            titanTp.sendMessage("\u00a7cYou were enlarged by " + p.getName() + "!");
        }
        if (p.hasMetadata("dagger_vampire_heal") && target instanceof Player) {
            double pct = this.plugin.getConfig().getDouble("daggers.vampire.ability1.heal-percent", 0.25);
            double heal = e.getFinalDamage() * pct;
            double max = p.getAttribute(Attribute.MAX_HEALTH).getValue();
            p.setHealth(Math.min(max, p.getHealth() + heal));
        }
        if (am.isHighestPrioritySlot(p, DaggerType.VAMPIRE) && target instanceof Player && (dot = (toAttacker = p.getLocation().getDirection().normalize()).dot(targetFacing2 = target.getLocation().getDirection().normalize())) > -(thr = this.plugin.getConfig().getDouble("daggers.vampire.passive.backstab-angle-threshold", -0.3))) {
            double mult = this.plugin.getConfig().getDouble("daggers.vampire.passive.backstab-multiplier", 1.5);
            e.setDamage(e.getDamage() * mult);
        }
        if (charged) {
            if (am.hasDaggerAnywhere(p, DaggerType.FROST)) {
                double chance2 = this.plugin.getConfig().getDouble("daggers.frost.passive.slowness-chance", 0.05);
                if (Math.random() < chance2) {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, (int)(this.plugin.getConfig().getDouble("daggers.frost.passive.slowness-duration-seconds", 3.0) * 20.0), this.plugin.getConfig().getInt("daggers.frost.passive.slowness-amplifier", 1)));
                }
            }
            if (am.hasDaggerAnywhere(p, DaggerType.VAMPIRE) && target instanceof Player) {
                final Player vTgt = (Player)target;
                chance = this.plugin.getConfig().getDouble("daggers.vampire.passive.bleed-chance", 0.05);
                if (Math.random() < chance) {
                    final int durSec = this.plugin.getConfig().getInt("daggers.vampire.passive.bleed-duration-seconds", 6);
                    final double tickDmg = this.plugin.getConfig().getDouble("daggers.vampire.passive.bleed-damage-per-tick", 1.0);
                    final Player owner = p;
                    new BukkitRunnable(){
                        int t = 0;

                        public void run() {
                            if (++this.t > durSec || vTgt.isDead() || !vTgt.isValid()) {
                                this.cancel();
                                return;
                            }
                            vTgt.damage(tickDmg, (Entity)owner);
                        }
                    }.runTaskTimer((Plugin)this.plugin, 0L, 20L);
                    vTgt.sendMessage("\u00a74You're bleeding!");
                }
            }
            if (am.hasDaggerAnywhere(p, DaggerType.STORM)) {
                double chance3 = this.plugin.getConfig().getDouble("daggers.storm.passive.lightning-chance-on-hit", 0.05);
                if (Math.random() < chance3) {
                    target.getWorld().strikeLightning(target.getLocation());
                    target.damage(this.plugin.getConfig().getDouble("daggers.storm.passive.lightning-damage", 4.0), (Entity)p);
                }
            }
        }
        if (am.hasDaggerAnywhere(p, DaggerType.MIRROR) && target instanceof Player) {
            Player mirrorTgt = (Player)target;
            chance = this.plugin.getConfig().getDouble("daggers.mirror.passive.copy-chance", 0.05);
            if (Math.random() < chance) {
                for (PotionEffect eff : mirrorTgt.getActivePotionEffects()) {
                    if (!this.isPositiveEffect(eff.getType())) continue;
                    p.addPotionEffect(new PotionEffect(eff.getType(), eff.getDuration(), eff.getAmplifier()));
                }
            }
        }
        if (target instanceof Player && am.hasDaggerAnywhere(voidTp = (Player)target, DaggerType.VOID)) {
            UUID uuid = voidTp.getUniqueId();
            CooldownManager cm = this.plugin.getCooldownManager();
            String key = "void_passive";
            if (!voidTp.hasMetadata("dagger_void_passive_cd")) {
                long cdSec = this.plugin.getConfig().getLong("daggers.void.passive.teleport-cooldown-seconds", 30L);
                voidTp.setMetadata("dagger_void_passive_cd", (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)true));
                Location behind = p.getLocation().clone().add(p.getLocation().getDirection().multiply(-2));
                voidTp.teleport(behind);
                voidTp.sendMessage("\u00a75Void teleport!");
                new BukkitRunnable(){

                    public void run() {
                        if (voidTp.isOnline()) {
                            voidTp.removeMetadata("dagger_void_passive_cd", (Plugin)DaggerListener.this.plugin);
                        }
                    }
                }.runTaskLater((Plugin)this.plugin, cdSec * 20L);
            }
        }
        if (am.hasDaggerAnywhere(p, DaggerType.VOID) && (toTarget = target.getLocation().toVector().subtract(p.getLocation().toVector()).normalize()).dot(targetFacing = target.getLocation().getDirection().normalize()) > 0.7) {
            double bonus = this.plugin.getConfig().getDouble("daggers.void.passive.backstab-bonus", 0.25);
            e.setDamage(e.getDamage() * (1.0 + bonus));
        }
        if (am.hasDaggerAnywhere(p, DaggerType.GUARDIAN)) {
            double thresholdHp = this.plugin.getConfig().getDouble("daggers.guardian.passive.threshold-hearts", 10.0);
            if (p.getHealth() < thresholdHp) {
                double bonusPerHp = this.plugin.getConfig().getDouble("daggers.guardian.passive.bonus-per-heart", 0.1);
                double mult = 1.0 + (thresholdHp - p.getHealth()) * bonusPerHp;
                if (p.getHealth() <= 1.0) {
                    mult = this.plugin.getConfig().getDouble("daggers.guardian.passive.one-heart-multiplier", 2.0);
                }
                e.setDamage(e.getDamage() * mult);
            }
        }
    }

    @EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
    public void onDamage(EntityDamageEvent e) {
        Player attacker;
        EntityDamageByEntityEvent ed;
        Entity damagerEntity;
        Entity entity = e.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        Player p = (Player)entity;
        AbilityManager am = this.plugin.getAbilityManager();
        if (e.getCause() == EntityDamageEvent.DamageCause.FALL) {
            if (am.hasDaggerAnywhere(p, DaggerType.WIND) && this.plugin.getConfig().getBoolean("daggers.wind.passive.no-fall-damage", true)) {
                e.setCancelled(true);
                return;
            }
            if (am.isWindFallImmune(p.getUniqueId())) {
                e.setCancelled(true);
                return;
            }
            if (am.hasDaggerAnywhere(p, DaggerType.GRAVITY)) {
                e.setDamage(e.getDamage() * 0.5);
                double minFall = this.plugin.getConfig().getDouble("daggers.gravity.passive.min-fall-blocks", 6.0);
                if ((double)p.getFallDistance() >= minFall) {
                    double radius2 = this.plugin.getConfig().getDouble("daggers.gravity.passive.shockwave-radius", 5.0);
                    double dmgPerBlock = this.plugin.getConfig().getDouble("daggers.gravity.passive.damage-per-block", 0.5);
                    double maxDmg = this.plugin.getConfig().getDouble("daggers.gravity.passive.max-damage", 10.0);
                    double dmg = Math.min(maxDmg, (double)p.getFallDistance() * dmgPerBlock);
                    for (Entity en : p.getNearbyEntities(radius2, radius2, radius2)) {
                        LivingEntity le;
                        if (!(en instanceof LivingEntity) || (le = (LivingEntity)en) == p || am.isTrustedEntity(p, en)) continue;
                        le.damage(dmg, (Entity)p);
                    }
                }
            }
        }
        if (am.hasDaggerAnywhere(p, DaggerType.GHOST) && Math.random() < this.plugin.getConfig().getDouble("daggers.ghost.passive.dodge-chance", 0.1)) {
            e.setCancelled(true);
            p.sendMessage("\u00a77Dodged!");
            return;
        }
        if (am.hasDaggerAnywhere(p, DaggerType.LUCKY) && e.getFinalDamage() >= p.getHealth() && Math.random() < this.plugin.getConfig().getDouble("daggers.lucky.passive.totem-chance", 0.2)) {
            e.setDamage(0.0);
            p.setHealth(2.0);
            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 2));
            p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 200, 2));
            p.getWorld().playSound(p.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
            p.sendMessage("\u00a7eLucky! Saved from death.");
        }
        if (am.hasDaggerAnywhere(p, DaggerType.TOXIC) && this.plugin.getConfig().getBoolean("daggers.toxic.passive.poison-immunity", true) && e.getCause() == EntityDamageEvent.DamageCause.POISON) {
            e.setCancelled(true);
            return;
        }
        if (p.hasMetadata("dagger_mirror_full_reflect") && e instanceof EntityDamageByEntityEvent && (damagerEntity = (ed = (EntityDamageByEntityEvent)e).getDamager()) instanceof Player && (attacker = (Player)damagerEntity) != p) {
            double pct = this.plugin.getConfig().getDouble("daggers.mirror.ability1.reflect-percent", 0.25);
            double back = e.getDamage() * pct;
            attacker.damage(back, (Entity)p);
        }
        if (p.hasMetadata("dagger_guardian_beam_active")) {
            am.cancelGuardianBeamIfHit(p);
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void onTarget(EntityTargetLivingEntityEvent e) {
        Entity entity = e.getEntity();
        if (!(entity instanceof Vindicator)) {
            return;
        }
        Vindicator v = (Vindicator)entity;
        if (!v.hasMetadata("dagger_mafia_owner")) {
            return;
        }
        LivingEntity livingEntity = e.getTarget();
        if (livingEntity instanceof Player) {
            Player tp = (Player)livingEntity;
            String ownerId = ((MetadataValue)v.getMetadata("dagger_mafia_owner").get(0)).asString();
            UUID owner = UUID.fromString(ownerId);
            if (tp.getUniqueId().equals(owner)) {
                e.setCancelled(true);
                return;
            }
            if (this.plugin.getTrustManager().isTrusted(owner, tp.getUniqueId())) {
                e.setCancelled(true);
                return;
            }
            if (!tp.hasMetadata("dagger_mafia_threat_" + String.valueOf(owner))) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
    public void onFireballHit(EntityDamageByEntityEvent e) {
        Entity entity = e.getDamager();
        if (!(entity instanceof Fireball)) {
            return;
        }
        Fireball fb = (Fireball)entity;
        if (!fb.hasMetadata("dagger_crimson_damage")) {
            return;
        }
        double dmg = ((MetadataValue)fb.getMetadata("dagger_crimson_damage").get(0)).asDouble();
        e.setDamage(dmg);
    }

    @EventHandler(ignoreCancelled=true)
    public void onPrepareCraft(PrepareItemCraftEvent e) {
        if (e.getRecipe() == null || e.getRecipe().getResult() == null) {
            return;
        }
        DaggerType type = DaggerType.fromItem(e.getRecipe().getResult());
        if (type == null) {
            return;
        }
        if (!this.plugin.getCraftLimitManager().canCraft(type)) {
            e.getInventory().setResult(null);
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void onCraft(CraftItemEvent e) {
        HumanEntity humanEntity;
        DaggerType type = DaggerType.fromItem(e.getRecipe().getResult());
        if (type == null) {
            return;
        }
        if (!this.plugin.getCraftLimitManager().canCraft(type)) {
            e.setCancelled(true);
            return;
        }
        this.plugin.getCraftLimitManager().recordCraft(type);
        if (this.plugin.getConfig().getBoolean("settings.broadcast-craft", true) && (humanEntity = e.getWhoClicked()) instanceof Player) {
            Player p = (Player)humanEntity;
            this.plugin.getServer().broadcastMessage("\u00a76" + p.getName() + " \u00a77crafted " + type.getDisplayName() + "\u00a77!");
        }
    }

    private boolean isPositiveEffect(PotionEffectType t) {
        if (t == PotionEffectType.JUMP_BOOST) {
            return false;
        }
        if (t == PotionEffectType.SLOW_FALLING) {
            return false;
        }
        if ("pale_rot".equals(t.getKey().getKey())) {
            return false;
        }
        if (t == PotionEffectType.SPEED) {
            return true;
        }
        if (t == PotionEffectType.STRENGTH) {
            return true;
        }
        if (t == PotionEffectType.RESISTANCE) {
            return true;
        }
        if (t == PotionEffectType.REGENERATION) {
            return true;
        }
        if (t == PotionEffectType.HASTE) {
            return true;
        }
        if (t == PotionEffectType.FIRE_RESISTANCE) {
            return true;
        }
        if (t == PotionEffectType.WATER_BREATHING) {
            return true;
        }
        if (t == PotionEffectType.NIGHT_VISION) {
            return true;
        }
        if (t == PotionEffectType.ABSORPTION) {
            return true;
        }
        if (t == PotionEffectType.HEALTH_BOOST) {
            return true;
        }
        if (t == PotionEffectType.LUCK) {
            return true;
        }
        if (t == PotionEffectType.HERO_OF_THE_VILLAGE) {
            return true;
        }
        if (t == PotionEffectType.DOLPHINS_GRACE) {
            return true;
        }
        if (t == PotionEffectType.CONDUIT_POWER) {
            return true;
        }
        return t == PotionEffectType.INVISIBILITY;
    }

    private void modifyArmorProtection(final Player target, int delta, int durTicks) {
        ItemStack[] armor = target.getInventory().getArmorContents();
        final HashMap<Integer, Integer> originals = new HashMap<Integer, Integer>();
        for (int i = 0; i < armor.length; ++i) {
            int lvl;
            ItemStack piece = armor[i];
            if (piece == null || (lvl = piece.getEnchantmentLevel(Enchantment.PROTECTION)) <= 0 && delta < 0) continue;
            int newLvl = Math.max(0, lvl + delta);
            originals.put(i, lvl);
            if (newLvl == 0) {
                piece.removeEnchantment(Enchantment.PROTECTION);
                continue;
            }
            piece.addUnsafeEnchantment(Enchantment.PROTECTION, newLvl);
        }
        target.getInventory().setArmorContents(armor);
        new BukkitRunnable(){

            public void run() {
                if (!target.isOnline()) {
                    return;
                }
                ItemStack[] cur = target.getInventory().getArmorContents();
                for (Map.Entry en : originals.entrySet()) {
                    ItemStack piece = cur[(Integer)en.getKey()];
                    if (piece == null) continue;
                    if ((Integer)en.getValue() == 0) {
                        piece.removeEnchantment(Enchantment.PROTECTION);
                        continue;
                    }
                    piece.addUnsafeEnchantment(Enchantment.PROTECTION, ((Integer)en.getValue()).intValue());
                }
                target.getInventory().setArmorContents(cur);
            }
        }.runTaskLater((Plugin)this.plugin, (long)durTicks);
    }

    private void applyScale(final Player p, double scale, int durTicks) {
        AttributeInstance attr = p.getAttribute(Attribute.SCALE);
        if (attr == null) {
            return;
        }
        final NamespacedKey key = new NamespacedKey("daggersmp", "titan_grow");
        for (AttributeModifier m : new ArrayList<AttributeModifier>(attr.getModifiers())) {
            if (!m.getKey().equals((Object)key)) continue;
            attr.removeModifier(m);
        }
        double base = attr.getBaseValue();
        attr.addModifier(new AttributeModifier(key, scale - base, AttributeModifier.Operation.ADD_NUMBER));
        new BukkitRunnable(){

            public void run() {
                AttributeInstance a = p.getAttribute(Attribute.SCALE);
                if (a == null) {
                    return;
                }
                for (AttributeModifier m : new ArrayList<AttributeModifier>(a.getModifiers())) {
                    if (!m.getKey().equals((Object)key)) continue;
                    a.removeModifier(m);
                }
            }
        }.runTaskLater((Plugin)this.plugin, (long)durTicks);
    }

    private static class Vector {
        private Vector() {
        }

        public static org.bukkit.util.Vector of(double x, double y, double z) {
            return new org.bukkit.util.Vector(x, y, z);
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void onArachnidMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        AbilityManager am = this.plugin.getAbilityManager();
        if (!am.hasArachnidHeld(p)) {
            return;
        }
        if (!am.isInCobweb(p)) {
            return;
        }
        Location from = e.getFrom();
        Location to = e.getTo();
        if (to == null) {
            return;
        }
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double curH = Math.sqrt(dx * dx + dz * dz);
        // Target walking speed (~0.215 blocks/tick). When the player is trying to move
        // even a tiny amount, scale them up to vanilla walking speed so cobwebs feel
        // walkable instead of slow / glitchy.
        double walkSpeed = this.plugin.getConfig().getDouble("daggers.arachnid.passive.cobweb-walk-speed", 0.215);
        double maxScale = this.plugin.getConfig().getDouble("daggers.arachnid.passive.cobweb-multiplier", 30.0);
        if (curH < 1.0E-4) {
            return;
        }
        double scale = walkSpeed / curH;
        if (scale < 1.0) scale = 1.0;
        if (scale > maxScale) scale = maxScale;
        double nx = from.getX() + dx * scale;
        double nz = from.getZ() + dz * scale;
        Location newTo = new Location(to.getWorld(), nx, to.getY(), nz, to.getYaw(), to.getPitch());
        if (newTo.getBlock().getType().isSolid()) {
            return;
        }
        Location head = newTo.clone().add(0.0, 1.0, 0.0);
        if (head.getBlock().getType().isSolid() && head.getBlock().getType() != Material.COBWEB) {
            return;
        }
        e.setTo(newTo);
        // Keep momentum across ticks so the player keeps moving instead of being re-clamped to ~0.05/t.
        org.bukkit.util.Vector v = p.getVelocity();
        double vh = Math.sqrt(v.getX() * v.getX() + v.getZ() * v.getZ());
        if (vh < walkSpeed) {
            double mul = (vh < 1.0E-4) ? walkSpeed : (walkSpeed / vh);
            p.setVelocity(new org.bukkit.util.Vector(v.getX() * mul, Math.max(v.getY(), -0.05), v.getZ() * mul));
            p.setFallDistance(0.0f);
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void onMidasKill(EntityDeathEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim instanceof ArmorStand) {
            return;
        }
        Player killer = victim.getKiller();
        if (killer == null) {
            return;
        }
        if (killer.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }
        AbilityManager am = this.plugin.getAbilityManager();
        if (!am.hasDaggerAnywhere(killer, DaggerType.MIDAS)) {
            return;
        }
        int amount = this.plugin.getConfig().getInt("daggers.midas.passive.gold-ingot-amount", 1);
        if (amount <= 0) {
            return;
        }
        e.getDrops().add(new ItemStack(Material.GOLD_INGOT, amount));
    }
}

