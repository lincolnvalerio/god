package com.onikoox.deus.client.tela;

import com.onikoox.deus.realidade.PerfilBioma;
import com.onikoox.deus.rede.PacoteUsar;
import com.onikoox.deus.rede.Rede;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Escolha de um bioma pronto (deserto, floresta, sakura...). Usa o raio e o modo escolhidos na Realidade. */
public class BiomasScreen extends Screen {
    private final Screen anterior;

    public BiomasScreen(Screen anterior) {
        super(Component.translatable("deus.biomas.titulo"));
        this.anterior = anterior;
    }

    @Override
    protected void init() {
        int largura = 110, altura = 20, folga = 6, colunas = 3;
        int total = colunas * largura + (colunas - 1) * folga;
        int x0 = width / 2 - total / 2, y = 44;

        addRenderableWidget(PreferenciasReal.botaoRaio(x0, y, (total - folga) / 2, altura));
        addRenderableWidget(PreferenciasReal.botaoModo(x0 + (total - folga) / 2 + folga, y, (total - folga) / 2, altura));
        y += altura + folga + 6;

        int i = 0;
        for (PerfilBioma p : PerfilBioma.LISTA) {
            addRenderableWidget(Button.builder(Component.translatable("deus.bioma." + p.id), b -> {
                Rede.CANAL.sendToServer(new PacoteUsar("bioma." + p.id, PreferenciasReal.raio(), PreferenciasReal.modoTexto()));
                minecraft.setScreen(null); // o servidor mira onde você está olhando
            }).bounds(x0 + (i % colunas) * (largura + folga), y + (i / colunas) * (altura + folga), largura, altura).build());
            i++;
        }
        addRenderableWidget(Button.builder(Component.translatable("deus.voltar"),
                b -> minecraft.setScreen(anterior)).bounds(width / 2 - 60, height - 32, 120, altura).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        g.drawCenteredString(font, title, width / 2, 20, 0xFFFFD700);
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
