package com.caszgamermd.caszualadditions.headvending;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
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

    private enum Tab { CUSTOM, PLAYER, MOB, FAVORITES, HISTORY }
    private enum Kind { CUSTOM, PLAYER, MOB }

    private final BlockPos machinePos;
    private final List<Result> results = new ArrayList<>();
    private final List<Button> buyButtons = new ArrayList<>();
    private final List<Button> starButtons = new ArrayList<>();
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

        String[] names = {"Custom", "Players", "Mobs", "Favorites", "History"};
        Tab[] tabs = Tab.values();
        for (int i = 0; i < tabs.length; i++) {
            final Tab selected = tabs[i];
            addRenderableWidget(Button.builder(Component.literal(names[i]),
                    ignored -> setTab(selected))
                    .bounds(left + 15 + i * 62, top + 36, 61, 21).build());
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

        buyButtons.clear();
        for (int i = 0; i < MAX_RESULTS; i++) {
            final int resultIndex = i;
            Button star = Button.builder(Component.literal("☆"),
                    ignored -> toggleFavorite(resultIndex))
                    .bounds(left + 221, top + 94 + i * 21, 23, 18).build();
            starButtons.add(star);
            addRenderableWidget(star);
            Button button = Button.builder(Component.literal("Buy"),
                    ignored -> buy(resultIndex))
                    .bounds(left + 245, top + 94 + i * 21, 75, 18).build();
            buyButtons.add(button);
            addRenderableWidget(button);
        }

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
                                new ItemStack(entry.getValue()), Kind.MOB, key));
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
            total = results.size();
            if (tab != Tab.PLAYER && total > MAX_RESULTS) {
                int last = Math.max(0, (total - 1) / MAX_RESULTS);
                page = Math.clamp(page, 0, last);
                results.subList(0, page * MAX_RESULTS).clear();
                if (results.size() > MAX_RESULTS) results.subList(MAX_RESULTS, results.size()).clear();
            }
        }
        updateButtons();
    }

    @Override
    public void tick() {
        super.tick();
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
            if (!search.getValue().trim().equals(data.get("query").getAsString())
                    || !category.equals(data.get("category").getAsString())
                    || page != data.get("page").getAsInt()) {
                return; // An earlier search completed after the user typed something new.
            }
            results.clear();
            JsonArray items = data.getAsJsonArray("items");
            for (JsonElement element : items) {
                if (results.size() >= MAX_RESULTS) break;
                JsonObject item = element.getAsJsonObject();
                String name = item.get("name").getAsString();
                String type = item.get("category").getAsString();
                String hash = item.get("hash").getAsString();
                ItemStack head = CustomHeadCatalog.createHead(name, hash);
                if (!head.isEmpty()) results.add(new Result(name, type, head, Kind.CUSTOM, hash));
            }

            if (data.has("categories") && data.get("categories").isJsonArray()) {
                List<String> options = new ArrayList<>();
                for (JsonElement element : data.getAsJsonArray("categories")) {
                    if (element.isJsonPrimitive()) options.add(element.getAsString());
                }
                if (!options.isEmpty()) categories = List.copyOf(options);
            }
            total = data.get("total").getAsInt();
            error = data.has("error") ? data.get("error").getAsString() : "";
            loading = false;
            updateButtons();
        } catch (RuntimeException ignored) {
            error = "Could not read the custom-head search results.";
            loading = false;
            updateButtons();
        }
    }

    private void updateButtons() {
        for (int i = 0; i < buyButtons.size(); i++) {
            Button button = buyButtons.get(i);
            button.visible = i < results.size();
            button.active = i < results.size();
            boolean free = minecraft != null && minecraft.player != null
                    && minecraft.player.getAbilities().instabuild;
            button.setMessage(Component.literal(free ? "Free" : "1 Emerald"));
            Button star = starButtons.get(i);
            star.visible = i < results.size();
            star.active = star.visible;
            if (star.visible) {
                Result entry = results.get(i);
                star.setMessage(Component.literal(isFavorite(entry) ? "★" : "☆"));
            }
        }
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
                item = new ItemStack(mob);
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

    private void toggleFavorite(int index) {
        if (index < 0 || index >= results.size()) return;
        Result entry = results.get(index);
        ClientPlayNetworking.send(new HeadVendingBookmarks.Toggle(machinePos,
                entry.kind().name().toLowerCase(Locale.ROOT),
                entry.target(), entry.label()));
    }

    private void buy(int index) {
        if (index < 0 || index >= results.size()) return;
        Result result = results.get(index);
        if (result.kind() == Kind.CUSTOM) {
            ClientPlayNetworking.send(
                    new HeadVendingContent.BuyCustom(machinePos, result.target()));
        } else {
            ClientPlayNetworking.send(new HeadVendingContent.Purchase(
                    machinePos, result.kind() == Kind.PLAYER, result.target()));
        }
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
                    : "No matching heads";
            graphics.text(font, Component.literal(message), left + 18,
                    top + 106, 0xff555555, false);
        }

        for (int i = 0; i < results.size(); i++) {
            Result result = results.get(i);
            int y = top + 94 + i * 21;
            graphics.fill(left + 14, y - 1, left + WIDTH - 14, y + 19,
                    (i & 1) == 0 ? 0xffc8cdd2 : 0xffbfc5cb);
            graphics.item(result.stack(), left + 19, y + 1);
            String label = result.label();
            if (label.length() > 27) label = label.substring(0, 24) + "...";
            graphics.text(font, Component.literal(label), left + 43,
                    y + 1, 0xff202020, false);
            String detail = result.category();
            if (detail.length() > 28) detail = detail.substring(0, 25) + "...";
            graphics.text(font, Component.literal(detail), left + 43,
                    y + 11, 0xff5d5d5d, false);
        }

        if (tab != Tab.PLAYER) {
            int pages = Math.max(1, (total + MAX_RESULTS - 1) / MAX_RESULTS);
            graphics.text(font,
                    Component.literal("Page " + (page + 1) + "/" + pages
                            + "  (" + total + " heads)"),
                    left + 112, top + 254, 0xff454545, false);
        }
        graphics.text(font, Component.literal(free ? "Creative: free heads" : "1 emerald per head"),
                left + 18, top + HEIGHT - 12, 0xff555555, false);
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
