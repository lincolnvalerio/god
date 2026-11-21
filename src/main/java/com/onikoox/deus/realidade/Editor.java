package com.onikoox.deus.realidade;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;

import java.util.Set;

/** Faz as trocas de bloco e de bioma registrando o estado original na Operacao. */
final class Editor {
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    final ServerLevel lv;
    final Operacao op;

    Editor(ServerLevel lv, Operacao op) {
        this.lv = lv;
        this.op = op;
    }

    boolean trocar(BlockPos pos, BlockState novo) {
        BlockState atual = lv.getBlockState(pos);
        if (atual == novo) return false;
        if (atual.getDestroySpeed(lv, pos) < 0) return false; // bedrock, barreira, command block...
        CompoundTag tag = null;
        BlockEntity be = lv.getBlockEntity(pos);
        if (be != null) {
            tag = be.saveWithFullMetadata();
            lv.removeBlockEntity(pos); // evita que baús soltem os itens (que voltariam duplicados na restauração)
        }
        op.blocoOriginal(pos, atual, tag);
        lv.setBlock(pos, novo, FLAGS);
        return true;
    }

    /** Define o bioma de uma célula 4x4x4 (coordenadas de "quart"). Retorna false se o chunk não está carregado. */
    @SuppressWarnings("unchecked")
    boolean bioma(int qx, int qy, int qz, Holder<Biome> novo, boolean registrar, boolean carregar, Set<ChunkAccess> sujos) {
        int x = qx << 2, y = qy << 2, z = qz << 2;
        LevelChunk ch = carregar ? lv.getChunk(x >> 4, z >> 4) : lv.getChunkSource().getChunkNow(x >> 4, z >> 4);
        if (ch == null) return false;
        int idx = ch.getSectionIndex(y);
        if (idx < 0 || idx >= ch.getSectionsCount()) return false;
        LevelChunkSection sec = ch.getSection(idx);
        PalettedContainer<Holder<Biome>> bc = (PalettedContainer<Holder<Biome>>) sec.getBiomes();
        int lx = qx & 3, ly = qy & 3, lz = qz & 3;
        Holder<Biome> antigo = bc.get(lx, ly, lz);
        if (antigo == novo) return false;
        if (registrar) {
            String nome = antigo.unwrapKey().map(k -> k.location().toString()).orElse("minecraft:plains");
            op.biomaOriginal(BlockPos.asLong(qx, qy, qz), nome);
        }
        bc.getAndSetUnchecked(lx, ly, lz, novo);
        ch.setUnsaved(true);
        sujos.add(ch);
        return true;
    }

    void reenviar(Set<ChunkAccess> sujos) {
        if (sujos.isEmpty()) return;
        lv.getChunkSource().chunkMap.resendBiomesForChunks(new java.util.ArrayList<>(sujos));
        sujos.clear();
    }
}
