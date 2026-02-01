package net.electricdog.didigetrobbed;

import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

// normalizing the chest position to always return the same BlockPos for both halves of a double chest

public class ChestUtils {
    public static BlockPos normalizeChestPos(BlockPos pos, World world) {
        if (world == null || pos == null) return pos;

        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock)) {
            return pos;
        }

        ChestType chestType = state.get(ChestBlock.CHEST_TYPE);

        if (chestType == ChestType.SINGLE) {
            return pos;
        }

        Direction facing = state.get(ChestBlock.FACING);

        if (chestType == ChestType.LEFT) {
            return pos;
        } else if (chestType == ChestType.RIGHT) {
            Direction offsetDir = getLeftDirection(facing);
            return pos.offset(offsetDir);
        }

        return pos;
    }

    // clears redundant JSON data of potential previously saved single chests which are now transformed into a double chest
    // this is a really inefficient fix, but it works
    public static BlockPos[] getChestPositionsForCleanup(BlockPos pos, World world) {
        if (world == null || pos == null) return new BlockPos[]{pos, null};

        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock)) {
            return new BlockPos[]{pos, null};
        }

        ChestType chestType = state.get(ChestBlock.CHEST_TYPE);
        BlockPos normalized = normalizeChestPos(pos, world);

        if (chestType == ChestType.SINGLE) {
            return new BlockPos[]{normalized, null};
        }

        Direction facing = state.get(ChestBlock.FACING);
        Direction offsetDir = (chestType == ChestType.LEFT) ?
                getRightDirection(facing) : getLeftDirection(facing);
        BlockPos otherHalf = pos.offset(offsetDir);

        return new BlockPos[]{normalized, otherHalf};
    }

    private static Direction getRightDirection(Direction facing) {
        return switch (facing) {
            case NORTH -> Direction.EAST;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            default -> Direction.SOUTH;
        };
    }

    private static Direction getLeftDirection(Direction facing) {
        return switch (facing) {
            case NORTH -> Direction.WEST;
            case SOUTH -> Direction.EAST;
            case WEST -> Direction.SOUTH;
            default -> Direction.NORTH;
        };
    }

    public static BlockPos normalizeChestPos(BlockPos pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return pos;
        return normalizeChestPos(pos, client.world);
    }
}