/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package com.daggersmp;

import com.daggersmp.commands.AbilityCommand;
import com.daggersmp.commands.DaggerCommand;
import com.daggersmp.commands.TrustCommand;
import com.daggersmp.listeners.DaggerListener;
import com.daggersmp.managers.AbilityManager;
import com.daggersmp.managers.ActionBarManager;
import com.daggersmp.managers.CooldownManager;
import com.daggersmp.managers.CraftLimitManager;
import com.daggersmp.managers.DaggerManager;
import com.daggersmp.managers.ToggleControlsManager;
import com.daggersmp.managers.TrustManager;
import com.daggersmp.managers.VoidStateManager;
import org.bukkit.command.CommandExecutor;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class DaggerSMP
extends JavaPlugin {
    private static DaggerSMP instance;
    private DaggerManager daggerManager;
    private CooldownManager cooldownManager;
    private AbilityManager abilityManager;
    private ActionBarManager actionBarManager;
    private TrustManager trustManager;
    private VoidStateManager voidStateManager;
    private CraftLimitManager craftLimitManager;
    private ToggleControlsManager toggleControlsManager;

    public static DaggerSMP getInstance() {
        return instance;
    }

    public void onEnable() {
        instance = this;
        this.saveDefaultConfig();
        this.cooldownManager = new CooldownManager(this);
        this.trustManager = new TrustManager(this);
        this.voidStateManager = new VoidStateManager(this);
        this.craftLimitManager = new CraftLimitManager(this);
        this.toggleControlsManager = new ToggleControlsManager(this);
        this.daggerManager = new DaggerManager(this);
        this.abilityManager = new AbilityManager(this);
        this.actionBarManager = new ActionBarManager(this);
        this.daggerManager.registerRecipes();
        this.getServer().getPluginManager().registerEvents((Listener)new DaggerListener(this), (Plugin)this);
        this.getCommand("dagger").setExecutor((CommandExecutor)new DaggerCommand(this));
        this.getCommand("trust").setExecutor((CommandExecutor)new TrustCommand(this, true));
        this.getCommand("untrust").setExecutor((CommandExecutor)new TrustCommand(this, false));
        this.getCommand("ability1").setExecutor((CommandExecutor)new AbilityCommand(this, 1));
        this.getCommand("ability2").setExecutor((CommandExecutor)new AbilityCommand(this, 2));
        this.getLogger().info("DaggerSMP 2.2.0 enabled (Paper 1.21.11, Java 21).");
    }

    public void onDisable() {
        if (this.abilityManager != null) {
            this.abilityManager.shutdown();
        }
    }

    public DaggerManager getDaggerManager() {
        return this.daggerManager;
    }

    public CooldownManager getCooldownManager() {
        return this.cooldownManager;
    }

    public AbilityManager getAbilityManager() {
        return this.abilityManager;
    }

    public ActionBarManager getActionBarManager() {
        return this.actionBarManager;
    }

    public TrustManager getTrustManager() {
        return this.trustManager;
    }

    public VoidStateManager getVoidStateManager() {
        return this.voidStateManager;
    }

    public CraftLimitManager getCraftLimitManager() {
        return this.craftLimitManager;
    }

    public ToggleControlsManager getToggleControlsManager() {
        return this.toggleControlsManager;
    }
}

