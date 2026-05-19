package cc.oniacute.plugin.itemglowing.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import java.util.Map;

/**
 * 占位符替换工具类。
 *
 * <h3>名牌占位符</h3>
 * <ul>
 *   <li>{@code {rarityColor}} — 品质颜色码字符串（如 {@code &#2dabff}），由调用方传入展开后的字符串</li>
 *   <li>{@code {item}}        — 物品本地化名称（TranslatableComponent），作为 Component 插入</li>
 *   <li>{@code {amount}}      — 物品堆叠数量</li>
 *   <li>{@code {time}}        — 剩余秒数</li>
 * </ul>
 *
 * <h3>消息占位符</h3>
 * <ul>
 *   <li>{@code {prefix}} — 消息前缀</li>
 *   <li>{@code {item}}   — 物品 Material 名称（字符串，命令反馈用）</li>
 *   <li>{@code {items}}  — 忽略名单数量</li>
 *   <li>{@code {input}}  — 用户原始输入</li>
 * </ul>
 */
public final class PlaceholderUtil {

    private PlaceholderUtil() {}

    // ─────────────────────────────────────────────────────────────────────────
    //  消息构建（纯字符串替换 → 颜色解析 → Component）
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 将消息模板替换 {prefix} 和其余占位符后，通过 {@link ColorUtil#parse} 解析颜色，返回 Component。
     *
     * @param template 消息模板（含 & 颜色码和 {placeholder}）
     * @param prefix   {prefix} 对应的前缀字符串
     * @param values   其余占位符 → 值 映射（可为 null）
     * @return 解析后的 Component
     */
    public static Component parseMessage(String template, String prefix, Map<String, String> values) {
        if (template == null) return Component.empty();
        String s = template.replace("{prefix}", prefix != null ? prefix : "");
        if (values != null) {
            for (Map.Entry<String, String> entry : values.entrySet()) {
                s = s.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return ColorUtil.parse(s);
    }

    /**
     * 无额外占位符的消息解析（仅替换 {prefix}）。
     */
    public static Component parseMessage(String template, String prefix) {
        return parseMessage(template, prefix, null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  名牌构建（含 {item} TranslatableComponent + {rarityColor} + {amount} + {time}）
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 构建名牌 Component。
     *
     * <p>处理顺序：
     * <ol>
     *   <li>将 {@code {rarityColor}} 替换为 {@code rarityColorHex}（如 {@code &#2dabff}）</li>
     *   <li>将 {@code {time}} 替换为 {@code remainSeconds}</li>
     *   <li>将 {@code {amount}} 替换为 {@code amount}</li>
     *   <li>按 {@code {item}} 切分，分段解析颜色后夹入 {@code itemComponent}（TranslatableComponent）</li>
     * </ol>
     * </p>
     *
     * @param template       名牌模板字符串
     * @param itemComponent  物品本地化名称 Component（由 {@code Component.translatable(...)} 生成）
     * @param rarityColorHex 品质颜色 hex 字符串，如 {@code "&#2dabff"}
     * @param amount         物品堆叠数量
     * @param remainSeconds  剩余秒数
     * @return 构建好的名牌 Component
     */
    public static Component buildNametag(String template, Component itemComponent,
                                         String rarityColorHex, int amount, int remainSeconds) {
        if (template == null || template.isEmpty()) return Component.empty();

        // 1. 替换纯字符串占位符
        String s = template
                .replace("{rarityColor}", rarityColorHex != null ? rarityColorHex : "")
                .replace("{time}",        String.valueOf(remainSeconds))
                .replace("{amount}",      String.valueOf(amount));

        // 2. 按 {item} 切分，分段解析后夹入 itemComponent
        String[] parts = s.split("\\{item}", -1);
        if (parts.length == 1) {
            // 模板中没有 {item}，直接解析
            return ColorUtil.parse(s);
        }

        TextComponent.Builder builder = Component.text();
        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                builder.append(ColorUtil.parse(parts[i]));
            }
            if (i < parts.length - 1) {
                builder.append(itemComponent);
            }
        }
        return builder.build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  纯字符串替换工具
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 在纯字符串中替换占位符（不做颜色解析，用于预处理或日志）。
     */
    public static String replacePlain(String template, Map<String, String> values) {
        if (template == null) return "";
        String result = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}
