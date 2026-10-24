package com.caszgamermd.caszualadditions.racks;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Warehouse Scanner LCD: seven buttons and the context-specific controls.
 * All clicks are sent to the server; no client-side ItemStack NBT editing. */
public final class ScannerScreen extends Screen {
    private final JsonObject settings;
    private final String mode;
    private int x,y,w,h;
    public ScannerScreen(String json){
        super(Component.literal("WAREHOUSE SCANNER"));
        this.settings=JsonParser.parseString(json).getAsJsonObject();
        this.mode=text("mode","REMOTE");
    }
    private String text(String key,String fallback){
        return settings.has(key)?settings.get(key).getAsString():fallback;
    }
    private boolean flag(String key,boolean fallback){
        return settings.has(key)?settings.get(key).getAsBoolean():fallback;
    }
    private void send(String key,String value){
        ClientPlayNetworking.send(new ScannerControlNet.SetOption(key,value));
    }
    private void choice(String key,String value,String label,int gx,int gy,int gw){
        boolean selected=value.equals(text(key,""));
        addRenderableWidget(Button.builder(Component.literal((selected?"● ":"○ ")+label),
            b->send(key,value)).bounds(gx,gy,gw,20).build());
    }
    @Override protected void init(){
        w=Math.min(460,Math.max(280,width-24));
        h=Math.min(370,Math.max(226,height-20));
        x=(width-w)/2;y=(height-h)/2;
        int margin=18,interior=w-margin*2;
        String[] modes={"DIRECT","REMOTE","FORKLIFT","RECALL","LINK","LOAD_UNLOAD","COMPUTER"};
        int bw=(interior-7)/2;
        for(int i=0;i<modes.length;i++){
            final String m=modes[i];
            String label=m.equals("LOAD_UNLOAD")?"LOAD / UNLOAD":m;
            choice("mode",m,label,x+margin+(i%2)*(bw+7),y+64+(i/2)*23,bw);
        }
        int oy=y+164;
        if(mode.equals("REMOTE")){
            addRenderableWidget(Button.builder(Component.literal(
                "Replacement pallet: "+(flag("remoteReplace",true)?"YES":"NO")),
                b->send("remoteReplace",Boolean.toString(!flag("remoteReplace",true))))
                .bounds(x+margin,oy,interior,20).build());
        }else if(mode.equals("FORKLIFT")){
            addRenderableWidget(Button.builder(Component.literal(
                "Follow between tasks: "+(flag("forkliftFollow",false)?"ON":"OFF")),
                b->send("forkliftFollow",Boolean.toString(!flag("forkliftFollow",false))))
                .bounds(x+margin,oy,interior,20).build());
        }else if(mode.equals("RECALL")){
            String[] keys={"GROUND","NEAREST_BAY","HOME_BAY","FOLLOW"};
            String[] labels={"Ground below","Nearest bay","Home bay","Follow player"};
            for(int i=0;i<4;i++)
                choice("recallTarget",keys[i],labels[i],
                    x+margin+(i%2)*(bw+7),oy+(i/2)*23,bw);
            addRenderableWidget(Button.builder(Component.literal(
                "Release pallet safely: "+(flag("recallRelease",false)?"YES":"NO")),
                b->send("recallRelease",Boolean.toString(!flag("recallRelease",false))))
                .bounds(x+margin,oy+47,interior,20).build());
        }else if(mode.equals("LINK")){
            choice("linkType","AIR_ROUTE","Node to node",x+margin,oy,bw);
            choice("linkType","WAREHOUSE","Warehouse to node",x+margin+bw+7,oy,bw);
        }else if(mode.equals("LOAD_UNLOAD")){
            choice("loadDirection","LOAD","Push onto transport",x+margin,oy,bw);
            choice("loadDirection","UNLOAD","Pull from transport",x+margin+bw+7,oy,bw);
        }
        addRenderableWidget(Button.builder(Component.literal("DONE"),b->onClose())
            .bounds(x+w-90,y+h-28,72,20).build());
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,
            int mouseX,int mouseY,float partialTick){
        super.extractBackground(g,mouseX,mouseY,partialTick);
        // Handheld scanner: thick graphite housing around a luminous LCD.
        g.fill(x-4,y-4,x+w+4,y+h+4,0xff111817);
        g.fill(x,y,x+w,y+h,0xff242e2b);
        g.fill(x+8,y+8,x+w-8,y+h-9,0xff0c2422);
        g.fill(x+12,y+11,x+w-12,y+57,0xff163934);
        g.text(font,Component.literal("CASZUAL  /  WAREHOUSE SCANNER"),
                x+17,y+14,0xff6dffb1,false);
        g.text(font,Component.literal("LINK: "+text("name","UNLINKED")),
                x+17,y+29,0xff54bccc,false);
        g.text(font,Component.literal("COORDS: "+text("coords","---")),
                x+17,y+43,0xffc79408,false);
        g.text(font,Component.literal("CURRENT MODE: "+mode),
                x+18,y+154,0xff0c8c06,false);
        String info=switch(mode){
            case "DIRECT"->"Click a shelf, pallet or placement face.";
            case "REMOTE"->"Target a wrapped pallet. Queue stays server-owned.";
            case "FORKLIFT"->"Select a source, then a rack or ground target.";
            case "RECALL"->"Recall drones within 40 blocks, or Golem teams.";
            case "LINK"->"Click warehouse/node, then destination node.";
            case "LOAD_UNLOAD"->"Two following Golems transfer a real pallet.";
            default->"Right-click to open the linked Warehouse Computer.";
        };
        g.text(font,Component.literal(info),
                x+18,y+h-47,0xff9ad7cc,false);
        g.text(font,Component.literal("MODE & SETTINGS SAVED ON THIS SCANNER"),
                x+18,y+h-17,0xff75968b,false);
    }
}
