package cc.oniacute.plugin.itemglowing.model;

import cc.oniacute.plugin.itemglowing.quality.ItemQuality;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.UUID;

/**
 * 追踪中的掉落物条目。
 * <p>
 * 只持有 UUID，不持有 Item 实体强引用，规避区块卸载导致的内存泄漏。
 * 所有可变字段均为 volatile 或仅在主线程访问，无需额外同步。
 * </p>
 */
public final class TrackedItem {

    /** 实体 UUID */
    public final UUID entityId;

    /** 期望消失的时间戳（毫秒，System.currentTimeMillis()） */
    public final long despawnAtMillis;

    /**
     * 品质缓存（物品品质在生命周期内不变，spawn 时计算一次）。
     * 主线程写入，主线程读取，无并发问题。
     */
    public final ItemQuality quality;

    /** 上次名牌展示的剩余秒数（-1 表示尚未设置）。 */
    public int lastShownSecond = -1;

    /** 上次是否可见（glow + nametag）。 */
    public boolean lastVisible = true;

    /** 上次设置的 glow 颜色，用于避免重复设置。 */
    public NamedTextColor lastGlowColor = null;

    public TrackedItem(UUID entityId, long despawnAtMillis, ItemQuality quality) {
        this.entityId = entityId;
        this.despawnAtMillis = despawnAtMillis;
        this.quality = quality;
    }
}
