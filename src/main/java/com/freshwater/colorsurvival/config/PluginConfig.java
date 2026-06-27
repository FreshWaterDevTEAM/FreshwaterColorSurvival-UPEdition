package com.freshwater.colorsurvival.config;

import com.freshwater.colorsurvival.color.GameColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * 读取并缓存 config.yml，并提供运行时修改/保存能力。作者：淡水岛开发组
 */
public final class PluginConfig {

    private final JavaPlugin plugin;

    private boolean showActionBar;
    private boolean sidebarEnabled;
    private boolean utilityExempt;
    private final Set<Material> exemptMaterials = EnumSet.noneOf(Material.class);

    private int autoEnableAfterMinutes;
    private double punishDamage;
    private boolean clearRandomItem;
    private int cooldownSeconds;

    private final Map<String, String> messages = new HashMap<>();
    private final List<Material> bingoItems = new ArrayList<>();
    private final Map<Material, Set<GameColor>> blockColorOverrides = new HashMap<>();

    public PluginConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        load();
    }

    public void load() {
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();

        showActionBar = c.getBoolean("show-action-bar", true);
        sidebarEnabled = c.getBoolean("sidebar-enabled", true);
        utilityExempt = c.getBoolean("utility-exempt", true);

        exemptMaterials.clear();
        for (String s : c.getStringList("exempt-materials")) {
            Material m = Material.matchMaterial(s);
            if (m != null) {
                exemptMaterials.add(m);
            } else {
                plugin.getLogger().warning("exempt-materials 中存在未知方块: " + s);
            }
        }

        autoEnableAfterMinutes = c.getInt("punishment.auto-enable-after-minutes", 15);
        punishDamage = c.getDouble("punishment.damage", 2.0);
        clearRandomItem = c.getBoolean("punishment.clear-random-item", true);
        cooldownSeconds = c.getInt("punishment.cooldown-seconds", 3);

        messages.clear();
        ConfigurationSection msgSec = c.getConfigurationSection("messages");
        if (msgSec != null) {
            for (String key : msgSec.getKeys(false)) {
                messages.put(key, msgSec.getString(key, ""));
            }
        }

        bingoItems.clear();
        for (String s : c.getStringList("bingo-items")) {
            Material m = Material.matchMaterial(s);
            if (m != null && m.isItem()) {
                bingoItems.add(m);
            } else {
                plugin.getLogger().warning("bingo-items 中存在无效物品: " + s);
            }
        }

        blockColorOverrides.clear();
        ConfigurationSection bc = c.getConfigurationSection("block-colors");
        if (bc != null) {
            for (String key : bc.getKeys(false)) {
                Material m = Material.matchMaterial(key);
                if (m == null) {
                    plugin.getLogger().log(Level.WARNING, "block-colors 中无效方块: {0}", key);
                    continue;
                }
                Set<GameColor> colors = parseColors(bc, key);
                if (!colors.isEmpty()) {
                    blockColorOverrides.put(m, colors);
                } else {
                    plugin.getLogger().log(Level.WARNING, "block-colors 中无效颜色: {0}", key);
                }
            }
        }
    }

    private Set<GameColor> parseColors(ConfigurationSection sec, String key) {
        Set<GameColor> colors = EnumSet.noneOf(GameColor.class);
        List<String> tokens = new ArrayList<>();
        if (sec.isList(key)) {
            tokens.addAll(sec.getStringList(key));
        } else {
            String raw = sec.getString(key, "");
            tokens.addAll(Arrays.asList(raw.split("[,/\\s]+")));
        }
        for (String t : tokens) {
            GameColor color = GameColor.fromName(t);
            if (color != null) {
                colors.add(color);
            }
        }
        return colors;
    }

    public boolean isShowActionBar() {
        return showActionBar;
    }

    public boolean isSidebarEnabled() {
        return sidebarEnabled;
    }

    public void setSidebarEnabled(boolean value) {
        sidebarEnabled = value;
        plugin.getConfig().set("sidebar-enabled", value);
        plugin.saveConfig();
    }

    public boolean isUtilityExempt() {
        return utilityExempt;
    }

    public Set<Material> getExemptMaterials() {
        return exemptMaterials;
    }

    public int getAutoEnableAfterMinutes() {
        return autoEnableAfterMinutes;
    }

    public double getPunishDamage() {
        return punishDamage;
    }

    public boolean isClearRandomItem() {
        return clearRandomItem;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public List<Material> getBingoItems() {
        return bingoItems;
    }

    public Map<Material, Set<GameColor>> getBlockColorOverrides() {
        return blockColorOverrides;
    }

    public String msg(String key) {
        return messages.getOrDefault(key, "");
    }

    public String prefix() {
        return messages.getOrDefault("prefix", "");
    }

    // ---- 运行时修改并持久化 ----

    public void setUtilityExempt(boolean value) {
        utilityExempt = value;
        plugin.getConfig().set("utility-exempt", value);
        plugin.saveConfig();
    }

    public boolean addExemptMaterial(Material material) {
        if (!exemptMaterials.add(material)) {
            return false;
        }
        List<String> list = plugin.getConfig().getStringList("exempt-materials");
        if (!list.contains(material.name())) {
            list.add(material.name());
        }
        plugin.getConfig().set("exempt-materials", list);
        plugin.saveConfig();
        return true;
    }

    public boolean removeExemptMaterial(Material material) {
        if (!exemptMaterials.remove(material)) {
            return false;
        }
        List<String> list = plugin.getConfig().getStringList("exempt-materials");
        list.remove(material.name());
        plugin.getConfig().set("exempt-materials", list);
        plugin.saveConfig();
        return true;
    }
}
