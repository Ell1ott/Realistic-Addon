package com.example.addon.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.example.addon.Utils.Logger;

import meteordevelopment.meteorclient.utils.player.Rotations;
import com.example.addon.Utils.ConfigModifier;
import meteordevelopment.meteorclient.utils.player.Rotations;

@Mixin(Rotations.class)
public abstract class RotationsMixin {

    // @Shadow
    // Rotation r;
    // // @Overwrite
    // // public void rotate(double yaw, double pitch, int priority, boolean clientSide, Runnable callback) {
    // //     Logger.Log("h");
    // // }
    // @Overwrite
    // public static void rotate(double yaw, double pitch, int priority, boolean clientSide, Runnable callback) {
    //     Logger.Log("h"+ConfigModifier.get().predict.get());

    // }


}
