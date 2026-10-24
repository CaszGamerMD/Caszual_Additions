package com.caszgamermd.caszualadditions.racks;

import com.caszgamermd.caszualadditions.pallet.PalletBlock;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

/**
 * Nine individually placed transfer blocks self-assemble into one 3x3 floor.
 * PART=0 is an unassembled block; 1..9 identifies the 3x3 cell, with 5 as
 * the ONLY inventory owner. Module blocks can replace their eligible cells.
 * Existing 2x2 Order Collector Hubs remain untouched and can coexist.
 */
public final class PtpBlock extends Block implements EntityBlock {
    public enum Kind { TRANSFER, HOPPER, WRAP }
    public static final IntegerProperty PART=IntegerProperty.create("part",0,9);
    public static final EnumProperty<Direction> FACING=BlockStateProperties.HORIZONTAL_FACING;
    private final Kind kind;
    private final MapCodec<PtpBlock> codec=MapCodec.unit(this);

    public PtpBlock(Kind kind,BlockBehaviour.Properties props){
        super(props);this.kind=kind;
        registerDefaultState(stateDefinition.any().setValue(PART,0)
                .setValue(FACING,Direction.NORTH));
    }
    public Kind kind(){return kind;}
    @Override protected MapCodec<? extends Block> codec(){return codec;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){
        builder.add(PART,FACING);
    }
    public static BlockPos cell(BlockPos root,int part){
        return root.offset((part-1)%3,0,(part-1)/3);
    }
    public static BlockPos root(BlockPos pos,int part){
        return pos.offset(-((part-1)%3),0,-((part-1)/3));
    }
    public static @Nullable PtpBlockEntity controller(Level level,BlockPos pos){
        BlockState state=level.getBlockState(pos);
        if(!(state.getBlock() instanceof PtpBlock))return null;
        int part=state.getValue(PART);
        if(part==0)return null;
        return level.getBlockEntity(root(pos,part).offset(1,0,1))
                instanceof PtpBlockEntity ptp?ptp:null;
    }
    public static boolean allowed(Kind kind,int part){
        return switch(kind){
            case TRANSFER -> true;
            case HOPPER -> part==1||part==3||part==7||part==9;
            case WRAP -> part==2||part==4||part==6||part==8;
        };
    }
    private static boolean canForm(Level level,BlockPos root){
        for(int part=1;part<=9;part++){
            BlockPos pos=cell(root,part);
            if(!level.hasChunkAt(pos))return false;
            BlockState state=level.getBlockState(pos);
            if(!(state.getBlock() instanceof PtpBlock ptp)
                    ||state.getValue(PART)!=0
                    ||!allowed(ptp.kind,part))return false;
        }
        return true;
    }
    private static void tryForm(Level level,BlockPos placed){
        // First completed 3x3 square wins. No chunk loading or overwrites.
        for(int dx=-2;dx<=0;dx++)for(int dz=-2;dz<=0;dz++){
            BlockPos root=placed.offset(dx,0,dz);
            if(!canForm(level,root))continue;
            // Verify reserved height for a centered 2x2 pallet and cargo
            // before converting anything into a controller.
            for(int part=1;part<=9;part++){
                if(part==5)continue;
                BlockPos p=cell(root,part);
                level.setBlock(p,level.getBlockState(p).setValue(PART,part),3);
            }
            BlockPos middle=cell(root,5);
            level.setBlock(middle,level.getBlockState(middle).setValue(PART,5),3);
            return;
        }
    }
    @Override public @Nullable BlockState getStateForPlacement(BlockPlaceContext context){
        // The full front approach of a rack stays available for physical
        // aisle tags, connector upgrades and pallet access.
        return RackBlock.reservedForAccess(context.getLevel(),context.getClickedPos())
                ?null:defaultBlockState().setValue(FACING,
                        context.getHorizontalDirection().getOpposite());
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,
            @Nullable LivingEntity player,ItemStack stack){
        super.setPlacedBy(level,pos,state,player,stack);
        if(!level.isClientSide()&&state.getValue(PART)==0)tryForm(level,pos);
    }
    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos,BlockState state){
        int part=state.getValue(PART);
        if(part==5)return new PtpBlockEntity(pos,state);
        if(kind==Kind.HOPPER&&allowed(kind,part)&&part!=0)
            return new PtpHopperPort(pos,state);
        return null;
    }
    private void swapModule(Level level,BlockPos pos,BlockState state,
            Player player,ItemStack held,PtpBlock replacement){
        int part=state.getValue(PART);
        if(part==0||!allowed(replacement.kind(),part))return;
        if(replacement==this)return;
        if(controller(level,pos)==null)return;
        level.setBlock(pos,replacement.defaultBlockState().setValue(PART,part)
                .setValue(FACING,player.getDirection().getOpposite()),3);
        if(!player.getAbilities().instabuild){
            held.shrink(1);
            player.getInventory().placeItemBackInInventory(new ItemStack(this));
        }
    }
    /** Sneak-right-click with an empty hand to reclaim an empty pallet.
     * Preserve item components/filter/material and never void rejected items. */
    private static void retrieveEmptyPallet(PtpBlockEntity ptp,Player player){
        ItemStack recovered=ptp.removePallet();
        if(!recovered.isEmpty()){
            player.getInventory().placeItemBackInInventory(recovered);
            player.sendSystemMessage(Component.literal("Removed empty pallet from transfer point."));
        }else if(ptp.sealed())
            player.sendSystemMessage(Component.literal(
                    "Unwrap the pallet with shears before removing it."));
        else if(!ptp.emptyCargo())
            player.sendSystemMessage(Component.literal(
                    "Empty the pallet inventory before removing it."));
        else player.sendSystemMessage(Component.literal(
                    "Pallet is reserved by the warehouse or has pending orders."));
    }
    @Override protected InteractionResult useItemOn(ItemStack held,BlockState state,Level level,
            BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(held.is(RackContent.STORAGE_SCANNER_ITEM)
                ||held.is(RackContent.PORTABLE_WRAPPER))return InteractionResult.PASS;
        PtpBlockEntity ptp=controller(level,pos);
        if(ptp==null)return InteractionResult.PASS;
        if(held.getItem() instanceof BlockItem bi && bi.getBlock() instanceof PtpBlock module
                && module.kind()!=Kind.TRANSFER){
            if(!allowed(module.kind(),state.getValue(PART)))return InteractionResult.PASS;
            if(!level.isClientSide()) {
                if(!ptp.sealed())swapModule(level,pos,state,player,held,module);
                else player.sendSystemMessage(Component.literal(
                        "Unwrap the pallet with shears before changing modules."));
            }
            return InteractionResult.SUCCESS;
        }
        if(held.is(Items.SHEARS)&&ptp.sealed()){
            if(!level.isClientSide()){
                ptp.setSealed(false);
                player.sendSystemMessage(Component.literal("Pallet unwrapped."));
            }
            return InteractionResult.SUCCESS;
        }
        if(held.getItem() instanceof BlockItem bi && bi.getBlock() instanceof PalletBlock) {
            if(!level.isClientSide()) {
                if(ptp.hasPallet()) {
                    if(player.isShiftKeyDown()) {
                        retrieveEmptyPallet(ptp,player);
                    }else if(player instanceof ServerPlayer server&&!ptp.sealed())
                        server.openMenu(ptp);
                }else if(ptp.installPallet(held.copyWithCount(1))) {
                    if(!player.getAbilities().instabuild)held.shrink(1);
                }else player.sendSystemMessage(Component.literal(
                        "Cannot load pallet: "+ptp.palletInstallProblem()));
            }
            return InteractionResult.SUCCESS;
        }
        // Ordinary held items can still access the pallet UI without placing
        // the block inside its reserved approach area.
        if(!level.isClientSide()&&ptp.hasPallet()&&!ptp.sealed()
                &&player instanceof ServerPlayer server)server.openMenu(ptp);
        return InteractionResult.SUCCESS;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,
            BlockPos pos,Player player,BlockHitResult hit){
        PtpBlockEntity ptp=controller(level,pos);
        if(ptp==null)return InteractionResult.PASS;
        if(!level.isClientSide()){
            if(player.isShiftKeyDown()){
                // Empty-hand sneak-click ALWAYS changes direction, even with
                // a pallet installed. To reclaim an empty pallet, sneak-click
                // while holding another Pallet item, or use the workstation.
                // Never disrupt an outstanding Builder shipment or active
                // order reservation.
                if(ptp.changeModeFromComputer())
                    player.sendSystemMessage(Component.literal(
                            "Transfer Point: "+(ptp.input()?"INPUT":"OUTPUT")));
                else player.sendSystemMessage(Component.literal(
                        "Cannot change PTP mode: "+ptp.modeChangeProblem()));
            }else if(kind==Kind.WRAP && ptp.hasPallet()){
                if(!ptp.sealed()){
                    ptp.setSealed(true);
                    player.sendSystemMessage(Component.literal(
                            "Pallet sealed. Only shears can unwrap it."));
                } else player.sendSystemMessage(Component.literal(
                        "Pallet is sealed. Use shears to unwrap."));
            }else if(ptp.sealed()){
                player.sendSystemMessage(Component.literal("Pallet is sealed. Use shears."));
            }else if(ptp.hasPallet()&&player instanceof ServerPlayer server)server.openMenu(ptp);
            else player.sendSystemMessage(Component.literal(
                    "Pallet Transfer Point: "+(ptp.input()?"INPUT":"OUTPUT")
                            +" — insert a pallet to begin."));
        }
        return InteractionResult.SUCCESS;
    }
    private static void dismantle(Level level,BlockPos broken,BlockState state,Player player){
        int part=state.getValue(PART);
        if(part==0)return;
        BlockPos origin=root(broken,part),center=cell(origin,5);
        PtpBlockEntity ptp=level.getBlockEntity(center) instanceof PtpBlockEntity t?t:null;
        if(ptp!=null){
            // Breaking the source cancels escrow before the normal survival
            // drop path unseals it; no worker holds a duplicate copy.
            ptp.cancelHaul();
            if(!player.getAbilities().instabuild){
                ptp.setSealed(false);
                Containers.dropContents(level,center,ptp);
                if(ptp.hasPallet())Block.popResource(level,center,ptp.dropPallet());
            }
        }
        for(int i=1;i<=9;i++){
            BlockPos p=cell(origin,i);
            if(p.equals(broken))continue;
            BlockState found=level.getBlockState(p);
            if(found.getBlock() instanceof PtpBlock &&found.getValue(PART)==i)
                level.setBlock(p,found.setValue(PART,0),3);
        }
    }
    @Override public BlockState playerWillDestroy(Level level,BlockPos pos,BlockState state,Player player){
        if(!level.isClientSide())dismantle(level,pos,state,player);
        return super.playerWillDestroy(level,pos,state,player);
    }
    @Override protected List<ItemStack> getDrops(BlockState state,LootParams.Builder loot){
        return List.of(new ItemStack(this));
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,
            CollisionContext context){
        return switch(kind){
            case HOPPER -> Block.box(0,0,0,16,24,16);
            case WRAP -> Block.box(0,0,0,16,8,16);
            default -> Block.box(0,0,0,16,4,16);
        };
    }
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,
            BlockPos pos,CollisionContext context){return getShape(state,level,pos,context);}
}
