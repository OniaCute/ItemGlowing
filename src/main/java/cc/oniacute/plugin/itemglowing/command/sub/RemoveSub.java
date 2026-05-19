package cc.oniacute.plugin.itemglowing.command.sub;

import cc.oniacute.plugin.itemglowing.config.ConfigManager;
import cc.oniacute.plugin.itemglowing.config.PluginConfig;
import cc.oniacute.plugin.itemglowing.util.MaterialUtil;
import cc.oniacute.plugin.itemglowing.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * /itemglowing remove &lt;material&gt;
 * 将物品从忽略名单中移除。
 */
public final class RemoveSub implements SubCommand {

    private final ConfigManager configManager;

    public RemoveSub(ConfigManager configManager) {
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

        boolean removed = configManager.removeIgnored(material);
        if (removed) {
            PluginConfig newCfg = configManager.current();
            MessageUtil.send(sender, newCfg.msg("removed"), newCfg.prefix(),
                    Map.of("item", material.name()));
        } else {
            // 不在名单中，返回 invalid
            MessageUtil.send(sender, cfg.msg("invalid"), cfg.prefix(),
                    Map.of("input", args[0]));
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // 补全当前忽略名单中的物品名
            PluginConfig cfg = configManager.current();
            String prefix = args[0].toUpperCase();
            List<String> result = new ArrayList<>();
            for (Material mat : cfg.ignoredItems) {
                if (mat.name().startsWith(prefix)) {
                    result.add(mat.name());
                }
            }
            return result;
        }
        return Collections.emptyList();
    }
}
