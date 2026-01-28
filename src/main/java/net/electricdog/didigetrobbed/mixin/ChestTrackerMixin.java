package net.electricdog.didigetrobbed.mixin;

import com.google.gson.*;
import net.electricdog.didigetrobbed.ChestContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(HandledScreen.class)
public abstract class ChestTrackerMixin {

    @Shadow @Final protected ScreenHandler handler;

    @Inject(method = "close", at = @At("HEAD"))
    private void onContainerClose(CallbackInfo ci) {
        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;

        String title = screen.getTitle().getString();
        if (title == null || !didigetrobbed$isStorageContainer(title)) return;

        BlockPos pos = ChestContext.getLastContainerPos();
        if (pos == null) return;

        int totalSlots = handler.slots.size();
        int containerSlots = totalSlots - 36;
        if (containerSlots <= 0) return;

        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < containerSlots; i++) {
            Slot slot = handler.getSlot(i);
            if (slot.hasStack()) {
                items.add(slot.getStack().copy());
            }
        }

        didigetrobbed$checkAndReport(pos, title, items);

        didigetrobbed$saveChest(pos, title, items);

        ChestContext.setLastContainerPos(null);
    }

    @Unique
    private void didigetrobbed$checkAndReport(BlockPos pos, String name, List<ItemStack> items) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        try {
            Path dir = client.runDirectory.toPath().resolve("didigetrobbed");
            Files.createDirectories(dir);
            Path file = dir.resolve("chests.json");

            if (!Files.exists(file)) return;

            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            String world = client.world.getRegistryKey().getValue().toString();
            String chestId = world + "@" + pos.getX() + "," + pos.getY() + "," + pos.getZ();

            if (!root.has(chestId)) return;

            JsonObject chest = root.getAsJsonObject(chestId);
            int openCount = chest.has("open_count") ? chest.get("open_count").getAsInt() : 0;

            Map<String, Integer> currentItems = new HashMap<>();
            for (ItemStack stack : items) {
                String itemId = Registries.ITEM.getId(stack.getItem()).toString();
                currentItems.put(itemId, currentItems.getOrDefault(itemId, 0) + stack.getCount());
            }

            Map<String, Integer> savedItems = new HashMap<>();
            JsonArray savedItemsArray = chest.getAsJsonArray("items");
            for (JsonElement element : savedItemsArray) {
                JsonObject obj = element.getAsJsonObject();
                String itemId = obj.get("id").getAsString();
                int count = obj.get("count").getAsInt();
                savedItems.put(itemId, savedItems.getOrDefault(itemId, 0) + count);
            }

            List<String> missingReport = new ArrayList<>();

            for (Map.Entry<String, Integer> entry : savedItems.entrySet()) {
                String itemId = entry.getKey();
                int savedCount = entry.getValue();
                int currentCount = currentItems.getOrDefault(itemId, 0);

                if (currentCount < savedCount) {
                    int missing = savedCount - currentCount;
                    String itemName = Registries.ITEM.get(Identifier.of(itemId)).getName().getString();
                    missingReport.add("§c- " + missing + "x " + itemName);
                }
            }

            if (!missingReport.isEmpty()) {
                client.player.sendMessage(Text.literal("§6[DidIGetRobbed] §fItems missing from " + name + ":"), false);

                for (String line : missingReport) {
                    client.player.sendMessage(Text.literal(line), false);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Unique
    private void didigetrobbed$saveChest(BlockPos pos, String name, List<ItemStack> items) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        try {
            Path dir = client.runDirectory.toPath().resolve("didigetrobbed");
            Files.createDirectories(dir);
            Path file = dir.resolve("chests.json");

            JsonObject root;
            if (Files.exists(file)) {
                try {
                    root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                } catch (Exception e) {
                    root = new JsonObject();
                }
            } else {
                root = new JsonObject();
            }

            String world = client.world.getRegistryKey().getValue().toString();
            String chestId = world + "@" + pos.getX() + "," + pos.getY() + "," + pos.getZ();

            JsonObject chest = new JsonObject();
            chest.addProperty("last_seen_name", name);
            chest.addProperty("timestamp", System.currentTimeMillis());

            JsonArray contents = new JsonArray();
            for (ItemStack stack : items) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", Registries.ITEM.getId(stack.getItem()).toString());
                obj.addProperty("count", stack.getCount());
                contents.add(obj);
            }

            chest.add("items", contents);
            root.add(chestId, chest);

            Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(root));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Unique
    private boolean didigetrobbed$isStorageContainer(String title) {
        String lower = title.toLowerCase();
        return lower.contains("chest") ||
                lower.contains("barrel") ||
                lower.contains("shulker") ||
                lower.contains("box");
    }
}