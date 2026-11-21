package com.onikoox.deus.rede;

import com.onikoox.deus.client.ClienteHud;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Servidor -> Deus: lista dos jogadores em observação (vazia = fim da observação). */
public class PacoteObservacao {
    private final List<String> linhas;
    private final int ativo;
    private final boolean sessao, vagando;

    public PacoteObservacao(List<String> linhas, int ativo, boolean sessao, boolean vagando) {
        this.linhas = linhas;
        this.ativo = ativo;
        this.sessao = sessao;
        this.vagando = vagando;
    }

    public static void escrever(PacoteObservacao m, FriendlyByteBuf b) {
        b.writeVarInt(m.linhas.size());
        for (String s : m.linhas) b.writeUtf(s, 96);
        b.writeVarInt(m.ativo);
        b.writeBoolean(m.sessao);
        b.writeBoolean(m.vagando);
    }

    public static PacoteObservacao ler(FriendlyByteBuf b) {
        int n = Math.min(b.readVarInt(), 8);
        List<String> l = new ArrayList<>(n);
        for (int i = 0; i < n; i++) l.add(b.readUtf(96));
        return new PacoteObservacao(l, b.readVarInt(), b.readBoolean(), b.readBoolean());
    }

    public static void tratar(PacoteObservacao m, Supplier<NetworkEvent.Context> c) {
        NetworkEvent.Context ctx = c.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClienteHud.definir(m.linhas, m.ativo, m.sessao, m.vagando)));
        ctx.setPacketHandled(true);
    }
}
