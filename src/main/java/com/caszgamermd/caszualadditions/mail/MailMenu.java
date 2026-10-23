package com.caszgamermd.caszualadditions.mail;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

/**
 * Sender: 18 browsable player heads + 9 PRIVATE outgoing staging slots.
 * Click a head to select its UUID, deposit items and press SEND. A recipient
 * name is never trusted from a client packet, and failed sends consume zero.
 * PO Box: exactly 27 saved shared slots, always keyed to the viewer UUID.
 */
public final class MailMenu extends AbstractContainerMenu {
    private static final int VISIBLE_PLAYERS=18,OUTGOING=9;
    public static final int PAGE_PREVIOUS=100,PAGE_NEXT=101,SEND=102;
    private final BlockPos pos;
    private final boolean sending;
    private final Container mail;
    private final SimpleContainer selectors=new SimpleContainer(VISIBLE_PLAYERS);
    private final SimpleContainer outgoing=new SimpleContainer(OUTGOING);
    private final MailSavedData saved;
    private final List<MailSavedData.Recipient> recipients;
    private final Player viewer;
    private int page=0,selected=-1,pageCount=1;

    public MailMenu(int id,Inventory inventory,BlockPos pos,boolean sending){
        super(sending?MailContent.MAILBOX_MENU:MailContent.PO_BOX_MENU,id);
        this.pos=pos;
        this.sending=sending;
        viewer=inventory.player;
        saved=viewer instanceof ServerPlayer sp?MailSavedData.get(sp.level().getServer()):null;
        if(saved!=null&&viewer instanceof ServerPlayer sp)saved.remember(sp);
        recipients=saved==null?List.of():saved.recipients();
        pageCount=Math.max(1,(recipients.size()+VISIBLE_PLAYERS-1)/VISIBLE_PLAYERS);
        mail=saved==null?new SimpleContainer(MailSavedData.BOX_SIZE)
                :saved.ownBox(viewer.getUUID());
        addDataSlot(new DataSlot(){
            @Override public int get(){return page;}
            @Override public void set(int v){page=Math.max(0,v);}
        });
        addDataSlot(new DataSlot(){
            @Override public int get(){return selected;}
            @Override public void set(int v){selected=v;}
        });
        addDataSlot(new DataSlot(){
            @Override public int get(){return pageCount;}
            @Override public void set(int v){pageCount=Math.max(1,v);}
        });
        if(sending){
            for(int i=0;i<VISIBLE_PLAYERS;i++)
                addSlot(new Slot(selectors,i,8+i%9*18,20+i/9*18){
                    @Override public boolean mayPlace(ItemStack item){return false;}
                    @Override public boolean mayPickup(Player player){return false;}
                });
            for(int i=0;i<OUTGOING;i++)
                addSlot(new Slot(outgoing,i,8+i*18,90));
            refreshSelectors();
            addStandardInventorySlots(inventory,8,152);
        }else{
            for(int i=0;i<MailSavedData.BOX_SIZE;i++)
                addSlot(new Slot(mail,i,8+i%9*18,20+i/9*18));
            addStandardInventorySlots(inventory,8,102);
        }
    }
    private void refreshSelectors(){
        if(!sending||saved==null)return;
        for(int i=0;i<VISIBLE_PLAYERS;i++){
            int which=page*VISIBLE_PLAYERS+i;
            ItemStack icon=ItemStack.EMPTY;
            if(which<recipients.size()){
                MailSavedData.Recipient entry=recipients.get(which);
                icon=new ItemStack(Items.PLAYER_HEAD);
                icon.set(DataComponents.CUSTOM_NAME,Component.literal(entry.name()));
            }
            selectors.setItem(i,icon);
        }
        broadcastFullState();
    }
    public boolean sending(){return sending;}
    public int page(){return page;}
    public int pageCount(){return pageCount;}
    public int selected(){return selected;}
    public String selectedName(){
        if(selected<0)return "None selected";
        // The server sends the selected player head via the selector slots.
        int visible=selected-page*VISIBLE_PLAYERS;
        if(visible>=0&&visible<VISIBLE_PLAYERS){
            ItemStack head=selectors.getItem(visible);
            if(!head.isEmpty())return head.getHoverName().getString();
        }
        return "Recipient selected";
    }
    @Override public boolean clickMenuButton(Player player,int id){
        if(!sending||saved==null||!stillValid(player))return false;
        // All player-head selections are real server-validated menu buttons;
        // no spoofable client-supplied recipient name or UUID is ever trusted.
        if(id>=0&&id<VISIBLE_PLAYERS){
            int candidate=page*VISIBLE_PLAYERS+id;
            if(candidate>=recipients.size())return false;
            selected=candidate;
            broadcastFullState();
            return true;
        }
        if(id==PAGE_PREVIOUS&&page>0){
            page--;refreshSelectors();return true;
        }
        if(id==PAGE_NEXT&&page+1<pageCount){
            page++;refreshSelectors();return true;
        }
        if(id!=SEND||selected<0||selected>=recipients.size())return false;
        MailSavedData.Recipient recipient=recipients.get(selected);
        if(!saved.deliver(recipient.id(),outgoing)){
            player.sendSystemMessage(Component.literal(
                    "Mail not sent: outgoing slots empty, or "+recipient.name()
                            +"'s PO Box cannot fit every item. All items retained."));
            return false;
        }
        player.sendSystemMessage(Component.literal(
                "Mail delivered to "+recipient.name()+"'s private PO Box."));
        broadcastFullState();
        return true;
    }
    @Override public boolean stillValid(Player player){
        if(!viewer.getUUID().equals(player.getUUID()))return false;
        Block expected=sending?MailContent.MAILBOX:MailContent.PO_BOX;
        return player.level().getBlockState(pos).is(expected)
                &&player.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<64.0;
    }
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(index<0||index>=slots.size())return ItemStack.EMPTY;
        if(sending&&index<VISIBLE_PLAYERS)return ItemStack.EMPTY;
        Slot source=slots.get(index);
        if(!source.hasItem())return ItemStack.EMPTY;
        ItemStack original=source.getItem(),result=original.copy();
        int start=sending?VISIBLE_PLAYERS+OUTGOING:MailSavedData.BOX_SIZE;
        boolean moved=index<start
                ?moveItemStackTo(original,start,slots.size(),true)
                :moveItemStackTo(original,sending?VISIBLE_PLAYERS:0,start,false);
        if(!moved)return ItemStack.EMPTY;
        if(original.isEmpty())source.setByPlayer(ItemStack.EMPTY);
        else source.setChanged();
        return result;
    }
    @Override public void removed(Player player){
        super.removed(player);
        if(!sending||player.level().isClientSide())return;
        // Outgoing items belong only to this sender. Return every unsent
        // stack on GUI close/disconnect; never leave it in a public box.
        for(int i=0;i<OUTGOING;i++){
            ItemStack stack=outgoing.removeItemNoUpdate(i);
            if(!stack.isEmpty())player.getInventory().placeItemBackInInventory(stack);
        }
    }
}
