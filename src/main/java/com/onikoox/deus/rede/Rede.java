package com.onikoox.deus.rede;

import com.onikoox.deus.DeusMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class Rede {
    private static final String VERSAO = "1";

    public static final SimpleChannel CANAL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(DeusMod.ID, "principal"), () -> VERSAO, VERSAO::equals, VERSAO::equals);

    private Rede() {}

    public static void registrar() {
        int id = 0;
        CANAL.registerMessage(id++, PacoteUsar.class, PacoteUsar::escrever, PacoteUsar::ler, PacoteUsar::tratar);
        CANAL.registerMessage(id++, PacoteGesto.class, PacoteGesto::escrever, PacoteGesto::ler, PacoteGesto::tratar);
        CANAL.registerMessage(id++, PacoteDetalhes.class, PacoteDetalhes::escrever, PacoteDetalhes::ler, PacoteDetalhes::tratar);
        CANAL.registerMessage(id++, PacoteEstado.class, PacoteEstado::escrever, PacoteEstado::ler, PacoteEstado::tratar);
        CANAL.registerMessage(id++, PacoteObservacao.class, PacoteObservacao::escrever, PacoteObservacao::ler, PacoteObservacao::tratar);
    }
}
