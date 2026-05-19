package cc.oniacute.plugin.itemglowing.quality;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 将 {@link ItemStack} 解析为 {@link ItemQuality}（5 级品质）。
 *
 * <h3>判定优先级（由高到低）</h3>
 * <ol>
 *   <li><b>用户自定义</b>：{@link RarityManager} 中通过命令设置的覆盖表（rarity.yml）。</li>
 *   <li><b>PDC 标记</b>：{@link ItemMeta} 的 PersistentDataContainer 包含 {@code itemglowing:legend = true} → LEGEND。</li>
 *   <li><b>内建预设表</b>：{@link RarityManager#BUILTIN_PRESETS}，涵盖约 200 种物品。</li>
 *   <li><b>兜底</b>：{@link ItemQuality#COMMON}。</li>
 * </ol>
 *
 * <p>
 * 注意：PDC 判定在用户自定义之后，是为了允许用户显式覆盖 LEGEND 标记的物品（例如把某个
 * 带 PDC 的物品降级为 RARE）。如果不希望被覆盖，可调整顺序。
 * </p>
 */
public final class QualityResolver {

    /** PersistentDataContainer 中标记 LEGEND 的 key（供其他插件联动使用） */
    private final NamespacedKey legendKey;
    private final RarityManager rarityManager;

    public QualityResolver(JavaPlugin plugin, RarityManager rarityManager) {
        this.legendKey     = new NamespacedKey(plugin, "legend");
        this.rarityManager = rarityManager;
    }

    /**
     * 解析物品品质。
     *
     * @param stack 物品堆（不可为 null）
     * @return 对应的 ItemQuality
     */
    public ItemQuality resolve(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return ItemQuality.COMMON;

        // 1. 用户自定义覆盖（rarity.yml）
        ItemQuality custom = rarityManager.getCustomOverrides().get(stack.getType());
        if (custom != null) return custom;

        // 2. PDC LEGEND 标记
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            if (pdc.has(legendKey, PersistentDataType.BOOLEAN)
                    && Boolean.TRUE.equals(pdc.get(legendKey, PersistentDataType.BOOLEAN))) {
                return ItemQuality.LEGEND;
            }
        }

        // 3. 内建预设表（包含 ~200 种物品）
        return rarityManager.getQuality(stack.getType());
        // getQuality 内部已包含 内建预设 → COMMON 的兜底逻辑
    }
}
