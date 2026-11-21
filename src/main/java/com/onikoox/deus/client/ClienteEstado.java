package com.onikoox.deus.client;

import com.onikoox.deus.client.tela.PainelCategoriaScreen;
import net.minecraft.client.Minecraft;

/** Estado dos poderes de onisciência no cliente (enviado pelo servidor). */
public final class ClienteEstado {
    /** O servidor confirmou que você é O Deus (libera a roda de poderes). */
    public static volatile boolean deus = false;
    public static volatile boolean radar = true, xrayMin = false, xrayEnt = false, escuta = false;

    private ClienteEstado() {}

    public static void definir(boolean d, boolean r, boolean xm, boolean xe, boolean es) {
        deus = d;
        radar = r;
        xrayMin = xm;
        xrayEnt = xe;
        escuta = es;
        if (Minecraft.getInstance().screen instanceof PainelCategoriaScreen p) p.refrescar();
    }

    public static void limpar() {
        deus = false;
        radar = true;
        xrayMin = xrayEnt = escuta = false;
        ClienteHud.definir(java.util.List.of(), -1, false, false);
    }
}
