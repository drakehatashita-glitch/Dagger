/*
 * Decompiled with CFR 0.152.
 */
package com.daggersmp.managers;

import com.daggersmp.DaggerSMP;
import com.daggersmp.daggers.DaggerType;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {
    private final DaggerSMP plugin;
    private final Map<String, Long> cooldowns = new HashMap<String, Long>();

    public CooldownManager(DaggerSMP plugin) {
        this.plugin = plugin;
    }

    private String key(UUID uuid, DaggerType type, int ability) {
        return String.valueOf(uuid) + ":" + type.getId() + ":" + ability;
    }

    public boolean isOnCooldown(UUID uuid, DaggerType type, int ability) {
        String k = this.key(uuid, type, ability);
        Long until = this.cooldowns.get(k);
        if (until == null) {
            return false;
        }
        if (until - System.currentTimeMillis() <= 0L) {
            this.cooldowns.remove(k);
            return false;
        }
        return true;
    }

    public long getRemainingSeconds(UUID uuid, DaggerType type, int ability) {
        String k = this.key(uuid, type, ability);
        Long until = this.cooldowns.get(k);
        if (until == null) {
            return 0L;
        }
        long rem = until - System.currentTimeMillis();
        if (rem <= 0L) {
            this.cooldowns.remove(k);
            return 0L;
        }
        return rem / 1000L + 1L;
    }

    public void setCooldown(UUID uuid, DaggerType type, int ability) {
        double seconds = this.plugin.getConfig().getDouble("daggers." + type.getId() + ".ability" + ability + ".cooldown-seconds", 30.0);
        this.cooldowns.put(this.key(uuid, type, ability), System.currentTimeMillis() + (long)(seconds * 1000.0));
    }

    public void setCooldownSeconds(UUID uuid, DaggerType type, int ability, long seconds) {
        this.cooldowns.put(this.key(uuid, type, ability), System.currentTimeMillis() + seconds * 1000L);
    }

    public void clearCooldown(UUID uuid, DaggerType type, int ability) {
        this.cooldowns.remove(this.key(uuid, type, ability));
    }

    public void clearAllCooldowns(UUID uuid) {
        this.cooldowns.keySet().removeIf(k -> k.startsWith(uuid.toString() + ":"));
    }
}

