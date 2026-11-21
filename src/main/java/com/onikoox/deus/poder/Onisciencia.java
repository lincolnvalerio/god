package com.onikoox.deus.poder;

import com.onikoox.deus.rede.DadosJogador;
import com.onikoox.deus.rede.PacoteDetalhes;
import com.onikoox.deus.rede.Rede;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Operações do painel de jogadores sobre o inventário e o estado de outro jogador. */
public final class Onisciencia {
    /** 0-35 inventário (0-8 hotbar), 36-39 armadura (botas..capacete), 40 mão secundária. */
    public static final int SLOTS = 41;

    private Onisciencia() {}

    public static void enviarDetalhes(ServerPlayer admin, ServerPlayer alvo) {
        List<ItemStack> itens = new ArrayList<>(SLOTS);
        for (int i = 0; i < SLOTS; i++) itens.add(alvo.getInventory().getItem(i).copy());
        DadosJogador d = new DadosJogador(alvo.getUUID(), alvo.getGameProfile().getName(),
                alvo.level().dimension().location().toString(), alvo.getX(), alvo.getY(), alvo.getZ(),
                alvo.getHealth(), alvo.getMaxHealth(), alvo.getFoodData().getFoodLevel(),
                alvo.gameMode.getGameModeForPlayer().getId(), alvo.experienceLevel,
                InventariosSalvos.get(admin.server).tem(alvo.getUUID()), itens);
        Rede.CANAL.send(PacketDistributor.PLAYER.with(() -> admin), new PacoteDetalhes(d));
    }

    private static void darAoAdmin(ServerPlayer admin, ItemStack st) {
        if (st.isEmpty()) return;
        if (!admin.getInventory().add(st)) admin.drop(st, false);
    }

    /** Retira um item do jogador e entrega ao Deus. */
    public static boolean retirar(ServerPlayer admin, ServerPlayer alvo, int slot) {
        if (slot < 0 || slot >= SLOTS) return false;
        ItemStack st = alvo.getInventory().removeItemNoUpdate(slot);
        if (st.isEmpty()) return false;
        alvo.getInventory().setChanged();
        darAoAdmin(admin, st);
        return true;
    }

    /** Entrega ao jogador o item que o Deus está segurando na mão principal. */
    public static boolean entregar(ServerPlayer admin, ServerPlayer alvo) {
        ItemStack fonte = admin.getMainHandItem();
        if (fonte.isEmpty()) return false;
        ItemStack mover = fonte.copy();
        fonte.setCount(0);
        if (!alvo.getInventory().add(mover)) alvo.drop(mover, false);
        return true;
    }

    private static boolean equipamento(ItemStack s) {
        Item i = s.getItem();
        return i instanceof ArmorItem || i instanceof TieredItem || i instanceof ProjectileWeaponItem
                || i instanceof TridentItem || i instanceof ShieldItem || i instanceof ElytraItem;
    }

    /** Armadura, mão secundária e qualquer ferramenta/arma/escudo do inventário vão para o Deus. */
    public static int removerEquipamentos(ServerPlayer admin, ServerPlayer alvo) {
        int n = 0;
        Inventory inv = alvo.getInventory();
        for (int i = 0; i < SLOTS; i++) {
            ItemStack st = inv.getItem(i);
            if (st.isEmpty()) continue;
            boolean armaduraOuMao = i >= 36;
            if (armaduraOuMao || equipamento(st)) {
                darAoAdmin(admin, inv.removeItemNoUpdate(i));
                n++;
            }
        }
        inv.setChanged();
        return n;
    }

    public static void salvar(ServerPlayer admin, ServerPlayer alvo) {
        ListTag lista = new ListTag();
        alvo.getInventory().save(lista);
        InventariosSalvos.get(admin.server).guardar(alvo.getUUID(), lista);
    }

    public static boolean restaurar(ServerPlayer admin, ServerPlayer alvo) {
        InventariosSalvos d = InventariosSalvos.get(admin.server);
        if (!d.tem(alvo.getUUID())) return false;
        alvo.getInventory().load(d.obter(alvo.getUUID()).copy());
        alvo.inventoryMenu.broadcastChanges();
        return true;
    }
}
