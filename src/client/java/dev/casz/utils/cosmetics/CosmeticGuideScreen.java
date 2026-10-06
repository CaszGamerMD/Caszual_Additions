package dev.casz.utils.cosmetics;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class CosmeticGuideScreen extends Screen {
 public record Entry(String name,String category,ItemStack item,String slots,String required,String description,
                     ItemStack head,ItemStack chest,ItemStack legs,ItemStack feet){}
 private static ItemStack s(net.minecraft.world.level.ItemLike item){return new ItemStack(item);}
 private static ItemStack lightningRod(){return new ItemStack(Items.LIGHTNING_ROD.asList().get(0));}
 private static Entry one(String name,String category,ItemStack item,String slots,String required,String description,
                          ItemStack head,ItemStack chest,ItemStack legs,ItemStack feet){
  return new Entry(name,category,item,slots,required,description,head,chest,legs,feet);
 }
 private static final ItemStack EMPTY=ItemStack.EMPTY;
 private static final List<Entry> ENTRIES=List.of(
  one("End Rod","Sets",s(Items.END_ROD),"Head / Legs / Feet","End Rod or any Caszual colored End Rod",
      "End rods become fitted body cosmetics. Colored rods keep their own color and RGB rods keep their animated look.",
      s(Items.END_ROD),EMPTY,s(Items.END_ROD),s(Items.END_ROD)),
  one("Bone Skeleton","Sets",s(Items.BONE),"All slots","Bone in any cosmetic slot",
      "Bones form a custom 3D skeleton outfit. Use one piece or fill all four cosmetic slots for the complete skeleton.",
      s(Items.BONE),s(Items.BONE),s(Items.BONE),s(Items.BONE)),
  one("Flower Crown","Head",s(Items.POPPY),"Head","Any block tagged as a flower",
      "A flower worn in the head cosmetic slot becomes a crown. Different flowers keep their own appearance.",
      s(Items.POPPY),EMPTY,EMPTY,EMPTY),
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
  one("Block Cosmetics","Sets",s(Items.DIAMOND_BLOCK),"Any slot","Any block item",
      "Most block items can be placed directly into cosmetic slots for appearance-only block outfits.",
      s(Items.DIAMOND_BLOCK),s(Items.DIAMOND_BLOCK),s(Items.DIAMOND_BLOCK),s(Items.DIAMOND_BLOCK))
 );
 private static final String[] CATEGORIES={"All","Head","Chest","Legs","Feet","Sets","Effects"};
 private int index;
 private int category;
 public CosmeticGuideScreen(){super(Component.literal("Cosmetic Guide"));}
 private List<Entry> visible(){
  var cat=CATEGORIES[category];
  return cat.equals("All")?ENTRIES:ENTRIES.stream().filter(e->e.category().equals(cat)||e.slots().contains(cat)||e.slots().equals("All slots")||e.slots().equals("Any slot")||e.slots().equals("Full set")).toList();
 }
 private void clamp(){var v=visible();if(v.isEmpty())index=0;else index=Math.floorMod(index,v.size());}
 @Override protected void init(){
  int l=width/2-130,t=height/2-104;
  addRenderableWidget(Button.builder(Component.literal("<"),b->{index--;clamp();}).bounds(l+12,t+184,22,18).build());
  addRenderableWidget(Button.builder(Component.literal(">"),b->{index++;clamp();}).bounds(l+224,t+184,22,18).build());
  addRenderableWidget(Button.builder(Component.literal("Done"),b->onClose()).bounds(width/2-34,t+184,68,18).build());
  for(int i=0;i<CATEGORIES.length;i++){
   final int n=i;
   int row=i/4,col=i%4;
   addRenderableWidget(Button.builder(Component.literal(CATEGORIES[i]),b->{category=n;index=0;}).bounds(l+8+col*61,t+7+row*18,58,16).build());
  }
 }
 private int wrapped(GuiGraphicsExtractor g,String text,int x,int y,int maxWidth,int color){
  var words=text.split(" ");
  var line=new StringBuilder();
  for(var word:words){
   var next=line.isEmpty()?word:line+" "+word;
   if(!line.isEmpty()&&font.width(next)>maxWidth){
    g.text(font,Component.literal(line.toString()),x,y,color,false);y+=10;line=new StringBuilder(word);
   }else{line=new StringBuilder(next);}
  }
  if(!line.isEmpty()){g.text(font,Component.literal(line.toString()),x,y,color,false);y+=10;}
  return y;
 }
 private void preview(GuiGraphicsExtractor g,Entry e,int l,int t,int mx,int my){
  if(minecraft.player==null)return;
  ItemStack[] before=new ItemStack[4];
  for(int i=0;i<4;i++)before[i]=Cosmetics.get(minecraft.player,Cosmetics.SLOTS[i]);
  try{
   ItemStack[] show={e.head(),e.chest(),e.legs(),e.feet()};
   for(int i=0;i<4;i++)Cosmetics.set(minecraft.player,Cosmetics.SLOTS[i],show[i]);
   InventoryScreen.extractEntityInInventoryFollowsMouse(g,l+18,t+58,l+111,t+171,38,.0625f,mx,my,minecraft.player);
  }finally{
   for(int i=0;i<4;i++)Cosmetics.set(minecraft.player,Cosmetics.SLOTS[i],before[i]);
  }
 }
 @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float partial){
  int l=width/2-130,t=height/2-104,r=width/2+130,b=height/2+104,fold=width/2;
  g.fill(l,t,r,b,0xff8c6d42);g.fill(l+3,t+3,r-3,b-3,0xffefdca8);g.fill(fold-2,t+3,fold+2,b-3,0xff9a7b4f);
  var entries=visible();
  if(entries.isEmpty())return;
  clamp();
  var e=entries.get(index);
  g.text(font,Component.literal("Cosmetic Guide"),l+12,t+45,0xff3b2b1c,false);
  g.text(font,Component.literal(CATEGORIES[category]+"  "+(index+1)+"/"+entries.size()),l+12,t+58,0xff6a5132,false);
  preview(g,e,l,t,mx,my);
  int x=fold+11,y=t+45,w=112;
  g.text(font,Component.literal(e.name()),x,y,0xff2f2116,false);y+=15;
  g.text(font,Component.literal("Slots: "+e.slots()),x,y,0xff4a3925,false);y+=14;
  g.text(font,Component.literal("Required:"),x,y,0xff6a5132,false);y+=11;
  y=wrapped(g,e.required(),x,y,w,0xff4a3925)+3;
  g.text(font,Component.literal("Effect:"),x,y,0xff6a5132,false);y+=11;
  wrapped(g,e.description(),x,y,w,0xff4a3925);
 }
}
