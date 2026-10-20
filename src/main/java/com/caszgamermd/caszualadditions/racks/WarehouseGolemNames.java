package com.caszgamermd.caszualadditions.racks;

import net.minecraft.util.RandomSource;

/** Friendly names belonging to individual warehouse robots, NOT their jobs. */
public final class WarehouseGolemNames {
    private WarehouseGolemNames(){}
    public static final String[] POOL={
        "Bolt","Rivet","Widget","Sprocket","Gizmo","Gasket","Piston","Clank",
        "Nibbles","Biscuit","Pebble","Pickles","Waffles","Muffin","Beans","Sprout",
        "Tinker","Sparky","Bubbles","Doodle","Noodle","Pip","Rusty","Copper",
        "Mochi","Toaster","Cricket","Zippy","Bumble","Button","Whiskers","Pogo",
        "Scooter","Mopsy","Cogs","Dusty","Scraps","Marbles","Bop","Fidget"
    };
    public static net.minecraft.world.item.ItemStack namedToken(String name){
        net.minecraft.world.item.ItemStack item=new net.minecraft.world.item.ItemStack(
                RackContent.STORAGE_GOLEM_ITEM);
        if(name!=null&&!name.isBlank())
            item.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                    net.minecraft.network.chat.Component.literal(name));
        return item;
    }
    public static String fromToken(net.minecraft.world.item.ItemStack item){
        net.minecraft.network.chat.Component label=item.get(
                net.minecraft.core.component.DataComponents.CUSTOM_NAME);
        return label==null?"":label.getString();
    }
    public static String random(RandomSource random){
        return POOL[random.nextInt(POOL.length)];
    }
    public static boolean known(String name){
        for(String option:POOL)if(option.equals(name))return true;
        return false;
    }
}
