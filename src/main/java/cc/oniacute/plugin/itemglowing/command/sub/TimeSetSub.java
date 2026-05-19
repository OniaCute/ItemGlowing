package cc.oniacute.plugin.itemglowing.command.sub;

import cc.oniacute.plugin.itemglowing.config.ConfigManager;
import cc.oniacute.plugin.itemglowing.config.PluginConfig;
import cc.oniacute.plugin.itemglowing.quality.ItemQuality;
import cc.oniacute.plugin.itemglowing.quality.RarityManager;
import cc.oniacute.plugin.itemglowing.util.MessageUtil;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * /itemglowing timeset <品质> <seconds>
 * 将指定品质的掉落物消失时间设为指定秒数（>= 1）。
 * 使用 messages.timeset 消息模板，支持 {rarity} 和 {time} 占位符。
 */
public final class TimeSetSub implements SubCommand {

    private final ConfigManager configManager;

    public TimeSetSub(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        PluginConfig cfg = configManager.current();

        if (args.length < 2) {
            MessageUtil.send(sender, cfg.msg("invalid"), cfg.prefix());
            return true;
        }

        // 解析品质
        ItemQuality quality = RarityManager.parseQuality(args[0]);
        if (quality == null) {
            MessageUtil.send(sender, cfg.msg("invalid"), cfg.prefix(),
                    Map.of("input", args[0]));
            return true;
        }

        // 解析秒数
        int seconds;
        try {
            seconds = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            MessageUtil.send(sender, cfg.msg("invalid"), cfg.prefix(),
                    Map.of("input", args[1]));
            return true;
        }

        if (seconds < 1) {
            MessageUtil.send(sender, cfg.msg("invalid"), cfg.prefix(),
                    Map.of("input", args[1]));
            return true;
        }

        configManager.setDespawnSeconds(quality, seconds);
        PluginConfig newCfg = configManager.current();
        MessageUtil.send(sender, newCfg.msg("timeset"), newCfg.prefix(),
                Map.of("rarity", quality.name(), "time", String.valueOf(seconds)));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // 第一参数：品质名称补全
            String prefix = args[0].toUpperCase();
            List<String> result = new ArrayList<>();
            for (String name : RarityManager.qualityNames()) {
                if (name.startsWith(prefix)) result.add(name);
            }
            return result;
        }
        // 第二参数：数字，无固定补全
        return Collections.emptyList();
    }
}
