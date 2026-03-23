package net.electricdog.didigetrobbed.mixin;

import net.electricdog.didigetrobbed.ChestContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.realms.dto.RealmsServer;
import net.minecraft.client.realms.gui.screen.RealmsMainScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RealmsMainScreen.class)
public class RealmsJoinMixin {

    @Inject(method = "play", at = @At("HEAD"))
    private static void didigetrobbed$captureRealmsId(RealmsServer server, Screen lastScreen, CallbackInfo ci) {
        if (server != null && server.ownerUUID != null) {
            String combined = server.ownerUUID + "_" + server.id;
            ChestContext.setCurrentRealmsId(combined);
        }
    }
}