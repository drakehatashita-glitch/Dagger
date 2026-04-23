/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.NamespacedKey
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.Recipe
 *  org.bukkit.inventory.ShapedRecipe
 *  org.bukkit.plugin.Plugin
 */
package com.daggersmp.managers;

import com.daggersmp.DaggerSMP;
import com.daggersmp.daggers.DaggerType;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

public class DaggerManager {
    private final DaggerSMP plugin;
    private final Map<DaggerType, Material[]> recipeMaterials = new HashMap<DaggerType, Material[]>();

    public DaggerManager(DaggerSMP plugin) {
        this.plugin = plugin;
    }

    public void registerRecipes() {
        this.rec(DaggerType.LIFE, Material.MELON_SLICE, Material.ENCHANTED_GOLDEN_APPLE, Material.ARMOR_STAND, Material.PRISMARINE_CRYSTALS, Material.IRON_SWORD, Material.BLACKSTONE, Material.ARMOR_STAND, Material.NETHERITE_CHESTPLATE, Material.ELYTRA);
        this.rec(DaggerType.WIND, Material.FEATHER, Material.ENCHANTED_GOLDEN_APPLE, Material.DIAMOND_BLOCK, Material.PRISMARINE_CRYSTALS, Material.IRON_SWORD, Material.BLACKSTONE, Material.ENCHANTED_BOOK, Material.OBSERVER, Material.AMETHYST_SHARD);
        this.rec(DaggerType.SPEED, Material.LEATHER_BOOTS, Material.ENCHANTED_GOLDEN_APPLE, Material.SHIELD, Material.PRISMARINE_CRYSTALS, Material.IRON_SWORD, Material.BLACKSTONE, Material.SUGAR, Material.COOKIE, Material.FLINT);
        this.rec(DaggerType.CRIMSON, Material.BLACKSTONE_SLAB, Material.ENCHANTED_GOLDEN_APPLE, Material.NETHER_WART_BLOCK, Material.PRISMARINE_CRYSTALS, Material.IRON_SWORD, Material.BLACKSTONE, Material.CRIMSON_NYLIUM, Material.WITHER_ROSE, Material.SOUL_SAND);
        this.rec(DaggerType.DARKNESS, Material.SCULK_CATALYST, Material.ENCHANTED_GOLDEN_APPLE, Material.SCULK_SENSOR, Material.PRISMARINE_CRYSTALS, Material.IRON_SWORD, Material.BLACKSTONE, Material.ECHO_SHARD, Material.CALIBRATED_SCULK_SENSOR, Material.SCULK_SHRIEKER);
        this.rec(DaggerType.HACK, Material.ENDER_EYE, Material.ENCHANTED_GOLDEN_APPLE, Material.TORCH, Material.PRISMARINE_CRYSTALS, Material.IRON_SWORD, Material.BLACKSTONE, Material.DIAMOND_PICKAXE, Material.RECOVERY_COMPASS, Material.BEACON);
        this.rec(DaggerType.FROST, Material.SNOW_BLOCK, Material.ENCHANTED_GOLDEN_APPLE, Material.BEACON, Material.PRISMARINE_CRYSTALS, Material.IRON_SWORD, Material.BLACKSTONE, Material.ICE, Material.STICK, Material.SNOW_BLOCK);
        this.rec(DaggerType.MAFIA, Material.MOSS_BLOCK, Material.ENCHANTED_GOLDEN_APPLE, Material.STICK, Material.PRISMARINE_CRYSTALS, Material.IRON_SWORD, Material.BLACKSTONE, Material.NAUTILUS_SHELL, Material.TOTEM_OF_UNDYING, Material.SPIDER_EYE);
        this.rec(DaggerType.STRENGTH, Material.FURNACE, Material.ENCHANTED_GOLDEN_APPLE, Material.NETHERRACK, Material.PRISMARINE_CRYSTALS, Material.IRON_SWORD, Material.BLACKSTONE, Material.FIRE_CHARGE, Material.ANVIL, Material.STICK);
        this.rec(DaggerType.PIRATE, Material.ELYTRA, Material.ENCHANTED_GOLDEN_APPLE, Material.ARROW, Material.PRISMARINE_CRYSTALS, Material.IRON_SWORD, Material.BLACKSTONE, Material.HEART_OF_THE_SEA, Material.LIME_DYE, Material.SLIME_BALL);
        this.rec(DaggerType.VOID, Material.OBSIDIAN, Material.ENCHANTED_GOLDEN_APPLE, Material.POTION, Material.PRISMARINE_CRYSTALS, Material.IRON_SWORD, Material.BLACKSTONE, Material.ELYTRA, Material.NETHERITE_LEGGINGS, Material.AMETHYST_SHARD);
        this.rec(DaggerType.LUCKY, Material.MOSS_BLOCK, Material.ENCHANTED_GOLDEN_APPLE, Material.MOSS_BLOCK, Material.PRISMARINE_CRYSTALS, Material.IRON_SWORD, Material.BLACKSTONE, Material.SCULK, Material.SLIME_BLOCK, Material.EMERALD_BLOCK);
        this.rec(DaggerType.MIRROR, Material.MUD, Material.ENCHANTED_GOLDEN_APPLE, Material.SHIELD, Material.PRISMARINE_CRYSTALS, Material.IRON_SWORD, Material.BLACKSTONE, Material.STONE, Material.BLACKSTONE, Material.ENDER_EYE);
        this.rec(DaggerType.JUNGLE, Material.MOSS_BLOCK, Material.ENCHANTED_GOLDEN_APPLE, Material.VINE, Material.PRISMARINE_CRYSTALS, Material.IRON_SWORD, Material.BLACKSTONE, Material.MOSS_CARPET, Material.STONE, Material.VINE);
        this.rec(DaggerType.MIDAS, Material.DISPENSER, Material.ENCHANTED_GOLDEN_APPLE, Material.GOLD_NUGGET, Material.PRISMARINE_CRYSTALS, Material.IRON_SWORD, Material.BLACKSTONE, Material.GOLD_INGOT, Material.NAUTILUS_SHELL, Material.GOLD_BLOCK);
        this.rec(DaggerType.TOXIC, Material.POISONOUS_POTATO, Material.ENCHANTED_GOLDEN_APPLE, Material.POTION, Material.PRISMARINE_CRYSTALS, Material.IRON_SWORD, Material.BLACKSTONE, Material.STICK, Material.MUSHROOM_STEW, Material.SLIME_BALL);
        this.rec(DaggerType.ARACHNID, Material.FERMENTED_SPIDER_EYE, Material.ENCHANTED_GOLDEN_APPLE, Material.SHIELD, Material.PRISMARINE_CRYSTALS, Material.IRON_SWORD, Material.BLACKSTONE, Material.DIAMOND_BLOCK, Material.COBWEB, Material.FEATHER);
        this.rec(DaggerType.VAMPIRE, Material.LEATHER_CHESTPLATE, Material.ENCHANTED_GOLDEN_APPLE, Material.SHIELD, Material.PRISMARINE_CRYSTALS, Material.IRON_SWORD, Material.BLACKSTONE, Material.ARMOR_STAND, Material.IRON_BLOCK, Material.REDSTONE);
        this.rec(DaggerType.GRAVITY, Material.ENDER_PEARL, Material.ENCHANTED_GOLDEN_APPLE, Material.BUCKET, Material.PRISMARINE_CRYSTALS, Material.IRON_SWORD, Material.BLACKSTONE, Material.PISTON, Material.DISPENSER, Material.AMETHYST_SHARD);
        this.rec(DaggerType.EARTH, Material.DIAMOND_ORE, Material.ENCHANTED_GOLDEN_APPLE, Material.DIAMOND_BLOCK, Material.PRISMARINE_CRYSTALS, Material.IRON_SWORD, Material.BLACKSTONE, Material.MOSS_BLOCK, Material.COAL_BLOCK, Material.DIRT);
        this.rec(DaggerType.TITAN, Material.NETHERITE_LEGGINGS, Material.ENCHANTED_GOLDEN_APPLE, Material.IRON_LEGGINGS, Material.PRISMARINE_CRYSTALS, Material.IRON_SWORD, Material.BLAZE_ROD, Material.IRON_CHESTPLATE, Material.DIAMOND_PICKAXE, Material.LEATHER_BOOTS);
        this.rec(DaggerType.GUARDIAN, Material.EMERALD, Material.ENCHANTED_GOLDEN_APPLE, Material.SPONGE, Material.PRISMARINE_CRYSTALS, Material.IRON_SWORD, Material.BLACKSTONE, Material.DIAMOND_PICKAXE, Material.DIAMOND, Material.PRISMARINE_SHARD);
        this.rec(DaggerType.GHOST, Material.CANDLE, Material.ENCHANTED_GOLDEN_APPLE, Material.SOUL_LANTERN, Material.PRISMARINE_CRYSTALS, Material.IRON_SWORD, Material.BLACKSTONE, Material.POTION, Material.WRITTEN_BOOK, Material.DIAMOND_BLOCK);
        this.rec(DaggerType.CHANCE, Material.SHIELD, Material.ENCHANTED_GOLDEN_APPLE, Material.SCULK_CATALYST, Material.PRISMARINE_CRYSTALS, Material.IRON_SWORD, Material.BLACKSTONE, Material.BARREL, Material.OBSIDIAN, Material.BEACON);
        this.rec(DaggerType.STORM, Material.SHIELD, Material.ENCHANTED_GOLDEN_APPLE, Material.ARROW, Material.PRISMARINE_CRYSTALS, Material.IRON_SWORD, Material.BLACKSTONE, Material.SLIME_BLOCK, Material.STICK, Material.SLIME_BALL);
    }

    private void rec(DaggerType type, Material ... mats) {
        this.recipeMaterials.put(type, mats);
        NamespacedKey key = new NamespacedKey((Plugin)this.plugin, type.getId() + "_dagger");
        this.plugin.getServer().removeRecipe(key);
        ItemStack result = type.createItem();
        ShapedRecipe r = new ShapedRecipe(key, result);
        r.shape(new String[]{"ABC", "DEF", "GHI"});
        char[] keys = new char[]{'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I'};
        for (int i = 0; i < 9; ++i) {
            r.setIngredient(keys[i], mats[i]);
        }
        this.plugin.getServer().addRecipe((Recipe)r);
    }

    public Material[] getRecipeMaterials(DaggerType type) {
        return this.recipeMaterials.get((Object)type);
    }

    public DaggerType getDaggerType(ItemStack item) {
        return DaggerType.fromItem(item);
    }
}

