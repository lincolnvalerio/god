package com.onikoox.deus.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.onikoox.deus.entity.CorpoEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/** Desenha o corpo do Deus com a skin dele, sentado no trono (ou em pé). */
public class CorpoRenderer extends LivingEntityRenderer<CorpoEntity, TronoModel<CorpoEntity>> {
    private final TronoModel<CorpoEntity> normal;
    private final TronoModel<CorpoEntity> magro;

    public CorpoRenderer(EntityRendererProvider.Context c) {
        super(c, new TronoModel<>(c.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.normal = this.model;
        this.magro = new TronoModel<>(c.bakeLayer(ModelLayers.PLAYER_SLIM), true);
    }

    private static PlayerInfo info(CorpoEntity e) {
        UUID id = e.dono();
        Minecraft mc = Minecraft.getInstance();
        if (id == null || mc.getConnection() == null) return null;
        return mc.getConnection().getPlayerInfo(id);
    }

    @Override
    public ResourceLocation getTextureLocation(CorpoEntity e) {
        PlayerInfo pi = info(e);
        if (pi != null) return pi.getSkinLocation();
        UUID id = e.dono();
        return DefaultPlayerSkin.getDefaultSkin(id != null ? id : e.getUUID());
    }

    @Override
    public void render(CorpoEntity e, float yaw, float parcial, PoseStack ps, MultiBufferSource buf, int luz) {
        PlayerInfo pi = info(e);
        this.model = pi != null && "slim".equals(pi.getModelName()) ? magro : normal;
        this.model.sentado = e.sentado() ? 1F : 0F;
        this.model.gesto = 0F;
        super.render(e, yaw, parcial, ps, buf, luz);
    }
}
