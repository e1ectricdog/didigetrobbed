package net.electricdog.didigetrobbed;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class DidIGetRobbedCommand {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register(DidIGetRobbedCommand::registerCommand);
    }

    private static void registerCommand(
            CommandDispatcher<FabricClientCommandSource> dispatcher,
            CommandRegistryAccess registryAccess) {

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
                Text.literal("Did I Get Robbed Config")
                        .formatted(Formatting.GOLD, Formatting.BOLD)
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

    private static MutableText createClickableOption(String name, boolean currentValue, String commandOption) {
        boolean newValue = !currentValue;
        String command = "/didigetrobbed " + commandOption + " " + newValue;

        MutableText text = Text.literal(name + ": ").formatted(Formatting.YELLOW);

        MutableText statusText = Text.literal("[" + (currentValue ? "True" : "False") + "]")
                .formatted(currentValue ? Formatting.GREEN : Formatting.RED, Formatting.BOLD)
                .styled(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                );

        return text.append(statusText);
    }
}