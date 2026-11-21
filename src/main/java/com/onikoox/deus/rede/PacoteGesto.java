package com.onikoox.deus.rede;

import com.onikoox.deus.client.ClienteGestos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> clientes: "esta entidade está conjurando" (animação sentado ou em pé). */
public class PacoteGesto {
    private final int entidade;
    private final int duracao;
    private final boolean sentado;

    public PacoteGesto(int entidade, int duracao, boolean sentado) {
        this.entidade = entidade;
        this.duracao = duracao;
        this.sentado = sentado;
    }

    public static void escrever(PacoteGesto m, FriendlyByteBuf b) {
        b.writeVarInt(m.entidade);
        b.writeVarInt(m.duracao);
        b.writeBoolean(m.sentado);
    }

    public static PacoteGesto ler(FriendlyByteBuf b) {
        return new PacoteGesto(b.readVarInt(), b.readVarInt(), b.readBoolean());
    }

    public static void tratar(PacoteGesto m, Supplier<NetworkEvent.Context> c) {
        NetworkEvent.Context ctx = c.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClienteGestos.tocar(m.entidade, m.duracao, m.sentado)));
        ctx.setPacketHandled(true);
    }
}
