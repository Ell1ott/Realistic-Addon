/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package com.example.addon.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.ProjectileEntityAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.ProjectileEntitySimulator;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.misc.Vec3;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;

import com.example.addon.Utils.Logger;
import com.example.addon.Utils.RendererUtils;
import com.example.addon.Utils.aUtils;
import com.example.addon.Utils.RotationUtils;

public class ArrowBlock extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgMovement = settings.createGroup("Movement");

    private final Setting<Double> distanceCheck = sgMovement.add(new DoubleSetting.Builder()
        .name("distance-check")
        .description("How far should an arrow be from the player to be considered not hitting.")
        .defaultValue(1)
        .min(0.01)
        .sliderRange(0.01, 5)
        .build()
    );

    private final Setting<Boolean> accurate = sgGeneral.add(new BoolSetting.Builder()
        .name("accurate")
        .description("Whether or not to calculate more accurate.")
        .defaultValue(false)
        .build()
    );


    private final Setting<Boolean> allProjectiles = sgGeneral.add(new BoolSetting.Builder()
        .name("all-projectiles")
        .description("Dodge all projectiles, not only arrows.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> ignoreOwn = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-own")
        .description("Ignore your own projectiles.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Integer> simulationSteps = sgGeneral.add(new IntSetting.Builder()
        .name("simulation-steps")
        .description("How many steps to simulate projectiles. Zero for no limit.")
        .defaultValue(500)
        .sliderMax(5000)
        .build()
    );
    public final Setting<Integer> releasedelay = sgGeneral.add(new IntSetting.Builder()
        .name("release-delay")
        .description("how many ticks it should take to realease the right click button again")
        .defaultValue(25)
        .range(0, 60)
        .sliderMax(50)
        .build()
    );




    private final ProjectileEntitySimulator simulator = new ProjectileEntitySimulator();
    private final Pool<Vec3> vec3s = new Pool<>(Vec3::new);
    private final List<Vec3> points = new ArrayList<>();

    public ArrowBlock() {
        super(Categories.Combat, "arrow-Block", "Tries to block arrows coming at you.");
    }

    public static Vec3d toVec3d(Vec3 vec3){
        return new Vec3d(vec3.x, vec3.y, vec3.z);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {

        // if(skeleton != null && skeleton.getHandItems().iterator().next().getItem() instanceof BowItem BowItem){


        // }


        for (Vec3 point : points) vec3s.free(point);
        points.clear();

        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof ProjectileEntity)) continue;
            if (!allProjectiles.get() && !(e instanceof ArrowEntity)) continue;
            if (ignoreOwn.get()) {
                UUID owner = ((ProjectileEntityAccessor) e).getOwnerUuid();
                if (owner != null && owner.equals(mc.player.getUuid())) continue;
            }
            if (!simulator.set(e, accurate.get(), 0.5D)) continue;
            for (int i = 0; i < (simulationSteps.get() > 0 ? simulationSteps.get() : Integer.MAX_VALUE); i++) {
                points.add(vec3s.get().set(simulator.pos));
                if (simulator.tick() != null) break;
            }
        }

        if (isValid(Vec3d.ZERO, false)) {
            LivingEntity entity = (LivingEntity) TargetUtils.get((Entity e) -> e instanceof SkeletonEntity && e instanceof LivingEntity s && s.getItemUseTime() > 10 && s.distanceTo(mc.player) < 30, SortPriority.LowestDistance);
            if(entity == null){

                entity = (LivingEntity) TargetUtils.get((Entity e) -> e instanceof CreeperEntity c && c.getClientFuseTime(0) > 0.7 && e.distanceTo(mc.player) < 30, SortPriority.LowestDistance);
            }


            if(entity == null) return;
            // Logger.Log(""+RotationUtils.calcDis(skeleton, mc.player));
            FindItemResult shield = InvUtils.findInHotbar(Items.SHIELD);
            useShild(entity.getPos());

            RendererUtils.addPoint(entity.getPos(), Color.BLUE);
            return;

        } // no need to move



        RendererUtils.addPoint(toVec3d(points.get(1)), Color.BLUE);
        useShild(toVec3d(points.get(0)));



    }
    public void useShild(Vec3d pos){
        FindItemResult shield = InvUtils.findInHotbar(Items.SHIELD);
        aUtils.rightClickItem(shield, pos, releasedelay.get());
    }

    private boolean isValid(Vec3d velocity, boolean checkGround) {
        Vec3d playerPos = mc.player.getPos().add(velocity);
        Vec3d headPos = playerPos.add(0, 1, 0);

        for (Vec3 pos : points) {
            Vec3d projectilePos = new Vec3d(pos.x, pos.y, pos.z);
            if (projectilePos.isInRange(playerPos, distanceCheck.get())) return false;
            if (projectilePos.isInRange(headPos, distanceCheck.get())) return false;
        }

        if (checkGround) {
            BlockPos blockPos = mc.player.getBlockPos().add(velocity.x, velocity.y, velocity.z);

            // check if target pos is air
            if (!mc.world.getBlockState(blockPos).getCollisionShape(mc.world, blockPos).isEmpty()) return false;
            else if (!mc.world.getBlockState(blockPos.up()).getCollisionShape(mc.world, blockPos.up()).isEmpty()) return false;


        }

        return true;
    }

    public enum MoveType {
        Velocity,
        Packet
    }
}
