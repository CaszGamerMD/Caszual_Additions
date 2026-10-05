package com.caszgamermd.caszualadditions;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class CaszualTags {
    public static final TagKey<Item> CASZUAL_CONTENT =
            TagKey.create(Registries.ITEM, CaszualAdditions.id("caszual_content"));

    private CaszualTags() {
    }
}
