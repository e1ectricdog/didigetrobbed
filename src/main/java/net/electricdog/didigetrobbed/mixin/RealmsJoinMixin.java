package net.electricdog.didigetrobbed.mixin;

import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.dto.RealmsServer;
import net.electricdog.didigetrobbed.ChestContext;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RealmsMainScreen.class)
public class RealmsJoinMixin {

    @Inject(method = "play", at = @At("HEAD"))
    private static void didigetrobbed$captureRealmsId(RealmsServer server, Screen lastScreen, CallbackInfo ci) {
        if (server != null) {
            String combined = server.ownerUUID + "_" + server.id;
            ChestContext.setCurrentRealmsId(combined);
        }
    }
}