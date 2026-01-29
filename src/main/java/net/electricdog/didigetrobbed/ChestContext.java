package net.electricdog.didigetrobbed;

import net.minecraft.util.math.BlockPos;

public final class ChestContext {

    private static BlockPos lastContainerPos;

    public static void setLastContainerPos(BlockPos pos) {
        lastContainerPos = pos;
    }

    public static BlockPos getLastContainerPos() {
        return lastContainerPos;
    }

    private ChestContext() {}
}
