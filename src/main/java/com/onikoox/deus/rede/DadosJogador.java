package com.onikoox.deus.rede;

import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

/** Retrato de um jogador enviado ao painel do Deus. itens: 0-35 inventário, 36-39 armadura, 40 mão secundária. */
public record DadosJogador(UUID id, String nome, String dim, double x, double y, double z,
                           float vida, float vidaMax, int fome, int modo, int nivel,
                           boolean temSalvo, List<ItemStack> itens) {}
