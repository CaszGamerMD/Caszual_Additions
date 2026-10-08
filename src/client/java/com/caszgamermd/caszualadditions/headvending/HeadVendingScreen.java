package com.caszgamermd.caszualadditions.headvending;

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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;

public final class HeadVendingScreen extends Screen {
    private static final int WIDTH = 276;
    private static final int HEIGHT = 218;
    private static final int MAX_RESULTS = 7;

    private final BlockPos machinePos;
    private final List<Result> results = new ArrayList<>();
    private final List<Button> buyButtons = new ArrayList<>();
    private EditBox search;

    public HeadVendingScreen(BlockPos machinePos) {
        super(Component.translatable("screen.caszual_additions.head_vending"));
        this.machinePos = machinePos;
    }

    @Override
    protected void init() {
        int left = (width - WIDTH) / 2;
        int top = (height - HEIGHT) / 2;

        search = addRenderableWidget(new EditBox(
                font,
                left + 18,
                top + 37,
                WIDTH - 36,
                20,
                Component.translatable("screen.caszual_additions.head_vending.search")
        ));
        search.setMaxLength(32);
        search.setHint(Component.literal("Player username or mob head..."));
        search.setResponder(value -> refreshResults());

        buyButtons.clear();
        for (int i = 0; i < MAX_RESULTS; i++) {
            final int resultIndex = i;
            Button button = Button.builder(
                            Component.literal("Buy — 1 Emerald"),
                            ignored -> buy(resultIndex)
                    )
                    .bounds(
                            left + 158,
                            top + 66 + i * 20,
                            100,
                            18
                    )
                    .build();
            buyButtons.add(button);
            addRenderableWidget(button);
        }

        refreshResults();
        setInitialFocus(search);
    }

    private void refreshResults() {
        results.clear();

        String raw = search == null ? "" : search.getValue().trim();
        String query = raw.toLowerCase(Locale.ROOT);

        if (!raw.isEmpty() && raw.matches("[A-Za-z0-9_]{1,16}")) {
            ItemStack head = new ItemStack(Items.PLAYER_HEAD);
            head.set(DataComponents.PROFILE, ResolvableProfile.createUnresolved(raw));
            head.set(DataComponents.ITEM_NAME, Component.literal(raw + "'s Head"));
            results.add(new Result(
                    "Player: " + raw,
                    head,
                    true,
                    raw
            ));
        }

        for (var entry : HeadVendingContent.MOB_HEADS.entrySet()) {
            String key = entry.getKey();
            String label = HeadVendingContent.displayName(key) + " Head";
            if (query.isEmpty()
                    || key.contains(query.replace(' ', '_'))
                    || label.toLowerCase(Locale.ROOT).contains(query)) {
                results.add(new Result(
                        label,
                        new ItemStack(entry.getValue()),
                        false,
                        key
                ));
            }
        }

        while (results.size() > MAX_RESULTS) {
            results.remove(results.size() - 1);
        }

        for (int i = 0; i < buyButtons.size(); i++) {
            Button button = buyButtons.get(i);
            button.visible = i < results.size();
            button.active = i < results.size();
        }
    }

    private void buy(int index) {
        if (index < 0 || index >= results.size()) return;
        Result result = results.get(index);

        ClientPlayNetworking.send(new HeadVendingContent.Purchase(
                machinePos,
                result.playerHead(),
                result.target()
        ));
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);

        int left = (width - WIDTH) / 2;
        int top = (height - HEIGHT) / 2;

        graphics.fill(left, top, left + WIDTH, top + HEIGHT, 0xff25292f);
        graphics.fill(left + 3, top + 3, left + WIDTH - 3, top + HEIGHT - 3, 0xffd9dde2);
        graphics.fill(left + 8, top + 8, left + WIDTH - 8, top + 31, 0xff59636f);

        graphics.text(
                font,
                title,
                left + 16,
                top + 15,
                0xffffffff,
                false
        );

        graphics.item(new ItemStack(Items.EMERALD), left + WIDTH - 31, top + 12);
        graphics.text(
                font,
                Component.literal("1"),
                left + WIDTH - 14,
                top + 17,
                0xffffffff,
                false
        );

        if (results.isEmpty()) {
            graphics.text(
                    font,
                    Component.literal("No matching heads"),
                    left + 18,
                    top + 72,
                    0xff555555,
                    false
            );
            return;
        }

        for (int i = 0; i < results.size(); i++) {
            Result result = results.get(i);
            int y = top + 67 + i * 20;

            graphics.fill(
                    left + 14,
                    y - 1,
                    left + WIDTH - 14,
                    y + 17,
                    (i & 1) == 0 ? 0xffc8cdd2 : 0xffbfc5cb
            );
            graphics.item(result.stack(), left + 18, y);
            graphics.text(
                    font,
                    Component.literal(result.label()),
                    left + 40,
                    y + 5,
                    0xff202020,
                    false
            );
        }

        graphics.text(
                font,
                Component.literal("Each purchase costs exactly 1 emerald."),
                left + 18,
                top + HEIGHT - 17,
                0xff555555,
                false
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record Result(
            String label,
            ItemStack stack,
            boolean playerHead,
            String target
    ) {}
}
