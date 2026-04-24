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
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
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
        this.migrateConfig();
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
        DaggerCommand daggerCmd = new DaggerCommand(this);
        TrustCommand trustCmd = new TrustCommand(this, true);
        TrustCommand untrustCmd = new TrustCommand(this, false);
        AbilityCommand a1 = new AbilityCommand(this, 1);
        AbilityCommand a2 = new AbilityCommand(this, 2);
        PluginCommand dc = this.getCommand("dagger");
        dc.setExecutor((CommandExecutor)daggerCmd);
        dc.setTabCompleter((TabCompleter)daggerCmd);
        PluginCommand tc = this.getCommand("trust");
        tc.setExecutor((CommandExecutor)trustCmd);
        tc.setTabCompleter((TabCompleter)trustCmd);
        PluginCommand uc = this.getCommand("untrust");
        uc.setExecutor((CommandExecutor)untrustCmd);
        uc.setTabCompleter((TabCompleter)untrustCmd);
        PluginCommand a1c = this.getCommand("ability1");
        a1c.setExecutor((CommandExecutor)a1);
        a1c.setTabCompleter((TabCompleter)a1);
        PluginCommand a2c = this.getCommand("ability2");
        a2c.setExecutor((CommandExecutor)a2);
        a2c.setTabCompleter((TabCompleter)a2);
        this.getLogger().info("DaggerSMP 2.2.0 enabled (Paper 1.21.11, Java 21).");
    }

    /**
     * One-time migration of stale config keys from older v2.2.0 builds to the current rewritten defaults.
     * Bukkit's getDouble/getString/etc. return the SAVED value if present (ignoring the new default in
     * the bundled config.yml), so old saved configs would otherwise shadow the new behavior. This forces
     * the rewritten knobs back to the new values and persists them.
     */
    private void migrateConfig() {
        org.bukkit.configuration.file.FileConfiguration cfg = this.getConfig();
        int currentVersion = cfg.getInt("config-version", 0);
        int targetVersion = 4;
        if (currentVersion >= targetVersion) {
            return;
        }
        // v3 — rewritten earth A1 obsidian, wind A1 dash 20, mirror A1 reflect 50%, storm A2 hits.
        cfg.set("daggers.earth.ability1.material", "OBSIDIAN");
        cfg.set("daggers.wind.ability1.dash-blocks", 20.0);
        cfg.set("daggers.mirror.ability1.reflect-percent", 0.5);
        cfg.set("daggers.storm.ability2.damage", 4.0);
        cfg.set("daggers.storm.ability2.radius", 5.0);
        cfg.set("daggers.storm.ability2.aoe-radius", 3.5);
        // v4 — gravity passive switched from fall-shockwave to 50% knockback reduction;
        //      arachnid wall-climb slowed; arachnid cobweb-walk replaced with cobweb-shred.
        cfg.set("daggers.gravity.passive.knockback-reduction", 0.5);
        cfg.set("daggers.arachnid.passive.wallclimb-y-velocity", 0.22);
        cfg.set("config-version", targetVersion);
        this.saveConfig();
        this.getLogger().info("DaggerSMP config migrated to v" + targetVersion + " (gravity knockback reduction, slower wall-climb, web shredder).");
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

