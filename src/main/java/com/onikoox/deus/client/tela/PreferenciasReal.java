package com.onikoox.deus.client.tela;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Raio e modo (Superfície / Domínio) escolhidos para a Realidade; valem para todas as telas. */
public final class PreferenciasReal {
    public static final int[] RAIOS = {16, 32, 64, 96};
    private static int raioSel = 1;
    public static boolean cupula = false;

    private PreferenciasReal() {}

    public static int raio() {
        return RAIOS[raioSel];
    }

    /** Texto enviado ao servidor para indicar o modo. */
    public static String modoTexto() {
        return cupula ? "cupula" : "";
    }

    private static Component textoRaio() {
        return Component.translatable("deus.raio", raio());
    }

    private static Component textoModo() {
        return Component.translatable("deus.real.modo",
                Component.translatable(cupula ? "deus.modo.dominio" : "deus.modo.superficie"));
    }

    public static Button botaoRaio(int x, int y, int w, int h) {
        return Button.builder(textoRaio(), b -> {
            raioSel = (raioSel + 1) % RAIOS.length;
            b.setMessage(textoRaio());
        }).bounds(x, y, w, h).build();
    }

    public static Button botaoModo(int x, int y, int w, int h) {
        return Button.builder(textoModo(), b -> {
            cupula = !cupula;
            b.setMessage(textoModo());
        }).bounds(x, y, w, h).build();
    }
}
