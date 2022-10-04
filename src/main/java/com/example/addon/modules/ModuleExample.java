package com.example.addon.modules;

import com.example.addon.Addon;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;

public class ModuleExample extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .defaultValue(60)
        .range(1, 200)
        .sliderRange(1, 200)
        .build()
    );


    public ModuleExample() {
        super(Addon.CATEGORY, "example", "An example module in a custom category.");
    }

    int timer = 0;

    @Override
    public void onActivate() {
        timer = 0;

        mc.player.sendCommand("rtp", null);
    }
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        timer++;


        if(timer < delay.get()) return;
        timer = 0;


        InvUtils.drop().slotHotbar(0);



        info("hic");
        toggle();


    }
}
