/*
 * Decompiled with CFR 0.152.
 */
package com.daggersmp.managers;

import com.daggersmp.DaggerSMP;
import com.daggersmp.daggers.DaggerType;
import java.util.HashMap;
import java.util.Map;

public class CraftLimitManager {
    private final DaggerSMP plugin;
    private final Map<String, Integer> craftCounts = new HashMap<String, Integer>();

    public CraftLimitManager(DaggerSMP plugin) {
        this.plugin = plugin;
    }

    public boolean canCraft(DaggerType type) {
        int limit = this.plugin.getConfig().getInt("daggers." + type.getId() + ".craft-limit", 1);
        if (limit <= 0) {
            return true;
        }
        return this.craftCounts.getOrDefault(type.getId(), 0) < limit;
    }

    public void recordCraft(DaggerType type) {
        this.craftCounts.merge(type.getId(), 1, Integer::sum);
    }

    public int getCraftCount(DaggerType type) {
        return this.craftCounts.getOrDefault(type.getId(), 0);
    }

    public int getLimit(DaggerType type) {
        return this.plugin.getConfig().getInt("daggers." + type.getId() + ".craft-limit", 1);
    }
}

