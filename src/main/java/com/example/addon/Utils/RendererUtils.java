package com.example.addon.Utils;



import java.util.ArrayList;
import java.util.List;

import javax.lang.model.util.Elements.Origin;

import org.checkerframework.checker.units.qual.radians;

import javassist.expr.NewArray;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.Renderer3D;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import oshi.driver.windows.perfmon.PhysicalDisk;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;

// import static meteordevelopment.meteorclient.MeteorClient.mc;

public class RendererUtils extends Module{
    public static List<BlockObj> blockobjs = new ArrayList<>();
    public static List<pointObj> pointobjs = new ArrayList<>();
    // private Pool<BlockObj> blockPool = new Pool<>(BlockObj::new);

    private final SettingGroup sgRender = settings.getDefaultGroup();
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
    .name("shape-mode")
    .description("How the shapes are rendered.")
    .defaultValue(ShapeMode.Both)
    .build()
);

private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
    .name("side-color")
    .description("The color of the sides of the blocks being rendered.")
    .defaultValue(new SettingColor(204, 0, 0, 10))
    .build()
);

private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
    .name("line-color")
    .description("The color of the lines of the blocks being rendered.")
    .defaultValue(new SettingColor(204, 0, 0, 255))
    .build()
);



private final Setting<SettingColor> c1 = sgRender.add(new ColorSetting.Builder()
    .name("color-1")
    .description("The color of the lines of the blocks being rendered.")
    .defaultValue(new SettingColor(255, 255, 255, 255))
    .build()
);
private final Setting<SettingColor> c2 = sgRender.add(new ColorSetting.Builder()
    .name("color-2")
    .description("The color of the lines of the blocks being rendered.")
    .defaultValue(new SettingColor(255, 255, 255, 255))
    .build()
);
private final Setting<SettingColor> c3 = sgRender.add(new ColorSetting.Builder()
    .name("color-3")
    .description("The color of the lines of the blocks being rendered.")
    .defaultValue(new SettingColor(255, 255, 255, 255))
    .build()
);
private final Setting<SettingColor> c4 = sgRender.add(new ColorSetting.Builder()
    .name("color-4")
    .description("The color of the lines of the blocks being rendered.")
    .defaultValue(new SettingColor(255, 255, 255, 255))
    .build()
);

private final Setting<Double> pointSize = sgRender.add(new DoubleSetting.Builder()
    .name("point size")
    .description("how much to predict when the player is falling.")
    .defaultValue(0.1)
    .build()
);

private final Setting<Integer> delay = sgRender.add(new IntSetting.Builder()
        .name("delay")
        .defaultValue(60)
        .range(1, 200)
        .sliderRange(1, 200)
        .build()
    );

    public RendererUtils() {
        super(Categories.Render, "RenderUtils", "utils for simple rendering");
    }

    public class renderobj{
        public int time;
        public Color color;
        public boolean shouldRemove(){
            time++;
            return time >= 10;
        }
    }

    public class pointObj extends renderobj{
        public Vec3d pos;

        public pointObj(Vec3d pos, Color color){
            this.pos = pos;
            this.color = color;
        }

        public void render(Render3DEvent event){
            double x1 = pos.getX() + pointSize.get();
            double y1 = pos.getY() + pointSize.get();
            double z1 = pos.getZ() + pointSize.get();
            double x2 = pos.getX() - pointSize.get();
            double y2 = pos.getY() - pointSize.get();
            double z2 = pos.getZ() - pointSize.get();

            event.renderer.box(x1, y1, z1, x2, y2, z2, color, lineColor.get(), shapeMode.get(), 0);
        }
    }
    public class BlockObj extends renderobj{
        public BlockPos bp;

        public int time;

        public BlockObj(BlockPos bp, Color color){
            this.bp = bp;
            this.color = color;
        }

        public void render(Render3DEvent event){
            VoxelShape shape = mc.world.getBlockState(bp).getOutlineShape(mc.world, bp);

            double x1 = bp.getX();
            double y1 = bp.getY();
            double z1 = bp.getZ();
            double x2 = bp.getX() + 1;
            double y2 = bp.getY() + 1;
            double z2 = bp.getZ() + 1;

            if (!shape.isEmpty()) {
                x1 = bp.getX() + shape.getMin(Direction.Axis.X);
                y1 = bp.getY() + shape.getMin(Direction.Axis.Y);
                z1 = bp.getZ() + shape.getMin(Direction.Axis.Z);
                x2 = bp.getX() + shape.getMax(Direction.Axis.X);
                y2 = bp.getY() + shape.getMax(Direction.Axis.Y);
                z2 = bp.getZ() + shape.getMax(Direction.Axis.Z);
            }
            event.renderer.box(x1, y1, z1, x2, y2, z2, color, lineColor.get(), shapeMode.get(), 0);


        }


    }

    public static void addBlock(BlockPos bp, Color color){
        blockobjs.add(new RendererUtils().new BlockObj(bp, color));
    }
    public static void addPoint(Vec3d pos, Color color){
        pointobjs.add(new RendererUtils().new pointObj(pos, color));
    }

    public static void renderLine(Renderer3D renderer, Vec3d v1, Vec3d v2, Color c){
        renderer.line(
            v1.x, v1.y, v1.z,
            v2.x, v2.y, v2.z,
            c
            );
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        // blockobjs = new ArrayList<>();

        Renderer3D renderer = event.renderer;
        for (BlockObj block : blockobjs) block.render(event);
        for (pointObj point : pointobjs) point.render(event);

        // renderLine(getPos(mc.player, event), getPos(mc.player, event).add(5, 0, 0), event);

        // Logger.TickLog(""+Math.sin(1));



        Vec3d Origin = Vec3d.ZERO.add(getPos(mc.player, event));//.add(0, mc.player.getHealth() / mc.player.getMaxHealth(), 0);
        // final int NUM_POINTS = 100;
        // final double RADIUS = 1d;

        Color bottomColor = new Color(Color.CYAN);
        Color topColor = new Color(Color.CYAN);
        topColor.a(0);
        bottomColor.a(100);

        renderCircel(renderer, 1d, 100, Origin, c1.get());


        // renderquad(
        //     event.renderer,
        //     getPos(mc.player, event), getPos(mc.player, event).add(1, 0, 0), getPos(mc.player, event).add(1, 0, 2), getPos(mc.player, event).add(0, 0, 2),
        //     c1.get(), c2.get(), c3.get(), c4.get());

        // renderGradientCylinder(event.renderer, 1d, 100, Origin, 0.2f, topColor, bottomColor);
        // renderGradientCirkel(event.renderer, 3d, 2.5d, 100, Origin, c1.get(), c2.get(), 0.1);
        // renderGradientCirkel(null, 0, 0, Origin);

    }
    public static void renderGradientCirkel(Renderer3D renderer, double r1, double r2, int NUM_POINTS, Vec3d Origin, Color innerColor, Color outerColor){
        renderGradientCirkel(renderer, r1, r2, NUM_POINTS, Origin, innerColor, outerColor, 1);
    }

    public static void renderGradientCirkel(Renderer3D renderer, double r1, double r2, int NUM_POINTS, Vec3d Origin, Color innerColor, Color outerColor, double t){
        renderGradientCirkel(renderer, r1, r2, NUM_POINTS, Origin, innerColor, outerColor, t, false);
    }
    public static void renderGradientCirkel(Renderer3D renderer, double r1, double r2, int NUM_POINTS, Vec3d Origin, Color innerColor, Color outerColor, double t, boolean rainbow){


        for (int i = 0; i < NUM_POINTS; ++i)
        {
            final double a1 = ((double) i / NUM_POINTS) * 360d;
            final double a2 = ((double) (i+1) / NUM_POINTS) * 360d;
            final Vec3d p1 = getCirclePoint(Math.toRadians(a1), r1).add(Origin); //.add(getPos(mc.player, event)).add(0, mc.player.getHealth()/mc.player.getMaxHealth(), 0);
            final Vec3d p2 = getCirclePoint(Math.toRadians(a2), r1).add(Origin);
            final Vec3d p3 = getCirclePoint(Math.toRadians(a1), r2).add(Origin);
            final Vec3d p4 = getCirclePoint(Math.toRadians(a2), r2).add(Origin);

            renderquad(
                renderer,
                p1, p2, p4, p3,
                t(outerColor, t),
                t(rainbow ? hsvToRgb(a1, 100, 100) : innerColor, t),
                t(rainbow ? hsvToRgb(((double) (i-1) / NUM_POINTS) * 360d, 100, 100) : innerColor, t),
                t(outerColor, t));
            // renderLine(p1.add(getPos(mc.player, event)), p2.add(getPos(mc.player, event)), event);

            // event.renderer.gradientQuadVertical(
            //     p1.x, p1.y, p1.z,
            //     p2.x, p2.y + 0.7, p2.z,
            //     topColor, bottomColor
            //     );

        }
    }

    public static Color t(Color c, double t){
        Color cc = new Color(c);
        cc.a((int) (c.a * t));
        return cc;
    }

    public static void renderGradientCylinder(Renderer3D renderer, double RADIUS, int NUM_POINTS, Vec3d Origin, Float height, Color topColor, Color bottomColor){
        for (int i = 0; i < NUM_POINTS; ++i)
        {

            final Vec3d p1 = getCirclePoint(Math.toRadians(((double) i / NUM_POINTS) * 360d), RADIUS).add(Origin); //.add(getPos(mc.player, event)).add(0, mc.player.getHealth()/mc.player.getMaxHealth(), 0);
            final Vec3d p2 = getCirclePoint(Math.toRadians(((double) (i+1) / NUM_POINTS) * 360d), RADIUS).add(Origin);



            // renderLine(p1.add(getPos(mc.player, event)), p2.add(getPos(mc.player, event)), event);

            renderer.gradientQuadVertical(
                p1.x, p1.y, p1.z,
                p2.x, p2.y + height, p2.z,
                topColor, bottomColor
                );
        }
    }

    public static void renderCircel(Renderer3D renderer, double RADIUS, int NUM_POINTS, Vec3d Origin, Color color){
        for (int i = 0; i < NUM_POINTS; ++i)
        {

            final Vec3d p1 = getCirclePoint(Math.toRadians(((double) i / NUM_POINTS) * 360d), RADIUS).add(Origin); //.add(getPos(mc.player, event)).add(0, mc.player.getHealth()/mc.player.getMaxHealth(), 0);
            final Vec3d p2 = getCirclePoint(Math.toRadians(((double) (i+1) / NUM_POINTS) * 360d), RADIUS).add(Origin);



            // renderLine(p1.add(getPos(mc.player, event)), p2.add(getPos(mc.player, event)), event);

            renderLine(renderer, p1, p2, color);
        }
    }

    public static Color hsvToRgb(double H, float S, float V) {

        float R, G, B;

        H /= 360f;
        S /= 100f;
        V /= 100f;

        if (S == 0)
        {
            R = V * 255;
            G = V * 255;
            B = V * 255;
        } else {
            float var_h = (float) (H * 6);
            if (var_h == 6)
                var_h = 0; // H must be < 1
            int var_i = (int) Math.floor((double) var_h); // Or ... var_i =
                                                            // floor( var_h )
            float var_1 = V * (1 - S);
            float var_2 = V * (1 - S * (var_h - var_i));
            float var_3 = V * (1 - S * (1 - (var_h - var_i)));

            float var_r;
            float var_g;
            float var_b;
            if (var_i == 0) {
                var_r = V;
                var_g = var_3;
                var_b = var_1;
            } else if (var_i == 1) {
                var_r = var_2;
                var_g = V;
                var_b = var_1;
            } else if (var_i == 2) {
                var_r = var_1;
                var_g = V;
                var_b = var_3;
            } else if (var_i == 3) {
                var_r = var_1;
                var_g = var_2;
                var_b = V;
            } else if (var_i == 4) {
                var_r = var_3;
                var_g = var_1;
                var_b = V;
            } else {
                var_r = V;
                var_g = var_1;
                var_b = var_2;
            }

            R = var_r * 255; // RGB results from 0 to 255
            G = var_g * 255;
            B = var_b * 255;

        }
        return new Color((int)R, (int)G, (int)B);
    }

    public void renderquad(Renderer3D renderer, Vec3d p1, Vec3d p2, Vec3d p3, Vec3d p4, Color color){
        renderquad(renderer, p1, p2, p3, p4, color);
    }
    public static void renderquad(Renderer3D renderer, Vec3d p1, Vec3d p2, Vec3d p3, Vec3d p4, Color topLeft, Color topRight, Color bottomRight, Color bottomLeft){
        renderer.quad(
            p1.x, p1.y, p1.z,
            p2.x, p2.y, p2.z,
            p3.x, p3.y, p3.z,
            p4.x, p4.y, p4.z,
            topLeft, topRight, bottomRight, bottomLeft);
    }


    public static Vec3d getCirclePoint(double angle, double RADIUS){
        return new Vec3d(
            Math.cos(angle) * RADIUS,
            0,
            Math.sin(angle) * RADIUS);
    }
    public static Vec3d getPos(Entity entity, Render3DEvent event){
        return new Vec3d(
        MathHelper.lerp(event.tickDelta, entity.lastRenderX, entity.getX()),
        MathHelper.lerp(event.tickDelta, entity.lastRenderY, entity.getY()),
        MathHelper.lerp(event.tickDelta, entity.lastRenderZ, entity.getZ()));

    }

    @EventHandler
    private  void onTick(TickEvent.Pre event) {
        blockobjs.removeIf(BlockObj::shouldRemove);
        pointobjs.removeIf(pointObj::shouldRemove);



        aUtils.tick();
        Logger.tick();
    }



    public enum RenderType{
        Line,
        Box,
        Block
    }
}
