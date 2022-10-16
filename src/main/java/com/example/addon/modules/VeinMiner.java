/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package com.example.addon.modules;

import com.google.common.collect.Sets;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalBlock;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.predicate.entity.EntityTypePredicate;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.shape.VoxelShape;

import com.example.addon.Utils.Logger;
import com.example.addon.Utils.aUtils;
import com.example.addon.mixin.breakingblockmixin;
import com.example.addon.modules.NoFall;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;

import javax.annotation.Nullable;
import org.apache.commons.logging.LogFactory;

import com.example.addon.Utils.aUtils.MyBlock;

public class VeinMiner extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Set<Vec3i> blockNeighbours = Sets.newHashSet(
        new Vec3i(1, -1, 1), new Vec3i(0, -1, 1), new Vec3i(-1, -1, 1),
        new Vec3i(1, -1, 0), new Vec3i(0, -1, 0), new Vec3i(-1, -1, 0),
        new Vec3i(1, -1, -1), new Vec3i(0, -1, -1), new Vec3i(-1, -1, -1),

        new Vec3i(1, 0, 1), new Vec3i(0, 0, 1), new Vec3i(-1, 0, 1),
        new Vec3i(1, 0, 0), new Vec3i(-1, 0, 0),
        new Vec3i(1, 0, -1), new Vec3i(0, 0, -1), new Vec3i(-1, 0, -1),

        new Vec3i(1, 1, 1), new Vec3i(0, 1, 1), new Vec3i(-1, 1, 1),
        new Vec3i(1, 1, 0), new Vec3i(0, 1, 0), new Vec3i(-1, 1, 0),
        new Vec3i(1, 1, -1), new Vec3i(0, 1, -1), new Vec3i(-1, 1, -1)
    );

    private final Setting<ModuleModeList> ModuleMode = sgGeneral.add(new EnumSetting.Builder<ModuleModeList>()
        .name("Mode")
        .description("Module mode")
        .defaultValue(ModuleModeList.vein)
        .build()
    );
    // General
    private final Setting<List<Block>> selectedBlocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("blocks")
        .description("Which blocks to select.")
        .defaultValue(Blocks.STONE, Blocks.DIRT, Blocks.GRASS)
        .build()
    );




    private final Setting<ListMode> listmode = sgGeneral.add(new EnumSetting.Builder<ListMode>()
        .name("Selection mode")
        .description("Selection mode.")
        .defaultValue(ListMode.Whitelist)
        .build()
    );

    private final Setting<Integer> depth = sgGeneral.add(new IntSetting.Builder()
        .name("depth")
        .description("Amount of iterations used to scan for similar blocks.")
        .defaultValue(3)
        .min(1)
        .sliderRange(1, 15)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay between mining blocks.")
        .defaultValue(0)
        .min(0)
        .sliderRange(0, 20)
        .build()
    );

    private final Setting<Boolean> walktoblock = sgGeneral.add(new BoolSetting.Builder()
        .name("walk to block")
        .description("if veinmeiner should walk to the first block in list")
        .defaultValue(false)
        .visible(() -> ModuleMode.get() == ModuleModeList.always)
        .build()

    );
    private final Setting<Boolean> pickup = sgGeneral.add(new BoolSetting.Builder()
        .name("pickup")
        .description("if veinmeiner should walk to item to be picked up")
        .defaultValue(false)
        .visible(() -> ModuleMode.get() == ModuleModeList.always)
        .build()

    );
    private final Setting<Double> pickupdis = sgGeneral.add(new DoubleSetting.Builder()
        .name("max pickup distance")
        .description("max distance from player where items should still be picked up")
        .defaultValue(8)
        .visible(() -> ModuleMode.get() == ModuleModeList.always)
        .build()

    );




    private final Setting<Boolean> slow = sgGeneral.add(new BoolSetting.Builder()
        .name("slow")
        .description("if enalbed it will only mine 1 block per tick")
        .defaultValue(false)
        .visible(() -> ModuleMode.get() == ModuleModeList.always)
        .build()

    );
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Sends rotation packets to the server when mining.")
        .defaultValue(true)
        .build()
    );

    // Render

    private final Setting<Boolean> swingHand = sgRender.add(new BoolSetting.Builder()
        .name("swing-hand")
        .description("Swing hand client-side.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Whether or not to render the block being mined.")
        .defaultValue(true)
        .build()
    );

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

    private final Pool<MyBlock> blockPool = new Pool<>(MyBlock::new);
    private final List<MyBlock> blocks = new ArrayList<>();
    private final List<BlockPos> foundBlockPositions = new ArrayList<>();
    private final BlockPos.Mutable bp = new BlockPos.Mutable();

    boolean isPathing = false;

    private int tick = 0;

    MyBlock block2break;



    public VeinMiner() {
        super(Categories.World, "vein-miner", "Mines all nearby blocks with this type");
    }

    @Override
    public void onDeactivate() {
        for (MyBlock block : blocks) blockPool.free(block);
        blocks.clear();
        foundBlockPositions.clear();
        Logger.Log(""+Blocks.STONE);
    }

    private boolean isMiningBlock(BlockPos pos) {
        for (MyBlock block : blocks) {
            if (block.blockPos.equals(pos)) return true;
        }

        return false;
    }
    private boolean isMiningBlock() {
        for (MyBlock block : blocks) {
            return true;
        }

        return false;
    }

    @EventHandler
    private void onStartBreakingBlock(StartBreakingBlockEvent event) {
        BlockState state = mc.world.getBlockState(event.blockPos);

        if (state.getHardness(mc.world, event.blockPos) < 0)
            return;
        if (listmode.get() == ListMode.Whitelist && !selectedBlocks.get().contains(state.getBlock()))
            return;
        if (listmode.get() == ListMode.Blacklist && selectedBlocks.get().contains(state.getBlock()))
            return;

        foundBlockPositions.clear();

        if (!isMiningBlock(event.blockPos)) {
            MyBlock block = blockPool.get();
            block.set(event.blockPos);
            blocks.add(block);
            mineNearbyBlocks(block.originalBlock.asItem(),event.blockPos,event.direction,depth.get());
        }
    }


    @EventHandler
    private void onTick(TickEvent.Pre event) {
        // Logger.Log(breakingblockmixin.isBreaking);
        if(pickup.get()){
            Entity Item = TargetUtils.get((Entity entity) -> entity.getType() == EntityType.ITEM, SortPriority.LowestDistance);

            if(Item != null) if (Item.distanceTo(mc.player) <= pickupdis.get()){
                BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(new BlockPos(Item.getPos().add(0, 0.2, 0))));

            } else if(BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing()) {

                BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();

            }

        }

        if(ModuleMode.get() == ModuleModeList.always)
        {
            foundBlockPositions.clear();

            if (!isMiningBlock()) {

                findblocksnearplayer();
                block2break = new MyBlock(aUtils.findblocksnearplayer(selectedBlocks.get(), 5).get(0));




            }
        }
        blocks.removeIf(MyBlock::shouldRemove);

        if (!blocks.isEmpty()) {
            if (tick < delay.get() && !blocks.get(0).mining) {
                tick++;
                return;
            }
            tick = 0;
            if(!aUtils.isBreaking){

                blocks.get(0).mine(rotate.get());
                aUtils.setIsBreaking(false);
            }

            else{
                Logger.Log("dghds");
            }

        }


    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (render.get()) {
            for (MyBlock block : blocks) block.render(event, sideColor.get(), lineColor.get(), shapeMode.get());
        }
    }

    // private class MyBlock {
    //     public BlockPos blockPos;
    //     public Direction direction;
    //     public Block originalBlock;
    //     public boolean mining;

    //     public void set(StartBreakingBlockEvent event) {
    //         this.blockPos = event.blockPos;
    //         this.direction = event.direction;
    //         this.originalBlock = mc.world.getBlockState(blockPos).getBlock();
    //         this.mining = false;
    //     }

    //     public void set(BlockPos pos, Direction dir) {
    //         this.blockPos = pos;
    //         this.direction = dir;
    //         this.originalBlock = mc.world.getBlockState(pos).getBlock();
    //         this.mining = false;
    //     }

    //     public boolean shouldRemove() {
    //         // Logger.Log("removed block: " + originalBlock + "bc is now " + mc.world.getBlockState(blockPos).getBlock() + " pos --> " + this.blockPos);
    //         return mc.world.getBlockState(blockPos).getBlock() != originalBlock || Utils.distance(mc.player.getX() - 0.5, mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ() - 0.5, blockPos.getX() + direction.getOffsetX(), blockPos.getY() + direction.getOffsetY(), blockPos.getZ() + direction.getOffsetZ()) > mc.interactionManager.getReachDistance();
    //     }

    //     public void mine() {
    //         if (!mining) {
    //             mc.player.swingHand(Hand.MAIN_HAND);
    //             mining = true;
    //         }
    //         if (rotate.get()) Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), 50, this::updateBlockBreakingProgress);
    //         else updateBlockBreakingProgress();
    //     }

    //     private void updateBlockBreakingProgress() {
    //         BlockUtils.breakBlock(blockPos, swingHand.get());
    //     }

    //     public void render(Render3DEvent event) {
    //         VoxelShape shape = mc.world.getBlockState(blockPos).getOutlineShape(mc.world, blockPos);

    //         double x1 = blockPos.getX();
    //         double y1 = blockPos.getY();
    //         double z1 = blockPos.getZ();
    //         double x2 = blockPos.getX() + 1;
    //         double y2 = blockPos.getY() + 1;
    //         double z2 = blockPos.getZ() + 1;

    //         if (!shape.isEmpty()) {
    //             x1 = blockPos.getX() + shape.getMin(Direction.Axis.X);
    //             y1 = blockPos.getY() + shape.getMin(Direction.Axis.Y);
    //             z1 = blockPos.getZ() + shape.getMin(Direction.Axis.Z);
    //             x2 = blockPos.getX() + shape.getMax(Direction.Axis.X);
    //             y2 = blockPos.getY() + shape.getMax(Direction.Axis.Y);
    //             z2 = blockPos.getZ() + shape.getMax(Direction.Axis.Z);
    //         }

    //         event.renderer.box(x1, y1, z1, x2, y2, z2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);


    //     }
    // }

    // public void findblocksnearplayer(){


    //     for (int x = (int) (mc.player.getX() - depth.get()); x < mc.player.getX() + depth.get(); x++) {
    //         for (int z = (int) (mc.player.getZ() - depth.get()); z < mc.player.getZ() + depth.get(); z++) {
    //             for (int y = (int) Math.max(mc.world.getBottomY(), mc.player.getY() - depth.get()); y < Math.min(mc.world.getTopY(), mc.player.getY() + depth.get()); y++) {
    //                 bp.set(x, y, z);

    //                 if (selectedBlocks.get().contains(mc.world.getBlockState(bp).getBlock())) {


    //                     MyBlock block = blockPool.get();

    //                     block.set(new BlockPos(bp));
    //                     if(!block.shouldRemove()){

    //                         blocks.add(block);
    //                         if(slow.get()) return;
    //                     }

    //                 }
    //             }
    //         }
    //     }





    private void mineNearbyBlocks(@Nullable Item item, BlockPos pos, Direction dir, int depth) {
        if (depth<=0) return;
        if (foundBlockPositions.contains(pos)) return;
        foundBlockPositions.add(pos);
        if (Utils.distance(mc.player.getX() - 0.5, mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ() - 0.5, pos.getX(), pos.getY(), pos.getZ()) > mc.interactionManager.getReachDistance()) return;
        for(Vec3i neighbourOffset: blockNeighbours) {
            BlockPos neighbour = pos.add(neighbourOffset);
            switch(ModuleMode.get()) {
                case vein:
                    if (mc.world.getBlockState(neighbour).getBlock().asItem() == item) {
                        MyBlock block = blockPool.get();
                        block.set(neighbour);
                        blocks.add(block);
                        mineNearbyBlocks(item, neighbour, dir, depth-1);
                    }
                case always:
                    for (Block _block : selectedBlocks.get()) {
                        if (mc.world.getBlockState(neighbour).getBlock().asItem() == _block.asItem()) {
                            MyBlock block = blockPool.get();
                            block.set(neighbour);
                            blocks.add(block);
                            mineNearbyBlocks(item, neighbour, dir, depth-1);

                    }
                }
            }
        }
    }

    @Override
    public String getInfoString() {
        return listmode.get().toString() + " (" + selectedBlocks.get().size() + ")";
    }

    public enum ListMode {
        Whitelist,
        Blacklist
    }
    public enum ModuleModeList {
        vein,
        always
    }
}
