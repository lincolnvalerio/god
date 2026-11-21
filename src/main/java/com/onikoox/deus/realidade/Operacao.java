package com.onikoox.deus.realidade;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Uma transformação da realidade e tudo o que ela alterou (estado original de cada bloco e de cada
 * célula de bioma), na ordem da onda. É isso que permite restaurar sem destruir nada.
 */
public class Operacao {
    public UUID id = UUID.randomUUID();
    public String dim = "minecraft:overworld";
    public BlockPos centro = BlockPos.ZERO;
    public int raio;
    public Tema tema = Tema.NETHER;
    public long tempo;

    public final LongArrayList pos = new LongArrayList();
    public final IntArrayList est = new IntArrayList();
    public final List<BlockState> paleta = new ArrayList<>();
    private final Object2IntOpenHashMap<BlockState> indice = new Object2IntOpenHashMap<>();
    public final Long2ObjectOpenHashMap<CompoundTag> blockEntities = new Long2ObjectOpenHashMap<>();

    public final LongArrayList bPos = new LongArrayList();
    public final IntArrayList bId = new IntArrayList();
    public final List<String> bPaleta = new ArrayList<>();
    private final Object2IntOpenHashMap<String> bIndice = new Object2IntOpenHashMap<>();

    public Operacao() {
        indice.defaultReturnValue(-1);
        bIndice.defaultReturnValue(-1);
    }

    public void blocoOriginal(BlockPos p, BlockState s, CompoundTag be) {
        int i = indice.getInt(s);
        if (i < 0) {
            i = paleta.size();
            paleta.add(s);
            indice.put(s, i);
        }
        pos.add(p.asLong());
        est.add(i);
        if (be != null) blockEntities.put(p.asLong(), be);
    }

    public void biomaOriginal(long chave, String bioma) {
        int i = bIndice.getInt(bioma);
        if (i < 0) {
            i = bPaleta.size();
            bPaleta.add(bioma);
            bIndice.put(bioma, i);
        }
        bPos.add(chave);
        bId.add(i);
    }

    public CompoundTag salvar() {
        CompoundTag t = new CompoundTag();
        t.putUUID("id", id);
        t.putString("dim", dim);
        t.putLong("centro", centro.asLong());
        t.putInt("raio", raio);
        t.putString("tema", tema.name());
        t.putLong("tempo", tempo);
        t.putLongArray("pos", pos.toLongArray());
        t.putIntArray("est", est.toIntArray());
        ListTag lp = new ListTag();
        for (BlockState s : paleta) lp.add(NbtUtils.writeBlockState(s));
        t.put("paleta", lp);
        ListTag lb = new ListTag();
        blockEntities.long2ObjectEntrySet().forEach(e -> {
            CompoundTag c = new CompoundTag();
            c.putLong("p", e.getLongKey());
            c.put("d", e.getValue());
            lb.add(c);
        });
        t.put("be", lb);
        t.putLongArray("bpos", bPos.toLongArray());
        t.putIntArray("bid", bId.toIntArray());
        ListTag bp = new ListTag();
        for (String s : bPaleta) bp.add(StringTag.valueOf(s));
        t.put("bpal", bp);
        return t;
    }

    public static Operacao carregar(CompoundTag t) {
        Operacao o = new Operacao();
        o.id = t.getUUID("id");
        o.dim = t.getString("dim");
        o.centro = BlockPos.of(t.getLong("centro"));
        o.raio = t.getInt("raio");
        try { o.tema = Tema.valueOf(t.getString("tema")); } catch (IllegalArgumentException ignored) {}
        o.tempo = t.getLong("tempo");
        for (long l : t.getLongArray("pos")) o.pos.add(l);
        for (int i : t.getIntArray("est")) o.est.add(i);
        ListTag lp = t.getList("paleta", Tag.TAG_COMPOUND);
        for (int i = 0; i < lp.size(); i++) {
            BlockState s = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), lp.getCompound(i));
            o.indice.put(s, o.paleta.size());
            o.paleta.add(s);
        }
        ListTag lb = t.getList("be", Tag.TAG_COMPOUND);
        for (int i = 0; i < lb.size(); i++) {
            CompoundTag c = lb.getCompound(i);
            o.blockEntities.put(c.getLong("p"), c.getCompound("d"));
        }
        for (long l : t.getLongArray("bpos")) o.bPos.add(l);
        for (int i : t.getIntArray("bid")) o.bId.add(i);
        ListTag bp = t.getList("bpal", Tag.TAG_STRING);
        for (int i = 0; i < bp.size(); i++) {
            o.bIndice.put(bp.getString(i), o.bPaleta.size());
            o.bPaleta.add(bp.getString(i));
        }
        return o;
    }
}
