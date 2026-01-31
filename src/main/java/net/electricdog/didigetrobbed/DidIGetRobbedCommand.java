package net.electricdog.didigetrobbed;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandRegistryAccess;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class DidIGetRobbedCommand {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register(DidIGetRobbedCommand::registerCommand);
    }

    private static void registerCommand(
        CommandDispatcher<FabricClientCommandSource> dispatcher,
        CommandRegistryAccess registryAccess) {

        dispatcher.register(literal("didigetrobbed").executes(context -> {MinecraftClient client = MinecraftClient.getInstance();client.execute(() -> client.setScreen(new ConfigScreen(client.currentScreen)));
            return 1;
        }));
    }
}