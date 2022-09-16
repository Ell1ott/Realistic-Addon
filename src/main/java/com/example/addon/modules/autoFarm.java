/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package com.example.addon.modules;






import net.minecraft.block.Block;
import meteordevelopment.meteorclient.events.entity.DamageEvent;
// import meteordevelopment.meteorclient.events.entity.player.BreakBlockEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
// import meteordevelopment.meteorclient.systems.modules.combat.Offhand.Item;
import meteordevelopment.meteorclient.systems.modules.movement.Flight;
// import meteordevelopment.meteorclient.systems.modules.player.Reach;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.resource.ResourceManager.Empty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import net.minecraft.util.math.Direction;

import com.example.addon.Utils.Logger;
import com.google.errorprone.annotations.Var;


import net.minecraft.block.Blocks;

import net.minecraft.entity.Entity;

import net.minecraft.block.LeavesBlock;

import net.minecraft.item.HoeItem;

import com.example.addon.Utils.RendererUtils;

import com.example.addon.Utils.aUtils;
// import com.example.addon.Utils.BlockUtils;

public class autoFarm extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("The way you are saved from fall damage.")
        .defaultValue(Mode.MLG)
        .build()
    );

    private final Setting<PlaceMode> airPlaceMode = sgGeneral.add(new EnumSetting.Builder<PlaceMode>()
        .name("place-mode")
        .description("Whether place mode places before you die or before you take damage.")
        .defaultValue(PlaceMode.BeforeDeath)
        .visible(() -> mode.get() == Mode.AirPlace)
        .build()
    );

    private final Setting<Boolean> anchor = sgGeneral.add(new BoolSetting.Builder()
        .name("anchor")
        .description("Centers the player and reduces movement when using bucket or air place mode.")
        .defaultValue(false)
        .visible(() -> mode.get() != Mode.Packet)
        .build()
    );
    private final Setting<Double> predict = sgGeneral.add(new DoubleSetting.Builder()
        .name("predict")
        .description("how much to predict when the player is falling.")
        .defaultValue(1)
        .visible(() -> mode.get() == Mode.MLG)
        .build()
    );

    private boolean placedWater = false;
    public Vec3d placedpos;
    private int preBaritoneFallHeight;
    public Item[] cluchItems = {Items.LAVA_BUCKET, Items.WATER_BUCKET, Items.POWDER_SNOW_BUCKET, Items.HAY_BLOCK, Items.SLIME_BLOCK};
    public static final Set<Block> cBlocks = Set.of(Blocks.SLIME_BLOCK, Blocks.POWDER_SNOW, Blocks.HAY_BLOCK);
    // public Block[] cBlocks = {Blocks.SLIME_BLOCK, Blocks.POWDER_SNOW, Blocks.HAY_BLOCK};
    public boolean isBlock;

    public autoFarm() {
        super(Categories.Movement, "autoFarm", "auto farms stuff");
    }

    @Override
    public void onActivate() {
        Logger.Log("hejs");
        placedpos = mc.player.getPos().subtract(0, 0.7, 0);
        Logger.Log("" + aUtils.findblocksnearplayer(Blocks.GRASS_BLOCK, 5, false, true));

        RendererUtils.addBlock(new BlockPos(0, 72, 0), Color.BLUE);
        // Vec3d pos = aUtils.getpos(aUtils.findblocksnearplayer(Blocks.GRASS_BLOCK, 3, true, true).get(0));
        // aUtils.interactBlock(InvUtils.findInHotbar(Items.NETHERITE_HOE), pos, true);
    }

    @Override
    public void onDeactivate() {

    }

    @EventHandler

    private void rotate(Vec3d pos) {
        Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos));
    }


    public Block getBlock(BlockHitResult hit){
        return mc.world.getBlockState(hit.getBlockPos()).getBlock();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        List<BlockPos> b = aUtils.findblocksnearplayer(Arrays.asList(Blocks.GRASS_BLOCK, Blocks.DIRT), 10, true, true);
        if (b.size() != 0){

            Vec3d pos = aUtils.closestPointOnBlock(b.get(0));
            FindItemResult hoe = InvUtils.findInHotbar(Items.IRON_HOE, Items.STONE_HOE, Items.WOODEN_HOE, Items.GOLDEN_HOE, Items.DIAMOND_HOE, Items.NETHERITE_HOE);
            aUtils.interactBlock(hoe, pos, true);
            FindItemResult seeds = InvUtils.findInHotbar(Items.WHEAT_SEEDS);
            BlockUtils.place(new BlockPos(pos).up(), seeds, true, 0);
        }
        else{
            // Logger.Log("Empty");
        }

    }

    public boolean canTillFarmland(BlockPos pos) {
        return mc.world.getBlockState(pos.up()).isAir();
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        // ((PlayerMoveC2SPacketAccessor) event.packet).(true);
    }

    public Vec3d FlatPredict(Entity entity, Double amount){
        Vec3d p = new Vec3d(entity.getVelocity().x, 0, entity.getVelocity().z);

        return entity.getPos().add(p.multiply(amount));
    }
    public Vec3d Predict(Entity entity, Double amount){
        Vec3d p = entity.getVelocity().normalize();

        return entity.getPos().add(p.multiply(amount));
    }

    private Vec3d tovec3d(BlockHitResult r) {
        return new Vec3d(
            r.getBlockPos().getX(),
            r.getBlockPos().getY(),
            r.getBlockPos().getZ());
    }
    // private void useHoe(FindItemResult Hoe, Vec3d pos, boolean placedWater) {
    //     if (!Hoe.found()) return;

    //     if (!placedWater) isBlock=false;
    //     if(PlayerUtils.distanceTo(placedpos) > 4) return;

    //     Rotations.rotate(Rotations.getYaw(placedpos), Rotations.getPitch(placedpos), 100, true, () -> {


    //         if (Hoe.isOffhand()) {
    //             mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
    //         }
    //         else {
    //             // int preSlot = mc.player.getInventory().selectedSlot;
    //             InvUtils.swap(Hoe.slot(), true);
    //             Logger.Log("Haj");

    //             if(mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(pos, Direction.UP, new BlockPos(pos), false)).isAccepted())Log("" + this.placedWater + placedWater); this.placedWater = placedWater;
    //             InvUtils.swapBack();
    //         }

    //     });
    // }
    private void onDamage(DamageEvent event) {


        if (mode.get() == Mode.MLG && event.entity.getHealth() <= 0) {
            placedWater = false;
        }
    }

    @Override
    public String getInfoString() {
        return mode.get().toString();
    }

    public enum Mode {
        Packet,
        AirPlace,
        MLG
    }

    public enum PlaceMode {
        BeforeDamage(height -> height > 2),
        BeforeDeath(height -> height > Math.max(PlayerUtils.getTotalHealth(), 2));

        private final Predicate<Float> fallHeight;

        PlaceMode(Predicate<Float> fallHeight) {
            this.fallHeight = fallHeight;
        }

        public boolean test(float fallheight) {
            return fallHeight.test(fallheight);
        }
    }

    public void Log(String Log){
        mc.player.sendChatMessage(String.valueOf(Log), null);
    }
    public void Log(ActionResult Log){
        mc.player.sendChatMessage(String.valueOf(Log), null);
    }
    public boolean Log(Boolean Log){
        mc.player.sendChatMessage(String.valueOf(Log), null);
        return Log;

    }
}
