package dev.casz.utils.cosmetics;

import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class CosmeticGuideScreen extends Screen {
 private int index;
 private int category;
 private final java.util.ArrayList<Button> categoryButtons=new java.util.ArrayList<>();
 public CosmeticGuideScreen(){super(Component.literal("Cosmetic Guide"));}
 private List<CosmeticGuideCatalog.Entry> visible(){
  var cat=CosmeticGuideCatalog.CATEGORIES[category];
  return cat.equals("All")?CosmeticGuideCatalog.ENTRIES:CosmeticGuideCatalog.ENTRIES.stream()
      .filter(e->e.category().equals(cat)||e.slots().contains(cat)||e.slots().equals("All slots")||e.slots().equals("Any slot")||e.slots().equals("Full set"))
      .toList();
 }
 private void clamp(){var v=visible();if(v.isEmpty())index=0;else index=Math.floorMod(index,v.size());}
 private void selectCategory(int value){category=value;index=0;for(int i=0;i<categoryButtons.size();i++)categoryButtons.get(i).active=i!=category;}
 @Override protected void init(){
  int l=width/2-130,t=height/2-104;
  addRenderableWidget(Button.builder(Component.literal("<"),b->{index--;clamp();}).bounds(l+12,t+184,22,18).build());
  addRenderableWidget(Button.builder(Component.literal(">"),b->{index++;clamp();}).bounds(l+224,t+184,22,18).build());
  addRenderableWidget(Button.builder(Component.literal("Done"),b->onClose()).bounds(width/2-34,t+184,68,18).build());
  for(int i=0;i<CosmeticGuideCatalog.CATEGORIES.length;i++){
   final int n=i;
   int row=i/4,col=i%4;
   var button=Button.builder(Component.literal(CosmeticGuideCatalog.CATEGORIES[i]),b->selectCategory(n))
       .bounds(l+8+col*61,t+7+row*18,58,16).build();
   categoryButtons.add(button);
   addRenderableWidget(button);
  }
  selectCategory(category);
 }
 private int wrapped(GuiGraphicsExtractor g,String text,int x,int y,int maxWidth,int color){
  var words=text.split(" ");
  var line=new StringBuilder();
  for(var word:words){
   var next=line.isEmpty()?word:line+" "+word;
   if(!line.isEmpty()&&font.width(next)>maxWidth){
    g.text(font,Component.literal(line.toString()),x,y,color,false);
    y+=10;
    line=new StringBuilder(word);
   }else line=new StringBuilder(next);
  }
  if(!line.isEmpty()){
   g.text(font,Component.literal(line.toString()),x,y,color,false);
   y+=10;
  }
  return y;
 }
 private void preview(GuiGraphicsExtractor g,CosmeticGuideCatalog.Entry e,int l,int t,int mx,int my){
  if(minecraft.player==null)return;
  try{
   CosmeticPreviewState.begin(minecraft.player,e.head(),e.chest(),e.legs(),e.feet());
   InventoryScreen.extractEntityInInventoryFollowsMouse(g,l+18,t+58,l+111,t+171,38,.0625f,mx,my,minecraft.player);
  }finally{
   CosmeticPreviewState.end();
  }
 }
 @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float partial){
  int l=width/2-130,t=height/2-104,r=width/2+130,b=height/2+104,fold=width/2;
  g.fill(l,t,r,b,0xff8c6d42);
  g.fill(l+3,t+3,r-3,b-3,0xffefdca8);
  g.fill(fold-2,t+3,fold+2,b-3,0xff9a7b4f);
  var entries=visible();
  if(entries.isEmpty())return;
  clamp();
  var e=entries.get(index);
  g.text(font,Component.literal("Cosmetic Guide"),l+12,t+45,0xff3b2b1c,false);
  g.text(font,Component.literal(CosmeticGuideCatalog.CATEGORIES[category]+"  "+(index+1)+"/"+entries.size()),l+12,t+58,0xff6a5132,false);
  g.text(font,Component.literal("Preview only - nothing is equipped"),l+12,t+71,0xff7a6244,false);
  preview(g,e,l,t+8,mx,my);
  int x=fold+11,y=t+45,w=112;
  g.text(font,Component.literal(e.name()),x,y,0xff2f2116,false);
  y+=14;
  g.text(font,Component.literal("Example: "+e.item().getHoverName().getString()),x,y,0xff4a3925,false);
  y+=12;
  g.text(font,Component.literal("Slots: "+e.slots()),x,y,0xff4a3925,false);
  y+=13;
  g.text(font,Component.literal("Required:"),x,y,0xff6a5132,false);
  y+=11;
  y=wrapped(g,e.required(),x,y,w,0xff4a3925)+3;
  g.text(font,Component.literal("Effect:"),x,y,0xff6a5132,false);
  y+=11;
  wrapped(g,e.description(),x,y,w,0xff4a3925);
 }
}
