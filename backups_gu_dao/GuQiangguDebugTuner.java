package net.tigereye.chestcavity.compat.guzhenren.item.gu_dao;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.tigereye.chestcavity.ChestCavity;

/** Utility to tweak the placeholder spear transform at runtime. */
final class GuQiangguDebugTuner {

    static final float TRANSLATION_STEP = 0.01f;
    static final float ROTATION_STEP = 1f;

    private static float thirdPersonX = -0.28f;
    private static float thirdPersonY = 0.39f;
    private static float thirdPersonZ = -0.61f;
    private static float thirdPersonRotX = 87f;
    private static float thirdPersonRotY = 8f;
    private static float thirdPersonRotZ = -8f;

    private static float firstPersonX = -0.40f;
    private static float firstPersonY = 0.39f;
    private static float firstPersonZ = -0.61f;
    private static float firstPersonRotX = 87f;
    private static float firstPersonRotY = 8f;
    private static float firstPersonRotZ = -8f;

    private GuQiangguDebugTuner() {
    }

    static void applyTransform(PoseStack poseStack, boolean rightArm, boolean firstPerson) {
        float x = firstPerson ? firstPersonX : thirdPersonX;
        float y = firstPerson ? firstPersonY : thirdPersonY;
        float z = firstPerson ? firstPersonZ : thirdPersonZ;
        float rotX = firstPerson ? firstPersonRotX : thirdPersonRotX;
        float rotY = firstPerson ? firstPersonRotY : thirdPersonRotY;
        float rotZ = firstPerson ? firstPersonRotZ : thirdPersonRotZ;

        poseStack.translate(rightArm ? x : -x, y, z);
        poseStack.mulPose(Axis.XP.rotationDegrees(rotX));
        poseStack.mulPose(Axis.YP.rotationDegrees(rightArm ? rotY : -rotY));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rightArm ? rotZ : -rotZ));
    }

    static String adjustOffset(Axis axis, float delta, boolean firstPerson) {
        if (firstPerson) {
            if (axis == Axis.XP) firstPersonX += delta;
            else if (axis == Axis.YP) firstPersonY += delta;
            else if (axis == Axis.ZP) firstPersonZ += delta;
        } else {
            if (axis == Axis.XP) thirdPersonX += delta;
            else if (axis == Axis.YP) thirdPersonY += delta;
            else if (axis == Axis.ZP) thirdPersonZ += delta;
        }
        return formatState(firstPerson ? "offset_fp" : "offset_tp");
    }

    static String adjustRotation(Axis axis, float delta, boolean firstPerson) {
        if (firstPerson) {
            if (axis == Axis.XP) firstPersonRotX += delta;
            else if (axis == Axis.YP) firstPersonRotY += delta;
            else if (axis == Axis.ZP) firstPersonRotZ += delta;
        } else {
            if (axis == Axis.XP) thirdPersonRotX += delta;
            else if (axis == Axis.YP) thirdPersonRotY += delta;
            else if (axis == Axis.ZP) thirdPersonRotZ += delta;
        }
        return formatState(firstPerson ? "rotation_fp" : "rotation_tp");
    }

    private static String formatState(String reason) {
        String message = String.format("[GuQiangDebug] %s -> third(x=%.3f, y=%.3f, z=%.3f, rotX=%.1f, rotY=%.1f, rotZ=%.1f) | first(x=%.3f, y=%.3f, z=%.3f, rotX=%.1f, rotY=%.1f, rotZ=%.1f)",
                reason,
                thirdPersonX, thirdPersonY, thirdPersonZ, thirdPersonRotX, thirdPersonRotY, thirdPersonRotZ,
                firstPersonX, firstPersonY, firstPersonZ, firstPersonRotX, firstPersonRotY, firstPersonRotZ);
        ChestCavity.LOGGER.debug(message);
        return message;
    }
}
