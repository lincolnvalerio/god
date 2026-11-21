package com.onikoox.deus.client.render;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;

/** Renderizador de jogador igual ao do jogo (armadura, itens, capa), mas com o modelo TronoModel. */
public final class TronoPlayerRenderer extends PlayerRenderer {
    private static EntityRendererProvider.Context contexto;
    private static TronoPlayerRenderer normal, magro;

    private final TronoModel<AbstractClientPlayer> tm;

    private TronoPlayerRenderer(EntityRendererProvider.Context c, boolean magro) {
        super(c, magro);
        TronoModel<AbstractClientPlayer> m = new TronoModel<>(c.bakeLayer(magro ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), magro);
        this.tm = m;
        this.model = m;
    }

    public static void contexto(EntityRendererProvider.Context c) {
        contexto = c;
        normal = null;
        magro = null;
    }

    public static TronoPlayerRenderer obter(AbstractClientPlayer p) {
        if (contexto == null) return null;
        boolean slim = "slim".equals(p.getModelName());
        if (slim) {
            if (magro == null) magro = new TronoPlayerRenderer(contexto, true);
            return magro;
        }
        if (normal == null) normal = new TronoPlayerRenderer(contexto, false);
        return normal;
    }

    public void preparar(float sentado, float gesto, boolean gestoSentado) {
        tm.sentado = sentado;
        tm.gesto = gesto;
        tm.gestoSentado = gestoSentado;
    }
}
