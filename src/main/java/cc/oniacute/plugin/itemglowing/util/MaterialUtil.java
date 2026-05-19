package cc.oniacute.plugin.itemglowing.util;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Material 工具类：提供名称解析（不区分大小写）和物品列表缓存。
 */
public final class MaterialUtil {

    /** 所有可作为物品的 Material 名称列表（只初始化一次） */
    private static volatile List<String> itemMaterialNames = null;

    private MaterialUtil() {}

    /**
     * 按名称查找 Material，不区分大小写。
     *
     * @param name Material 名称，如 "netherrack" 或 "NETHERRACK"
     * @return 对应的 Material；未找到返回 null
     */
    public static Material fromName(String name) {
        if (name == null || name.isEmpty()) return null;
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 获取所有可作为物品的 Material 名称列表（懒加载，线程安全）。
     * 主要供 Tab 补全使用。
     *
     * @return 不可变的名称列表
     */
    public static List<String> getItemMaterialNames() {
        if (itemMaterialNames == null) {
            synchronized (MaterialUtil.class) {
                if (itemMaterialNames == null) {
                    List<String> names = new ArrayList<>(1500);
                    for (Material m : Material.values()) {
                        if (m.isItem() && !m.isAir()) {
                            names.add(m.name());
                        }
                    }
                    itemMaterialNames = Collections.unmodifiableList(names);
                }
            }
        }
        return itemMaterialNames;
    }

    /**
     * 获取以指定前缀开头的 Material 名称列表（供 Tab 补全过滤使用）。
     *
     * @param prefix 前缀，不区分大小写
     * @return 匹配的名称列表
     */
    public static List<String> filterMaterials(String prefix) {
        String upper = prefix.toUpperCase();
        List<String> result = new ArrayList<>();
        for (String name : getItemMaterialNames()) {
            if (name.startsWith(upper)) {
                result.add(name);
            }
        }
        return result;
    }
}
