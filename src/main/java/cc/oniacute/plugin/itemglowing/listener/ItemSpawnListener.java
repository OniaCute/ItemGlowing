package cc.oniacute.plugin.itemglowing.listener;

import cc.oniacute.plugin.itemglowing.config.ConfigManager;
import cc.oniacute.plugin.itemglowing.config.PluginConfig;
import cc.oniacute.plugin.itemglowing.model.TrackedItem;
import cc.oniacute.plugin.itemglowing.quality.ItemQuality;
import cc.oniacute.plugin.itemglowing.quality.QualityResolver;
import cc.oniacute.plugin.itemglowing.service.DropItemTracker;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 监听掉落物生成事件，将符合条件的掉落物注册到 {@link DropItemTracker}。
 */
public final class ItemSpawnListener implements Listener {

    private final ConfigManager configManager;
    private final DropItemTracker tracker;
    private final QualityResolver qualityResolver;

    public ItemSpawnListener(ConfigManager configManager,
                             DropItemTracker tracker,
                             QualityResolver qualityResolver) {
        this.configManager = configManager;
        this.tracker = tracker;
        this.qualityResolver = qualityResolver;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        Item entity = event.getEntity();
        ItemStack stack = entity.getItemStack();
        PluginConfig cfg = configManager.current();

        // 忽略名单检查
        if (cfg.ignoredItems.contains(stack.getType())) {
            return;
        }

        ItemQuality quality = qualityResolver.resolve(stack);
        long despawnAt = System.currentTimeMillis() + (long) cfg.despawnFor(quality) * 1000L;

        tracker.register(new TrackedItem(entity.getUniqueId(), despawnAt, quality));
    }
}
