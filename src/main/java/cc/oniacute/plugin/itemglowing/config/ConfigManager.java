package cc.oniacute.plugin.itemglowing.config;

import cc.oniacute.plugin.itemglowing.quality.ItemQuality;
import cc.oniacute.plugin.itemglowing.quality.RarityManager;
import cc.oniacute.plugin.itemglowing.util.ColorUtil;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

/**
 * 配置管理器：负责加载、解析、保存和热重载配置。
 * <p>
 * 使用 {@link AtomicReference} 持有配置快照，reload 时原子替换，无锁读取。
 * 同时持有 {@link RarityManager} 负责物品稀有度的读写。
 * </p>
 */
public final class ConfigManager {

    private RarityManager rarityManager;

    /** 品质 → 配置 key → 默认颜色 的映射表 */
    private static final Object[][] QUALITY_DEFS = {
            // { ItemQuality,     configKey,      defaultHex    }
            { ItemQuality.COMMON,   "commonColor",   "#ababab" },
            { ItemQuality.UNCOMMON, "uncommonColor", "#ffffff" },
            { ItemQuality.RARE,     "rareColor",     "#2dabff" },
            { ItemQuality.EPIC,     "epicColor",     "#dc67ff" },
            { ItemQuality.LEGEND,   "legendColor",   "#ffbe3c" },
    };

    /** 品质 → despawn 配置 key → 默认秒数 的映射表 */
    private static final Object[][] DESPAWN_DEFS = {
            // { ItemQuality,     configKey,           defaultSeconds }
            { ItemQuality.COMMON,   "despawnCommon",   150 },
            { ItemQuality.UNCOMMON, "despawnUncommon", 260 },
            { ItemQuality.RARE,     "despawnRare",     300 },
            { ItemQuality.EPIC,     "despawnEpic",     400 },
            { ItemQuality.LEGEND,   "despawnLegend",   500 },
    };

    private final JavaPlugin plugin;
    private final AtomicReference<PluginConfig> configRef = new AtomicReference<>();

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  生命周期
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 初始化：保存默认配置并加载。
     */
    public void init() {
        plugin.saveDefaultConfig();
        reload();
    }

    /**
     * 注入 {@link RarityManager}（在主类 onEnable 中创建后调用）。
     */
    public void setRarityManager(RarityManager rarityManager) {
        this.rarityManager = rarityManager;
    }

    /**
     * 获取当前 {@link RarityManager}。
     */
    public RarityManager getRarityManager() {
        return rarityManager;
    }

    /**
     * 重新从磁盘加载配置并原子替换快照。
     * 同时重载 {@link RarityManager}（若已注入）。
     * 若解析期间抛出异常，会记录日志并保留旧快照（若存在）。
     */
    public void reload() {
        plugin.reloadConfig();
        try {
            PluginConfig cfg = parse(plugin.getConfig());
            configRef.set(cfg);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[ConfigManager] 配置解析失败: " + e.getMessage(), e);
            throw e;
        }
        // 同步重载品质覆盖表（从 config.yml 的 rarity 节点读取）
        if (rarityManager != null) {
            rarityManager.load(plugin.getConfig());
        }
    }

    /**
     * 获取当前生效的配置快照（线程安全，O(1)）。
     */
    public PluginConfig current() {
        return configRef.get();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  解析
    // ─────────────────────────────────────────────────────────────────────────

    private PluginConfig parse(FileConfiguration fc) {
        // ── 发光开关 ──────────────────────────────────────────────────────────
        boolean glowing = fc.getBoolean("glowing", true);

        // ── 品质颜色（扁平 key） ───────────────────────────────────────────────
        Map<ItemQuality, TextColor> qualityColors = new EnumMap<>(ItemQuality.class);
        for (Object[] def : QUALITY_DEFS) {
            ItemQuality quality   = (ItemQuality) def[0];
            String      configKey = (String)      def[1];
            String      defHex    = (String)      def[2];

            String raw = fc.getString(configKey, defHex);
            TextColor color = parseColor(raw.trim(), defHex);
            qualityColors.put(quality, color);
        }

        // ── 名牌模板 ──────────────────────────────────────────────────────────
        String nametagTemplate = fc.getString("nametag",
                "{rarityColor}{item} &fx &b{amount} &7| &#6cd3ff{time}s");

        // ── 按品质分离的 despawn 时间 ─────────────────────────────────────────
        Map<ItemQuality, Integer> despawnMap = new EnumMap<>(ItemQuality.class);
        for (Object[] def : DESPAWN_DEFS) {
            ItemQuality quality   = (ItemQuality) def[0];
            String      configKey = (String)      def[1];
            int         defSecs   = (int)         def[2];

            int seconds = fc.getInt(configKey, defSecs);
            if (seconds < 1) {
                plugin.getLogger().warning("[ConfigManager] " + configKey + " 值无效（< 1），已使用默认值 " + defSecs);
                seconds = defSecs;
            }
            despawnMap.put(quality, seconds);
        }

        // ── radius ────────────────────────────────────────────────────────────
        int radius = fc.getInt("radius", -1);

        // ── detectTimer ───────────────────────────────────────────────────────
        int detectTimerTicks = fc.getInt("detectTimer", 2);
        if (detectTimerTicks < 1) {
            plugin.getLogger().warning("[ConfigManager] detectTimer 值无效（< 1），已重置为 1");
            detectTimerTicks = 1;
        }

        // ── messages ──────────────────────────────────────────────────────────
        Map<String, String> messages = new LinkedHashMap<>();
        if (fc.isConfigurationSection("messages")) {
            for (String key : Objects.requireNonNull(
                    fc.getConfigurationSection("messages")).getKeys(false)) {
                messages.put(key, fc.getString("messages." + key, ""));
            }
        }
        // 注入默认消息项（防止配置文件缺项时 NullPointerException）
        messages.putIfAbsent("prefix",        "&#6cd3ffItemGlowing &8»");
        messages.putIfAbsent("noPromission",  "{prefix} &c你无权执行该操作!");
        messages.putIfAbsent("invalid",       "{prefix} &c错误的用法! 用例:add/remove/reload/setrarity [item]");
        messages.putIfAbsent("added",         "{prefix} &a物品 &e{item} &a已经被添加到忽略名单.");
        messages.putIfAbsent("removed",       "{prefix} &c物品 &e{item} &c已经被移出忽略名单.");
        messages.putIfAbsent("timeset",       "{prefix} &a品质为 &e{rarity} &a的物品消失时间已设置为 &e{time} &a秒.");
        messages.putIfAbsent("rarityset",     "{prefix} &a物品 &e{item} &a的品质已设置为 &e{rarity} &a.");
        messages.putIfAbsent("reloaded",      "{prefix} &a插件已重载,有 &e{items} &a个项目被忽略.");
        messages.putIfAbsent("error",         "{prefix} &c重载失败, 请检查配置文件是否正确!");

        // ── ignoredItems（从 ignored 节点读取）────────────────────────────────
        Set<Material> ignoredItems = new LinkedHashSet<>();
        List<?> rawList = fc.getList("ignored", Collections.emptyList());
        for (Object obj : rawList) {
            if (obj instanceof String s) {
                Material mat = Material.matchMaterial(s.trim());
                if (mat != null) {
                    ignoredItems.add(mat);
                } else {
                    plugin.getLogger().warning("[ConfigManager] 未知物品名称（已忽略）: " + s);
                }
            }
        }

        return new PluginConfig(
                glowing, qualityColors,
                nametagTemplate, despawnMap, radius, detectTimerTicks,
                messages, ignoredItems
        );
    }

    /**
     * 解析颜色字符串，兼容 {@code "#RRGGBB"} 和 {@code "&#RRGGBB"} 两种写法。
     * 若解析失败则使用 fallbackHex。
     */
    private TextColor parseColor(String raw, String fallbackHex) {
        // 优先尝试 ColorUtil（处理 &# 前缀）
        TextColor color = ColorUtil.parseTextColor(raw);
        if (color == null) {
            // 尝试直接 #RRGGBB
            try {
                if (raw.startsWith("#")) color = TextColor.fromHexString(raw);
            } catch (Exception ignored) {}
        }
        if (color == null) {
            plugin.getLogger().warning("[ConfigManager] 颜色值无效: \"" + raw + "\"，使用默认值 " + fallbackHex);
            color = TextColor.fromHexString(fallbackHex);
        }
        return color;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  持久化
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 向忽略名单中添加 Material，写盘并热重载。
     *
     * @return true=新增成功；false=已存在
     */
    public boolean addIgnored(Material material) {
        FileConfiguration fc = plugin.getConfig();
        List<String> list = getIgnoredList(fc);
        String name = material.name();
        if (list.contains(name)) return false;
        list.add(name);
        fc.set("ignored", list);
        saveAsync();
        reload();
        return true;
    }

    /**
     * 从忽略名单中移除 Material，写盘并热重载。
     *
     * @return true=移除成功；false=不在名单中
     */
    public boolean removeIgnored(Material material) {
        FileConfiguration fc = plugin.getConfig();
        List<String> list = getIgnoredList(fc);
        boolean removed = list.remove(material.name());
        if (removed) {
            fc.set("ignored", list);
            saveAsync();
            reload();
        }
        return removed;
    }

    /**
     * 更新指定品质的 despawn 时间，写盘并热重载。
     *
     * @param quality 品质等级
     * @param seconds 消失时间（秒，>= 1）
     */
    public void setDespawnSeconds(ItemQuality quality, int seconds) {
        String key = getDespawnKey(quality);
        if (key == null) return;
        FileConfiguration fc = plugin.getConfig();
        fc.set(key, seconds);
        saveAsync();
        reload();
    }

    /**
     * 将当前 FileConfiguration 异步写入磁盘。
     * 写入操作在 Bukkit 异步线程中执行，不阻塞主线程。
     */
    public void saveAsync() {
        FileConfiguration snapshot = plugin.getConfig();
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                snapshot.save(configFile);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "[ConfigManager] 配置保存失败: " + e.getMessage(), e);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  辅助
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 设置物品的自定义稀有度并持久化到 {@code config.yml} 的 rarity 节点。
     * 传入 {@code null} 表示移除自定义覆盖（回退内建预设）。
     *
     * @param material 物品类型
     * @param quality  品质等级（null = 移除）
     */
    public void setMaterialRarity(Material material, ItemQuality quality) {
        if (rarityManager == null) return;
        rarityManager.setQuality(material, quality);
        // 将变更写回 config.yml 的 rarity 节点
        saveRarityToConfig();
    }

    /**
     * 将 RarityManager 中的自定义品质覆盖表写入 config.yml 的 rarity 节点，然后异步保存。
     */
    private void saveRarityToConfig() {
        if (rarityManager == null) return;
        FileConfiguration fc = plugin.getConfig();
        // 清空旧节点再重写
        fc.set("rarity", null);
        Map<Material, ItemQuality> overrides = rarityManager.getCustomOverrides();
        if (!overrides.isEmpty()) {
            for (Map.Entry<Material, ItemQuality> entry : overrides.entrySet()) {
                fc.set("rarity." + entry.getKey().name(), entry.getValue().name().toLowerCase());
            }
        }
        saveAsync();
    }

    private List<String> getIgnoredList(FileConfiguration fc) {
        List<?> raw = fc.getList("ignored", Collections.emptyList());
        List<String> result = new ArrayList<>();
        for (Object obj : raw) {
            if (obj instanceof String s) result.add(s);
        }
        return result;
    }

    /**
     * 根据品质等级返回对应的 config.yml key。
     */
    private static String getDespawnKey(ItemQuality quality) {
        return switch (quality) {
            case COMMON   -> "despawnCommon";
            case UNCOMMON -> "despawnUncommon";
            case RARE     -> "despawnRare";
            case EPIC     -> "despawnEpic";
            case LEGEND   -> "despawnLegend";
        };
    }
}
