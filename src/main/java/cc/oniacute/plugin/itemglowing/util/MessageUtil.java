package cc.oniacute.plugin.itemglowing.util;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

import java.util.Map;

/**
 * 消息发送工具类。
 * 从配置模板解析颜色/占位符后发送给 CommandSender。
 */
public final class MessageUtil {

    private MessageUtil() {}

    /**
     * 向 CommandSender 发送一条消息。
     *
     * @param sender   接收者
     * @param template 消息模板（含 & 颜色码 和 {placeholder}）
     * @param prefix   {prefix} 对应的前缀字符串（含颜色码）
     * @param values   其余占位符 → 值 映射（可为 null）
     */
    public static void send(CommandSender sender, String template, String prefix,
                            Map<String, String> values) {
        Component component = PlaceholderUtil.parseMessage(template, prefix, values);
        sender.sendMessage(component);
    }

    /**
     * 向 CommandSender 发送简单消息（无额外占位符）。
     */
    public static void send(CommandSender sender, String template, String prefix) {
        send(sender, template, prefix, null);
    }
}
