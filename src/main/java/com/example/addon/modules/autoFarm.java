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
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
// import meteordevelopment.meteorclient.systems.modules.combat.Offhand.Item;
import meteordevelopment.meteorclient.systems.modules.movement.Flight;
import com.example.addon.Utils.aUtils.MyBlock;
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

import org.apache.logging.log4j.core.pattern.AbstractStyleNameConverter.Green;

import net.minecraft.util.math.Direction;

import com.example.addon.Utils.Logger;
import com.google.errorprone.annotations.Var;
import com.ibm.icu.text.AlphabeticIndex.Bucket;

import net.minecraft.block.Blocks;

import net.minecraft.entity.Entity;

import net.minecraft.block.LeavesBlock;

import net.minecraft.item.HoeItem;

import com.example.addon.Utils.RendererUtils;

import com.example.addon.Utils.aUtils;

import java.util.ArrayList;
// import com.example.addon.Utils.BlockUtils;

public class autoFarm extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("The way you are saved from fall damage.")
        .defaultValue(Mode.MLG)
        .build()
    );

    private final Setting<Boolean> anchor = sgGeneral.add(new BoolSetting.Builder()
        .name("anchor")
        .description("Centers the player and reduces movement when using bucket or air place mode.")
        .defaultValue(false)
        .visible(() -> mode.get() != Mode.Packet)
        .build()
    );


    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
       .name("delay-after-water-place")
       .description("the delay in ticks to wait before placing water")
       .defaultValue(2)
       .range(0, 40)
       .sliderRange(0, 40)
       .build()
    );



    private boolean placedWater = false;
    public Vec3d placedpos;
    private int preBaritoneFallHeight;
    int timer = 0;

    public Item[] cluchItems = {Items.LAVA_BUCKET, Items.WATER_BUCKET, Items.POWDER_SNOW_BUCKET, Items.HAY_BLOCK, Items.SLIME_BLOCK};
    public static final Set<Block> cBlocks = Set.of(Blocks.SLIME_BLOCK, Blocks.POWDER_SNOW, Blocks.HAY_BLOCK);

    public static MyBlock BlockToBreak;


    // public Block[] cBlocks = {Blocks.SLIME_BLOCK, Blocks.POWDER_SNOW, Blocks.HAY_BLOCK};
    public boolean isBlock;

    public autoFarm() {
        super(Categories.Movement, "autoFarm", "auto farms stuff");
    }

    @Override
    public void onActivate() {
        timer = 0;
    }

    @Override
    public void onDeactivate() {
    }



    @EventHandler
    private void onTick(TickEvent.Pre event) {
        timer--;
        if(timer > 0){
            return;
        }
        List<BlockPos> b2bs = aUtils.findblocksnearplayer(Arrays.asList(Blocks.GRASS_BLOCK, Blocks.DIRT),
                                                    5,
                                                    true,
                                                    true,
                                                    (bp) -> bp.getX() % 9 == 0 && bp.getZ() % 9 == 0 && (mc.world.getBlockState(bp.up().south()).isAir() || mc.world.getBlockState(bp.up().north()).isAir() || mc.world.getBlockState(bp.up().west()).isAir() || mc.world.getBlockState(bp.up().east()).isAir()) );

        if(BlockToBreak == null ) {if(b2bs.size() != 0){
            BlockPos pos = b2bs.get(0);


            BlockToBreak = new MyBlock();
            BlockToBreak.set(pos);
        }}
        else{

            if(BlockToBreak.shouldRemove()) {
                if(timer == 0){
                    aUtils.useItem(aUtils.findAndMove(Items.WATER_BUCKET, -2, Items.BUCKET), aUtils.getpos(BlockToBreak.blockPos).add(0.5, 1, 0.5));
                    RendererUtils.addPoint(aUtils.getpos(BlockToBreak.blockPos).add(0.5, 1, 0.5), Color.GREEN.a(155));
                    BlockToBreak = null;
                }
                else timer = delay.get();

            }
            else {


                BlockToBreak.mine(true);}



        }

        if(b2bs.size() == 0){

            List<BlockPos> b = aUtils.findblocksnearplayer(Arrays.asList(Blocks.GRASS_BLOCK, Blocks.DIRT), 5, true, true);
            FindItemResult seeds = aUtils.findAndMove(Items.WHEAT_SEEDS, 8);

            if (b.size() != 0 && seeds.found()){

                Vec3d pos = aUtils.closestPointOnBlock(b.get(0));
                FindItemResult hoe = InvUtils.findInHotbar(Items.IRON_HOE, Items.STONE_HOE, Items.WOODEN_HOE, Items.GOLDEN_HOE, Items.DIAMOND_HOE, Items.NETHERITE_HOE);
                aUtils.interactBlock(hoe, pos, false);
                BlockUtils.place(new BlockPos(pos).up(), seeds, true, 0);
            }
            else{
                // Logger.Log("Empty");
            }

            RendererUtils.addPoint(aUtils.closestPointOnBlock(new BlockPos(0, 100, 0)), Color.BLUE.a(50));
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
