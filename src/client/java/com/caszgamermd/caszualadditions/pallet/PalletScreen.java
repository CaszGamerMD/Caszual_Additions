package com.caszgamermd.caszualadditions.pallet;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Warehouse-style pallet inventory, with an explicit distinction between
 * storage shelves, page navigation and the player's own inventory.
 *
 * Slot positions MUST match PalletMenu. The buttons are ordinary GUI widgets,
 * not hand-drawn hotspots, so their text, focus and mouse input stay aligned.
 */
public final class PalletScreen extends AbstractContainerScreen<PalletMenu> {
    private static final int IMAGE_W = 554;
    private static final int IMAGE_H = 342;
    private static final int STORAGE_X = 23;
    private static final int STORAGE_Y = 51;
    private static final int PLAYER_X = 104;
    private static final int PLAYER_Y = 253;

    // Neutral industrial styling, coordinated with Minecraft's own slot colors.
    private static final int OUTER = 0xff111923;
    private static final int HEADER = 0xff253341;
    private static final int FRAME = 0xff344454;
    private static final int CONTENT = 0xffd9e0e6;
    private static final int PANEL = 0xffc3cdd6;
    private static final int SLOT_EDGE = 0xff697781;
    private static final int SLOT_FILL = 0xffe5eaf0;
    private static final int TEXT = 0xff1c2c3a;
    private static final int MUTED = 0xff546473;
    private static final int ACCENT = 0xff3d9c9e;

    private Button previous,next,modeButton,scopeButton,sampleButton,applyButton,ptpButton,clearFilter;
    private boolean filterOpen; // true only while the filter module is installed
    private String ghostPreview="";
    private EditBox targetField;
    private String lastRule = "";

    public PalletScreen(PalletMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, IMAGE_W, IMAGE_H);
        // Labels rendered explicitly; no duplicated inventory text.
        this.titleLabelX = 16;
        this.titleLabelY = 16;
        this.inventoryLabelX = PLAYER_X;
        this.inventoryLabelY = 241;
    }

    @Override
    protected void init() {
        super.init();
        previous = addRenderableWidget(Button.builder(
                Component.literal("◀ Previous"), button -> changePage(0))
                .bounds(leftPos + 18, topPos + 163, 92, 21).build());
        next = addRenderableWidget(Button.builder(
                Component.literal("Next ▶"), button -> changePage(1))
                .bounds(leftPos + 254, topPos + 163, 92, 21).build());
        // The filter lives in an independent right-hand column. No filter
        // widget or ghost sample covers any real pallet/player slot.
        modeButton=addRenderableWidget(Button.builder(Component.literal("Whitelist"),
                button->changeFilter(2)).bounds(leftPos+389,topPos+170,74,20).build());
        scopeButton=addRenderableWidget(Button.builder(Component.literal("Item"),
                button->changeFilter(3)).bounds(leftPos+467,topPos+170,73,20).build());
        sampleButton=addRenderableWidget(Button.builder(Component.literal("Offhand Sample"),
                button->changeFilter(4)).bounds(leftPos+389,topPos+225,151,20).build());
        targetField=addRenderableWidget(new EditBox(font,leftPos+389,topPos+200,
                151,20,Component.literal("Filter targets")));
        targetField.setMaxLength(512);
        targetField.setHint(Component.literal("Item IDs / mod / tag"));
        targetField.setValue(menu.filterTarget());
        lastRule=menu.filterTarget();
        ghostPreview=menu.filterTarget();
        applyButton=addRenderableWidget(Button.builder(Component.literal("Apply"),
                button->applyTarget()).bounds(leftPos+389,topPos+251,73,20).build());
        clearFilter=addRenderableWidget(Button.builder(Component.literal("Clear Samples"),
                button->{
                    ghostPreview="";
                    targetField.setValue("");
                    applyTarget();
                }).bounds(leftPos+467,topPos+251,73,20).build());
        if(menu.isTransferPoint())
            ptpButton=addRenderableWidget(Button.builder(Component.literal("PTP: INPUT"),
                    button->togglePtp()).bounds(leftPos+14,topPos+266,85,32).build());
        updateControls();
    }

    private void togglePtp(){
        if(minecraft==null||minecraft.player==null||minecraft.gameMode==null
                ||!menu.isTransferPoint())return;
        if(menu.clickMenuButton(minecraft.player,5))
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId,5);
        updateControls();
    }

    private void changeFilter(int action){
        if(minecraft==null||minecraft.player==null||minecraft.gameMode==null
                || !menu.hasFilter())return;
        if(menu.clickMenuButton(minecraft.player,action))
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId,action);
        if(action==3||action==4){
            String next=menu.filterTarget();
            targetField.setValue(next);
            lastRule=next;
        }
        updateControls();
    }

    private void applyTarget(){
        if(!menu.hasFilter()||targetField==null)return;
        ghostPreview=targetField.getValue();
        ClientPlayNetworking.send(new PalletFilterNet.SetTarget(
                menu.containerId,ghostPreview));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateControls();
    }

    private void updateControls() {
        if(previous!=null)previous.active=menu.page()>0;
        if(next!=null)next.active=menu.page()<1;
        if(ptpButton!=null)
            ptpButton.setMessage(Component.literal(menu.ptpInput()?"PTP: INPUT":"PTP: OUTPUT"));
        filterOpen=menu.hasFilter();
        if(modeButton!=null){
            modeButton.visible=filterOpen;
            modeButton.setMessage(Component.literal(menu.filterMode()==0?"Whitelist":"Blacklist"));
        }
        if(scopeButton!=null){
            scopeButton.visible=filterOpen;
            String[] names={"Item","Whole Mod","Item Tag","Exact Data"};
            scopeButton.setMessage(Component.literal(names[menu.filterScope()]));
        }
        if(sampleButton!=null){
            sampleButton.visible=filterOpen;
            sampleButton.active=menu.filterScope()!=2;
            sampleButton.setMessage(Component.literal(menu.filterScope()==2
                    ?"Type tag ID":"Offhand Sample"));
        }
        if(applyButton!=null){
            applyButton.visible=filterOpen;
            applyButton.active=menu.filterScope()!=3;
        }
        if(clearFilter!=null)clearFilter.visible=filterOpen;
        if(targetField!=null){
            targetField.visible=filterOpen;
            targetField.active=menu.filterScope()!=3;
        }
    }

    @Override public void extractBackground(
            GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partialTick) {
        super.extractBackground(graphics,mouseX,mouseY,partialTick);
        int x=leftPos,y=topPos;
        graphics.fill(x,y,x+IMAGE_W,y+IMAGE_H,OUTER);
        graphics.fill(x+2,y+2,x+IMAGE_W-2,y+IMAGE_H-2,FRAME);
        graphics.fill(x+4,y+4,x+IMAGE_W-4,y+IMAGE_H-4,CONTENT);
        graphics.fill(x+4,y+4,x+IMAGE_W-4,y+32,HEADER);
        graphics.fill(x+4,y+31,x+IMAGE_W-4,y+33,ACCENT);
        // One 18x6 page of the 216-slot pallet, with controls immediately
        // beneath. The side filter never obscures these live inventory slots.
        graphics.fill(x+12,y+40,x+375,y+160,PANEL);
        drawSlots(graphics,x+STORAGE_X,y+STORAGE_Y,PalletMenu.COLS,PalletMenu.ROWS);
        graphics.fill(x+12,y+161,x+375,y+188,CONTENT);
        graphics.fill(x+114,y+164,x+252,y+184,HEADER);
        // Always show the module slot so a module can be inserted; editor
        // and its 16 ghost samples only exist when a module is installed.
        graphics.fill(x+353,y+48,x+376,y+74,SLOT_EDGE);
        drawSlots(graphics,x+356,y+53,1,1);
        if(filterOpen){
            graphics.fill(x+382,y+39,x+543,y+329,PANEL);
            graphics.fill(x+386,y+44,x+539,y+164,0xffd7dfda);
            for(int slot=0;slot<16;slot++){
                int px=x+398+(slot%4)*22,py=y+72+(slot/4)*22;
                graphics.fill(px,py,px+19,py+19,SLOT_EDGE);
                graphics.fill(px+1,py+1,px+18,py+18,SLOT_FILL);
                String[] samples=ghostPreview.isBlank()
                        ?new String[0]:ghostPreview.split(",");
                if(menu.filterScope()==0&&slot<samples.length){
                    Identifier id=Identifier.tryParse(samples[slot]);
                    if(id!=null){
                        var item=BuiltInRegistries.ITEM.getValue(id);
                        if(item!=null)graphics.item(new ItemStack(item),px+1,py+1);
                    }
                }
            }
        }
        graphics.fill(x+12,y+239,x+375,y+333,PANEL);
        graphics.fill(x+PLAYER_X-4,y+PLAYER_Y-4,x+PLAYER_X+167,y+332,SLOT_EDGE);
        drawSlots(graphics,x+PLAYER_X,y+PLAYER_Y,9,3);
        drawSlots(graphics,x+PLAYER_X,y+PLAYER_Y+58,9,1);
    }

    private static void drawSlots(GuiGraphicsExtractor graphics,
                                  int x, int y, int columns, int rows) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                int sx = x + col * 18;
                int sy = y + row * 18;
                graphics.fill(sx, sy, sx + 18, sy + 18, SLOT_EDGE);
                graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, SLOT_FILL);
            }
        }
    }

    @Override protected void extractLabels(GuiGraphicsExtractor graphics,int mouseX,int mouseY) {
        graphics.text(font,title,16,16,0xfff1f6fa,false);
        graphics.text(font,Component.literal("216 SLOTS  /  2 PAGES"),
                224,17,0xffbed0da,false);
        graphics.text(font,Component.literal("PALLET STORAGE"),22,39,TEXT,false);
        Component page=Component.literal("PAGE "+(menu.page()+1)+" / 2");
        graphics.text(font,page,(375-font.width(page))/2,170,0xffe8f4f7,false);
        graphics.text(font,Component.literal("PLAYER INVENTORY"),PLAYER_X,240,TEXT,false);
        if(filterOpen){
            graphics.text(font,Component.literal("PALLET FILTER"),390,46,TEXT,false);
            graphics.text(font,Component.literal("16 ITEM SAMPLES"),390,59,MUTED,false);
            graphics.text(font,Component.literal("POLICY / TARGET TYPE"),389,158,TEXT,false);
            graphics.text(font,Component.literal("Filter target"),389,191,TEXT,false);
            graphics.text(font,Component.literal("Click samples with held items"),389,288,MUTED,false);
            graphics.text(font,Component.literal("Empty cursor removes samples"),389,301,MUTED,false);
        }
    }

    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick){
        int x=(int)event.x()-leftPos,y=(int)event.y()-topPos;
        if(filterOpen&&menu.filterScope()==0
                &&x>=398&&x<398+4*22&&y>=72&&y<72+4*22){
            int col=(x-398)/22,row=(y-72)/22;
            if((x-398)%22<19&&(y-72)%22<19){
                editGhost(row*4+col);
                return true;
            }
        }
        return super.mouseClicked(event,doubleClick);
    }
    private void editGhost(int slot){
        if(!menu.hasFilter()||menu.filterScope()!=0)return;
        java.util.ArrayList<String> samples=new java.util.ArrayList<>();
        if(!ghostPreview.isBlank()){
            for(String s:ghostPreview.split(",")){
                if(!s.isBlank())samples.add(s);
            }
        }
        ItemStack cursor=menu.getCarried();
        if(cursor.isEmpty()){
            if(slot<samples.size())samples.remove(slot);
        }else{
            String id=BuiltInRegistries.ITEM.getKey(cursor.getItem()).toString();
            if(samples.contains(id))return;
            if(slot<samples.size())samples.set(slot,id);
            else if(samples.size()<16)samples.add(id);
        }
        ghostPreview=String.join(",",samples);
        targetField.setValue(ghostPreview);
        applyTarget();
    }

    private void changePage(int page) {
        if (minecraft == null || minecraft.player == null || minecraft.gameMode == null) return;
        if (page == menu.page()) return;
        if (menu.clickMenuButton(minecraft.player, page)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, page);
            updateControls();
        }
    }
}
