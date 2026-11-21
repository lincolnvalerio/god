package com.onikoox.deus.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class Teclas {
    public static final String CATEGORIA = "key.categories.deus";

    /** Alternativa ao botão do meio do mouse (sem tecla por padrão; escolha em Controles). */
    public static final KeyMapping ABRIR = new KeyMapping("key.deus.abrir", InputConstants.UNKNOWN.getValue(), CATEGORIA);

    /** Volta ao corpo (sai da observação / do modo vagar). */
    public static final KeyMapping VOLTAR = new KeyMapping("key.deus.voltar", GLFW.GLFW_KEY_BACKSPACE, CATEGORIA);

    /** Alternar entre os jogadores em observação (só valem enquanto observa). */
    public static final KeyMapping OBS_ANT = new KeyMapping("key.deus.obs_ant", GLFW.GLFW_KEY_COMMA, CATEGORIA);
    public static final KeyMapping OBS_PROX = new KeyMapping("key.deus.obs_prox", GLFW.GLFW_KEY_PERIOD, CATEGORIA);

    // Teclas de poder (telecinese e outros usam estas na etapa 7). F coincide com "trocar de mão" do jogo base.
    public static final KeyMapping Z = new KeyMapping("key.deus.z", GLFW.GLFW_KEY_Z, CATEGORIA);
    public static final KeyMapping X = new KeyMapping("key.deus.x", GLFW.GLFW_KEY_X, CATEGORIA);
    public static final KeyMapping C = new KeyMapping("key.deus.c", GLFW.GLFW_KEY_C, CATEGORIA);
    public static final KeyMapping F = new KeyMapping("key.deus.f", GLFW.GLFW_KEY_F, CATEGORIA);
    public static final KeyMapping V = new KeyMapping("key.deus.v", GLFW.GLFW_KEY_V, CATEGORIA);
    public static final KeyMapping B = new KeyMapping("key.deus.b", GLFW.GLFW_KEY_B, CATEGORIA);

    private Teclas() {}
}
