package com.onikoox.deus.poder;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Cópias de inventário guardadas pelo Deus (uma por jogador). Persistem em disco. */
public class InventariosSalvos extends SavedData {
    private final Map<UUID, ListTag> mapa = new HashMap<>();

    public static InventariosSalvos get(MinecraftServer s) {
        return s.overworld().getDataStorage().computeIfAbsent(InventariosSalvos::carregar, InventariosSalvos::new, "deus_inventarios");
    }

    public static InventariosSalvos carregar(CompoundTag t) {
        InventariosSalvos d = new InventariosSalvos();
        for (String k : t.getAllKeys()) {
            try {
                d.mapa.put(UUID.fromString(k), t.getList(k, Tag.TAG_COMPOUND));
            } catch (IllegalArgumentException ignored) {}
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag t) {
        mapa.forEach((k, v) -> t.put(k.toString(), v));
        return t;
    }

    public boolean tem(UUID id) { return mapa.containsKey(id); }

    public ListTag obter(UUID id) { return mapa.get(id); }

    public void guardar(UUID id, ListTag lista) {
        mapa.put(id, lista);
        setDirty();
    }
}
