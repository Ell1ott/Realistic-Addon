package com.example.addon.Utils;



import java.util.ArrayList;
import java.util.List;

import javassist.expr.NewArray;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import oshi.driver.windows.perfmon.PhysicalDisk;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;

import static meteordevelopment.meteorclient.MeteorClient.mc;

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

    @EventHandler
    private static void onRender(Render3DEvent event) {
        // blockobjs = new ArrayList<>();
        for (BlockObj block : blockobjs) block.render(event);
        for (pointObj point : pointobjs) point.render(event);

    }

    @EventHandler
    private static void onTick(TickEvent.Pre event) {
        blockobjs.removeIf(BlockObj::shouldRemove);
        pointobjs.removeIf(pointObj::shouldRemove);
    }



    public enum RenderType{
        Line,
        Box,
        Block
    }
}
