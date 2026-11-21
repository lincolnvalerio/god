package com.onikoox.deus.client;

import com.onikoox.deus.DeusMod;
import com.onikoox.deus.client.render.CorpoRenderer;
import com.onikoox.deus.client.render.TronoPlayerRenderer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DeusMod.ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClienteSetup {
    private ClienteSetup() {}

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(DeusMod.ASSENTO.get(), NoopRenderer::new); // assento é invisível
        e.registerEntityRenderer(DeusMod.CORPO.get(), CorpoRenderer::new);  // corpo do Deus no trono
    }

    /** Guarda o contexto de renderização para criar o desenhista de jogador com a pose de trono. */
    @SubscribeEvent
    public static void camadas(EntityRenderersEvent.AddLayers e) {
        TronoPlayerRenderer.contexto(e.getContext());
    }

    @SubscribeEvent
    public static void overlays(RegisterGuiOverlaysEvent e) {
        e.registerAboveAll("observacao", (gui, g, parcial, largura, altura) -> ClienteHud.desenhar(g, gui.getFont()));
    }

    @SubscribeEvent
    public static void teclas(RegisterKeyMappingsEvent e) {
        e.register(Teclas.ABRIR);
        e.register(Teclas.VOLTAR);
        e.register(Teclas.OBS_ANT);
        e.register(Teclas.OBS_PROX);
        e.register(Teclas.Z);
        e.register(Teclas.X);
        e.register(Teclas.C);
        e.register(Teclas.F);
        e.register(Teclas.V);
        e.register(Teclas.B);
    }
}
