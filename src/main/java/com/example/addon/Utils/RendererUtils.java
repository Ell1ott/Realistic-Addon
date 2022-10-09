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

private final Setting<Double> pointSize = sgRender.add(new DoubleSetting.Builder()
    .name("point size")
    .description("how much to predict when the player is falling.")
    .defaultValue(0.1)
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

    public static void renderLine(Vec3d v1, Vec3d v2, Render3DEvent event){
        event.renderer.line(
            v1.x, v1.y, v1.z,
            v2.x, v2.y, v2.z,
            Color.BLACK
            );
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        // blockobjs = new ArrayList<>();
        for (BlockObj block : blockobjs) block.render(event);
        for (pointObj point : pointobjs) point.render(event);

        renderLine(getPos(mc.player, event), getPos(mc.player, event).add(5, 0, 0), event);

        // Logger.TickLog(""+Math.sin(1));


        Vec3d lastpoint = Vec3d.ZERO;
        Vec3d point = Vec3d.ZERO;
        Vec3d Origin = Vec3d.ZERO.add(getPos(mc.player, event)).add(0, event.tickDelta, 0);
        final int NUM_POINTS = 100;
        final double RADIUS = 1d;

        Color bottomColor = new Color(Color.CYAN);
        Color topColor = new Color(Color.CYAN);
        topColor.a(0);
        bottomColor.a(100);


        for (int i = 0; i < NUM_POINTS; ++i)
        {

            final Vec3d p1 = getCirclePoint(Math.toRadians(((double) i / NUM_POINTS) * 360d), RADIUS).add(Origin); //.add(getPos(mc.player, event)).add(0, mc.player.getHealth()/mc.player.getMaxHealth(), 0);
            final Vec3d p2 = getCirclePoint(Math.toRadians(((double) (i+1) / NUM_POINTS) * 360d), RADIUS).add(Origin); //).add(getPos(mc.player, event)).add(0, mc.player.getHealth()/mc.player.getMaxHealth(), 0);

            // renderLine(p1.add(getPos(mc.player, event)), p2.add(getPos(mc.player, event)), event);
            renderquad(event.renderer, getPos(mc.player, event), getPos(mc.player, event).add(1, 0, 0), getPos(mc.player, event).add(1, 0, 2), getPos(mc.player, event).add(0, 0, 2), topColor, topColor, bottomColor, bottomColor);

            // event.renderer.gradientQuadVertical(
            //     p1.x, p1.y, p1.z,
            //     p2.x, p2.y + 0.7, p2.z,
            //     topColor, bottomColor
            //     );

        }



    }

    public void renderquad(Renderer3D renderer, Vec3d p1, Vec3d p2, Vec3d p3, Vec3d p4, Color topLeft, Color topRight, Color bottomRight, Color bottomLeft){
        renderer.quad(
            p1.x, p1.y, p1.z,
            p2.x, p2.y, p2.z,
            p3.x, p3.y, p3.z,
            p4.x, p4.y, p4.z,
            topLeft, topRight, bottomRight, bottomLeft);
    }


    public Vec3d getCirclePoint(double angle, double RADIUS){
        return new Vec3d(
            Math.cos(angle) * RADIUS,
            0,
            Math.sin(angle) * RADIUS);
    }
    public Vec3d getPos(Entity entity, Render3DEvent event){
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
