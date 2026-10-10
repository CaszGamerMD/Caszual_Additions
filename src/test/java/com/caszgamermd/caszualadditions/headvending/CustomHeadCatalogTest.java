package com.caszgamermd.caszualadditions.headvending;

import com.google.gson.JsonParser;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HeadDB's public JSON schema (verified by the CI API smoke test) and the
 * player-head preview and purchase path must remain compatible with 26.2.
 */
class CustomHeadCatalogTest {
    private static final String HASH =
            "1ded4105dc6600ed82f61692095f254746affbaab976002ab754cb47c9874a6a";

    @BeforeAll static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void parsesOfficialHeadDbTextureField() {
        var json = JsonParser.parseString("""
                {"id":90545,"name":"Alex In Melon",
                "texture":"1ded4105dc6600ed82f61692095f254746affbaab976002ab754cb47c9874a6a",
                "category":{"name":"Humans","slug":"humans"}}
                """);
        var head = HeadDbSearch.parseHead(json);
        assertNotNull(head, "Official HeadDB API results must be displayable");
        assertEquals(HASH, head.hash());
        assertEquals("humans", head.category());
    }

    @Test void createsUsableCustomPlayerHeadForPreviewAndPurchase() {
        var stack = CustomHeadCatalog.createHead("Alex In Melon", HASH);
        assertTrue(stack.is(Items.PLAYER_HEAD));
        assertNotNull(stack.get(DataComponents.PROFILE));
        assertEquals("Alex In Melon", stack.get(DataComponents.ITEM_NAME).getString());
    }
}
