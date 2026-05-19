package cc.oniacute.plugin.itemglowing.service;

import cc.oniacute.plugin.itemglowing.model.TrackedItem;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 核心追踪器：管理所有需要被监控的掉落物条目。
 * <p>
 * 使用 {@link ConcurrentHashMap} 存储，支持在主循环中迭代的同时，
 * 在事件回调中安全地增删条目（弱一致性迭代器，不抛 ConcurrentModificationException）。
 * </p>
 * <p>
 * 所有实体操作（glow/customName/remove）仍需在 Bukkit 主线程执行，
 * 追踪集合本身的读写线程安全。
 * </p>
 */
public final class DropItemTracker {

    private final ConcurrentHashMap<UUID, TrackedItem> items = new ConcurrentHashMap<>(64);

    /**
     * 注册一个新的掉落物追踪条目。
     * 若该 UUID 已存在，将覆盖（合并事件后目标 item 保留新 despawnAt）。
     */
    public void register(TrackedItem item) {
        items.put(item.entityId, item);
    }

    /**
     * 移除追踪条目。
     *
     * @param uuid 实体 UUID
     */
    public void unregister(UUID uuid) {
        items.remove(uuid);
    }

    /**
     * 检查是否追踪了指定 UUID。
     */
    public boolean isTracked(UUID uuid) {
        return items.containsKey(uuid);
    }

    /**
     * 获取追踪条目（可为 null）。
     */
    public TrackedItem get(UUID uuid) {
        return items.get(uuid);
    }

    /**
     * 获取所有追踪条目的视图（可安全迭代）。
     */
    public Collection<TrackedItem> all() {
        return items.values();
    }

    /**
     * 清空所有追踪条目（在 onDisable 时调用）。
     */
    public void clear() {
        items.clear();
    }

    /**
     * 当前追踪数量。
     */
    public int size() {
        return items.size();
    }
}
