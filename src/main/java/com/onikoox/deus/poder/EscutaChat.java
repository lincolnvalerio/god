package com.onikoox.deus.poder;

import com.onikoox.deus.Admin;
import com.onikoox.deus.DeusConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Moderação: encaminha /msg, /tell e /w aos admins que ligaram a escuta. Só funciona se o config
 * "chat_privado_ativo" estiver ligado; nesse caso TODOS os jogadores recebem o aviso ao entrar.
 */
public final class EscutaChat {
    private EscutaChat() {}

    @SubscribeEvent
    public static void comando(CommandEvent e) {
        if (!DeusConfig.CHAT_PRIVADO_ATIVO.get()) return;
        String bruto = e.getParseResults().getReader().getString();
        if (bruto.startsWith("/")) bruto = bruto.substring(1);
        String[] p = bruto.split(" ", 3);
        if (p.length < 3) return;
        String cmd = p[0].toLowerCase();
        if (cmd.startsWith("minecraft:")) cmd = cmd.substring(10);
        if (!cmd.equals("msg") && !cmd.equals("tell") && !cmd.equals("w")) return;
        ServerPlayer rem = e.getParseResults().getContext().getSource().getPlayer();
        if (rem == null) return;
        for (ServerPlayer a : rem.server.getPlayerList().getPlayers()) {
            if (a == rem || !Admin.isAdmin(a) || !EstadoAdmin.get(a, EstadoAdmin.ESCUTA)) continue;
            if (a.getGameProfile().getName().equalsIgnoreCase(p[1])) continue; // o admin é o destinatário: já leu
            a.sendSystemMessage(Component.translatable("msg.deus.sussurro", rem.getGameProfile().getName(), p[1], p[2])
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
    }

    @SubscribeEvent
    public static void entrou(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (DeusConfig.CHAT_PRIVADO_ATIVO.get()) {
            p.sendSystemMessage(Component.literal(DeusConfig.AVISO_TEXTO.get()).withStyle(ChatFormatting.YELLOW));
        }
        if (Admin.isAdmin(p)) EstadoAdmin.enviar(p);
    }
}
