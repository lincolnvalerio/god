package com.onikoox.deus.rede;

import com.onikoox.deus.poder.Executor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente -> servidor: "usar o poder X". O servidor revalida tudo (admin, item, poder). */
public class PacoteUsar {
    private final String id;
    private final int valor;
    private final String texto;

    public PacoteUsar(String id, int valor, String texto) {
        this.id = id;
        this.valor = valor;
        this.texto = texto;
    }

    public static void escrever(PacoteUsar m, FriendlyByteBuf b) {
        b.writeUtf(m.id, 64);
        b.writeVarInt(m.valor);
        b.writeUtf(m.texto, 64);
    }

    public static PacoteUsar ler(FriendlyByteBuf b) {
        return new PacoteUsar(b.readUtf(64), b.readVarInt(), b.readUtf(64));
    }

    public static void tratar(PacoteUsar m, Supplier<NetworkEvent.Context> c) {
        NetworkEvent.Context ctx = c.get();
        ctx.enqueueWork(() -> {
            ServerPlayer p = ctx.getSender();
            if (p != null) Executor.executar(p, m.id, m.valor, m.texto);
        });
        ctx.setPacketHandled(true);
    }
}
