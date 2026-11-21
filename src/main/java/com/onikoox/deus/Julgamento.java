package com.onikoox.deus;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class Julgamento {
    private static final Random RNG = new Random();

    private Julgamento() {}

    private static final List<ServerPlayer> FILA = new ArrayList<>();

    /** Agenda o envio ao Inferno para o fim do tick (trocar de dimensão no meio do tick do inventário é perigoso). */
    public static void condenar(ServerPlayer p) {
        if (!FILA.contains(p)) FILA.add(p);
    }

    @SubscribeEvent
    public static void aoTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || FILA.isEmpty()) return;
        List<ServerPlayer> copia = new ArrayList<>(FILA);
        FILA.clear();
        for (ServerPlayer p : copia) {
            if (!p.isRemoved()) enviar(p);
        }
    }

    /** Manda o jogador para o Inferno na hora, com um clarão sem dano. */
    private static void enviar(ServerPlayer p) {
        com.onikoox.deus.entity.AssentoEntity.soltar(p);
        MinecraftServer srv = p.server;
        ServerLevel alvo = null;
        try {
            ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(DeusConfig.DIM_INFERNO.get()));
            alvo = srv.getLevel(key);
        } catch (Exception ignored) {}
        if (alvo == null) {
            System.err.println("[Deus] Dimensão do Inferno não encontrada; usando o Nether.");
            alvo = srv.getLevel(Level.NETHER);
        }
        if (alvo == null) return;

        raioVisual(p.serverLevel(), p.getX(), p.getY(), p.getZ());
        BlockPos pouso = acharPouso(alvo);
        p.teleportTo(alvo, pouso.getX() + 0.5, pouso.getY(), pouso.getZ() + 0.5, p.getYRot(), 0F);
        p.sendSystemMessage(Component.translatable("msg.deus.condenado").withStyle(ChatFormatting.DARK_RED));

        for (ServerPlayer o : srv.getPlayerList().getPlayers()) {
            if (Admin.isAdmin(o)) {
                o.sendSystemMessage(Component.translatable("msg.deus.alerta_admin", p.getGameProfile().getName())
                        .withStyle(ChatFormatting.GOLD));
            }
        }
    }

    private static void raioVisual(ServerLevel l, double x, double y, double z) {
        LightningBolt b = EntityType.LIGHTNING_BOLT.create(l);
        if (b == null) return;
        b.moveTo(x, y, z);
        b.setVisualOnly(true);
        l.addFreshEntity(b);
    }

    private static BlockPos acharPouso(ServerLevel l) {
        int topo = Math.min(l.dimensionType().logicalHeight(), l.getMaxBuildHeight()) - 10;
        int base = l.getMinBuildHeight() + 4;
        for (int i = 0; i < 60; i++) {
            int x = RNG.nextInt(129) - 64, z = RNG.nextInt(129) - 64;
            for (int y = topo; y > base; y--) {
                BlockPos pe = new BlockPos(x, y, z);
                BlockState chao = l.getBlockState(pe.below());
                if (chao.isFaceSturdy(l, pe.below(), net.minecraft.core.Direction.UP)
                        && l.getBlockState(pe).isAir() && l.getBlockState(pe.above()).isAir()
                        && l.getFluidState(pe.below()).isEmpty()) {
                    return pe;
                }
            }
        }
        // Sem chão achado: plataforma de obsidiana.
        BlockPos c = new BlockPos(0, Math.min(70, topo), 0);
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                l.setBlockAndUpdate(c.offset(dx, -1, dz), Blocks.OBSIDIAN.defaultBlockState());
                l.setBlockAndUpdate(c.offset(dx, 0, dz), Blocks.AIR.defaultBlockState());
                l.setBlockAndUpdate(c.offset(dx, 1, dz), Blocks.AIR.defaultBlockState());
            }
        return c;
    }
}
