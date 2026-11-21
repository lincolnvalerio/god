package com.onikoox.deus.block;

import com.onikoox.deus.Admin;
import com.onikoox.deus.DeusConfig;
import com.onikoox.deus.entity.AssentoEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ThroneBlock extends HorizontalDirectionalBlock {
    private static final VoxelShape[] FORMAS = new VoxelShape[4];

    static {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            FORMAS[d.get2DDataValue()] = Shapes.or(
                    caixa(d, 1, 0, 1, 15, 8, 15),   // base + assento
                    caixa(d, 1, 8, 12, 15, 16, 15), // encosto
                    caixa(d, 1, 8, 2, 3, 13, 12),   // braço esquerdo
                    caixa(d, 13, 8, 2, 15, 13, 12)  // braço direito
            );
        }
    }

    /** Gira uma caixa feita para NORTH (encosto em +Z) para a direção pedida. */
    private static VoxelShape caixa(Direction d, double x1, double y1, double z1, double x2, double y2, double z2) {
        double ax, az, bx, bz;
        switch (d) {
            case EAST -> { ax = 16 - z1; az = x1; bx = 16 - z2; bz = x2; }
            case SOUTH -> { ax = 16 - x1; az = 16 - z1; bx = 16 - x2; bz = 16 - z2; }
            case WEST -> { ax = z1; az = 16 - x1; bx = z2; bz = 16 - x2; }
            default -> { ax = x1; az = z1; bx = x2; bz = z2; }
        }
        return Block.box(Math.min(ax, bx), y1, Math.min(az, bz), Math.max(ax, bx), y2, Math.max(az, bz));
    }

    public ThroneBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    /** FACING = para onde quem senta olha (de frente para quem colocou o bloco). */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return FORMAS[s.getValue(FACING).get2DDataValue()];
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (DeusConfig.SO_ADMIN_SENTA.get() && !Admin.isAdmin(player)) {
            player.displayClientMessage(Component.translatable("msg.deus.trono_negado"), true);
            return InteractionResult.CONSUME;
        }
        if (player.isPassenger()) return InteractionResult.CONSUME;
        if (!level.getEntitiesOfClass(AssentoEntity.class, new AABB(pos).inflate(0.6)).isEmpty()) {
            return InteractionResult.CONSUME; // ocupado
        }
        AssentoEntity.criar(level, pos, state.getValue(FACING), player);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState novo, boolean movido) {
        if (!state.is(novo.getBlock()) && !level.isClientSide) {
            for (AssentoEntity s : level.getEntitiesOfClass(AssentoEntity.class, new AABB(pos).inflate(1.5))) {
                s.liberar();
            }
        }
        super.onRemove(state, level, pos, novo, movido);
    }
}
