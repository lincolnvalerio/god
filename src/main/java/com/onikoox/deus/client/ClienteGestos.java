package com.onikoox.deus.client;

import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;

/** Quem está conjurando agora e de que forma (sentado = gesto discreto; em pé = braços erguidos). */
public final class ClienteGestos {
    private static final Int2IntOpenHashMap RESTANTE = new Int2IntOpenHashMap();
    private static final Int2BooleanOpenHashMap SENTADO = new Int2BooleanOpenHashMap();

    private ClienteGestos() {}

    public static void tocar(int entidade, int duracao, boolean sentado) {
        RESTANTE.put(entidade, duracao);
        SENTADO.put(entidade, sentado);
    }

    public static boolean ativo(int entidade) {
        return RESTANTE.get(entidade) > 0;
    }

    /** 0..1: sobe e desce durante o gesto (pico no meio). */
    public static float intensidade(int entidade) {
        int r = RESTANTE.get(entidade);
        if (r <= 0) return 0F;
        return (float) Math.sin(Math.PI * (1.0 - r / 30.0));
    }

    public static boolean sentado(int entidade) {
        return SENTADO.get(entidade);
    }

    public static void tick() {
        if (RESTANTE.isEmpty()) return;
        IntArrayList fim = new IntArrayList();
        for (Int2IntMap.Entry e : RESTANTE.int2IntEntrySet()) {
            int v = e.getIntValue() - 1;
            if (v <= 0) fim.add(e.getIntKey());
            else e.setValue(v);
        }
        for (int k : fim) {
            RESTANTE.remove(k);
            SENTADO.remove(k);
        }
    }
}
