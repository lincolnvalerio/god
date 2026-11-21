package com.onikoox.deus.rede;

import com.onikoox.deus.client.ClienteEstado;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> Deus: quais poderes de onisciência estão ligados. */
public class PacoteEstado {
    private final boolean deus, radar, xrayMin, xrayEnt, escuta;

    public PacoteEstado(boolean deus, boolean radar, boolean xrayMin, boolean xrayEnt, boolean escuta) {
        this.deus = deus;
        this.radar = radar;
        this.xrayMin = xrayMin;
        this.xrayEnt = xrayEnt;
        this.escuta = escuta;
    }

    public static void escrever(PacoteEstado m, FriendlyByteBuf b) {
        b.writeBoolean(m.deus);
        b.writeBoolean(m.radar);
        b.writeBoolean(m.xrayMin);
        b.writeBoolean(m.xrayEnt);
        b.writeBoolean(m.escuta);
    }

    public static PacoteEstado ler(FriendlyByteBuf b) {
        return new PacoteEstado(b.readBoolean(), b.readBoolean(), b.readBoolean(), b.readBoolean(), b.readBoolean());
    }

    public static void tratar(PacoteEstado m, Supplier<NetworkEvent.Context> c) {
        NetworkEvent.Context ctx = c.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClienteEstado.definir(m.deus, m.radar, m.xrayMin, m.xrayEnt, m.escuta)));
        ctx.setPacketHandled(true);
    }
}
