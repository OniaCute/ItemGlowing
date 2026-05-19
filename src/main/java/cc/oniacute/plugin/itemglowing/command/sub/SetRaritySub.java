package cc.oniacute.plugin.itemglowing.command.sub;

import cc.oniacute.plugin.itemglowing.config.ConfigManager;
import cc.oniacute.plugin.itemglowing.config.PluginConfig;
import cc.oniacute.plugin.itemglowing.quality.ItemQuality;
import cc.oniacute.plugin.itemglowing.quality.RarityManager;
import cc.oniacute.plugin.itemglowing.util.MaterialUtil;
import cc.oniacute.plugin.itemglowing.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * /itemglowing setrarity &lt;material&gt; &lt;quality&gt;
 * <p>
 * 设置指定物品的品质等级，持久化到 {@code config.yml} 的 {@code rarity} 节点。
 * quality 可选：COMMON / UNCOMMON / RARE / EPIC / LEGEND
 * 使用 messages.rarityset 消息模板，支持 {item} 和 {rarity} 占位符。
 * </p>
 * <p>
 * 特殊用法：{@code /itemglowing setrarity <material> reset} 移除自定义覆盖，回退到内建预设。
 * </p>
 */
public final class SetRaritySub implements SubCommand {

    private static final List<String> QUALITY_OPTIONS;

    static {
        List<String> opts = new ArrayList<>(RarityManager.qualityNames());
        opts.add("RESET");
        QUALITY_OPTIONS = Collections.unmodifiableList(opts);
    }

    private final ConfigManager configManager;

    public SetRaritySub(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        PluginConfig cfg = configManager.current();

        if (args.length < 2) {
            MessageUtil.send(sender, cfg.msg("invalid"), cfg.prefix());
            return true;
        }

        // 解析 Material
        Material material = MaterialUtil.fromName(args[0]);
        if (material == null) {
            MessageUtil.send(sender, cfg.msg("invalid"), cfg.prefix(),
                    Map.of("input", args[0]));
            return true;
        }

        String qualityStr = args[1].toUpperCase().trim();

        // 特殊值 RESET：移除自定义覆盖
        if (qualityStr.equals("RESET")) {
            configManager.setMaterialRarity(material, null);
            // 重置后获取实际回退的品质（内建预设或 COMMON）
            RarityManager rm = configManager.getRarityManager();
            ItemQuality fallback = rm != null ? rm.getQuality(material) : ItemQuality.COMMON;
            PluginConfig newCfg = configManager.current();
            MessageUtil.send(sender, newCfg.msg("rarityset"), newCfg.prefix(),
                    Map.of("item", material.name(), "rarity", fallback.name()));
            return true;
        }

        // 解析 ItemQuality
        ItemQuality quality = RarityManager.parseQuality(qualityStr);
        if (quality == null) {
            MessageUtil.send(sender, cfg.msg("invalid"), cfg.prefix(),
                    Map.of("input", args[1]));
            return true;
        }

        configManager.setMaterialRarity(material, quality);
        PluginConfig newCfg = configManager.current();
        MessageUtil.send(sender, newCfg.msg("rarityset"), newCfg.prefix(),
                Map.of("item", material.name(), "rarity", quality.name()));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // 第一参数：Material 名称补全
            return MaterialUtil.filterMaterials(args[0]);
        }
        if (args.length == 2) {
            // 第二参数：品质等级补全
            String prefix = args[1].toUpperCase();
            List<String> result = new ArrayList<>();
            for (String opt : QUALITY_OPTIONS) {
                if (opt.startsWith(prefix)) result.add(opt);
            }
            return result;
        }
        return Collections.emptyList();
    }
}
