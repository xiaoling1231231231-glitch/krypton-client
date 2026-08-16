package com.krypton.client.client;

import com.krypton.client.KryptonClient;
import com.krypton.client.module.Module;
import com.krypton.client.modules.hud.*;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public class HudRenderer {
    public boolean editing = false;
    private final Map<String, int[]> positions = new HashMap<>();

    private long[] leftClicks = new long[20];
    private long[] rightClicks = new long[20];
    private int leftIdx = 0;
    private int rightIdx = 0;

    private static final int MARGIN = 4;
    private static final int EDIT_BOX = 14;

    public HudRenderer() {
        positions.put("fps", new int[]{8, 8});
        positions.put("coords", new int[]{8, 22});
        positions.put("session", new int[]{8, 36});
        positions.put("keystrokes", new int[]{-1, -1});
        positions.put("watermark", new int[]{8, 8});

        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            if (editing) renderEditMode(context);
            renderHud(context);
            KryptonClient.MODULE_MANAGER.saveAll();
        });
    }

    public void onLeftClick() {
        leftClicks[leftIdx++ % leftClicks.length] = System.currentTimeMillis();
    }

    public void onRightClick() {
        rightClicks[rightIdx++ % rightClicks.length] = System.currentTimeMillis();
    }

    private int cps(long[] clicks) {
        long now = System.currentTimeMillis();
        int count = 0;
        for (long c : clicks) {
            if (now - c < 1000) count++;
        }
        return count;
    }

    private void renderHud(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;

        for (Module m : KryptonClient.MODULE_MANAGER.getModules()) {
            if (!m.isEnabled()) continue;
            if (m instanceof Fps fps) {
                drawText(context, tr, fps.getText(), positions.get("fps"), 0xFFFFFFFF, fps.isShadow());
            } else if (m instanceof Coords coords) {
                drawText(context, tr, coords.getText(), positions.get("coords"), 0xFFFFFFFF, true);
            } else if (m instanceof SessionInfo session) {
                drawText(context, tr, session.getText(), positions.get("session"), 0xFFFFFFFF, true);
            } else if (m instanceof Keystrokes) {
                renderKeystrokes(context, mc, tr);
            } else if (m instanceof Watermark watermark) {
                drawText(context, tr, watermark.getText(), positions.get("watermark"), 0xFFFFFFFF, watermark.isShadow());
            }
        }
    }

    private void drawText(DrawContext context, TextRenderer tr, String text, int[] pos, int color, boolean shadow) {
        if (pos == null || text == null || text.isEmpty()) return;
        context.drawText(tr, Text.literal(text), pos[0], pos[1], color, shadow);
    }

    private void renderKeystrokes(DrawContext context, MinecraftClient mc, TextRenderer tr) {
        int[] pos = positions.get("keystrokes");
        int bx, by;
        if (pos[0] < 0) {
            int w = mc.getWindow().getScaledWidth();
            bx = w - 4 - 54;
            by = mc.getWindow().getScaledHeight() - 4 - 54;
        } else {
            bx = pos[0];
            by = pos[1];
        }
        int keySize = 16;
        int gap = 2;

        boolean w = InputUtil.isKeyPressed(mc.getWindow(), GLFW.GLFW_KEY_W);
        boolean a = InputUtil.isKeyPressed(mc.getWindow(), GLFW.GLFW_KEY_A);
        boolean s = InputUtil.isKeyPressed(mc.getWindow(), GLFW.GLFW_KEY_S);
        boolean d = InputUtil.isKeyPressed(mc.getWindow(), GLFW.GLFW_KEY_D);
        boolean space = InputUtil.isKeyPressed(mc.getWindow(), GLFW.GLFW_KEY_SPACE);
        boolean lmb = mc.options.attackKey.isPressed();
        boolean rmb = mc.options.useKey.isPressed();

        context.fill(bx + keySize + gap, by, bx + keySize * 2 + gap, by + keySize, w ? 0xAA35D07F : 0x44000000);
        context.drawText(tr, Text.literal("W"), bx + keySize + gap + 5, by + 4, 0xFFFFFFFF, true);

        context.fill(bx, by + keySize + gap, bx + keySize, by + keySize * 2 + gap, a ? 0xAA35D07F : 0x44000000);
        context.drawText(tr, Text.literal("A"), bx + 4, by + keySize + gap + 4, 0xFFFFFFFF, true);

        context.fill(bx + keySize + gap, by + keySize + gap, bx + keySize * 2 + gap, by + keySize * 2 + gap, s ? 0xAA35D07F : 0x44000000);
        context.drawText(tr, Text.literal("S"), bx + keySize + gap + 5, by + keySize + gap + 4, 0xFFFFFFFF, true);

        context.fill(bx + (keySize + gap) * 2, by + keySize + gap, bx + (keySize + gap) * 3, by + keySize * 2 + gap, d ? 0xAA35D07F : 0x44000000);
        context.drawText(tr, Text.literal("D"), bx + (keySize + gap) * 2 + 5, by + keySize + gap + 4, 0xFFFFFFFF, true);

        int cpsY = by + (keySize + gap) * 2;
        context.fill(bx, cpsY, bx + (keySize + gap) * 3, cpsY + 10, 0x66000000);
        context.drawText(tr, Text.literal("LMB " + cps(leftClicks) + "  RMB " + cps(rightClicks)),
                bx + 2, cpsY + 1, 0xFFE8EAF2, true);

        if (space) {
            context.fill(bx, cpsY + 12, bx + (keySize + gap) * 3, cpsY + 22, 0xAA35D07F);
        } else {
            context.fill(bx, cpsY + 12, bx + (keySize + gap) * 3, cpsY + 22, 0x44000000);
        }
        context.drawText(tr, Text.literal("SPACE"), bx + (keySize + gap) * 3 / 2 - 18, cpsY + 14, 0xFFFFFFFF, true);
    }

    private void renderEditMode(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        int w = mc.getWindow().getScaledWidth();
        int h = mc.getWindow().getScaledHeight();

        context.fill(0, 0, w, h, 0x22000000);
        context.drawText(tr, Text.literal("§9HUD Edit Mode  §7[drag boxes, right-click to close]"),
                8, 4, 0xFFFFFFFF, true);

        for (Map.Entry<String, int[]> e : positions.entrySet()) {
            int[] p = e.getValue();
            if (p[0] < 0) continue;
            String label = e.getKey();
            int tw = tr.getWidth(label);
            context.fill(p[0] - MARGIN, p[1] - MARGIN, p[0] + tw + MARGIN, p[1] + EDIT_BOX, 0xAA4F8CFF);
            context.drawText(tr, Text.literal(label), p[0], p[1], 0xFF000000, false);
        }
    }

    public int[] getPosition(String key) {
        return positions.get(key);
    }

    public void setPosition(String key, int x, int y) {
        positions.put(key, new int[]{x, y});
    }

    public void resetPosition(String key) {
        positions.remove(key);
    }

    public String hitTest(int mx, int my) {
        for (Map.Entry<String, int[]> e : positions.entrySet()) {
            int[] p = e.getValue();
            if (p[0] < 0) continue;
            int tw = MinecraftClient.getInstance().textRenderer.getWidth(e.getKey());
            if (mx >= p[0] - MARGIN && mx <= p[0] + tw + MARGIN && my >= p[1] - MARGIN && my <= p[1] + EDIT_BOX) {
                return e.getKey();
            }
        }
        return null;
    }
}