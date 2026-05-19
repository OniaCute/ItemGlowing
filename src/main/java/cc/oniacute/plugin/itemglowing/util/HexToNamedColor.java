package cc.oniacute.plugin.itemglowing.util;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 将任意 TextColor 就近映射到 NamedTextColor（16 色）。
 * <p>
 * 使用 RGB 欧氏距离（平方）匹配，结果缓存避免重复计算。
 * </p>
 */
public final class HexToNamedColor {

    /** 缓存：argb int -> NamedTextColor */
    private static final Map<Integer, NamedTextColor> CACHE = new ConcurrentHashMap<>(64);

    /** 所有 NamedTextColor 的列表（在类加载时初始化） */
    private static final NamedTextColor[] ALL_NAMED;

    static {
        // NamedTextColor.NAMES 包含所有 16 种命名颜色
        ALL_NAMED = NamedTextColor.NAMES.values().toArray(new NamedTextColor[0]);
    }

    private HexToNamedColor() {}

    /**
     * 找到与给定 TextColor 在 RGB 空间最接近的 NamedTextColor。
     *
     * @param color 任意 TextColor；若为 null 则返回 WHITE
     * @return 最接近的 NamedTextColor
     */
    public static NamedTextColor nearest(TextColor color) {
        if (color == null) return NamedTextColor.WHITE;
        if (color instanceof NamedTextColor named) return named;

        int rgb = color.value(); // 0xRRGGBB
        return CACHE.computeIfAbsent(rgb, HexToNamedColor::compute);
    }

    private static NamedTextColor compute(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        NamedTextColor best = NamedTextColor.WHITE;
        long bestDist = Long.MAX_VALUE;

        for (NamedTextColor nc : ALL_NAMED) {
            int nRgb = nc.value();
            int dr = r - ((nRgb >> 16) & 0xFF);
            int dg = g - ((nRgb >> 8) & 0xFF);
            int db = b - (nRgb & 0xFF);
            long dist = (long) dr * dr + (long) dg * dg + (long) db * db;
            if (dist < bestDist) {
                bestDist = dist;
                best = nc;
                if (dist == 0) break; // 精确匹配，提前结束
            }
        }
        return best;
    }

    /**
     * 清空缓存（仅供测试/热重载使用）。
     */
    public static void clearCache() {
        CACHE.clear();
    }
}
