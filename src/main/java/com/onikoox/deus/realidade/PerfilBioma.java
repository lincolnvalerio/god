package com.onikoox.deus.realidade;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/** Um bioma pronto: bioma do jogo + blocos do chão + plantas + árvores. */
public final class PerfilBioma {
    public final String id, bioma;
    private final Block topo, meio, base;
    private Block topoAlt;
    private double chanceAlt;
    private final List<Block> plantas = new ArrayList<>();
    private final List<Double> chances = new ArrayList<>();
    public String[] arvores = new String[0];
    public double densidade = 0;
    public boolean congela = false;
    private boolean mesa = false;

    private static final Block[] BANDAS = {Blocks.TERRACOTTA, Blocks.ORANGE_TERRACOTTA, Blocks.RED_TERRACOTTA,
            Blocks.YELLOW_TERRACOTTA, Blocks.WHITE_TERRACOTTA, Blocks.BROWN_TERRACOTTA};

    private PerfilBioma(String id, String bioma, Block topo, Block meio, Block base) {
        this.id = id;
        this.bioma = bioma;
        this.topo = topo;
        this.meio = meio;
        this.base = base;
    }

    private PerfilBioma planta(Block b, double chance) {
        plantas.add(b);
        chances.add(chance);
        return this;
    }

    private PerfilBioma arvores(double dens, String... ids) {
        this.densidade = dens;
        this.arvores = ids;
        return this;
    }

    private PerfilBioma alt(Block b, double chance) {
        this.topoAlt = b;
        this.chanceAlt = chance;
        return this;
    }

    private PerfilBioma congela() {
        this.congela = true;
        return this;
    }

    private PerfilBioma mesa() {
        this.mesa = true;
        return this;
    }

    /** d = camada (0 = topo). y = altura (usada nas faixas da mesa). */
    public Block chao(int d, int y, RandomSource rng) {
        if (mesa) return d == 0 ? Blocks.RED_SAND : BANDAS[Math.floorMod(y / 2, BANDAS.length)];
        if (d == 0) return topoAlt != null && rng.nextDouble() < chanceAlt ? topoAlt : topo;
        return d < 3 ? meio : base;
    }

    /** Planta/decoração para pôr em cima do chão (ou null). */
    public Block decor(RandomSource rng) {
        double q = rng.nextDouble(), acum = 0;
        for (int i = 0; i < plantas.size(); i++) {
            acum += chances.get(i);
            if (q < acum) return plantas.get(i);
        }
        return null;
    }

    public static final List<PerfilBioma> LISTA = List.of(
            new PerfilBioma("deserto", "desert", Blocks.SAND, Blocks.SAND, Blocks.SANDSTONE)
                    .planta(Blocks.DEAD_BUSH, 0.04).planta(Blocks.CACTUS, 0.012),
            new PerfilBioma("floresta", "forest", Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.DIRT)
                    .planta(Blocks.GRASS, 0.25).planta(Blocks.DANDELION, 0.02).planta(Blocks.POPPY, 0.02)
                    .planta(Blocks.LILY_OF_THE_VALLEY, 0.01)
                    .arvores(0.02, "oak", "birch", "oak"),
            new PerfilBioma("taiga", "taiga", Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.DIRT).alt(Blocks.PODZOL, 0.25)
                    .planta(Blocks.FERN, 0.10).planta(Blocks.GRASS, 0.10)
                    .arvores(0.025, "spruce", "pine"),
            new PerfilBioma("neve", "snowy_plains", Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.DIRT)
                    .planta(Blocks.SNOW, 1.0).arvores(0.006, "spruce").congela(),
            new PerfilBioma("selva", "jungle", Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.DIRT)
                    .planta(Blocks.GRASS, 0.20).planta(Blocks.FERN, 0.15)
                    .arvores(0.045, "jungle_tree"),
            new PerfilBioma("savana", "savanna", Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.DIRT)
                    .planta(Blocks.GRASS, 0.20).arvores(0.006, "acacia"),
            new PerfilBioma("mesa", "badlands", Blocks.RED_SAND, Blocks.TERRACOTTA, Blocks.TERRACOTTA)
                    .planta(Blocks.DEAD_BUSH, 0.03).mesa(),
            new PerfilBioma("sakura", "cherry_grove", Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.DIRT)
                    .planta(Blocks.PINK_PETALS, 0.10).planta(Blocks.GRASS, 0.12)
                    .arvores(0.03, "cherry"),
            new PerfilBioma("pantano", "swamp", Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.DIRT)
                    .planta(Blocks.GRASS, 0.12).planta(Blocks.BLUE_ORCHID, 0.02)
                    .arvores(0.015, "swamp_oak"),
            new PerfilBioma("cogumelos", "mushroom_fields", Blocks.MYCELIUM, Blocks.DIRT, Blocks.DIRT)
                    .planta(Blocks.RED_MUSHROOM, 0.03).planta(Blocks.BROWN_MUSHROOM, 0.03),
            new PerfilBioma("gelo", "ice_spikes", Blocks.SNOW_BLOCK, Blocks.SNOW_BLOCK, Blocks.PACKED_ICE).congela(),
            new PerfilBioma("sombria", "dark_forest", Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.DIRT)
                    .planta(Blocks.GRASS, 0.10).planta(Blocks.RED_MUSHROOM, 0.01).planta(Blocks.BROWN_MUSHROOM, 0.01)
                    .arvores(0.04, "dark_oak"),
            new PerfilBioma("planicie", "plains", Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.DIRT)
                    .planta(Blocks.GRASS, 0.30).planta(Blocks.DANDELION, 0.03).planta(Blocks.OXEYE_DAISY, 0.03)
                    .planta(Blocks.CORNFLOWER, 0.01)
    );

    public static PerfilBioma achar(String id) {
        for (PerfilBioma p : LISTA) if (p.id.equals(id)) return p;
        return null;
    }
}
