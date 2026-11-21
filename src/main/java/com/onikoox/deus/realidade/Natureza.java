package com.onikoox.deus.realidade;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** Classifica blocos: o que é vegetação (muda) e o que é terreno natural (muda); construções ficam intactas. */
public final class Natureza {
    private Natureza() {}

    public static boolean vegetacao(BlockState s) {
        Block b = s.getBlock();
        if (s.is(BlockTags.CROPS)) return false; // não destrói plantações
        return s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS)
                || b instanceof BushBlock || b instanceof GrowingPlantBlock || b instanceof VineBlock
                || b instanceof HugeMushroomBlock || b instanceof CactusBlock || b instanceof SugarCaneBlock
                || b instanceof SnowLayerBlock;
    }

    public static boolean terreno(BlockState s) {
        return s.is(BlockTags.DIRT) || s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(BlockTags.SAND)
                || s.is(Blocks.TERRACOTTA) || s.is(BlockTags.ICE) || s.is(Blocks.GRAVEL) || s.is(Blocks.CLAY)
                || s.is(Blocks.SNOW_BLOCK) || s.is(Blocks.SANDSTONE) || s.is(Blocks.RED_SANDSTONE) || s.is(Blocks.MUD);
    }
}
