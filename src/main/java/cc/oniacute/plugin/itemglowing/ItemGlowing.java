package cc.oniacute.plugin.itemglowing;

import cc.oniacute.plugin.itemglowing.command.ItemGlowingCommand;
import cc.oniacute.plugin.itemglowing.config.ConfigManager;
import cc.oniacute.plugin.itemglowing.listener.ItemDespawnListener;
import cc.oniacute.plugin.itemglowing.listener.ItemMergeListener;
import cc.oniacute.plugin.itemglowing.listener.ItemSpawnListener;
import cc.oniacute.plugin.itemglowing.model.TrackedItem;
import cc.oniacute.plugin.itemglowing.quality.ItemQuality;
import cc.oniacute.plugin.itemglowing.quality.QualityResolver;
import cc.oniacute.plugin.itemglowing.quality.RarityManager;
import cc.oniacute.plugin.itemglowing.service.DropItemTracker;
import cc.oniacute.plugin.itemglowing.service.DespawnEffectService;
import cc.oniacute.plugin.itemglowing.service.DropProcessor;
import cc.oniacute.plugin.itemglowing.service.GlowService;
import cc.oniacute.plugin.itemglowing.util.HexToNamedColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * ItemGlowing 插件主类。
 * <p>
 * 负责各功能模块的初始化、注册与销毁。
 * </p>
 *
 * @author OniaCute (www.oniacute.cc)
 */
public final class ItemGlowing extends JavaPlugin {

    private static ItemGlowing instance;

    private ConfigManager       configManager;
    private RarityManager       rarityManager;
    private QualityResolver     qualityResolver;
    private DropItemTracker     tracker;
    private GlowService         glowService;
    private DespawnEffectService effectService;
    private DropProcessor        dropProcessor;

    @Override
    public void onEnable() {
        instance = this;

        // ── 1. 准备 ConfigManager 和 RarityManager ──────────────────────────
        configManager = new ConfigManager(this);
        rarityManager = new RarityManager(this);
        // 注入 RarityManager，使 configManager.reload() 时同步加载 rarity 节点
        configManager.setRarityManager(rarityManager);

        // ── 2. 加载主配置（config.yml），同时加载品质覆盖（rarity 节点） ─────
        try {
            configManager.init();
        } catch (Exception e) {
            getLogger().severe("[ItemGlowing] 配置加载失败，插件禁用: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // ── 3. 初始化核心服务 ────────────────────────────────────────────────
        qualityResolver = new QualityResolver(this, rarityManager);
        tracker         = new DropItemTracker();
        glowService     = new GlowService();
        effectService   = new DespawnEffectService(this);
        dropProcessor   = new DropProcessor(this, configManager, tracker, glowService, effectService);

        // ── 4. 注册事件监听器 ────────────────────────────────────────────────
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new ItemSpawnListener(configManager, tracker, qualityResolver), this);
        pm.registerEvents(new ItemDespawnListener(tracker), this);
        pm.registerEvents(new ItemMergeListener(tracker), this);

        // ── 5. 注册命令 ──────────────────────────────────────────────────────
        ItemGlowingCommand commandHandler = new ItemGlowingCommand(configManager);
        PluginCommand cmd = getCommand("itemglowing");
        if (cmd != null) {
            cmd.setExecutor(commandHandler);
            cmd.setTabCompleter(commandHandler);
        } else {
            getLogger().severe("[ItemGlowing] 命令 'itemglowing' 未在 plugin.yml 中注册！");
        }

        // ── 6. 预填充存量掉落物 ──────────────────────────────────────────────
        preloadExistingItems();

        // ── 7. 启动主循环 ────────────────────────────────────────────────────
        dropProcessor.start();

        getLogger().info("[ItemGlowing] v" + getPluginMeta().getVersion() + " 已启动，"
                + "追踪 " + tracker.size() + " 个存量掉落物。");
    }

    @Override
    public void onDisable() {
        // ── 1. 停止主循环 ────────────────────────────────────────────────────
        if (dropProcessor != null) {
            dropProcessor.stop();
        }

        // ── 1.5 取消所有正在播放的黑烟特效 ──────────────────────────────────
        if (effectService != null) {
            effectService.cancelAll();
        }

        // ── 2. 清理追踪中实体的视觉效果 ─────────────────────────────────────
        if (tracker != null && glowService != null) {
            for (TrackedItem tracked : tracker.all()) {
                Entity entity = Bukkit.getEntity(tracked.entityId);
                if (entity instanceof Item item && !item.isDead()) {
                    glowService.removeGlow(item, tracked.lastGlowColor);
                    item.setCustomNameVisible(false);
                }
            }
            tracker.clear();
        }

        // ── 3. 清理 Scoreboard Teams ─────────────────────────────────────────
        if (glowService != null) {
            glowService.cleanupTeams();
        }

        // ── 4. 卸载品质管理器 ────────────────────────────────────────────────
        if (rarityManager != null) {
            rarityManager.unload();
        }

        // ── 5. 清理缓存 ──────────────────────────────────────────────────────
        HexToNamedColor.clearCache();

        // ── 6. 取消所有调度任务 ──────────────────────────────────────────────
        getServer().getScheduler().cancelTasks(this);

        getLogger().info("[ItemGlowing] 已停止。");
        instance = null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  私有辅助
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 遍历所有世界中已存在的 Item 实体，将符合条件的纳入追踪。
     * despawnAt 根据物品品质从配置中获取对应的消失时间。
     */
    private void preloadExistingItems() {
        long now = System.currentTimeMillis();
        var cfg  = configManager.current();

        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof Item item)) continue;
                if (item.isDead()) continue;

                var stack = item.getItemStack();
                if (cfg.ignoredItems.contains(stack.getType())) continue;
                if (tracker.isTracked(item.getUniqueId())) continue;

                ItemQuality quality = qualityResolver.resolve(stack);
                long despawnMs = (long) cfg.despawnFor(quality) * 1000L;
                tracker.register(new TrackedItem(item.getUniqueId(), now + despawnMs, quality));
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  公开访问器
    // ─────────────────────────────────────────────────────────────────────────

    public static ItemGlowing getInstance()     { return instance; }
    public ConfigManager  getConfigManager()    { return configManager; }
    public RarityManager  getRarityManager()    { return rarityManager; }
    public DropItemTracker getTracker()         { return tracker; }
}
