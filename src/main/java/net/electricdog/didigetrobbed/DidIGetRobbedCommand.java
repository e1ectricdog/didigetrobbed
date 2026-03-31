package net.electricdog.didigetrobbed;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public class DidIGetRobbedCommand {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register(DidIGetRobbedCommand::registerCommand);
    }

    private static void registerCommand(
            CommandDispatcher<FabricClientCommandSource> dispatcher,
            CommandBuildContext registryAccess) {

        dispatcher.register(literal("didigetrobbed")
                .executes(context -> {
                    showCurrentSettings(context);
                    return 1;
                })
                .then(literal("trackChests")
                        .then(argument("enabled", BoolArgumentType.bool())
                                .executes(context -> {
                                    boolean enabled = BoolArgumentType.getBool(context, "enabled");
                                    Config.getInstance().trackChests = enabled;
                                    Config.getInstance().save();
                                    showCurrentSettings(context);
                                    return 1;
                                })))
                .then(literal("trackBarrels")
                        .then(argument("enabled", BoolArgumentType.bool())
                                .executes(context -> {
                                    boolean enabled = BoolArgumentType.getBool(context, "enabled");
                                    Config.getInstance().trackBarrels = enabled;
                                    Config.getInstance().save();
                                    showCurrentSettings(context);
                                    return 1;
                                })))
                .then(literal("trackShulkerBoxes")
                        .then(argument("enabled", BoolArgumentType.bool())
                                .executes(context -> {
                                    boolean enabled = BoolArgumentType.getBool(context, "enabled");
                                    Config.getInstance().trackShulkerBoxes = enabled;
                                    Config.getInstance().save();
                                    showCurrentSettings(context);
                                    return 1;
                                })))
                .then(literal("trackAllContainersByDefault")
                        .then(argument("enabled", BoolArgumentType.bool())
                                .executes(context -> {
                                    boolean enabled = BoolArgumentType.getBool(context, "enabled");
                                    Config.getInstance().trackAllChestsByDefault = enabled;
                                    Config.getInstance().save();
                                    showCurrentSettings(context);
                                    return 1;
                                })))
        );
    }

    private static void showCurrentSettings(CommandContext<FabricClientCommandSource> context) {
        Config config = Config.getInstance();

        context.getSource().sendFeedback(
                Component.literal("Did I Get Robbed Config")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
        );

        context.getSource().sendFeedback(createClickableOption(
                "Track Chests",
                config.trackChests,
                "trackChests"
        ));

        context.getSource().sendFeedback(createClickableOption(
                "Track Barrels",
                config.trackBarrels,
                "trackBarrels"
        ));

        context.getSource().sendFeedback(createClickableOption(
                "Track Shulker Boxes",
                config.trackShulkerBoxes,
                "trackShulkerBoxes"
        ));

        context.getSource().sendFeedback(createClickableOption(
                "Track All Containers By Default",
                config.trackAllChestsByDefault,
                "trackAllContainersByDefault"
        ));
    }

    private static MutableComponent createClickableOption(String name, boolean currentValue, String commandOption) {
        boolean newValue = !currentValue;
        String command = "/didigetrobbed " + commandOption + " " + newValue;

        MutableComponent text = Component.literal(name + ": ").withStyle(ChatFormatting.YELLOW);

        MutableComponent statusText = Component.literal("[" + (currentValue ? "True" : "False") + "]")
                .withStyle(currentValue ? ChatFormatting.GREEN : ChatFormatting.RED, ChatFormatting.BOLD)
                .withStyle(style -> style
                        .withClickEvent(new ClickEvent.RunCommand(command))
                );

        return text.append(statusText);
    }
}