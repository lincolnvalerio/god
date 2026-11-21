package com.onikoox.deus;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Sons do Hand of God reaproveitados nos efeitos da Realidade. */
public final class DeusSons {
    public static final DeferredRegister<SoundEvent> SONS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, DeusMod.ID);

    public static final RegistryObject<SoundEvent> REALIDADE_CARGA = som("realidade_carga");
    public static final RegistryObject<SoundEvent> REALIDADE_ONDA = som("realidade_onda");
    public static final RegistryObject<SoundEvent> REALIDADE_DESFEITA = som("realidade_desfeita");
    public static final RegistryObject<SoundEvent> ONDA_CHOQUE = som("onda_choque");
    public static final RegistryObject<SoundEvent> ENERGIA_PULSO = som("energia_pulso");
    public static final RegistryObject<SoundEvent> ELETRO_TROVAO = som("eletro_trovao");

    private DeusSons() {}

    private static RegistryObject<SoundEvent> som(String nome) {
        return SONS.register(nome, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(DeusMod.ID, nome)));
    }
}
