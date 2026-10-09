package com.caszgamermd.caszualadditions.headvending;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;

/**
 * Server-side, read-only catalog of decorative heads.
 *
 * The upstream is HeadDB's public compatibility catalog endpoint. No catalog data
 * is bundled with the mod. Only validated texture hashes,
 * names, and categories are sent to the client, and purchases are resolved against
 * this server-held catalog (never against an arbitrary client-supplied texture).
 */
public final class CustomHeadCatalog {
    private static final URI CATALOG_URL = URI.create(
            "https://headdb.net/api/v1/legacy/heads.json"
    );
    private static final long REFRESH_MILLIS = 6L * 60L * 60L * 1000L;
    private static final long RETRY_MILLIS = 2L * 60L * 1000L;
    private static final int MAX_DOWNLOAD_BYTES = 40_000_000;
    public static final int PAGE_SIZE = 7;

    private static volatile CompletableFuture<Catalog> loading;
    private static volatile Catalog lastGood;
    private static volatile long nextRefresh;

    private CustomHeadCatalog() {}

    public static synchronized CompletableFuture<Catalog> load() {
        long now = System.currentTimeMillis();
        if (loading == null || (loading.isDone() && now >= nextRefresh)) {
            nextRefresh = now + REFRESH_MILLIS;
            loading = CompletableFuture.supplyAsync(CustomHeadCatalog::download, Util.nonCriticalIoPool())
                    .handle((catalog, error) -> {
                        if (error != null) {
                            nextRefresh = System.currentTimeMillis() + RETRY_MILLIS;
                            if (lastGood != null) return lastGood;
                            throw new CompletionException(error);
                        }
                        lastGood = catalog;
                        return catalog;
                    });
        }
        return loading;
    }

    private static Catalog download() {
        try {
            HttpRequest request = HttpRequest.newBuilder(CATALOG_URL)
                    .timeout(Duration.ofSeconds(20))
                    .header("User-Agent", "CaszualAdditions/0.1 HeadVending HeadDB")
                    .GET()
                    .build();
            HttpResponse<byte[]> response = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(10))
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200 || response.body().length > MAX_DOWNLOAD_BYTES) {
                throw new IllegalStateException("Custom-head catalog unavailable (HTTP "
                        + response.statusCode() + ")");
            }

            JsonElement root = JsonParser.parseString(
                    new String(response.body(), StandardCharsets.UTF_8));
            if (!root.isJsonArray()) {
                throw new IllegalStateException("Unexpected custom-head catalog format");
            }

            List<Head> heads = new ArrayList<>();
            Map<String, Head> byHash = new HashMap<>();
            Set<String> categoryNames = new HashSet<>();
            for (JsonElement element : root.getAsJsonArray()) {
                if (!element.isJsonObject()) continue;
                JsonObject object = element.getAsJsonObject();
                String name = string(object, "name");
                String category = string(object, "category");
                String hash = string(object, "texture").toLowerCase(Locale.ROOT);
                if (hash.isBlank()) {
                    hash = string(object, "hash").toLowerCase(Locale.ROOT);
                }
                if (name.isBlank() || name.length() > 100
                        || !hash.matches("[a-f0-9]{32,128}")) {
                    continue;
                }
                if (category.isBlank() || category.length() > 40) category = "other";
                category = category.toLowerCase(Locale.ROOT);
                StringBuilder keywords = new StringBuilder(name).append(' ').append(category);
                if (object.has("tags") && object.get("tags").isJsonArray()) {
                    for (JsonElement tag : object.getAsJsonArray("tags")) {
                        if (tag.isJsonPrimitive() && tag.getAsJsonPrimitive().isString()) {
                            keywords.append(' ').append(tag.getAsString(), 0,
                                    Math.min(80, tag.getAsString().length()));
                        }
                    }
                }
                Head head = new Head(name, category, hash,
                        keywords.toString().toLowerCase(Locale.ROOT));
                heads.add(head);
                byHash.putIfAbsent(hash, head);
                categoryNames.add(category);
            }
            if (heads.isEmpty()) throw new IllegalStateException("The custom-head catalog was empty");
            heads.sort(Comparator.comparing(Head::name, String.CASE_INSENSITIVE_ORDER));
            List<String> categories = new ArrayList<>(categoryNames);
            categories.sort(String.CASE_INSENSITIVE_ORDER);
            categories.add(0, "all");
            return new Catalog(List.copyOf(heads), Map.copyOf(byHash), List.copyOf(categories));
        } catch (Exception exception) {
            throw new CompletionException(exception);
        }
    }

    private static String string(JsonObject object, String property) {
        JsonElement value = object.get(property);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                ? value.getAsString() : "";
    }

    public static ItemStack createHead(String name, String textureHash) {
        if (textureHash == null || !textureHash.matches("[a-f0-9]{32,128}")) {
            return ItemStack.EMPTY;
        }
        String url = "https://textures.minecraft.net/texture/" + textureHash;
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}";
        String base64 = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        GameProfile profile = new GameProfile(
                UUID.nameUUIDFromBytes(("caszual-head:" + textureHash)
                        .getBytes(StandardCharsets.UTF_8)),
                "CustomHead"
        );
        profile.properties().put("textures", new Property("textures", base64));

        ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
        stack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(profile));
        stack.set(DataComponents.ITEM_NAME, Component.literal(name));
        return stack;
    }

    public record Head(String name, String category, String hash, String keywords) {}

    public record Catalog(List<Head> heads, Map<String, Head> byHash, List<String> categories) {
        public Head find(String hash) {
            return byHash.get(hash);
        }

        public String search(String query, String category, int page) {
            String term = query.trim().toLowerCase(Locale.ROOT);
            String filter = category.toLowerCase(Locale.ROOT);
            int safePage = Math.clamp(page, 0, 10_000);

            List<Head> matches = new ArrayList<>();
            for (Head head : heads) {
                if (!filter.equals("all") && !head.category().equals(filter)) continue;
                if (!term.isEmpty() && !head.keywords().contains(term)) continue;
                matches.add(head);
            }

            int total = matches.size();
            int maxPage = Math.max(0, (total - 1) / PAGE_SIZE);
            safePage = Math.min(safePage, maxPage);

            JsonObject response = new JsonObject();
            response.addProperty("query", query);
            response.addProperty("category", category);
            response.addProperty("page", safePage);
            response.addProperty("total", total);
            JsonArray options = new JsonArray();
            for (String option : categories) options.add(option);
            response.add("categories", options);

            JsonArray items = new JsonArray();
            for (int i = safePage * PAGE_SIZE;
                    i < Math.min(total, (safePage + 1) * PAGE_SIZE); i++) {
                Head head = matches.get(i);
                JsonObject item = new JsonObject();
                item.addProperty("name", head.name());
                item.addProperty("category", head.category());
                item.addProperty("hash", head.hash());
                items.add(item);
            }
            response.add("items", items);
            return response.toString();
        }

        public static String failure(String query, String category, int page) {
            JsonObject response = new JsonObject();
            response.addProperty("query", query);
            response.addProperty("category", category);
            response.addProperty("page", page);
            response.addProperty("total", 0);
            response.addProperty("error", "Custom heads are temporarily unavailable. Try again later.");
            response.add("categories", new JsonArray());
            response.add("items", new JsonArray());
            return response.toString();
        }
    }
}
