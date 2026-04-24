package com.daggersmp.commands;

import com.daggersmp.DaggerSMP;
import com.daggersmp.daggers.DaggerType;
import java.util.Collections;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class AbilityCommand implements CommandExecutor, TabCompleter {
    private final DaggerSMP plugin;
    private final int abilityNum;

    public AbilityCommand(DaggerSMP plugin, int abilityNum) {
        this.plugin = plugin;
        this.abilityNum = abilityNum;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("daggersmp.use")) {
            player.sendMessage("\u00a7cNo permission.");
            return true;
        }
        ItemStack mainHeld = player.getInventory().getItemInMainHand();
        ItemStack offHeld = player.getInventory().getItemInOffHand();
        DaggerType type = DaggerType.fromItem(mainHeld);
        if (type == null) {
            type = DaggerType.fromItem(offHeld);
        }
        if (type == null) {
            player.sendMessage("\u00a7cYou must hold a dagger to use abilities!");
            return true;
        }
        this.plugin.getAbilityManager().tryActivate(player, type, this.abilityNum);
        return true;
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
