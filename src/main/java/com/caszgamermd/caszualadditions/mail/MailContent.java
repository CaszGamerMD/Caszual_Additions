package com.caszgamermd.caszualadditions.mail;

import com.caszgamermd.caszualadditions.CaszualAdditions;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class MailContent {
    public static MailBlock MAILBOX,PO_BOX;
    public static Item MAILBOX_ITEM,PO_BOX_ITEM;
    public static MenuType<MailMenu> MAILBOX_MENU,PO_BOX_MENU;
    private MailContent(){}
    private static MailBlock registerBlock(String name,boolean outgoing){
        Identifier id=CaszualAdditions.id(name);
        ResourceKey<Block> key=ResourceKey.create(Registries.BLOCK,id);
        return Registry.register(BuiltInRegistries.BLOCK,key,
                new MailBlock(outgoing,BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                        .strength(3.5f).noOcclusion().setId(key)));
    }
    private static Item item(String name,Block block){
        Identifier id=CaszualAdditions.id(name);
        ResourceKey<Item> key=ResourceKey.create(Registries.ITEM,id);
        return Registry.register(BuiltInRegistries.ITEM,key,
                new BlockItem(block,new Item.Properties().stacksTo(64)
                        .useBlockDescriptionPrefix().setId(key)));
    }
    public static void initialize(){
        MAILBOX=registerBlock("mailbox",true);
        PO_BOX=registerBlock("po_box",false);
        MAILBOX_ITEM=item("mailbox",MAILBOX);
        PO_BOX_ITEM=item("po_box",PO_BOX);
        MAILBOX_MENU=Registry.register(BuiltInRegistries.MENU,
                CaszualAdditions.id("mailbox"),
                new ExtendedMenuType<>((id,inventory,pos)->
                        new MailMenu(id,inventory,pos,true),BlockPos.STREAM_CODEC));
        PO_BOX_MENU=Registry.register(BuiltInRegistries.MENU,
                CaszualAdditions.id("po_box"),
                new ExtendedMenuType<>((id,inventory,pos)->
                        new MailMenu(id,inventory,pos,false),BlockPos.STREAM_CODEC));
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->
                MailSavedData.get(server).remember(handler.player));
    }
}
