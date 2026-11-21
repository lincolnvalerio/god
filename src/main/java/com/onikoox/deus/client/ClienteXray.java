package com.onikoox.deus.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.onikoox.deus.DeusMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Visão divina (só desenha se o servidor ligou o poder para você):
 *  - mineral: contorno dos minérios ao redor, através das paredes;
 *  - entidades: contorno de jogadores, hostis e animais, através das paredes.
 */
@Mod.EventBusSubscriber(modid = DeusMod.ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClienteXray {
    private record Alvo(AABB caixa, float r, float g, float b) {}

    private static final int RAIO_MINERIO = 20, MAX_MINERIOS = 2500;
    private static final double RAIO_ENTIDADES = 128;
    private static final List<Alvo> MINERIOS = new ArrayList<>();
    private static int contador = 0;

    private ClienteXray() {}

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !ClienteEstado.xrayMin) {
            MINERIOS.clear();
            return;
        }
        if (++contador % 20 != 0) return;
        escanear(mc);
    }

    private static void escanear(Minecraft mc) {
        BlockPos c = mc.player.blockPosition();
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        List<Alvo> nova = new ArrayList<>();
        int ymin = Math.max(mc.level.getMinBuildHeight(), c.getY() - RAIO_MINERIO);
        int ymax = Math.min(mc.level.getMaxBuildHeight() - 1, c.getY() + RAIO_MINERIO);
        busca:
        for (int x = c.getX() - RAIO_MINERIO; x <= c.getX() + RAIO_MINERIO; x++) {
            for (int z = c.getZ() - RAIO_MINERIO; z <= c.getZ() + RAIO_MINERIO; z++) {
                for (int y = ymin; y <= ymax; y++) {
                    mp.set(x, y, z);
                    BlockState s = mc.level.getBlockState(mp);
                    if (s.isAir()) continue;
                    float[] cor = corMinerio(s);
                    if (cor == null) continue;
                    nova.add(new Alvo(new AABB(x - 0.002, y - 0.002, z - 0.002, x + 1.002, y + 1.002, z + 1.002), cor[0], cor[1], cor[2]));
                    if (nova.size() >= MAX_MINERIOS) break busca;
                }
            }
        }
        MINERIOS.clear();
        MINERIOS.addAll(nova);
    }

    private static float[] corMinerio(BlockState s) {
        if (!s.is(Tags.Blocks.ORES) && !s.is(Blocks.ANCIENT_DEBRIS)) return null;
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(s.getBlock());
        String n = id == null ? "" : id.getPath();
        if (n.contains("diamond")) return new float[]{0.2F, 1F, 1F};
        if (n.contains("emerald")) return new float[]{0.2F, 1F, 0.3F};
        if (n.contains("gold")) return new float[]{1F, 0.85F, 0.1F};
        if (n.contains("iron")) return new float[]{0.9F, 0.7F, 0.55F};
        if (n.contains("copper")) return new float[]{1F, 0.5F, 0.2F};
        if (n.contains("redstone")) return new float[]{1F, 0.1F, 0.1F};
        if (n.contains("lapis")) return new float[]{0.2F, 0.3F, 1F};
        if (n.contains("coal")) return new float[]{0.6F, 0.6F, 0.6F};
        if (n.contains("quartz")) return new float[]{1F, 1F, 1F};
        if (n.contains("debris")) return new float[]{0.6F, 0.35F, 0.2F};
        return new float[]{1F, 0.2F, 1F};
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (!ClienteEstado.xrayMin && !ClienteEstado.xrayEnt) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Vec3 cam = e.getCamera().getPosition();
        Matrix4f m = e.getPoseStack().last().pose();

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.lineWidth(2.0F);
        Tesselator t = Tesselator.getInstance();
        BufferBuilder b = t.getBuilder();
        b.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        if (ClienteEstado.xrayMin) {
            for (Alvo a : MINERIOS) caixa(b, m, cam, a.caixa(), a.r(), a.g(), a.b());
        }
        if (ClienteEstado.xrayEnt) {
            for (Entity en : mc.level.entitiesForRendering()) {
                if (en == mc.player || !(en instanceof LivingEntity)) continue;
                if (en.distanceToSqr(mc.player) > RAIO_ENTIDADES * RAIO_ENTIDADES) continue;
                float r, g, bl;
                if (en instanceof Player) { r = 0.3F; g = 0.6F; bl = 1F; }
                else if (en instanceof Enemy) { r = 1F; g = 0.2F; bl = 0.2F; }
                else { r = 0.3F; g = 1F; bl = 0.4F; }
                caixa(b, m, cam, en.getBoundingBox(), r, g, bl);
            }
        }

        t.end();
        RenderSystem.lineWidth(1.0F);
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }

    private static void linha(BufferBuilder b, Matrix4f m, Vec3 cam, double x1, double y1, double z1,
                              double x2, double y2, double z2, float r, float g, float bl) {
        b.vertex(m, (float) (x1 - cam.x), (float) (y1 - cam.y), (float) (z1 - cam.z)).color(r, g, bl, 1F).endVertex();
        b.vertex(m, (float) (x2 - cam.x), (float) (y2 - cam.y), (float) (z2 - cam.z)).color(r, g, bl, 1F).endVertex();
    }

    private static void caixa(BufferBuilder b, Matrix4f m, Vec3 cam, AABB a, float r, float g, float bl) {
        double x1 = a.minX, y1 = a.minY, z1 = a.minZ, x2 = a.maxX, y2 = a.maxY, z2 = a.maxZ;
        // base
        linha(b, m, cam, x1, y1, z1, x2, y1, z1, r, g, bl);
        linha(b, m, cam, x2, y1, z1, x2, y1, z2, r, g, bl);
        linha(b, m, cam, x2, y1, z2, x1, y1, z2, r, g, bl);
        linha(b, m, cam, x1, y1, z2, x1, y1, z1, r, g, bl);
        // topo
        linha(b, m, cam, x1, y2, z1, x2, y2, z1, r, g, bl);
        linha(b, m, cam, x2, y2, z1, x2, y2, z2, r, g, bl);
        linha(b, m, cam, x2, y2, z2, x1, y2, z2, r, g, bl);
        linha(b, m, cam, x1, y2, z2, x1, y2, z1, r, g, bl);
        // colunas
        linha(b, m, cam, x1, y1, z1, x1, y2, z1, r, g, bl);
        linha(b, m, cam, x2, y1, z1, x2, y2, z1, r, g, bl);
        linha(b, m, cam, x2, y1, z2, x2, y2, z2, r, g, bl);
        linha(b, m, cam, x1, y1, z2, x1, y2, z2, r, g, bl);
    }
}
