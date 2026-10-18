package com.caszgamermd.caszualadditions.xp;

import com.caszgamermd.caszualadditions.CaszualAdditions;
import java.util.HashSet;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Reusable personal XP reservoir. The player must HOLD the collector to
 * absorb loose nearby orbs, so it never steals XP from other players or
 * snatches XP while idle in a chest. Stored XP follows the physical item.
 *
 * Nothing is awarded until the orb was consumed; a partially filled tank
 * skips too-large orbs instead of destroying partial XP. No duplication.
 */
public final class XpCollectorItem extends Item {
    public static final int CAPACITY=30970; // level 0 through 100
    public static final int RADIUS=8;
    public static Item ITEM;
    public XpCollectorItem(Properties properties){super(properties);}
    public static void initialize(){
        var id=CaszualAdditions.id("xp_collector");
        var key=ResourceKey.create(Registries.ITEM,id);
        ITEM=Registry.register(BuiltInRegistries.ITEM,key,
                new XpCollectorItem(new Item.Properties().setId(key).stacksTo(1)));
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(server.getTickCount()%5!=0)return;
            for(var user:server.getPlayerList().getPlayers()){
                if(!(user.level() instanceof ServerLevel level)
                        ||user.isSpectator())continue;
                // One reservoir at a time: never fill the same ItemStack
                // twice simply because it occupies both equipment slots.
                java.util.Set<ItemStack> seen=
                        java.util.Collections.newSetFromMap(
                                new java.util.IdentityHashMap<>());
                for(InteractionHand hand:InteractionHand.values()){
                    ItemStack stack=user.getItemInHand(hand);
                    if(stack.is(ITEM)&&seen.add(stack))
                        collect(level,user,stack);
                }
            }
        });
    }
    public static int stored(ItemStack stack){
        CustomData value=stack.get(DataComponents.CUSTOM_DATA);
        if(value==null)return 0;
        return Math.clamp(value.copyTag().getIntOr("stored_xp",0),0,CAPACITY);
    }
    private static void setStored(ItemStack stack,int amount){
        CustomData existing=stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag=existing==null?new CompoundTag():existing.copyTag();
        tag.putInt("stored_xp",Math.clamp(amount,0,CAPACITY));
        stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
    }
    public static int collect(ServerLevel level,Player owner,ItemStack stack){
        int amount=stored(stack);
        if(amount>=CAPACITY)return 0;
        int collected=0;
        // Check actual distance, not only the 16x16 cube.
        for(ExperienceOrb orb:level.getEntitiesOfClass(ExperienceOrb.class,
                new AABB(owner.blockPosition()).inflate(RADIUS),
                o->o.isAlive()&&o.distanceToSqr(owner)<=RADIUS*RADIUS)){
            int value=orb.getValue();
            if(value<=0||value>CAPACITY-amount)continue;
            amount+=value;
            collected+=value;
            // Remove EXACTLY the orb whose XP value we stored.
            orb.discard();
            if(amount==CAPACITY)break;
        }
        if(collected>0)setStored(stack,amount);
        return collected;
    }
    @Override public InteractionResult use(Level level,Player player,
            InteractionHand hand){
        if(!(level instanceof ServerLevel))return InteractionResult.SUCCESS;
        ItemStack stack=player.getItemInHand(hand);
        int amount=stored(stack);
        if(amount>0){
            // Consume the reservoir FIRST, then award once. No dropped
            // experience orbs / no accidental Mending double-consumption.
            setStored(stack,0);
            player.giveExperiencePoints(amount);
        }
        if(player instanceof net.minecraft.server.level.ServerPlayer user)
            user.connection.send(new net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket(
                    Component.literal("XP Collector: "+(amount>0
                        ?"Transferred "+amount+" XP to you"
                        :"Empty — hold it near XP orbs to collect")
                        +" ("+stored(stack)+"/"+CAPACITY+")")));
        return InteractionResult.SUCCESS;
    }
}
