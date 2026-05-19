package cc.oniacute.plugin.itemglowing.command.sub;

import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * 子命令接口。
 */
public interface SubCommand {

    /**
     * 执行子命令。
     *
     * @param sender 命令执行者
     * @param args   子命令之后的参数（不含子命令名本身）
     * @return true=处理完毕；false=显示 usage
     */
    boolean execute(CommandSender sender, String[] args);

    /**
     * 提供 Tab 补全列表。
     *
     * @param sender 命令执行者
     * @param args   当前已输入的参数（不含子命令名）
     * @return 补全候选列表，不可为 null
     */
    List<String> tabComplete(CommandSender sender, String[] args);
}
