package net.electricdog.didigetrobbed.mixin;

import com.google.gson.*;
import net.electricdog.didigetrobbed.ChestContext;
import net.electricdog.didigetrobbed.ChestUtils;
import net.electricdog.didigetrobbed.Config;
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
import net.minecraft.util.WorldSavePath;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.math.BigInteger;

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

        if (didigetrobbed$isChestTracked(pos)) {
            didigetrobbed$saveChest(pos, title, containerSlots);
        }

        ChestContext.setLastContainerPos(null);
    }

    @Unique
    private boolean didigetrobbed$isChestTracked(BlockPos pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return false;

        pos = ChestUtils.normalizeChestPos(pos, client.world);

        try {
            Path file = didigetrobbed$getStoragePath(client);
            if (!Files.exists(file)) return Config.getInstance().trackAllChestsByDefault;

            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            String world = client.world.getRegistryKey().getValue().toString();
            String chestId = world + "@" + pos.getX() + "," + pos.getY() + "," + pos.getZ();

            if (!root.has(chestId)) return Config.getInstance().trackAllChestsByDefault;

            JsonElement chestElement = root.get(chestId);
            if (chestElement == null || !chestElement.isJsonObject()) return Config.getInstance().trackAllChestsByDefault;

            JsonObject chest = chestElement.getAsJsonObject();
            if (!chest.has("tracking_enabled")) return Config.getInstance().trackAllChestsByDefault;

            return chest.get("tracking_enabled").getAsBoolean();
        } catch (Exception e) {
            e.printStackTrace();
            return Config.getInstance().trackAllChestsByDefault;
        }
    }

    @Unique
    private void didigetrobbed$saveChest(BlockPos pos, String name, int containerSlots) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        BlockPos[] positions = ChestUtils.getChestPositionsForCleanup(pos, client.world);
        BlockPos normalizedPos = positions[0];
        BlockPos posToCleanup = positions[1];

        try {
            Path file = didigetrobbed$getStoragePath(client);
            if (file.getParent() != null) Files.createDirectories(file.getParent());

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
            String chestId = world + "@" + normalizedPos.getX() + "," + normalizedPos.getY() + "," + normalizedPos.getZ();

            if (posToCleanup != null && !posToCleanup.equals(normalizedPos)) {
                String oldChestId = world + "@" + posToCleanup.getX() + "," + posToCleanup.getY() + "," + posToCleanup.getZ();
                if (root.has(oldChestId)) {
                    root.remove(oldChestId);
                    System.out.println("[DidIGetRobbed] Cleaned up redundant chest entry at " + oldChestId);
                }
            }

            JsonObject chest;
            if (root.has(chestId)) {
                chest = root.get(chestId).getAsJsonObject();
            } else {
                chest = new JsonObject();
                chest.addProperty("tracking_enabled", Config.getInstance().trackAllChestsByDefault);
            }

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

                    var enchants = stack.getEnchantments();
                    if (!enchants.isEmpty()) {
                        obj.addProperty("enchants", enchants.toString());
                    } else {
                        obj.addProperty("enchants", "");
                    }

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

        if (client.getNetworkHandler() != null && client.world != null) {
            try {
                String address;
                if (client.getCurrentServerEntry() != null) {
                    address = client.getCurrentServerEntry().address;
                } else {
                    address = client.getNetworkHandler().getConnection().getAddress().toString();
                }

                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] hash = md.digest(address.getBytes(StandardCharsets.UTF_8));
                String serverUid = new BigInteger(1, hash).toString(36).substring(0, 13);

                String worldName = client.world.getRegistryKey().getValue().toString().replace(":", "@@");

                return client.runDirectory.toPath()
                        .resolve("didigetrobbed")
                        .resolve("multiplayer")
                        .resolve(serverUid)
                        .resolve(worldName + ".json");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return client.runDirectory.toPath().resolve("didigetrobbed").resolve("chests_local.json");
    }

    @Unique
    private boolean didigetrobbed$isStorageContainer(String title) {
        Config config = Config.getInstance();
        String lower = title.toLowerCase();

        if (lower.contains("ender chest")) return false;

        if (config.trackChests && lower.contains("chest")) return true;
        if (config.trackBarrels && lower.contains("barrel")) return true;
        return config.trackShulkerBoxes && (lower.contains("shulker") || lower.contains("box"));
    }
}