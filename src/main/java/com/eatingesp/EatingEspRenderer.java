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
            matrices.translate(ex - camPos.x, ey - camPos.y, ez - camPos.z);

            float yaw = camera.getYaw();
            matrices.multiply(new org.joml.Quaternionf().rotationY(org.joml.Math.toRadians(-yaw)));

            float time  = (System.currentTimeMillis() % (long)(PULSE_SPEED * 1000))
                          / (PULSE_SPEED * 1000f);
            float pulse = 1.0f + 0.06f * (float) Math.sin(time * Math.PI * 2);
            matrices.scale(ITEM_SCALE * pulse, ITEM_SCALE * pulse, ITEM_SCALE * pulse);

            int[] colour = getItemColour(usingStack);

            float maxUseTicks = usingStack.getMaxUseTime(player);
            float ticksUsed   = maxUseTicks - player.getItemUseTimeLeft();
            float progress    = maxUseTicks > 0 ? Math.min(1.0f, ticksUsed / maxUseTicks) : 0f;

            renderCircleDisc(matrices, immediate, 0, 0, RING_OUTER + 0.04f, 20, 20, 20, 180);
            renderArcRing(matrices, immediate, RING_INNER, RING_OUTER, 1.0f, 60, 60, 60, 200);
            renderArcRing(matrices, immediate, RING_INNER, RING_OUTER, progress, colour[0], colour[1], colour[2], 240);
            renderItemIcon(mc, matrices, immediate, usingStack);

            matrices.pop();
        }

        immediate.draw();
    }

    private static void renderCircleDisc(
            MatrixStack matrices, VertexConsumerProvider provider,
            float cx, float cy, float radius,
            int r, int g, int b, int a) {

        VertexConsumer vc  = provider.getBuffer(RenderLayer.getDebugFilledBox());
        Matrix4f        mat = matrices.peek().getPositionMatrix();

        for (int i = 0; i < ARC_SEGMENTS; i++) {
            float a0 = (float) i       / ARC_SEGMENTS * (float)(Math.PI * 2);
            float a1 = (float)(i + 1)  / ARC_SEGMENTS * (float)(Math.PI * 2);
            vc.vertex(mat, cx, cy, 0).color(r, g, b, a);
            vc.vertex(mat, cx + (float)Math.cos(a0) * radius, cy + (float)Math.sin(a0) * radius, 0).color(r, g, b, a);
            vc.vertex(mat, cx + (float)Math.cos(a1) * radius, cy + (float)Math.sin(a1) * radius, 0).color(r, g, b, a);
            vc.vertex(mat, cx, cy, 0).color(r, g, b, a);
        }
    }

    private static void renderArcRing(
            MatrixStack matrices, VertexConsumerProvider provider,
            float innerR, float outerR, float fraction,
            int r, int g, int b, int a) {

        if (fraction <= 0) return;

        VertexConsumer vc  = provider.getBuffer(RenderLayer.getDebugFilledBox());
        Matrix4f        mat = matrices.peek().getPositionMatrix();

        float startAngle = (float)(-Math.PI / 2);
        float sweep      = fraction * (float)(Math.PI * 2);
        int totalSeg = Math.max(1, Math.round(ARC_SEGMENTS * fraction));

        for (int i = 0; i < totalSeg; i++) {
            float t0 = startAngle + sweep * ((float) i      / totalSeg);
            float t1 = startAngle + sweep * ((float)(i + 1) / totalSeg);

            float ix0 = (float)Math.cos(t0) * innerR;
            float iy0 = (float)Math.sin(t0) * innerR;
            float ox0 = (float)Math.cos(t0) * outerR;
            float oy0 = (float)Math.sin(t0) * outerR;
            float ix1 = (float)Math.cos(t1) * innerR;
            float iy1 = (float)Math.sin(t1) * innerR;
            float ox1 = (float)Math.cos(t1) * outerR;
            float oy1 = (float)Math.sin(t1) * outerR;

            vc.vertex(mat, ix0, iy0, 0).color(r, g, b, a);
            vc.vertex(mat, ox0, oy0, 0).color(r, g, b, a);
            vc.vertex(mat, ox1, oy1, 0).color(r, g, b, a);
            vc.vertex(mat, ix1, iy1, 0).color(r, g, b, a);
        }
    }

    private static void renderItemIcon(
            MinecraftClient mc,
            MatrixStack matrices,
            VertexConsumerProvider.Immediate immediate,
            ItemStack stack) {

        ItemRenderer ir = mc.getItemRenderer();
        matrices.push();
        matrices.translate(-0.5f, -0.5f, 0.0f);
        RenderSystem.disableDepthTest();
        ir.renderItem(
        stack,
        net.minecraft.item.ModelTransformationMode.GUI,
        false,
        matrices,
        immediate,
        LightmapTextureManager.MAX_LIGHT_COORDINATE,
        OverlayTexture.DEFAULT_UV,
        mc.getBakedModelManager().getItemModels().getModel(stack)
        );
        immediate.draw();
        RenderSystem.enableDepthTest();
        matrices.pop();
    }

    private static boolean isConsumable(ItemStack stack) {
        Item item = stack.getItem();
        if (stack.isOf(Items.APPLE))                   return true;
        if (stack.isOf(Items.GOLDEN_APPLE))            return true;
        if (stack.isOf(Items.ENCHANTED_GOLDEN_APPLE))  return true;
        if (stack.isOf(Items.GOLDEN_CARROT))           return true;
        if (stack.isOf(Items.CARROT))                  return true;
        if (stack.isOf(Items.POTATO))                  return true;
        if (stack.isOf(Items.BAKED_POTATO))            return true;
        if (stack.isOf(Items.BEETROOT))                return true;
        if (stack.isOf(Items.BEETROOT_SOUP))           return true;
        if (stack.isOf(Items.BREAD))                   return true;
        if (stack.isOf(Items.COOKED_BEEF))             return true;
        if (stack.isOf(Items.BEEF))                    return true;
        if (stack.isOf(Items.COOKED_PORKCHOP))         return true;
        if (stack.isOf(Items.PORKCHOP))                return true;
        if (stack.isOf(Items.COOKED_CHICKEN))          return true;
        if (stack.isOf(Items.CHICKEN))                 return true;
        if (stack.isOf(Items.COOKED_COD))              return true;
        if (stack.isOf(Items.COD))                     return true;
        if (stack.isOf(Items.COOKED_SALMON))           return true;
        if (stack.isOf(Items.SALMON))                  return true;
        if (stack.isOf(Items.COOKED_MUTTON))           return true;
        if (stack.isOf(Items.MUTTON))                  return true;
        if (stack.isOf(Items.COOKED_RABBIT))           return true;
        if (stack.isOf(Items.RABBIT))                  return true;
        if (stack.isOf(Items.RABBIT_STEW))             return true;
        if (stack.isOf(Items.MUSHROOM_STEW))           return true;
        if (stack.isOf(Items.SUSPICIOUS_STEW))         return true;
        if (stack.isOf(Items.MELON_SLICE))             return true;
        if (stack.isOf(Items.PUMPKIN_PIE))             return true;
        if (stack.isOf(Items.COOKIE))                  return true;
        if (stack.isOf(Items.CAKE))                    return true;
        if (stack.isOf(Items.DRIED_KELP))              return true;
        if (stack.isOf(Items.SWEET_BERRIES))           return true;
        if (stack.isOf(Items.GLOW_BERRIES))            return true;
        if (stack.isOf(Items.CHORUS_FRUIT))            return true;
        if (stack.isOf(Items.ROTTEN_FLESH))            return true;
        if (stack.isOf(Items.SPIDER_EYE))              return true;
        if (stack.isOf(Items.POISONOUS_POTATO))        return true;
        if (stack.isOf(Items.PUFFERFISH))              return true;
        if (stack.isOf(Items.TROPICAL_FISH))           return true;
        if (stack.isOf(Items.KELP))                    return true;
        if (item instanceof PotionItem)                return true;
        if (stack.isOf(Items.HONEY_BOTTLE))            return true;
        if (stack.isOf(Items.MILK_BUCKET))             return true;
        if (stack.getComponents().contains(net.minecraft.component.DataComponentTypes.FOOD)) return true;
        return false;
    }

    private static int[] getItemColour(ItemStack stack) {
        Item item = stack.getItem();
        if (stack.isOf(Items.GOLDEN_APPLE) || stack.isOf(Items.ENCHANTED_GOLDEN_APPLE))
            return new int[]{255, 215,   0};
        if (stack.isOf(Items.GOLDEN_CARROT))
            return new int[]{255, 200,  50};
        if (item instanceof PotionItem)
            return new int[]{128,   0, 255};
        if (stack.isOf(Items.HONEY_BOTTLE))
            return new int[]{255, 165,   0};
        if (stack.isOf(Items.MILK_BUCKET))
            return new int[]{255, 255, 255};
        if (stack.isOf(Items.CHORUS_FRUIT))
            return new int[]{180, 100, 200};
        if (stack.isOf(Items.ROTTEN_FLESH) || stack.isOf(Items.POISONOUS_POTATO)
                || stack.isOf(Items.SPIDER_EYE) || stack.isOf(Items.PUFFERFISH))
            return new int[]{200,  50,  50};
        if (stack.isOf(Items.SUSPICIOUS_STEW))
            return new int[]{100, 220, 130};
        return new int[]{ 80, 220,  80};
    }
}
