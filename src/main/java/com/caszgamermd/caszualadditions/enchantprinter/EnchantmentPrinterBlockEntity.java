package com.caszgamermd.caszualadditions.enchantprinter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Enchantment library is a finite collection of refilled printing patterns.
 * A print charges all costs up front, applies several compatible enchantments
 * to one real input item, and then independently tests each pattern for a jam.
 * Jams last 2 minutes; feeding a book/tool containing that enchant also clears
 * its jam, drops its strain to zero and may improve the stored level.
 *
 * Reuse the selected set as a persistent preset. Enchanted input items are
 * consumed ONLY via the explicit FEED action, never by preview or printing.
 */
public final class EnchantmentPrinterBlockEntity extends BlockEntity
        implements Container,ExtendedMenuProvider<BlockPos> {
    public static final int FEED=0,TARGET=1,LAPIS=2,EMERALD=3,SLOTS=4,MAX_LIBRARY=128;
    public static final long JAM_TICKS=2400;
    private static final int MAX_RISK=85;
    private NonNullList<ItemStack> items=NonNullList.withSize(SLOTS,ItemStack.EMPTY);
    private final Map<String,Pattern> library=new LinkedHashMap<>();
    private final Set<String> selected=new LinkedHashSet<>();
    private String message="Feed an enchanted item or book to learn its enchantments.";
    private record Pattern(int level,int strain,long blockedUntil){}

    public EnchantmentPrinterBlockEntity(BlockPos pos,BlockState state){
        super(EnchantmentPrinterContent.ENTITY,pos,state);
    }
    public static int risk(int strain){return Math.min(MAX_RISK,4+Math.max(0,strain)*7);}
    private static String id(Holder<Enchantment> enchant){
        return enchant.unwrapKey().map(key->key.identifier().toString()).orElse("");
    }
    private @Nullable Holder<Enchantment> resolve(ServerLevel world,String id){
        try{
            return world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(
                ResourceKey.create(Registries.ENCHANTMENT,Identifier.parse(id))).orElse(null);
        }catch(RuntimeException e){return null;}
    }
    public List<String> keys(){return library.keySet().stream().sorted().toList();}
    public String snapshot(ServerLevel world,int page){
        long now=world.getGameTime();
        List<String> keys=keys();
        int maxPage=Math.max(0,(keys.size()-1)/EnchantmentPrinterMenu.PAGE_SIZE);
        int bounded=Math.clamp(page,0,maxPage);
        StringBuilder result=new StringBuilder("STATE\t"+keys.size()+"\t"+bounded
            +"\t"+selected.size()+"\t"+(message.replace('\n',' ').replace('\t',' ')));
        int xp=0,lapis=0,emerald=0;
        for(String key:selected){
            Pattern p=library.get(key);
            if(p==null)continue;
            xp+=xpCost(p.level());lapis+=lapisCost(p.level());emerald+=emeraldCost(p.level());
        }
        result.append("\nCOST\t").append(xp).append("\t").append(lapis).append("\t").append(emerald);
        for(int i=bounded*EnchantmentPrinterMenu.PAGE_SIZE;
                i<Math.min(keys.size(),(bounded+1)*EnchantmentPrinterMenu.PAGE_SIZE);i++){
            String key=keys.get(i);
            Pattern p=library.get(key);
            Holder<Enchantment> enchant=resolve(world,key);
            String name=enchant==null?key:enchant.value().description().getString();
            name=name.replace('\n',' ').replace('\t',' ');
            long remaining=Math.max(0,p.blockedUntil()-now);
            result.append("\nROW\t").append(key).append("\t").append(name)
                .append("\t").append(p.level()).append("\t")
                .append(risk(p.strain())).append("\t").append(remaining/20)
                .append("\t").append(selected.contains(key)?"1":"0");
        }
        return result.toString();
    }
    public static int xpCost(int level){return 3+2*level;}
    public static int lapisCost(int level){return Math.max(1,level);}
    public static int emeraldCost(int level){return Math.max(1,(level+1)/2);}
    public Component feed(ServerLevel world){
        ItemStack donor=getItem(FEED);
        if(donor.isEmpty())return result("Put an enchanted item or enchanted book in FEED.");
        ItemEnchantments enchantments=EnchantmentHelper.getEnchantmentsForCrafting(donor);
        if(enchantments.isEmpty())return result("The feeder item has no enchantments.");
        Map<String,Integer> learned=new LinkedHashMap<>();
        for(var entry:enchantments.entrySet()){
            String id=id(entry.getKey());
            if(!id.isBlank())learned.merge(id,entry.getIntValue(),Math::max);
        }
        if(learned.isEmpty())return result("No registered enchantments on donor.");
        for(String key:learned.keySet())
            if(!library.containsKey(key)&&library.size()>=MAX_LIBRARY)
                return result("Printer library full (128 enchantments).");
        long now=world.getGameTime();
        for(var entry:learned.entrySet()){
            Pattern old=library.get(entry.getKey());
            int level=Math.max(entry.getValue(),old==null?0:old.level());
            library.put(entry.getKey(),new Pattern(level,0,0));
        }
        donor.shrink(1);
        if(donor.isEmpty())items.set(FEED,ItemStack.EMPTY);
        setChanged();
        return result("Learned/refilled "+learned.size()+" enchantment(s). Donor consumed.");
    }
    public void toggle(ServerLevel world,String key){
        if(!library.containsKey(key))return;
        if(!selected.add(key))selected.remove(key);
        setChanged();
    }
    public void selectLast(){/* selected is saved permanently, including between jobs */ }
    public void clearSelection(){selected.clear();setChanged();}
    private Component result(String value){message=value;setChanged();return Component.literal(value);}

    public Component print(ServerLevel world,Player player){
        if(selected.isEmpty())return result("Select at least one enchantment.");
        ItemStack target=getItem(TARGET);
        if(target.isEmpty()||target.getCount()!=1||!EnchantmentHelper.canStoreEnchantments(target))
            return result("Insert one enchantable item in TARGET.");
        long now=world.getGameTime();
        List<Map.Entry<Holder<Enchantment>,Integer>> changes=new ArrayList<>();
        List<String> names=new ArrayList<>();
        List<Holder<Enchantment>> existing=new ArrayList<>(
            EnchantmentHelper.getEnchantmentsForCrafting(target).keySet());
        int xp=0,lapis=0,emerald=0;
        for(String key:selected){
            Pattern p=library.get(key);
            Holder<Enchantment> enchant=resolve(world,key);
            if(p==null||enchant==null)return result("A selected enchantment is unavailable.");
            if(p.blockedUntil()>now)return result("A selected pattern is temporarily depleted. Refill or wait.");
            if(!enchant.value().isSupportedItem(target))
                return result("Target does not support "+enchant.value().description().getString()+".");
            int current=EnchantmentHelper.getEnchantmentsForCrafting(target).getLevel(enchant);
            if(current>=p.level())return result("Target already has "+key+" at this level or higher.");
            if(!EnchantmentHelper.isEnchantmentCompatible(existing,enchant))
                return result("Incompatible enchantment: "+enchant.value().description().getString()+".");
            existing.add(enchant);
            changes.add(Map.entry(enchant,p.level()));
            names.add(key);
            xp+=xpCost(p.level());lapis+=lapisCost(p.level());emerald+=emeraldCost(p.level());
        }
        if(getItem(LAPIS).getCount()<lapis||getItem(EMERALD).getCount()<emerald)
            return result("Need "+lapis+" lapis and "+emerald+" emeralds in the supply slots.");
        if(!player.getAbilities().instabuild&&player.experienceLevel<xp)
            return result("Need "+xp+" XP levels for this print.");
        // All validation is completed before any resource/item changes.
        ItemStack output=target.copy();
        EnchantmentHelper.updateEnchantments(output,mutable->{
            for(var change:changes)mutable.set(change.getKey(),change.getValue());
        });
        if(!player.getAbilities().instabuild)player.giveExperienceLevels(-xp);
        getItem(LAPIS).shrink(lapis);
        getItem(EMERALD).shrink(emerald);
        items.set(TARGET,output);
        int jammed=0;
        for(String key:names){
            Pattern p=library.get(key);
            int percent=risk(p.strain());
            boolean jam=world.getRandom().nextInt(100)<percent;
            library.put(key,new Pattern(p.level(),jam?0:p.strain()+1,
                jam?now+JAM_TICKS:0));
            if(jam)jammed++;
        }
        setChanged();
        return result("Printed "+names.size()+" enchantment(s)! "+jammed+
            " pattern(s) temporarily depleted. Selection saved for repeat printing.");
    }
    public void refreshCooldown(ServerLevel world){
        boolean changed=false;long now=world.getGameTime();
        for(var e:new ArrayList<>(library.entrySet())){
            Pattern p=e.getValue();
            if(p.blockedUntil()!=0&&p.blockedUntil()<=now){
                library.put(e.getKey(),new Pattern(p.level(),0,0));
                changed=true;
            }
        }
        if(changed)setChanged();
    }
    @Override public int getContainerSize(){return SLOTS;}
    @Override public boolean isEmpty(){return items.stream().allMatch(ItemStack::isEmpty);}
    @Override public ItemStack getItem(int slot){return slot>=0&&slot<SLOTS?items.get(slot):ItemStack.EMPTY;}
    @Override public ItemStack removeItem(int slot,int count){
        ItemStack v=ContainerHelper.removeItem(items,slot,count);
        if(!v.isEmpty())setChanged();
        return v;
    }
    @Override public ItemStack removeItemNoUpdate(int slot){
        ItemStack v=ContainerHelper.takeItem(items,slot);
        if(!v.isEmpty())setChanged();
        return v;
    }
    @Override public void setItem(int slot,ItemStack stack){
        if(slot<0||slot>=SLOTS)return;
        items.set(slot,stack);
        if(stack.getCount()>getMaxStackSize(stack))stack.setCount(getMaxStackSize(stack));
        setChanged();
    }
    @Override public boolean canPlaceItem(int slot,ItemStack stack){
        return switch(slot){
            case FEED -> !EnchantmentHelper.getEnchantmentsForCrafting(stack).isEmpty();
            case TARGET -> EnchantmentHelper.canStoreEnchantments(stack) && stack.getCount()==1;
            case LAPIS -> stack.is(Items.LAPIS_LAZULI);
            case EMERALD -> stack.is(Items.EMERALD);
            default -> false;
        };
    }
    @Override public int getMaxStackSize(ItemStack stack){return 64;}
    @Override public void clearContent(){items.clear();items=NonNullList.withSize(SLOTS,ItemStack.EMPTY);setChanged();}
    @Override public boolean stillValid(Player player){return Container.stillValidBlockEntity(this,player,8.0f);}
    @Override protected void saveAdditional(ValueOutput out){
        super.saveAdditional(out);
        ContainerHelper.saveAllItems(out,items);
        out.putInt("PrinterLibraryCount",library.size());
        int i=0;
        for(var e:library.entrySet()){
            out.putString("PrinterId"+i,e.getKey());
            out.putInt("PrinterLevel"+i,e.getValue().level());
            out.putInt("PrinterStrain"+i,e.getValue().strain());
            out.putLong("PrinterBlockedUntil"+i,e.getValue().blockedUntil());
            i++;
        }
        out.putInt("PrinterSelectedCount",selected.size());
        i=0;for(String id:selected)out.putString("PrinterSelected"+i++,id);
    }
    @Override protected void loadAdditional(ValueInput in){
        super.loadAdditional(in);
        items=NonNullList.withSize(SLOTS,ItemStack.EMPTY);
        ContainerHelper.loadAllItems(in,items);
        library.clear();selected.clear();
        int n=Math.clamp(in.getIntOr("PrinterLibraryCount",0),0,MAX_LIBRARY);
        for(int i=0;i<n;i++){
            String id=in.getStringOr("PrinterId"+i,"");
            if(id.length()>128||id.isBlank())continue;
            library.put(id,new Pattern(Math.clamp(in.getIntOr("PrinterLevel"+i,1),1,255),
                Math.clamp(in.getIntOr("PrinterStrain"+i,0),0,64),
                Math.max(0,in.getLongOr("PrinterBlockedUntil"+i,0))));
        }
        n=Math.clamp(in.getIntOr("PrinterSelectedCount",0),0,MAX_LIBRARY);
        for(int i=0;i<n;i++){
            String key=in.getStringOr("PrinterSelected"+i,"");
            if(library.containsKey(key))selected.add(key);
        }
    }
    @Override public Component getDisplayName(){return Component.literal("Enchantment Printer");}
    @Override public @Nullable AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){
        return new EnchantmentPrinterMenu(id,inventory,this);
    }
    @Override public BlockPos getScreenOpeningData(ServerPlayer player){return getBlockPos();}
}
