/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 */
package com.daggersmp.managers;

import com.daggersmp.DaggerSMP;
import com.daggersmp.daggers.DaggerType;
import com.daggersmp.managers.CooldownManager;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class ActionBarManager {
    private final DaggerSMP plugin;

    public ActionBarManager(DaggerSMP plugin) {
        this.plugin = plugin;
        this.startTask();
    }

    private void startTask() {
        new BukkitRunnable(){

            public void run() {
                for (Player player : ActionBarManager.this.plugin.getServer().getOnlinePlayers()) {
                    DaggerType held = DaggerType.fromItem(player.getInventory().getItemInMainHand());
                    if (held == null) {
                        held = DaggerType.fromItem(player.getInventory().getItemInOffHand());
                    }
                    if (held == null) continue;
                    UUID uuid = player.getUniqueId();
                    CooldownManager cm = ActionBarManager.this.plugin.getCooldownManager();
                    long cd1 = cm.getRemainingSeconds(uuid, held, 1);
                    String bar = "\u00a76" + held.getDisplayName() + "  \u00a7aA1: " + (String)(cd1 > 0L ? "\u00a7c" + cd1 + "s" : "\u00a7aReady");
                    player.sendActionBar((Component)Component.text((String)bar));
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 20L);
    }
}

