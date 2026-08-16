package com.krypton.client;

import com.krypton.client.client.ClickGUI;
import com.krypton.client.client.ConfigManager;
import com.krypton.client.client.HudRenderer;
import com.krypton.client.module.Module;
import com.krypton.client.module.ModuleManager;
import com.krypton.client.modules.combat.AimAssist;
import com.krypton.client.modules.combat.AutoClicker;
import com.krypton.client.modules.combat.Reach;
import com.krypton.client.modules.combat.TriggerBot;
import com.krypton.client.modules.hud.Coords;
import com.krypton.client.modules.hud.Fps;
import com.krypton.client.modules.hud.Keystrokes;
import com.krypton.client.modules.hud.SessionInfo;
import com.krypton.client.modules.hud.Watermark;
import com.krypton.client.modules.misc.CustomTitle;
import com.krypton.client.modules.misc.ScreenshotName;
import com.krypton.client.modules.movement.AutoSprint;
import com.krypton.client.modules.movement.NoFall;
import com.krypton.client.modules.movement.Sprint;
import com.krypton.client.modules.player.HitBox;
import com.krypton.client.modules.render.FullBright;
import com.krypton.client.modules.render.NoHurtCam;
import com.krypton.client.modules.render.ViewModel;
import com.krypton.client.modules.render.Zoom;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class KryptonClient implements ClientModInitializer {
    public static final String MOD_ID = "krypton";
    public static final String MOD_NAME = "Krypton Client";

    public static ModuleManager MODULE_MANAGER;
    public static ClickGUI CLICK_GUI;
    public static HudRenderer HUD_RENDERER;
    public static ConfigManager CONFIG;

    public static KeyBinding clickGuiKey;
    public static KeyBinding hudKey;
    public static KeyBinding zoomKey;

    public static int hudEditingModule = -1;

    @Override
    public void onInitializeClient() {
        CONFIG = new ConfigManager();
        MODULE_MANAGER = new ModuleManager();
        CLICK_GUI = new ClickGUI();
        HUD_RENDERER = new HudRenderer();

        registerModules();
        MODULE_MANAGER.loadAll();

        clickGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.krypton.clickgui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, KeyBinding.Category.MISC));
        hudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.krypton.hud", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_CONTROL, KeyBinding.Category.MISC));
        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.krypton.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, KeyBinding.Category.MISC));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            MODULE_MANAGER.onTick();
            while (clickGuiKey.wasPressed()) {
                if (client.currentScreen instanceof ClickGUI) {
                    client.setScreen(null);
                } else {
                    client.setScreen(CLICK_GUI);
                }
            }
            while (hudKey.wasPressed()) {
                HUD_RENDERER.editing = !HUD_RENDERER.editing;
                sendChatMessage(HUD_RENDERER.editing ? "§7HUD §aediting mode on" : "§7HUD §cediting mode off");
            }
        });
    }

    private void registerModules() {
        List<Module> modules = List.of(
                // Combat
                new AutoClicker(),
                new AimAssist(),
                new Reach(),
                new TriggerBot(),
                new HitBox(),
                // Movement
                new AutoSprint(),
                new Sprint(),
                new NoFall(),
                // Render
                new FullBright(),
                new NoHurtCam(),
                new ViewModel(),
                new Zoom(),
                // HUD
                new Fps(),
                new Coords(),
                new Keystrokes(),
                new SessionInfo(),
                new Watermark(),
                // Misc
                new ScreenshotName(),
                new CustomTitle()
        );
        MODULE_MANAGER.registerAll(modules.toArray(new Module[0]));
    }

    public static void sendChatMessage(String msg) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(net.minecraft.text.Text.literal(msg), false);
        }
    }
}