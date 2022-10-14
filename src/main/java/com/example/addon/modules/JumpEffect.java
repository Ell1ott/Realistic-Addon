package com.example.addon.modules;

import com.example.addon.Addon;
import com.example.addon.Utils.Logger;
import com.example.addon.Utils.RendererUtils;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.Vec3d;

public class JumpEffect extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
        .name("speed")
        .defaultValue(1.2)
        .range(1, 10)
        .sliderRange(1, 4)
        .build()
    );
    private final Setting<Double> maxsize = sgGeneral.add(new DoubleSetting.Builder()
        .name("max-size")
        .defaultValue(0.8)
        .range(0, 30)
        .sliderRange(0, 5)
        .build()
    );
    private final Setting<Double> thicness = sgGeneral.add(new DoubleSetting.Builder()
        .name("thicness")
        .defaultValue(0.5)
        .range(0, 1)
        .sliderRange(0, 1)
        .build()
    );

    private final Setting<SettingColor> c1 = sgGeneral.add(new ColorSetting.Builder()
    .name("color-1")
    .description("The color of the lines of the blocks being rendered.")
    .defaultValue(new SettingColor(255, 255, 255, 255))
    .build()
);
private final Setting<SettingColor> c2 = sgGeneral.add(new ColorSetting.Builder()
    .name("color-2")
    .description("The color of the lines of the blocks being rendered.")
    .defaultValue(new SettingColor(255, 255, 255, 255))
    .build()
);


    public JumpEffect() {
        super(Addon.CATEGORY, "Jump-effect", "An example module in a custom category.");
    }

    int timer = 0;
    circleEffect ce;
    @Override
    public void onActivate() {
        timer = 0;

        final circleEffect ce = new circleEffect();
        ce.get();
    }
    @EventHandler
    private void onTick(TickEvent.Pre event) {



    }

    public void onJump(){
        // Logger.Log("player has jumped");
        if(mc.player.isOnGround()){

            final circleEffect ce = new circleEffect();
            ce.get();
        }


    }

    public class circleEffect {
        public int tick = 0;
        public float size = 0.01f;
        Vec3d pos = mc.player.getPos();
        public void get(){
            MeteorClient.EVENT_BUS.subscribe(this);
        }
        @EventHandler
        private void onTick(TickEvent.Pre event) {
            tick++;

        }


        @EventHandler
        private void onRender(Render3DEvent event) {
            size += speed.get() * event.frameTime;



            RendererUtils.renderGradientCirkel(
                event.renderer,
                size, size*(1-thicness.get()),
                100,
                pos,
                c1.get(), c2.get(),
                (maxsize.get()-size)/maxsize.get());

            if(size > maxsize.get()) MeteorClient.EVENT_BUS.unsubscribe(this);


        }
    }

}
