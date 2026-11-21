package com.onikoox.deus.poder;

import com.onikoox.deus.rede.PacoteEstado;
import com.onikoox.deus.rede.Rede;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;

/** Liga/desliga dos poderes de onisciência, guardado no jogador (sobrevive a morrer e relogar). */
public final class EstadoAdmin {
    public static final String RADAR = "radar", XRAY_MIN = "xray_min", XRAY_ENT = "xray_ent", ESCUTA = "escuta";

    private EstadoAdmin() {}

    private static CompoundTag tag(Player p) {
        CompoundTag raiz = p.getPersistentData();
        if (!raiz.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) raiz.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        CompoundTag pers = raiz.getCompound(Player.PERSISTED_NBT_TAG);
        if (!pers.contains("deus", Tag.TAG_COMPOUND)) pers.put("deus", new CompoundTag());
        return pers.getCompound("deus");
    }

    /** O radar vem ligado por padrão; o resto desligado. */
    public static boolean get(Player p, String chave) {
        CompoundTag t = tag(p);
        if (!t.contains(chave)) return chave.equals(RADAR);
        return t.getBoolean(chave);
    }

    public static boolean alternar(Player p, String chave) {
        boolean novo = !get(p, chave);
        tag(p).putBoolean(chave, novo);
        return novo;
    }

    public static void enviar(ServerPlayer p) {
        Rede.CANAL.send(PacketDistributor.PLAYER.with(() -> p),
                new PacoteEstado(com.onikoox.deus.Admin.isAdmin(p), get(p, RADAR), get(p, XRAY_MIN), get(p, XRAY_ENT), get(p, ESCUTA)));
    }
}
