package com.krypton.client.client;

import com.google.gson.*;
import com.krypton.client.module.Module;
import com.krypton.client.module.ModuleManager;
import com.krypton.client.module.setting.*;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {
    private final Path configDir = FabricLoader.getInstance().getConfigDir().resolve("krypton");
    private final Path configFile = configDir.resolve("config.json");
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public void saveModules(ModuleManager manager) {
        try {
            Files.createDirectories(configDir);
            JsonObject root = new JsonObject();
            JsonArray modulesArr = new JsonArray();
            for (Module m : manager.getModules()) {
                JsonObject mo = new JsonObject();
                mo.addProperty("name", m.name);
                mo.addProperty("enabled", m.isEnabled());
                mo.addProperty("key", m.getKey());
                JsonArray settingsArr = new JsonArray();
                for (Setting<?> s : m.getSettings()) {
                    JsonObject so = new JsonObject();
                    so.addProperty("name", s.name);
                    if (s instanceof BooleanSetting) {
                        so.addProperty("value", ((BooleanSetting) s).get());
                    } else if (s instanceof NumberSetting) {
                        so.addProperty("value", ((NumberSetting) s).get());
                    } else if (s instanceof ModeSetting) {
                        so.addProperty("value", ((ModeSetting) s).get());
                    } else if (s instanceof StringSetting) {
                        so.addProperty("value", ((StringSetting) s).get());
                    }
                    settingsArr.add(so);
                }
                mo.add("settings", settingsArr);
                modulesArr.add(mo);
            }
            root.add("modules", modulesArr);
            Files.writeString(configFile, gson.toJson(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadModules(ModuleManager manager) {
        if (!Files.exists(configFile)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(configFile)).getAsJsonObject();
            JsonArray modulesArr = root.getAsJsonArray("modules");
            for (JsonElement me : modulesArr) {
                JsonObject mo = me.getAsJsonObject();
                String name = mo.get("name").getAsString();
                manager.get(name).ifPresent(m -> {
                    if (mo.has("enabled")) m.setEnabled(mo.get("enabled").getAsBoolean());
                    if (mo.has("key")) m.setKey(mo.get("key").getAsInt());
                    if (mo.has("settings")) {
                        JsonArray settingsArr = mo.getAsJsonArray("settings");
                        for (JsonElement se : settingsArr) {
                            JsonObject so = se.getAsJsonObject();
                            String sName = so.get("name").getAsString();
                            for (Setting<?> s : m.getSettings()) {
                                if (!s.name.equals(sName)) continue;
                                JsonElement val = so.get("value");
                                if (s instanceof BooleanSetting && val.isJsonPrimitive() && val.getAsJsonPrimitive().isBoolean()) {
                                    ((BooleanSetting) s).set(val.getAsBoolean());
                                } else if (s instanceof NumberSetting && val.isJsonPrimitive() && val.getAsJsonPrimitive().isNumber()) {
                                    ((NumberSetting) s).set(val.getAsDouble());
                                } else if (s instanceof ModeSetting && val.isJsonPrimitive()) {
                                    ((ModeSetting) s).set(val.getAsString());
                                } else if (s instanceof StringSetting && val.isJsonPrimitive()) {
                                    ((StringSetting) s).setText(val.getAsString());
                                }
                                break;
                            }
                        }
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}