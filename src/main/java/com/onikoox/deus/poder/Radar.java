package com.onikoox.deus.poder;

import com.onikoox.deus.Admin;
import com.onikoox.deus.DeusConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Radar de proximidade (atravessa paredes: é só distância). Mede a partir do corpo do Deus, mesmo que ele
 * esteja observando alguém. Jogador entrando no raio: alerta e, se estiver observando, a observação termina.
 */
public final class Radar {
    private static final String[] DIRECOES = {"Norte", "Nordeste", "Leste", "Sudeste", "Sul", "Sudoeste", "Oeste", "Noroeste"};
    private static final Map<UUID, Set<UUID>> JOGADORES = new HashMap<>();
    private static final Map<UUID, Set<UUID>> MOBS = new HashMap<>();
    private static final Map<UUID, Integer> ULTIMO_AVISO_MOBS = new HashMap<>();

    private Radar() {}

    @SubscribeEvent
    public static void aoTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        MinecraftServer srv = ServerLifecycleHooks.getCurrentServer();
        if (srv == null || srv.getTickCount() % 10 != 0) return;
        for (ServerPlayer admin : srv.getPlayerList().getPlayers()) {
            if (Admin.isAdmin(admin) && EstadoAdmin.get(admin, EstadoAdmin.RADAR)) varrer(srv, admin);
        }
    }

    private static String direcao(Vec3 de, Vec3 para) {
        double graus = Math.toDegrees(Math.atan2(para.x - de.x, -(para.z - de.z))); // 0 = norte, horário
        return DIRECOES[Math.floorMod((int) Math.round(graus / 45.0), 8)];
    }

    private static void varrer(MinecraftServer srv, ServerPlayer admin) {
        Observacao.Sessao ses = Observacao.sessao(admin);
        Vec3 origem = ses != null ? ses.origem : admin.position();
        ServerLevel lv = srv.getLevel(ses != null ? ses.dim : admin.level().dimension());
        if (lv == null) return;
        UUID observado = ses != null ? ses.alvoAtual : null;

        // ---- jogadores
        double rj = DeusConfig.RADAR_JOGADORES.get();
        Set<UUID> antes = JOGADORES.computeIfAbsent(admin.getUUID(), k -> new HashSet<>());
        Set<UUID> agora = new HashSet<>();
        List<ServerPlayer> entrantes = new ArrayList<>();
        for (ServerPlayer o : lv.players()) {
            if (o == admin || Admin.isAdmin(o) || o.isSpectator() || o.getUUID().equals(observado)) continue;
            if (o.position().distanceToSqr(origem) <= rj * rj) {
                agora.add(o.getUUID());
                if (!antes.contains(o.getUUID())) entrantes.add(o);
            }
        }
        JOGADORES.put(admin.getUUID(), agora);
        if (!entrantes.isEmpty()) {
            for (ServerPlayer o : entrantes) {
                int dist = (int) Math.round(o.position().distanceTo(origem));
                String dy = (o.getY() >= origem.y ? "+" : "") + Math.round(o.getY() - origem.y) + "y";
                admin.sendSystemMessage(Component.translatable("msg.deus.radar_jogador",
                        o.getGameProfile().getName(), dist, direcao(origem, o.position()), dy));
            }
            admin.level().playSound(null, admin.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 0.5F);
            if (ses != null) Observacao.parar(admin, true, true);
        }

        // ---- mobs hostis (avisa só quando surgem novos, no máximo a cada 5 s)
        double rm = DeusConfig.RADAR_MOBS.get();
        List<Mob> hostis = lv.getEntitiesOfClass(Mob.class, new AABB(origem, origem).inflate(rm),
                m -> m instanceof Enemy && m.position().distanceToSqr(origem) <= rm * rm);
        Set<UUID> mobsAntes = MOBS.computeIfAbsent(admin.getUUID(), k -> new HashSet<>());
        Set<UUID> mobsAgora = new HashSet<>();
        boolean novos = false;
        for (Mob m : hostis) {
            mobsAgora.add(m.getUUID());
            if (!mobsAntes.contains(m.getUUID())) novos = true;
        }
        MOBS.put(admin.getUUID(), mobsAgora);
        int agoraTick = srv.getTickCount();
        if (novos && agoraTick - ULTIMO_AVISO_MOBS.getOrDefault(admin.getUUID(), -1000) >= 100) {
            ULTIMO_AVISO_MOBS.put(admin.getUUID(), agoraTick);
            hostis.sort(Comparator.comparingDouble(m -> m.position().distanceToSqr(origem)));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(3, hostis.size()); i++) {
                Mob m = hostis.get(i);
                if (i > 0) sb.append("; ");
                sb.append(m.getType().getDescription().getString()).append(" ")
                        .append(Math.round(m.position().distanceTo(origem))).append("m ")
                        .append(direcao(origem, m.position()));
            }
            if (hostis.size() > 3) sb.append(" (+").append(hostis.size() - 3).append(")");
            admin.displayClientMessage(Component.translatable("msg.deus.radar_mobs", sb.toString()), true);
        }
    }

    @SubscribeEvent
    public static void aoParar(ServerStoppedEvent e) {
        JOGADORES.clear();
        MOBS.clear();
        ULTIMO_AVISO_MOBS.clear();
    }
}
