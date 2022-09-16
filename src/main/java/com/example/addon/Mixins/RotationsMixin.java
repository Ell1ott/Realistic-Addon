package com.example.addon.Mixins;

import org.spongepowered.asm.mixin.Mixin;

import com.example.addon.Utils.Logger;

import meteordevelopment.meteorclient.utils.player.Rotations;

@Mixin(Rotations.class)
public class RotationsMixin {
    Logger.log("hello");
    @Override
    public static void rotate(double yaw, double pitch, int priority, boolean clientSide, Runnable callback) {

    }

}
