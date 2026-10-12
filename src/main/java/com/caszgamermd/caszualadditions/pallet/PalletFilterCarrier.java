package com.caszgamermd.caszualadditions.pallet;

import com.caszgamermd.caszualadditions.racks.RackContent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;

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
        // All typed rules are ORed: whitelist admits any match, blacklist
        // rejects any match. EXACT keeps one full component signature from
        // the sampled offhand stack and is intentionally never split.
        boolean matched=false;
        if(filterScope()==3) {
            matched=item.toString().equals(target)
                    &&candidate.getComponents().toString().equals(filterComponentSignature());
        }else for(String entry:target.split(",")){
            String rule=entry.strip();
            if(rule.isBlank())continue;
            boolean found=switch(filterScope()) {
                case 1 -> item.getNamespace().equals(rule);
                case 2 -> {
                    Identifier tag=Identifier.tryParse(rule);
                    if(tag==null)yield false;
                    boolean itemTag=candidate.is(TagKey.create(Registries.ITEM,tag));
                    boolean blockTag=candidate.getItem() instanceof BlockItem blockItem
                            && blockItem.getBlock().defaultBlockState().is(
                                    TagKey.create(Registries.BLOCK,tag));
                    yield itemTag||blockTag;
                }
                default -> item.toString().equals(rule);
            };
            if(found){matched=true;break;}
        }
        return filterMode()==1 ? !matched : matched;
    }
}
