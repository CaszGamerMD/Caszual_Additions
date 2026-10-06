package dev.casz.utils.cosmetics;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class CosmeticGuideCatalog {
 public record Entry(String name,String category,ItemStack item,String slots,String required,String description,
                     ItemStack head,ItemStack chest,ItemStack legs,ItemStack feet){}
 private static ItemStack s(net.minecraft.world.level.ItemLike item){return new ItemStack(item);}
 private static ItemStack lightningRod(){return new ItemStack(Items.LIGHTNING_ROD.asList().get(0));}
 private static Entry one(String name,String category,ItemStack item,String slots,String required,String description,
                          ItemStack head,ItemStack chest,ItemStack legs,ItemStack feet){
  return new Entry(name,category,item,slots,required,description,head,chest,legs,feet);
 }
 private static final ItemStack EMPTY=ItemStack.EMPTY;
 public static final String[] CATEGORIES={"All","Head","Chest","Legs","Feet","Sets","Effects"};
 public static final List<Entry> ENTRIES=List.of(
  one("End Rod","Sets",s(Items.END_ROD),"Head / Legs / Feet","End Rod or any Caszual colored End Rod",
      "End rods become fitted body cosmetics. Colored rods keep their own color and RGB rods keep their animated look.",
      s(Items.END_ROD),EMPTY,s(Items.END_ROD),s(Items.END_ROD)),
  one("Bone Skeleton","Sets",s(Items.BONE),"All slots","Bone in any cosmetic slot",
      "Bones form a custom 3D skeleton outfit. Use one piece or fill all four cosmetic slots for the complete skeleton.",
      s(Items.BONE),s(Items.BONE),s(Items.BONE),s(Items.BONE)),
  one("Flower Crown","Head",s(Items.POPPY),"Head","Any block tagged as a flower",
      "A flower worn in the head cosmetic slot becomes a crown. Different flowers keep their own appearance.",
      s(Items.POPPY),EMPTY,EMPTY,EMPTY),
  one("Flower Trail","Effects",s(Items.POPPY),"Feet","Any block tagged as a flower",
      "A flower in the feet slot creates colored dust footsteps based on the flower while you move.",
      EMPTY,EMPTY,EMPTY,s(Items.POPPY)),
  one("Crystal Spines","Head",s(Items.AMETHYST_CLUSTER),"Head","Amethyst or compatible crystal cluster",
      "Crystal clusters form a Godzilla-like crest running back from the head. Compatible modded crystal clusters are supported.",
      s(Items.AMETHYST_CLUSTER),EMPTY,EMPTY,EMPTY),
  one("Lightning Rod Horn","Head",lightningRod(),"Head","Any lightning rod variant",
      "A lightning rod becomes a short horn set into the head instead of floating above it.",
      lightningRod(),EMPTY,EMPTY,EMPTY),
  one("Blaze Crown","Head",s(Items.BLAZE_POWDER),"Head","Blaze Powder",
      "Blaze Powder in the head slot creates a fiery head effect.",
      s(Items.BLAZE_POWDER),EMPTY,EMPTY,EMPTY),
  one("Ember Steps","Effects",s(Items.BLAZE_POWDER),"Feet","Blaze Powder",
      "Blaze Powder in the feet slot leaves a hot ember-style trail as you move.",
      EMPTY,EMPTY,EMPTY,s(Items.BLAZE_POWDER)),
  one("Nautilus Set","Sets",s(Items.NAUTILUS_SHELL),"All slots","Nautilus Shell in one or more cosmetic slots",
      "Nautilus shells build an ocean-floor themed outfit. Fill more slots for a fuller shell-covered look.",
      s(Items.NAUTILUS_SHELL),s(Items.NAUTILUS_SHELL),s(Items.NAUTILUS_SHELL),s(Items.NAUTILUS_SHELL)),
  one("Echo Soul","Effects",s(Items.ECHO_SHARD),"Feet","Echo Shard",
      "Echo Shards in the feet slot create a sculk-soul themed footstep effect.",
      EMPTY,EMPTY,EMPTY,s(Items.ECHO_SHARD)),
  one("Slime Trail","Effects",s(Items.SLIME_BALL),"Feet","Slime Ball",
      "Slime Balls in the feet slot create a slime footstep effect.",
      EMPTY,EMPTY,EMPTY,s(Items.SLIME_BALL)),
  one("Smoke Trail","Effects",s(Items.GUNPOWDER),"Feet","Gunpowder",
      "Gunpowder in the feet slot creates a drifting smoke footstep effect.",
      EMPTY,EMPTY,EMPTY,s(Items.GUNPOWDER)),
  one("Snow Golem","Sets",s(Items.CARVED_PUMPKIN),"Full set","Head: Carved Pumpkin  Chest: Stick  Legs + Feet: Snow Blocks",
      "This exact four-piece combination transforms the cosmetic model into a controllable snow golem appearance.",
      s(Items.CARVED_PUMPKIN),s(Items.STICK),s(Items.SNOW_BLOCK),s(Items.SNOW_BLOCK)),
  one("Mob Heads","Head",s(Items.ZOMBIE_HEAD),"Head","Any mob head",
      "Mob heads replace the visible player head so custom head models can take over cleanly.",
      s(Items.ZOMBIE_HEAD),EMPTY,EMPTY,EMPTY),
  one("Player Heads","Head",s(Items.PLAYER_HEAD),"Head","Player Head",
      "Player heads use their stored profile and replace the visible player head cleanly.",
      s(Items.PLAYER_HEAD),EMPTY,EMPTY,EMPTY),
  one("Armor Cosmetics","Sets",s(Items.DIAMOND_CHESTPLATE),"Matching armor slot","Any equippable armor piece",
      "Armor placed in cosmetic slots is appearance-only, so your visible outfit can be different from the armor providing stats.",
      s(Items.DIAMOND_HELMET),s(Items.DIAMOND_CHESTPLATE),s(Items.DIAMOND_LEGGINGS),s(Items.DIAMOND_BOOTS)),
  one("Block Cosmetics","Sets",s(Items.DIAMOND_BLOCK),"Any slot","Any block item",
      "Most block items can be placed directly into cosmetic slots for appearance-only block outfits.",
      s(Items.DIAMOND_BLOCK),s(Items.DIAMOND_BLOCK),s(Items.DIAMOND_BLOCK),s(Items.DIAMOND_BLOCK))
 );
 private CosmeticGuideCatalog(){}
}
