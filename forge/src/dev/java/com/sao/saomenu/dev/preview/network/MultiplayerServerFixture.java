package com.sao.saomenu.dev.preview.network;

import com.google.gson.JsonObject;
import com.sao.saomenu.SAOMenu;
import com.sao.saomenu.server.party.SAOTeamManager;
import com.sao.saomenu.server.skill.SaoSkillCooldowns;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeConfig;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Dedicated-server fixture. Inert unless {@code saomenu.preview.multiplayer=server}.
 * Does not bind, start, accept the EULA, or rewrite online-mode.
 */
@Mod.EventBusSubscriber(modid = SAOMenu.MOD_ID, value = Dist.DEDICATED_SERVER)
public final class MultiplayerServerFixture {
    private static final int TICK_LIMIT = 12000;
    private static final int NETHER_HOLD = 40;

    private static boolean active;
    private static MinecraftServer server;
    private static BlockPos origin;
    private static String stage = "";
    private static String run;
    private static int seq;
    private static int ticks;
    private static int hold;
    private static final Set<UUID> fitted = new HashSet<>();
    private static boolean netherSeen;
    private static boolean overworldReturnSeen;
    private static boolean passed;

    private MultiplayerServerFixture() {
    }

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent event) {
        if (!MultiplayerPreviewSupport.isServer()) {
            return;
        }
        active = true;
        server = event.getServer();
        MultiplayerPreviewSupport.require("127.0.0.1".equals(server.getLocalIp())
                        && MultiplayerPreviewSupport.address().equals("127.0.0.1:" + server.getPort()),
                "Dedicated fixture must use the explicitly configured loopback endpoint");
        MultiplayerPreviewSupport.require(!ForgeConfig.SERVER.advertiseDedicatedServerToLan.get(),
                "Loopback verification must disable Forge LAN advertisement");
        run = UUID.randomUUID().toString();
        ServerLevel overworld = server.overworld();
        MultiplayerPreviewSupport.require(overworld != null, "Dedicated overworld is missing");
        origin = overworld.getSharedSpawnPos();
        prepareWorld(overworld);
        writeStage(MultiplayerPreviewSupport.STAGE_BOOT, facts(overworld));
        MultiplayerPreviewSupport.pass("server", MultiplayerPreviewSupport.STAGE_BOOT,
                "platform=" + origin.toShortString());
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent event) {
        if (!active) {
            return;
        }
        active = false;
        server = null;
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!active || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        String name = player.getGameProfile().getName();
        if (!MultiplayerPreviewSupport.ALPHA.equals(name) && !MultiplayerPreviewSupport.BETA.equals(name)) {
            return;
        }
        installPlayer(player, MultiplayerPreviewSupport.STAGE_BETA_DISC.equals(stage)
                || MultiplayerPreviewSupport.STAGE_BETA_REJOIN.equals(stage)
                || MultiplayerPreviewSupport.STAGE_DIMENSION.equals(stage)
                || MultiplayerPreviewSupport.STAGE_PARTY_LEFT.equals(stage)
                || MultiplayerPreviewSupport.STAGE_PARTY_JOINED.equals(stage)
                || MultiplayerPreviewSupport.STAGE_INVENTORY.equals(stage));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!active || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        fitted.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!active || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!MultiplayerPreviewSupport.ALPHA.equals(player.getGameProfile().getName())) {
            return;
        }
        if (event.getTo() == Level.NETHER) {
            netherSeen = true;
        }
        if (netherSeen && event.getTo() == Level.OVERWORLD) {
            overworldReturnSeen = true;
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if (!active || event.phase != TickEvent.Phase.END || passed || server == null) {
            return;
        }
        if (MultiplayerPreviewSupport.safetyWaiting()) return;
        if (!MultiplayerPreviewSupport.STAGE_BOOT.equals(stage) && ++ticks > TICK_LIMIT) {
            throw new IllegalStateException("Dedicated multiplayer fixture timed out at " + stage
                    + " alpha=" + online(MultiplayerPreviewSupport.ALPHA)
                    + " beta=" + online(MultiplayerPreviewSupport.BETA));
        }
        switch (stage) {
            case MultiplayerPreviewSupport.STAGE_BOOT -> waitFixtures();
            case MultiplayerPreviewSupport.STAGE_FIXTURES -> waitInventory();
            case MultiplayerPreviewSupport.STAGE_INVENTORY -> waitPartyJoined();
            case MultiplayerPreviewSupport.STAGE_PARTY_JOINED -> waitPartyLeft();
            case MultiplayerPreviewSupport.STAGE_PARTY_LEFT -> runDimension();
            case MultiplayerPreviewSupport.STAGE_DIMENSION -> waitBetaGone();
            case MultiplayerPreviewSupport.STAGE_BETA_DISC -> waitBetaBack();
            case MultiplayerPreviewSupport.STAGE_BETA_REJOIN -> finish();
            default -> { }
        }
    }

    private static void waitFixtures() {
        ServerPlayer alpha = player(MultiplayerPreviewSupport.ALPHA);
        ServerPlayer beta = player(MultiplayerPreviewSupport.BETA);
        if (alpha == null || beta == null) {
            return;
        }
        if (!fitted.contains(alpha.getUUID()) || !fitted.contains(beta.getUUID())) {
            return;
        }
        if (!MultiplayerPreviewSupport.acked(MultiplayerPreviewSupport.ROLE_ALPHA, MultiplayerPreviewSupport.STAGE_BOOT)
                || !MultiplayerPreviewSupport.acked(MultiplayerPreviewSupport.ROLE_BETA, MultiplayerPreviewSupport.STAGE_BOOT)) {
            return;
        }
        MultiplayerPreviewSupport.require(alpha.serverLevel().dimension() == Level.OVERWORLD,
                "Alpha did not spawn in the owned overworld");
        MultiplayerPreviewSupport.require(beta.serverLevel().dimension() == Level.OVERWORLD,
                "Beta did not spawn in the owned overworld");
        writeStage(MultiplayerPreviewSupport.STAGE_FIXTURES, facts(server.overworld()));
        MultiplayerPreviewSupport.pass("server", MultiplayerPreviewSupport.STAGE_FIXTURES,
                "both players online with owned inventories");
    }

    private static void waitInventory() {
        ServerPlayer alpha = player(MultiplayerPreviewSupport.ALPHA);
        if (alpha == null) {
            return;
        }
        var inv = alpha.getInventory();
        if (!inv.armor.get(2).is(Items.IRON_CHESTPLATE)) {
            return;
        }
        if (count(alpha, Items.APPLE) != MultiplayerPreviewSupport.APPLE_AFTER_DROP) {
            return;
        }
        if (!alpha.getMainHandItem().is(Items.DIAMOND_SWORD) || !alpha.getOffhandItem().is(Items.GOLDEN_SWORD)) {
            return;
        }
        MultiplayerPreviewSupport.require(count(alpha, Items.DIAMOND_SWORD) == 1
                        && count(alpha, Items.GOLDEN_SWORD) == 1
                        && count(alpha, Items.IRON_SWORD) == 1
                        && count(alpha, Items.STONE_SWORD) == 1,
                "Dual-wield did not conserve swords");
        MultiplayerPreviewSupport.require(count(alpha, Items.APPLE) == MultiplayerPreviewSupport.APPLE_AFTER_DROP,
                "Drop did not conserve remaining apples");
        MultiplayerPreviewSupport.require(nearbyApple(alpha),
                "Authoritative drop did not produce an apple item entity");
        MultiplayerPreviewSupport.require(inv.getItem(MultiplayerPreviewSupport.SLOT_IRON).is(Items.IRON_SWORD)
                        && inv.getItem(MultiplayerPreviewSupport.SLOT_STONE).is(Items.STONE_SWORD),
                "Cooldown dual-wield mutated reserved swords");
        writeStage(MultiplayerPreviewSupport.STAGE_INVENTORY, facts(alpha.serverLevel()));
        MultiplayerPreviewSupport.pass("server", MultiplayerPreviewSupport.STAGE_INVENTORY,
                "equip/drop/dual-wield/cooldown observed on the dedicated player");
    }

    private static void waitPartyJoined() {
        ServerPlayer alpha = player(MultiplayerPreviewSupport.ALPHA);
        ServerPlayer beta = player(MultiplayerPreviewSupport.BETA);
        if (alpha == null || beta == null) {
            return;
        }
        PlayerTeam team = SAOTeamManager.teamOf(server, alpha);
        if (team == null) {
            return;
        }
        if (!team.getPlayers().contains(MultiplayerPreviewSupport.ALPHA)
                || !team.getPlayers().contains(MultiplayerPreviewSupport.BETA)) {
            return;
        }
        MultiplayerPreviewSupport.require(SAOTeamManager.teamOf(server, beta) == team,
                "Beta is not on Alpha's authoritative team");
        MultiplayerPreviewSupport.require(alpha.getMainHandItem().is(Items.DIAMOND_SWORD)
                        && alpha.getOffhandItem().is(Items.GOLDEN_SWORD)
                        && alpha.getInventory().getItem(MultiplayerPreviewSupport.SLOT_IRON).is(Items.IRON_SWORD)
                        && alpha.getInventory().getItem(MultiplayerPreviewSupport.SLOT_STONE).is(Items.STONE_SWORD)
                        && count(alpha, Items.DIAMOND_SWORD) == 1 && count(alpha, Items.GOLDEN_SWORD) == 1
                        && count(alpha, Items.IRON_SWORD) == 1 && count(alpha, Items.STONE_SWORD) == 1
                        && count(alpha, Items.SHIELD) == 1 && count(alpha, Items.IRON_CHESTPLATE) == 1,
                "Authoritative inventory changed after the cooldown request");
        writeStage(MultiplayerPreviewSupport.STAGE_PARTY_JOINED, facts(alpha.serverLevel()));
        MultiplayerPreviewSupport.pass("server", MultiplayerPreviewSupport.STAGE_PARTY_JOINED,
                "scoreboard members=" + team.getPlayers());
    }

    private static void waitPartyLeft() {
        ServerPlayer alpha = player(MultiplayerPreviewSupport.ALPHA);
        ServerPlayer beta = player(MultiplayerPreviewSupport.BETA);
        if (alpha == null || beta == null) {
            return;
        }
        PlayerTeam betaTeam = SAOTeamManager.teamOf(server, beta);
        if (betaTeam != null) {
            return;
        }
        writeStage(MultiplayerPreviewSupport.STAGE_PARTY_LEFT, facts(alpha.serverLevel()));
        MultiplayerPreviewSupport.pass("server", MultiplayerPreviewSupport.STAGE_PARTY_LEFT,
                "Beta left; remaining=" + String.valueOf(SAOTeamManager.teamOf(server, alpha)));
    }

    private static void runDimension() {
        if (!MultiplayerPreviewSupport.acked(MultiplayerPreviewSupport.ROLE_ALPHA,
                MultiplayerPreviewSupport.STAGE_PARTY_LEFT)) {
            return;
        }
        ServerPlayer alpha = player(MultiplayerPreviewSupport.ALPHA);
        MultiplayerPreviewSupport.require(alpha != null, "Alpha vanished before dimension teleport");
        ServerLevel nether = server.getLevel(Level.NETHER);
        MultiplayerPreviewSupport.require(nether != null,
                "Level.NETHER is not loaded (allow-nether closed); cannot exercise SessionListener dimension transition");
        if (!netherSeen) {
            placeNetherPad(nether);
            nether.getChunkAt(BlockPos.ZERO);
            alpha.teleportTo(nether, 0.5, 65.0, 0.5, Set.<RelativeMovement>of(), 0f, 0f);
            hold = 0;
            return;
        }
        if (alpha.serverLevel().dimension() == Level.NETHER && !overworldReturnSeen) {
            if (++hold < NETHER_HOLD) {
                return;
            }
            ServerLevel overworld = server.overworld();
            alpha.teleportTo(overworld, origin.getX() + 0.5, origin.getY() + 1.0, origin.getZ() + 0.5,
                    Set.<RelativeMovement>of(), 0f, 0f);
            hold = 0;
            return;
        }
        if (!overworldReturnSeen || alpha.serverLevel().dimension() != Level.OVERWORLD) {
            return;
        }
        writeStage(MultiplayerPreviewSupport.STAGE_DIMENSION, facts(alpha.serverLevel()));
        MultiplayerPreviewSupport.pass("server", MultiplayerPreviewSupport.STAGE_DIMENSION,
                "Alpha overworld→nether→overworld");
    }

    private static void waitBetaGone() {
        if (online(MultiplayerPreviewSupport.BETA)) {
            return;
        }
        MultiplayerPreviewSupport.require(player(MultiplayerPreviewSupport.ALPHA) != null,
                "Alpha dropped while waiting for Beta disconnect");
        writeStage(MultiplayerPreviewSupport.STAGE_BETA_DISC, facts(server.overworld()));
        MultiplayerPreviewSupport.pass("server", MultiplayerPreviewSupport.STAGE_BETA_DISC, "Beta logged out");
    }

    private static void waitBetaBack() {
        ServerPlayer beta = player(MultiplayerPreviewSupport.BETA);
        if (beta == null) {
            return;
        }
        writeStage(MultiplayerPreviewSupport.STAGE_BETA_REJOIN, facts(beta.serverLevel()));
        MultiplayerPreviewSupport.pass("server", MultiplayerPreviewSupport.STAGE_BETA_REJOIN, "Beta logged in again");
    }

    private static void finish() {
        if (!MultiplayerPreviewSupport.acked(MultiplayerPreviewSupport.ROLE_ALPHA,
                MultiplayerPreviewSupport.STAGE_DIMENSION)) {
            return;
        }
        if (!MultiplayerPreviewSupport.acked(MultiplayerPreviewSupport.ROLE_BETA,
                MultiplayerPreviewSupport.STAGE_BETA_REJOIN)) {
            return;
        }
        writeStage(MultiplayerPreviewSupport.STAGE_PASSED, facts(server.overworld()));
        passed = true;
        MultiplayerPreviewSupport.pass("server", MultiplayerPreviewSupport.STAGE_PASSED,
                "authoritative inventory, party, dimension and reconnect");
        SAOMenu.LOGGER.info("[SAOMenu] multiplayer server checks passed: alpha={} beta={} ticks={}",
                MultiplayerPreviewSupport.ALPHA, MultiplayerPreviewSupport.BETA, ticks);
    }

    private static void prepareWorld(ServerLevel overworld) {
        server.setDifficulty(Difficulty.PEACEFUL, true);
        GameRules rules = overworld.getGameRules();
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        rules.getRule(GameRules.RULE_MOBGRIEFING).set(false, server);
        overworld.setDayTime(6000L);
        BlockState floor = Blocks.SMOOTH_STONE.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        int y = origin.getY();
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                BlockPos floorPos = new BlockPos(origin.getX() + dx, y, origin.getZ() + dz);
                overworld.setBlockAndUpdate(floorPos, floor);
                for (int dy = 1; dy <= 4; dy++) {
                    overworld.setBlockAndUpdate(floorPos.above(dy), air);
                }
            }
        }
        overworld.setDefaultSpawnPos(origin.above(), 0f);
    }

    private static void placeNetherPad(ServerLevel nether) {
        BlockState obsidian = Blocks.OBSIDIAN.defaultBlockState();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                nether.setBlockAndUpdate(new BlockPos(dx, 64, dz), obsidian);
                nether.setBlockAndUpdate(new BlockPos(dx, 65, dz), Blocks.AIR.defaultBlockState());
                nether.setBlockAndUpdate(new BlockPos(dx, 66, dz), Blocks.AIR.defaultBlockState());
            }
        }
    }

    private static void installPlayer(ServerPlayer player, boolean rejoin) {
        player.setGameMode(GameType.SURVIVAL);
        player.setHealth(20f);
        player.getFoodData().setFoodLevel(20);
        if (origin != null) {
            ServerLevel home = player.getServer().overworld();
            double offset = MultiplayerPreviewSupport.BETA.equals(player.getGameProfile().getName()) ? 3.5 : 0.5;
            player.teleportTo(home, origin.getX() + offset, origin.getY() + 1.0, origin.getZ() + 0.5,
                    Set.<RelativeMovement>of(), 0f, 0f);
        }
        if (rejoin) {
            return;
        }
        var inv = player.getInventory();
        inv.clearContent();
        inv.selected = 0;
        inv.setItem(MultiplayerPreviewSupport.SLOT_APPLES, new ItemStack(Items.APPLE, MultiplayerPreviewSupport.APPLE_START));
        inv.setItem(MultiplayerPreviewSupport.SLOT_CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        inv.setItem(MultiplayerPreviewSupport.SLOT_DIAMOND, new ItemStack(Items.DIAMOND_SWORD));
        inv.setItem(MultiplayerPreviewSupport.SLOT_GOLD, new ItemStack(Items.GOLDEN_SWORD));
        inv.setItem(MultiplayerPreviewSupport.SLOT_IRON, new ItemStack(Items.IRON_SWORD));
        inv.setItem(MultiplayerPreviewSupport.SLOT_STONE, new ItemStack(Items.STONE_SWORD));
        inv.offhand.set(0, new ItemStack(Items.SHIELD));
        SaoSkillCooldowns.clearPlayer(player.getUUID());
        inv.setChanged();
        player.inventoryMenu.broadcastChanges();
        fitted.add(player.getUUID());
    }

    private static boolean nearbyApple(ServerPlayer player) {
        return !player.serverLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(6.0),
                entity -> entity.getItem().is(Items.APPLE)).isEmpty();
    }

    private static int count(ServerPlayer player, net.minecraft.world.item.Item item) {
        int total = 0;
        var inv = player.getInventory();
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static ServerPlayer player(String name) {
        return server == null ? null : server.getPlayerList().getPlayerByName(name);
    }

    private static boolean online(String name) {
        return player(name) != null;
    }

    private static JsonObject facts(ServerLevel level) {
        JsonObject json = new JsonObject();
        json.addProperty("alphaOnline", online(MultiplayerPreviewSupport.ALPHA));
        json.addProperty("betaOnline", online(MultiplayerPreviewSupport.BETA));
        ResourceKey<Level> dim = level == null ? null : level.dimension();
        json.addProperty("dimension", dim == null ? "" : dim.location().toString());
        json.addProperty("origin", origin == null ? "" : origin.toShortString());
        return json;
    }

    private static void writeStage(String next, JsonObject facts) {
        stage = next;
        seq++;
        JsonObject json = facts == null ? new JsonObject() : facts.deepCopy();
        json.addProperty("run", run);
        json.addProperty("stage", next);
        json.addProperty("seq", seq);
        json.addProperty("alpha", MultiplayerPreviewSupport.ALPHA);
        json.addProperty("beta", MultiplayerPreviewSupport.BETA);
        MultiplayerPreviewSupport.writeAtomic(MultiplayerPreviewSupport.stateFile(), json);
    }
}
