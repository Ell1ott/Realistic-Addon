package com.example.addon.Utils;
import static meteordevelopment.meteorclient.MeteorClient.mc;

import meteordevelopment.meteorclient.utils.entity.Target;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
public class RotationUtils {
    public static double getYaw(Entity eFrom, Entity eTo) {
        return eFrom.getYaw() + MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(eTo.getZ() - eFrom.getZ(), eTo.getX() - eFrom.getX())) - 90f - eFrom.getYaw());
    }

    public static double getYaw(Vec3d pFrom, Vec3d pTo) {
        return MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(pTo.getZ() - pFrom.getZ(), pTo.getX() - pFrom.getX())) - 90f);
    }

    public static double getPitch(Vec3d pFrom, Vec3d pTo) {
        double diffX = pTo.getX() - mc.player.getX();
        double diffY = pTo.getY() - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        double diffZ = pTo.getZ() - mc.player.getZ();

        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        return mc.player.getPitch() + MathHelper.wrapDegrees((float) -Math.toDegrees(Math.atan2(diffY, diffXZ)) - mc.player.getPitch());
    }

    public static double getPitch(Entity eFrom, Entity eTo, Target target) {
        double y;
        if (target == Target.Head) y = eTo.getEyeY();
        else if (target == Target.Body) y = eTo.getY() + eTo.getHeight() / 2;
        else y = eTo.getY();

        double diffX = eTo.getX() - eFrom.getX();
        double diffY = y - (eFrom.getY() + eFrom.getEyeHeight(eFrom.getPose()));
        double diffZ = eTo.getZ() - eFrom.getZ();

        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        return eFrom.getPitch() + MathHelper.wrapDegrees((float) -Math.toDegrees(Math.atan2(diffY, diffXZ)) - eFrom.getPitch());
    }

    public static double calcDis(Entity eFrom, Entity eTo)
    {

        double yawdis = getYaw(eFrom, eTo) - eFrom.getYaw();
        double pitchdis = getPitch(eFrom, eTo) - eFrom.getYaw();

        if (yawdis < -180) {yawdis = yawdis + 360;}
        if (yawdis > 180) {yawdis =  yawdis - 360;}

        return Math.sqrt(yawdis * yawdis + pitchdis * pitchdis);


    }

    public static double getPitch(Entity eFrom, Entity eTo) {
        return getPitch(eFrom, eTo, Target.Body);
    }

    // public static double getYaw(BlockPos pos) {
    //     return mc.player.getYaw() + MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(pos.getZ() + 0.5 - mc.player.getZ(), pos.getX() + 0.5 - mc.player.getX())) - 90f - mc.player.getYaw());
    // }

    // public static double getPitch(BlockPos pos) {
    //     double diffX = pos.getX() + 0.5 - mc.player.getX();
    //     double diffY = pos.getY() + 0.5 - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
    //     double diffZ = pos.getZ() + 0.5 - mc.player.getZ();

    //     double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

    //     return mc.player.getPitch() + MathHelper.wrapDegrees((float) -Math.toDegrees(Math.atan2(diffY, diffXZ)) - mc.player.getPitch());
    // }
}
