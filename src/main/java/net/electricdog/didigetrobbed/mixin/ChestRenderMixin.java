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
import net.minecraft.util.WorldSavePath;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.math.BigInteger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(HandledScreen.class)
public abstract class ChestRenderMixin {

    @Shadow @Final protected ScreenHandler handler;
    @Shadow protected int x;
    @Shadow protected int y;

    @Unique private Map<Integer, ItemStack> didigetrobbed$missingItems = null;
    @Unique private boolean didigetrobbed$hasChecked = false;
    @Unique private BlockPos didigetrobbed$currentChestPos = null;
    @Unique private String didigetrobbed$currentChestName = null;

    @Inject(method = "init", at = @At("TAIL"))
    private void onScreenInit(CallbackInfo ci) {
        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
        String title = screen.getTitle().getString();

        didigetrobbed$hasChecked = false;
        didigetrobbed$missingItems = null;

        if (!didigetrobbed$isStorageContainer(title)) {
            didigetrobbed$currentChestPos = null;
            didigetrobbed$currentChestName = null;
            return;
        }

        BlockPos pos = ChestContext.getLastContainerPos();
        if (pos == null) {
            didigetrobbed$currentChestPos = null;
            didigetrobbed$currentChestName = null;
            return;
        }

        didigetrobbed$currentChestPos = pos;
        didigetrobbed$currentChestName = title;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!didigetrobbed$hasChecked && didigetrobbed$currentChestPos != null) {
            didigetrobbed$hasChecked = true;
            didigetrobbed$missingItems = didigetrobbed$loadMissingItems(didigetrobbed$currentChestPos, didigetrobbed$currentChestName);
        }

        if (didigetrobbed$missingItems == null || didigetrobbed$missingItems.isEmpty()) return;

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
    private Map<Integer, ItemStack> didigetrobbed$loadMissingItems(BlockPos pos, String containerName) {
        Map<Integer, ItemStack> missingItems = new HashMap<>();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return missingItems;

        try {

            Path file = didigetrobbed$getStoragePath(client);

            if (!Files.exists(file)) return missingItems;

            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            String world = client.world.getRegistryKey().getValue().toString();
            String chestId = world + "@" + pos.getX() + "," + pos.getY() + "," + pos.getZ();

            if (!root.has(chestId)) return missingItems;

            JsonObject chest = root.getAsJsonObject(chestId);
            if (!chest.has("items")) return missingItems;

            JsonArray savedItems = chest.getAsJsonArray("items");
            int containerSlots = handler.slots.size() - 36;
            List<String> missingReport = new ArrayList<>();

            for (JsonElement element : savedItems) {
                JsonObject obj = element.getAsJsonObject();
                if (!obj.has("slot")) continue;

                int savedSlot = obj.get("slot").getAsInt();
                String itemId = obj.get("id").getAsString();
                int savedCount = obj.get("count").getAsInt();

                if (savedSlot >= containerSlots) continue;

                Slot slot = handler.getSlot(savedSlot);

                if (!slot.hasStack()) {
                    Identifier id = Identifier.of(itemId);
                    missingItems.put(savedSlot, new ItemStack(Registries.ITEM.get(id), savedCount));
                    missingReport.add("§c- " + savedCount + "x " + Registries.ITEM.get(id).getName().getString());
                } else {
                    ItemStack currentStack = slot.getStack();
                    String currentItemId = Registries.ITEM.getId(currentStack.getItem()).toString();
                    int currentCount = currentStack.getCount();

                    if (currentItemId.equals(itemId)) {
                        if (currentCount < savedCount) {
                            int missing = savedCount - currentCount;
                            Identifier id = Identifier.of(itemId);
                            missingItems.put(savedSlot, new ItemStack(Registries.ITEM.get(id), missing));
                            missingReport.add("§c- " + missing + "x " + Registries.ITEM.get(id).getName().getString());
                        }
                    } else {
                        Identifier id = Identifier.of(itemId);
                        missingItems.put(savedSlot, new ItemStack(Registries.ITEM.get(id), savedCount));
                        missingReport.add("§c- " + savedCount + "x " + Registries.ITEM.get(id).getName().getString());
                    }
                }
            }

            if (!missingReport.isEmpty()) {
                client.player.sendMessage(Text.literal("§6[DidIGetRobbed] §fItems missing from " + containerName + ":"), false);
                for (String line : missingReport) {
                    client.player.sendMessage(Text.literal(line), false);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return missingItems;
    }

    @Unique
    private Path didigetrobbed$getStoragePath(MinecraftClient client) {

        if (client.isInSingleplayer() && client.getServer() != null) {
            return client.getServer()
                    .getSavePath(WorldSavePath.ROOT)
                    .resolve("didigetrobbed")
                    .resolve("chests.json");
        }

        if (client.getCurrentServerEntry() != null) {
            String serverAddress = client.getCurrentServerEntry().address;

            String worldIdentifier = client.world.getRegistryKey().getValue().toString();

            String rawIdentifier = serverAddress + "_" + worldIdentifier;
            String sanitized = rawIdentifier.replaceAll("[^a-zA-Z0-9._-]", "_");

            return client.runDirectory.toPath()
                    .resolve("didigetrobbed")
                    .resolve("chests_" + sanitized + ".json"); //
        }

        return client.runDirectory.toPath()
                .resolve("didigetrobbed")
                .resolve("chests_local.json");
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