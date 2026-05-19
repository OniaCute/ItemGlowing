package cc.oniacute.plugin.itemglowing.service;

import cc.oniacute.plugin.itemglowing.config.ConfigManager;
import cc.oniacute.plugin.itemglowing.config.PluginConfig;
import cc.oniacute.plugin.itemglowing.model.TrackedItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.Iterator;

/**
 * 主循环处理器（Bukkit 主线程调度）。
 * <p>
 * 每 {@code detectTimerTicks} tick 执行一次，遍历所有追踪条目：
 * <ol>
 *   <li>清理失效/已死亡实体</li>
 *   <li>倒计时到期自动消失</li>
 *   <li>radius 视距过滤</li>
 *   <li>设置/更新发光颜色（Scoreboard Team）</li>
 *   <li>刷新名牌（仅在剩余秒数变化时才更新，减少包发送次数）</li>
 * </ol>
 * </p>
 */
public final class DropProcessor {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final DropItemTracker tracker;
    private final GlowService glowService;

    private BukkitTask task;

    public DropProcessor(JavaPlugin plugin, ConfigManager configManager,
                         DropItemTracker tracker, GlowService glowService) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.tracker = tracker;
        this.glowService = glowService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  生命周期
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 启动主循环（幂等：若已在运行则先停止再重启）。
     */
    public void start() {
        stop();
        int ticks = configManager.current().detectTimerTicks;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, ticks, ticks);
    }

    /**
     * 停止主循环。
     */
    public void stop() {
        if (task != null && !task.isCancelled()) {
            task.cancel();
            task = null;
        }
    }

    public boolean isRunning() {
        return task != null && !task.isCancelled();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  主循环（Bukkit 主线程）
    // ─────────────────────────────────────────────────────────────────────────

    private void tick() {
        PluginConfig cfg = configManager.current();
        long now = System.currentTimeMillis();

        Iterator<TrackedItem> it = tracker.all().iterator();
        while (it.hasNext()) {
            processItem(it.next(), cfg, now, it);
        }
    }

    private void processItem(TrackedItem tracked, PluginConfig cfg,
                              long now, Iterator<TrackedItem> it) {
        // ── 1. 实体有效性检查 ────────────────────────────────────────────────
        Entity entity = Bukkit.getEntity(tracked.entityId);
        if (entity == null || entity.isDead() || !(entity instanceof Item item)) {
            it.remove();
            return;
        }

        // ── 2. 倒计时到期 ────────────────────────────────────────────────────
        long remainMillis = tracked.despawnAtMillis - now;
        if (remainMillis <= 0) {
            glowService.removeGlow(item, tracked.lastGlowColor);
            item.setCustomNameVisible(false);
            item.remove();
            it.remove();
            return;
        }
        int remainSeconds = (int) (remainMillis / 1000L);

        ItemStack stack = item.getItemStack();

        // ── 3. radius 视距过滤 ───────────────────────────────────────────────
        if (cfg.radius >= 0) {
            boolean hasNearby = hasPlayerNearby(item.getLocation(), cfg.radius);
            if (!hasNearby) {
                if (tracked.lastVisible) {
                    glowService.removeGlow(item, tracked.lastGlowColor);
                    item.setCustomNameVisible(false);
                    tracked.lastVisible = false;
                }
                return;
            }
            if (!tracked.lastVisible) {
                // 重新进入范围，强制重刷所有状态
                tracked.lastVisible = true;
                tracked.lastShownSecond = -1;
                tracked.lastGlowColor = null;
            }
        }

        // ── 4. 发光 ──────────────────────────────────────────────────────────
        if (cfg.glowing) {
            TextColor qualityColor = cfg.colorFor(tracked.quality);
            NamedTextColor newGlowColor = glowService.applyGlow(item, qualityColor, tracked.lastGlowColor);
            tracked.lastGlowColor = newGlowColor;
        } else {
            if (tracked.lastGlowColor != null || item.isGlowing()) {
                glowService.removeGlow(item, tracked.lastGlowColor);
                tracked.lastGlowColor = null;
            }
        }

        // ── 5. 名牌 ──────────────────────────────────────────────────────────
        if (!cfg.nametagTemplate.isEmpty()) {
            if (remainSeconds != tracked.lastShownSecond) {
                Component nametag = NameTagService.build(cfg, stack, tracked.quality, remainSeconds);
                item.customName(nametag);
                item.setCustomNameVisible(true);
                tracked.lastShownSecond = remainSeconds;
            }
        } else {
            if (item.isCustomNameVisible()) {
                item.setCustomNameVisible(false);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  工具方法
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 判断指定位置半径内是否有在线玩家（球形范围，使用 distanceSquared 避免开方）。
     */
    private static boolean hasPlayerNearby(Location location, int radius) {
        double radiusSq = (double) radius * radius;
        // getNearbyEntities 用 AABB 粗筛，再用球形距离精确过滤
        Collection<Entity> nearby = location.getWorld().getNearbyEntities(
                location, radius, radius, radius,
                e -> e instanceof Player);
        for (Entity e : nearby) {
            if (e.getLocation().distanceSquared(location) <= radiusSq) {
                return true;
            }
        }
        return false;
    }
}
