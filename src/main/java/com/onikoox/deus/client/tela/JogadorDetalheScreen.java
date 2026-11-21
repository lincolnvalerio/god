package com.onikoox.deus.client.tela;

import com.onikoox.deus.rede.DadosJogador;
import com.onikoox.deus.rede.PacoteUsar;
import com.onikoox.deus.rede.Rede;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/** Retrato completo de um jogador: dados, inventário, armadura e ações do Deus. */
public class JogadorDetalheScreen extends Screen {
    private static final int[] ORDEM_ARMADURA = {39, 38, 37, 36, 40}; // capacete, peitoral, calças, botas, mão secundária

    private DadosJogador d;
    private final String alvo;
    private final Screen anterior;
    private final int[] sx = new int[41], sy = new int[41];
    private int ticks = 0;

    public JogadorDetalheScreen(Screen anterior, DadosJogador d) {
        super(Component.translatable("deus.det.titulo", d.nome()));
        this.anterior = anterior;
        this.d = d;
        this.alvo = d.id().toString();
    }

    public void atualizar(DadosJogador novo) {
        this.d = novo;
    }

    private void enviar(String id, int valor) {
        Rede.CANAL.sendToServer(new PacoteUsar(id, valor, alvo));
    }

    @Override
    protected void init() {
        int gx = width - 16 - 162, gy = 30;
        for (int i = 0; i < ORDEM_ARMADURA.length; i++) {
            sx[ORDEM_ARMADURA[i]] = gx + i * 18;
            sy[ORDEM_ARMADURA[i]] = gy;
        }
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                sx[9 + r * 9 + c] = gx + c * 18;
                sy[9 + r * 9 + c] = gy + 26 + r * 18;
            }
        }
        for (int c = 0; c < 9; c++) {
            sx[c] = gx + c * 18;
            sy[c] = gy + 26 + 3 * 18 + 4;
        }

        int larg = (width - 32 - 3 * 6) / 4, alt = 20;
        int y1 = height - 56, y2 = height - 30;
        botao(0, y1, larg, alt, "deus.det.remover", () -> enviar("onis.equip_remover", 0));
        botao(1, y1, larg, alt, "deus.det.entregar", () -> enviar("onis.entregar", 0));
        botao(2, y1, larg, alt, "deus.det.salvar", () -> enviar("onis.inv_salvar", 0));
        botao(3, y1, larg, alt, "deus.det.restaurar", () -> enviar("onis.inv_restaurar", 0));
        botao(0, y2, larg, alt, "deus.jogadores.ir", () -> enviar("onis.ir", 0));
        botao(1, y2, larg, alt, "deus.jogadores.trazer", () -> enviar("onis.trazer", 0));
        botao(2, y2, larg, alt, "deus.jogadores.observar", () -> {
            enviar("onis.obs_iniciar", 0);
            minecraft.setScreen(null);
        });
        botao(3, y2, larg, alt, "deus.voltar", () -> minecraft.setScreen(anterior));
    }

    private void botao(int coluna, int y, int larg, int alt, String chave, Runnable acao) {
        addRenderableWidget(Button.builder(Component.translatable(chave), b -> acao.run())
                .bounds(16 + coluna * (larg + 6), y, larg, alt).build());
    }

    @Override
    public void tick() {
        if (++ticks % 40 == 0) enviar("onis.detalhe", 0); // atualiza sozinho a cada 2 s
    }

    private static String num(float v) {
        return String.format("%.1f", v);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        g.drawCenteredString(font, title, width / 2, 10, 0xFFFFD700);

        int x = 16, y = 30;
        Component[] linhas = {
                Component.translatable("deus.det.dim", d.dim()),
                Component.translatable("deus.det.pos", (int) Math.floor(d.x()), (int) Math.floor(d.y()), (int) Math.floor(d.z())),
                Component.translatable("deus.det.vida", num(d.vida()), num(d.vidaMax())),
                Component.translatable("deus.det.fome", d.fome()),
                Component.translatable("deus.det.nivel", d.nivel()),
                Component.translatable("deus.det.modo", GameType.byId(d.modo()).getShortDisplayName()),
                Component.translatable(d.temSalvo() ? "deus.det.salvo_sim" : "deus.det.salvo_nao")
        };
        for (Component c : linhas) {
            g.drawString(font, c, x, y, 0xFFFFFFFF, true);
            y += 12;
        }
        g.drawString(font, Component.translatable("deus.det.dica"), x, y + 6, 0xFFAAAAAA, true);

        ItemStack sobre = ItemStack.EMPTY;
        for (int i = 0; i < 41 && i < d.itens().size(); i++) {
            int ix = sx[i], iy = sy[i];
            g.fill(ix, iy, ix + 16, iy + 16, 0x55FFFFFF);
            ItemStack st = d.itens().get(i);
            if (st.isEmpty()) continue;
            g.renderItem(st, ix, iy);
            g.renderItemDecorations(font, st, ix, iy);
            if (mx >= ix && mx < ix + 16 && my >= iy && my < iy + 16) sobre = st;
        }
        super.render(g, mx, my, pt);
        if (!sobre.isEmpty()) g.renderTooltip(font, sobre, mx, my);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int botao) {
        if (botao == 0) {
            for (int i = 0; i < 41 && i < d.itens().size(); i++) {
                if (mx >= sx[i] && mx < sx[i] + 16 && my >= sy[i] && my < sy[i] + 16 && !d.itens().get(i).isEmpty()) {
                    enviar("onis.retirar", i);
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, botao);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
