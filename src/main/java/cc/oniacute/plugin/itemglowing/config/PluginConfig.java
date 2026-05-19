package cc.oniacute.plugin.itemglowing.config;

import cc.oniacute.plugin.itemglowing.quality.ItemQuality;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * 插件配置的不可变快照。
 * <p>
 * 通过 {@link ConfigManager#current()} 获取当前生效的配置实例（AtomicReference 原子替换，线程安全）。
 * </p>
 */
public final class PluginConfig {

    // ── 发光开关 ──────────────────────────────────────────────────────────────
    /** 是否为掉落物添加发光效果 */
    public final boolean glowing;

    // ── 品质颜色（5级，始终按品质决定颜色）────────────────────────────────────
    /**
     * 5 级品质 → TextColor 映射。
     * key：COMMON / UNCOMMON / RARE / EPIC / LEGEND
     */
    public final Map<ItemQuality, TextColor> qualityColors;

    // ── 名牌 ─────────────────────────────────────────────────────────────────
    /**
     * 名牌模板，空字符串表示不显示。
     * <p>
     * 支持占位符：{@code {rarityColor}} {@code {item}} {@code {amount}} {@code {time}}
     * 以及 {@code &} 颜色码和 {@code &#RRGGBB} 十六进制颜色。
     * </p>
     */
    public final String nametagTemplate;

    // ── 计时/距离 ─────────────────────────────────────────────────────────────
    /**
     * 各品质掉落物消失时间（秒）。
     * key：COMMON / UNCOMMON / RARE / EPIC / LEGEND
     */
    public final Map<ItemQuality, Integer> despawnMap;

    /** 玩家可视半径（格），-1 = 始终显示 */
    public final int radius;

    /** 主循环检测间隔（Tick，最低 1） */
    public final int detectTimerTicks;

    // ── 消息 ─────────────────────────────────────────────────────────────────
    /**
     * 原始消息模板表。
     * key 列表：prefix / noPromission / invalid / added / removed / reloaded / error
     */
    public final Map<String, String> messages;

    // ── 忽略名单 ──────────────────────────────────────────────────────────────
    /** 已校验的 Material 忽略集合 */
    public final Set<Material> ignoredItems;

    public PluginConfig(
            boolean glowing,
            Map<ItemQuality, TextColor> qualityColors,
            String nametagTemplate,
            Map<ItemQuality, Integer> despawnMap,
            int radius,
            int detectTimerTicks,
            Map<String, String> messages,
            Set<Material> ignoredItems) {
        this.glowing = glowing;
        this.qualityColors = Collections.unmodifiableMap(new EnumMap<>(qualityColors));
        this.nametagTemplate = nametagTemplate != null ? nametagTemplate : "";
        this.despawnMap = Collections.unmodifiableMap(new EnumMap<>(despawnMap));
        this.radius = radius;
        this.detectTimerTicks = Math.max(1, detectTimerTicks);
        this.messages = Collections.unmodifiableMap(messages);
        this.ignoredItems = Collections.unmodifiableSet(ignoredItems);
    }

    // ── 便捷访问器 ────────────────────────────────────────────────────────────

    /**
     * 获取指定品质的消失时间（秒）。
     * 若未配置则回退到 COMMON 时间；若 COMMON 也没有则返回 150。
     */
    public int despawnFor(ItemQuality quality) {
        Integer seconds = despawnMap.get(quality);
        if (seconds == null) seconds = despawnMap.get(ItemQuality.COMMON);
        if (seconds == null) seconds = 150;
        return seconds;
    }

    /**
     * 获取指定品质的 TextColor。
     * 若未配置则回退到 COMMON 颜色；若 COMMON 也没有则返回白色。
     */
    public TextColor colorFor(ItemQuality quality) {
        TextColor color = qualityColors.get(quality);
        if (color == null) color = qualityColors.get(ItemQuality.COMMON);
        if (color == null) color = TextColor.fromHexString("#ffffff");
        return color;
    }

    /**
     * 获取指定品质颜色的 {@code &#RRGGBB} 字符串表示，用于 {@code {rarityColor}} 占位符展开。
     */
    public String colorHexFor(ItemQuality quality) {
        TextColor color = colorFor(quality);
        // TextColor#asHexString() 返回 "#RRGGBB"，需要转换为 "&#RRGGBB"
        return "&" + color.asHexString();
    }

    /**
     * 获取消息模板，若不存在则返回 key 本身（防空指针）。
     */
    public String msg(String key) {
        return messages.getOrDefault(key, key);
    }

    /**
     * 获取消息前缀原始字符串（含颜色码）。
     */
    public String prefix() {
        return messages.getOrDefault("prefix", "");
    }
}
