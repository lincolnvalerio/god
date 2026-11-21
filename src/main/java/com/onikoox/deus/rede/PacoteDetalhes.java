package com.onikoox.deus.rede;

import com.onikoox.deus.client.ClienteDetalhes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Servidor -> Deus: dados de um jogador (painel de detalhes). */
public class PacoteDetalhes {
    private final DadosJogador d;

    public PacoteDetalhes(DadosJogador d) {
        this.d = d;
    }

    public static void escrever(PacoteDetalhes m, FriendlyByteBuf b) {
        DadosJogador d = m.d;
        b.writeUUID(d.id());
        b.writeUtf(d.nome(), 32);
        b.writeUtf(d.dim(), 64);
        b.writeDouble(d.x());
        b.writeDouble(d.y());
        b.writeDouble(d.z());
        b.writeFloat(d.vida());
        b.writeFloat(d.vidaMax());
        b.writeVarInt(d.fome());
        b.writeVarInt(d.modo());
        b.writeVarInt(d.nivel());
        b.writeBoolean(d.temSalvo());
        b.writeVarInt(d.itens().size());
        for (ItemStack st : d.itens()) b.writeItem(st);
    }

    public static PacoteDetalhes ler(FriendlyByteBuf b) {
        var id = b.readUUID();
        String nome = b.readUtf(32), dim = b.readUtf(64);
        double x = b.readDouble(), y = b.readDouble(), z = b.readDouble();
        float vida = b.readFloat(), vidaMax = b.readFloat();
        int fome = b.readVarInt(), modo = b.readVarInt(), nivel = b.readVarInt();
        boolean salvo = b.readBoolean();
        int n = Math.min(b.readVarInt(), 64);
        List<ItemStack> itens = new ArrayList<>(n);
        for (int i = 0; i < n; i++) itens.add(b.readItem());
        return new PacoteDetalhes(new DadosJogador(id, nome, dim, x, y, z, vida, vidaMax, fome, modo, nivel, salvo, itens));
    }

    public static void tratar(PacoteDetalhes m, Supplier<NetworkEvent.Context> c) {
        NetworkEvent.Context ctx = c.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClienteDetalhes.receber(m.d)));
        ctx.setPacketHandled(true);
    }
}
