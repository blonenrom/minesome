package com.eatingesp;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class EatingEspRenderer {

    private static final float PULSE_SPEED  = 2.0f;
    private static final float ITEM_SCALE   = 0.35f;
    private static final int   ARC_SEGMENTS = 48;
    private static final float RING_OUTER   = 0.72f;
    private static final float RING_INNER   = 0.58f;

    public static void onWorldRender(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        World world = mc.world;
        if (world == null) return;

        Camera camera = ctx.camera();
        Vec3d camPos = camera.getPos();
        float tickDelta = ctx.tickCounter().getTickDelta(true);

        // Матрица проекции + вид камеры
        Matrix4f proj = RenderSystem.getProjectionMatrix();
        Matrix4f view = new Matrix4f(RenderSystem.getModelViewMatrix());

        for (PlayerEntity player : world.getPlayers()) {
            if (player == mc.player) continue;
            if (!player.isUsingItem()) continue;

            Hand activeHand = player.getActiveHand();
            ItemStack usingStack = player.getStackInHand(activeHand);
            if (usingStack.isEmpty()) continue;
            if (!isConsumable(usingStack)) continue;

            double ex = player.prevX + (player.getX() - player.prevX) * tickDelta;
            double ey = player.prevY + (player.getY() - player.prevY) * tickDelta + player.getHeight() * 0.5 + 0.5;
            double ez = player.prevZ + (player.getZ() - player.prevZ) * tickDelta;

            float dx = (float)(ex - camPos.x);
            float dy = (float)(ey - camPos.y);
            float dz = (float)(ez - camPos.z);

            float maxUseTicks = usingStack.getMaxUseTime(player);
            float ticksUsed = maxUseTicks - player.getItemUseTimeLeft();
            float progress = maxUseTicks > 0 ? Math.min(1.0f, ticksUsed / maxUseTicks) : 0f;
            int[] colour = getItemColour(usingStack);

            float time = (System.currentTimeMillis() % (long)(PULSE_SPEED * 1000)) / (PULSE_SPEED * 1000f);
            float pulse = 1.0f + 0.06f * (float) Math.sin(time * Math.PI * 2);

            // Строим матрицу: view * translate * billboardYaw * scale
            Matrix4f model = new Matrix4f(view);
            model.translate(dx, dy, dz);
            model.rotateY(org.joml.Math.toRadians(-camera.getYaw()));
            model.rotateX(org.joml.Math.toRadians(camera.getPitch()));
            model.scale(ITEM_SCALE * pulse);

            Matrix4f mvp = new Matrix4f(proj).mul(model);

            MatrixStack matrices = new MatrixStack();
            matrices.multiplyPositionMatrix(mvp);

            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(net.minecraft.client.gl.ShaderProgramKeys.POSITION_COLOR);

            renderCircleDisc(matrices, RING_OUTER + 0.04f, 20, 20, 20, 180);
            renderArcRing(matrices, RING_INNER, RING_OUTER, 1.0f, 60, 60, 60, 200);
            renderArcRing(matrices, RING_INNER, RING_OUTER, progress, colour[0], colour[1], colour[2], 240);

            RenderSystem.enableDepthTest();

            // Иконка
            MatrixStack iconMat = new MatrixStack();
            iconMat.multiplyPositionMatrix(mvp);
            iconMat.translate(-0.5f, -0.5f, 0f);

            VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
            RenderSystem.disableDepthTest();
            mc.getItemRenderer().renderItem(null, usingStack,
                    net.minecraft.item.ModelTransformationMode.GUI, false,
                    iconMat, immediate, mc.world,
                    LightmapTextureManager.MAX_LIGHT_COORDINATE,
                    OverlayTexture.DEFAULT_UV, 0);
            immediate.draw();
            RenderSystem.enableDepthTest();
        }
    }

    private static void renderCircleDisc(MatrixStack matrices, float radius, int r, int g, int b, int a) {
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        Matrix4f mat = matrices.peek().getPositionMatrix();
        for (int i = 0; i < ARC_SEGMENTS; i++) {
            float a0 = (float) i / ARC_SEGMENTS * (float)(Math.PI * 2);
            float a1 = (float)(i + 1) / ARC_SEGMENTS * (float)(Math.PI * 2);
            buf.vertex(mat, 0, 0, 0).color(r, g, b, a);
            buf.vertex(mat, (float)Math.cos(a0) * radius, (float)Math.sin(a0) * radius, 0).color(r, g, b, a);
            buf.vertex(mat, (float)Math.cos(a1) * radius, (float)Math.sin(a1) * radius, 0).color(r, g, b, a);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private static void renderArcRing(MatrixStack matrices, float innerR, float outerR, float fraction, int r, int g, int b, int a) {
        if (fraction <= 0) return;
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        Matrix4f mat = matrices.peek().getPositionMatrix();
        float startAngle = (float)(-Math.PI / 2);
        float sweep = fraction * (float)(Math.PI * 2);
        int totalSeg = Math.max(1, Math.round(ARC_SEGMENTS * fraction));
        for (int i = 0; i < totalSeg; i++) {
            float t0 = startAngle + sweep * ((float) i / totalSeg);
            float t1 = startAngle + sweep * ((float)(i + 1) / totalSeg);
            buf.vertex(mat, (float)Math.cos(t0) * innerR, (float)Math.sin(t0) * innerR, 0).color(r, g, b, a);
            buf.vertex(mat, (float)Math.cos(t0) * outerR, (float)Math.sin(t0) * outerR, 0).color(r, g, b, a);
            buf.vertex(mat, (float)Math.cos(t1) * outerR, (float)Math.sin(t1) * outerR, 0).color(r, g, b, a);
            buf.vertex(mat, (float)Math.cos(t1) * innerR, (float)Math.sin(t1) * innerR, 0).color(r, g, b, a);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private static boolean isConsumable(ItemStack stack) {
        Item item = stack.getItem();
        if (stack.isOf(Items.APPLE))                  return true;
        if (stack.isOf(Items.GOLDEN_APPLE))           return true;
        if (stack.isOf(Items.ENCHANTED_GOLDEN_APPLE)) return true;
        if (stack.isOf(Items.GOLDEN_CARROT))          return true;
        if (stack.isOf(Items.CARROT))                 return true;
        if (stack.isOf(Items.POTATO))                 return true;
        if (stack.isOf(Items.BAKED_POTATO))           return true;
        if (stack.isOf(Items.BEETROOT))               return true;
        if (stack.isOf(Items.BEETROOT_SOUP))          return true;
        if (stack.isOf(Items.BREAD))                  return true;
        if (stack.isOf(Items.COOKED_BEEF))            return true;
        if (stack.isOf(Items.BEEF))                   return true;
        if (stack.isOf(Items.COOKED_PORKCHOP))        return true;
        if (stack.isOf(Items.PORKCHOP))               return true;
        if (stack.isOf(Items.COOKED_CHICKEN))         return true;
        if (stack.isOf(Items.CHICKEN))                return true;
        if (stack.isOf(Items.COOKED_COD))             return true;
        if (stack.isOf(Items.COD))                    return true;
        if (stack.isOf(Items.COOKED_SALMON))          return true;
        if (stack.isOf(Items.SALMON))                 return true;
        if (stack.isOf(Items.COOKED_MUTTON))          return true;
        if (stack.isOf(Items.MUTTON))                 return true;
        if (stack.isOf(Items.COOKED_RABBIT))          return true;
        if (stack.isOf(Items.RABBIT))                 return true;
        if (stack.isOf(Items.RABBIT_STEW))            return true;
        if (stack.isOf(Items.MUSHROOM_STEW))          return true;
        if (stack.isOf(Items.SUSPICIOUS_STEW))        return true;
        if (stack.isOf(Items.MELON_SLICE))            return true;
        if (stack.isOf(Items.PUMPKIN_PIE))            return true;
        if (stack.isOf(Items.COOKIE))                 return true;
        if (stack.isOf(Items.CAKE))                   return true;
        if (stack.isOf(Items.DRIED_KELP))             return true;
        if (stack.isOf(Items.SWEET_BERRIES))          return true;
        if (stack.isOf(Items.GLOW_BERRIES))           return true;
        if (stack.isOf(Items.CHORUS_FRUIT))           return true;
        if (stack.isOf(Items.ROTTEN_FLESH))           return true;
        if (stack.isOf(Items.SPIDER_EYE))             return true;
        if (stack.isOf(Items.POISONOUS_POTATO))       return true;
        if (stack.isOf(Items.PUFFERFISH))             return true;
        if (stack.isOf(Items.TROPICAL_FISH))          return true;
        if (stack.isOf(Items.KELP))                   return true;
        if (item instanceof PotionItem)               return true;
        if (stack.isOf(Items.HONEY_BOTTLE))           return true;
        if (stack.isOf(Items.MILK_BUCKET))            return true;
        if (stack.getComponents().contains(net.minecraft.component.DataComponentTypes.FOOD)) return true;
        return false;
    }

    private static int[] getItemColour(ItemStack stack) {
        Item item = stack.getItem();
        if (stack.isOf(Items.GOLDEN_APPLE) || stack.isOf(Items.ENCHANTED_GOLDEN_APPLE))
            return new int[]{255, 215, 0};
        if (stack.isOf(Items.GOLDEN_CARROT))
            return new int[]{255, 200, 50};
        if (item instanceof PotionItem)
            return new int[]{128, 0, 255};
        if (stack.isOf(Items.HONEY_BOTTLE))
            return new int[]{255, 165, 0};
        if (stack.isOf(Items.MILK_BUCKET))
            return new int[]{255, 255, 255};
        if (stack.isOf(Items.CHORUS_FRUIT))
            return new int[]{180, 100, 200};
        if (stack.isOf(Items.ROTTEN_FLESH) || stack.isOf(Items.POISONOUS_POTATO)
                || stack.isOf(Items.SPIDER_EYE) || stack.isOf(Items.PUFFERFISH))
            return new int[]{200, 50, 50};
        if (stack.isOf(Items.SUSPICIOUS_STEW))
            return new int[]{100, 220, 130};
        return new int[]{80, 220, 80};
    }
}
