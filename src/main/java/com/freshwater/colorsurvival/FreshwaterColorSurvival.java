package com.freshwater.colorsurvival;

import com.freshwater.colorsurvival.bingo.BingoManager;
import com.freshwater.colorsurvival.color.BlockColorMapper;
import com.freshwater.colorsurvival.commands.FwfishColorsCommand;
import com.freshwater.colorsurvival.config.PluginConfig;
import com.freshwater.colorsurvival.game.GameManager;
import com.freshwater.colorsurvival.game.PunishmentManager;
import com.freshwater.colorsurvival.listeners.BingoListener;
import com.freshwater.colorsurvival.listeners.RestrictionListener;
import com.freshwater.colorsurvival.ui.CardGui;
import com.freshwater.colorsurvival.ui.HudManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * FreshwaterColorSurvival 主类。
 * 玩法：每名玩家分配一种颜色，只能破坏/使用/放置自己颜色的方块，配合两队 5x5 Bingo 连线竞速。
 * 作者：淡水岛开发组
 */
public final class FreshwaterColorSurvival extends JavaPlugin {

    private PluginConfig pluginConfig;
    private BlockColorMapper mapper;
    private GameManager gameManager;
    private BingoManager bingoManager;
    private PunishmentManager punishmentManager;
    private HudManager hudManager;

    @Override
    public void onEnable() {
        this.pluginConfig = new PluginConfig(this);

        this.mapper = new BlockColorMapper();
        this.mapper.setOverrides(pluginConfig.getBlockColorOverrides());

        this.gameManager = new GameManager(this, pluginConfig);
        this.bingoManager = new BingoManager(this, pluginConfig);
        this.bingoManager.setGame(gameManager);
        this.punishmentManager = new PunishmentManager(this, pluginConfig);
        this.hudManager = new HudManager(this);
        this.hudManager.setGame(gameManager);
        this.gameManager.wire(bingoManager, punishmentManager, hudManager);
        this.hudManager.enable();

        CardGui cardGui = new CardGui(gameManager);

        getServer().getPluginManager().registerEvents(
                new RestrictionListener(gameManager, pluginConfig, mapper), this);
        getServer().getPluginManager().registerEvents(new BingoListener(bingoManager), this);
        getServer().getPluginManager().registerEvents(cardGui, this);

        FwfishColorsCommand command = new FwfishColorsCommand(this, gameManager, pluginConfig, mapper, cardGui);
        PluginCommand pc = getCommand("fwfish-colors");
        if (pc != null) {
            pc.setExecutor(command);
            pc.setTabCompleter(command);
        } else {
            getLogger().severe("命令 fwfish-colors 注册失败，请检查 plugin.yml。");
        }

        getLogger().info("================================================");
        getLogger().info(" FreshwaterColorSurvival 已启用");
        getLogger().info(" 颜色生存 + Bingo  ·  作者：淡水岛开发组");
        getLogger().info("================================================");
    }

    @Override
    public void onDisable() {
        if (bingoManager != null) {
            bingoManager.stopScanTask();
        }
        if (gameManager != null) {
            gameManager.reset();
        }
        if (hudManager != null) {
            hudManager.disable();
        }
        getLogger().info("FreshwaterColorSurvival 已卸载。  作者：淡水岛开发组");
    }
}
