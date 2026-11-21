package com.onikoox.deus;

import com.onikoox.deus.block.ThroneBlock;
import com.onikoox.deus.entity.AssentoEntity;
import com.onikoox.deus.entity.CorpoEntity;
import com.onikoox.deus.poder.EscutaChat;
import com.onikoox.deus.poder.Observacao;
import com.onikoox.deus.poder.Radar;
import com.onikoox.deus.realidade.Motor;
import com.onikoox.deus.rede.Rede;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(DeusMod.ID)
public class DeusMod {
    public static final String ID = "deus";

    public static final DeferredRegister<Block> BLOCOS = DeferredRegister.create(ForgeRegistries.BLOCKS, ID);
    public static final DeferredRegister<Item> ITENS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);
    public static final DeferredRegister<EntityType<?>> ENTIDADES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ID);
    public static final DeferredRegister<CreativeModeTab> ABAS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);

    public static final RegistryObject<Block> TRONO = BLOCOS.register("trono",
            () -> new ThroneBlock(BlockBehaviour.Properties.of()
                    .strength(5.0F, 1200.0F).noOcclusion().sound(SoundType.METAL).requiresCorrectToolForDrops()));

    public static final RegistryObject<Item> TRONO_ITEM = ITENS.register("trono",
            () -> new BlockItem(TRONO.get(), new Item.Properties().rarity(Rarity.EPIC)));

    public static final RegistryObject<EntityType<AssentoEntity>> ASSENTO = ENTIDADES.register("assento",
            () -> EntityType.Builder.<AssentoEntity>of(AssentoEntity::new, MobCategory.MISC)
                    .sized(0.01F, 0.01F).noSave().noSummon().fireImmune()
                    .clientTrackingRange(10).updateInterval(1).build("assento"));

    public static final RegistryObject<EntityType<CorpoEntity>> CORPO = ENTIDADES.register("corpo",
            () -> EntityType.Builder.<CorpoEntity>of(CorpoEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F).noSave().fireImmune().clientTrackingRange(10).updateInterval(2).build("corpo"));

    public static final RegistryObject<CreativeModeTab> ABA = ABAS.register("deus",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.deus"))
                    .icon(() -> TRONO_ITEM.get().getDefaultInstance())
                    .displayItems((params, out) -> {
                        out.accept(TRONO_ITEM.get());
                    }).build());

    public DeusMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCOS.register(bus);
        ITENS.register(bus);
        ENTIDADES.register(bus);
        ABAS.register(bus);
        DeusSons.SONS.register(bus);
        bus.addListener((EntityAttributeCreationEvent e) -> e.put(CORPO.get(), CorpoEntity.atributos().build()));
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, DeusConfig.SPEC);
        Rede.registrar();
        MinecraftForge.EVENT_BUS.register(Eventos.class);
        MinecraftForge.EVENT_BUS.register(Julgamento.class);
        MinecraftForge.EVENT_BUS.register(Motor.class);
        MinecraftForge.EVENT_BUS.register(Observacao.class);
        MinecraftForge.EVENT_BUS.register(Radar.class);
        MinecraftForge.EVENT_BUS.register(EscutaChat.class);
    }
}
