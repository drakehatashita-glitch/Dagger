/*
 * Decompiled with CFR 0.152.
 */
package com.daggersmp.managers;

import com.daggersmp.DaggerSMP;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TrustManager {
    private final Map<UUID, Set<UUID>> trusted = new HashMap<UUID, Set<UUID>>();

    public TrustManager(DaggerSMP plugin) {
    }

    public void trust(UUID owner, UUID target) {
        this.trusted.computeIfAbsent(owner, k -> new HashSet()).add(target);
    }

    public void untrust(UUID owner, UUID target) {
        Set<UUID> set = this.trusted.get(owner);
        if (set != null) {
            set.remove(target);
        }
    }

    public boolean isTrusted(UUID owner, UUID target) {
        Set<UUID> set = this.trusted.get(owner);
        return set != null && set.contains(target);
    }
}

