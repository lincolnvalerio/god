package com.onikoox.deus.poder;

import java.util.List;

/** Catálogo de poderes (compartilhado entre cliente e servidor). */
public final class Poderes {
    public enum Categoria {
        ONISCIENCIA,
        REALIDADE,
        PROTECAO,
        CLONES,
        TEMPO,
        DIVINDADE,
        DARK,
        ONIPOTENCIA,
        MUNDOS
    }

    /** pronto = já implementado; os demais aparecem no painel como "em breve". */
    public record Poder(String id, Categoria cat, boolean pronto) {}

    public static final List<Poder> TODOS = List.of(
            p("onis.jogadores", Categoria.ONISCIENCIA, true),
            p("onis.observar", Categoria.ONISCIENCIA, true),
            p("onis.vagar", Categoria.ONISCIENCIA, true),
            p("onis.radar", Categoria.ONISCIENCIA, true),
            p("onis.xray_min", Categoria.ONISCIENCIA, true),
            p("onis.xray_ent", Categoria.ONISCIENCIA, true),
            p("onis.escuta", Categoria.ONISCIENCIA, true),
            p("real.vazio", Categoria.REALIDADE, true),
            p("real.abismo", Categoria.REALIDADE, true),
            p("real.nether", Categoria.REALIDADE, true),
            p("real.corrupcao", Categoria.REALIDADE, true),
            p("real.limpar", Categoria.REALIDADE, true),
            p("real.restaurar", Categoria.REALIDADE, true),
            p("real.biomas", Categoria.REALIDADE, true),
            p("real.inversao", Categoria.REALIDADE, false),
            p("prot.infinito", Categoria.PROTECAO, false),
            p("clo.exercito", Categoria.CLONES, false),
            p("clo.substituicao", Categoria.CLONES, false),
            p("clo.avatar", Categoria.CLONES, false),
            p("tempo.chuva", Categoria.TEMPO, true),
            p("tempo.tempestade", Categoria.TEMPO, true),
            p("tempo.limpo", Categoria.TEMPO, true),
            p("tempo.dia", Categoria.TEMPO, true),
            p("tempo.noite", Categoria.TEMPO, true),
            p("tempo.horario", Categoria.TEMPO, true),
            p("tempo.gravacao", Categoria.TEMPO, false),
            p("tempo.espiritual", Categoria.TEMPO, false),
            p("tempo.raio", Categoria.TEMPO, false),
            p("tempo.surpreender", Categoria.TEMPO, false),
            p("div.explosao", Categoria.DIVINDADE, false),
            p("div.suspenso", Categoria.DIVINDADE, false),
            p("div.reverencia", Categoria.DIVINDADE, false),
            p("div.luz", Categoria.DIVINDADE, false),
            p("div.prisao", Categoria.DIVINDADE, false),
            p("div.intang", Categoria.DIVINDADE, false),
            p("div.invis", Categoria.DIVINDADE, false),
            p("dark.paredes", Categoria.DARK, false),
            p("dark.empurrar", Categoria.DARK, false),
            p("dark.espinhos", Categoria.DARK, false),
            p("dark.lancas", Categoria.DARK, false),
            p("dark.contencao", Categoria.DARK, false),
            p("dark.maos", Categoria.DARK, false),
            p("dark.atacar", Categoria.DARK, false),
            p("omni.visual", Categoria.ONIPOTENCIA, false),
            p("omni.telecinese", Categoria.ONIPOTENCIA, false),
            p("mundo.ceu", Categoria.MUNDOS, false),
            p("mundo.inferno", Categoria.MUNDOS, false)
    );

    private static Poder p(String id, Categoria c, boolean pronto) {
        return new Poder(id, c, pronto);
    }

    public static Poder achar(String id) {
        for (Poder x : TODOS) if (x.id().equals(id)) return x;
        return null;
    }

    public static List<Poder> da(Categoria c) {
        return TODOS.stream().filter(x -> x.cat() == c).toList();
    }

    private Poderes() {}
}
