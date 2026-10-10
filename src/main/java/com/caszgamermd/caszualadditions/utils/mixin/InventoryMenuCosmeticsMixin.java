package com.caszgamermd.caszualadditions.utils.mixin;

import com.caszgamermd.caszualadditions.utils.cosmetics.Cosmetics;
import com.caszgamermd.caszualadditions.utils.cosmetics.CosmeticSlotContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryMenu.class)
public abstract class InventoryMenuCosmeticsMixin {
    @Inject(method="<init>", at=@At("RETURN"))
    private void caszual_additions$addCosmeticSlots(Inventory inventory, boolean active, Player owner, CallbackInfo ci) {
        var cosmetics = new CosmeticSlotContainer(owner);
        String[] icons={"helmet","chestplate","leggings","boots"};
        for(int i=0;i<4;i++){
            final var cosmeticSlot=Cosmetics.SLOTS[i];
            final Identifier emptyIcon=Identifier.withDefaultNamespace("container/slot/"+icons[i]);
            ((AbstractContainerMenuAccessor)(Object)this).caszual_additions$addSlot(new Slot(cosmetics,i,59,8+18*i){
                @Override public int getMaxStackSize(){return 1;}
                @Override public boolean mayPlace(ItemStack stack){return Cosmetics.accepts(stack,cosmeticSlot);}
                @Override public Identifier getNoItemIcon(){return emptyIcon;}
            });
        }
    }
}
