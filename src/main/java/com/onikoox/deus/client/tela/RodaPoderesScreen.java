package com.onikoox.deus.client.tela;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.onikoox.deus.poder.Poderes.Categoria;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

/** Roda de categorias (botão do meio do mouse). Clicar numa fatia abre o painel dela. */
public class RodaPoderesScreen extends Screen {
    private static final Categoria[] CATS = Categoria.values();
    private static final float R0 = 52F, R1 = 118F;
    private int hover = -1;

    public RodaPoderesScreen() {
        super(Component.translatable("deus.roda.titulo"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        int cx = width / 2, cy = height / 2;
        double dx = mx - cx, dy = my - cy, dist = Math.sqrt(dx * dx + dy * dy);
        double fatia = Math.PI * 2 / CATS.length;

        hover = -1;
        if (dist >= R0 && dist <= R1 + 12) {
            double ang = Math.atan2(dy, dx) + Math.PI / 2;
            ang = (ang + Math.PI * 2) % (Math.PI * 2);
            hover = (int) (ang / fatia) % CATS.length;
        }

        for (int i = 0; i < CATS.length; i++) {
            double a0 = -Math.PI / 2 + i * fatia + 0.02, a1 = -Math.PI / 2 + (i + 1) * fatia - 0.02;
            boolean h = i == hover;
            desenharFatia(g, cx, cy, h ? R0 : R0, h ? R1 + 10 : R1, a0, a1, h ? 0xE0D4A017 : 0xB0101020);
            double mid = (a0 + a1) / 2, rm = (R0 + R1) / 2 + (h ? 5 : 0);
            Component nome = Component.translatable("deus.cat." + CATS[i].name().toLowerCase());
            int tw = font.width(nome);
            g.drawString(font, nome, (int) (cx + Math.cos(mid) * rm) - tw / 2, (int) (cy + Math.sin(mid) * rm) - 4,
                    h ? 0xFFFFFFFF : 0xFFE8C860, true);
        }

        g.drawCenteredString(font, title, cx, cy - 8, 0xFFFFD700);
        g.drawCenteredString(font, Component.translatable("deus.roda.dica"), cx, height - 20, 0xFFAAAAAA);
        super.render(g, mx, my, pt);
    }

    private static void desenharFatia(GuiGraphics g, int cx, int cy, float r0, float r1, double a0, double a1, int argb) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator t = Tesselator.getInstance();
        BufferBuilder b = t.getBuilder();
        b.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f m = g.pose().last().pose();
        int A = argb >>> 24 & 255, R = argb >> 16 & 255, G = argb >> 8 & 255, B = argb & 255;
        int passos = 24;
        for (int i = 0; i <= passos; i++) {
            double a = a0 + (a1 - a0) * i / passos;
            float c = (float) Math.cos(a), s = (float) Math.sin(a);
            b.vertex(m, cx + c * r1, cy + s * r1, 0).color(R, G, B, A).endVertex();
            b.vertex(m, cx + c * r0, cy + s * r0, 0).color(R, G, B, A).endVertex();
        }
        t.end();
        RenderSystem.disableBlend();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int botao) {
        if (botao == 2) { // botão do meio de novo fecha
            onClose();
            return true;
        }
        if (botao == 0 && hover >= 0) {
            minecraft.setScreen(new PainelCategoriaScreen(CATS[hover], this));
            return true;
        }
        return super.mouseClicked(mx, my, botao);
    }
}
