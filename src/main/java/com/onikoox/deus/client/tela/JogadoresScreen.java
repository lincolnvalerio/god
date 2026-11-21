package com.onikoox.deus.client.tela;

import com.mojang.authlib.GameProfile;
import com.onikoox.deus.rede.PacoteUsar;
import com.onikoox.deus.rede.Rede;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Painel de jogadores: lista quem está conectado. Detalhes, ir até, trazer e observar. */
public class JogadoresScreen extends Screen {
    private static final int POR_PAGINA = 9;
    private final Screen anterior;
    private final List<GameProfile> jogadores = new ArrayList<>();
    private int pagina = 0;

    public JogadoresScreen(Screen anterior) {
        super(Component.translatable("deus.jogadores.titulo"));
        this.anterior = anterior;
    }

    private static void enviar(String id, GameProfile gp) {
        Rede.CANAL.sendToServer(new PacoteUsar(id, 0, gp.getId().toString()));
    }

    @Override
    protected void init() {
        jogadores.clear();
        if (minecraft.getConnection() != null && minecraft.player != null) {
            for (PlayerInfo info : minecraft.getConnection().getOnlinePlayers()) {
                if (!info.getProfile().getId().equals(minecraft.player.getUUID())) jogadores.add(info.getProfile());
            }
        }
        jogadores.sort(Comparator.comparing(GameProfile::getName, String.CASE_INSENSITIVE_ORDER));

        int paginas = Math.max(1, (jogadores.size() + POR_PAGINA - 1) / POR_PAGINA);
        pagina = Math.min(pagina, paginas - 1);
        int x0 = width / 2 - 175, y = 44;
        for (int i = 0; i < POR_PAGINA; i++) {
            int idx = pagina * POR_PAGINA + i;
            if (idx >= jogadores.size()) break;
            GameProfile gp = jogadores.get(idx);
            int yy = y + i * 24;
            addRenderableWidget(Button.builder(Component.translatable("deus.jogadores.detalhes"),
                    b -> enviar("onis.detalhe", gp)).bounds(x0 + 112, yy, 60, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("deus.jogadores.ir"),
                    b -> enviar("onis.ir", gp)).bounds(x0 + 176, yy, 34, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("deus.jogadores.trazer"),
                    b -> enviar("onis.trazer", gp)).bounds(x0 + 214, yy, 50, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("deus.jogadores.observar"), b -> {
                enviar("onis.obs_iniciar", gp);
                minecraft.setScreen(null);
            }).bounds(x0 + 268, yy, 58, 20).build());
        }
        if (paginas > 1) {
            addRenderableWidget(Button.builder(Component.literal("<"), b -> { pagina = Math.max(0, pagina - 1); rebuildWidgets(); })
                    .bounds(width / 2 - 70, height - 58, 30, 20).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> { pagina = Math.min(paginas - 1, pagina + 1); rebuildWidgets(); })
                    .bounds(width / 2 + 40, height - 58, 30, 20).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("deus.jogadores.parar"),
                b -> Rede.CANAL.sendToServer(new PacoteUsar("onis.obs_parar", 0, "")))
                .bounds(width / 2 - 130, height - 32, 120, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("deus.voltar"),
                b -> minecraft.setScreen(anterior)).bounds(width / 2 + 10, height - 32, 120, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        g.drawCenteredString(font, title, width / 2, 20, 0xFFFFD700);
        int x0 = width / 2 - 175;
        if (jogadores.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("deus.jogadores.vazio"), width / 2, 70, 0xFFAAAAAA);
        }
        for (int i = 0; i < POR_PAGINA; i++) {
            int idx = pagina * POR_PAGINA + i;
            if (idx >= jogadores.size()) break;
            g.drawString(font, jogadores.get(idx).getName(), x0, 50 + i * 24, 0xFFFFFFFF, true);
        }
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
