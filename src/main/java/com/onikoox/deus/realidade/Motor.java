package com.onikoox.deus.realidade;

import com.onikoox.deus.DeusConfig;
import com.onikoox.deus.DeusSons;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/** Agenda e executa as transformações da realidade com orçamento de tempo por tick. */
public final class Motor {
    interface Job {
        /** @return true quando terminou. */
        boolean passo(long limiteNs);
        UUID dono();
    }

    private static final List<Job> JOBS = new ArrayList<>();

    private Motor() {}

    /** Ponto que Deus está olhando (até 256 blocos); se for céu, a posição dele. */
    private static BlockPos alvo(ServerPlayer p) {
        HitResult hr = p.pick(256.0D, 1.0F, false);
        return hr.getType() == HitResult.Type.MISS ? p.blockPosition() : BlockPos.containing(hr.getLocation());
    }

    private static boolean limite(ServerPlayer p) {
        long meus = JOBS.stream().filter(j -> p.getUUID().equals(j.dono())).count();
        if (meus >= 3) {
            p.displayClientMessage(Component.translatable("msg.deus.real_limite"), true);
            return true;
        }
        return false;
    }

    /**
     * @param cupula modo Domínio: depois da onda, uma cúpula sobe do chão e tranca todos lá dentro
     * @param perfil bioma pronto (só para Tema.BIOMA)
     */
    public static boolean transformar(ServerPlayer p, Tema tema, int raioPedido, boolean cupula, PerfilBioma perfil) {
        if (tema == Tema.BIOMA && perfil == null) return false;
        if (limite(p)) return false;
        ServerLevel lv = p.serverLevel();
        int max = tema == Tema.VAZIO ? DeusConfig.REAL_RAIO_ESFERA.get() : DeusConfig.REAL_RAIO_SUPERFICIE.get();
        int r = Math.max(8, Math.min(raioPedido, max));

        Operacao op = new Operacao();
        op.dim = lv.dimension().location().toString();
        op.centro = alvo(p);
        op.raio = r;
        op.tema = tema;
        op.tempo = System.currentTimeMillis();
        Historico.get(p.server).adicionar(op);

        JOBS.add(new JobTransformar(lv, op, p.getUUID(), perfil, cupula));
        lv.playSound(null, op.centro, DeusSons.REALIDADE_CARGA.get(), SoundSource.PLAYERS, 3.0F, 1.0F);
        lv.playSound(null, op.centro, DeusSons.REALIDADE_ONDA.get(), SoundSource.PLAYERS, 4.0F, 1.0F);
        p.displayClientMessage(Component.translatable("msg.deus.real_inicio", r), true);
        return true;
    }

    /** Restaura a região olhada: desfaz tudo (da transformação mais nova para a mais antiga), até o Vazio e a cúpula. */
    public static boolean desfazer(ServerPlayer p, int raioPedido) {
        if (limite(p)) return false;
        ServerLevel lv = p.serverLevel();
        BlockPos alvo = alvo(p);
        String dim = lv.dimension().location().toString();
        Historico h = Historico.get(p.server);

        List<Operacao> escolhidas = new ArrayList<>();
        for (int i = h.ops.size() - 1; i >= 0; i--) {
            Operacao o = h.ops.get(i);
            if (!o.dim.equals(dim)) continue;
            if (emAndamento(o)) continue;
            if (Math.sqrt(o.centro.distSqr(alvo)) <= o.raio + raioPedido) escolhidas.add(o);
        }
        if (escolhidas.isEmpty()) {
            p.displayClientMessage(Component.translatable("msg.deus.real_nada"), true);
            return false;
        }
        int total = escolhidas.stream().mapToInt(o -> o.pos.size()).sum();
        h.ops.removeAll(escolhidas);
        h.setDirty();
        JOBS.add(new JobRestaurar(lv, escolhidas, p.getUUID()));
        lv.playSound(null, alvo, DeusSons.REALIDADE_DESFEITA.get(), SoundSource.PLAYERS, 4.0F, 1.0F);
        p.displayClientMessage(Component.translatable("msg.deus.real_desfeita", total), true);
        return true;
    }

    private static boolean emAndamento(Operacao o) {
        for (Job j : JOBS) if (j instanceof JobTransformar t && t.op == o) return true;
        return false;
    }

    @SubscribeEvent
    public static void aoTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || JOBS.isEmpty()) return;
        long limite = Math.max(1L, DeusConfig.REAL_MS_POR_TICK.get() * 1_000_000L / JOBS.size());
        Iterator<Job> it = JOBS.iterator();
        while (it.hasNext()) {
            Job j = it.next();
            boolean fim;
            try {
                fim = j.passo(limite);
            } catch (RuntimeException ex) {
                System.err.println("[Deus] Erro na realidade: " + ex);
                ex.printStackTrace();
                fim = true;
            }
            if (fim) it.remove();
        }
        MinecraftServer s = ServerLifecycleHooks.getCurrentServer();
        if (s != null) Historico.get(s).setDirty();
    }

    @SubscribeEvent
    public static void aoParar(ServerStoppedEvent e) {
        JOBS.clear(); // não carregar tarefas de um mundo para outro (modo solo)
    }
}
