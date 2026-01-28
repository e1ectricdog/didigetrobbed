package net.electricdog.didigetrobbed.mixin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.electricdog.didigetrobbed.ChestContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
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
import java.util.HashMap;
import java.util.Map;

@Mixin(HandledScreen.class)
public abstract class ChestRenderMixin {

    @Shadow @Final protected ScreenHandler handler;
    @Shadow protected int x;
    @Shadow protected int y;

    @Unique
    private Map<Integer, ItemStack> didigetrobbed$missingItems = null;

    @Inject(method = "init", at = @At("TAIL"))
    private void onScreenInit(CallbackInfo ci) {
        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
        String title = screen.getTitle().getString();

        if (!didigetrobbed$isStorageContainer(title)) {
            didigetrobbed$missingItems = null;
            return;
        }

        BlockPos pos = ChestContext.getLastContainerPos();
        if (pos == null) {
            didigetrobbed$missingItems = null;
            return;
        }

        didigetrobbed$missingItems = didigetrobbed$loadMissingItems(pos);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (didigetrobbed$missingItems == null || didigetrobbed$missingItems.isEmpty()) {
            return;
        }

        for (Map.Entry<Integer, ItemStack> entry : didigetrobbed$missingItems.entrySet()) {
            int slotIndex = entry.getKey();
            ItemStack missingStack = entry.getValue();

            if (slotIndex >= handler.slots.size()) continue;

            Slot slot = handler.getSlot(slotIndex);
            if (slot.hasStack()) continue;

            int slotX = this.x + slot.x;
            int slotY = this.y + slot.y;

            context.drawItem(missingStack, slotX, slotY);

            context.fill(slotX, slotY, slotX + 16, slotY + 16, 0x88FF0000);
        }
    }

    @Unique
    private Map<Integer, ItemStack> didigetrobbed$loadMissingItems(BlockPos pos) {
        Map<Integer, ItemStack> missingItems = new HashMap<>();

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return missingItems;
        }

        try {
            Path file = client.runDirectory.toPath().resolve("didigetrobbed").resolve("chests.json");
            if (!Files.exists(file)) {
                return missingItems;
            }

            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            String world = client.world.getRegistryKey().getValue().toString();
            String chestId = world + "@" + pos.getX() + "," + pos.getY() + "," + pos.getZ();

            if (!root.has(chestId)) {
                return missingItems;
            }

            JsonObject chest = root.getAsJsonObject(chestId);
            JsonArray savedItems = chest.getAsJsonArray("items");

            Map<String, Integer> currentItems = new HashMap<>();
            int containerSlots = handler.slots.size() - 36;
            for (int i = 0; i < containerSlots; i++) {
                Slot slot = handler.getSlot(i);
                if (slot.hasStack()) {
                    ItemStack stack = slot.getStack();
                    String itemId = Registries.ITEM.getId(stack.getItem()).toString();
                    currentItems.put(itemId, currentItems.getOrDefault(itemId, 0) + stack.getCount());
                }
            }

            Map<String, Integer> savedItemCounts = new HashMap<>();
            for (JsonElement element : savedItems) {
                JsonObject obj = element.getAsJsonObject();
                String itemId = obj.get("id").getAsString();
                int count = obj.get("count").getAsInt();
                savedItemCounts.put(itemId, savedItemCounts.getOrDefault(itemId, 0) + count);
            }

            for (Map.Entry<String, Integer> entry : savedItemCounts.entrySet()) {
                String itemId = entry.getKey();
                int savedCount = entry.getValue();
                int currentCount = currentItems.getOrDefault(itemId, 0);

                if (currentCount < savedCount) {
                    int missingCount = savedCount - currentCount;
                    Identifier id = Identifier.of(itemId);
                    ItemStack ghostStack = new ItemStack(Registries.ITEM.get(id), missingCount);

                    for (int i = 0; i < containerSlots; i++) {
                        if (!handler.getSlot(i).hasStack() && !missingItems.containsKey(i)) {
                            missingItems.put(i, ghostStack);
                            break;
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return missingItems;
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