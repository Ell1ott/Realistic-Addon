package com.example.addon.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import static meteordevelopment.meteorclient.MeteorClient.mc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.text.html.HTMLDocument.BlockElement;
import javax.xml.crypto.dsig.keyinfo.RetrievalMethod;

import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import  meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.player.InvUtils;
public class aUtils {
    static boolean isAccepted;

    public boolean interactBlock(FindItemResult Hoe, Vec3d pos) {
        return interactBlock(Hoe, pos, true);
    }

    public static boolean interactBlock(FindItemResult Hoe, Vec3d pos, Boolean swapback) {

        if (!Hoe.found()) return false;

        // if (!placedWater) isBlock=false;
        // if(PlayerUtils.distanceTo(pos) > mc.interactionManager.getReachDistance()) return false;


        Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), 100, true, () -> {


            if (Hoe.isOffhand()) {
                mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
            }
            else {
                // int preSlot = mc.player.getInventory().selectedSlot;
                InvUtils.swap(Hoe.slot(), true);

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
    public static float clamp(Double val,  int min, int max) {
        return Math.max(min, Math.min(max, val));
    }

    public static Vec3d clamp3d(Vec3d val, BlockPos bPos){
        return new Vec3d(clamp(mc.player.getX(), bPos.getX(), bPos.getX() + 1), )
    }



    public static boolean isReachable(BlockPos blockPos){
        Vec3d dis = new Vec3d(mc.player.getX() - 0.5, mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ() - 0.5).subtract(getpos(blockPos));
        dis = dis.normalize();

        Vec3d closestpos = new Vec3d(clamp(mc.player.getX(), blockPos.getX(), blockPos.getX()))




        // return Utils.distance(mc.player.getX() - 0.5, mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ() - 0.5, blockPos.getX() + direction.getOffsetX(), blockPos.getY() + direction.getOffsetY(), blockPos.getZ() + direction.getOffsetZ()) > mc.interactionManager.getReachDistance();
    }

    public static List<BlockPos> findblocksnearplayer(List<Block> sblocks, int depth){
        return findblocksnearplayer(sblocks, depth, false, false);
    }
    public static List<BlockPos> findblocksnearplayer(List<Block> sblocks, int depth, Boolean topY){
        return findblocksnearplayer(sblocks, depth, topY, false);
    }
    public static List<BlockPos> findblocksnearplayer(Block sblock, int depth, Boolean topY, Boolean slow){

        return findblocksnearplayer(Arrays.asList(sblock), depth, topY, slow);
    }


    public static List<BlockPos> findblocksnearplayer(List<Block> sblocks, int depth, Boolean topY, Boolean slow){
        BlockPos.Mutable bp = new BlockPos.Mutable();

        List<BlockPos> Blocks = new ArrayList<>();

        for (int x = (int) (mc.player.getX() - depth - 1); x < mc.player.getX() + depth; x++) {
            for (int z = (int) (mc.player.getZ() - depth); z < mc.player.getZ() + depth; z++) {
                for (int y = (int) Math.min(mc.world.getTopY(), mc.player.getY() + depth); y > Math.max(mc.world.getBottomY(), mc.player.getY() - depth); y--) {
                    bp.set(x, y, z);

                    // Logger.Log("" + mc.world.getBlockState(bp).getBlock());
                    if(topY && !mc.world.getBlockState(bp.up()).isAir()) continue;
                    if(PlayerUtils.distanceTo(bp) < )
                    if (sblocks.contains(mc.world.getBlockState(bp).getBlock())){

                        // block.set(new BlockPos(bp), mc.player.getMovementDirection());
                        // if(!block.shouldRemove()){

                        Blocks.add(new BlockPos(bp));
                        if(slow) return Blocks;


                    }

                }
            }
        }
        return Blocks;
    }


}
