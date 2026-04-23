/*
 * Decompiled with CFR 0.152.
 */
package com.daggersmp.managers;

import com.daggersmp.DaggerSMP;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ToggleControlsManager {
    private final DaggerSMP plugin;
    private final Set<UUID> disabled = new HashSet<UUID>();

    public ToggleControlsManager(DaggerSMP plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled(UUID uuid) {
        return !this.disabled.contains(uuid);
    }

    public boolean toggle(UUID uuid) {
        if (this.disabled.contains(uuid)) {
            this.disabled.remove(uuid);
            return true;
        }
        this.disabled.add(uuid);
        return false;
    }
}

