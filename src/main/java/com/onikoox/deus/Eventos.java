package com.onikoox.deus;

import com.mojang.brigadier.Command;
import com.onikoox.deus.entity.AssentoEntity;
import com.onikoox.deus.poder.EstadoAdmin;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class Eventos {
    private Eventos() {}

    /** Shift no trono não desmonta na hora: inicia a animação de levantar. */
    @SubscribeEvent
    public static void montar(EntityMountEvent e) {
        if (!e.isDismounting() || e.getLevel().isClientSide) return;
        if (!(e.getEntityBeingMounted() instanceof AssentoEntity s)) return;
        if (s.isLiberado() || s.isRemoved()) return;
        e.setCanceled(true);
        s.iniciarLevantar();
    }

    /**
     * /deus (qualquer jogador pode digitar):
     *  - quem está na lista do config e ainda não há outro Deus: torna-se O Deus (sem item, sem nada na mão);
     *  - qualquer outra pessoa que tentar: vai para o Inferno na hora.
     */
    @SubscribeEvent
    public static void comandos(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("deus").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            if (Admin.tornar(p)) {
                EstadoAdmin.enviar(p);
                ctx.getSource().sendSuccess(() -> Component.translatable("msg.deus.agora_deus"), false);
            } else {
                Julgamento.condenar(p);
            }
            return Command.SINGLE_SUCCESS;
        }));
    }
}
