/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 */
package com.daggersmp.managers;

import com.daggersmp.DaggerSMP;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;

public class VoidStateManager {
    private final DaggerSMP plugin;
    private final Map<UUID, VoidEntry> voidPlayers = new HashMap<UUID, VoidEntry>();

    public VoidStateManager(DaggerSMP plugin) {
        this.plugin = plugin;
    }

    public void enterVoid(UUID uuid, Location returnLoc) {
        this.voidPlayers.put(uuid, new VoidEntry(returnLoc));
    }

    public boolean isInVoid(UUID uuid) {
        return this.voidPlayers.containsKey(uuid);
    }

    public VoidEntry getEntry(UUID uuid) {
        return this.voidPlayers.get(uuid);
    }

    public void exitVoid(UUID uuid) {
        this.voidPlayers.remove(uuid);
    }

    public static class VoidEntry {
        public Location returnLocation;
        public boolean inVoid;
        public long enteredAt;

        public VoidEntry(Location returnLocation) {
            this.returnLocation = returnLocation;
            this.inVoid = true;
            this.enteredAt = System.currentTimeMillis();
        }
    }
}

