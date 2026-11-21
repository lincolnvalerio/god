package com.onikoox.deus.poder;

import com.onikoox.deus.Admin;
import com.onikoox.deus.DeusConfig;
import com.onikoox.deus.entity.AssentoEntity;
import com.onikoox.deus.realidade.Motor;
import com.onikoox.deus.realidade.PerfilBioma;
import com.onikoox.deus.realidade.Tema;
import com.onikoox.deus.rede.PacoteGesto;
import com.onikoox.deus.rede.Rede;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/** Recebe o pedido do cliente e executa o poder no servidor (igual sentado ou em pé; só muda o gesto). */
public final class Executor {
    /** Ações internas dos painéis (não são poderes do catálogo, mas passam pelo mesmo controle). */
    private static final Set<String> ACOES = Set.of(
            "onis.ir", "onis.trazer", "onis.detalhe", "onis.retirar", "onis.entregar", "onis.equip_remover",
            "onis.inv_salvar", "onis.inv_restaurar", "onis.obs_iniciar", "onis.obs_prox", "onis.obs_ant", "onis.obs_parar");

    private Executor() {}

    public static void executar(ServerPlayer p, String id, int valor, String texto) {
        if (!Admin.isAdmin(p)) return; // só O Deus (na lista E ativado com /deus) executa algo
        boolean acao = ACOES.contains(id) || id.startsWith("bioma.");
        if (!acao) {
            Poderes.Poder poder = Poderes.achar(id);
            if (poder == null || !poder.pronto()) return;
        }

        boolean sentado = p.getVehicle() instanceof AssentoEntity;
        ServerLevel lv = p.serverLevel();
        boolean cupula = "cupula".equals(texto);

        boolean ok = switch (id) {
            case "real.vazio" -> Motor.transformar(p, Tema.VAZIO, valor, cupula, null);
            case "real.abismo" -> Motor.transformar(p, Tema.ABISMO, valor, cupula, null);
            case "real.nether" -> Motor.transformar(p, Tema.NETHER, valor, cupula, null);
            case "real.corrupcao" -> Motor.transformar(p, Tema.CORRUPCAO, valor, cupula, null);
            case "real.limpar" -> Motor.transformar(p, Tema.LIMPAR, valor, cupula, null);
            case "real.restaurar" -> Motor.desfazer(p, valor);
            case "tempo.chuva" -> clima(lv, 0, 12000, true, false);
            case "tempo.tempestade" -> clima(lv, 0, 12000, true, true);
            case "tempo.limpo" -> clima(lv, 12000, 0, false, false);
            case "tempo.dia" -> hora(lv, 7);
            case "tempo.noite" -> hora(lv, 19);
            case "tempo.horario" -> hora(lv, Math.floorMod(valor, 24));

            case "onis.ir" -> ir(p, texto);
            case "onis.trazer" -> trazer(p, texto);
            case "onis.detalhe" -> com(p, texto, a -> Onisciencia.enviarDetalhes(p, a));
            case "onis.retirar" -> com(p, texto, a -> { Onisciencia.retirar(p, a, valor); Onisciencia.enviarDetalhes(p, a); });
            case "onis.entregar" -> com(p, texto, a -> { feito(p, Onisciencia.entregar(p, a)); Onisciencia.enviarDetalhes(p, a); });
            case "onis.equip_remover" -> com(p, texto, a -> { feito(p, Onisciencia.removerEquipamentos(p, a) > 0); Onisciencia.enviarDetalhes(p, a); });
            case "onis.inv_salvar" -> com(p, texto, a -> { Onisciencia.salvar(p, a); feito(p, true); Onisciencia.enviarDetalhes(p, a); });
            case "onis.inv_restaurar" -> com(p, texto, a -> { feito(p, Onisciencia.restaurar(p, a)); Onisciencia.enviarDetalhes(p, a); });

            case "onis.obs_iniciar" -> com(p, texto, a -> Observacao.iniciar(p, a));
            case "onis.vagar" -> Observacao.vagar(p);
            case "onis.obs_prox" -> Observacao.alternar(p, 1);
            case "onis.obs_ant" -> Observacao.alternar(p, -1);
            case "onis.obs_parar" -> Observacao.parar(p, false, true);

            case "onis.radar" -> alternar(p, EstadoAdmin.RADAR, id);
            case "onis.xray_min" -> alternar(p, EstadoAdmin.XRAY_MIN, id);
            case "onis.xray_ent" -> alternar(p, EstadoAdmin.XRAY_ENT, id);
            case "onis.escuta" -> escuta(p, id);
            default -> id.startsWith("bioma.")
                    && Motor.transformar(p, Tema.BIOMA, valor, cupula, PerfilBioma.achar(id.substring(6)));
        };
        if (ok && (id.startsWith("real.") || id.startsWith("tempo.") || id.startsWith("bioma."))) gesto(p, sentado);
    }

    private static void feito(ServerPlayer p, boolean ok) {
        p.displayClientMessage(Component.translatable(ok ? "msg.deus.acao_ok" : "msg.deus.acao_falha"), true);
    }

    private static boolean alternar(ServerPlayer p, String chave, String idPoder) {
        boolean on = EstadoAdmin.alternar(p, chave);
        EstadoAdmin.enviar(p);
        p.displayClientMessage(Component.translatable("msg.deus.toggle", Component.translatable("deus.poder." + idPoder),
                Component.translatable(on ? "deus.estado.on" : "deus.estado.off")), true);
        return true;
    }

    private static boolean escuta(ServerPlayer p, String idPoder) {
        if (!DeusConfig.CHAT_PRIVADO_ATIVO.get()) {
            p.displayClientMessage(Component.translatable("msg.deus.escuta_off_config"), true);
            return false;
        }
        return alternar(p, EstadoAdmin.ESCUTA, idPoder);
    }

    private static boolean clima(ServerLevel lv, int limpo, int chuva, boolean chovendo, boolean trovoada) {
        lv.setWeatherParameters(limpo, chuva, chovendo, trovoada);
        return true;
    }

    /** Hora do relógio (0-23). 6h = amanhecer (tick 0 do Minecraft). Mantém o dia atual. */
    private static boolean hora(ServerLevel lv, int hora) {
        long ticks = Math.floorMod(hora - 6, 24) * 1000L;
        long atual = lv.getDayTime();
        lv.setDayTime(atual - Math.floorMod(atual, 24000L) + ticks);
        return true;
    }

    private static ServerPlayer achar(ServerPlayer p, String uuid) {
        try {
            ServerPlayer alvo = p.server.getPlayerList().getPlayer(UUID.fromString(uuid));
            if (alvo == null) p.displayClientMessage(Component.translatable("msg.deus.jogador_off"), true);
            return alvo;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean com(ServerPlayer p, String uuid, Consumer<ServerPlayer> acao) {
        ServerPlayer alvo = achar(p, uuid);
        if (alvo == null) return false;
        acao.accept(alvo);
        return true;
    }

    private static boolean ir(ServerPlayer p, String uuid) {
        ServerPlayer alvo = achar(p, uuid);
        if (alvo == null || alvo == p) return false;
        Observacao.parar(p, false, true);
        AssentoEntity.soltar(p);
        p.teleportTo(alvo.serverLevel(), alvo.getX(), alvo.getY(), alvo.getZ(), p.getYRot(), p.getXRot());
        return true;
    }

    private static boolean trazer(ServerPlayer p, String uuid) {
        ServerPlayer alvo = achar(p, uuid);
        if (alvo == null || alvo == p) return false;
        AssentoEntity.soltar(alvo);
        alvo.teleportTo(p.serverLevel(), p.getX(), p.getY(), p.getZ(), alvo.getYRot(), alvo.getXRot());
        return true;
    }

    private static void gesto(ServerPlayer p, boolean sentado) {
        Rede.CANAL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> p),
                new PacoteGesto(p.getId(), 30, sentado));
    }
}
