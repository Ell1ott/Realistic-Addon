package com.example.addon.modules;

import com.example.addon.Addon;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;

public class ExampleModule extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();



    public ExampleModule() {
        super(Addon.CATEGORY, "Module", "discrition");
    }



    @Override
    public void onActivate() {

    }
    @EventHandler
    private void onTick(TickEvent.Pre event) {

    }
}
