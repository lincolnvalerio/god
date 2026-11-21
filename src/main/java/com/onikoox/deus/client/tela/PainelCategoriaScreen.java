package com.onikoox.deus.client.tela;

import com.onikoox.deus.client.ClienteEstado;
import com.onikoox.deus.poder.Poderes;
import com.onikoox.deus.poder.Poderes.Categoria;
import com.onikoox.deus.poder.Poderes.Poder;
import com.onikoox.deus.rede.PacoteUsar;
import com.onikoox.deus.rede.Rede;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Painel de uma categoria: um botão por poder, com botão de voltar para a roda. */
public class PainelCategoriaScreen extends Screen {
    private final Categoria cat;
    private final Screen anterior;
    private HoraSlider slider;

    public PainelCategoriaScreen(Categoria cat, Screen anterior) {
        super(Component.translatable("deus.cat." + cat.name().toLowerCase()));
        this.cat = cat;
        this.anterior = anterior;
    }

    @Override
    protected void init() {
        List<Poder> lista = Poderes.da(cat);
        int largura = 170, altura = 20, folga = 6;
        int total = 2 * largura + folga;
        int x0 = width / 2 - total / 2;
        int y = 44;

        if (cat == Categoria.REALIDADE) {
            addRenderableWidget(PreferenciasReal.botaoRaio(x0, y, largura, altura));
            addRenderableWidget(PreferenciasReal.botaoModo(x0 + largura + folga, y, largura, altura));
            y += altura + folga + 4;
        }

        int i = 0;
        for (Poder p : lista) {
            if (p.id().equals("tempo.horario")) continue; // tratado pelo controle deslizante
            Component nome = Component.translatable("deus.poder." + p.id());
            if (!p.pronto()) nome = nome.copy().append(Component.translatable("deus.em_breve"));
            Boolean ligado = estado(p.id());
            if (ligado != null) nome = nome.copy().append(Component.translatable(ligado ? "deus.estado.on" : "deus.estado.off"));
            Button b = Button.builder(nome, btn -> usar(p))
                    .bounds(x0 + (i % 2) * (largura + folga), y + (i / 2) * (altura + folga), largura, altura).build();
            b.active = p.pronto();
            addRenderableWidget(b);
            i++;
        }
        y += ((i + 1) / 2) * (altura + folga) + 8;

        if (cat == Categoria.TEMPO) {
            slider = new HoraSlider(x0, y, largura, altura, 12);
            addRenderableWidget(slider);
            addRenderableWidget(Button.builder(Component.translatable("deus.horario_aplicar"),
                    b -> enviar("tempo.horario", slider.hora(), "")).bounds(x0 + largura + folga, y, largura, altura).build());
        }

        addRenderableWidget(Button.builder(Component.translatable("deus.voltar"),
                b -> minecraft.setScreen(anterior)).bounds(width / 2 - 60, height - 32, 120, altura).build());
    }

    private static Boolean estado(String id) {
        return switch (id) {
            case "onis.radar" -> ClienteEstado.radar;
            case "onis.xray_min" -> ClienteEstado.xrayMin;
            case "onis.xray_ent" -> ClienteEstado.xrayEnt;
            case "onis.escuta" -> ClienteEstado.escuta;
            default -> null;
        };
    }

    /** Chamado quando o servidor muda o estado de um poder: redesenha os botões. */
    public void refrescar() {
        rebuildWidgets();
    }

    private void usar(Poder p) {
        switch (p.id()) {
            case "onis.jogadores", "onis.observar" -> minecraft.setScreen(new JogadoresScreen(this));
            case "real.biomas" -> minecraft.setScreen(new BiomasScreen(this));
            case "onis.vagar" -> {
                enviar(p.id(), 0, "");
                minecraft.setScreen(null); // fecha a tela para você sair voando
            }
            default -> {
                if (cat == Categoria.REALIDADE) {
                    // O servidor mira onde você está olhando; fecha a tela para você ver acontecer.
                    enviar(p.id(), PreferenciasReal.raio(), PreferenciasReal.modoTexto());
                    minecraft.setScreen(null);
                } else {
                    enviar(p.id(), 0, "");
                }
            }
        }
    }

    private static void enviar(String id, int valor, String texto) {
        Rede.CANAL.sendToServer(new PacoteUsar(id, valor, texto));
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

    /** Controle deslizante 0-23h. */
    private static final class HoraSlider extends AbstractSliderButton {
        HoraSlider(int x, int y, int w, int h, int horaInicial) {
            super(x, y, w, h, Component.empty(), horaInicial / 24.0);
            updateMessage();
        }

        int hora() {
            return Math.min(23, (int) Math.round(value * 24));
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("deus.horario", String.format("%02d:00", hora())));
        }

        @Override
        protected void applyValue() {}
    }
}
