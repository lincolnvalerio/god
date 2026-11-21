package com.onikoox.deus;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class DeusConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ADMINS;
    public static final ForgeConfigSpec.BooleanValue SO_ADMIN_SENTA;
    public static final ForgeConfigSpec.ConfigValue<String> DIM_INFERNO;
    public static final ForgeConfigSpec.BooleanValue CHAT_PRIVADO_ATIVO;
    public static final ForgeConfigSpec.ConfigValue<String> AVISO_TEXTO;
    public static final ForgeConfigSpec.IntValue RADAR_JOGADORES;
    public static final ForgeConfigSpec.IntValue RADAR_MOBS;
    public static final ForgeConfigSpec.ConfigValue<String> CUPULA_BLOCO;
    public static final ForgeConfigSpec.IntValue REAL_RAIO_SUPERFICIE;
    public static final ForgeConfigSpec.IntValue REAL_RAIO_ESFERA;
    public static final ForgeConfigSpec.IntValue REAL_MS_POR_TICK;
    public static final ForgeConfigSpec.IntValue REAL_COLUNAS_POR_TICK;
    public static final ForgeConfigSpec.IntValue REAL_BLOCOS_POR_TICK;
    public static final ForgeConfigSpec.IntValue REAL_MAX_OPERACOES;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        ADMINS = b.comment(
                "Quem pode usar o item Deus e os poderes. Coloque seu NICK ou, melhor, seu UUID.",
                "Em servidor offline-mode o nick pode ser falsificado: use o UUID.",
                "Só quem estiver nesta lista pode virar Deus, usando /deus. Gamemode e OP não mudam isso.")
                .defineListAllowEmpty("admins", List.of("SEU_NICK_AQUI"), o -> o instanceof String);
        SO_ADMIN_SENTA = b.comment("Se verdadeiro, só os admins acima conseguem sentar no trono.")
                .define("so_admin_senta", true);
        DIM_INFERNO = b.comment("Dimensão para onde vai quem tentar virar Deus (/deus) sem ser O Deus.")
                .define("dimensao_inferno", "deus:inferno");
        b.push("onisciencia");
        RADAR_JOGADORES = b.comment("Raio (blocos) em que o radar detecta jogadores.").defineInRange("radar_jogadores", 100, 10, 500);
        RADAR_MOBS = b.comment("Raio (blocos) em que o radar detecta mobs hostis.").defineInRange("radar_mobs", 50, 10, 300);
        CHAT_PRIVADO_ATIVO = b.comment(
                "Permite ao admin ler /msg /tell /w para moderação (poder 'Escuta de sussurros').",
                "DESLIGADO por padrão. Ligado, todos os jogadores recebem o aviso abaixo ao entrar.")
                .define("chat_privado_ativo", false);
        AVISO_TEXTO = b.comment("Aviso mostrado a todos ao entrar quando a escuta está ativa (regras de moderação).")
                .define("aviso_texto", "Aviso: a administração pode ler mensagens privadas (/msg, /tell, /w) para fins de moderação.");
        b.pop();
        b.push("realidade");
        CUPULA_BLOCO = b.comment("Bloco da cúpula do modo Domínio (bedrock não quebra em sobrevivência).")
                .define("cupula_bloco", "minecraft:bedrock");
        REAL_RAIO_SUPERFICIE = b.comment("Raio máximo de Abismo/Nether/Corrupção (colunas ao redor do alvo).")
                .defineInRange("raio_max_superficie", 96, 8, 127);
        REAL_RAIO_ESFERA = b.comment("Raio máximo do Vazio (esfera inteira; é pesado, mantenha baixo).")
                .defineInRange("raio_max_vazio", 40, 8, 100);
        REAL_MS_POR_TICK = b.comment("Tempo máximo por tick gasto reescrevendo o mundo (ms).")
                .defineInRange("ms_por_tick", 6, 1, 40);
        REAL_COLUNAS_POR_TICK = b.comment("Colunas por tick (define a velocidade visual da onda).")
                .defineInRange("colunas_por_tick", 500, 50, 5000);
        REAL_BLOCOS_POR_TICK = b.comment("Blocos por tick no Vazio.")
                .defineInRange("blocos_por_tick", 15000, 1000, 200000);
        REAL_MAX_OPERACOES = b.comment("Quantas transformações ficam guardadas no histórico para restaurar.")
                .defineInRange("max_operacoes", 40, 1, 500);
        b.pop();
        SPEC = b.build();
    }

    private DeusConfig() {}
}
