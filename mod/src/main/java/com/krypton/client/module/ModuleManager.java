package com.krypton.client.module;

import com.krypton.client.KryptonClient;
import com.krypton.client.module.Category;
import com.krypton.client.module.Module;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ModuleManager {
    private final List<Module> modules = new ArrayList<>();

    public void register(Module module) {
        modules.add(module);
    }

    public void registerAll(Module... mods) {
        for (Module m : mods) register(m);
    }

    public List<Module> getModules() {
        return modules;
    }

    public List<Module> getModules(Category category) {
        return modules.stream().filter(m -> m.category == category)
                .sorted(Comparator.comparing(m -> m.name))
                .collect(Collectors.toList());
    }

    public Optional<Module> get(String name) {
        String lower = name.toLowerCase();
        return modules.stream().filter(m -> m.name.toLowerCase().equals(lower)).findFirst();
    }

    public void onTick() {
        for (Module m : modules) {
            if (m.isEnabled()) m.onTick();
            m.onTickAlways();
        }
    }

    public void onKey(int key) {
        for (Module m : modules) {
            if (m.getKey() == key) {
                m.toggle();
                KryptonClient.sendChatMessage("§7[§9Krypton§7] §f" + m.name + " §7" + (m.isEnabled() ? "§aON" : "§cOFF"));
            }
        }
    }

    public void saveAll() {
        KryptonClient.CONFIG.saveModules(this);
    }

    public void loadAll() {
        KryptonClient.CONFIG.loadModules(this);
    }
}