package com.eatingesp;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Matrix4f;

public class EatingEspRenderer {

    private static final float  PULSE_SPEED  = 2.0f;
    private static final double Y_OFFSET     = 0.3;
    private static final float  ITEM_SCALE   = 0.35f;
    private static final int    ARC_SEGMENTS = 48;
    private static final float  RING_OUTER   = 0.72f;
    private static final float  RING_INNER   = 0.58f;

    public static void onWorldRender(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        World world = mc.world;
        if (world == null) return;

        MatrixStack matrices = ctx.matrixStack();
        VertexConsumerProvider.Immediate immediate =
                mc.getBufferBuilders().getEntityVertexConsumers();

        Camera camera = ctx.camera();
        Vec3d camPos  = camera.getPos();

        for (PlayerEntity player : world.getPlayers()) {
            if (player == mc.player) continue;
            if (!player.isUsingItem()) continue;

            Hand activeHand = player.getActiveHand();
            ItemStack usingStack = player.getStackInHand(activeHand);
            if (usingStack.isEmpty()) continue;
            if (!isConsumable(usingStack)) continue;

            float tickDelta = ctx.tickCounter().getTickDelta(true);
            Vec3d lerpPos = player.getLerpedPos(tickDelta);

            double ex = lerpPos.x;
            double ey = lerpPos.y + player.getHeight() * 0.5 + Y_OFFSET;
            double ez = lerpPos.z;

            matrices.push();
            matrices.translate(ex - camPos.x, ey - camPos.y,
