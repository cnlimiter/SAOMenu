package com.sao.saomenu.server.party;

import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * 组队成员关系的解析与自愈。
 *
 * <p>成员关系必须同时写在 {@code PlayerTeam.players} 与 {@code Scoreboard} 的
 * {@code teamsByPlayer} 索引里,而索引只由 {@code addPlayerToTeam} 维护。
 * 历史版本只写了成员表,导致 {@code getPlayersTeam} 恒为 null、退队与登出清理全部失效;
 * 这些用例锁住「索引优先 + 成员表反查补登记」这一契约。</p>
 */
class SAOTeamManagerTest {

    private static final String PLAYER = "Alice";

    @Test
    void resolvesTeamFromIndex() {
        Scoreboard sb = new Scoreboard();
        PlayerTeam team = sb.addPlayerTeam("saomenu_aaaa");
        sb.addPlayerToTeam(PLAYER, team);

        assertSame(team, SAOTeamManager.teamOf(sb, PLAYER));
    }

    @Test
    void repairsMembershipMissingFromIndex() {
        Scoreboard sb = new Scoreboard();
        PlayerTeam team = sb.addPlayerTeam("saomenu_bbbb");
        // 模拟旧存档:名字只在成员表里,索引没登记
        team.getPlayers().add(PLAYER);
        assertNull(sb.getPlayersTeam(PLAYER), "前置条件:索引里确实查不到");

        assertSame(team, SAOTeamManager.teamOf(sb, PLAYER), "应能按成员表反查出来");
        assertSame(team, sb.getPlayersTeam(PLAYER), "反查命中后应就地补登记索引");
    }

    @Test
    void ignoresVanillaTeams() {
        Scoreboard sb = new Scoreboard();
        PlayerTeam vanilla = sb.addPlayerTeam("red");
        sb.addPlayerToTeam(PLAYER, vanilla);

        assertNull(SAOTeamManager.teamOf(sb, PLAYER), "原版 /team 建的队伍不参与组队逻辑");
    }

    @Test
    void returnsNullWhenPlayerHasNoParty() {
        Scoreboard sb = new Scoreboard();
        sb.addPlayerTeam("saomenu_cccc");

        assertNull(SAOTeamManager.teamOf(sb, PLAYER));
    }
}
