package com.krypton.client.client;

import com.krypton.client.KryptonClient;
import com.krypton.client.module.Category;
import com.krypton.client.module.Module;
import com.krypton.client.module.setting.BooleanSetting;
import com.krypton.client.module.setting.ModeSetting;
import com.krypton.client.module.setting.NumberSetting;
import com.krypton.client.module.setting.Setting;
import com.krypton.client.module.setting.StringSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ClickGUI extends Screen {
    private static final int PANEL_WIDTH = 110;
    private static final int PANEL_GAP = 8;
    private static final int HEADER_HEIGHT = 18;
    private static final int MODULE_HEIGHT = 14;
    private static final int SETTING_HEIGHT = 14;

    private final List<Panel> panels = new ArrayList<>();
    private boolean dragMode = false;
    private int dragX, dragY, dragOffsetX, dragOffsetY;
    private StringSetting focusedString = null;

    public ClickGUI() {
        super(Text.literal("Krypton ClickGUI"));
    }

    @Override
    protected void init() {
        super.init();
        if (!panels.isEmpty()) return;
        int x = 20;
        for (Category cat : Category.values()) {
            panels.add(new Panel(cat, x, 30));
            x += PANEL_WIDTH + PANEL_GAP;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        renderBackground(context, mouseX, mouseY, delta);
        if (dragMode) {
            dragX = mouseX - dragOffsetX;
            dragY = mouseY - dragOffsetY;
        }
        for (Panel p : panels) {
            p.render(context, mouseX, mouseY);
        }
        context.drawText(this.textRenderer, Text.literal("Krypton Client  §7[right shift to close]"),
                8, 6, 0xFFFFFFFF, true);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubleClick) {
        int mouseX = (int) click.x();
        int mouseY = (int) click.y();
        int button = click.button();
        for (Panel p : panels) {
            if (p.mouseClicked(mouseX, mouseY, button)) {
                if (button == 0 && mouseY >= p.y && mouseY <= p.y + HEADER_HEIGHT) {
                    dragMode = true;
                    dragOffsetX = mouseX - p.x;
                    dragOffsetY = mouseY - p.y;
                    draggingPanel = p;
                }
                return true;
            }
        }
        draggingPanel = null;
        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.gui.Click click) {
        dragMode = false;
        draggingPanel = null;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        for (Panel p : panels) {
            if (mouseX >= p.x && mouseX <= p.x + PANEL_WIDTH && mouseY >= p.y && mouseY <= p.y + p.height) {
                p.scroll(verticalAmount);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
        int keyCode = input.key();
        if (focusedString != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) {
                focusedString = null;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                focusedString.backspace();
                return true;
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharInput input) {
        if (focusedString != null) {
            String s = input.asString();
            if (s != null) {
                for (char c : s.toCharArray()) focusedString.append(c);
            }
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        KryptonClient.MODULE_MANAGER.saveAll();
        super.close();
    }

    private Panel draggingPanel;

    private class Panel {
        private final Category category;
        int x, y;
        private float scroll;
        private boolean extended = true;
        private int height;

        Panel(Category category, int x, int y) {
            this.category = category;
            this.x = x;
            this.y = y;
        }

        void render(DrawContext context, int mouseX, int mouseY) {
            List<Module> mods = KryptonClient.MODULE_MANAGER.getModules(category);
            int content = 0;
            if (extended) {
                content = mods.stream().mapToInt(m -> MODULE_HEIGHT + (m.isEnabled() && m.getSettings().size() > 0 ? m.getSettings().size() * SETTING_HEIGHT : 0)).sum();
            }
            height = HEADER_HEIGHT + (extended ? content : 0);

            context.fill(x, y, x + PANEL_WIDTH, y + height, 0xDD12141D);
            context.fill(x, y, x + PANEL_WIDTH, y + HEADER_HEIGHT, 0xEE20273A);

            int yOff = y;
            context.drawText(textRenderer, Text.literal(category.getDisplay() + (extended ? " ▼" : " ▶")),
                    x + 4, yOff + 5, 0xFF4F8CFF, true);
            yOff += HEADER_HEIGHT;

            if (!extended) return;

            for (Module m : mods) {
                int baseH = MODULE_HEIGHT;
                List<Setting<?>> settings = m.getSettings();
                boolean showSettings = m.isEnabled() && settings.size() > 0;
                int blockH = baseH + (showSettings ? settings.size() * SETTING_HEIGHT : 0);

                context.fill(x, yOff, x + PANEL_WIDTH, yOff + baseH, m.isEnabled() ? 0xAA35D07F : 0x33000000);
                context.drawText(textRenderer, Text.literal((m.isEnabled() ? "§a" : "§f") + m.name),
                        x + 4, yOff + 3, m.isEnabled() ? 0xFF35D07F : 0xFFE8EAF2, true);

                if (showSettings) {
                    int sOff = yOff + MODULE_HEIGHT;
                    for (Setting<?> s : settings) {
                        context.fill(x, sOff, x + PANEL_WIDTH, sOff + SETTING_HEIGHT, 0x221B1F31);
                        if (s instanceof BooleanSetting b) {
                            context.drawText(textRenderer, Text.literal((b.get() ? "§a✔ " : "§7✖ ") + s.name),
                                    x + 4, sOff + 3, 0xFFE8EAF2, true);
                        } else if (s instanceof NumberSetting n) {
                            context.drawText(textRenderer, Text.literal(s.name + ": §f" + n.getDisplayValue()),
                                    x + 4, sOff + 3, 0xFF8A90A8, true);
                        } else if (s instanceof ModeSetting mo) {
                            context.drawText(textRenderer, Text.literal(s.name + ": §f" + mo.get()),
                                    x + 4, sOff + 3, 0xFF8A90A8, true);
                        } else if (s instanceof StringSetting st) {
                            String label = st.name + ": ";
                            boolean focus = st == focusedString;
                            String shown = focus ? st.get() + "▌" : st.get();
                            context.drawText(textRenderer, Text.literal(label + (focus ? "§b" : "§f") + shown),
                                    x + 4, sOff + 3, focus ? 0xFF4FC3FF : 0xFF8A90A8, true);
                        }
                        sOff += SETTING_HEIGHT;
                    }
                }
                yOff += blockH;
            }
        }

        boolean mouseClicked(int mouseX, int mouseY, int button) {
            if (mouseX < x || mouseX > x + PANEL_WIDTH || mouseY < y || mouseY > y + height) return false;

            if (mouseY >= y && mouseY <= y + HEADER_HEIGHT) {
                if (button == 1) extended = !extended;
                return true;
            }

            if (!extended) return true;
            int yOff = y + HEADER_HEIGHT;
            for (Module m : KryptonClient.MODULE_MANAGER.getModules(category)) {
                int baseH = MODULE_HEIGHT;
                boolean showSettings = m.isEnabled() && m.getSettings().size() > 0;
                int blockH = baseH + (showSettings ? m.getSettings().size() * SETTING_HEIGHT : 0);

                if (mouseY >= yOff && mouseY <= yOff + baseH) {
                    if (button == 0) m.toggle();
                    return true;
                }
                if (showSettings) {
                    int sOff = yOff + MODULE_HEIGHT;
                    for (Setting<?> s : m.getSettings()) {
                        if (mouseY >= sOff && mouseY <= sOff + SETTING_HEIGHT) {
                            if (button == 0) {
                                if (s instanceof BooleanSetting b) b.toggle();
                                else if (s instanceof ModeSetting mo) mo.cycle(true);
                                else if (s instanceof StringSetting st) focusedString = st;
                            } else if (button == 1) {
                                if (s instanceof NumberSetting n) {
                                    n.set(n.get() - (mouseX > x + PANEL_WIDTH / 2 ? 0 : 0)); // left/right handled via scroll
                                }
                            } else if (button == 2) {
                                if (s instanceof ModeSetting mo) mo.cycle(false);
                            }
                            m.onSettingChanged(s);
                            return true;
                        }
                        sOff += SETTING_HEIGHT;
                    }
                }
                yOff += blockH;
            }
            return true;
        }

        void scroll(double amount) {
            if (amount > 0) scroll += 10; else scroll -= 10;
        }
    }
}