package com.onikoox.deus.realidade;

import com.onikoox.deus.DeusConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** Histórico persistente de transformações (sobrevive a reiniciar o servidor). */
public class Historico extends SavedData {
    public final List<Operacao> ops = new ArrayList<>();

    public static Historico get(MinecraftServer s) {
        return s.overworld().getDataStorage().computeIfAbsent(Historico::carregar, Historico::new, "deus_realidade");
    }

    public static Historico carregar(CompoundTag t) {
        Historico h = new Historico();
        ListTag l = t.getList("ops", Tag.TAG_COMPOUND);
        for (int i = 0; i < l.size(); i++) h.ops.add(Operacao.carregar(l.getCompound(i)));
        return h;
    }

    @Override
    public CompoundTag save(CompoundTag t) {
        ListTag l = new ListTag();
        for (Operacao o : ops) l.add(o.salvar());
        t.put("ops", l);
        return t;
    }

    public void adicionar(Operacao o) {
        ops.add(o);
        int max = DeusConfig.REAL_MAX_OPERACOES.get();
        while (ops.size() > max) ops.remove(0); // as mais antigas deixam de poder ser restauradas
        setDirty();
    }
}
