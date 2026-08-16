package com.krypton.client.mixin;

import com.krypton.client.KryptonClient;
import com.krypton.client.modules.misc.CustomTitle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.LogoDrawer;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {

    @Redirect(method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/LogoDrawer;draw(Lnet/minecraft/client/gui/DrawContext;IF)V"))
    private void krypton$customLogo(LogoDrawer drawer, DrawContext context, int width, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        CustomTitle mod = KryptonClient.MODULE_MANAGER.get("CustomTitle")
                .filter(m -> m instanceof CustomTitle)
                .map(m -> (CustomTitle) m)
                .orElse(null);

        if (mod == null || !mod.isEnabled()) {
            drawer.draw(context, width, tickDelta);
            return;
        }

        String title = mod.getTitle();
        if (title.isEmpty()) return;
        int[] palette = mod.getPalette();
        TextRenderer tr = mc.textRenderer;

        float scale = 3.0f;
        int totalW = (int) (tr.getWidth(title) * scale);
        int startX = width / 2 - totalW / 2;
        int y = 30;

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(startX, y);
        context.getMatrices().scale(scale);

        int charIndex = 0;
        float cursor = 0;
        for (int i = 0; i < title.length(); i++) {
            String ch = String.valueOf(title.charAt(i));
            if (ch.equals(" ")) {
                cursor += tr.getWidth(" ");
                charIndex++;
                continue;
            }
            int color = (0xFF << 24) | (palette[charIndex % palette.length] & 0xFFFFFF);
            context.drawText(tr, ch, (int) cursor + 1, 1, 0xFF000000, false);
            context.drawText(tr, ch, (int) cursor, 0, color, false);
            cursor += tr.getWidth(ch);
            charIndex++;
        }
        context.getMatrices().popMatrix();
    }
}