/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package com.example.addon.modules;


import baritone.api.BaritoneAPI;
import javassist.expr.Instanceof;
import net.fabricmc.loader.impl.util.log.Log;
import net.minecraft.block.Block;
import meteordevelopment.meteorclient.events.entity.DamageEvent;
// import meteordevelopment.meteorclient.events.entity.player.BreakBlockEvent;
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
// import meteordevelopment.meteorclient.systems.modules.player.Reach;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.item.Item;
import java.util.Set;
import java.util.function.Predicate;

import com.example.addon.Utils.RendererUtils;
import com.google.errorprone.annotations.Var;

import net.minecraft.block.Blocks;

import net.minecraft.entity.Entity;

import net.minecraft.block.LeavesBlock;

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

    private boolean placedWater = false;
    public Vec3d placedpos;
    private int preBaritoneFallHeight;
    public Item[] cluchItems = {Items.LAVA_BUCKET, Items.WATER_BUCKET, Items.POWDER_SNOW_BUCKET, Items.HAY_BLOCK, Items.SLIME_BLOCK};
    public static final Set<Block> cBlocks = Set.of(Blocks.SLIME_BLOCK, Blocks.POWDER_SNOW, Blocks.HAY_BLOCK);
    // public Block[] cBlocks = {Blocks.SLIME_BLOCK, Blocks.POWDER_SNOW, Blocks.HAY_BLOCK};
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

    public FindItemResult FindCluchItem(Block landingBlock){
        FindItemResult CluchItem = null;
        for (Item cItem : cluchItems) {
            CluchItem = InvUtils.findInHotbar(cItem);
            if (CluchItem.found()){

                if(cItem == Items.WATER_BUCKET && landingBlock instanceof LeavesBlock lb) continue;
                if(cItem instanceof BlockItem block)isBlock = true;
                else isBlock = false;
                return CluchItem;
            }
        }
        return CluchItem;
    }

    public Block getBlock(BlockHitResult hit){
        return mc.world.getBlockState(hit.getBlockPos()).getBlock();
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
        if (mode.get() == Mode.MLG && mc.player != null && mc.world != null) {
            if (mc.player.fallDistance > 3 && !EntityUtils.isAboveWater(mc.player) && !placedWater) {
                // Place water





                // Center player
                if (anchor.get()) PlayerUtils.centerPlayer();

                BlockHitResult mresult = null;

                // Check if there is a block within 5 blocks
                BlockHitResult result = null;

                Vec3d ppos = Predict(mc.player, predict.get());

                // Box bb = mc.player.getBoundingBox();

                for (int x = 0; x < 2; x++) {
                    for (int y = 0; y < 2; y++) {
                        mresult = mc.world.raycast(new RaycastContext(mc.player.getPos().subtract(x * 0.6 - 0.3, 0, y * 0.6 - 0.3), ppos.subtract(x * 0.6 - 0.3, 1, y * 0.6 - 0.3), RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));


                        if (mresult != null && mresult.getType() == HitResult.Type.BLOCK) {
                            RendererUtils.addPoint(mresult.getPos(), Color.BLUE);
                            if (result == null){
                                result = mresult;
                            }
                            else if (mresult.getBlockPos().getY() > result.getBlockPos().getY()) {
                            }
                            if(ppos.distanceTo(tovec3d(mresult).add(0.5, -2, 0.5)) < ppos.distanceTo(tovec3d(result).add(0.5, -2, 0.5))){

                                result = mresult;
                            }
                        }
                    }
                }
                if(result != null){


                }
                // Place water
                if (result != null && result.getType() == HitResult.Type.BLOCK &&  !cBlocks.contains(getBlock(result))) {
                    FindItemResult cluchItem = FindCluchItem(getBlock(result));
                    placedpos = tovec3d(result).add(0.5, 1, 0.5);

                    useBucket(cluchItem, true);
                }
            }

            // Remove water
            if (placedWater){

                if (mc.player.getBlockStateAtPos().getFluidState().getFluid() == Fluids.WATER || mc.player.getBlockStateAtPos().getFluidState().getFluid() == Fluids.FLOWING_WATER || mc.player.getBlockStateAtPos().getBlock() == Blocks.POWDER_SNOW || mc.player.isTouchingWater() || mc.player.isOnGround()) {
                    Log("trying to remove water");
                    useBucket(InvUtils.findInHotbar(Items.BUCKET), false);
                }
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

        if (!placedWater) isBlock=false;
        if(PlayerUtils.distanceTo(placedpos) > 4) {this.placedWater = placedWater; return;}

        Rotations.rotate(Rotations.getYaw(placedpos), Rotations.getPitch(placedpos), 100, true, () -> {
            if(isBlock){
                if(isBlock)  if(BlockUtils.place(new BlockPos(placedpos), bucket, true, 0)) this.placedWater = placedWater;
            }

            else if (bucket.isOffhand()) {
                mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
            }
            else {
                // int preSlot = mc.player.getInventory().selectedSlot;
                InvUtils.swap(bucket.slot(), true);
                if(mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND).isAccepted()){Log("" + this.placedWater + placedWater); this.placedWater = placedWater;}
                else Log("was not accepted");
                InvUtils.swapBack();
            }

        });
    }
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
