package com.caszgamermd.caszualadditions.xp;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** A ground-standing jar/collector that vacuums nearby XP orbs.
 * Right click withdraws XP; sneak-right-click checks remaining capacity.
 * Breaking the device drops ONE item with its stored XP intact.
 */
public final class XpCollectorBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE=box(2,0,2,14,10,14);
    private final MapCodec<XpCollectorBlock> codec=MapCodec.unit(this);
    public XpCollectorBlock(BlockBehaviour.Properties properties){super(properties);}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return codec;}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,
            BlockPos pos,CollisionContext context){return SHAPE;}
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,
            BlockPos pos,CollisionContext context){return SHAPE;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){
        return new XpCollectorBlockEntity(pos,state);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,BlockState state,BlockEntityType<T> type){
        if(level.isClientSide())return null;
        return type==XpBlockEntities.COLLECTOR
                ?(world,pos,block,be)->XpCollectorBlockEntity.tick(
                        (ServerLevel)world,pos,(XpCollectorBlockEntity)be):null;
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,
            @Nullable LivingEntity placer,ItemStack stack){
        super.setPlacedBy(level,pos,state,placer,stack);
        if(level.getBlockEntity(pos) instanceof XpCollectorBlockEntity collector)
            collector.setStored(XpCollectorBlockEntity.itemXp(stack));
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,
            Level level,BlockPos pos,Player player,BlockHitResult hit){
        if(level instanceof ServerLevel
                &&level.getBlockEntity(pos) instanceof XpCollectorBlockEntity collector){
            int before=collector.stored();
            if(!player.isShiftKeyDown()&&before>0){
                // Withdraw from the block FIRST; grant precisely that
                // quantity once, never releasing duplicate XP orbs.
                player.giveExperiencePoints(collector.withdraw());
            }
            String label=player.isShiftKeyDown()
                    ?"XP Collector storage: "+before+"/"+XpCollectorBlockEntity.CAPACITY
                    :before>0?"Transferred "+before+" XP to player."
                        :"XP Collector empty — collecting nearby ground XP.";
            if(player instanceof net.minecraft.server.level.ServerPlayer user)
                user.connection.send(
                        new net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket(
                                Component.literal(label)));
        }
        return InteractionResult.SUCCESS;
    }
    @Override protected List<ItemStack> getDrops(BlockState state,LootParams.Builder builder){
        BlockEntity entity=builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if(entity instanceof XpCollectorBlockEntity collector)
            return List.of(collector.asItem());
        return List.of(new ItemStack(XpBlocks.XP_COLLECTOR));
    }
    @Override protected ItemStack getCloneItemStack(LevelReader level,BlockPos pos,
            BlockState state,boolean includeData){
        return level.getBlockEntity(pos) instanceof XpCollectorBlockEntity collector
                ?collector.asItem():new ItemStack(XpBlocks.XP_COLLECTOR);
    }
}
