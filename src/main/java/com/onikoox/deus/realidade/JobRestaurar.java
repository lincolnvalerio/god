package com.onikoox.deus.realidade;

import com.onikoox.deus.DeusConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Restaura operações (da mais nova para a mais antiga), de fora para dentro, a prazo por tick. */
final class JobRestaurar implements Motor.Job {
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    private final ServerLevel lv;
    private final List<Operacao> ops;
    private final UUID dono;
    private final Registry<Biome> regBioma;
    private final Set<ChunkAccess> sujos = new HashSet<>();

    private int idx = 0, fase = 0, cursor = 0;
    private boolean iniciado = false;
    private Editor ed;

    JobRestaurar(ServerLevel lv, List<Operacao> ops, UUID dono) {
        this.lv = lv;
        this.ops = ops;
        this.dono = dono;
        this.regBioma = lv.registryAccess().registryOrThrow(Registries.BIOME);
    }

    @Override public UUID dono() { return dono; }

    @Override
    public boolean passo(long limite) {
        long ini = System.nanoTime();
        int n = 0, max = DeusConfig.REAL_BLOCOS_POR_TICK.get();
        while (idx < ops.size()) {
            Operacao o = ops.get(idx);
            if (!iniciado) {
                ed = new Editor(lv, o);
                cursor = o.pos.size() - 1;
                fase = 0;
                iniciado = true;
            }
            if (fase == 0) {
                while (cursor >= 0) {
                    bloco(o, cursor--);
                    n++;
                    if (n >= max || ((n & 15) == 0 && System.nanoTime() - ini > limite)) return false;
                }
                fase = 1;
                cursor = 0;
            }
            if (fase == 1) {
                while (cursor < o.bPos.size()) {
                    bioma(o, cursor++);
                    n++;
                    if ((n & 15) == 0 && System.nanoTime() - ini > limite) return false;
                }
                ed.reenviar(sujos);
                idx++;
                iniciado = false;
            }
        }
        return true;
    }

    private void bloco(Operacao o, int i) {
        BlockPos p = BlockPos.of(o.pos.getLong(i));
        BlockState s = o.paleta.get(o.est.getInt(i));
        lv.getChunk(p.getX() >> 4, p.getZ() >> 4); // carrega se preciso
        lv.removeBlockEntity(p);
        lv.setBlock(p, s, FLAGS);
        CompoundTag be = o.blockEntities.get(p.asLong());
        if (be != null) {
            BlockEntity b = lv.getBlockEntity(p);
            if (b != null) {
                b.load(be);
                b.setChanged();
            }
        }
    }

    private void bioma(Operacao o, int i) {
        BlockPos q = BlockPos.of(o.bPos.getLong(i));
        String nome = o.bPaleta.get(o.bId.getInt(i));
        regBioma.getHolder(ResourceKey.create(Registries.BIOME, new ResourceLocation(nome)))
                .ifPresent(h -> ed.bioma(q.getX(), q.getY(), q.getZ(), h, false, true, sujos));
    }
}
