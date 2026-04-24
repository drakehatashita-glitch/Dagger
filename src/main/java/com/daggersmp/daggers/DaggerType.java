/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.format.TextDecoration
 *  org.bukkit.Material
 *  org.bukkit.NamespacedKey
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.enchantments.Enchantment
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.persistence.PersistentDataType
 */
package com.daggersmp.daggers;

import com.daggersmp.DaggerSMP;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public enum DaggerType {
    STRENGTH("strength", "\u00a7c\u00a7lStrength Dagger", "\u00a7c", 1001),
    SPEED("speed", "\u00a7b\u00a7lSpeed Dagger", "\u00a7b", 1002),
    WIND("wind", "\u00a7f\u00a7lWind Dagger", "\u00a7f", 1003),
    LIFE("life", "\u00a7a\u00a7lLife Dagger", "\u00a7a", 1004),
    CRIMSON("crimson", "\u00a74\u00a7lCrimson Dagger", "\u00a74", 1005),
    DARKNESS("darkness", "\u00a78\u00a7lDarkness Dagger", "\u00a78", 1006),
    HACK("hack", "\u00a72\u00a7lHack Dagger", "\u00a72", 1007),
    FROST("frost", "\u00a73\u00a7lFrost Dagger", "\u00a73", 1008),
    MAFIA("mafia", "\u00a76\u00a7lMafia Dagger", "\u00a76", 1009),
    PIRATE("pirate", "\u00a79\u00a7lPirate Dagger", "\u00a79", 1010),
    VOID("void", "\u00a75\u00a7lVoid Dagger", "\u00a75", 1011),
    LUCKY("lucky", "\u00a7e\u00a7lLucky Dagger", "\u00a7e", 1012),
    MIRROR("mirror", "\u00a77\u00a7lMirror Dagger", "\u00a77", 1013),
    JUNGLE("jungle", "\u00a72\u00a7lJungle Dagger", "\u00a72", 1014),
    MIDAS("midas", "\u00a76\u00a7lMidas Dagger", "\u00a76", 1015),
    TOXIC("toxic", "\u00a7a\u00a7lToxic Dagger", "\u00a7a", 1016),
    ARACHNID("arachnid", "\u00a78\u00a7lArachnid Dagger", "\u00a78", 1017),
    VAMPIRE("vampire", "\u00a74\u00a7lVampire Dagger", "\u00a74", 1018),
    GRAVITY("gravity", "\u00a75\u00a7lGravity Dagger", "\u00a75", 1019),
    EARTH("earth", "\u00a76\u00a7lEarth Dagger", "\u00a76", 1020),
    TITAN("titan", "\u00a7c\u00a7lTitan Dagger", "\u00a7c", 1021),
    GUARDIAN("guardian", "\u00a7b\u00a7lGuardian Dagger", "\u00a7b", 1022),
    GHOST("ghost", "\u00a77\u00a7lGhost Dagger", "\u00a77", 1023),
    CHANCE("chance", "\u00a7d\u00a7lChance Dagger", "\u00a7d", 1024),
    STORM("storm", "\u00a7e\u00a7lStorm Dagger", "\u00a7e", 1025);

    private final String id;
    private final String displayName;
    private final String color;
    private final int customModelData;

    private DaggerType(String id, String displayName, String color, int customModelData) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
        this.customModelData = customModelData;
    }

    public int getCustomModelData() {
        return this.customModelData;
    }

    public String getId() {
        return this.id;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getColor() {
        return this.color;
    }

    public ItemStack createItem() {
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text((String)this.displayName.replace("\u00a7", "")).decoration(TextDecoration.ITALIC, false));
        meta.setDisplayName(this.displayName);
        meta.setLore(this.buildLore());
        NamespacedKey key = new NamespacedKey("daggersmp", "dagger_type");
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, this.id);
        meta.addEnchant(Enchantment.SHARPNESS, 5, true);
        meta.addEnchant(Enchantment.LOOTING, 3, true);
        meta.addEnchant(Enchantment.SWEEPING_EDGE, 3, true);
        meta.setUnbreakable(true);
        meta.setCustomModelData(this.customModelData);
        if (this == CRIMSON) {
            meta.addEnchant(Enchantment.FIRE_ASPECT, 5, true);
        }
        item.setItemMeta(meta);
        return item;
    }

    private FileConfiguration cfg() {
        DaggerSMP plugin = DaggerSMP.getInstance();
        return plugin != null ? plugin.getConfig() : null;
    }

    private int ci(FileConfiguration c, String path, int def) {
        return c != null ? c.getInt("daggers." + this.id + "." + path, def) : def;
    }

    private double cd(FileConfiguration c, String path, double def) {
        return c != null ? c.getDouble("daggers." + this.id + "." + path, def) : def;
    }

    private void addAbility(List<String> lore, FileConfiguration c, int n, String text) {
        int cd = this.ci(c, "ability" + n + ".cooldown-seconds", 30);
        lore.add(this.color + "Ability " + n + ": \u00a77" + text);
        lore.add(this.color + "Cooldown: " + cd + " Seconds");
    }

    private void addPassive(List<String> lore, String text) {
        lore.add(this.color + "Passive: \u00a77" + text);
    }

    public List<String> buildLore() {
        FileConfiguration c = this.cfg();
        ArrayList<String> lore = new ArrayList<String>();
        switch (this.ordinal()) {
            case 0: {
                int pAmp = this.ci(c, "passive.strength-amplifier", 0);
                int aAmp = this.ci(c, "ability1.amplifier", 2);
                int aDur = this.ci(c, "ability1.duration-seconds", 3);
                int aPct = (int)Math.round(this.cd(c, "ability2.armor-durability-percent", 0.15) * 100.0);
                this.addPassive(lore, "Permanently gain " + DaggerType.roman(pAmp + 1) + " Strength");
                lore.add("");
                this.addAbility(lore, c, 1, "Gain Strength " + DaggerType.roman(aAmp + 1) + " for " + aDur + " seconds");
                this.addAbility(lore, c, 2, "Next hit on a player will deal " + aPct + "% armor durability to a random piece");
                break;
            }
            case 1: {
                int pAmp = this.ci(c, "passive.speed-amplifier", 2);
                int a1Amp = this.ci(c, "ability1.amplifier", 4);
                int a1Dur = this.ci(c, "ability1.duration-seconds", 7);
                int a2Pct = (int)Math.round(this.cd(c, "ability2.attack-speed-bonus", 0.5) * 100.0);
                int a2Dur = this.ci(c, "ability2.duration-seconds", 7);
                this.addPassive(lore, "Permanently have Speed " + DaggerType.roman(pAmp + 1));
                lore.add("");
                this.addAbility(lore, c, 1, "Gain Speed " + DaggerType.roman(a1Amp + 1) + " for " + a1Dur + " seconds");
                this.addAbility(lore, c, 2, "Gain " + a2Pct + "% attack speed for " + a2Dur + " seconds");
                break;
            }
            case 2: {
                int dash = (int)this.cd(c, "ability1.dash-blocks", 35.0);
                double dmg = this.cd(c, "ability2.shockwave-damage", 6.0);
                this.addPassive(lore, "Permanent Speed & ignore falling damage");
                lore.add("");
                this.addAbility(lore, c, 1, "Dash forward " + dash + " blocks");
                this.addAbility(lore, c, 2, "Leap upward & create a shockwave on landing \u2014 " + dmg + " HP");
                break;
            }
            case 3: {
                double bonusHp = this.cd(c, "passive.max-health-bonus", 10.0);
                int dur = this.ci(c, "ability2.duration-seconds", 5);
                int regen = this.ci(c, "ability2.regen-amplifier", 1);
                int extra = this.ci(c, "ability1.extra-health-duration-seconds", 10);
                this.addPassive(lore, "+" + (int)(bonusHp / 2.0) + " hearts permanently");
                lore.add("");
                this.addAbility(lore, c, 1, "Steal HP from nearby enemies (extra hearts for " + extra + " seconds)");
                this.addAbility(lore, c, 2, "Grant Regen " + DaggerType.roman(regen + 1) + " to nearby allies for " + dur + " seconds");
                break;
            }
            case 4: {
                int wd = this.ci(c, "ability1.wither-duration-seconds", 4);
                int wa = this.ci(c, "ability1.wither-amplifier", 2);
                double dmg = this.cd(c, "ability2.damage", 6.0);
                this.addPassive(lore, "Permanent Fire Resistance & Haste");
                lore.add("");
                this.addAbility(lore, c, 1, "Next hit inflicts Wither " + DaggerType.roman(wa + 1) + " for " + wd + " seconds");
                this.addAbility(lore, c, 2, "Launch a fireball \u2014 " + dmg + " HP");
                break;
            }
            case 5: {
                int dur = this.ci(c, "ability1.duration-seconds", 5);
                double bdmg = this.cd(c, "ability2.beam-damage", 6.0);
                this.addPassive(lore, "Permanent Strength & Slowness");
                lore.add("");
                this.addAbility(lore, c, 1, "Apply Slowness & Darkness to nearby enemies for " + dur + " seconds");
                this.addAbility(lore, c, 2, "Fire the Warden's Sonic Beam \u2014 " + bdmg + " HP");
                break;
            }
            case 6: {
                int gd = this.ci(c, "ability1.glow-duration-seconds", 30);
                int hd = this.ci(c, "ability2.hitbox-duration-seconds", 5);
                this.addPassive(lore, "Permanent Haste & Speed");
                lore.add("");
                this.addAbility(lore, c, 1, "Reveal nearby entities (glowing for " + gd + " seconds)");
                this.addAbility(lore, c, 2, "Expand your hitbox & damage for " + hd + " seconds");
                break;
            }
            case 7: {
                int fd = this.ci(c, "ability1.freeze-duration-seconds", 5);
                int dd = this.ci(c, "ability2.debuff-duration-seconds", 5);
                int extra = (int)Math.round((this.cd(c, "ability2.incoming-damage-multiplier", 1.15) - 1.0) * 100.0);
                this.addPassive(lore, "Slow enemies you attack");
                lore.add("");
                this.addAbility(lore, c, 1, "Freeze nearby enemies for " + fd + " seconds");
                this.addAbility(lore, c, 2, "Frost field \u2014 frosted enemies take " + extra + "% extra damage for " + dd + " seconds");
                break;
            }
            case 8: {
                int n = this.ci(c, "ability2.vindicator-count", 4);
                int debuff = this.ci(c, "ability1.debuff-duration-seconds", 3);
                this.addPassive(lore, "Permanent invisibility");
                lore.add("");
                this.addAbility(lore, c, 1, "Next hit applies Poison, Weakness & Hunger for " + debuff + " seconds");
                this.addAbility(lore, c, 2, "Summon " + n + " loyal Johnnys that fight for you");
                break;
            }
            case 9: {
                int dur = this.ci(c, "ability1.duration-seconds", 15);
                this.addPassive(lore, "Permanent Water Breathing");
                lore.add("");
                this.addAbility(lore, c, 1, "Gain Dolphin's Grace for " + dur + " seconds");
                this.addAbility(lore, c, 2, "Release a wave that pushes nearby enemies back");
                break;
            }
            case 10: {
                long cdSec = this.ci(c, "passive.teleport-cooldown-seconds", 30);
                int rs = this.ci(c, "ability2.return-seconds", 300);
                int tp = (int)this.cd(c, "ability1.teleport-blocks", 20.0);
                this.addPassive(lore, "Permanent Speed & Strength");
                this.addPassive(lore, "Teleport behind your attacker when hit (" + cdSec + "s CD)");
                lore.add("");
                this.addAbility(lore, c, 1, "Dash through any block (up to " + tp + " blocks)");
                this.addAbility(lore, c, 2, "Save your position. Use again within " + rs + " seconds to return \u2014 cooldown starts after you return");
                break;
            }
            case 11: {
                int n = this.ci(c, "ability1.effect-count", 3);
                int dur = this.ci(c, "ability1.duration-seconds", 10);
                this.addPassive(lore, "Hero of the Village & Luck");
                lore.add("");
                this.addAbility(lore, c, 1, "Apply " + n + " random positive effects for " + dur + " seconds");
                break;
            }
            case 12: {
                int reflect = (int)Math.round(this.cd(c, "passive.copy-chance", 0.05) * 100.0);
                int full = (int)Math.round(this.cd(c, "ability1.reflect-percent", 0.25) * 100.0);
                int dur = this.ci(c, "ability1.full-reflect-duration-seconds", 10);
                this.addPassive(lore, reflect + "% chance to copy enemy positive effects on hit");
                lore.add("");
                this.addAbility(lore, c, 1, "Reflect " + full + "% of damage taken for " + dur + " seconds");
                this.addAbility(lore, c, 2, "Swap positions with the nearest untrusted player");
                break;
            }
            case 13: {
                int range = (int)this.cd(c, "ability1.range", 20.0);
                int timeout = this.ci(c, "ability1.timeout-seconds", 7);
                int pd = this.ci(c, "ability2.poison-duration-seconds", 5);
                this.addPassive(lore, "Resistance");
                lore.add("");
                this.addAbility(lore, c, 1, "Grapple to a block or entity within " + range + " blocks (cancels after " + timeout + "s)");
                this.addAbility(lore, c, 2, "Vine-pull a nearby enemy & poison them for " + pd + " seconds");
                break;
            }
            case 14: {
                int dec = this.ci(c, "ability1.enemy-protection-decrease", 2);
                int eDur = this.ci(c, "ability1.enemy-duration-seconds", 3);
                int inc = this.ci(c, "ability1.trusted-protection-increase", 1);
                int tDur = this.ci(c, "ability1.trusted-duration-seconds", 3);
                int dur = this.ci(c, "ability2.duration-seconds", 5);
                this.addPassive(lore, "Mobs drop gold on death");
                lore.add("");
                this.addAbility(lore, c, 1, "Next hit: enemies lose " + dec + " protection levels for " + eDur + "s; trusted gain +" + inc + " for " + tDur + "s");
                this.addAbility(lore, c, 2, "Grant Resistance to yourself & trusted allies for " + dur + " seconds");
                break;
            }
            case 15: {
                int cdur = this.ci(c, "ability1.cloud-duration-seconds", 5);
                this.addPassive(lore, "Immune to poison");
                lore.add("");
                this.addAbility(lore, c, 1, "Release a poison cloud around you for " + cdur + " seconds");
                this.addAbility(lore, c, 2, "Inject lethal poison on next hit");
                break;
            }
            case 16: {
                int dur = this.ci(c, "ability1.duration-seconds", 2);
                this.addPassive(lore, "Walk through cobwebs freely & climb walls");
                lore.add("");
                this.addAbility(lore, c, 1, "The next attack you do paralyzes enemies for " + dur + " seconds");
                break;
            }
            case 17: {
                double bonus = this.cd(c, "passive.max-health-bonus", 4.0);
                int chance = (int)Math.round(this.cd(c, "passive.bleed-chance", 0.05) * 100.0);
                int heal = (int)Math.round(this.cd(c, "ability1.heal-percent", 0.25) * 100.0);
                int dur = this.ci(c, "ability1.duration-seconds", 8);
                this.addPassive(lore, "+" + (int)(bonus / 2.0) + " hearts & " + chance + "% bleed on hit");
                this.addPassive(lore, "1.5x damage on backstab (stacks with crit)");
                lore.add("");
                this.addAbility(lore, c, 1, "Heal " + heal + "% of damage dealt to players for " + dur + " seconds");
                break;
            }
            case 18: {
                double pdmg = this.cd(c, "ability1.pull-damage", 6.0);
                int dur = this.ci(c, "ability2.duration-seconds", 5);
                this.addPassive(lore, "Reduce your fall damage by 50% (shockwave on big falls)");
                lore.add("");
                this.addAbility(lore, c, 1, "Create a black hole that pulls entities \u2014 " + pdmg + " HP");
                this.addAbility(lore, c, 2, "Levitate nearby enemies for " + dur + " seconds");
                break;
            }
            case 19: {
                int w = this.ci(c, "ability1.width", 4);
                int h = this.ci(c, "ability1.height", 3);
                int dur = this.ci(c, "ability1.duration-seconds", 10);
                double bdmg = this.cd(c, "ability2.boulder-damage", 8.0);
                int eAmp = this.ci(c, "passive.haste-amplifier", 1);
                this.addPassive(lore, "Permanent Haste " + DaggerType.roman(eAmp + 1));
                lore.add("");
                this.addAbility(lore, c, 1, "Raise a wall (" + w + "\u00d7" + h + ") for " + dur + " seconds");
                this.addAbility(lore, c, 2, "Hurl a boulder \u2014 " + bdmg + " HP on impact");
                break;
            }
            case 20: {
                int dur = this.ci(c, "ability2.duration-seconds", 5);
                double scale = this.cd(c, "ability2.scale", 0.4);
                this.addPassive(lore, "Permanent Resistance");
                lore.add("");
                this.addAbility(lore, c, 1, "Next hit on a player makes them grow giant");
                this.addAbility(lore, c, 2, "Shrink yourself to " + scale + "x with Speed for " + dur + " seconds");
                break;
            }
            case 21: {
                int dur = this.ci(c, "ability1.duration-seconds", 8);
                int gdur = this.ci(c, "ability2.duration-seconds", 10);
                this.addPassive(lore, "Deal more damage when low on health");
                lore.add("");
                this.addAbility(lore, c, 1, "Fire the Guardian Beam (cancels if hit) for " + dur + " seconds");
                this.addAbility(lore, c, 2, "Apply Mining Fatigue to nearby enemies for " + gdur + " seconds");
                break;
            }
            case 22: {
                int dash = (int)this.cd(c, "ability1.dash-blocks", 10.0);
                int fdur = this.ci(c, "ability2.flight-duration-seconds", 8);
                this.addPassive(lore, "Chance to dodge incoming hits");
                lore.add("");
                this.addAbility(lore, c, 1, "Dash forward " + dash + " blocks");
                this.addAbility(lore, c, 2, "Enter ghost form (flight + invisible) for " + fdur + " seconds");
                break;
            }
            case 23: {
                int dur = this.ci(c, "ability1.duration-seconds", 20);
                int interval = this.ci(c, "passive.interval-seconds", 30);
                this.addPassive(lore, "Random positive effect every " + interval + " seconds");
                lore.add("");
                this.addAbility(lore, c, 1, "Transform into a random dagger for " + dur + " seconds");
                break;
            }
            case 24: {
                double dmg = this.cd(c, "ability1.damage", 6.0);
                double dmg2 = this.cd(c, "ability2.damage", 4.0);
                this.addPassive(lore, "Permanent Speed & lightning chance on hit");
                lore.add("");
                this.addAbility(lore, c, 1, "Strike lightning on a target \u2014 " + dmg + " HP");
                this.addAbility(lore, c, 2, "Unleash storm bolts at all nearby enemies \u2014 " + dmg2 + " HP each");
                break;
            }
        }
        return lore;
    }

    private static String roman(int n) {
        switch (n) {
            case 1: {
                return "I";
            }
            case 2: {
                return "II";
            }
            case 3: {
                return "III";
            }
            case 4: {
                return "IV";
            }
            case 5: {
                return "V";
            }
            case 6: {
                return "VI";
            }
            case 7: {
                return "VII";
            }
            case 8: {
                return "VIII";
            }
            case 9: {
                return "IX";
            }
            case 10: {
                return "X";
            }
        }
        return String.valueOf(n);
    }

    public static DaggerType fromId(String id) {
        for (DaggerType dt : DaggerType.values()) {
            if (!dt.id.equals(id)) continue;
            return dt;
        }
        return null;
    }

    public static DaggerType fromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey("daggersmp", "dagger_type");
        if (!meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
            return null;
        }
        return DaggerType.fromId((String)meta.getPersistentDataContainer().get(key, PersistentDataType.STRING));
    }
}

