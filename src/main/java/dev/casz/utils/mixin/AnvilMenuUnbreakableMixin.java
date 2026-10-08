package dev.casz.utils.mixin;

import com.caszgamermd.caszualadditions.unbreakable.UnbreakableContent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Unit;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuUnbreakableMixin {
    @Shadow @Final private DataSlot cost;

    @Inject(method = "createResult", at = @At("TAIL"))
    private void caszualAdditions$applyUnbreakableBook(CallbackInfo ci) {
        AnvilMenu menu = (AnvilMenu)(Object)this;
        ItemStack base = menu.getSlot(AnvilMenu.INPUT_SLOT).getItem();
        ItemStack book = menu.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem();

        if (base.isEmpty()
                || !base.isDamageableItem()
                || !book.is(UnbreakableContent.UNBREAKABLE_BOOK)) {
            return;
        }

        ItemStack output = base.copy();
        output.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);

        menu.getSlot(AnvilMenu.RESULT_SLOT).set(output);
        cost.set(1);
        menu.broadcastChanges();
    }
}
