package com.daggersmp.managers;

import com.daggersmp.DaggerSMP;
import com.daggersmp.daggers.DaggerType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

public class DaggerManager {
    private static final Object ANY = new Object();

    private final DaggerSMP plugin;
    private final Map<DaggerType, Object[]> recipeMaterials = new HashMap<>();
    private RecipeChoice anyItemChoice;

    public DaggerManager(DaggerSMP plugin) {
        this.plugin = plugin;
    }

    public void registerRecipes() {
        rec(DaggerType.STRENGTH,
            Material.ANCIENT_DEBRIS,        Material.ENCHANTED_GOLDEN_APPLE, Material.NAUTILUS_SHELL,
            Material.NETHER_STAR,           Material.NETHERITE_SWORD,        Material.GILDED_BLACKSTONE,
            Material.BLAZE_POWDER,          Material.NETHERITE_INGOT,        Material.NETHERITE_AXE);

        rec(DaggerType.SPEED,
            Material.NETHERITE_BOOTS,       Material.ENCHANTED_GOLDEN_APPLE, Material.NAUTILUS_SHELL,
            Material.NETHER_STAR,           Material.NETHERITE_SWORD,        Material.NETHERITE_INGOT,
            Material.SUGAR,                 Material.RABBIT_FOOT,            Material.DIAMOND_HORSE_ARMOR);

        rec(DaggerType.WIND,
            Material.FEATHER,               Material.ENCHANTED_GOLDEN_APPLE, Material.DIAMOND_BLOCK,
            Material.NETHER_STAR,           Material.NETHERITE_SWORD,        Material.NETHERITE_INGOT,
            Material.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE, Material.HEAVY_CORE, Material.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE);

        rec(DaggerType.LIFE,
            Material.GLISTERING_MELON_SLICE, Material.ENCHANTED_GOLDEN_APPLE, Material.TOTEM_OF_UNDYING,
            Material.NETHER_STAR,            Material.NETHERITE_SWORD,        Material.TOTEM_OF_UNDYING,
            Material.NETHERITE_CHESTPLATE,   Material.VEX_ARMOR_TRIM_SMITHING_TEMPLATE, Material.CRIMSON_PLANKS);

        rec(DaggerType.CRIMSON,
            Material.WITHER_SKELETON_SKULL, Material.ENCHANTED_GOLDEN_APPLE, Material.CRIMSON_HYPHAE,
            Material.NETHER_STAR,           Material.NETHERITE_SWORD,        Material.NETHERITE_INGOT,
            Material.RED_NETHER_BRICKS,     Material.WITHER_ROSE,            Material.WITHER_SKELETON_SKULL);

        rec(DaggerType.DARKNESS,
            Material.SCULK_SHRIEKER,        Material.ENCHANTED_GOLDEN_APPLE, Material.SCULK_SENSOR,
            Material.NETHER_STAR,           Material.NETHERITE_SWORD,        Material.NETHERITE_INGOT,
            Material.SCULK_VEIN,            Material.MUSIC_DISC_5,           Material.SCULK_CATALYST);

        rec(DaggerType.HACK,
            Material.ENDER_EYE,             Material.ENCHANTED_GOLDEN_APPLE, Material.SPYGLASS,
            Material.NETHER_STAR,           Material.NETHERITE_SWORD,        Material.NETHERITE_INGOT,
            Material.NETHERITE_PICKAXE,     Material.RECOVERY_COMPASS,       Material.BEACON);

        rec(DaggerType.FROST,
            Material.NAUTILUS_SHELL,        Material.ENCHANTED_GOLDEN_APPLE, Material.BEACON,
            Material.NETHER_STAR,           Material.NETHERITE_SWORD,        Material.POWDER_SNOW_BUCKET,
            Material.NETHERITE_SHOVEL,      Material.PEARLESCENT_FROGLIGHT,  Material.ICE);

        rec(DaggerType.MAFIA,
            Material.DEEPSLATE_EMERALD_ORE, Material.ENCHANTED_GOLDEN_APPLE, Material.NAME_TAG,
            Material.NETHER_STAR,           Material.NETHERITE_SWORD,        Material.NAUTILUS_SHELL,
            Material.EYE_ARMOR_TRIM_SMITHING_TEMPLATE, Material.DEEPSLATE_EMERALD_ORE, Material.BLACKSTONE);

        rec(DaggerType.PIRATE,
            Material.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, Material.ENCHANTED_GOLDEN_APPLE, Material.TRIDENT,
            Material.NETHER_STAR,           Material.NETHERITE_SWORD,        Material.HEART_OF_THE_SEA,
            Material.TURTLE_SCUTE,          Material.NAUTILUS_SHELL,         Material.BLACKSTONE);

        rec(DaggerType.VOID,
            Material.DRAGON_HEAD,           Material.ENCHANTED_GOLDEN_APPLE, Material.DRAGON_BREATH,
            Material.NETHER_STAR,           Material.NETHERITE_SWORD,        Material.ELYTRA,
            Material.SHULKER_SHELL,         Material.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE, Material.END_STONE);

        rec(DaggerType.LUCKY,
            Material.EMERALD_ORE,           Material.ENCHANTED_GOLDEN_APPLE, Material.DEEPSLATE_EMERALD_ORE,
            Material.NETHER_STAR,           Material.NETHERITE_SWORD,        Material.MUSIC_DISC_11,
            Material.CREEPER_HEAD,          Material.EMERALD_BLOCK,          Material.SKELETON_SKULL);

        rec(DaggerType.MIRROR,
            Material.ANCIENT_DEBRIS,        Material.ENCHANTED_GOLDEN_APPLE, Material.NAUTILUS_SHELL,
            Material.NETHER_STAR,           Material.NETHERITE_SWORD,        Material.TINTED_GLASS,
            Material.CREAKING_HEART,        Material.ENDER_EYE,              Material.AMETHYST_BLOCK);

        rec(DaggerType.JUNGLE,
            Material.MOSSY_COBBLESTONE,     Material.ENCHANTED_GOLDEN_APPLE, Material.WARPED_ROOTS,
            Material.NETHER_STAR,           Material.NETHERITE_SWORD,        Material.VINE,
            Material.WILD_ARMOR_TRIM_SMITHING_TEMPLATE, Material.WEEPING_VINES, Material.TWISTING_VINES);

        rec(DaggerType.MIDAS,
            Material.SKELETON_SKULL,        Material.ENCHANTED_GOLDEN_APPLE, Material.NAUTILUS_SHELL,
            Material.NETHER_STAR,           Material.NETHERITE_SWORD,        Material.GOLDEN_HORSE_ARMOR,
            Material.EYE_ARMOR_TRIM_SMITHING_TEMPLATE, Material.GOLD_BLOCK,  Material.GOLD_INGOT);

        rec(DaggerType.TOXIC,
            Material.POISONOUS_POTATO,      Material.ENCHANTED_GOLDEN_APPLE, Material.PUFFERFISH_BUCKET,
            Material.NETHER_STAR,           Material.NETHERITE_SWORD,        Material.SLIME_BALL,
            Material.NAUTILUS_SHELL,        Material.FERMENTED_SPIDER_EYE,   Material.SPIDER_EYE);

        rec(DaggerType.ARACHNID,
            Material.FERMENTED_SPIDER_EYE,  Material.ENCHANTED_GOLDEN_APPLE, Material.NAUTILUS_SHELL,
            Material.NETHER_STAR,           Material.NETHERITE_SWORD,        Material.DIAMOND_BLOCK,
            Material.COBWEB,                Material.SHEARS,                 Material.SPIDER_EYE);

        rec(DaggerType.VAMPIRE,
            Material.RIB_ARMOR_TRIM_SMITHING_TEMPLATE, Material.ENCHANTED_GOLDEN_APPLE, Material.NAUTILUS_SHELL,
            Material.NETHER_STAR,           Material.NETHERITE_SWORD,        Material.TOTEM_OF_UNDYING,
            Material.WITHER_SKELETON_SKULL, Material.REDSTONE,               Material.SPIDER_EYE);

        rec(DaggerType.GRAVITY,
            Material.WARD_ARMOR_TRIM_SMITHING_TEMPLATE, Material.ENCHANTED_GOLDEN_APPLE, Material.WATER_BUCKET,
            Material.NETHER_STAR,           Material.NETHERITE_SWORD,        Material.HEAVY_CORE,
            Material.DRIED_GHAST,           Material.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE, Material.SAND);

        rec(DaggerType.EARTH,
            Material.DIAMOND_ORE,           Material.ENCHANTED_GOLDEN_APPLE, Material.DIAMOND_BLOCK,
            Material.NETHER_STAR,           Material.NETHERITE_SWORD,        Material.DEEPSLATE_EMERALD_ORE,
            Material.DEEPSLATE_COAL_ORE,    Material.MYCELIUM,               Material.DIRT);

        rec(DaggerType.TITAN,
            Material.NETHERITE_HELMET,      Material.ENCHANTED_GOLDEN_APPLE, Material.NETHERITE_CHESTPLATE,
            Material.NETHER_STAR,           Material.NETHERITE_SWORD,        Material.SPYGLASS,
            Material.DIAMOND_PICKAXE,       Material.NETHERITE_LEGGINGS,     Material.NETHERITE_BOOTS);

        rec(DaggerType.GUARDIAN,
            Material.PRISMARINE_SHARD,      Material.ENCHANTED_GOLDEN_APPLE, Material.SPONGE,
            Material.NETHER_STAR,           Material.NETHERITE_SWORD,        Material.NETHERITE_PICKAXE,
            Material.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE, Material.PRISMARINE_CRYSTALS, Material.SEA_LANTERN);

        rec(DaggerType.GHOST,
            Material.CANDLE,                Material.ENCHANTED_GOLDEN_APPLE, Material.SOUL_CAMPFIRE,
            Material.NETHER_STAR,           Material.NETHERITE_SWORD,        Material.SOUL_LANTERN,
            Material.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE, Material.DIAMOND_BLOCK, Material.PHANTOM_MEMBRANE);

        rec(DaggerType.CHANCE,
            Material.NAUTILUS_SHELL,        Material.ENCHANTED_GOLDEN_APPLE, Material.DEEPSLATE_EMERALD_ORE,
            Material.NETHER_STAR,           Material.NETHERITE_SWORD,        Material.CONDUIT,
            Material.DRAGON_EGG,            Material.BEACON,                 ANY);

        rec(DaggerType.STORM,
            Material.NAUTILUS_SHELL,        Material.ENCHANTED_GOLDEN_APPLE, Material.TRIDENT,
            Material.NETHER_STAR,           Material.NETHERITE_SWORD,        Material.CREEPER_HEAD,
            Material.LIGHTNING_ROD,         Material.ZOMBIE_HEAD,            Material.GHAST_TEAR);
    }

    private RecipeChoice getAnyItemChoice() {
        if (this.anyItemChoice == null) {
            List<Material> items = new ArrayList<>();
            for (Material m : Material.values()) {
                if (m.isItem() && !m.isAir() && !m.isLegacy()) {
                    items.add(m);
                }
            }
            this.anyItemChoice = new RecipeChoice.MaterialChoice(items);
        }
        return this.anyItemChoice;
    }

    private void rec(DaggerType type, Object... mats) {
        this.recipeMaterials.put(type, mats);
        NamespacedKey key = new NamespacedKey((Plugin) this.plugin, type.getId() + "_dagger");
        this.plugin.getServer().removeRecipe(key);
        ItemStack result = type.createItem();
        ShapedRecipe r = new ShapedRecipe(key, result);
        r.shape("ABC", "DEF", "GHI");
        char[] keys = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I'};
        for (int i = 0; i < 9; ++i) {
            Object o = mats[i];
            if (o == ANY) {
                r.setIngredient(keys[i], getAnyItemChoice());
            } else if (o instanceof Material) {
                r.setIngredient(keys[i], (Material) o);
            } else if (o instanceof RecipeChoice) {
                r.setIngredient(keys[i], (RecipeChoice) o);
            }
        }
        this.plugin.getServer().addRecipe((Recipe) r);
    }

    public Material[] getRecipeMaterials(DaggerType type) {
        Object[] mats = this.recipeMaterials.get(type);
        if (mats == null) return null;
        Material[] out = new Material[mats.length];
        for (int i = 0; i < mats.length; i++) {
            out[i] = (mats[i] instanceof Material) ? (Material) mats[i] : Material.BARRIER;
        }
        return out;
    }

    public DaggerType getDaggerType(ItemStack item) {
        return DaggerType.fromItem(item);
    }
}
