package dev.casz.utils.cosmetics;
import java.util.Map;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.FlowerBlock;

public final class FlowerTrail {
 private static final Map<String,Integer> COLORS=Map.ofEntries(
  Map.entry("dandelion",0xF2C94C),Map.entry("poppy",0xE53935),Map.entry("blue_orchid",0x4FC3F7),
  Map.entry("allium",0xB56BE3),Map.entry("azure_bluet",0xE8E8E0),Map.entry("red_tulip",0xE53935),
  Map.entry("orange_tulip",0xF28C28),Map.entry("white_tulip",0xF4F4F4),Map.entry("pink_tulip",0xF48FB1),
  Map.entry("oxeye_daisy",0xF5F5E8),Map.entry("cornflower",0x4169E1),Map.entry("lily_of_the_valley",0xF4F4F4),
  Map.entry("wither_rose",0x3A3038),Map.entry("torchflower",0xF57C2B),Map.entry("open_eyeblossom",0xFFB02E),
  Map.entry("closed_eyeblossom",0x8B6F47),Map.entry("pink_petals",0xF29AB2),Map.entry("spore_blossom",0xD95F8D)
 );
 private FlowerTrail(){}
 public static void initialize(){ServerTickEvents.END_SERVER_TICK.register(server->{for(var level:server.getAllLevels())for(var p:level.players()){if(p.tickCount%3!=0||p.getDeltaMovement().horizontalDistanceSqr()<0.0025)continue;var feet=Cosmetics.get(p,EquipmentSlot.FEET);int rgb=color(feet);if(rgb<0)continue;level.sendParticles(new DustParticleOptions(rgb,.8f),p.getX(),p.getY()+.08,p.getZ(),2,.18,.04,.18,.005);}});}
 private static int color(ItemStack s){if(!FlowerCosmetics.isFlower(s))return -1;String path=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();return COLORS.getOrDefault(path,0x7ED957);}
}
