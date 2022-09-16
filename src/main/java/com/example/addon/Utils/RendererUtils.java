package com.example.addon.Utils;



import java.util.ArrayList;
import java.util.List;

import javassist.expr.NewArray;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;


public class RendererUtils {
    public static List<BlockObj> blockobjs = new ArrayList<>();
    public static class BlockObj{
        public BlockPos bp;
        public SettingColor color;

        public BlockObj(BlockPos bp, Color color){

        }

        public void render(Render3DEvent event){
            double x1 = bp.getX();
            double y1 = bp.getY();
            double z1 = bp.getZ();
            double x2 = bp.getX() + 1;
            double y2 = bp.getY() + 1;
            double z2 = bp.getZ() + 1;

            event.renderer.box(bp, color, SettingColor.WHITE, ShapeMode.Sides, 0);
        }
    }

    public static void addBlock(BlockPos bp, Color color){
        blockobjs.add(new BlockObj(bp, color));
        Logger.Log("added block " + blockobjs);
        Logger.TickLog("hej");


    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        for (BlockObj block : blockobjs) block.render(event);
        Logger.TickLog("hej");
    }



    public enum RenderType{
        Line,
        Box,
        Block
    }
}
