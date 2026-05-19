package cc.oniacute.plugin.itemglowing.listener;

import cc.oniacute.plugin.itemglowing.service.DropItemTracker;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;

/**
 * 监听掉落物消失和实体从世界移除事件，及时清理 {@link DropItemTracker} 中的条目，
 * 防止追踪僵尸实体（内存泄漏）。
 */
public final class ItemDespawnListener implements Listener {

    private final DropItemTracker tracker;

    public ItemDespawnListener(DropItemTracker tracker) {
        this.tracker = tracker;
    }

    /**
     * 原版/插件触发的 despawn 事件（如 5 分钟到期、/kill 等）。
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onItemDespawn(ItemDespawnEvent event) {
        tracker.unregister(event.getEntity().getUniqueId());
    }

    /**
     * 区块卸载时实体被移除，清理对应追踪条目。
     * 使用 EntitiesUnloadEvent（Paper 1.20.6+）批量处理，比逐个实体事件开销小。
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitiesUnload(EntitiesUnloadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (entity instanceof Item) {
                tracker.unregister(entity.getUniqueId());
            }
        }
    }
}
