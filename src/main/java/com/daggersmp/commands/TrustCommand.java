/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 */
package com.daggersmp.commands;

import com.daggersmp.DaggerSMP;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TrustCommand
implements CommandExecutor {
    private final DaggerSMP plugin;
    private final boolean trust;

    public TrustCommand(DaggerSMP plugin, boolean trust) {
        this.plugin = plugin;
        this.trust = trust;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Player player = (Player)sender;
        if (args.length == 0) {
            player.sendMessage("\u00a7cUsage: /" + (this.trust ? "trust" : "untrust") + " <player>");
            return true;
        }
        Player target = Bukkit.getPlayer((String)args[0]);
        if (target == null) {
            player.sendMessage("\u00a7cPlayer not found: " + args[0]);
            return true;
        }
        if (this.trust) {
            this.plugin.getTrustManager().trust(player.getUniqueId(), target.getUniqueId());
            player.sendMessage("\u00a7aTrusted \u00a7e" + target.getName() + "\u00a7a \u2014 they won't be affected by your passives.");
        } else {
            this.plugin.getTrustManager().untrust(player.getUniqueId(), target.getUniqueId());
            player.sendMessage("\u00a7eRemoved trust from \u00a7e" + target.getName() + "\u00a7e.");
        }
        return true;
    }
}

