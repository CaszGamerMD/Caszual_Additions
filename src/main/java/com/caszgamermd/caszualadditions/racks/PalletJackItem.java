package com.caszgamermd.caszualadditions.racks;

import com.caszgamermd.caszualadditions.CaszualAdditions;
import com.caszgamermd.caszualadditions.pallet.PalletBlock;
import com.caszgamermd.caszualadditions.pallet.PalletBlockEntity;
import com.caszgamermd.caszualadditions.pallet.PalletCargoCollisionBlock;
import java.util.Objects;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Player-operated pallet jack. Owns exactly ONE intact, glass-wrapped 2x2
 * ground pallet and ALL of its 216 original cargo slots.
 *
 * Stored layout in Jack's CONTAINER component: slot 0 = pallet deck item
 * (wood/plastic/filter/wrapping kept), slots 1..216 = cargo. No nested
 * containers or auxiliary world entity, so saving, dropping the tool,
 * death and relog retain the payload without duplicating a ground pallet.
 */
public final class PalletJackItem extends Item {
    private static final int SIZE=217;
    private static final net.minecraft.resources.Identifier SLOW=
            CaszualAdditions.id("loaded_pallet_jack_slowdown");
    private static final AttributeModifier LOAD_PENALTY=new AttributeModifier(
            SLOW,-0.40,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    public PalletJackItem(Properties settings){super(settings);}

    private static NonNullList<ItemStack> unpack(ItemStack jack){
        NonNullList<ItemStack> slots=NonNullList.withSize(SIZE,ItemStack.EMPTY);
        jack.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY)
                .copyInto(slots);
        return slots;
    }
    public static boolean loaded(ItemStack jack){
        if(!jack.is(RackContent.PALLET_JACK))return false;
        ItemStack deck=unpack(jack).get(0);
        return deck.getItem() instanceof BlockItem item
                &&item.getBlock() instanceof PalletBlock;
    }
    /** Any player carrying a loaded pallet jack moves 40% slower, even if
     * they change their hotbar selection. An empty Jack never slows them.
     */
    public static void initialize(){
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(server.getTickCount()%5!=0)return;
            for(var player:server.getPlayerList().getPlayers()){
                boolean carrying=false;
                var inv=player.getInventory();
                for(int slot=0;slot<inv.getContainerSize();slot++){
                    ItemStack item=inv.getItem(slot);
                    if(item.is(RackContent.PALLET_JACK)&&loaded(item)){
                        carrying=true;break;
                    }
                }
                var speed=player.getAttribute(Attributes.MOVEMENT_SPEED);
                if(speed==null)continue;
                if(carrying){
                    if(!speed.hasModifier(SLOW))speed.addTransientModifier(LOAD_PENALTY);
                }else if(speed.hasModifier(SLOW))speed.removeModifier(SLOW);
            }
        });
    }
    @Override public Component getName(ItemStack stack){
        if(!loaded(stack))return Component.literal("Pallet Jack");
        ItemStack pallet=unpack(stack).get(0);
        return Component.literal("Pallet Jack (Loaded: ")
                .append(pallet.getHoverName()).append(")");
    }
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
        if(!level.isClientSide()){
            player.sendSystemMessage(Component.literal(loaded(player.getItemInHand(hand))
                    ?"Pallet Jack loaded. Right-click a clear floor to unload the wrapped pallet."
                    :"Pallet Jack empty. Right-click a glass-wrapped ground pallet to lift it."));
        }
        return InteractionResult.SUCCESS;
    }

    @Override public InteractionResult useOn(UseOnContext context){
        Player player=context.getPlayer();
        if(player==null)return InteractionResult.PASS;
        Level level=context.getLevel();
        ItemStack jack=context.getItemInHand();
        BlockPos clicked=context.getClickedPos();
        if(level.getBlockState(clicked).getBlock() instanceof PalletCargoCollisionBlock){
            BlockPos owner=PalletCargoCollisionBlock.owningPallet(level,clicked);
            if(owner!=null)clicked=owner;
        }
        if(!(level instanceof ServerLevel server))return InteractionResult.SUCCESS;
        if(loaded(jack))
            return unload(server,player,jack,clicked.relative(context.getClickedFace()),
                    context.getClickedFace());
        return lift(server,player,jack,clicked);
    }

    private static InteractionResult lift(ServerLevel world,Player player,
            ItemStack jack,BlockPos selected){
        PalletBlockEntity source=StorageGroundPalletTransfer.wrappedController(world,selected);
        if(source==null){
            player.sendSystemMessage(Component.literal(
                    "Pallet Jack: only an intact, plastic-wrapped 2x2 ground pallet can be lifted."));
            return InteractionResult.SUCCESS;
        }
        BlockPos root=source.getBlockPos();
        if(!world.mayInteract(player,root)||!player.mayUseItemAt(root,Direction.UP,jack))
            return InteractionResult.FAIL;
        if(loaded(jack))return InteractionResult.FAIL;
        ItemStack deck=source.createDropStack();
        if(deck.isEmpty())return InteractionResult.FAIL;
        NonNullList<ItemStack> packed=NonNullList.withSize(SIZE,ItemStack.EMPTY);
        packed.set(0,deck.copyWithCount(1));
        for(int i=0;i<216;i++)packed.set(i+1,source.getItem(i).copy());
        BlockState state=world.getBlockState(root);
        // Claim the entire payload in the tool BEFORE clearing the original
        // four blocks; no inventory or drop hook can create a second copy.
        jack.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(packed));
        source.clearContent();
        PalletCargoCollisionBlock.clear(world,root,state);
        for(int part=3;part>=0;part--){
            BlockPos at=PalletBlock.partPos(root,part,state);
            BlockState old=world.getBlockState(at);
            if(old.is(state.getBlock())&&old.getValue(PalletBlock.PART)==part)
                world.removeBlock(at,false);
        }
        player.sendSystemMessage(Component.literal(
                "Pallet Jack loaded with wrapped pallet. Walking speed -40%. "
                +"Right-click a clear floor to unload."));
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult unload(ServerLevel world,Player player,
            ItemStack jack,BlockPos desired,Direction clickedFace){
        NonNullList<ItemStack> packed=unpack(jack);
        ItemStack deck=packed.get(0);
        if(!(deck.getItem() instanceof BlockItem blockItem)
                ||!(blockItem.getBlock() instanceof PalletBlock block)){
            player.sendSystemMessage(Component.literal("Pallet Jack: no valid pallet to unload."));
            return InteractionResult.FAIL;
        }
        BlockState facing=null;
        for(int turn=0;turn<4;turn++){
            Direction dir=player.getDirection();
            for(int i=0;i<turn;i++)dir=dir.getClockWise();
            BlockState test=block.defaultBlockState().setValue(PalletBlock.PART,0)
                    .setValue(PalletBlock.LEGACY,false)
                    .setValue(PalletBlock.FACING,dir);
            if(fits(world,player,jack,desired,test)){facing=test;break;}
        }
        if(facing==null){
            player.sendSystemMessage(Component.literal(
                    "Pallet Jack: clear a 2x2 floor footprint before unloading. "
                    +"Your wrapped pallet is still safely carried."));
            return InteractionResult.SUCCESS;
        }
        // Create all four pallet parts as one operation. Never lose the
        // Jack's cargo on a blocked/partially successful placement.
        int placed=0;
        for(int part=0;part<4;part++){
            BlockPos at=PalletBlock.partPos(desired,part,facing);
            if(!world.setBlock(at,facing.setValue(PalletBlock.PART,part),3))break;
            placed++;
        }
        if(placed!=4){
            for(int i=placed-1;i>=0;i--)
                world.removeBlock(PalletBlock.partPos(desired,i,facing),false);
            player.sendSystemMessage(Component.literal(
                    "Pallet Jack: unable to place all four parts. Load retained."));
            return InteractionResult.SUCCESS;
        }
        if(!(world.getBlockEntity(desired) instanceof PalletBlockEntity controller)){
            for(int i=3;i>=0;i--)
                world.removeBlock(PalletBlock.partPos(desired,i,facing),false);
            player.sendSystemMessage(Component.literal(
                    "Pallet Jack: pallet controller could not be created. Load retained."));
            return InteractionResult.SUCCESS;
        }
        controller.initialize(desired,0,deck);
        NonNullList<ItemStack> contents=NonNullList.withSize(216,ItemStack.EMPTY);
        for(int i=0;i<216;i++)contents.set(i,packed.get(i+1).copy());
        controller.restoreJackCargo(contents);
        // Release only AFTER the new real root owns the 216 slots.
        jack.remove(DataComponents.CONTAINER);
        player.sendSystemMessage(Component.literal(
                "Pallet Jack unloaded the wrapped pallet with all cargo preserved."));
        return InteractionResult.SUCCESS;
    }

    private static boolean fits(ServerLevel world,Player player,ItemStack tool,
            BlockPos origin,BlockState shape){
        for(int part=0;part<4;part++){
            BlockPos at=PalletBlock.partPos(origin,part,shape);
            if(world.isOutsideBuildHeight(at)
                    ||!world.getWorldBorder().isWithinBounds(at)
                    ||!world.hasChunkAt(at)
                    ||!world.getBlockState(at).canBeReplaced()
                    ||RackBlock.reservedForAccess(world,at)
                    ||!world.mayInteract(player,at)
                    ||!player.mayUseItemAt(at,Direction.UP,tool))
                return false;
        }
        return true;
    }
}
