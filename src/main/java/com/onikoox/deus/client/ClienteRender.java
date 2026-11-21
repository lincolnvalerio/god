package com.onikoox.deus.client;

import com.onikoox.deus.DeusMod;
import com.onikoox.deus.client.render.TronoPlayerRenderer;
import com.onikoox.deus.entity.AssentoEntity;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Quando um jogador está sentado no trono ou conjurando, o desenho padrão é cancelado e usamos o nosso
 * (mesmo jogador, com a pose de trono / gesto animados).
 */
@Mod.EventBusSubscriber(modid = DeusMod.ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClienteRender {
    private static boolean dentro = false;

    private ClienteRender() {}

    @SubscribeEvent
    public static void jogador(RenderPlayerEvent.Pre e) {
        if (dentro) return; // nosso próprio desenho também dispara este evento
        if (!(e.getEntity() instanceof AbstractClientPlayer ap)) return;
        AssentoEntity assento = ap.getVehicle() instanceof AssentoEntity s ? s : null;
        float gesto = ClienteGestos.intensidade(ap.getId());
        if (assento == null && gesto <= 0F) return;
        TronoPlayerRenderer r = TronoPlayerRenderer.obter(ap);
        if (r == null) return;

        float parcial = e.getPartialTick();
        r.preparar(assento != null ? assento.pose(parcial) : 0F, gesto, assento != null);
        e.setCanceled(true);
        dentro = true;
        try {
            r.render(ap, Mth.lerp(parcial, ap.yRotO, ap.getYRot()), parcial, e.getPoseStack(), e.getMultiBufferSource(), e.getPackedLight());
        } finally {
            dentro = false;
        }
    }
}
