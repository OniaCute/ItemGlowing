package cc.oniacute.plugin.itemglowing.quality;

/**
 * 插件自定义的 5 级物品品质枚举。
 * <p>
 * COMMON → UNCOMMON → RARE → EPIC → LEGEND
 * </p>
 * <p>
 * 判定顺序（由 {@link QualityResolver} 实现）：
 * <ol>
 *   <li>检查 PersistentDataContainer 是否标记了 {@code itemglowing:legend}，是则为 LEGEND。</li>
 *   <li>否则映射原版 {@link org.bukkit.inventory.ItemRarity}：
 *       COMMON→COMMON, UNCOMMON→UNCOMMON, RARE→RARE, EPIC→EPIC。</li>
 * </ol>
 * </p>
 */
public enum ItemQuality {
    COMMON,
    UNCOMMON,
    RARE,
    EPIC,
    LEGEND
}
