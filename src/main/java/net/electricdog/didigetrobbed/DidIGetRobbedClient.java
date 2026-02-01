package net.electricdog.didigetrobbed;

import net.fabricmc.api.ClientModInitializer;

public class DidIGetRobbedClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Config.getInstance();

        DidIGetRobbedCommand.register();

        System.out.println("[DidIGetRobbed] Client initialized");
    }
}