/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.event.inventory.InventoryType
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 */
package com.daggersmp.commands;

import com.daggersmp.DaggerSMP;
import com.daggersmp.daggers.DaggerType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class DaggerCommand
implements CommandExecutor, TabCompleter {
    private final DaggerSMP plugin;

    public DaggerCommand(DaggerSMP plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub;
        if (args.length == 0) {
            this.sendHelp(sender);
            return true;
        }
        switch (sub = args[0].toLowerCase()) {
            case "give": {
                return this.give(sender, args);
            }
            case "list": {
                return this.list(sender);
            }
            case "togglecontrols": {
                return this.togglecontrols(sender);
            }
            case "recipe": 
            case "recipes": {
                return this.recipe(sender);
            }
            case "cooldown": {
                return this.cooldown(sender, args);
            }
            case "reload": {
                return this.reload(sender);
            }
        }
        this.sendHelp(sender);
        return true;
    }

    private boolean give(CommandSender sender, String[] args) {
        Player target;
        if (!sender.hasPermission("daggersmp.admin")) {
            sender.sendMessage("\u00a7cNo permission.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("\u00a7cUsage: /dagger give <type> [player]");
            return true;
        }
        DaggerType type = DaggerType.fromId(args[1].toLowerCase());
        if (type == null) {
            sender.sendMessage("\u00a7cUnknown dagger: " + args[1]);
            return true;
        }
        if (args.length >= 3) {
            target = Bukkit.getPlayer((String)args[2]);
        } else if (sender instanceof Player) {
            Player p;
            target = p = (Player)sender;
        } else {
            sender.sendMessage("\u00a7cSpecify a player from console.");
            return true;
        }
        if (target == null) {
            sender.sendMessage("\u00a7cPlayer not found.");
            return true;
        }
        target.getInventory().addItem(new ItemStack[]{type.createItem()});
        sender.sendMessage("\u00a7aGave " + type.getDisplayName() + "\u00a7a to " + target.getName());
        return true;
    }

    private boolean list(CommandSender sender) {
        sender.sendMessage("\u00a76=== Daggers ===");
        for (DaggerType t : DaggerType.values()) {
            sender.sendMessage("\u00a77- " + t.getDisplayName());
        }
        return true;
    }

    private boolean togglecontrols(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        Player p = (Player)sender;
        boolean enabled = this.plugin.getToggleControlsManager().toggle(p.getUniqueId());
        if (enabled) {
            p.sendMessage("\u00a7aOffhand-swap controls \u00a7lENABLED\u00a7r\u00a7a \u2014 F triggers Ability 1, Shift+F triggers Ability 2.");
        } else {
            p.sendMessage("\u00a7eOffhand-swap controls \u00a7lDISABLED\u00a7r\u00a7e \u2014 F now swaps items normally. Use /ability1 or /ability2.");
        }
        return true;
    }

    private boolean recipe(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        Player p = (Player)sender;
        DaggerType[] types = DaggerType.values();
        int rows = (int)Math.ceil((double)types.length / 9.0);
        if (rows < 3) {
            rows = 3;
        }
        if (rows > 6) {
            rows = 6;
        }
        RecipeMenuHolder holder = new RecipeMenuHolder(null);
        Inventory inv = Bukkit.createInventory((InventoryHolder)holder, (int)(rows * 9), (String)"Dagger Recipes");
        holder.setInventory(inv);
        for (int i = 0; i < types.length && i < rows * 9; ++i) {
            ItemStack item = types[i].createItem();
            inv.setItem(i, item);
        }
        p.openInventory(inv);
        return true;
    }

    public static void openRecipeFor(Player p, DaggerType type, DaggerSMP plugin) {
        RecipeMenuHolder holder = new RecipeMenuHolder(type);
        Inventory inv = Bukkit.createInventory((InventoryHolder)holder, (InventoryType)InventoryType.WORKBENCH, (String)(type.getDisplayName() + " Recipe"));
        holder.setInventory(inv);
        Material[] mats = plugin.getDaggerManager().getRecipeMaterials(type);
        if (mats == null) {
            p.sendMessage("\u00a7cNo recipe registered.");
            return;
        }
        for (int i = 0; i < 9 && i < mats.length; ++i) {
            ItemStack stack = new ItemStack(mats[i]);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("\u00a7f" + mats[i].name().replace("_", " "));
                stack.setItemMeta(meta);
            }
            inv.setItem(i + 1, stack);
        }
        inv.setItem(0, type.createItem());
        p.openInventory(inv);
    }

    private boolean cooldown(CommandSender sender, String[] args) {
        if (!sender.hasPermission("daggersmp.admin")) {
            sender.sendMessage("\u00a7cNo permission.");
            return true;
        }
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayer((String)args[1]);
            if (target == null) {
                sender.sendMessage("\u00a7cPlayer not found.");
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player)sender;
        } else {
            sender.sendMessage("\u00a7cUsage from console: /dagger cooldown <player>");
            return true;
        }
        this.plugin.getCooldownManager().clearAllCooldowns(target.getUniqueId());
        sender.sendMessage("\u00a7aReset cooldowns for " + target.getName());
        return true;
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : Arrays.asList("give", "list", "togglecontrols", "recipe", "cooldown", "reload")) {
                if (s.startsWith(args[0].toLowerCase())) out.add(s);
            }
            return out;
        }
        String sub = args[0].toLowerCase();
        if (sub.equals("give") && args.length == 2) {
            for (com.daggersmp.daggers.DaggerType t : com.daggersmp.daggers.DaggerType.values()) {
                if (t.getId().startsWith(args[1].toLowerCase())) out.add(t.getId());
            }
        } else if ((sub.equals("give") && args.length == 3) || (sub.equals("cooldown") && args.length == 2)) {
            String prefix = args[args.length - 1].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(prefix)) out.add(p.getName());
            }
        }
        return out;
    }

    private boolean reload(CommandSender sender) {
        if (!sender.hasPermission("daggersmp.admin")) {
            sender.sendMessage("\u00a7cNo permission.");
            return true;
        }
        this.plugin.reloadConfig();
        this.plugin.getDaggerManager().registerRecipes();
        sender.sendMessage("\u00a7aDaggerSMP reloaded.");
        return true;
    }

    private void sendHelp(CommandSender s) {
        s.sendMessage("\u00a76/dagger \u00a77- DaggerSMP commands");
        s.sendMessage("\u00a77  give <type> [player] \u00a78(admin)");
        s.sendMessage("\u00a77  list");
        s.sendMessage("\u00a77  togglecontrols \u00a78- toggle F-key ability activation");
        s.sendMessage("\u00a77  recipe \u00a78- view all dagger recipes");
        s.sendMessage("\u00a77  cooldown [player] \u00a78(admin) - resets cooldowns; defaults to self");
        s.sendMessage("\u00a77  reload \u00a78(admin)");
    }

    public static class RecipeMenuHolder
    implements InventoryHolder {
        public final DaggerType type;
        private Inventory inv;

        public RecipeMenuHolder(DaggerType type) {
            this.type = type;
        }

        public void setInventory(Inventory i) {
            this.inv = i;
        }

        public Inventory getInventory() {
            return this.inv;
        }
    }
}

