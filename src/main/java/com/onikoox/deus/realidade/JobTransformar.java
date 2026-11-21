package com.onikoox.deus.realidade;

import com.onikoox.deus.DeusConfig;
import com.onikoox.deus.DeusSons;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.GrowingPlantBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Onda que se expande a partir do alvo reescrevendo blocos (fase 0) e depois biomas (fase 1).
 * No modo Domínio, ao fim da onda uma cúpula sobe do chão até fechar no topo (fase 2).
 */
final class JobTransformar implements Motor.Job {
    private final ServerLevel lv;
    final Operacao op;
    private final Editor ed;
    private final UUID dono;
    private final Tema tema;
    private final PerfilBioma perfil;
    private final boolean cupula;
    private final boolean esfera;
    private final int cx, cy, cz, r;
    private final long semente;
    private final RandomSource rng;
    private final Registry<Biome> regBioma;
    private final Registry<ConfiguredFeature<?, ?>> regFeature;
    private final BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
    private final BlockPos.MutableBlockPos mp2 = new BlockPos.MutableBlockPos();

    private final long[] ordem;
    private int cursor = 0;
    private int fase = 0;
    private int frente = 0;
    private int tickRaio = 0;

    private final LongArrayList celulas = new LongArrayList();
    private int bCursor = 0;
    private final Set<ChunkAccess> sujos = new HashSet<>();

    private long[] casca;
    private int cCursor = 0;
    private int frenteY = -128;

    JobTransformar(ServerLevel lv, Operacao op, UUID dono, PerfilBioma perfil, boolean cupula) {
        this.lv = lv;
        this.op = op;
        this.ed = new Editor(lv, op);
        this.dono = dono;
        this.tema = op.tema;
        this.perfil = perfil;
        this.cupula = cupula;
        this.esfera = tema == Tema.VAZIO;
        this.cx = op.centro.getX();
        this.cy = op.centro.getY();
        this.cz = op.centro.getZ();
        this.r = op.raio;
        this.semente = op.id.getMostSignificantBits();
        this.rng = RandomSource.create(semente);
        this.regBioma = lv.registryAccess().registryOrThrow(Registries.BIOME);
        this.regFeature = lv.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        this.ordem = montarOrdem(r, esfera);
    }

    @Override public UUID dono() { return dono; }

    // ------------------------------------------------------------ geometria
    private static long cod(int d, int dx, int dy, int dz) {
        return ((long) d << 32) | ((long) (dx & 0xFF) << 16) | ((long) (dy & 0xFF) << 8) | (long) (dz & 0xFF);
    }

    /** Offsets ordenados por distância ao centro (a onda). Cada offset cabe num byte (raio <= 127). */
    private static long[] montarOrdem(int r, boolean esfera) {
        LongArrayList l = new LongArrayList();
        int r2 = r * r;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (esfera) {
                    for (int dy = -r; dy <= r; dy++) {
                        int d = dx * dx + dy * dy + dz * dz;
                        if (d <= r2) l.add(cod(d, dx, dy, dz));
                    }
                } else {
                    int d = dx * dx + dz * dz;
                    if (d <= r2) l.add(cod(d, dx, 0, dz));
                }
            }
        }
        long[] a = l.toLongArray();
        Arrays.sort(a);
        return a;
    }

    /** Casca da esfera (espessura ~1,6), ordenada de baixo para cima: a cúpula "sobe". */
    private long[] montarCasca() {
        LongArrayList l = new LongArrayList();
        double ri = r - 1.6, ri2 = ri * ri;
        int r2 = r * r;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int rho2 = dx * dx + dz * dz;
                if (rho2 > r2) continue;
                int dyMax = (int) Math.floor(Math.sqrt(r2 - rho2));
                int dyMin = ri2 > rho2 ? (int) Math.ceil(Math.sqrt(ri2 - rho2)) : 0;
                for (int dy = dyMin; dy <= dyMax; dy++) {
                    l.add(cod(dy + 128, dx, dy, dz));
                    if (dy != 0) l.add(cod(-dy + 128, dx, -dy, dz));
                }
            }
        }
        long[] a = l.toLongArray();
        Arrays.sort(a);
        return a;
    }

    // ---------------------------------------------------------------- passo
    @Override
    public boolean passo(long limite) {
        long ini = System.nanoTime();
        if (fase == 0) {
            int max = esfera ? DeusConfig.REAL_BLOCOS_POR_TICK.get() : DeusConfig.REAL_COLUNAS_POR_TICK.get();
            int intervalo = esfera ? 256 : 8;
            int n = 0;
            while (cursor < ordem.length && n < max) {
                long v = ordem[cursor++];
                n++;
                int dx = (byte) (v >> 16), dy = (byte) (v >> 8), dz = (byte) v;
                frente = (int) (v >>> 32);
                if (esfera) bloco(cx + dx, cy + dy, cz + dz);
                else coluna(cx + dx, cz + dz);
                if ((n % intervalo) == 0 && System.nanoTime() - ini > limite) break;
            }
            anel(Math.sqrt(frente));
            return cursor >= ordem.length && depoisDaOnda();
        }
        if (fase == 1) {
            int n = 0;
            while (bCursor < celulas.size()) {
                BlockPos q = BlockPos.of(celulas.getLong(bCursor++));
                if (esfera) {
                    bioma(q.getX(), q.getY(), q.getZ());
                } else {
                    int qMin = lv.getMinBuildHeight() >> 2, qMax = (lv.getMaxBuildHeight() - 1) >> 2;
                    for (int qy = qMin; qy <= qMax; qy++) bioma(q.getX(), qy, q.getZ());
                }
                if ((++n % 4) == 0 && System.nanoTime() - ini > limite) return false;
            }
            ed.reenviar(sujos);
            if (!cupula) return true;
            iniciarCupula();
            return false;
        }
        // fase 2: a cúpula sobe
        int max = DeusConfig.REAL_BLOCOS_POR_TICK.get(), n = 0;
        BlockState bloco = estadoCupula();
        while (cCursor < casca.length && n < max) {
            long v = casca[cCursor++];
            n++;
            int dx = (byte) (v >> 16), dy = (byte) (v >> 8), dz = (byte) v;
            int y = cy + dy;
            frenteY = dy;
            if (y < lv.getMinBuildHeight() || y >= lv.getMaxBuildHeight()) continue;
            mp.set(cx + dx, y, cz + dz);
            if (!lv.hasChunkAt(mp)) continue;
            ed.trocar(mp, bloco);
            if ((n % 256) == 0 && System.nanoTime() - ini > limite) break;
        }
        anelCupula(frenteY);
        if (cCursor >= casca.length) {
            lv.playSound(null, op.centro, DeusSons.ONDA_CHOQUE.get(), SoundSource.PLAYERS, 4.0F, 0.8F);
            return true;
        }
        return false;
    }

    /** @return true se o trabalho terminou aqui (Limpar sem cúpula). */
    private boolean depoisDaOnda() {
        if (tema == Tema.LIMPAR) {            // limpar não muda bioma
            if (cupula) {
                iniciarCupula();
                return false;
            }
            return true;
        }
        prepararBiomas();
        fase = 1;
        return false;
    }

    private void iniciarCupula() {
        casca = montarCasca();
        cCursor = 0;
        fase = 2;
        lv.playSound(null, op.centro, DeusSons.ENERGIA_PULSO.get(), SoundSource.PLAYERS, 4.0F, 0.7F);
    }

    private BlockState estadoCupula() {
        Block b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(DeusConfig.CUPULA_BLOCO.get()));
        if (b == null || b == Blocks.AIR) b = Blocks.BEDROCK;
        return b.defaultBlockState();
    }

    // ------------------------------------------------------------------ Vazio
    private void bloco(int x, int y, int z) {
        if (y < lv.getMinBuildHeight() || y >= lv.getMaxBuildHeight()) return;
        mp.set(x, y, z);
        if (!lv.hasChunkAt(mp)) return;
        if (lv.getBlockState(mp).isAir()) return;
        ed.trocar(mp, Blocks.AIR.defaultBlockState());
    }

    // ------------------------------------------------ colunas (todos os outros temas)
    private void coluna(int x, int z) {
        mp.set(x, 0, z);
        if (!lv.hasChunkAt(mp)) return;
        int topo = lv.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        int min = lv.getMinBuildHeight();
        if (tema == Tema.LIMPAR) {
            limpar(x, z, topo, min);
            return;
        }
        boolean folha = false;
        int solo = Integer.MIN_VALUE;
        for (int y = topo; y >= min; y--) {
            mp.set(x, y, z);
            BlockState s = lv.getBlockState(mp);
            if (s.isAir()) continue;
            if (s.is(BlockTags.LEAVES)) folha = true;
            if (s.is(BlockTags.LOGS) && !folha) { solo = y; break; } // tronco sem folhas: é construção, não mexe
            if (Natureza.vegetacao(s)) {
                BlockState n = vegetacao(s, x, z);
                if (n != null) ed.trocar(mp, n);
                continue;
            }
            if (s.getBlock() instanceof LiquidBlock) {
                agua(s, x, y, z);
                continue;
            }
            solo = y;
            break;
        }
        if (solo != Integer.MIN_VALUE) chao(x, solo, z);
    }

    /** Limpar: remove tudo acima do chão natural (e acima da água). Fica o terreno. */
    private void limpar(int x, int z, int topo, int min) {
        for (int y = topo; y >= min; y--) {
            mp.set(x, y, z);
            BlockState s = lv.getBlockState(mp);
            if (s.isAir()) continue;
            if (Natureza.terreno(s) || s.getBlock() instanceof LiquidBlock) break;
            ed.trocar(mp, Blocks.AIR.defaultBlockState());
        }
    }

    private void agua(BlockState s, int x, int y, int z) {
        if (!s.is(Blocks.WATER)) return;
        if (tema == Tema.NETHER) {
            ed.trocar(mp, Blocks.LAVA.defaultBlockState());
        } else if (tema == Tema.BIOMA && perfil != null && perfil.congela) {
            mp2.set(x, y + 1, z);
            if (lv.getBlockState(mp2).isAir()) ed.trocar(mp, Blocks.ICE.defaultBlockState());
        }
    }

    private void chao(int x, int solo, int z) {
        for (int d = 0; d < 4; d++) {
            mp.set(x, solo - d, z);
            BlockState s = lv.getBlockState(mp);
            if (!Natureza.terreno(s)) break;
            BlockState n = chaoNovo(d, x, solo - d, z, s);
            if (n != null) ed.trocar(mp, n);
            if (d == 0) decorar(x, solo + 1, z, n != null ? n : s);
        }
    }

    private BlockState vegetacao(BlockState s, int x, int z) {
        boolean folha = s.is(BlockTags.LEAVES), tronco = s.is(BlockTags.LOGS);
        if (tema == Tema.ABISMO || tema == Tema.BIOMA) return Blocks.AIR.defaultBlockState();
        if (tema == Tema.CORRUPCAO) {
            // deltas de basalto: árvores viram colunas de basalto, o resto some
            if (tronco) return pilar(Blocks.BASALT, s);
            return Blocks.AIR.defaultBlockState();
        }
        if (tema == Tema.NETHER) {
            int zn = zona(x, z);
            if (folha) {
                return zn == 1 ? Blocks.NETHER_WART_BLOCK.defaultBlockState()
                        : zn == 2 ? Blocks.WARPED_WART_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState();
            }
            if (tronco) {
                Block novo = zn == 1 ? Blocks.CRIMSON_STEM : zn == 2 ? Blocks.WARPED_STEM
                        : zn == 3 ? Blocks.BONE_BLOCK : Blocks.BASALT;
                return pilar(novo, s);
            }
            return Blocks.AIR.defaultBlockState();
        }
        return null;
    }

    private static BlockState pilar(Block novo, BlockState orig) {
        BlockState n = novo.defaultBlockState();
        if (orig.hasProperty(BlockStateProperties.AXIS) && n.hasProperty(BlockStateProperties.AXIS)) {
            n = n.setValue(BlockStateProperties.AXIS, orig.getValue(BlockStateProperties.AXIS));
        }
        return n;
    }

    private BlockState chaoNovo(int d, int x, int y, int z, BlockState atual) {
        double q = rng.nextDouble();
        if (tema == Tema.ABISMO) {
            if (d == 0) return (q < 0.62 ? Blocks.SCULK : Blocks.DEEPSLATE).defaultBlockState();
            if (d == 1) return (q < 0.12 ? Blocks.SCULK : Blocks.DEEPSLATE).defaultBlockState();
            return Blocks.DEEPSLATE.defaultBlockState();
        }
        if (tema == Tema.CORRUPCAO) return deltas(d, q);
        if (tema == Tema.BIOMA) return perfil.chao(d, y, rng).defaultBlockState();
        if (tema == Tema.NETHER) {
            Block b;
            switch (zona(x, z)) {
                case 1 -> b = d == 0 ? Blocks.CRIMSON_NYLIUM : Blocks.NETHERRACK;
                case 2 -> b = d == 0 ? Blocks.WARPED_NYLIUM : Blocks.NETHERRACK;
                case 3 -> b = d == 0 ? (q < 0.6 ? Blocks.SOUL_SAND : Blocks.SOUL_SOIL) : Blocks.SOUL_SOIL;
                case 4 -> { return deltas(d, q); }
                default -> b = d == 0 && q < 0.04 ? Blocks.MAGMA_BLOCK : Blocks.NETHERRACK;
            }
            return b.defaultBlockState();
        }
        return null;
    }

    /** Chão dos deltas de basalto: basalto, blackstone e magma. */
    private static BlockState deltas(int d, double q) {
        Block b = d == 0 ? (q < 0.5 ? Blocks.BASALT : q < 0.82 ? Blocks.BLACKSTONE : q < 0.93 ? Blocks.MAGMA_BLOCK : Blocks.SMOOTH_BASALT)
                : (q < 0.5 ? Blocks.BASALT : Blocks.BLACKSTONE);
        return b.defaultBlockState();
    }

    private void decorar(int x, int y, int z, BlockState chao) {
        mp.set(x, y, z);
        if (!lv.getBlockState(mp).isAir()) return;
        double q = rng.nextDouble();
        Block d = null;
        if (tema == Tema.ABISMO) {
            if (chao.is(Blocks.SCULK) && q < 0.012) d = Blocks.SCULK_SENSOR;
        } else if (tema == Tema.NETHER) {
            int zn = zona(x, z);
            if (zn == 1 && chao.is(Blocks.CRIMSON_NYLIUM) && q < 0.3) d = q < 0.15 ? Blocks.CRIMSON_ROOTS : Blocks.CRIMSON_FUNGUS;
            if (zn == 2 && chao.is(Blocks.WARPED_NYLIUM) && q < 0.3)
                d = q < 0.12 ? Blocks.WARPED_ROOTS : q < 0.2 ? Blocks.WARPED_FUNGUS : Blocks.NETHER_SPROUTS;
        } else if (tema == Tema.BIOMA && perfil != null) {
            if (perfil.arvores.length > 0 && rng.nextDouble() < perfil.densidade) {
                arvore(x, y, z);
                return;
            }
            d = perfil.decor(rng);
            if (d == Blocks.CACTUS && !vizinhosLivres(x, y, z)) d = null;
        }
        if (d != null) ed.trocar(mp, d.defaultBlockState());
    }

    private boolean vizinhosLivres(int x, int y, int z) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            mp2.set(x + dir.getStepX(), y, z + dir.getStepZ());
            if (!lv.getBlockState(mp2).isAir()) return false;
        }
        return true;
    }

    /**
     * Planta uma árvore do jogo. As árvores escrevem blocos por conta própria, então fotografamos a área antes
     * e registramos o que mudou no histórico (assim "Restaurar" também desfaz as árvores).
     */
    private void arvore(int x, int y, int z) {
        final int R = 6, ALT = 24;
        if (!lv.hasChunksAt(x - R, y, z - R, x + R, y, z + R)) return;
        String id = perfil.arvores[rng.nextInt(perfil.arvores.length)];
        Optional<Holder.Reference<ConfiguredFeature<?, ?>>> h =
                regFeature.getHolder(ResourceKey.create(Registries.CONFIGURED_FEATURE, new ResourceLocation("minecraft", id)));
        if (h.isEmpty()) return;

        int larg = 2 * R + 1;
        BlockState[] antes = new BlockState[larg * larg * ALT];
        int i = 0;
        for (int dx = -R; dx <= R; dx++)
            for (int dz = -R; dz <= R; dz++)
                for (int dy = -1; dy < ALT - 1; dy++) {
                    mp2.set(x + dx, y + dy, z + dz);
                    antes[i++] = lv.getBlockState(mp2);
                }
        boolean ok;
        try {
            ok = h.get().value().place(lv, lv.getChunkSource().getGenerator(), rng, new BlockPos(x, y, z));
        } catch (RuntimeException e) {
            ok = false;
        }
        if (!ok) return;
        i = 0;
        for (int dx = -R; dx <= R; dx++)
            for (int dz = -R; dz <= R; dz++)
                for (int dy = -1; dy < ALT - 1; dy++) {
                    mp2.set(x + dx, y + dy, z + dz);
                    BlockState antes1 = antes[i++];
                    if (lv.getBlockState(mp2) != antes1) op.blocoOriginal(mp2.immutable(), antes1, null);
                }
    }

    /** 0 desolado, 1 carmesim, 2 distorcido, 3 vale de almas, 4 deltas de basalto (manchas irregulares). */
    private int zona(int x, int z) {
        double nx = x + 14 * Math.sin(z * 0.045 + (semente % 7)), nz = z + 14 * Math.sin(x * 0.05 + (semente % 5));
        long gx = Math.floorDiv((int) nx, 28), gz = Math.floorDiv((int) nz, 28);
        long hh = (gx * 73856093L) ^ (gz * 19349663L) ^ semente;
        hh ^= hh >>> 13;
        hh *= 0x5DEECE66DL;
        return (int) Math.floorMod(hh >>> 20, 5L);
    }

    // ------------------------------------------------------------------ Biomas
    private void prepararBiomas() {
        double r2 = (double) r * r;
        if (esfera) {
            for (int qx = (cx - r) >> 2; qx <= (cx + r) >> 2; qx++)
                for (int qy = (cy - r) >> 2; qy <= (cy + r) >> 2; qy++)
                    for (int qz = (cz - r) >> 2; qz <= (cz + r) >> 2; qz++) {
                        double dx = (qx << 2) + 2 - cx, dy = (qy << 2) + 2 - cy, dz = (qz << 2) + 2 - cz;
                        if (dx * dx + dy * dy + dz * dz <= r2) celulas.add(BlockPos.asLong(qx, qy, qz));
                    }
        } else {
            for (int qx = (cx - r) >> 2; qx <= (cx + r) >> 2; qx++)
                for (int qz = (cz - r) >> 2; qz <= (cz + r) >> 2; qz++) {
                    double dx = (qx << 2) + 2 - cx, dz = (qz << 2) + 2 - cz;
                    if (dx * dx + dz * dz <= r2) celulas.add(BlockPos.asLong(qx, 0, qz));
                }
        }
    }

    @SuppressWarnings("unchecked")
    private void bioma(int qx, int qy, int qz) {
        String id = switch (tema) {
            case VAZIO -> "the_void";
            case ABISMO -> "deep_dark";
            case CORRUPCAO -> "basalt_deltas";
            case BIOMA -> perfil.bioma;
            case LIMPAR -> "plains";
            case NETHER -> switch (zona((qx << 2) + 2, (qz << 2) + 2)) {
                case 1 -> "crimson_forest";
                case 2 -> "warped_forest";
                case 3 -> "soul_sand_valley";
                case 4 -> "basalt_deltas";
                default -> "nether_wastes";
            };
        };
        regBioma.getHolder(ResourceKey.create(Registries.BIOME, new ResourceLocation(id)))
                .ifPresent(h -> ed.bioma(qx, qy, qz, (Holder<Biome>) h, true, false, sujos));
    }

    // ---------------------------------------------------------------- Efeitos
    private ParticleOptions particula() {
        return switch (tema) {
            case VAZIO -> ParticleTypes.PORTAL;
            case ABISMO -> ParticleTypes.SCULK_SOUL;
            case NETHER -> ParticleTypes.LAVA;
            case CORRUPCAO -> ParticleTypes.WHITE_ASH;
            case LIMPAR -> ParticleTypes.CLOUD;
            case BIOMA -> ParticleTypes.HAPPY_VILLAGER;
        };
    }

    private void raioVisual(double x, double y, double z) {
        LightningBolt b = EntityType.LIGHTNING_BOLT.create(lv);
        if (b == null) return;
        b.moveTo(x, y, z);
        b.setVisualOnly(true); // só o clarão: não queima nem fere
        lv.addFreshEntity(b);
    }

    /** Anel de partículas na frente da onda e, de vez em quando, um raio visual (como no Hand of God). */
    private void anel(double d) {
        if (d < 1) return;
        ParticleOptions pt = particula();
        tickRaio++;
        for (int i = 0; i < 16; i++) {
            double a = Math.PI * 2 * i / 16;
            int bx = (int) Math.floor(cx + 0.5 + Math.cos(a) * d), bz = (int) Math.floor(cz + 0.5 + Math.sin(a) * d);
            double y = cy + 1;
            if (!esfera) {
                mp.set(bx, 0, bz);
                if (!lv.hasChunkAt(mp)) continue;
                y = lv.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz) + 0.5;
            }
            lv.sendParticles(pt, bx + 0.5, y, bz + 0.5, 3, 0.6, 1.0, 0.6, 0.02);
            if (tickRaio % 14 == 0 && i == (tickRaio / 14) % 16) raioVisual(bx + 0.5, y, bz + 0.5);
        }
    }

    /** Anel na altura em que a cúpula está subindo agora. */
    private void anelCupula(int dy) {
        if (dy == -128) return;
        double rho = Math.sqrt(Math.max(0, (double) r * r - (double) dy * dy));
        for (int i = 0; i < 24; i++) {
            double a = Math.PI * 2 * i / 24;
            lv.sendParticles(ParticleTypes.END_ROD, cx + 0.5 + Math.cos(a) * rho, cy + dy + 0.5, cz + 0.5 + Math.sin(a) * rho,
                    2, 0.3, 0.3, 0.3, 0.01);
        }
    }
}
