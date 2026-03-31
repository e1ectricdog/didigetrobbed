package net.electricdog.didigetrobbed;

import net.minecraft.core.BlockPos;

public final class ChestContext {

    private static BlockPos lastContainerPos;

    public static void setLastContainerPos(BlockPos pos) {
        lastContainerPos = pos;
    }

    public static BlockPos getLastContainerPos() {
        return lastContainerPos;
    }

    private ChestContext() {}

    private static String currentRealmsId = null;

    public static void setCurrentRealmsId(String id) { currentRealmsId = id; }

    public static String getCurrentRealmsId() { return currentRealmsId; }
}