package com.onikoox.deus.client;

import com.onikoox.deus.client.tela.JogadorDetalheScreen;
import com.onikoox.deus.rede.DadosJogador;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class ClienteDetalhes {
    private ClienteDetalhes() {}

    public static void receber(DadosJogador d) {
        Minecraft mc = Minecraft.getInstance();
        Screen atual = mc.screen;
        if (atual instanceof JogadorDetalheScreen tela) {
            tela.atualizar(d);
        } else {
            mc.setScreen(new JogadorDetalheScreen(atual, d));
        }
    }
}
