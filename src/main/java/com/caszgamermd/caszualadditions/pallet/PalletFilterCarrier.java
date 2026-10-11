package com.caszgamermd.caszualadditions.pallet;

import com.caszgamermd.caszualadditions.racks.RackContent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;

/** Shared filter upgrade behavior for free-standing, racked, and transfer pallets. */
public interface PalletFilterCarrier {
    ItemStack filterUpgrade();
    void setFilterUpgrade(ItemStack module);
    int filterMode();
    void setFilterMode(int mode); // 0 = whitelist, 1 = blacklist
    int filterScope();
    void setFilterScope(int scope); // 0 item, 1 namespace, 2 tag, 3 exact stack components
    String filterTarget();
    void setFilterTarget(String target);
    String filterComponentSignature();
    void setFilterComponentSignature(String signature);

    default boolean allows(ItemStack candidate) {
        if(candidate.isEmpty() || !filterUpgrade().is(RackContent.PALLET_FILTER))
            return true;
        String target=filterTarget();
        if(target.isBlank()) return true; // installed but not configured
        Identifier item=BuiltInRegistries.ITEM.getKey(candidate.getItem());
        boolean matched=switch(filterScope()) {
            case 1 -> item.getNamespace().equals(target);
            case 2 -> {
                Identifier tag=Identifier.tryParse(target);
                yield tag!=null && candidate.is(TagKey.create(Registries.ITEM,tag));
            }
            case 3 -> item.toString().equals(target) &&
                    candidate.getComponents().toString().equals(filterComponentSignature());
            default -> item.toString().equals(target);
        };
        return filterMode()==1 ? !matched : matched;
    }
}
