package com.onikoox.deus.client;

import com.onikoox.deus.client.tela.RodaPoderesScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import com.onikoox.deus.rede.PacoteUsar;
import com.onikoox.deus.rede.Rede;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = com.onikoox.deus.DeusMod.ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClienteEventos {
    private ClienteEventos() {}

    private static void abrir(Minecraft mc) {
        if (mc.player != null && mc.screen == null && ClienteEstado.deus) {
            mc.setScreen(new RodaPoderesScreen());
        }
    }

    /** Botão do meio do mouse abre a roda (o servidor ainda confere se você é mesmo o Deus). */
    @SubscribeEvent
    public static void mouse(InputEvent.MouseButton.Pre e) {
        if (e.getButton() != GLFW.GLFW_MOUSE_BUTTON_MIDDLE || e.getAction() != GLFW.GLFW_PRESS) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || !ClienteEstado.deus) return;
        e.setCanceled(true);
        abrir(mc);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        ClienteGestos.tick();
        Minecraft mc = Minecraft.getInstance();
        while (Teclas.ABRIR.consumeClick()) abrir(mc);
        while (Teclas.VOLTAR.consumeClick()) {
            if (ClienteHud.observando()) Rede.CANAL.sendToServer(new PacoteUsar("onis.obs_parar", 0, ""));
        }
        while (Teclas.OBS_ANT.consumeClick()) {
            if (ClienteHud.observando()) Rede.CANAL.sendToServer(new PacoteUsar("onis.obs_ant", 0, ""));
        }
        while (Teclas.OBS_PROX.consumeClick()) {
            if (ClienteHud.observando()) Rede.CANAL.sendToServer(new PacoteUsar("onis.obs_prox", 0, ""));
        }
    }

    /** Ao sair do servidor, esquece os poderes ligados (X-Ray, HUD...). */
    @SubscribeEvent
    public static void saiu(ClientPlayerNetworkEvent.LoggingOut e) {
        ClienteEstado.limpar();
    }

}
