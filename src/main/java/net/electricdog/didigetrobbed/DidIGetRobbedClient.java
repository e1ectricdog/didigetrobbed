package net.electricdog.didigetrobbed;

import net.fabricmc.api.ClientModInitializer;

public class DidIGetRobbedClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Config.getInstance();

        // the screen broke and only works in 1.21.11 and can't be bothered to fix it rn
        // DidIGetRobbedCommand.register();

        System.out.println("[DidIGetRobbed] Client initialized");
    }
}