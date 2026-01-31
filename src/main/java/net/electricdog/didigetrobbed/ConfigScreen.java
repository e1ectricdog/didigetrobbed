package net.electricdog.didigetrobbed;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private final Config config;

    public ConfigScreen(Screen parent) {
        super(Text.literal("Config"));
        this.parent = parent;
        this.config = Config.getInstance();
    }

    @Override
    protected void init() {
        int buttonWidth = 300;
        int buttonHeight = 20;
        int centerX = this.width / 2;
        int startY = 80;
        int spacing = 26;

        int currentY = startY;

        this.addDrawableChild(ButtonWidget.builder(
                        Text.literal("Track Chests: " + (config.trackChests ? "ON" : "OFF")),
                        button -> {
                            config.trackChests = !config.trackChests;
                            config.save();
                            this.clearAndInit();
                        })
                .dimensions(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight)
                .build());
        currentY += spacing;

        this.addDrawableChild(ButtonWidget.builder(
                        Text.literal("Track Barrels: " + (config.trackBarrels ? "ON" : "OFF")),
                        button -> {
                            config.trackBarrels = !config.trackBarrels;
                            config.save();
                            this.clearAndInit();
                        })
                .dimensions(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight)
                .build());
        currentY += spacing;

        this.addDrawableChild(ButtonWidget.builder(
                        Text.literal("Track Shulker Boxes: " + (config.trackShulkerBoxes ? "ON" : "OFF")),
                        button -> {
                            config.trackShulkerBoxes = !config.trackShulkerBoxes;
                            config.save();
                            this.clearAndInit();
                        })
                .dimensions(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight)
                .build());

        this.addDrawableChild(ButtonWidget.builder(
                        Text.literal("Done"),
                        button -> this.close())
                .dimensions(centerX - 100, this.height - 28, 200, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }
}