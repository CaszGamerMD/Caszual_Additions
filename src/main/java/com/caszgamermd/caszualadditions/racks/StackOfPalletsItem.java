package com.caszgamermd.caszualadditions.racks;

import com.caszgamermd.caszualadditions.pallet.PalletBlock;
import com.caszgamermd.caszualadditions.pallet.PalletBlockEntity;
import com.caszgamermd.caszualadditions.pallet.PalletCargoCollisionBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A one-item warehouse magazine of 12 INDEPENDENT empty pallet items.
 * The complete ItemStacks (wood plank data, plastic dye, copper patina,
 * wrapping and filter properties) are preserved in CONTAINER components.
 * Never merges pallet variants or destroys a full pallet's inventory.
 */
public final class StackOfPalletsItem extends BlockItem {
    public static final int CAPACITY=12;
    public StackOfPalletsItem(StackOfPalletsBlock block,Properties settings){super(block,settings);}

    public static List<ItemStack> pallets(ItemStack magazine){
        var contents=magazine.getOrDefault(DataComponents.CONTAINER,
                ItemContainerContents.EMPTY);
        List<ItemStack> stored=new ArrayList<>(CAPACITY);
        NonNullList<ItemStack> physical=NonNullList.withSize(CAPACITY,ItemStack.EMPTY);
        contents.copyInto(physical);
        for(ItemStack item:physical)
            if(!item.isEmpty())stored.add(item.copyWithCount(1));
        return stored;
    }
    public static boolean isPallet(ItemStack item){
        return item.getItem() instanceof BlockItem block
                &&block.getBlock() instanceof PalletBlock;
    }
    public static int count(ItemStack magazine){return pallets(magazine).size();}
    public static boolean canStore(ItemStack magazine,ItemStack item){
        return magazine.is(RackContent.STACK_OF_PALLETS)&&isPallet(item)
                &&count(magazine)<CAPACITY;
    }
    public static void setContents(ItemStack magazine,List<ItemStack> stacks){
        if(stacks.size()>CAPACITY)throw new IllegalArgumentException("At most 12 pallets");
        List<ItemStack> safe=new ArrayList<>(stacks.size());
        for(ItemStack item:stacks){
            if(!isPallet(item))throw new IllegalArgumentException("Not a pallet");
            safe.add(item.copyWithCount(1));
        }
        magazine.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(safe));
    }
    public static boolean put(ItemStack magazine,ItemStack pallet){
        if(!canStore(magazine,pallet))return false;
        List<ItemStack> all=pallets(magazine);
        all.add(pallet.copyWithCount(1));
        setContents(magazine,all);
        return true;
    }
    public static ItemStack take(ItemStack magazine){
        List<ItemStack> all=pallets(magazine);
        if(all.isEmpty())return ItemStack.EMPTY;
        ItemStack last=all.remove(all.size()-1);
        setContents(magazine,all);
        return last;
    }
    @Override public InteractionResult use(Level level,Player player,
            InteractionHand hand){
        if(level instanceof ServerLevel){
            ItemStack container=player.getItemInHand(hand);
            ItemStack extracted=take(container);
            if(extracted.isEmpty()){
                player.sendSystemMessage(Component.literal("Pallet stack empty (0/12)."));
            }else{
                player.getInventory().placeItemBackInInventory(extracted);
                player.sendSystemMessage(Component.literal("Retrieved "+
                        extracted.getHoverName().getString()+" ("
                        +count(container)+"/12 left)."));
            }
        }
        return InteractionResult.SUCCESS;
    }
    @Override public InteractionResult useOn(UseOnContext context){
        Level level=context.getLevel();
        Player player=context.getPlayer();
        if(player==null)return InteractionResult.PASS;
        BlockPos clicked=context.getClickedPos();
        ItemStack container=context.getItemInHand();
        // Sneak-click a vacant rack/PTP to use the next stored pallet.
        if(player.isShiftKeyDown()){
            RackBlockEntity rack=RackBlock.controller(level,clicked);
            PtpBlockEntity ptp=PtpBlock.controller(level,clicked);
            if(rack!=null||ptp!=null){
                if(level instanceof ServerLevel){
                    List<ItemStack> all=pallets(container);
                    if(all.isEmpty())return InteractionResult.FAIL;
                    ItemStack stored=all.get(all.size()-1);
                    boolean installed=false;
                    if(rack!=null&&!rack.hasPallet()&&rack.isEmpty()){
                        rack.install(stored);installed=rack.hasPallet();
                    }else if(ptp!=null&&!ptp.hasPallet()){
                        installed=ptp.installPallet(stored);
                    }
                    if(installed){
                        all.remove(all.size()-1);
                        setContents(container,all);
                        player.sendSystemMessage(Component.literal(
                                "Installed "+stored.getHoverName().getString()
                                +" from stack ("+all.size()+"/12)."));
                    }else player.sendSystemMessage(Component.literal(
                            "Pallet destination must be vacant and accessible."));
                }
                return InteractionResult.SUCCESS;
            }
        }
        BlockState state=level.getBlockState(clicked);
        if(!(state.getBlock() instanceof PalletBlock))return super.useOn(context);
        BlockPos root=PalletBlock.rootPos(clicked,state);
        if(!(level.getBlockEntity(root) instanceof PalletBlockEntity pallet))
            return InteractionResult.FAIL;
        if(!pallet.isEmpty()||!canStore(container,pallet.createDropStack())){
            if(level instanceof ServerLevel)
                player.sendSystemMessage(Component.literal(
                        "Only empty pallets can be stacked (maximum 12)."));
            return InteractionResult.FAIL;
        }
        if(!(level instanceof ServerLevel server))return InteractionResult.SUCCESS;
        ItemStack stored=pallet.createDropStack();
        if(!put(container,stored))return InteractionResult.FAIL;
        // Vanilla break hooks are deliberately not called. We already took
        // ownership of this EMPTY pallet item once, so there must be no drop.
        PalletCargoCollisionBlock.clear(server,root,state);
        for(int part=3;part>=0;part--){
            BlockPos cell=PalletBlock.partPos(root,part,state);
            BlockState existing=server.getBlockState(cell);
            if(existing.getBlock()==state.getBlock()
                    &&existing.getValue(PalletBlock.PART)==part)
                server.removeBlock(cell,false);
        }
        player.sendSystemMessage(Component.literal("Stored "+
                stored.getHoverName().getString()+" ("+count(container)+"/12)."));
        return InteractionResult.SUCCESS;
    }
}
