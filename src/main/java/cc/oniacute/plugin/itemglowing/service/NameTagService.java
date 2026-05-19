package cc.oniacute.plugin.itemglowing.service;

import cc.oniacute.plugin.itemglowing.config.PluginConfig;
import cc.oniacute.plugin.itemglowing.quality.ItemQuality;
import cc.oniacute.plugin.itemglowing.util.PlaceholderUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;

/**
 * 名牌构建服务。
 * <p>
 * 根据名牌模板、物品品质、物品堆叠数量和剩余秒数构建 Adventure Component，
 * 用于 {@code Item#customName(Component)} 调用。
 * </p>
 * <p>
 * 所有方法为无状态纯函数，线程安全。
 * </p>
 */
public final class NameTagService {

    private NameTagService() {}

    /**
     * 构建名牌 Component。
     *
     * @param cfg           当前配置快照
     * @param stack         物品堆（用于获取 Material.translationKey() 和 amount）
     * @param quality       物品品质
     * @param remainSeconds 剩余秒数（>= 0）
     * @return 名牌 Component；若模板为空则返回 {@link Component#empty()}
     */
    public static Component build(PluginConfig cfg, ItemStack stack,
                                  ItemQuality quality, int remainSeconds) {
        String template = cfg.nametagTemplate;
        if (template == null || template.isEmpty()) return Component.empty();

        // {item} → TranslatableComponent（客户端按其语言显示，中文客户端显示中文）
        Component itemComponent = Component.translatable(stack.getType().translationKey());

        // {rarityColor} → &#RRGGBB 字符串，用于 Legacy 颜色解析器渲染
        String rarityColorHex = cfg.colorHexFor(quality);

        int amount = stack.getAmount();

        return PlaceholderUtil.buildNametag(template, itemComponent, rarityColorHex, amount, remainSeconds);
    }
}
