package com.example.addon.Utils;
import net.fabricmc.loader.impl.lib.sat4j.specs.ContradictionException;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import static meteordevelopment.meteorclient.MeteorClient.mc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;
import javax.lang.model.util.ElementScanner14;
import javax.swing.text.html.HTMLDocument.BlockElement;
import javax.xml.crypto.dsig.keyinfo.RetrievalMethod;

import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import  meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;


import net.minecraft.util.hit.HitResult;
public class aUtils {
    static boolean isAccepted;

    public boolean interactBlock(FindItemResult Item, Vec3d pos) {
        return interactBlock(Item, pos, true);
    }

    public static Vec3d getEyesPos(){
        return new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());
    }

    public static class MyBlock {
        public BlockPos blockPos;

        public Block originalBlock;
        public boolean mining;
        public boolean swingHand;

        public void set(BlockPos pos) {
            this.blockPos = pos;

            this.originalBlock = mc.world.getBlockState(pos).getBlock();
            this.mining = false;
        }

        public boolean shouldRemove() {
            // Logger.Log("removed block: " + originalBlock + "bc is now " + mc.world.getBlockState(blockPos).getBlock() + " pos --> " + this.blockPos);
            return mc.world.getBlockState(blockPos).getBlock() != originalBlock;
        }

        public void mine(boolean rotate) {
            if (!mining) {
                mc.player.swingHand(Hand.MAIN_HAND);
                mining = true;
            }
            if (rotate) Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), 50, this::updateBlockBreakingProgress);
            else updateBlockBreakingProgress();
        }

        private void updateBlockBreakingProgress() {
            BlockUtils.breakBlock(blockPos, swingHand);
        }

        // public void render(Render3DEvent event) {
        //     VoxelShape shape = mc.world.getBlockState(blockPos).getOutlineShape(mc.world, blockPos);

        //     double x1 = blockPos.getX();
        //     double y1 = blockPos.getY();
        //     double z1 = blockPos.getZ();
        //     double x2 = blockPos.getX() + 1;
        //     double y2 = blockPos.getY() + 1;
        //     double z2 = blockPos.getZ() + 1;

        //     if (!shape.isEmpty()) {
        //         x1 = blockPos.getX() + shape.getMin(Direction.Axis.X);
        //         y1 = blockPos.getY() + shape.getMin(Direction.Axis.Y);
        //         z1 = blockPos.getZ() + shape.getMin(Direction.Axis.Z);
        //         x2 = blockPos.getX() + shape.getMax(Direction.Axis.X);
        //         y2 = blockPos.getY() + shape.getMax(Direction.Axis.Y);
        //         z2 = blockPos.getZ() + shape.getMax(Direction.Axis.Z);
        //     }

        //     event.renderer.box(x1, y1, z1, x2, y2, z2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);


        }

    public static boolean interactBlock(FindItemResult Item, Vec3d pos, Boolean swapback) {

        if (!Item.found()) return false;




        // if (!placedWater) isBlock=false;
        // if(PlayerUtils.distanceTo(pos) > mc.interactionManager.getReachDistance()) return false;


        Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), 100, true, () -> {


            if (Item.isOffhand()) {
                mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
            }
            else {
                // int preSlot = mc.player.getInventory().selectedSlot;
                InvUtils.swap(Item.slot(), true);

                isAccepted = (mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(pos, Direction.UP, new BlockPos(pos), false)).isAccepted());
                if (swapback) InvUtils.swapBack();
            }



        });

        return isAccepted;
    }

    public static Vec3d getpos(BlockPos p){
        return new Vec3d(
            p.getX(),
            p.getY(),
            p.getZ());
    }
    public static Double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }

    public static Vec3d clamp3d(Vec3d val, BlockPos bPos, double size){
        return new Vec3d(
            clamp(val.getX(), bPos.getX(), bPos.getX() + size),
            clamp(val.getY(), bPos.getY(), bPos.getY() + size),
            clamp(val.getZ(), bPos.getZ(), bPos.getZ() + size));
    }

    public static Vec3d closestPointOnBlock(BlockPos bPos){
        return clamp3d(mc.player.getEyePos(), bPos, 0.999999);
    }



    public static boolean isReachable(Vec3d p){
        BlockHitResult hit = mc.world.raycast(new RaycastContext(getEyesPos(), p, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));

        if(hit.getType() != HitResult.Type.MISS){
            if (p.distanceTo(hit.getPos()) > 0.2){

                // Logger.Log("" + p.distanceTo(hit.getPos()));

                return false;
            }
        }
        return mc.player.getEyePos().distanceTo(p) < mc.interactionManager.getReachDistance();

    }

    public static boolean isReachable(BlockPos bp){
        if (isReachable(closestPointOnBlock(bp))){
            RendererUtils.addBlock(bp, Color.GREEN.a(50));
        }
        else {
            RendererUtils.addBlock(bp, Color.RED.a(50));

        }
        return isReachable(closestPointOnBlock(bp));
        // return mc.player.getEyePos().distanceTo(closestPointOnBlock(bp)) < mc.interactionManager.getReachDistance();
    }

    public static List<BlockPos> findblocksnearplayer(List<Block> sblocks, int depth){
        return findblocksnearplayer(sblocks, depth, false);
    }
    public static List<BlockPos> findblocksnearplayer(List<Block> sblocks, int depth, Boolean topY){
        return findblocksnearplayer(sblocks, depth, topY, false, null);
    }
    public static List<BlockPos> findblocksnearplayer(Block sblock, int depth, Boolean topY, Boolean slow){

        return findblocksnearplayer(Arrays.asList(sblock), depth, topY, slow, null);
    }
    public interface blockFunction {
        boolean run(BlockPos bp);
    }

    public static List<BlockPos> findblocksnearplayer(List<Block> sblocks, int depth, Boolean topY, Boolean slow, @Nullable blockFunction bpf){
        BlockPos.Mutable bp = new BlockPos.Mutable();

        List<BlockPos> Blocks = new ArrayList<>();

        for (int x = (int) (mc.player.getX() - depth - 1); x < mc.player.getX() + depth; x++) {
            for (int z = (int) (mc.player.getZ() - depth); z < mc.player.getZ() + depth; z++) {
                for (int y = (int) Math.min(mc.world.getTopY(), mc.player.getY() + depth); y > Math.max(mc.world.getBottomY(), mc.player.getY() - depth); y--) {
                    bp.set(x, y, z);

                    if(topY && !mc.world.getBlockState(bp.up()).isAir()) continue;
                    if (!sblocks.contains(mc.world.getBlockState(bp).getBlock())) continue;

                    if(!isReachable(new BlockPos(bp))) continue;

                    if(bpf != null) if(!bpf.run(new BlockPos(bp))) continue;

                    Blocks.add(new BlockPos(bp));
                    if(slow) return Blocks;


                }
            }
        }
        return Blocks;
    }


}

