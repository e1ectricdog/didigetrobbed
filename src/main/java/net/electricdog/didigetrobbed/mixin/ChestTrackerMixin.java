package net.electricdog.didigetrobbed.mixin;

import com.google.gson.*;
import net.electricdog.didigetrobbed.ChestContext;
import net.electricdog.didigetrobbed.ChestUtils;
import net.electricdog.didigetrobbed.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Files;
import java.nio.file.Path;

@Mixin(AbstractContainerScreen.class)
public abstract class ChestTrackerMixin {

    @Shadow @Final protected AbstractContainerMenu menu;

    @Inject(method = "onClose", at = @At("HEAD"))
    private void onContainerClose(CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.isLocalServer()) return;

        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;

        String title = screen.getTitle().getString();
        if (title == null || !didigetrobbed$isStorageContainer(title)) return;

        BlockPos pos = ChestContext.getLastContainerPos();
        if (pos == null) return;

        int totalSlots = menu.slots.size();
        int containerSlots = totalSlots - 36;
        if (containerSlots <= 0) return;

        if (didigetrobbed$isChestTracked(pos)) {
            didigetrobbed$saveChest(pos, title, containerSlots);
        }

        ChestContext.setLastContainerPos(null);
    }

    @Unique
    private boolean didigetrobbed$isChestTracked(BlockPos pos) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return false;

        pos = ChestUtils.normalizeChestPos(pos, client.level);

        try {
            Path file = didigetrobbed$getStoragePath(client);
            if (!Files.exists(file)) return Config.getInstance().trackAllChestsByDefault;

            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            String world = client.level.dimension().identifier().toString();
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
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        BlockPos[] positions = ChestUtils.getChestPositionsForCleanup(pos, client.level);
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

            String world = client.level.dimension().identifier().toString();
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
                Slot slot = menu.getSlot(i);
                if (slot.hasItem()) {
                    ItemStack stack = slot.getItem();
                    JsonObject obj = new JsonObject();

                    obj.addProperty("slot", i);
                    obj.addProperty("id", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
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
    private String didigetrobbed$getItemIdentity(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        var enchants = stack.getEnchantments();

        if (enchants.isEmpty()) {
            return id;
        }

        return id + enchants.toString();
    }

    @Unique
    private String didigetrobbed$legacyHash(String address) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(address.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return new java.math.BigInteger(1, hash).toString(36).substring(0, 13);
        } catch (Exception e) {
            return null;
        }
    }

    @Unique
    private Path didigetrobbed$getStoragePath(Minecraft client) {
        if (client.isLocalServer() && client.getSingleplayerServer() != null) {
            return client.getSingleplayerServer().getWorldPath(LevelResource.ROOT).resolve("didigetrobbed").resolve("chests.json");
        }

        if (client.getConnection() != null && client.level != null) {
            try {
                String serverUid;

                if (client.getCurrentServer() != null && client.getCurrentServer().isRealm()) {
                    String ownerUUID = ChestContext.getCurrentRealmsId();

                    if (ownerUUID != null) {
                        serverUid = "realms__" + ownerUUID.replaceAll("[^a-zA-Z0-9._-]", "_");
                    } else {
                        serverUid = "realms__unknown";
                    }
                } else if (client.getCurrentServer() != null) {

                    String address = client.getCurrentServer().ip;
                    serverUid = address.replaceAll("[^a-zA-Z0-9._-]", "_");

                    String legacyHash = didigetrobbed$legacyHash(address);
                    if (legacyHash != null) {
                        Path oldDir = client.gameDirectory.toPath().resolve("didigetrobbed").resolve("multiplayer").resolve(legacyHash);
                        Path newDir = client.gameDirectory.toPath().resolve("didigetrobbed").resolve("multiplayer").resolve(serverUid);
                        if (Files.exists(oldDir) && !Files.exists(newDir)) {
                            try {
                                Files.move(oldDir, newDir);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } else {

                    java.net.SocketAddress socketAddress = client.getConnection().getConnection().getRemoteAddress();
                    String address = (socketAddress instanceof java.net.InetSocketAddress inetAddress) ? inetAddress.getHostString() : socketAddress.toString();
                    serverUid = address.replaceAll("[^a-zA-Z0-9._-]", "_");
                }

                String worldName = client.level.dimension().identifier().toString().replace(":", "@@");
                return client.gameDirectory.toPath().resolve("didigetrobbed").resolve("multiplayer").resolve(serverUid).resolve(worldName + ".json");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return client.gameDirectory.toPath().resolve("didigetrobbed").resolve("chests_local.json");
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