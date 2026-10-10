package com.caszgamermd.caszualadditions.headvending;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HeadDB's public JSON schema (verified by the CI API smoke test) and the
 * player-head preview and purchase path must remain compatible with 26.2.
 */
class CustomHeadCatalogTest {
    private static final String HASH =
            "1ded4105dc6600ed82f61692095f254746affbaab976002ab754cb47c9874a6a";

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

    @Test void buildsValidTextureProfileWithoutMutatingImmutableGameProfile() {
        var profile = CustomHeadCatalog.createHeadProfile(HASH);
        assertEquals("CustomHead", profile.name());
        assertTrue(profile.properties().containsKey("textures"),
                "Custom skin texture must be included at construction time");
        assertEquals(1, profile.properties().get("textures").size());
    }
}
