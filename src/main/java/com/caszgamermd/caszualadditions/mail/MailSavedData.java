package com.caszgamermd.caszualadditions.mail;

import com.caszgamermd.caszualadditions.CaszualAdditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.NonNullList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * One authoritative OVERWORLD saved file shared across dimensions and PO Box
 * blocks. Inventories belong to player UUIDs, not usernames or physical boxes.
 * Stored player names are only labels; changing a name cannot steal a box.
 */
public final class MailSavedData extends SavedData {
    public static final int BOX_SIZE=27;
    private static final Codec<MailSavedData> CODEC=RecordCodecBuilder.create(instance->
            instance.group(
                    Codec.unboundedMap(Codec.STRING,ItemStack.OPTIONAL_CODEC.listOf())
                            .fieldOf("inboxes").forGetter(data->data.serializedBoxes()),
                    Codec.unboundedMap(Codec.STRING,Codec.STRING)
                            .fieldOf("players").forGetter(data->data.names)
            ).apply(instance,MailSavedData::new));
    private static final SavedDataType<MailSavedData> TYPE=new SavedDataType<>(
            CaszualAdditions.id("post_office"),MailSavedData::new,CODEC,null);

    private final Map<String,NonNullList<ItemStack>> boxes=new HashMap<>();
    private final Map<String,String> names=new HashMap<>();

    public MailSavedData(){}
    private MailSavedData(Map<String,List<ItemStack>> stored,Map<String,String> names){
        this.names.putAll(names);
        stored.forEach((owner,items)->{
            NonNullList<ItemStack> box=NonNullList.withSize(BOX_SIZE,ItemStack.EMPTY);
            for(int i=0;i<Math.min(BOX_SIZE,items.size());i++)
                box.set(i,items.get(i).copy());
            boxes.put(owner,box);
        });
    }
    private Map<String,List<ItemStack>> serializedBoxes(){
        Map<String,List<ItemStack>> result=new HashMap<>();
        boxes.forEach((key,items)->result.put(key,new ArrayList<>(items)));
        return result;
    }
    public static MailSavedData get(MinecraftServer server){
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }
    public void remember(ServerPlayer player){
        String uuid=player.getUUID().toString(),name=player.getGameProfile().name();
        if(!name.equals(names.put(uuid,name)))setDirty();
    }
    public record Recipient(UUID id,String name){}
    public List<Recipient> recipients(){
        List<Recipient> result=new ArrayList<>();
        names.forEach((id,name)->{
            try{result.add(new Recipient(UUID.fromString(id),name));}
            catch(IllegalArgumentException ignored){}
        });
        result.sort(Comparator.comparing(Recipient::name,String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(result);
    }
    private NonNullList<ItemStack> items(UUID owner){
        return boxes.computeIfAbsent(owner.toString(),ignored->{
            setDirty();
            return NonNullList.withSize(BOX_SIZE,ItemStack.EMPTY);
        });
    }
    public Container ownBox(UUID player){return new BoxContainer(player);}

    /** Commit entire consignment atomically. No input stacks are consumed if
     * the destination does not fit every item, including component-specific
     * stack rules and partial stack merging. Called only on the server thread.
     */
    public boolean deliver(UUID recipient,Container outgoing){
        if(!names.containsKey(recipient.toString()))return false;
        NonNullList<ItemStack> original=items(recipient);
        NonNullList<ItemStack> trial=NonNullList.withSize(BOX_SIZE,ItemStack.EMPTY);
        for(int i=0;i<BOX_SIZE;i++)trial.set(i,original.get(i).copy());
        boolean anything=false;
        for(int slot=0;slot<outgoing.getContainerSize();slot++){
            ItemStack stack=outgoing.getItem(slot).copy();
            if(stack.isEmpty())continue;
            anything=true;
            // Merge only equal item+components into stacks with free capacity.
            for(int i=0;i<BOX_SIZE&&!stack.isEmpty();i++){
                ItemStack present=trial.get(i);
                if(present.isEmpty()||!ItemStack.isSameItemSameComponents(present,stack))
                    continue;
                int move=Math.min(stack.getCount(),present.getMaxStackSize()-present.getCount());
                if(move>0){present.grow(move);stack.shrink(move);}
            }
            for(int i=0;i<BOX_SIZE&&!stack.isEmpty();i++){
                if(!trial.get(i).isEmpty())continue;
                int move=Math.min(stack.getCount(),stack.getMaxStackSize());
                trial.set(i,stack.copyWithCount(move));
                stack.shrink(move);
            }
            if(!stack.isEmpty())return false;
        }
        if(!anything)return false;
        for(int i=0;i<BOX_SIZE;i++)original.set(i,trial.get(i));
        setDirty();
        for(int i=0;i<outgoing.getContainerSize();i++)outgoing.setItem(i,ItemStack.EMPTY);
        return true;
    }

    private final class BoxContainer implements Container {
        private final UUID owner;
        private BoxContainer(UUID owner){this.owner=owner;}
        private NonNullList<ItemStack> content(){return items(owner);}
        @Override public int getContainerSize(){return BOX_SIZE;}
        @Override public boolean isEmpty(){
            for(ItemStack stack:content())if(!stack.isEmpty())return false;
            return true;
        }
        @Override public ItemStack getItem(int slot){
            return slot>=0&&slot<BOX_SIZE?content().get(slot):ItemStack.EMPTY;
        }
        @Override public ItemStack removeItem(int slot,int amount){
            if(slot<0||slot>=BOX_SIZE||amount<=0)return ItemStack.EMPTY;
            ItemStack result=ContainerHelper.removeItem(content(),slot,amount);
            if(!result.isEmpty())setDirty();
            return result;
        }
        @Override public ItemStack removeItemNoUpdate(int slot){
            if(slot<0||slot>=BOX_SIZE)return ItemStack.EMPTY;
            ItemStack result=ContainerHelper.takeItem(content(),slot);
            if(!result.isEmpty())setDirty();
            return result;
        }
        @Override public void setItem(int slot,ItemStack stack){
            if(slot<0||slot>=BOX_SIZE)return;
            content().set(slot,stack.copy());setDirty();
        }
        @Override public void setChanged(){MailSavedData.this.setDirty();}
        @Override public boolean stillValid(Player player){
            return player.getUUID().equals(owner);
        }
        @Override public void clearContent(){
            for(int i=0;i<BOX_SIZE;i++)content().set(i,ItemStack.EMPTY);
            setDirty();
        }
    }
}
