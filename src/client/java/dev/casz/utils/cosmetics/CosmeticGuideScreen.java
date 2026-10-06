package dev.casz.utils.cosmetics;

import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class CosmeticGuideScreen extends Screen {
 public record Entry(String name,String category,ItemStack item,String slots,String description){}
 private static final List<Entry> ENTRIES=List.of(
  new Entry("End Rod","Sets",new ItemStack(Items.END_ROD),"Head / Legs / Feet","Rod-themed body cosmetics; colored rods work too."),
  new Entry("Bone Skeleton","Sets",new ItemStack(Items.BONE),"All slots","Build a custom 3D skeleton outfit from bones."),
  new Entry("Flower Crown","Head",new ItemStack(Items.POPPY),"Head","Flowers become a crown and can add colored footstep dust."),
  new Entry("Crystal Spines","Head",new ItemStack(Items.AMETHYST_CLUSTER),"Head","Crystal clusters form a Godzilla-like crystal crest."),
  new Entry("Lightning Rod Horn","Head",new ItemStack(Items.IRON_HELMET),"Head","A shorter lightning-rod horn set into the head."),
  new Entry("Blaze","Effects",new ItemStack(Items.BLAZE_POWDER),"Head / Feet","Flame head effect or ember footsteps."),
  new Entry("Nautilus","Sets",new ItemStack(Items.NAUTILUS_SHELL),"All slots","Ocean-floor themed shell cosmetics."),
  new Entry("Echo Soul","Effects",new ItemStack(Items.ECHO_SHARD),"Feet","Sculk-like soul footsteps."),
  new Entry("Slime Trail","Effects",new ItemStack(Items.SLIME_BALL),"Feet","Slime footstep effect."),
  new Entry("Smoke Trail","Effects",new ItemStack(Items.GUNPOWDER),"Feet","Smoke footstep effect."),
  new Entry("Snow Golem","Sets",new ItemStack(Items.CARVED_PUMPKIN),"Full set","Pumpkin head + stick chest + snow block legs and feet.")
 );
 private static final String[] CATEGORIES={"All","Head","Chest","Legs","Feet","Sets","Effects"};
 private int index;
 private int category;
 public CosmeticGuideScreen(){super(Component.literal("Cosmetic Guide"));}
 private List<Entry> visible(){var cat=CATEGORIES[category];return cat.equals("All")?ENTRIES:ENTRIES.stream().filter(e->e.category().equals(cat)||e.slots().contains(cat)).toList();}
 private void clamp(){var v=visible();if(v.isEmpty())index=0;else index=Math.floorMod(index,v.size());}
 @Override protected void init(){
  addRenderableWidget(Button.builder(Component.literal("<"),b->{index--;clamp();}).bounds(width/2-92,height/2+70,20,20).build());
  addRenderableWidget(Button.builder(Component.literal(">"),b->{index++;clamp();}).bounds(width/2+72,height/2+70,20,20).build());
  addRenderableWidget(Button.builder(Component.literal("Done"),b->onClose()).bounds(width/2-35,height/2+70,70,20).build());
  for(int i=0;i<CATEGORIES.length;i++){final int n=i;addRenderableWidget(Button.builder(Component.literal(CATEGORIES[i]),b->{category=n;index=0;}).bounds(width/2-108+i*31,height/2-91,30,16).build());}
 }
 @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float partial){
  int l=width/2-110,t=height/2-95,r=width/2+110,b=height/2+95;
  g.fill(l,t,r,b,0xffd7c08a);g.fill(l+4,t+4,r-4,b-4,0xfff1dfaa);g.fill(width/2-2,t+4,width/2+2,b-4,0xff9a7b4f);
  var entries=visible();if(entries.isEmpty())return;clamp();var e=entries.get(index);
  g.text(font,Component.literal("Cosmetic Guide"),l+12,t+10,0xff3b2b1c,false);
  g.text(font,Component.literal(CATEGORIES[category]+"  •  "+(index+1)+"/"+entries.size()),l+12,t+25,0xff6a5132,false);
  g.text(font,Component.literal(e.name()),width/2+12,t+14,0xff3b2b1c,false);
  g.text(font,Component.literal("Item: "+e.item().getHoverName().getString()),width/2+12,t+32,0xff4a3925,false);
  g.text(font,Component.literal("Slots: "+e.slots()),width/2+12,t+46,0xff4a3925,false);
  g.text(font,Component.literal(e.description()),width/2+12,t+66,0xff4a3925,false);
  if(minecraft.player!=null)InventoryScreen.extractEntityInInventoryFollowsMouse(g,l+18,t+42,width/2-14,t+150,32,.0625f,mx,my,minecraft.player);
 }
}
