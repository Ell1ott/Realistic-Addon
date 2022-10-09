package com.example.addon.modules;

import com.example.addon.Addon;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalBlock;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

public class hunt extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> dis = sgGeneral.add(new IntSetting.Builder()
        .name("distance")
        .defaultValue(60)
        .range(1, 200)
        .sliderRange(1, 200)
        .build()
    );

        private final Setting<Boolean> ignorePassive = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-passive")
        .description("Will only attack sometimes passive mobs if they are targeting you.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> ignoreTamed = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-tamed")
        .description("Will avoid attacking mobs you tamed.")
        .defaultValue(false)
        .build()
    );


    private final Setting<Object2BooleanMap<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("Entities to attack.")
        .onlyAttackable()
        .build()
    );

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("The maximum range the entity can be to attack it.")
        .defaultValue(4.5)
        .min(0)
        .sliderMax(100)
        .build()
    );

    private final Setting<Double> wallsRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("walls-range")
        .description("The maximum range the entity can be attacked through walls.")
        .defaultValue(3.5)
        .min(0)
        .sliderMax(6)
        .build()
    );

    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
        .name("priority")
        .description("How to filter targets within range.")
        .defaultValue(SortPriority.LowestHealth)
        .build()
    );

    private final Setting<Integer> maxTargets = sgGeneral.add(new IntSetting.Builder()
        .name("max-targets")
        .description("How many entities to target at once.")
        .defaultValue(1)
        .min(1)
        .sliderRange(1, 5)
        .build()
    );

    private final Setting<Boolean> babies = sgGeneral.add(new BoolSetting.Builder()
        .name("babies")
        .description("Whether or not to attack baby variants of the entity.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> nametagged = sgGeneral.add(new BoolSetting.Builder()
        .name("nametagged")
        .description("Whether or not to attack mobs with a name tag.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> ignoreShield = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-shield")
        .description("Attacks only if the blow is not blocked by a shield.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> noRightClick = sgGeneral.add(new BoolSetting.Builder()
    .name("pause-on-use")
    .description("Does not attack if using an item.")
    .defaultValue(true)
    .build()
    );


    public hunt() {
        super(Addon.CATEGORY, "hunt", "hunts specified entities");
    }



    @Override
    public void onActivate() {

    }
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        Entity entity = TargetUtils.get(this::entityCheck, SortPriority.LowestDistance);



        if(entity != null) if (entity.distanceTo(mc.player) <= dis.get()){
            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(new BlockPos(entity.getPos().add(0, 0.2, 0))));

        } else if(BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing()) {

            BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();

        }


    }

    private boolean entityCheck(Entity entity) {
        if (entity.equals(mc.player) || entity.equals(mc.cameraEntity)) return false;
        if ((entity instanceof LivingEntity && ((LivingEntity) entity).isDead()) || !entity.isAlive()) return false;
        if (noRightClick.get() && (mc.interactionManager.isBreakingBlock() || mc.player.isUsingItem())) return false;
        if (PlayerUtils.distanceTo(entity) > range.get()) return false;
        if (!entities.get().getBoolean(entity.getType())) return false;
        if (!nametagged.get() && entity.hasCustomName()) return false;
        if (!PlayerUtils.canSeeEntity(entity) && PlayerUtils.distanceTo(entity) > wallsRange.get()) return false;
        if (ignoreTamed.get()) {
            if (entity instanceof Tameable tameable
                && tameable.getOwnerUuid() != null
                && tameable.getOwnerUuid().equals(mc.player.getUuid())
            ) return false;
        }
        if (ignorePassive.get()) {
            if (entity instanceof EndermanEntity enderman && !enderman.isAngryAt(mc.player)) return false;
            if (entity instanceof ZombifiedPiglinEntity piglin && !piglin.isAngryAt(mc.player)) return false;
            if (entity instanceof WolfEntity wolf && !wolf.isAttacking()) return false;
        }
        if (entity instanceof PlayerEntity player) {
            if (player.isCreative()) return false;
            if (!Friends.get().shouldAttack(player)) return false;
            if (ignoreShield.get() && player.blockedByShield(DamageSource.player(mc.player))) return false;
        }
        return !(entity instanceof AnimalEntity animal) || babies.get() || !animal.isBaby();
    }

}
