package net.electricdog.didigetrobbed.mixin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.electricdog.didigetrobbed.ChestContext;
import net.electricdog.didigetrobbed.ChestUtils;
import net.electricdog.didigetrobbed.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(AbstractContainerScreen.class)
public abstract class ChestRenderMixin {

    @Shadow @Final protected AbstractContainerMenu menu;
    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow protected int imageWidth;
    @Shadow protected int imageHeight;

    @Unique private Map<Integer, ItemStack> didigetrobbed$missingItems = null;
    @Unique private boolean didigetrobbed$hasChecked = false;
    @Unique private BlockPos didigetrobbed$currentChestPos = null;
    @Unique private String didigetrobbed$currentChestName = null;
    @Unique private int didigetrobbed$ticksWaited = 0;
    @Unique private static final int TICKS_TO_WAIT = 5;
    @Unique private boolean didigetrobbed$isTracking = false;

    @Unique private static final int BUTTON_SIZE = 16;
    @Unique private static final int BUTTON_MARGIN = 4;

    @Unique private static final Identifier ICON_GREEN = Identifier.fromNamespaceAndPath("didigetrobbed", "textures/gui/icon-green.png");
    @Unique private static final Identifier ICON_RED = Identifier.fromNamespaceAndPath("didigetrobbed", "textures/gui/icon-red.png");

    @Inject(method = "init", at = @At("TAIL"))
    private void onScreenInit(CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.isLocalServer()) return;

        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        String title = screen.getTitle().getString();

        didigetrobbed$hasChecked = false;
        didigetrobbed$missingItems = null;
        didigetrobbed$ticksWaited = 0;

        if (!didigetrobbed$isStorageContainer(title)) {
            didigetrobbed$currentChestPos = null;
            didigetrobbed$currentChestName = null;
            didigetrobbed$isTracking = false;
            return;
        }

        BlockPos pos = ChestContext.getLastContainerPos();
        if (pos == null) {
            didigetrobbed$currentChestPos = null;
            didigetrobbed$currentChestName = null;
            didigetrobbed$isTracking = false;
            return;
        }

        didigetrobbed$currentChestPos = pos;
        didigetrobbed$currentChestName = title;
        didigetrobbed$isTracking = didigetrobbed$getChestTrackingState(pos);
    }

    @Unique
    private boolean didigetrobbed$isAnyItemPresent() {
        int containerSlots = menu.slots.size() - 36;
        if (containerSlots <= 0) return false;

        for (int i = 0; i < containerSlots; i++) {
            if (menu.getSlot(i).hasItem()) {
                return true;
            }
        }
        return false;
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    @Unique
    private void onRender(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!didigetrobbed$hasChecked && didigetrobbed$currentChestPos != null && didigetrobbed$isTracking) {
            didigetrobbed$ticksWaited++;

            boolean anyItemPresent = didigetrobbed$isAnyItemPresent();

            if (anyItemPresent && didigetrobbed$ticksWaited >= 1) {
                didigetrobbed$hasChecked = true;
                didigetrobbed$missingItems = didigetrobbed$loadMissingItems(didigetrobbed$currentChestPos, didigetrobbed$currentChestName);
            }

            else if (didigetrobbed$ticksWaited >= TICKS_TO_WAIT * 2) {
                didigetrobbed$hasChecked = true;
                didigetrobbed$missingItems = didigetrobbed$loadMissingItems(didigetrobbed$currentChestPos, didigetrobbed$currentChestName);
            }
        }

        if (didigetrobbed$missingItems != null && !didigetrobbed$missingItems.isEmpty()) {
            for (Map.Entry<Integer, ItemStack> entry : didigetrobbed$missingItems.entrySet()) {
                int slotIndex = entry.getKey();
                ItemStack missingStack = entry.getValue();

                if (slotIndex >= menu.slots.size()) continue;

                Slot slot = menu.getSlot(slotIndex);
                if (slot.hasItem()) continue;

                int slotX = this.leftPos + slot.x;
                int slotY = this.topPos + slot.y;

                context.item(missingStack, slotX, slotY);
                context.fill(slotX, slotY, slotX + 16, slotY + 16, 0x88FF0000);
            }
        }

        if (didigetrobbed$currentChestPos != null) {
            int containerSlots = menu.slots.size() - 36;
            Slot lastSlot = menu.getSlot(containerSlots - 1);

            int buttonX = this.leftPos - BUTTON_MARGIN - BUTTON_SIZE;
            int buttonY = this.topPos + lastSlot.y;

            Identifier icon = didigetrobbed$isTracking ? ICON_GREEN : ICON_RED;
            context.blit(RenderPipelines.GUI_TEXTURED, icon, buttonX, buttonY, 0.0f, 0.0f, BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (didigetrobbed$currentChestPos == null) return;

        int containerSlots = menu.slots.size() - 36;
        Slot lastSlot = menu.getSlot(containerSlots - 1);

        int buttonX = this.leftPos - BUTTON_MARGIN - BUTTON_SIZE;
        int buttonY = this.topPos + lastSlot.y;

        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        if (mouseX >= buttonX && mouseX <= buttonX + BUTTON_SIZE &&
                mouseY >= buttonY && mouseY <= buttonY + BUTTON_SIZE) {

            if (button == 0) {
                didigetrobbed$toggleTracking();
                cir.setReturnValue(true);
            }
        }
    }

    @Unique
    private void didigetrobbed$toggleTracking() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || didigetrobbed$currentChestPos == null) return;

        didigetrobbed$isTracking = !didigetrobbed$isTracking;
        didigetrobbed$setChestTrackingState(didigetrobbed$currentChestPos, didigetrobbed$isTracking);

        if (didigetrobbed$isTracking) {
            client.player.sendSystemMessage(Component.literal("§a[DidIGetRobbed] §fTracking enabled for this container"));
        } else {
            client.player.sendSystemMessage(Component.literal("§c[DidIGetRobbed] §fTracking disabled for this container"));
            didigetrobbed$missingItems = null;
        }
    }

    @Unique
    private boolean didigetrobbed$getChestTrackingState(BlockPos pos) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return Config.getInstance().trackAllChestsByDefault;

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
    private void didigetrobbed$setChestTrackingState(BlockPos pos, boolean enabled) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        pos = ChestUtils.normalizeChestPos(pos, client.level);

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
            String chestId = world + "@" + pos.getX() + "," + pos.getY() + "," + pos.getZ();

            JsonObject chest;
            if (root.has(chestId)) {
                chest = root.get(chestId).getAsJsonObject();
            } else {
                chest = new JsonObject();
            }

            chest.addProperty("tracking_enabled", enabled);
            root.add(chestId, chest);

            Files.writeString(file, root.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Unique
    private boolean didigetrobbed$isInventoryLoaded() {
        return didigetrobbed$isAnyItemPresent();
    }

    @Unique
    private Map<Integer, ItemStack> didigetrobbed$loadMissingItems(BlockPos pos, String containerName) {
        Map<Integer, ItemStack> missingItems = new HashMap<>();
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return missingItems;

        pos = ChestUtils.normalizeChestPos(pos, client.level);

        try {
            Path file = didigetrobbed$getStoragePath(client);
            if (!Files.exists(file)) return missingItems;

            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            String world = client.level.dimension().identifier().toString();
            String chestId = world + "@" + pos.getX() + "," + pos.getY() + "," + pos.getZ();

            if (!root.has(chestId)) return missingItems;

            JsonElement chestElement = root.get(chestId);
            if (chestElement == null || !chestElement.isJsonObject()) return missingItems;

            JsonObject chest = chestElement.getAsJsonObject();
            if (!chest.has("items")) return missingItems;

            JsonElement itemsElement = chest.get("items");
            if (itemsElement == null || !itemsElement.isJsonArray()) return missingItems;

            JsonArray savedItems = itemsElement.getAsJsonArray();
            if (savedItems == null || savedItems.isEmpty()) return missingItems;

            int containerSlots = menu.slots.size() - 36;
            List<String> missingReport = new ArrayList<>();

            Map<String, Integer> currentPool = new HashMap<>();
            for (int i = 0; i < containerSlots; i++) {
                ItemStack stack = menu.getSlot(i).getItem();
                if (!stack.isEmpty()) {
                    String identity = didigetrobbed$getItemIdentity(stack);
                    currentPool.put(identity, currentPool.getOrDefault(identity, 0) + stack.getCount());
                }
            }

            for (JsonElement element : savedItems) {
                if (element == null || !element.isJsonObject()) continue;

                JsonObject obj = element.getAsJsonObject();
                if (!obj.has("id") || !obj.has("count") || !obj.has("slot")) continue;

                String savedItemId = obj.get("id").getAsString();
                int savedCount = obj.get("count").getAsInt();
                int savedSlot = obj.get("slot").getAsInt();

                String savedEnchants = obj.has("enchants") ? obj.get("enchants").getAsString() : "";
                String savedIdentity = savedItemId + savedEnchants;

                if (savedSlot >= containerSlots) continue;

                int amountInPool = currentPool.getOrDefault(savedIdentity, 0);

                if (amountInPool >= savedCount) {
                    currentPool.put(savedIdentity, amountInPool - savedCount);
                } else {
                    int missingCount = savedCount - amountInPool;
                    currentPool.put(savedIdentity, 0);

                    Identifier itemIdentifier = Identifier.parse(savedItemId);
                    ItemStack ghostStack = new ItemStack(BuiltInRegistries.ITEM.getValue(itemIdentifier), missingCount);

                    StringBuilder reportLine = new StringBuilder("§c- ").append(missingCount).append("x ").append(ghostStack.getHoverName().getString());

                    if (!savedEnchants.isEmpty()) {
                        String formattedEnchants = didigetrobbed$formatEnchantments(savedEnchants);
                        if (!formattedEnchants.isEmpty()) {
                            reportLine.append(" §7").append(formattedEnchants);
                        }
                    }

                    missingItems.put(savedSlot, ghostStack);
                    missingReport.add(reportLine.toString());
                }
            }

            if (!missingReport.isEmpty()) {
                client.player.sendSystemMessage(Component.literal("§6[DidIGetRobbed] §fItems missing:"));
                for (String line : missingReport) client.player.sendSystemMessage(Component.literal(line));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return missingItems;
    }

    @Unique
    private String didigetrobbed$formatEnchantments(String enchantsString) {
        if (enchantsString == null || enchantsString.isEmpty()) {
            return "";
        }

        Pattern pattern = Pattern.compile("minecraft:enchantment\\s*/\\s*minecraft:([a-z_]+)\\].*?=>?(\\d+)");
        Matcher matcher = pattern.matcher(enchantsString);

        List<String> formattedEnchants = new ArrayList<>();

        while (matcher.find()) {
            String enchantName = matcher.group(1);
            String level = matcher.group(2);

            String readableName = didigetrobbed$toTitleCase(enchantName);
            String readableLevel = didigetrobbed$toRomanNumeral(Integer.parseInt(level));

            formattedEnchants.add(readableName + " " + readableLevel);
        }

        if (formattedEnchants.isEmpty()) {
            Pattern oldPattern = Pattern.compile("minecraft:([a-z_]+)=(\\d+)");
            Matcher oldMatcher = oldPattern.matcher(enchantsString);

            while (oldMatcher.find()) {
                String enchantName = oldMatcher.group(1);
                String level = oldMatcher.group(2);

                String readableName = didigetrobbed$toTitleCase(enchantName);
                String readableLevel = didigetrobbed$toRomanNumeral(Integer.parseInt(level));

                formattedEnchants.add(readableName + " " + readableLevel);
            }
        }

        if (formattedEnchants.isEmpty()) {
            return "";
        }

        return "(" + String.join(", ", formattedEnchants) + ")";
    }

    @Unique
    private String didigetrobbed$toTitleCase(String snakeCase) {
        String[] words = snakeCase.split("_");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    result.append(word.substring(1));
                }
                result.append(" ");
            }
        }

        return result.toString().trim();
    }

    @Unique
    private String didigetrobbed$toRomanNumeral(int number) {
        switch (number) {
            case 1: return "I";
            case 2: return "II";
            case 3: return "III";
            case 4: return "IV";
            case 5: return "V";
            case 6: return "VI";
            case 7: return "VII";
            case 8: return "VIII";
            case 9: return "IX";
            case 10: return "X";
            default: return String.valueOf(number);
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
        if (config.trackShulkerBoxes && (lower.contains("shulker") || lower.contains("box"))) return true;

        return false;
    }
}