package com.krypton.client.mixin;

import com.krypton.client.KryptonClient;
import com.krypton.client.client.HudRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {
    private String kryptonDragging = null;
    private int kryptonDragOffsetX, kryptonDragOffsetY;

    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void krypton$onMouseButton(long window, MouseInput input, int action, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen != null || !KryptonClient.HUD_RENDERER.editing) return;
        int button = input.button();
        HudRenderer hud = KryptonClient.HUD_RENDERER;
        int mx = (int) mc.mouse.getX();
        int my = (int) mc.mouse.getY();

        if (button == 0) {
            if (action == 1) { // pressed
                String hit = hud.hitTest(mx, my);
                if (hit != null) {
                    kryptonDragging = hit;
                    int[] pos = hud.getPosition(hit);
                    kryptonDragOffsetX = mx - pos[0];
                    kryptonDragOffsetY = my - pos[1];
                }
            } else if (action == 0) { // released
                if (kryptonDragging != null) {
                    hud.setPosition(kryptonDragging, mx - kryptonDragOffsetX, my - kryptonDragOffsetY);
                    KryptonClient.MODULE_MANAGER.saveAll();
                    kryptonDragging = null;
                }
            }
        }
    }

    @Inject(method = "onCursorPos", at = @At("HEAD"))
    private void krypton$onCursorPos(long window, double x, double y, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen != null || !KryptonClient.HUD_RENDERER.editing || kryptonDragging == null) return;
        KryptonClient.HUD_RENDERER.setPosition(kryptonDragging, (int) x - kryptonDragOffsetX, (int) y - kryptonDragOffsetY);
    }
}