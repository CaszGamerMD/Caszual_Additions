package com.caszgamermd.caszualadditions.headvending;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.item.Item;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;

public final class HeadVendingScreen extends Screen {
    private static final int WIDTH = 336;
    private static final int HEIGHT = 282;
    private static final int MAX_RESULTS = CustomHeadCatalog.PAGE_SIZE;

    private enum Tab { CUSTOM, PLAYER, MOB, FAVORITES, HISTORY, CART }
    private enum Kind { CUSTOM, PLAYER, MOB }
    private static final class CartEntry {
        Result result;
        int amount;
        CartEntry(Result result) { this.result = result; this.amount = 1; }
    }

    private final BlockPos machinePos;
    private final List<Result> results = new ArrayList<>();
    private static final int GRID_COLS = 6;
    private static final int GRID_X = 18;
    private static final int GRID_Y = 94;
    private static final int CELL_W = 50;
    private static final int CELL_H = 47;
    private static final int CART_LIMIT = 64;
    private final Map<String, CartEntry> cart = new LinkedHashMap<>();
    private final Map<String, String> mobTextures = new java.util.HashMap<>();
    private final java.util.Set<String> mobRequested = new java.util.HashSet<>();
    private Button checkoutButton;
    private Button favoriteCartButton;
    private Button clearCartButton;
    private String notice = "";
    private String hovered = "";
    private final List<SavedHead> favorites = new ArrayList<>();
    private final List<SavedHead> history = new ArrayList<>();
    private List<String> categories = List.of("all");

    private Tab tab = Tab.CUSTOM;
    private String category = "all";
    private EditBox search;
    private Button categoryButton;
    private Button refreshButton;
    private Button previousButton;
    private Button nextButton;
    private int page;
    private int total;
    private int searchDelay;
    private boolean loading = true;
    private String error = "";

    public HeadVendingScreen(BlockPos machinePos) {
        super(Component.translatable("screen.caszual_additions.head_vending"));
        this.machinePos = machinePos;
    }

    @Override
    protected void init() {
        int left = (width - WIDTH) / 2;
        int top = (height - HEIGHT) / 2;
        String priorText = search == null ? "" : search.getValue();

        String[] names = {"Custom", "Players", "Mobs", "Favorites", "History", "Cart"};
        Tab[] tabs = Tab.values();
        for (int i = 0; i < tabs.length; i++) {
            final Tab selected = tabs[i];
            addRenderableWidget(Button.builder(Component.literal(names[i]),
                    ignored -> setTab(selected))
                    .bounds(left + 13 + i * 52, top + 36, 51, 21).build());
        }

        refreshButton = addRenderableWidget(Button.builder(
                Component.literal("Refresh"), ignored -> refreshResults()
        ).bounds(left + 227, top + 10, 62, 18).build());

        search = addRenderableWidget(new EditBox(
                font, left + 18, top + 64, 185, 20,
                Component.translatable("screen.caszual_additions.head_vending.search")
        ));
        search.setMaxLength(48);
        search.setValue(priorText);
        search.setHint(Component.literal(hint()));
        search.setResponder(value -> {
            page = 0;
            refreshResults();
        });

        categoryButton = addRenderableWidget(Button.builder(
                Component.literal("Category: All"),
                ignored -> cycleCategory()
        ).bounds(left + 208, top + 64, 111, 20).build());

        checkoutButton = addRenderableWidget(Button.builder(
                Component.literal("Checkout"), ignored -> checkout())
                .bounds(left + 18, top + 247, 96, 19).build());
        favoriteCartButton = addRenderableWidget(Button.builder(
                Component.literal("☆ Cart"), ignored -> favoriteCart())
                .bounds(left + 120, top + 247, 96, 19).build());
        clearCartButton = addRenderableWidget(Button.builder(
                Component.literal("Clear Cart"), ignored -> clearCart())
                .bounds(left + 222, top + 247, 96, 19).build());

        previousButton = addRenderableWidget(Button.builder(
                Component.literal("< Previous"), ignored -> changePage(-1)
        ).bounds(left + 18, top + 248, 86, 19).build());
        nextButton = addRenderableWidget(Button.builder(
                Component.literal("Next >"), ignored -> changePage(1)
        ).bounds(left + WIDTH - 104, top + 248, 86, 19).build());

        refreshResults();
        setInitialFocus(search);
    }

    private String hint() {
        return switch (tab) {
            case CUSTOM -> "Search decorative heads...";
            case PLAYER -> "Enter a player username...";
            case MOB -> "Search vanilla mob heads...";
            case FAVORITES -> "Filter your favorites...";
            case HISTORY -> "Filter recent purchases...";
            case CART -> "Filter items in cart...";
        };
    }

    private void setTab(Tab selected) {
        if (tab == selected) return;
        tab = selected;
        page = 0;
        category = "all";
        search.setValue("");
        search.setHint(Component.literal(hint()));
        refreshResults();
        setInitialFocus(search);
    }

    private void cycleCategory() {
        if (tab != Tab.CUSTOM || categories.isEmpty()) return;
        int next = (categories.indexOf(category) + 1) % categories.size();
        category = categories.get(next);
        page = 0;
        refreshResults();
    }

    private void changePage(int direction) {
        if (tab == Tab.PLAYER) return;
        int lastPage = Math.max(0, (total - 1) / MAX_RESULTS);
        int target = Math.clamp(page + direction, 0, lastPage);
        if (target == page) return;
        page = target;
        refreshResults();
    }

    private void refreshResults() {
        results.clear();
        error = "";
        if (tab == Tab.CUSTOM) {
            loading = true;
            searchDelay = 8; // Debounce typing to avoid network requests per keystroke.
        } else {
            loading = false;
            searchDelay = -1;
            String raw = search == null ? "" : search.getValue().trim();
            String query = raw.toLowerCase(Locale.ROOT);

            if (tab == Tab.PLAYER && raw.matches("[A-Za-z0-9_]{1,16}")) {
                ItemStack head = new ItemStack(Items.PLAYER_HEAD);
                head.set(DataComponents.PROFILE, ResolvableProfile.createUnresolved(raw));
                head.set(DataComponents.ITEM_NAME, Component.literal(raw + "'s Head"));
                results.add(new Result(raw + "'s Head", "Player", head, Kind.PLAYER, raw));
            }
            if (tab == Tab.MOB) {
                for (var entry : HeadVendingContent.MOB_HEADS.entrySet()) {
                    String key = entry.getKey();
                    // Backward-compatible "dragon" alias is available to purchases,
                    // but the catalog only displays the Ender Dragon once.
                    if (key.equals("dragon")) continue;
                    String label = HeadVendingContent.displayName(key) + " Head";
                    if (query.isEmpty() || key.contains(query.replace(' ', '_'))
                            || label.toLowerCase(Locale.ROOT).contains(query)) {
                        results.add(new Result(label, "Vanilla Mob",
                                mobPreview(key, entry.getValue()), Kind.MOB, key));
                    }
                }
            }
            if (tab == Tab.FAVORITES || tab == Tab.HISTORY) {
                for (SavedHead saved : (tab == Tab.FAVORITES ? favorites : history)) {
                    if (!query.isEmpty() && !saved.label().toLowerCase(Locale.ROOT).contains(query)) continue;
                    Result result = resultForSaved(saved);
                    if (result != null) results.add(result);
                }
            }
            if (tab == Tab.CART) {
                for (CartEntry entry : cart.values()) {
                    if (query.isEmpty() || entry.result.label().toLowerCase(Locale.ROOT).contains(query))
                        results.add(entry.result);
                }
            }
            total = results.size();
            if (tab != Tab.PLAYER) {
                int last = Math.max(0, (total - 1) / MAX_RESULTS);
                page = Math.clamp(page, 0, last);
                int skip = page * MAX_RESULTS;
                if (skip > 0) results.subList(0, skip).clear();
                if (results.size() > MAX_RESULTS)
                    results.subList(MAX_RESULTS, results.size()).clear();
            }
        }
        updateButtons();
    }

    @Override
    public void tick() {
        super.tick();
        if (tab == Tab.MOB || tab == Tab.FAVORITES || tab == Tab.HISTORY || tab == Tab.CART)
            requestVisibleMobTextures();
        if (tab == Tab.CUSTOM && searchDelay >= 0) {
            if (searchDelay-- == 0) {
                ClientPlayNetworking.send(new HeadVendingContent.SearchCustom(
                        machinePos, search.getValue().trim(), category, page));
            }
        }
    }

    public void acceptCustomResults(BlockPos pos, String json) {
        if (!machinePos.equals(pos) || tab != Tab.CUSTOM || search == null) return;

        try {
            JsonObject data = JsonParser.parseString(json).getAsJsonObject();
            if (!data.has("query") || !data.has("category") || !data.has("page")) {
                error = "Invalid head-search reply. Ensure client and server use the same mod version.";
                loading = false;
                results.clear();
                updateButtons();
                return;
            }
            if (!search.getValue().trim().equals(data.get("query").getAsString())
                    || !category.equals(data.get("category").getAsString())
                    || page != data.get("page").getAsInt()) {
                return; // Ignore replies to earlier searches.
            }

            results.clear();
            total = data.has("total") ? data.get("total").getAsInt() : 0;
            error = data.has("error") && !data.get("error").isJsonNull()
                    ? data.get("error").getAsString() : "";
            if (data.has("categories") && data.get("categories").isJsonArray()) {
                List<String> options = new ArrayList<>();
                for (JsonElement option : data.getAsJsonArray("categories")) {
                    if (option.isJsonPrimitive()) options.add(option.getAsString());
                }
                if (!options.isEmpty()) categories = List.copyOf(options);
            }

            int rejected = 0;
            int previewFailures = 0;
            String reason = "";
            JsonArray items = data.has("items") && data.get("items").isJsonArray()
                    ? data.getAsJsonArray("items") : new JsonArray();
            for (JsonElement element : items) {
                if (results.size() >= MAX_RESULTS) break;
                try {
                    if (!element.isJsonObject()) {
                        rejected++;
                        continue;
                    }
                    JsonObject item = element.getAsJsonObject();
                    String name = item.has("name") ? item.get("name").getAsString() : "";
                    String type = item.has("category") ? item.get("category").getAsString() : "other";
                    String hash = item.has("hash") ? item.get("hash").getAsString() : "";
                    if (name.isBlank() || !hash.matches("[a-fA-F0-9]{32,128}")) {
                        rejected++;
                        continue;
                    }
                    // A texture or profile rendering failure must never hide
                    // an otherwise valid purchasable head. The server already
                    // verified this hash against the real HeadDB response.
                    ItemStack preview;
                    try {
                        preview = CustomHeadCatalog.createHead(
                                name, hash.toLowerCase(Locale.ROOT));
                        if (preview.isEmpty()) throw new IllegalStateException("Empty player head preview");
                    } catch (RuntimeException previewError) {
                        previewFailures++;
                        preview = new ItemStack(Items.PLAYER_HEAD);
                        org.slf4j.LoggerFactory.getLogger("CaszualAdditions/HeadVending")
                                .warn("Failed to prepare preview for HeadDB head '{}' (hash {}); "
                                        + "keeping the result purchasable", name, hash, previewError);
                    }
                    results.add(new Result(name, type, preview, Kind.CUSTOM,
                            hash.toLowerCase(Locale.ROOT)));
                } catch (RuntimeException exception) {
                    rejected++;
                    reason = exception.getClass().getSimpleName() + ": " + exception.getMessage();
                    org.slf4j.LoggerFactory.getLogger("CaszualAdditions/HeadVending")
                            .warn("Could not render one HeadDB search result", exception);
                }
            }

            if (error.isBlank() && results.isEmpty() && (rejected > 0 || total > 0)) {
                error = "HeadDB found " + total + " heads but sent no usable texture hashes. "
                        + (reason.isBlank() ? "Check latest.log for HeadVending diagnostics." : reason);
            } else if (error.isBlank() && previewFailures > 0) {
                // Warn without hiding the rows or disabling purchases.
                error = previewFailures + " head preview(s) unavailable; "
                        + "buying and favoriting still work.";
            }
            if (rejected > 0) {
                org.slf4j.LoggerFactory.getLogger("CaszualAdditions/HeadVending")
                        .warn("Rejected {} malformed custom-head entries from HeadDB ({} total)",
                                rejected, total);
            }
        } catch (RuntimeException exception) {
            org.slf4j.LoggerFactory.getLogger("CaszualAdditions/HeadVending")
                    .error("Malformed custom-head search response ({} characters)", json.length(), exception);
            error = "Invalid search reply: " + exception.getClass().getSimpleName()
                    + ". Check latest.log; ensure mod versions match on client and server.";
            results.clear();
            total = 0;
        } finally {
            loading = false;
            updateButtons();
        }
    }

    private void updateButtons() {
        if (refreshButton != null) refreshButton.visible = tab == Tab.CUSTOM;
        if (categoryButton != null) {
            categoryButton.visible = tab == Tab.CUSTOM;
            String label = category.length() > 11 ? category.substring(0, 10) + "..." : category;
            categoryButton.setMessage(Component.literal("Category: " + label));
        }
        if (previousButton != null) {
            previousButton.visible = tab != Tab.PLAYER;
            previousButton.active = !loading && page > 0;
        }
        if (nextButton != null) {
            nextButton.visible = tab != Tab.PLAYER;
            nextButton.active = !loading && (page + 1) * MAX_RESULTS < total;
        }
        if (checkoutButton != null) {
            checkoutButton.visible = tab == Tab.CART;
            checkoutButton.active = !cart.isEmpty();
            checkoutButton.setMessage(Component.literal("Checkout (" + cartCount() + ")"));
        }
        if (favoriteCartButton != null) {
            favoriteCartButton.visible = tab == Tab.CART;
            favoriteCartButton.active = !cart.isEmpty();
        }
        if (clearCartButton != null) {
            clearCartButton.visible = tab == Tab.CART;
            clearCartButton.active = !cart.isEmpty();
        }
    }

    public void acceptSavedHeads(String json) {
        try {
            JsonObject data = JsonParser.parseString(json).getAsJsonObject();
            favorites.clear();
            history.clear();
            for (JsonElement entry : data.getAsJsonArray("favorites")) {
                SavedHead parsed = parseSaved(entry.getAsString());
                if (parsed != null) favorites.add(parsed);
            }
            for (JsonElement entry : data.getAsJsonArray("history")) {
                SavedHead parsed = parseSaved(entry.getAsString());
                if (parsed != null) history.add(parsed);
            }
            if (tab == Tab.FAVORITES || tab == Tab.HISTORY) refreshResults();
            else updateButtons();
        } catch (RuntimeException ignored) {
            // Keep screen functional when the server data cannot be parsed.
        }
    }

    private static SavedHead parseSaved(String raw) {
        String[] parts = raw.split("\\|", 3);
        return parts.length == 3 ? new SavedHead(parts[0], parts[1], parts[2]) : null;
    }

    private Result resultForSaved(SavedHead saved) {
        Kind kind;
        ItemStack item;
        switch (saved.kind()) {
            case "player" -> {
                kind = Kind.PLAYER;
                item = new ItemStack(Items.PLAYER_HEAD);
                item.set(DataComponents.PROFILE, ResolvableProfile.createUnresolved(saved.target()));
            }
            case "mob" -> {
                kind = Kind.MOB;
                var mob = HeadVendingContent.MOB_HEADS.get(saved.target());
                if (mob == null) return null;
                item = mobPreview(saved.target(), mob);
            }
            case "custom" -> {
                kind = Kind.CUSTOM;
                item = CustomHeadCatalog.createHead(saved.label(), saved.target());
                if (item.isEmpty()) return null;
            }
            default -> { return null; }
        }
        return new Result(saved.label(), saved.kind(), item, kind, saved.target());
    }

    private boolean isFavorite(Result result) {
        String kind = result.kind().name().toLowerCase(Locale.ROOT);
        return favorites.stream().anyMatch(entry -> entry.kind().equals(kind)
                && entry.target().equalsIgnoreCase(result.target()));
    }

    private static String identity(Result result) {
        return result.kind().name().toLowerCase(Locale.ROOT) + "|" +
                result.target().toLowerCase(Locale.ROOT);
    }

    private int cartCount() {
        int count = 0;
        for (CartEntry entry : cart.values()) count += entry.amount;
        return count;
    }

    private void addToCart(Result result) {
        if (cartCount() >= CART_LIMIT) {
            notice = "Cart is full (" + CART_LIMIT + " heads maximum).";
            return;
        }
        String key = identity(result);
        CartEntry entry = cart.get(key);
        if (entry == null) cart.put(key, new CartEntry(result));
        else entry.amount++;
        notice = result.label() + " added to cart.";
        updateButtons();
    }

    private void removeFromCart(Result result) {
        String key = identity(result);
        CartEntry entry = cart.get(key);
        if (entry == null) return;
        if (--entry.amount <= 0) cart.remove(key);
        refreshResults();
        notice = result.label() + " removed from cart.";
    }

    private void setFavorite(Result result, boolean add) {
        if (isFavorite(result) == add) return; // idempotent right-click
        ClientPlayNetworking.send(new HeadVendingBookmarks.Toggle(machinePos,
                result.kind().name().toLowerCase(Locale.ROOT),
                result.target(), result.label(), add));
        SavedHead saved = new SavedHead(result.kind().name().toLowerCase(Locale.ROOT),
                result.target().toLowerCase(Locale.ROOT), result.label());
        favorites.removeIf(entry -> entry.kind().equals(saved.kind())
                && entry.target().equals(saved.target()));
        if (add) favorites.add(0, saved);
        if (tab == Tab.FAVORITES) refreshResults();
        notice = add ? "Added to favorites." : "Removed from favorites.";
    }

    private void checkout() {
        if (cart.isEmpty()) return;
        JsonArray lines = new JsonArray();
        for (CartEntry entry : cart.values()) {
            JsonObject line = new JsonObject();
            line.addProperty("kind", entry.result.kind().name().toLowerCase(Locale.ROOT));
            line.addProperty("target", entry.result.target());
            line.addProperty("qty", entry.amount);
            lines.add(line);
        }
        ClientPlayNetworking.send(new HeadVendingContent.Checkout(machinePos, lines.toString()));
        notice = "Checking out " + cartCount() + " heads...";
        // Wait for server acknowledgement before clearing a cart: inventory full,
        // missing emeralds or a failed remote lookup must not erase it.
        checkoutButton.active = false;
    }

    public void acceptCheckout(boolean success, String message) {
        notice = message;
        if (success) {
            cart.clear();
            if (tab == Tab.CART) refreshResults();
        }
        updateButtons();
    }

    private void favoriteCart() {
        for (CartEntry entry : cart.values())
            setFavorite(entry.result, true);
        notice = "Cart items added to favorites.";
    }

    private void clearCart() {
        cart.clear();
        notice = "Cart cleared.";
        refreshResults();
    }

    private ItemStack mobPreview(String key, Item nativeItem) {
        if (nativeItem != Items.PLAYER_HEAD) return new ItemStack(nativeItem);
        String hash = mobTextures.get(key);
        if (hash != null && hash.matches("[a-f0-9]{32,128}"))
            return CustomHeadCatalog.createHead(HeadVendingContent.displayName(key) + " Head", hash);
        return new ItemStack(Items.PLAYER_HEAD);
    }

    private void requestVisibleMobTextures() {
        int sent = 0;
        for (Result result : results) {
            if (result.kind() != Kind.MOB
                    || HeadVendingContent.MOB_HEADS.get(result.target()) != Items.PLAYER_HEAD
                    || !mobRequested.add(result.target())) continue;
            ClientPlayNetworking.send(new HeadVendingContent.MobPreviewRequest(
                    machinePos, result.target()));
            if (++sent == 2) break; // Avoid flooding HeadDB with a whole page at once.
        }
    }

    public void acceptMobPreview(BlockPos pos, String key, String hash) {
        if (!machinePos.equals(pos)) return;
        if (!hash.matches("[a-f0-9]{32,128}")) return;
        mobTextures.put(key, hash);
        for (int i = 0; i < results.size(); i++) {
            Result entry = results.get(i);
            if (entry.kind() == Kind.MOB && entry.target().equals(key))
                results.set(i, new Result(entry.label(), entry.category(),
                        mobPreview(key, Items.PLAYER_HEAD), Kind.MOB, key));
        }
        // Refresh the cart snapshot as well so its previews update.
        CartEntry inCart = cart.get("mob|" + key);
        if (inCart != null) {
            Result old = inCart.result;
            inCart.result = new Result(old.label(), old.category(),
                    mobPreview(key, Items.PLAYER_HEAD), Kind.MOB, key);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int left = (width - WIDTH) / 2;
        int top = (height - HEIGHT) / 2;
        double mx = event.x(), my = event.y();
        if (mx >= left + GRID_X && mx < left + GRID_X + GRID_COLS * CELL_W
                && my >= top + GRID_Y && my < top + GRID_Y + 3 * CELL_H) {
            int col = (int)(mx - left - GRID_X) / CELL_W;
            int row = (int)(my - top - GRID_Y) / CELL_H;
            int index = row * GRID_COLS + col;
            if (index >= 0 && index < results.size()) {
                Result result = results.get(index);
                if (event.button() == 0) {
                    if (tab == Tab.CART) removeFromCart(result);
                    else addToCart(result);
                    return true;
                }
                if (event.button() == 1) {
                    setFavorite(result, tab != Tab.FAVORITES);
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick
    ) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int left = (width - WIDTH) / 2;
        int top = (height - HEIGHT) / 2;

        graphics.fill(left, top, left + WIDTH, top + HEIGHT, 0xff25292f);
        graphics.fill(left + 3, top + 3, left + WIDTH - 3, top + HEIGHT - 3, 0xffd9dde2);
        graphics.fill(left + 8, top + 8, left + WIDTH - 8, top + 31, 0xff59636f);
        graphics.text(font, title, left + 16, top + 15, 0xffffffff, false);
        boolean free = minecraft != null && minecraft.player != null
                && minecraft.player.getAbilities().instabuild;
        if (free) {
            graphics.text(font, Component.literal("FREE"), left + WIDTH - 46,
                    top + 17, 0xffaaffaa, false);
        } else {
            graphics.item(new ItemStack(Items.EMERALD), left + WIDTH - 34, top + 12);
            graphics.text(font, Component.literal("1"), left + WIDTH - 16,
                    top + 17, 0xffffffff, false);
        }

        if (results.isEmpty()) {
            String message = !error.isEmpty() ? error
                    : loading ? "Searching custom-head database..."
                    : tab == Tab.PLAYER ? "Type a Minecraft username"
                    : tab == Tab.CART ? "Your cart is empty"
                    : "No matching heads";
            drawStatus(graphics, message, left + 18, top + 110, 3);
        }
        hovered = "";
        for (int i = 0; i < results.size(); i++) {
            Result result = results.get(i);
            int x = left + GRID_X + (i % GRID_COLS) * CELL_W;
            int y = top + GRID_Y + (i / GRID_COLS) * CELL_H;
            boolean hot = mouseX >= x && mouseX < x + CELL_W - 2
                    && mouseY >= y && mouseY < y + CELL_H - 2;
            graphics.fill(x, y, x + CELL_W - 2, y + CELL_H - 2,
                    hot ? 0xffaabfd7 : (i % 2 == 0 ? 0xffc7cdd4 : 0xffbec6ce));
            graphics.fill(x + 2, y + 2, x + CELL_W - 4, y + CELL_H - 4,
                    0xffe7e9ec);
            try {
                graphics.item(result.stack(), x + 16, y + 5);
            } catch (RuntimeException badPreview) {
                graphics.item(new ItemStack(Items.PLAYER_HEAD), x + 16, y + 5);
            }
            String caption = result.label();
            if (caption.length() > 9) caption = caption.substring(0, 8) + "...";
            graphics.text(font, Component.literal(caption), x + 3, y + 26,
                    0xff262b32, false);
            if (isFavorite(result))
                graphics.text(font, Component.literal("★"), x + 35, y + 3,
                        0xffbc8a22, false);
            CartEntry quantity = cart.get(identity(result));
            if (quantity != null) {
                graphics.text(font, Component.literal("" + quantity.amount),
                        x + 36, y + 15, 0xff315a24, false);
            }
            if (hot) hovered = result.label() + " (" + result.category() + ")";
        }
        if (!error.isEmpty() && !results.isEmpty()) {
            drawStatus(graphics, error, left + 18, top + 234, 1);
        } else if (!hovered.isEmpty()) {
            String line = hovered.length() > 50 ? hovered.substring(0, 48) + "..." : hovered;
            graphics.text(font, Component.literal(line), left + 18, top + 234,
                    0xff394658, false);
        } else if (!notice.isBlank()) {
            String line = notice.length() > 50 ? notice.substring(0, 48) + "..." : notice;
            graphics.text(font, Component.literal(line), left + 18, top + 234,
                    0xff394658, false);
        }
        if (tab != Tab.PLAYER) {
            int pages = Math.max(1, (total + MAX_RESULTS - 1) / MAX_RESULTS);
            graphics.text(font,
                    Component.literal("Page " + (page + 1) + "/" + pages + "  (" + total + ")"),
                    left + 117, top + 254, 0xff454545, false);
        }
        boolean creative = minecraft != null && minecraft.player != null
                && minecraft.player.getAbilities().instabuild;
        graphics.text(font, Component.literal(tab == Tab.CART
                        ? "Left click removes 1 · Right click favorites"
                        : "Left click: cart · Right click: favorite"),
                left + 18, top + HEIGHT - 12, 0xff555555, false);
    }

    /** Wrap errors instead of clipping their useful diagnostic text to one row. */
    private void drawStatus(GuiGraphicsExtractor graphics, String message,
                            int x, int y, int maxLines) {
        String remaining = message;
        for (int line = 0; line < maxLines && !remaining.isEmpty(); line++) {
            int chars = Math.min(48, remaining.length());
            if (chars < remaining.length()) {
                int space = remaining.lastIndexOf(' ', chars);
                if (space > 12) chars = space;
            }
            graphics.text(font, Component.literal(remaining.substring(0, chars)),
                    x, y + line * 11, 0xff4b4b4b, false);
            remaining = remaining.substring(chars).stripLeading();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record SavedHead(String kind, String target, String label) {}

    private record Result(
            String label, String category, ItemStack stack, Kind kind, String target
    ) {}
}
