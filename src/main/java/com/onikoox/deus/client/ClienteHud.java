package com.onikoox.deus.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Estado do espírito: lista dos jogadores em observação (até 4) ou "vagando". */
public final class ClienteHud {
    private static volatile List<String> linhas = List.of();
    private static volatile int ativo = -1;
    private static volatile boolean sessao = false, vagando = false;

    private ClienteHud() {}

    public static void definir(List<String> l, int a, boolean s, boolean v) {
        linhas = List.copyOf(l);
        ativo = a;
        sessao = s;
        vagando = v;
    }

    /** Você está fora do corpo (observando ou vagando)? */
    public static boolean observando() {
        return sessao;
    }

    public static void desenhar(GuiGraphics g, Font f) {
        if (!sessao) return;
        List<String> l = linhas;
        int y = 8;
        g.drawString(f, Component.translatable(vagando ? "deus.hud.vagando" : "deus.hud.observando"), 8, y, 0xFFFFD700, true);
        for (int i = 0; i < l.size(); i++) {
            y += 11;
            boolean a = i == ativo;
            g.drawString(f, (a ? "\u25B6 " : "   ") + (i + 1) + ". " + l.get(i), 8, y, a ? 0xFFFFFFFF : 0xFF999999, true);
        }
        y += 13;
        g.drawString(f, Component.translatable("deus.hud.dica1"), 8, y, 0xFF888888, true);
        g.drawString(f, Component.translatable("deus.hud.dica2"), 8, y + 11, 0xFF888888, true);
    }
}
