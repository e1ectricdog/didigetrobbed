package net.electricdog.didigetrobbed.mixin;

import com.google.gson.*;
import net.electricdog.didigetrobbed.ChestContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.nio.file.Files;
import java.nio.file.Path;

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

        didigetrobbed$saveChest(pos, title, containerSlots);
        ChestContext.setLastContainerPos(null);
    }

    @Unique
    private void didigetrobbed$saveChest(BlockPos pos, String name, int containerSlots) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        try {

            Path file = didigetrobbed$getStoragePath(client);

            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }

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
            for (int i = 0; i < containerSlots; i++) {
                Slot slot = handler.getSlot(i);
                if (slot.hasStack()) {
                    ItemStack stack = slot.getStack();
                    JsonObject obj = new JsonObject();
                    obj.addProperty("slot", i);
                    obj.addProperty("id", Registries.ITEM.getId(stack.getItem()).toString());
                    obj.addProperty("count", stack.getCount());
                    contents.add(obj);
                }
            }

            chest.add("items", contents);
            root.add(chestId, chest);

            Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(root));

        } catch (Exception e) {
            e.printStackTrace();
        }
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
            String sanitized = serverAddress.replaceAll("[^a-zA-Z0-9._-]", "_");
            return client.runDirectory.toPath()
                    .resolve("didigetrobbed")
                    .resolve("chests_" + sanitized + ".json");
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