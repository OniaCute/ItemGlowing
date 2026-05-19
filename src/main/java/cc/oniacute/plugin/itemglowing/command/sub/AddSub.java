package cc.oniacute.plugin.itemglowing.command.sub;

import cc.oniacute.plugin.itemglowing.config.ConfigManager;
import cc.oniacute.plugin.itemglowing.config.PluginConfig;
import cc.oniacute.plugin.itemglowing.util.MaterialUtil;
import cc.oniacute.plugin.itemglowing.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * /itemglowing add &lt;material&gt;
 * 将物品添加到忽略名单。
 */
public final class AddSub implements SubCommand {

    private final ConfigManager configManager;

    public AddSub(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        PluginConfig cfg = configManager.current();

        if (args.length < 1) {
            MessageUtil.send(sender, cfg.msg("invalid"), cfg.prefix());
            return true;
        }

        Material material = MaterialUtil.fromName(args[0]);
        if (material == null) {
            MessageUtil.send(sender, cfg.msg("invalid"), cfg.prefix(),
                    Map.of("input", args[0]));
            return true;
        }

        boolean added = configManager.addIgnored(material);
        if (added) {
            // 重载后获取最新配置
            PluginConfig newCfg = configManager.current();
            MessageUtil.send(sender, newCfg.msg("added"), newCfg.prefix(),
                    Map.of("item", material.name()));
        } else {
            // 已在名单中，仍视为合法操作，给出 added 提示
            MessageUtil.send(sender, cfg.msg("added"), cfg.prefix(),
                    Map.of("item", material.name()));
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return MaterialUtil.filterMaterials(args[0]);
        }
        return Collections.emptyList();
    }
}
