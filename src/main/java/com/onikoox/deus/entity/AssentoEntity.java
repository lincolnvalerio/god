package com.onikoox.deus.entity;

import com.onikoox.deus.DeusMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * Assento invisível do trono. O jogador "monta" nele e é o próprio assento que desliza: entrando, sentado e
 * levantando. A pose (0 = em pé, 1 = sentado como rei) é sincronizada para o cliente animar o corpo.
 */
public class AssentoEntity extends Entity {
    public enum Fase { ENTRANDO, SENTADO, LEVANTANDO }

    private static final EntityDataAccessor<Float> POSE = SynchedEntityData.defineId(AssentoEntity.class, EntityDataSerializers.FLOAT);

    // ---- Ajustes finos de altura (testar no jogo e mexer aqui) ----
    /** Distância entre o ponto de montaria e os pés do jogador (Player.getMyRidingOffset). */
    private static final double OFFSET_PES = 0.35;
    /** Altura dos pés em relação ao chão do trono quando sentado: quadril no assento (0,5) menos a perna (0,75). */
    private static final double PES_SENTADO = -0.25;
    private static final int DURACAO = 14; // ticks de cada animação

    private Fase fase = Fase.ENTRANDO;
    private int t = 0;
    private Vec3 de = Vec3.ZERO, para = Vec3.ZERO;
    private Vec3 posSentado = Vec3.ZERO, posDePe = Vec3.ZERO;
    private Direction frente = Direction.NORTH;
    private BlockPos trono = BlockPos.ZERO;
    private boolean liberado = false;

    // interpolação no cliente
    private float poseAnt = 0F, poseAtual = 0F;

    public AssentoEntity(EntityType<?> tipo, Level level) {
        super(tipo, level);
        this.noPhysics = true;
    }

    private void configurar(BlockPos trono, Direction frente) {
        this.trono = trono;
        this.frente = frente;
        double cx = trono.getX() + 0.5, cz = trono.getZ() + 0.5;
        posSentado = new Vec3(cx - frente.getStepX() * 0.05, trono.getY() + PES_SENTADO + OFFSET_PES,
                cz - frente.getStepZ() * 0.05);
        posDePe = new Vec3(cx + frente.getStepX() * 1.1, trono.getY(), cz + frente.getStepZ() * 1.1);
        setYRot(frente.toYRot());
    }

    /** Sentar com animação (andar até o trono e sentar). */
    public static AssentoEntity criar(Level level, BlockPos trono, Direction frente, Player jogador) {
        AssentoEntity s = new AssentoEntity(DeusMod.ASSENTO.get(), level);
        s.configurar(trono, frente);
        s.de = jogador.position().add(0, OFFSET_PES, 0);
        s.para = s.posSentado;
        s.setPos(s.de);
        level.addFreshEntity(s);
        jogador.startRiding(s, true);
        return s;
    }

    /** Sentar na hora, já na pose final (volta da observação). */
    public static AssentoEntity sentarDireto(Level level, BlockPos trono, Direction frente, Player jogador) {
        AssentoEntity s = new AssentoEntity(DeusMod.ASSENTO.get(), level);
        s.configurar(trono, frente);
        s.fase = Fase.SENTADO;
        s.setPos(s.posSentado);
        s.entityData.set(POSE, 1F);
        level.addFreshEntity(s);
        jogador.startRiding(s, true);
        return s;
    }

    public boolean isLiberado() { return liberado; }
    public BlockPos trono() { return trono; }
    public Direction frente() { return frente; }
    public Vec3 posDePe() { return posDePe; }

    /** Pose interpolada para o desenho (0 em pé .. 1 sentado). */
    public float pose(float parcial) {
        return Mth.lerp(parcial, poseAnt, poseAtual);
    }

    /** Se o jogador estiver sentado no trono, solta sem animação (antes de teleportes). */
    public static void soltar(Player p) {
        if (p.getVehicle() instanceof AssentoEntity s) s.liberar();
    }

    /** Libera o jogador sem animação (bloco quebrado, jogador morto, etc.). */
    public void liberar() {
        liberado = true;
        discard();
    }

    public void iniciarLevantar() {
        if (fase != Fase.SENTADO) return;
        fase = Fase.LEVANTANDO;
        t = 0;
        de = position();
        para = posDePe.add(0, OFFSET_PES, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            poseAnt = poseAtual;
            poseAtual = entityData.get(POSE);
            return;
        }

        Entity p = getFirstPassenger();
        if (p == null) {
            if (tickCount > 5) discard();
            return;
        }
        if (!p.isAlive()) { liberar(); return; }

        if (p instanceof LivingEntity l) {
            l.setYBodyRot(frente.toYRot());
        }

        switch (fase) {
            case ENTRANDO -> {
                double e = mover(++t);
                entityData.set(POSE, (float) e);
                if (t >= DURACAO) { fase = Fase.SENTADO; t = 0; }
            }
            case SENTADO -> {
                setPos(posSentado);
                entityData.set(POSE, 1F);
                if (p.distanceToSqr(this) > 16.0) liberar(); // foi teleportado para longe
            }
            case LEVANTANDO -> {
                double e = mover(++t);
                entityData.set(POSE, (float) (1.0 - e));
                if (t >= DURACAO) terminar(p);
            }
        }
    }

    private double mover(int passo) {
        float x = Mth.clamp(passo / (float) DURACAO, 0F, 1F);
        double e = x * x * (3 - 2 * x); // smoothstep
        Vec3 v = de.lerp(para, e);
        double arco = Math.sin(Math.PI * e) * 0.10; // leve arco ao levantar/sentar
        setPos(v.x, v.y + arco, v.z);
        setDeltaMovement(Vec3.ZERO);
        return e;
    }

    private void terminar(Entity p) {
        liberado = true;
        p.stopRiding();
        if (p instanceof ServerPlayer sp) {
            sp.teleportTo(sp.serverLevel(), posDePe.x, posDePe.y, posDePe.z, sp.getYRot(), sp.getXRot());
        } else {
            p.setPos(posDePe);
        }
        discard();
    }

    // ---- Entity boilerplate ----
    @Override protected void defineSynchedData() { entityData.define(POSE, 0F); }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
    @Override public double getPassengersRidingOffset() { return 0.0; }
    @Override protected boolean canAddPassenger(Entity e) { return getPassengers().isEmpty(); }
    @Override public boolean isPickable() { return false; }
    @Override public boolean isNoGravity() { return true; }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
