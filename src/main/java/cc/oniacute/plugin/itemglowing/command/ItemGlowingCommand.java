package cc.oniacute.plugin.itemglowing.command;

import cc.oniacute.plugin.itemglowing.command.sub.*;
import cc.oniacute.plugin.itemglowing.config.ConfigManager;
import cc.oniacute.plugin.itemglowing.config.PluginConfig;
import cc.oniacute.plugin.itemglowing.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.*;

/**
 * /itemglowing 主命令处理器。
 * <p>
 * 负责权限校验、子命令分发与 Tab 补全。
 * </p>
 */
public final class ItemGlowingCommand implements CommandExecutor, TabCompleter {

    private static final String PERMISSION = "itemglowing.admin";

    private final ConfigManager configManager;
    private final Map<String, SubCommand> subCommands;

    public ItemGlowingCommand(ConfigManager configManager) {
        this.configManager = configManager;

        subCommands = new LinkedHashMap<>();
        subCommands.put("add",       new AddSub(configManager));
        subCommands.put("remove",    new RemoveSub(configManager));
        subCommands.put("reload",    new ReloadSub(configManager));
        subCommands.put("timeset",   new TimeSetSub(configManager));
        subCommands.put("setrarity", new SetRaritySub(configManager));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CommandExecutor
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        PluginConfig cfg = configManager.current();

        // 权限检查
        if (!sender.hasPermission(PERMISSION)) {
            MessageUtil.send(sender, cfg.msg("noPromission"), cfg.prefix());
            return true;
        }

        // 无子命令 → 显示用法
        if (args.length == 0) {
            MessageUtil.send(sender, cfg.msg("invalid"), cfg.prefix());
            return true;
        }

        String subName = args[0].toLowerCase();
        SubCommand sub = subCommands.get(subName);

        if (sub == null) {
            MessageUtil.send(sender, cfg.msg("invalid"), cfg.prefix(),
                    Map.of("input", args[0]));
            return true;
        }

        // 截取子命令之后的参数
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        sub.execute(sender, subArgs);
        return true;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  TabCompleter
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String label, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            // 补全子命令名
            String prefix = args[0].toLowerCase();
            List<String> result = new ArrayList<>();
            for (String name : subCommands.keySet()) {
                if (name.startsWith(prefix)) result.add(name);
            }
            return result;
        }

        if (args.length >= 2) {
            String subName = args[0].toLowerCase();
            SubCommand sub = subCommands.get(subName);
            if (sub != null) {
                String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
                return sub.tabComplete(sender, subArgs);
            }
        }

        return Collections.emptyList();
    }
}
