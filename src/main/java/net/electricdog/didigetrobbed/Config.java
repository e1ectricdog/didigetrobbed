package net.electricdog.didigetrobbed;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.MinecraftClient;

import java.nio.file.Files;
import java.nio.file.Path;

public class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Config instance;

    public boolean trackChests = true;
    public boolean trackBarrels = true;
    public boolean trackShulkerBoxes = true;

    public static Config getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static Config load() {
        try {
            Path configPath = getConfigPath();
            if (Files.exists(configPath)) {
                String json = Files.readString(configPath);
                return GSON.fromJson(json, Config.class);
            }
        } catch (Exception e) {
            DidIGetRobbed.LOGGER.error("[DidIGetRobbed] Failed to load config", e);
        }
        return new Config();
    }

    public void save() {
        try {
            Path configPath = getConfigPath();
            if (configPath.getParent() != null) {
                Files.createDirectories(configPath.getParent());
            }
            Files.writeString(configPath, GSON.toJson(this));
        } catch (Exception e) {
            DidIGetRobbed.LOGGER.error("[DidIGetRobbed] Failed to save config", e);
        }
    }

    private static Path getConfigPath() {
        return MinecraftClient.getInstance().runDirectory.toPath()
                .resolve("config")
                .resolve("didigetrobbed.json");
    }
}