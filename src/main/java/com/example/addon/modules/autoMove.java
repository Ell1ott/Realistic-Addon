package com.example.addon.modules;

import com.example.addon.Addon;
import com.example.addon.Utils.RendererUtils;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;
import baritone.api.BaritoneAPI;
import baritone.api.utils.input.Input;

public class autoMove extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private Vec3d moveDirection;

    private int delay;

    private double x;
    private double z;

    float prevYaw;
    float prevPitch;


    public autoMove() {
        super(Addon.CATEGORY, "move", "discrition");
    }





    @Override
    public void onActivate() {

    }
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        mc.player.setYaw(0);

        if (x > z){

        }

    }
    @EventHandler
    private void onTick(TickEvent.Post event) {
        mc.player.setYaw(prevYaw);
        mc.player.setPitch(prevPitch);


    }


    @EventHandler
    private void onRender(Render3DEvent event) {
        prevYaw = mc.player.getYaw();
        prevPitch = mc.player.getPitch();
        // mc.player.travel(new Vec3d(1, 0, 0));
    }

    public void move(Vec3d dir){
        moveDirection = dir;

        x = dir.x;
        z = dir.z;
    }
}
