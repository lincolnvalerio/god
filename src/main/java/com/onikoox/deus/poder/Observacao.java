package com.onikoox.deus.poder;

import com.onikoox.deus.entity.AssentoEntity;
import com.onikoox.deus.entity.CorpoEntity;
import com.onikoox.deus.rede.PacoteObservacao;
import com.onikoox.deus.rede.Rede;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Espírito do Deus. O corpo fica parado no lugar (um CorpoEntity com a sua aparência; sentado no trono se você
 * estava nele) e o espírito sai em modo espectador:
 *  - Observar: a câmera vira a de um jogador (até 4 marcados; , e . alternam). Você ainda usa o painel dele
 *    (inventário, remover equipamentos, salvar...).
 *  - Vagar: sai voando livremente, sem escolher ninguém. Shift solta a câmera de quem você observa e vira Vagar.
 *  - Voltar ao corpo: Backspace ou o botão do painel. Se o corpo for atacado ou alguém se aproximar, volta sozinho.
 */
public final class Observacao {
    public static final int MAX = 4;

    public static final class Sessao {
        public Vec3 origem;
        public Vec3 posDePe;
        public float yRot, xRot;
        public ResourceKey<Level> dim;
        public GameType modo;
        public final List<UUID> slots = new ArrayList<>();
        public UUID alvoAtual;          // null = vagando livre
        public boolean sentado;
        public BlockPos trono;
        public Direction frente;
        public UUID corpo;
    }

    private static final Map<UUID, Sessao> SESSOES = new HashMap<>();

    private Observacao() {}

    public static Sessao sessao(ServerPlayer admin) {
        return SESSOES.get(admin.getUUID());
    }

    /** Deixa o corpo para trás e vira espírito (modo espectador). */
    private static Sessao abrir(ServerPlayer admin) {
        Sessao s = new Sessao();
        s.origem = admin.position();
        s.dim = admin.level().dimension();
        s.modo = admin.gameMode.getGameModeForPlayer();
        s.xRot = admin.getXRot();
        float yaw = admin.getYRot();
        if (admin.getVehicle() instanceof AssentoEntity seat) {
            s.sentado = true;
            s.trono = seat.trono();
            s.frente = seat.frente();
            s.posDePe = seat.posDePe();
            yaw = s.frente.toYRot();
        }
        s.yRot = yaw;
        s.corpo = CorpoEntity.criar(admin.serverLevel(), admin.getUUID(), s.sentado, s.origem, yaw).getUUID();
        AssentoEntity.soltar(admin);
        SESSOES.put(admin.getUUID(), s);
        admin.setGameMode(GameType.SPECTATOR);
        return s;
    }

    public static boolean iniciar(ServerPlayer admin, ServerPlayer alvo) {
        if (alvo == admin) return false;
        Sessao s = SESSOES.get(admin.getUUID());
        if (s == null) s = abrir(admin);
        if (!s.slots.contains(alvo.getUUID())) {
            if (s.slots.size() >= MAX) {
                admin.displayClientMessage(Component.translatable("msg.deus.obs_cheio"), true);
                return false;
            }
            s.slots.add(alvo.getUUID());
        }
        focar(admin, s, alvo);
        return true;
    }

    /** Sai voando livremente. Se já está observando alguém, solta a câmera dele. */
    public static boolean vagar(ServerPlayer admin) {
        Sessao s = SESSOES.get(admin.getUUID());
        if (s == null) {
            s = abrir(admin);
            enviarHud(admin, s);
            return true;
        }
        if (s.alvoAtual != null) desanexar(admin, s);
        return true;
    }

    private static void focar(ServerPlayer admin, Sessao s, ServerPlayer alvo) {
        s.alvoAtual = alvo.getUUID();
        admin.setCamera(alvo);
        enviarHud(admin, s);
    }

    private static void desanexar(ServerPlayer admin, Sessao s) {
        s.alvoAtual = null;
        admin.setCamera(admin);
        enviarHud(admin, s);
    }

    public static boolean alternar(ServerPlayer admin, int delta) {
        Sessao s = sessao(admin);
        if (s == null || s.slots.isEmpty()) return false;
        MinecraftServer srv = admin.server;
        int base = s.alvoAtual == null ? (delta > 0 ? -1 : 0) : s.slots.indexOf(s.alvoAtual);
        for (int tentativas = 0; tentativas < s.slots.size(); tentativas++) {
            int i = Math.floorMod(base + delta * (tentativas + 1), s.slots.size());
            ServerPlayer alvo = srv.getPlayerList().getPlayer(s.slots.get(i));
            if (alvo != null && alvo != admin) {
                focar(admin, s, alvo);
                return true;
            }
        }
        return false;
    }

    /** Volta ao corpo: posição, dimensão e modo originais; se estava no trono, senta de novo. */
    public static boolean parar(ServerPlayer admin, boolean interrompida, boolean avisar) {
        Sessao s = SESSOES.remove(admin.getUUID());
        if (s == null) return false;
        admin.setCamera(admin);
        MinecraftServer srv = admin.server;
        ServerLevel lv = srv.getLevel(s.dim);
        if (lv != null) {
            Entity corpo = lv.getEntity(s.corpo);
            if (corpo != null) corpo.discard();
            boolean ressentar = s.sentado && s.trono != null && avisar;
            Vec3 destino = (s.sentado && !ressentar && s.posDePe != null) ? s.posDePe : s.origem;
            admin.teleportTo(lv, destino.x, destino.y, destino.z, s.yRot, s.xRot);
            admin.setGameMode(s.modo);
            if (ressentar) AssentoEntity.sentarDireto(lv, s.trono, s.frente, admin);
        } else {
            admin.setGameMode(s.modo);
        }
        if (avisar) {
            Rede.CANAL.send(PacketDistributor.PLAYER.with(() -> admin), new PacoteObservacao(List.of(), -1, false, false));
            admin.displayClientMessage(Component.translatable(
                    interrompida ? "msg.deus.obs_interrompida" : "msg.deus.obs_fim"), true);
        }
        return true;
    }

    /** Atacaram o corpo: o Deus volta na hora. */
    public static void corpoAtacado(CorpoEntity c) {
        UUID dono = c.dono();
        MinecraftServer srv = ServerLifecycleHooks.getCurrentServer();
        if (dono == null || srv == null) return;
        ServerPlayer admin = srv.getPlayerList().getPlayer(dono);
        if (admin != null) parar(admin, true, true);
    }

    private static void enviarHud(ServerPlayer admin, Sessao s) {
        List<String> linhas = new ArrayList<>();
        int ativo = -1;
        for (int i = 0; i < s.slots.size(); i++) {
            ServerPlayer o = admin.server.getPlayerList().getPlayer(s.slots.get(i));
            if (o == null) {
                linhas.add("(offline)");
            } else {
                linhas.add(o.getGameProfile().getName() + "  \u2764 " + Math.round(o.getHealth()) + "/" + Math.round(o.getMaxHealth()));
            }
            if (s.slots.get(i).equals(s.alvoAtual)) ativo = i;
        }
        Rede.CANAL.send(PacketDistributor.PLAYER.with(() -> admin),
                new PacoteObservacao(linhas, ativo, true, s.alvoAtual == null));
    }

    /** Recria o corpo se ele sumiu (ex.: o chunk descarregou enquanto você vagava longe). */
    private static void garantirCorpo(MinecraftServer srv, Sessao s, ServerPlayer admin) {
        ServerLevel lv = srv.getLevel(s.dim);
        if (lv == null || !lv.hasChunkAt(BlockPos.containing(s.origem))) return;
        Entity e = lv.getEntity(s.corpo);
        if (e == null || e.isRemoved()) {
            s.corpo = CorpoEntity.criar(lv, admin.getUUID(), s.sentado, s.origem, s.yRot).getUUID();
        }
    }

    @SubscribeEvent
    public static void aoTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || SESSOES.isEmpty()) return;
        MinecraftServer srv = ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        for (UUID id : new ArrayList<>(SESSOES.keySet())) {
            Sessao s = SESSOES.get(id);
            ServerPlayer admin = srv.getPlayerList().getPlayer(id);
            if (s == null) continue;
            if (admin == null) { SESSOES.remove(id); continue; }

            if (admin.getCamera() == admin) {
                // Shift: o jogo devolveu a câmera. Em vez de voltar ao corpo, você passa a vagar.
                if (s.alvoAtual != null) {
                    s.alvoAtual = null;
                    enviarHud(admin, s);
                }
            } else {
                ServerPlayer alvo = s.alvoAtual == null ? null : srv.getPlayerList().getPlayer(s.alvoAtual);
                if (alvo == null || alvo.isRemoved() || !alvo.isAlive()) {
                    if (!alternar(admin, 1)) desanexar(admin, s);
                    continue;
                }
            }
            int tick = srv.getTickCount();
            if (tick % 20 == 0) enviarHud(admin, s);
            if (tick % 40 == 0) garantirCorpo(srv, s, admin);
        }
    }

    /** Se o Deus sair do jogo como espírito, o corpo volta para onde estava antes de salvar. */
    @SubscribeEvent
    public static void aoSair(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) parar(p, false, false);
    }

    @SubscribeEvent
    public static void aoParar(ServerStoppedEvent e) {
        SESSOES.clear();
    }
}
