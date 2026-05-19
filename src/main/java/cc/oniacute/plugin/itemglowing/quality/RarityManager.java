package cc.oniacute.plugin.itemglowing.quality;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 物品稀有度管理器。
 *
 * <h3>优先级（由高到低）</h3>
 * <ol>
 *   <li><b>用户自定义</b>：通过 {@code /itemglowing setrarity} 写入 {@code rarity.yml}。</li>
 *   <li><b>内建预设</b>：{@link #BUILTIN_PRESETS}，涵盖所有主要稀有物品。</li>
 *   <li><b>兜底</b>：{@link ItemQuality#COMMON}。</li>
 * </ol>
 *
 * <p>
 * 用户自定义表存储在 {@code config.yml} 的 {@code rarity} 节点中。
 * 通过 {@link #load(FileConfiguration)} 从主配置加载，通过 {@link ConfigManager} 写回保存。
 * 内存中的 {@link ConcurrentHashMap} 确保线程安全读取。
 * </p>
 */
public final class RarityManager {

    // ─────────────────────────────────────────────────────────────────────────
    //  内建预设表
    //  规则：
    //    COMMON   — 普通方块、农作物、基础材料
    //    UNCOMMON — 矿石、基础金属工具/装备、皮革装备、弓、部分食物
    //    RARE     — 钻石系列、下界物品、附魔书、地图、唱片、三叉戟、弩
    //    EPIC     — 下界合金系列、附魔金苹果、龙蛋、信标、鞘翅、幻术师之弓
    //    LEGEND   — 命令方块相关、屏障、结构方块（理论上不会自然掉落，但保留定义）
    // ─────────────────────────────────────────────────────────────────────────
    private static final Map<Material, ItemQuality> BUILTIN_PRESETS;

    static {
        Map<Material, ItemQuality> m = new EnumMap<>(Material.class);

        // ── UNCOMMON ─────────────────────────────────────────────────────────
        // 矿石
        m.put(Material.COAL_ORE,            ItemQuality.UNCOMMON);
        m.put(Material.DEEPSLATE_COAL_ORE,  ItemQuality.UNCOMMON);
        m.put(Material.IRON_ORE,            ItemQuality.UNCOMMON);
        m.put(Material.DEEPSLATE_IRON_ORE,  ItemQuality.UNCOMMON);
        m.put(Material.COPPER_ORE,          ItemQuality.UNCOMMON);
        m.put(Material.DEEPSLATE_COPPER_ORE,ItemQuality.UNCOMMON);
        m.put(Material.COAL,                ItemQuality.UNCOMMON);
        m.put(Material.RAW_IRON,            ItemQuality.UNCOMMON);
        m.put(Material.RAW_COPPER,          ItemQuality.UNCOMMON);
        m.put(Material.IRON_INGOT,          ItemQuality.UNCOMMON);
        m.put(Material.COPPER_INGOT,        ItemQuality.UNCOMMON);
        // 基础工具（铁）
        m.put(Material.IRON_SWORD,          ItemQuality.UNCOMMON);
        m.put(Material.IRON_AXE,            ItemQuality.UNCOMMON);
        m.put(Material.IRON_PICKAXE,        ItemQuality.UNCOMMON);
        m.put(Material.IRON_SHOVEL,         ItemQuality.UNCOMMON);
        m.put(Material.IRON_HOE,            ItemQuality.UNCOMMON);
        // 基础装备（铁）
        m.put(Material.IRON_HELMET,         ItemQuality.UNCOMMON);
        m.put(Material.IRON_CHESTPLATE,     ItemQuality.UNCOMMON);
        m.put(Material.IRON_LEGGINGS,       ItemQuality.UNCOMMON);
        m.put(Material.IRON_BOOTS,          ItemQuality.UNCOMMON);
        // 基础工具（金）
        m.put(Material.GOLD_INGOT,          ItemQuality.UNCOMMON);
        m.put(Material.RAW_GOLD,            ItemQuality.UNCOMMON);
        m.put(Material.GOLD_NUGGET,         ItemQuality.UNCOMMON);
        m.put(Material.GOLDEN_SWORD,        ItemQuality.UNCOMMON);
        m.put(Material.GOLDEN_AXE,          ItemQuality.UNCOMMON);
        m.put(Material.GOLDEN_PICKAXE,      ItemQuality.UNCOMMON);
        m.put(Material.GOLDEN_SHOVEL,       ItemQuality.UNCOMMON);
        m.put(Material.GOLDEN_HOE,          ItemQuality.UNCOMMON);
        m.put(Material.GOLDEN_HELMET,       ItemQuality.UNCOMMON);
        m.put(Material.GOLDEN_CHESTPLATE,   ItemQuality.UNCOMMON);
        m.put(Material.GOLDEN_LEGGINGS,     ItemQuality.UNCOMMON);
        m.put(Material.GOLDEN_BOOTS,        ItemQuality.UNCOMMON);
        m.put(Material.GOLDEN_APPLE,        ItemQuality.UNCOMMON);
        // 皮革装备
        m.put(Material.LEATHER_HELMET,      ItemQuality.UNCOMMON);
        m.put(Material.LEATHER_CHESTPLATE,  ItemQuality.UNCOMMON);
        m.put(Material.LEATHER_LEGGINGS,    ItemQuality.UNCOMMON);
        m.put(Material.LEATHER_BOOTS,       ItemQuality.UNCOMMON);
        // 弓箭
        m.put(Material.BOW,                 ItemQuality.UNCOMMON);
        m.put(Material.ARROW,               ItemQuality.UNCOMMON);
        m.put(Material.SPECTRAL_ARROW,      ItemQuality.UNCOMMON);
        // 红石系
        m.put(Material.REDSTONE,            ItemQuality.UNCOMMON);
        m.put(Material.LAPIS_LAZULI,        ItemQuality.UNCOMMON);
        // 食物
        m.put(Material.BREAD,               ItemQuality.UNCOMMON);
        m.put(Material.COOKED_BEEF,         ItemQuality.UNCOMMON);
        m.put(Material.COOKED_PORKCHOP,     ItemQuality.UNCOMMON);
        m.put(Material.COOKED_CHICKEN,      ItemQuality.UNCOMMON);
        m.put(Material.COOKED_MUTTON,       ItemQuality.UNCOMMON);
        m.put(Material.COOKED_RABBIT,       ItemQuality.UNCOMMON);
        m.put(Material.COOKED_SALMON,       ItemQuality.UNCOMMON);
        m.put(Material.COOKED_COD,          ItemQuality.UNCOMMON);
        // 其他
        m.put(Material.FLINT_AND_STEEL,     ItemQuality.UNCOMMON);
        m.put(Material.FISHING_ROD,         ItemQuality.UNCOMMON);
        m.put(Material.SHEARS,              ItemQuality.UNCOMMON);
        m.put(Material.BUCKET,              ItemQuality.UNCOMMON);
        m.put(Material.COMPASS,             ItemQuality.UNCOMMON);
        m.put(Material.CLOCK,               ItemQuality.UNCOMMON);
        m.put(Material.EXPERIENCE_BOTTLE,   ItemQuality.UNCOMMON);
        m.put(Material.SADDLE,              ItemQuality.UNCOMMON);
        m.put(Material.NAME_TAG,            ItemQuality.UNCOMMON);
        m.put(Material.LEAD,                ItemQuality.UNCOMMON);
        m.put(Material.ENDER_PEARL,         ItemQuality.UNCOMMON);
        m.put(Material.BLAZE_ROD,           ItemQuality.UNCOMMON);
        m.put(Material.BLAZE_POWDER,        ItemQuality.UNCOMMON);
        m.put(Material.GHAST_TEAR,          ItemQuality.UNCOMMON);
        m.put(Material.MAGMA_CREAM,         ItemQuality.UNCOMMON);
        m.put(Material.SLIME_BALL,          ItemQuality.UNCOMMON);
        m.put(Material.SPIDER_EYE,          ItemQuality.UNCOMMON);
        m.put(Material.FERMENTED_SPIDER_EYE,ItemQuality.UNCOMMON);
        m.put(Material.PHANTOM_MEMBRANE,    ItemQuality.UNCOMMON);
        m.put(Material.RABBIT_FOOT,         ItemQuality.UNCOMMON);
        m.put(Material.RABBIT_HIDE,         ItemQuality.UNCOMMON);
        m.put(Material.TURTLE_SCUTE,        ItemQuality.UNCOMMON);
        m.put(Material.ARMADILLO_SCUTE,     ItemQuality.UNCOMMON);
        m.put(Material.TURTLE_EGG,          ItemQuality.UNCOMMON);
        m.put(Material.HONEYCOMB,           ItemQuality.UNCOMMON);
        m.put(Material.HONEY_BOTTLE,        ItemQuality.UNCOMMON);
        m.put(Material.NETHER_STAR,         ItemQuality.UNCOMMON); // 重写为 EPIC 在下面
        m.put(Material.AMETHYST_SHARD,      ItemQuality.UNCOMMON);

        // ── RARE ─────────────────────────────────────────────────────────────
        // 钻石系列
        m.put(Material.DIAMOND_ORE,         ItemQuality.RARE);
        m.put(Material.DEEPSLATE_DIAMOND_ORE,ItemQuality.RARE);
        m.put(Material.DIAMOND,             ItemQuality.RARE);
        m.put(Material.DIAMOND_SWORD,       ItemQuality.RARE);
        m.put(Material.DIAMOND_AXE,         ItemQuality.RARE);
        m.put(Material.DIAMOND_PICKAXE,     ItemQuality.RARE);
        m.put(Material.DIAMOND_SHOVEL,      ItemQuality.RARE);
        m.put(Material.DIAMOND_HOE,         ItemQuality.RARE);
        m.put(Material.DIAMOND_HELMET,      ItemQuality.RARE);
        m.put(Material.DIAMOND_CHESTPLATE,  ItemQuality.RARE);
        m.put(Material.DIAMOND_LEGGINGS,    ItemQuality.RARE);
        m.put(Material.DIAMOND_BOOTS,       ItemQuality.RARE);
        // 下界合金原矿（半加工），制作完是 EPIC
        m.put(Material.ANCIENT_DEBRIS,      ItemQuality.RARE);
        m.put(Material.NETHERITE_SCRAP,     ItemQuality.RARE);
        // 附魔书
        m.put(Material.ENCHANTED_BOOK,      ItemQuality.RARE);
        // 三叉戟 / 弩
        m.put(Material.TRIDENT,             ItemQuality.RARE);
        m.put(Material.CROSSBOW,            ItemQuality.RARE);
        // 下界物品
        m.put(Material.NETHER_BRICK,        ItemQuality.RARE);
        m.put(Material.NETHER_QUARTZ_ORE,   ItemQuality.RARE);
        m.put(Material.QUARTZ,              ItemQuality.RARE);
        m.put(Material.MAGMA_BLOCK,         ItemQuality.RARE);
        m.put(Material.WITHER_SKELETON_SKULL,ItemQuality.RARE);
        m.put(Material.WITHER_ROSE,         ItemQuality.RARE);
        m.put(Material.CRYING_OBSIDIAN,     ItemQuality.RARE);
        m.put(Material.GILDED_BLACKSTONE,   ItemQuality.RARE);
        m.put(Material.SOUL_SAND,           ItemQuality.RARE);
        m.put(Material.GLOWSTONE,           ItemQuality.RARE);
        // 唱片
        m.put(Material.MUSIC_DISC_13,       ItemQuality.RARE);
        m.put(Material.MUSIC_DISC_CAT,      ItemQuality.RARE);
        m.put(Material.MUSIC_DISC_BLOCKS,   ItemQuality.RARE);
        m.put(Material.MUSIC_DISC_CHIRP,    ItemQuality.RARE);
        m.put(Material.MUSIC_DISC_FAR,      ItemQuality.RARE);
        m.put(Material.MUSIC_DISC_MALL,     ItemQuality.RARE);
        m.put(Material.MUSIC_DISC_MELLOHI,  ItemQuality.RARE);
        m.put(Material.MUSIC_DISC_STAL,     ItemQuality.RARE);
        m.put(Material.MUSIC_DISC_STRAD,    ItemQuality.RARE);
        m.put(Material.MUSIC_DISC_WARD,     ItemQuality.RARE);
        m.put(Material.MUSIC_DISC_11,       ItemQuality.RARE);
        m.put(Material.MUSIC_DISC_WAIT,     ItemQuality.RARE);
        m.put(Material.MUSIC_DISC_OTHERSIDE,ItemQuality.RARE);
        m.put(Material.MUSIC_DISC_5,        ItemQuality.RARE);
        m.put(Material.MUSIC_DISC_PIGSTEP,  ItemQuality.RARE);
        m.put(Material.MUSIC_DISC_RELIC,    ItemQuality.RARE);
        m.put(Material.MUSIC_DISC_CREATOR,  ItemQuality.RARE);
        m.put(Material.MUSIC_DISC_CREATOR_MUSIC_BOX, ItemQuality.RARE);
        m.put(Material.MUSIC_DISC_PRECIPICE,ItemQuality.RARE);
        // 地图 / 书
        m.put(Material.FILLED_MAP,          ItemQuality.RARE);
        m.put(Material.WRITTEN_BOOK,        ItemQuality.RARE);
        // 末地物品
        m.put(Material.ENDER_EYE,           ItemQuality.RARE);
        m.put(Material.END_ROD,             ItemQuality.RARE);
        m.put(Material.CHORUS_FRUIT,        ItemQuality.RARE);
        m.put(Material.POPPED_CHORUS_FRUIT, ItemQuality.RARE);
        m.put(Material.SHULKER_SHELL,       ItemQuality.RARE);
        m.put(Material.PURPUR_BLOCK,        ItemQuality.RARE);
        // 海洋
        m.put(Material.PRISMARINE_SHARD,    ItemQuality.RARE);
        m.put(Material.PRISMARINE_CRYSTALS, ItemQuality.RARE);
        m.put(Material.HEART_OF_THE_SEA,    ItemQuality.RARE);
        m.put(Material.NAUTILUS_SHELL,      ItemQuality.RARE);
        m.put(Material.SEA_LANTERN,         ItemQuality.RARE);
        // 特殊宝石
        m.put(Material.EMERALD,             ItemQuality.RARE);
        m.put(Material.EMERALD_ORE,         ItemQuality.RARE);
        m.put(Material.DEEPSLATE_EMERALD_ORE,ItemQuality.RARE);
        // 头颅
        m.put(Material.SKELETON_SKULL,      ItemQuality.RARE);
        m.put(Material.ZOMBIE_HEAD,         ItemQuality.RARE);
        m.put(Material.CREEPER_HEAD,        ItemQuality.RARE);
        m.put(Material.PIGLIN_HEAD,         ItemQuality.RARE);
        // 考古物品
        m.put(Material.ARCHER_POTTERY_SHERD,   ItemQuality.RARE);
        m.put(Material.PRIZE_POTTERY_SHERD,    ItemQuality.RARE);
        m.put(Material.ARMS_UP_POTTERY_SHERD,  ItemQuality.RARE);
        m.put(Material.SKULL_POTTERY_SHERD,    ItemQuality.RARE);
        // 盾牌
        m.put(Material.SHIELD,              ItemQuality.RARE);
        // 旗帜图案
        m.put(Material.CREEPER_BANNER_PATTERN, ItemQuality.RARE);
        m.put(Material.SKULL_BANNER_PATTERN,   ItemQuality.RARE);
        m.put(Material.FLOWER_BANNER_PATTERN,  ItemQuality.RARE);
        m.put(Material.MOJANG_BANNER_PATTERN,  ItemQuality.RARE);
        m.put(Material.GLOBE_BANNER_PATTERN,   ItemQuality.RARE);
        m.put(Material.PIGLIN_BANNER_PATTERN,  ItemQuality.RARE);
        // 神秘刷怪蛋
        m.put(Material.TRIAL_KEY,           ItemQuality.RARE);
        m.put(Material.OMINOUS_TRIAL_KEY,   ItemQuality.RARE);
        m.put(Material.OMINOUS_BOTTLE,      ItemQuality.RARE);
        m.put(Material.WIND_CHARGE,         ItemQuality.RARE);
        m.put(Material.HEAVY_CORE,          ItemQuality.RARE);
        m.put(Material.BREEZE_ROD,          ItemQuality.RARE);
        // 矿石（金）
        m.put(Material.GOLD_ORE,            ItemQuality.RARE);
        m.put(Material.DEEPSLATE_GOLD_ORE,  ItemQuality.RARE);
        m.put(Material.NETHER_GOLD_ORE,     ItemQuality.RARE);
        // 青金石矿
        m.put(Material.LAPIS_ORE,           ItemQuality.RARE);
        m.put(Material.DEEPSLATE_LAPIS_ORE, ItemQuality.RARE);
        // 刷怪蛋（稀有来源）
        m.put(Material.ELDER_GUARDIAN_SPAWN_EGG, ItemQuality.RARE);
        m.put(Material.WARDEN_SPAWN_EGG,    ItemQuality.RARE);
        m.put(Material.ENDER_DRAGON_SPAWN_EGG, ItemQuality.RARE);
        m.put(Material.WITHER_SPAWN_EGG,    ItemQuality.RARE);

        // ── EPIC ─────────────────────────────────────────────────────────────
        // 下界合金完整套装
        m.put(Material.NETHERITE_INGOT,     ItemQuality.EPIC);
        m.put(Material.NETHERITE_SWORD,     ItemQuality.EPIC);
        m.put(Material.NETHERITE_AXE,       ItemQuality.EPIC);
        m.put(Material.NETHERITE_PICKAXE,   ItemQuality.EPIC);
        m.put(Material.NETHERITE_SHOVEL,    ItemQuality.EPIC);
        m.put(Material.NETHERITE_HOE,       ItemQuality.EPIC);
        m.put(Material.NETHERITE_HELMET,    ItemQuality.EPIC);
        m.put(Material.NETHERITE_CHESTPLATE,ItemQuality.EPIC);
        m.put(Material.NETHERITE_LEGGINGS,  ItemQuality.EPIC);
        m.put(Material.NETHERITE_BOOTS,     ItemQuality.EPIC);
        m.put(Material.NETHERITE_BLOCK,     ItemQuality.EPIC);
        m.put(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, ItemQuality.EPIC);
        // 附魔金苹果（覆盖上面的 UNCOMMON）
        m.put(Material.ENCHANTED_GOLDEN_APPLE, ItemQuality.EPIC);
        // 信标
        m.put(Material.BEACON,              ItemQuality.EPIC);
        // 鞘翅
        m.put(Material.ELYTRA,              ItemQuality.EPIC);
        // 龙蛋
        m.put(Material.DRAGON_EGG,          ItemQuality.EPIC);
        // 龙息
        m.put(Material.DRAGON_BREATH,       ItemQuality.EPIC);
        // 下界之星（覆盖上面的 UNCOMMON）
        m.put(Material.NETHER_STAR,         ItemQuality.EPIC);
        // 幻术师之弓
        // （ILLUSIONER_SPAWN_EGG 不可获得，但弓本身注册）
        // 末影龙头颅
        m.put(Material.DRAGON_HEAD,         ItemQuality.EPIC);
        // 玩家头颅（有自定义皮肤）
        m.put(Material.PLAYER_HEAD,         ItemQuality.EPIC);
        // 旗帜图案（稀有）
        m.put(Material.FIELD_MASONED_BANNER_PATTERN,   ItemQuality.EPIC);
        m.put(Material.BORDURE_INDENTED_BANNER_PATTERN, ItemQuality.EPIC);
        // 决心盔甲纹饰
        m.put(Material.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE,   ItemQuality.EPIC);
        m.put(Material.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE,  ItemQuality.EPIC);
        m.put(Material.HOST_ARMOR_TRIM_SMITHING_TEMPLATE,   ItemQuality.EPIC);
        m.put(Material.WARD_ARMOR_TRIM_SMITHING_TEMPLATE,   ItemQuality.EPIC);
        m.put(Material.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE,ItemQuality.EPIC);
        m.put(Material.VEX_ARMOR_TRIM_SMITHING_TEMPLATE,    ItemQuality.EPIC);
        m.put(Material.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE, ItemQuality.EPIC);
        m.put(Material.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE, ItemQuality.EPIC);
        m.put(Material.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE, ItemQuality.EPIC);
        // 幸运马甲
        m.put(Material.ARMADILLO_SCUTE,     ItemQuality.EPIC);
        m.put(Material.WOLF_ARMOR,          ItemQuality.EPIC);
        // 铭文（刷声器盔甲）
        m.put(Material.MACE,                ItemQuality.EPIC);

        // ── LEGEND ───────────────────────────────────────────────────────────
        // 命令方块类（理论上只有创造/管理员能获得）
        m.put(Material.COMMAND_BLOCK,       ItemQuality.LEGEND);
        m.put(Material.CHAIN_COMMAND_BLOCK, ItemQuality.LEGEND);
        m.put(Material.REPEATING_COMMAND_BLOCK, ItemQuality.LEGEND);
        m.put(Material.COMMAND_BLOCK_MINECART, ItemQuality.LEGEND);
        m.put(Material.STRUCTURE_BLOCK,     ItemQuality.LEGEND);
        m.put(Material.STRUCTURE_VOID,      ItemQuality.LEGEND);
        m.put(Material.JIGSAW,              ItemQuality.LEGEND);
        m.put(Material.BARRIER,             ItemQuality.LEGEND);
        m.put(Material.LIGHT,               ItemQuality.LEGEND);
        m.put(Material.DEBUG_STICK,         ItemQuality.LEGEND);
        m.put(Material.KNOWLEDGE_BOOK,      ItemQuality.LEGEND);

        BUILTIN_PRESETS = Collections.unmodifiableMap(m);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  实例字段
    // ─────────────────────────────────────────────────────────────────────────

    private final JavaPlugin plugin;
    private final ConcurrentHashMap<Material, ItemQuality> customOverrides = new ConcurrentHashMap<>(64);

    public RarityManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  生命周期
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 从 {@code config.yml} 的 {@code rarity} 节点加载用户自定义品质覆盖。
     * 每次 reload 时调用，会清空并重建内存缓存。
     *
     * @param mainConfig 主配置文件（由 ConfigManager 传入）
     */
    public void load(FileConfiguration mainConfig) {
        customOverrides.clear();
        ConfigurationSection section = mainConfig.getConfigurationSection("rarity");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            Material mat = Material.matchMaterial(key.trim());
            if (mat == null) {
                plugin.getLogger().warning("[RarityManager] 未知 Material: " + key);
                continue;
            }
            String val = section.getString(key, "").toUpperCase().trim();
            ItemQuality quality = parseQuality(val);
            if (quality == null) {
                plugin.getLogger().warning("[RarityManager] 未知品质等级: " + val + " (for " + key + ")");
                continue;
            }
            customOverrides.put(mat, quality);
        }
        plugin.getLogger().info("[RarityManager] 已加载 " + customOverrides.size() + " 条自定义稀有度覆盖。");
    }

    /**
     * 清空内存缓存（插件卸载时调用）。
     */
    public void unload() {
        customOverrides.clear();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  查询
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 获取物品的品质。
     * <p>优先级：用户自定义 → 内建预设 → COMMON。</p>
     *
     * @param material 物品类型
     * @return 对应的 ItemQuality
     */
    public ItemQuality getQuality(Material material) {
        // 1. 用户自定义覆盖
        ItemQuality custom = customOverrides.get(material);
        if (custom != null) return custom;

        // 2. 内建预设
        ItemQuality preset = BUILTIN_PRESETS.get(material);
        if (preset != null) return preset;

        // 3. 兜底
        return ItemQuality.COMMON;
    }

    /**
     * 获取所有用户自定义覆盖的快照（不可变视图）。
     */
    public Map<Material, ItemQuality> getCustomOverrides() {
        return Collections.unmodifiableMap(customOverrides);
    }

    /**
     * 获取内建预设表（不可变）。
     */
    public static Map<Material, ItemQuality> getBuiltinPresets() {
        return BUILTIN_PRESETS;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  修改 & 持久化
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 设置物品的自定义稀有度（仅更新内存缓存）。
     * 持久化由 {@link cc.oniacute.plugin.itemglowing.config.ConfigManager#setMaterialRarity} 负责，
     * 它会将变更写回 {@code config.yml} 的 {@code rarity} 节点并异步保存。
     *
     * @param material 物品类型
     * @param quality  新品质；传 {@code null} 表示移除自定义覆盖（回退到内建预设）
     */
    public void setQuality(Material material, ItemQuality quality) {
        if (quality == null) {
            customOverrides.remove(material);
        } else {
            customOverrides.put(material, quality);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  工具方法
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 将字符串解析为 ItemQuality（不区分大小写）。
     * 接受：COMMON / UNCOMMON / RARE / EPIC / LEGEND
     * 返回 null 表示解析失败。
     */
    public static ItemQuality parseQuality(String s) {
        if (s == null) return null;
        return switch (s.trim().toUpperCase()) {
            case "COMMON"   -> ItemQuality.COMMON;
            case "UNCOMMON" -> ItemQuality.UNCOMMON;
            case "RARE"     -> ItemQuality.RARE;
            case "EPIC"     -> ItemQuality.EPIC;
            case "LEGEND"   -> ItemQuality.LEGEND;
            default         -> null;
        };
    }

    /**
     * 返回所有合法品质名称列表（供 Tab 补全）。
     */
    public static List<String> qualityNames() {
        return List.of("COMMON", "UNCOMMON", "RARE", "EPIC", "LEGEND");
    }
}
