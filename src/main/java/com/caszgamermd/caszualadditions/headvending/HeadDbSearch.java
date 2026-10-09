package com.caszgamermd.caszualadditions.headvending;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.util.Util;

/**
 * Searches HeadDB's official, small public API pages rather than blocking
 * every vending-machine lookup on a multi-megabyte full-catalog download.
 *
 * Upstream requires a minimum limit of 12; the vending screen displays 7.
 * We bridge the underlying 12-head pages to continuous seven-head UI pages
 * so that no heads are skipped or repeated between vending pages.
 *
 * The server retains the validated returned hashes for purchase authorization.
 * The client never supplies arbitrary skins.
 */
public final class HeadDbSearch {
    private static final String API = "https://headdb.net/api/v1/heads";
    private static final int UPSTREAM_PAGE_SIZE = 12;
    private static final int MAX_RESPONSE_BYTES = 1_000_000;
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL).build();
    private static final Map<String, CustomHeadCatalog.Head> VERIFIED = new ConcurrentHashMap<>();
    private static volatile List<String> knownCategories = List.of("all");

    private HeadDbSearch() {}

    public static CompletableFuture<String> searchAsync(String query, String category, int page) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return search(query, category, page);
            } catch (Exception failure) {
                return error(query, category, page, failure);
            }
        }, Util.nonCriticalIoPool());
    }

    public static CompletableFuture<CustomHeadCatalog.Head> findMobAsync(String mobName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                RemotePage results = requestPage(mobName, "all", 1);
                String normalizedName = normalize(mobName);
                CustomHeadCatalog.Head best = null;
                for (JsonElement e : results.items()) {
                    CustomHeadCatalog.Head head = parseHead(e);
                    if (head == null) continue;
                    String name = normalize(head.name());
                    if (name.equals(normalizedName) || name.equals(normalizedName + " head")) {
                        return head;
                    }
                    if (best == null && name.startsWith(normalizedName + " ")) best = head;
                }
                return best;
            } catch (Exception failure) {
                return null;
            }
        }, Util.nonCriticalIoPool());
    }

    public static CustomHeadCatalog.Head verified(String hash) {
        return hash == null ? null : VERIFIED.get(hash.toLowerCase(Locale.ROOT));
    }

    public static String categoryOf(String raw) {
        return raw == null || raw.isBlank() ? "all" : raw.strip().toLowerCase(Locale.ROOT);
    }

    private static String search(String query, String category, int requestedPage) throws Exception {
        int page = Math.clamp(requestedPage, 0, 10_000);
        int start = page * CustomHeadCatalog.PAGE_SIZE;
        int remoteFirst = start / UPSTREAM_PAGE_SIZE + 1;
        int remoteLast = (start + CustomHeadCatalog.PAGE_SIZE - 1) / UPSTREAM_PAGE_SIZE + 1;

        RemotePage first = requestPage(query, category, remoteFirst);
        RemotePage second = remoteLast == remoteFirst
                ? first : requestPage(query, category, remoteLast);

        JsonArray visible = new JsonArray();
        int total = first.total();
        for (int i = start; i < Math.min(start + CustomHeadCatalog.PAGE_SIZE, total); i++) {
            RemotePage source = i / UPSTREAM_PAGE_SIZE + 1 == remoteFirst ? first : second;
            int index = i % UPSTREAM_PAGE_SIZE;
            if (index >= source.items().size()) continue;
            CustomHeadCatalog.Head head = parseHead(source.items().get(index));
            if (head == null) continue;
            JsonObject entry = new JsonObject();
            entry.addProperty("name", head.name());
            entry.addProperty("category", head.category());
            entry.addProperty("hash", head.hash());
            visible.add(entry);
        }

        JsonObject response = new JsonObject();
        response.addProperty("query", query);
        response.addProperty("category", category);
        response.addProperty("page", requestedPage);
        response.addProperty("total", total);
        JsonArray categories = new JsonArray();
        for (String name : knownCategories) categories.add(name);
        response.add("categories", categories);
        response.add("items", visible);
        return response.toString();
    }

    private static RemotePage requestPage(String query, String category, int oneBasedPage) throws Exception {
        StringBuilder url = new StringBuilder(API).append("?limit=12&sort=name&direction=asc&page=")
                .append(oneBasedPage);
        if (!query.isBlank()) url.append("&q=").append(enc(query.strip()));
        if (!category.isBlank() && !"all".equals(category))
            url.append("&category=").append(enc(category));

        HttpRequest request = HttpRequest.newBuilder(URI.create(url.toString()))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .header("User-Agent", "CaszualAdditions/0.1 (custom-head vending)")
                .GET().build();
        HttpResponse<byte[]> response = HTTP.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200)
            throw new IllegalStateException("HeadDB returned HTTP " + response.statusCode());
        if (response.body().length > MAX_RESPONSE_BYTES)
            throw new IllegalStateException("HeadDB sent an unexpectedly large search page");

        JsonElement root = JsonParser.parseString(new String(response.body(), StandardCharsets.UTF_8));
        if (!root.isJsonObject()) throw new IllegalStateException("Unrecognized HeadDB search response");
        JsonObject data = root.getAsJsonObject();
        JsonArray items = getArray(data, "items");
        if (items == null && data.has("data") && data.get("data").isJsonObject())
            items = getArray(data.getAsJsonObject("data"), "items");
        if (items == null) throw new IllegalStateException("HeadDB search did not include items");

        JsonObject pagination = data.has("pagination") && data.get("pagination").isJsonObject()
                ? data.getAsJsonObject("pagination") : data;
        int total = firstInt(pagination, "total", "totalItems", "totalCount", "count");
        if (total < 0) total = firstInt(data, "total", "totalItems", "totalCount", "count");
        if (total < 0) total = items.size() < UPSTREAM_PAGE_SIZE
                ? (oneBasedPage - 1) * UPSTREAM_PAGE_SIZE + items.size()
                : oneBasedPage * UPSTREAM_PAGE_SIZE + UPSTREAM_PAGE_SIZE;

        updateCategories(items);
        return new RemotePage(items, Math.max(0, total));
    }

    private static void updateCategories(JsonArray items) {
        Set<String> categories = new LinkedHashSet<>(knownCategories);
        categories.add("all");
        for (JsonElement e : items) {
            if (!e.isJsonObject()) continue;
            String category = category(e.getAsJsonObject());
            if (!category.isBlank() && category.length() <= 40) categories.add(category);
        }
        if (categories.size() > 64) return;
        List<String> sorted = new ArrayList<>(categories);
        sorted.remove("all");
        sorted.sort(Comparator.naturalOrder());
        sorted.add(0, "all");
        knownCategories = List.copyOf(sorted);
    }

    private static CustomHeadCatalog.Head parseHead(JsonElement item) {
        if (!item.isJsonObject()) return null;
        JsonObject object = item.getAsJsonObject();
        String name = firstString(object, "name", "displayName", "title");
        String category = category(object);
        String hash = firstString(object, "textureHash", "texture_hash",
                "hash", "texture", "textureUrl", "skinHash");
        if (hash.startsWith("http")) {
            int i = hash.lastIndexOf('/');
            hash = hash.substring(i + 1);
        }
        if (object.has("texture") && object.get("texture").isJsonObject()) {
            JsonObject texture = object.getAsJsonObject("texture");
            if (!hash.matches("[a-fA-F0-9]{32,128}"))
                hash = firstString(texture, "hash", "textureHash");
        }
        hash = hash.toLowerCase(Locale.ROOT);
        if (name.isBlank() || name.length() > 100 || !hash.matches("[a-f0-9]{32,128}"))
            return null;
        if (category.isBlank()) category = "other";
        StringBuilder keywords = new StringBuilder(name).append(' ').append(category);
        JsonArray tags = getArray(object, "tags");
        if (tags != null) for (JsonElement tag : tags) {
            if (tag.isJsonPrimitive()) keywords.append(' ').append(tag.getAsString());
            else if (tag.isJsonObject()) keywords.append(' ')
                    .append(firstString(tag.getAsJsonObject(), "name", "slug"));
        }

        CustomHeadCatalog.Head head = new CustomHeadCatalog.Head(name, category, hash,
                keywords.toString().toLowerCase(Locale.ROOT));
        if (VERIFIED.size() > 12_000) VERIFIED.clear();
        VERIFIED.put(hash, head);
        return head;
    }

    private static String category(JsonObject object) {
        JsonElement category = object.get("category");
        if (category == null || category.isJsonNull()) return "other";
        if (category.isJsonPrimitive()) return category.getAsString().toLowerCase(Locale.ROOT);
        if (category.isJsonObject())
            return firstString(category.getAsJsonObject(), "slug", "name").toLowerCase(Locale.ROOT);
        return "other";
    }

    private static String normalize(String input) {
        return input.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    private static String firstString(JsonObject object, String... keys) {
        for (String key : keys) {
            JsonElement el = object.get(key);
            if (el != null && el.isJsonPrimitive() && el.getAsJsonPrimitive().isString())
                return el.getAsString();
        }
        return "";
    }

    private static int firstInt(JsonObject object, String... keys) {
        for (String key : keys) {
            JsonElement el = object.get(key);
            if (el != null && el.isJsonPrimitive()) {
                try { return el.getAsInt(); } catch (RuntimeException ignored) {}
            }
        }
        return -1;
    }

    private static JsonArray getArray(JsonObject object, String name) {
        return object.has(name) && object.get(name).isJsonArray()
                ? object.getAsJsonArray(name) : null;
    }

    private static String error(String query, String category, int page, Exception exception) {
        JsonObject response = new JsonObject();
        response.addProperty("query", query);
        response.addProperty("category", category);
        response.addProperty("page", page);
        response.addProperty("total", 0);
        response.add("items", new JsonArray());
        JsonArray categories = new JsonArray();
        for (String name : knownCategories) categories.add(name);
        response.add("categories", categories);
        response.addProperty("error", "HeadDB search failed: " + exception.getMessage()
                + ". Check the server's internet access, then retry.");
        return response.toString();
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private record RemotePage(JsonArray items, int total) {}
}
