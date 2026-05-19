package cc.oniacute.plugin.itemglowing.listener;

import cc.oniacute.plugin.itemglowing.service.DropItemTracker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemMergeEvent;

/**
 * 监听物品堆叠合并事件，将被合并的实体从追踪器中移除。
 * <p>
 * 合并后，被合并方（source）实体将从世界移除；目标方（target）继续存在。
 * NameTag 的 {amount} 将在下次主循环刷新时自动取最新堆叠数量，无需额外处理。
 * </p>
 */
public final class ItemMergeListener implements Listener {

    private final DropItemTracker tracker;

    public ItemMergeListener(DropItemTracker tracker) {
        this.tracker = tracker;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemMerge(ItemMergeEvent event) {
        // 移除被合并方的追踪条目
        tracker.unregister(event.getEntity().getUniqueId());

        // 目标方的 lastShownSecond 重置为 -1，强制下次刷新名牌（数量变了）
        var tracked = tracker.get(event.getTarget().getUniqueId());
        if (tracked != null) {
            tracked.lastShownSecond = -1;
        }
    }
}
