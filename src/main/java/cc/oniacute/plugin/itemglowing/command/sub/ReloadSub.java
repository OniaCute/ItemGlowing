package cc.oniacute.plugin.itemglowing.command.sub;

import cc.oniacute.plugin.itemglowing.config.ConfigManager;
import cc.oniacute.plugin.itemglowing.config.PluginConfig;
import cc.oniacute.plugin.itemglowing.util.MessageUtil;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * /itemglowing reload
 * 从磁盘重新加载配置文件。
 */
public final class ReloadSub implements SubCommand {

    private final ConfigManager configManager;

    public ReloadSub(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        try {
            configManager.reload();
            PluginConfig cfg = configManager.current();
            MessageUtil.send(sender, cfg.msg("reloaded"), cfg.prefix(),
                    Map.of("items", String.valueOf(cfg.ignoredItems.size())));
        } catch (Exception e) {
            // reload 抛出异常时，尝试从旧快照取 error 消息；若快照也为空则硬编码
            PluginConfig cfg = configManager.current();
            if (cfg != null) {
                MessageUtil.send(sender, cfg.msg("error"), cfg.prefix());
            } else {
                sender.sendMessage("§cItemGlowing: 重载失败，请检查配置文件是否正确!");
            }
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
