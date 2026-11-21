package com.onikoox.deus.entity;

import com.onikoox.deus.DeusMod;
import com.onikoox.deus.poder.Observacao;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * O corpo do Deus enquanto o espírito observa/vaga: fica parado onde ele estava (sentado no trono ou em pé),
 * com a aparência dele. Invulnerável; se alguém o atacar, o Deus volta ao corpo na hora.
 */
public class CorpoEntity extends LivingEntity {
    private static final EntityDataAccessor<Optional<UUID>> DONO = SynchedEntityData.defineId(CorpoEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> SENTADO = SynchedEntityData.defineId(CorpoEntity.class, EntityDataSerializers.BOOLEAN);

    public CorpoEntity(EntityType<? extends CorpoEntity> tipo, Level level) {
        super(tipo, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
    }

    public static AttributeSupplier.Builder atributos() {
        return LivingEntity.createLivingAttributes();
    }

    public static CorpoEntity criar(ServerLevel lv, UUID dono, boolean sentado, Vec3 pos, float yaw) {
        CorpoEntity c = new CorpoEntity(DeusMod.CORPO.get(), lv);
        c.moveTo(pos.x, pos.y, pos.z, yaw, 0F);
        c.yBodyRot = yaw;
        c.yBodyRotO = yaw;
        c.yHeadRot = yaw;
        c.yHeadRotO = yaw;
        c.entityData.set(DONO, Optional.of(dono));
        c.entityData.set(SENTADO, sentado);
        lv.addFreshEntity(c);
        return c;
    }

    public UUID dono() { return entityData.get(DONO).orElse(null); }

    public boolean sentado() { return entityData.get(SENTADO); }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DONO, Optional.empty());
        entityData.define(SENTADO, false);
    }

    @Override
    public boolean hurt(DamageSource fonte, float quantidade) {
        if (!level().isClientSide) Observacao.corpoAtacado(this);
        return false;
    }

    @Override public Iterable<ItemStack> getArmorSlots() { return List.of(); }
    @Override public ItemStack getItemBySlot(EquipmentSlot slot) { return ItemStack.EMPTY; }
    @Override public void setItemSlot(EquipmentSlot slot, ItemStack stack) {}
    @Override public HumanoidArm getMainArm() { return HumanoidArm.RIGHT; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeCollidedWith() { return false; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
    }
}
