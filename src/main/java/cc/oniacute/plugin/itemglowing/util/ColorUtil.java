package cc.oniacute.plugin.itemglowing.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 颜色工具类
 * 支持以下格式：
 * - {@code &a}, {@code &l} 等传统 & 颜色码
 * - {@code &#RRGGBB} 十六进制 RGB 颜色
 */
public final class ColorUtil {

    /** 匹配 &#RRGGBB 格式（也兼容 § 变体） */
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([0-9a-fA-F]{6})");

    /** Legacy 序列化器，使用 & 作为代理字符 */
    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.builder()
                    .character('&')
                    .hexColors()
                    .useUnusualXRepeatedCharacterHexFormat()
                    .build();

    private ColorUtil() {}

    /**
     * 将包含 & 颜色码和 &#RRGGBB 颜色的字符串解析为 Adventure Component。
     *
     * @param raw 含颜色码的原始字符串
     * @return 解析后的 Component
     */
    public static Component parse(String raw) {
        if (raw == null || raw.isEmpty()) return Component.empty();
        // 将 &#RRGGBB 转换为 LegacyComponentSerializer 能识别的 §x§R§R§G§G§B§B 格式
        String converted = convertHexToLegacy(raw);
        return LEGACY.deserialize(converted);
    }

    /**
     * 解析颜色字符串，但保留 {placeholder} 占位符不变。
     * 先进行占位符提取，再解析颜色，最后重新组合。
     *
     * @param raw 含颜色码和占位符的原始字符串
     * @return 解析后的 Component
     */
    public static Component parseWithPlaceholders(String raw) {
        // 直接 parse，占位符会作为普通文本保留
        return parse(raw);
    }

    /**
     * 去除字符串中所有颜色码，返回纯文本。
     *
     * @param raw 含颜色码的字符串
     * @return 纯文本
     */
    public static String stripColors(String raw) {
        if (raw == null) return "";
        String noHex = HEX_PATTERN.matcher(raw).replaceAll("");
        return noHex.replaceAll("&[0-9a-fA-FklmnorKLMNOR]", "");
    }

    /**
     * 将 {@code &#RRGGBB} 格式转换为 LegacyComponentSerializer 能识别的
     * {@code §x§R§R§G§G§B§B} 格式（其中 R/G/B 是十六进制字符）。
     */
    static String convertHexToLegacy(String input) {
        Matcher m = HEX_PATTERN.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String hex = m.group(1);
            StringBuilder replacement = new StringBuilder("&x");
            for (char c : hex.toCharArray()) {
                replacement.append('&').append(c);
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement.toString()));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * 从 &#RRGGBB 格式的字符串中提取 TextColor。
     * 如果字符串本身就是 &#RRGGBB 格式，返回对应颜色；否则返回 null。
     *
     * @param raw &#RRGGBB 格式字符串（如 {@code "&#6cd3ff"}）
     * @return TextColor，解析失败返回 null
     */
    public static net.kyori.adventure.text.format.TextColor parseTextColor(String raw) {
        if (raw == null) return null;
        raw = raw.trim();
        // 支持 &#RRGGBB 和 #RRGGBB 两种写法
        String hex = null;
        if (raw.startsWith("&#") && raw.length() == 8) {
            hex = raw.substring(1); // 变为 #RRGGBB
        } else if (raw.startsWith("#") && raw.length() == 7) {
            hex = raw;
        }
        if (hex == null) return null;
        try {
            return net.kyori.adventure.text.format.TextColor.fromHexString(hex);
        } catch (Exception e) {
            return null;
        }
    }
}
