package com.onikoox.deus;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.UUID;

/** Guarda quem é O Deus. Só pode existir um; sobrevive a morte e a reiniciar o servidor. */
public class DeusData extends SavedData {
    private UUID deus;

    public static DeusData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(DeusData::carregar, DeusData::new, "deus_unico");
    }

    public static DeusData carregar(CompoundTag t) {
        DeusData d = new DeusData();
        if (t.hasUUID("deus")) d.deus = t.getUUID("deus");
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag t) {
        if (deus != null) t.putUUID("deus", deus);
        return t;
    }

    public UUID getDeus() { return deus; }

    public void setDeus(UUID id) {
        this.deus = id;
        setDirty();
    }
}
