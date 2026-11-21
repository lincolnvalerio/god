package com.onikoox.deus;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public final class Admin {
    private Admin() {}

    /** Está na lista do config (nick ou UUID)? Sozinho isso NÃO dá poder: é preciso usar /deus. */
    public static boolean naLista(Player p) {
        String nome = p.getGameProfile().getName();
        String uuid = p.getUUID().toString();
        for (String s : DeusConfig.ADMINS.get()) {
            if (s.equalsIgnoreCase(nome) || s.equalsIgnoreCase(uuid)) return true;
        }
        return false;
    }

    /** É O Deus: está na lista E já se tornou o Deus com /deus. Só no servidor. */
    public static boolean isAdmin(Player p) {
        if (!(p instanceof ServerPlayer sp)) return false;
        if (!naLista(p)) return false;
        UUID deus = DeusData.get(sp.server).getDeus();
        return deus != null && deus.equals(p.getUUID());
    }

    /** /deus: quem está na lista e não há outro Deus vira o Deus. */
    public static boolean tornar(ServerPlayer p) {
        if (!naLista(p)) return false;
        DeusData d = DeusData.get(p.server);
        UUID atual = d.getDeus();
        if (atual == null || atual.equals(p.getUUID())) {
            d.setDeus(p.getUUID());
            return true;
        }
        return false;
    }
}
