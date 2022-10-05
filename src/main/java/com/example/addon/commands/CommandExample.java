package com.example.addon.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.commands.Command;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.command.CommandSource;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class CommandExample extends Command {
    public CommandExample() {
        super("example", "Sends a message.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            info("hi");
            mc.player.sendCommand("warp end", null);
            InvUtils.drop().slotHotbar(0);
            return SINGLE_SUCCESS;
        });
    }

    // @Override
    // public void build(LiteralArgumentBuilder<CommandSource> builder) {
    //     builder.then(argument("damage", IntegerArgumentType.integer(1, 7)).executes(context -> {
    //         int amount = IntegerArgumentType.getInteger(context, "damage");

    //         if (mc.player.getAbilities().invulnerable) {
    //             throw INVULNERABLE.create();
    //         }

    //         damagePlayer(amount);
    //         return SINGLE_SUCCESS;
    //     }));



    @EventHandler
    private void onTick(TickEvent.Pre event) {
        info("hi");
    }
}
