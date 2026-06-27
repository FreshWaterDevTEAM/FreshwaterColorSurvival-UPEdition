package com.freshwater.colorsurvival.game;

import com.freshwater.colorsurvival.config.PluginConfig;
import com.freshwater.colorsurvival.util.Text;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 两阶段惩罚：开局 GRACE，15 分钟（可配置）后自动转 PUNISH；
 * PUNISH 阶段对违规者扣血并随机清除一个物品。管理员可手动开关。
 * 作者：淡水岛开发组
 */
public final class PunishmentManager {

    private final JavaPlugin plugin;
    private final PluginConfig config;

    private PunishPhase phase = PunishPhase.GRACE;
    private BukkitTask autoTask;
    private long autoEnableAtMillis = -1;
    private final Random random = new Random();
    private final Map<UUID, Long> cooldownUntil = new ConcurrentHashMap<>();

    public PunishmentManager(JavaPlugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public PunishPhase getPhase() {
        return phase;
    }

    public boolean isPunishPhase() {
        return phase == PunishPhase.PUNISH;
    }

    public void onGameStart() {
        cancelAutoTask();
        cooldownUntil.clear();
        phase = PunishPhase.GRACE;
        int minutes = Math.max(0, config.getAutoEnableAfterMinutes());
        if (minutes == 0) {
            enablePunish(false);
            return;
        }
        long delayTicks = minutes * 60L * 20L;
        autoEnableAtMillis = System.currentTimeMillis() + minutes * 60_000L;
        autoTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (phase == PunishPhase.GRACE) {
                enablePunish(false);
            }
        }, delayTicks);
        Bukkit.broadcast(Text.amp(config.prefix() + "&7当前为 &a宽容阶段&7，" + minutes
                + " 分钟后将自动进入 &c惩罚阶段&7。"));
    }

    public void onGameStop() {
        cancelAutoTask();
        cooldownUntil.clear();
        phase = PunishPhase.GRACE;
        autoEnableAtMillis = -1;
    }

    /** 手动或自动进入惩罚阶段。 */
    public void enablePunish(boolean manual) {
        cancelAutoTask();
        if (phase == PunishPhase.PUNISH) {
            return;
        }
        phase = PunishPhase.PUNISH;
        Bukkit.broadcast(Text.amp(config.prefix() + "&c惩罚阶段已开启！" + (manual ? "（管理员手动）" : "（自动）")
                + " &7操作错误颜色将扣血并随机丢失物品。"));
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showTitle(Title.title(
                    Text.amp("&c惩罚阶段"),
                    Text.amp("&7小心！操作错误颜色会受罚"),
                    Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(3), Duration.ofMillis(800))));
        }
    }

    /** 手动关闭惩罚阶段，回到宽容阶段（不再自动开启）。 */
    public void disablePunish() {
        cancelAutoTask();
        phase = PunishPhase.GRACE;
        autoEnableAtMillis = -1;
        Bukkit.broadcast(Text.amp(config.prefix() + "&a惩罚阶段已关闭，回到宽容阶段。"));
    }

    /** 距离自动开启惩罚的剩余秒数；无计划返回 -1。 */
    public long remainingSecondsToAuto() {
        if (phase == PunishPhase.PUNISH || autoEnableAtMillis < 0) {
            return -1;
        }
        long diff = autoEnableAtMillis - System.currentTimeMillis();
        return Math.max(0, diff / 1000L);
    }

    /** 在违规发生时调用。仅惩罚阶段会真正执行惩罚。 */
    public void applyViolation(Player player) {
        if (phase != PunishPhase.PUNISH) {
            return;
        }
        long now = System.currentTimeMillis();
        Long until = cooldownUntil.get(player.getUniqueId());
        if (until != null && until > now) {
            return;
        }
        cooldownUntil.put(player.getUniqueId(), now + config.getCooldownSeconds() * 1000L);

        double damage = config.getPunishDamage();
        if (damage > 0) {
            player.damage(damage);
        }
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);

        if (config.isClearRandomItem()) {
            clearRandomItem(player);
        }
    }

    private void clearRandomItem(Player player) {
        PlayerInventory inv = player.getInventory();
        ItemStack[] storage = inv.getStorageContents();
        List<Integer> nonEmpty = new ArrayList<>();
        for (int i = 0; i < storage.length; i++) {
            ItemStack it = storage[i];
            if (it != null && !it.getType().isAir()) {
                nonEmpty.add(i);
            }
        }
        if (nonEmpty.isEmpty()) {
            return;
        }
        int slot = nonEmpty.get(random.nextInt(nonEmpty.size()));
        ItemStack removed = storage[slot];
        inv.setItem(slot, null);
        player.sendMessage(Text.amp(config.prefix() + "&c惩罚：你失去了 &f"
                + removed.getType().name() + " x" + removed.getAmount()));
    }

    private void cancelAutoTask() {
        if (autoTask != null) {
            autoTask.cancel();
            autoTask = null;
        }
    }
}
