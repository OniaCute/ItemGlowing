package cc.oniacute.plugin.itemglowing.service;

import cc.oniacute.plugin.itemglowing.util.HexToNamedColor;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map;

/**
 * 发光效果控制服务。
 * <p>
 * 使用 Bukkit Scoreboard Team 机制控制掉落物的发光颜色：
 * <ul>
 *   <li>为每种 {@link NamedTextColor} 维护一个 Team（最多 16 个，延迟创建）。</li>
 *   <li>Team 名格式：{@code ig_<colorName>}（如 {@code ig_aqua}）。</li>
 *   <li>实体加入对应颜色的 Team 后开启 {@code setGlowing(true)}，即可显示对应颜色的发光轮廓。</li>
 *   <li>品质对应的精确 Hex 颜色通过 {@link HexToNamedColor#nearest} 近似映射到 NamedTextColor。</li>
 * </ul>
 * </p>
 * <p>
 * 所有方法必须在 Bukkit 主线程调用。
 * </p>
 */
public final class GlowService {

    /** Team 名称前缀，避免与其他插件冲突 */
    private static final String TEAM_PREFIX = "ig_";

    /** 颜色 → Team 缓存（延迟初始化，最多 16 条） */
    private final Map<NamedTextColor, Team> teamCache = new HashMap<>(16);

    private final Scoreboard scoreboard;

    public GlowService() {
        // 使用主 Scoreboard（所有玩家默认使用它，能看到 team glow 颜色）
        this.scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        // 清理上次插件运行时遗留的 Teams（防止重载后残留）
        cleanupTeams();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  公开 API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 为掉落物开启发光，并设置品质对应的近似颜色。
     * 仅在颜色发生变化时才重新分配 Team，减少不必要的操作。
     *
     * @param entity       掉落物实体
     * @param qualityColor 品质对应的精确 TextColor
     * @param lastColor    上次设置的颜色（null 表示首次设置）
     * @return 本次实际设置的 NamedTextColor（供调用方缓存，避免重复计算）
     */
    public NamedTextColor applyGlow(Item entity, TextColor qualityColor, NamedTextColor lastColor) {
        NamedTextColor named = HexToNamedColor.nearest(qualityColor);

        // 仅在颜色变化时重新分配 Team（避免每 tick 都做 Team 操作）
        if (!named.equals(lastColor)) {
            // 先从旧 Team 移除（若有）
            if (lastColor != null) {
                Team oldTeam = teamCache.get(lastColor);
                if (oldTeam != null) {
                    try { oldTeam.removeEntity(entity); } catch (Exception ignored) {}
                }
            }
            // 加入新 Team
            Team team = getOrCreateTeam(named);
            try { team.addEntity(entity); } catch (Exception ignored) {}
        }

        if (!entity.isGlowing()) {
            entity.setGlowing(true);
        }
        return named;
    }

    /**
     * 关闭掉落物的发光效果并将其从 Team 中移除。
     *
     * @param entity    掉落物实体
     * @param lastColor 上次设置的颜色（用于定位 Team），可为 null
     */
    public void removeGlow(Item entity, NamedTextColor lastColor) {
        if (entity.isGlowing()) {
            entity.setGlowing(false);
        }
        if (lastColor != null) {
            Team team = teamCache.get(lastColor);
            if (team != null) {
                try { team.removeEntity(entity); } catch (Exception ignored) {}
            }
        }
    }

    /**
     * 插件禁用时清理所有由本插件创建的 Teams。
     */
    public void cleanupTeams() {
        for (Team team : scoreboard.getTeams()) {
            if (team.getName().startsWith(TEAM_PREFIX)) {
                team.unregister();
            }
        }
        teamCache.clear();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  内部辅助
    // ─────────────────────────────────────────────────────────────────────────

    private Team getOrCreateTeam(NamedTextColor color) {
        return teamCache.computeIfAbsent(color, c -> {
            String teamName = TEAM_PREFIX + NamedTextColor.NAMES.key(c);
            // 若 Team 已存在（插件重载前残留），直接复用
            Team existing = scoreboard.getTeam(teamName);
            if (existing != null) {
                existing.color(c);
                return existing;
            }
            Team team = scoreboard.registerNewTeam(teamName);
            team.color(c);
            return team;
        });
    }
}
