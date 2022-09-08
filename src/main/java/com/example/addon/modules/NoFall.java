/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package com.example.addon.modules;


import baritone.api.BaritoneAPI;
import javassist.bytecode.analysis.ControlFlow.Block;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.PlayerMoveC2SPacketAccessor;
import meteordevelopment.meteorclient.mixininterface.IPlayerMoveC2SPacket;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
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
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.item.Item;
import java.io.Console;
import java.lang.System.Logger;
import java.util.function.Predicate;

import net.minecraft.block.Blocks;
import net.minecraft.block.PowderSnowBlock;
import net.minecraft.entity.Entity;

import com.mojang.logging.LogUtils;

public class NoFall extends Module {
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

    private boolean placedWater;
    public Vec3d placedpos;
    private int preBaritoneFallHeight;
    public Item[] cluchItems = {Items.LAVA_BUCKET, Items.WATER_BUCKET, Items.POWDER_SNOW_BUCKET, Items.HAY_BLOCK};
    public boolean isBlock;

    public NoFall() {
        super(Categories.Movement, "no-fall", "Attempts to prevent you from taking fall damage.");
    }

    @Override
    public void onActivate() {
        preBaritoneFallHeight = BaritoneAPI.getSettings().maxFallHeightNoWater.value;
        if (mode.get() == Mode.Packet) BaritoneAPI.getSettings().maxFallHeightNoWater.value = 255;
        placedWater = false;
    }

    @Override
    public void onDeactivate() {
        BaritoneAPI.getSettings().maxFallHeightNoWater.value = preBaritoneFallHeight;
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (mc.player.getAbilities().creativeMode
            || !(event.packet instanceof PlayerMoveC2SPacket)
            || mode.get() != Mode.Packet
            || ((IPlayerMoveC2SPacket) event.packet).getTag() == 1337) return;


        if (!Modules.get().isActive(Flight.class)) {
            if (mc.player.isFallFlying()) return;
            if (mc.player.getVelocity().y > -0.5) return;
            ((PlayerMoveC2SPacketAccessor) event.packet).setOnGround(true);
        } else {
            ((PlayerMoveC2SPacketAccessor) event.packet).setOnGround(true);
        }
    }
    private void rotate(Vec3d pos) {
        Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos));
    }

    public FindItemResult FindCluchItem(){
        FindItemResult CluchItem = null;
        for (Item cItem : cluchItems) {
            CluchItem = InvUtils.findInHotbar(cItem);
            if (CluchItem.found()){
                if(cItem instanceof BlockItem block) isBlock = true;
                else isBlock = false;
                return CluchItem;
            }
        }
        mc.player.sendChatMessage("message", null);
        return CluchItem;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        // rotate(new Vec3d(31, 92 + 1, 58));
        if (mc.player.getAbilities().creativeMode) return;

        // Airplace mode
        if (mode.get() == Mode.AirPlace) {
            // Test if fall damage setting is valid
            if (!airPlaceMode.get().test(mc.player.fallDistance)) return;

            // Center and place block
            if (anchor.get()) PlayerUtils.centerPlayer();

            Rotations.rotate(mc.player.getYaw(), 90, Integer.MAX_VALUE, () -> {
                double preY = mc.player.getVelocity().y;
                ((IVec3d) mc.player.getVelocity()).setY(0);

                BlockUtils.place(mc.player.getBlockPos().down(), InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BlockItem), false, 0, true);

                ((IVec3d) mc.player.getVelocity()).setY(preY);
            });
        }

        // Bucket mode
        if (mode.get() == Mode.MLG) {
            if (mc.player.fallDistance > 3 && !EntityUtils.isAboveWater(mc.player)) {
                // Place water

                FindItemResult cluchItem = FindCluchItem();


                // Center player
                if (anchor.get()) PlayerUtils.centerPlayer();

                BlockHitResult mresult = null;

                // Check if there is a block within 5 blocks
                BlockHitResult result = null;

                for (int x = 0; x < 2; x++) {
                    for (int y = 0; y < 2; y++) {
                        mresult = mc.world.raycast(new RaycastContext(mc.player.getPos().subtract(x * 0.8 - 0.4, 0, y * 0.8 - 0.4), Predict(mc.player, predict.get()).subtract(x * 0.8 - 0.4, 1, y * 0.8 - 0.4), RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));


                        if (mresult != null && mresult.getType() == HitResult.Type.BLOCK) {
                            if (result == null){
                                result = mresult;
                            }
                            else if (mresult.getBlockPos().getY() > result.getBlockPos().getY()) {
                                result = mresult;


                            }
                        }
                    }
                }
                if(result != null){


                }
                // Place water
                if (result != null && result.getType() == HitResult.Type.BLOCK) {
                    placedpos = tovec3d(result).add(0.5, 1, 0.5);
                    useBucket(cluchItem, true);
                }
            }

            // Remove water
            if (placedWater && mc.player.getBlockStateAtPos().getFluidState().getFluid() == Fluids.WATER || mc.player.getBlockStateAtPos().getFluidState().getFluid() == Fluids.FLOWING_WATER || mc.player.getBlockStateAtPos().getBlock() == Blocks.POWDER_SNOW || mc.player.isTouchingWater()) {
                useBucket(InvUtils.findInHotbar(Items.BUCKET), false);
            }
        }
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
    private void useBucket(FindItemResult bucket, boolean placedWater) {
        if (!bucket.found()) return;
        mc.player.sendChatMessage(String.valueOf(bucket), null);
        if (!placedWater) isBlock=false;


        Rotations.rotate(Rotations.getYaw(placedpos), Rotations.getPitch(placedpos), 100, true, () -> {
            if (bucket.isOffhand()) {
                mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
            } else {
                // int preSlot = mc.player.getInventory().selectedSlot;
                InvUtils.swap(bucket.slot(), true);
                if(isBlock) BlockUtils.place(new BlockPos(placedpos), bucket, true, 0);
                else mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                // InvUtils.swapBack();
            }

            this.placedWater = placedWater;
        });
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
}
