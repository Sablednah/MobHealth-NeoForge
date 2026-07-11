package com.sablednah.mobhealth.neoforge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.sablednah.mobhealth.network.MobHealthNetwork;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * The {@code /mobhealth} command tree.
 *
 * <ul>
 *     <li>{@code /mobhealth reload} — op (LEVEL_GAMEMASTERS). Config is read live, so this mainly
 *         acknowledges; edits to the TOML apply on save regardless.</li>
 *     <li>{@code /mobhealth toggle [on|off]} — anyone. Mutes/unmutes the player's own displays.</li>
 * </ul>
 */
public final class MobHealthCommands {

    private MobHealthCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mobhealth")
                .then(Commands.literal("reload")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(MobHealthCommands::reload))
                .then(Commands.literal("toggle")
                        .executes(ctx -> setMute(ctx, null))
                        .then(Commands.literal("on").executes(ctx -> setMute(ctx, Boolean.FALSE)))
                        .then(Commands.literal("off").executes(ctx -> setMute(ctx, Boolean.TRUE)))));
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        // Re-sync every online player's graphical-bar gate in case the graphical/see settings changed.
        ctx.getSource().getServer().getPlayerList().getPlayers().forEach(MobHealthNetwork::sync);
        ctx.getSource().sendSuccess(
                () -> Component.literal("MobHealth configuration reloaded (settings apply live on save)."), true);
        return 1;
    }

    private static int setMute(CommandContext<CommandSourceStack> ctx, Boolean forceMuted) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        boolean muted = forceMuted != null ? forceMuted : !MobHealthPermissions.isMuted(player);
        MobHealthPermissions.setMuted(player, muted);
        MobHealthNetwork.sync(player); // muting also hides the client's graphical bars

        Component message = muted
                ? Component.literal("MobHealth displays hidden for you (chat & boss bar). "
                        + "Note: nameplates are shared and stay visible.")
                : Component.literal("MobHealth displays shown for you.");
        ctx.getSource().sendSuccess(() -> message, false);
        return 1;
    }
}
