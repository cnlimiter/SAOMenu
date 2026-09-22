package com.sao.saomenu.dev.preview.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sao.saomenu.SAOMenu;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Objects;

/**
 * Shared stage rendezvous for the dedicated-server plus two-client probe.
 * JSON files only unblock the next stage; they are not behavioral proof.
 */
public final class MultiplayerPreviewSupport {
    public static final String PROP_DIR = "saomenu.preview";
    public static final String PROP_ROLE = "saomenu.preview.multiplayer";
    public static final String PROP_ADDRESS = "saomenu.preview.multiplayerAddress";
    public static final String PROP_KEEP_OPEN = "saomenu.preview.keepOpen";

    public static final String ROLE_SERVER = "server";
    public static final String ROLE_ALPHA = "alpha";
    public static final String ROLE_BETA = "beta";

    public static final String ALPHA = "SaoVerifyAlpha";
    public static final String BETA = "SaoVerifyBeta";

    public static final String STATE_FILE = "multiplayer-state.json";
    public static final String ALPHA_FILE = "alpha-observed.json";
    public static final String BETA_FILE = "beta-observed.json";

    public static final String STAGE_BOOT = "boot";
    public static final String STAGE_FIXTURES = "fixtures_ready";
    public static final String STAGE_INVENTORY = "inventory";
    public static final String STAGE_PARTY_JOINED = "party_joined";
    public static final String STAGE_PARTY_LEFT = "party_left";
    public static final String STAGE_DIMENSION = "dimension";
    public static final String STAGE_BETA_DISC = "beta_disconnected";
    public static final String STAGE_BETA_REJOIN = "beta_rejoined";
    public static final String STAGE_PASSED = "passed";

    public static final int SLOT_APPLES = 0;
    public static final int SLOT_CHEST = 2;
    public static final int SLOT_DIAMOND = 10;
    public static final int SLOT_GOLD = 11;
    public static final int SLOT_IRON = 12;
    public static final int SLOT_STONE = 13;
    public static final int APPLE_START = 4;
    public static final int APPLE_AFTER_DROP = 3;

    public static final float HUD_HOME_X = 0.82f;
    public static final float HUD_HOME_Y = 0.12f;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private MultiplayerPreviewSupport() {
    }

    public static String role() {
        String value = System.getProperty(PROP_ROLE);
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isServer() {
        return ROLE_SERVER.equals(role());
    }

    public static boolean isClientRole() {
        String value = role();
        return ROLE_ALPHA.equals(value) || ROLE_BETA.equals(value);
    }

    public static Path previewDir() {
        String out = System.getProperty(PROP_DIR);
        if (out == null || out.isBlank()) {
            throw new IllegalStateException(PROP_DIR + " must name the owned evidence/coordination directory");
        }
        return Path.of(out);
    }

    public static String address() {
        String value = System.getProperty(PROP_ADDRESS);
        if (value == null || !value.matches("127\\.0\\.0\\.1:[0-9]{1,5}")) {
            throw new IllegalStateException(PROP_ADDRESS + " must supply an explicit IPv4 loopback server address");
        }
        int port = Integer.parseInt(value.substring(value.indexOf(':') + 1));
        require(port > 0 && port <= 65535, "Loopback port is out of range");
        return value;
    }

    public static boolean keepOpen() {
        return Boolean.getBoolean(PROP_KEEP_OPEN);
    }

    public static Path stateFile() {
        return previewDir().resolve(STATE_FILE);
    }

    public static Path observedFile(String clientRole) {
        if (ROLE_ALPHA.equals(clientRole)) {
            return previewDir().resolve(ALPHA_FILE);
        }
        if (ROLE_BETA.equals(clientRole)) {
            return previewDir().resolve(BETA_FILE);
        }
        throw new IllegalArgumentException("Unknown client role: " + clientRole);
    }

    public static JsonObject readJson(Path file) {
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            String raw = Files.readString(file, StandardCharsets.UTF_8);
            if (raw.isBlank()) {
                return null;
            }
            return JsonParser.parseString(raw).getAsJsonObject();
        } catch (IOException | RuntimeException error) {
            throw new IllegalStateException("Cannot read multiplayer rendezvous " + file, error);
        }
    }

    public static void writeAtomic(Path file, JsonObject json) {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(json, "json");
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path tmp = file.resolveSibling(file.getFileName().toString() + ".tmp");
            Files.writeString(tmp, GSON.toJson(json), StandardCharsets.UTF_8);
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed writing " + file, e);
        }
    }

    public static String stageOf(JsonObject json) {
        if (json == null || !json.has("stage")) {
            return "";
        }
        return json.get("stage").getAsString();
    }

    public static int seqOf(JsonObject json) {
        if (json == null || !json.has("seq")) {
            return 0;
        }
        return json.get("seq").getAsInt();
    }

    public static String ackStageOf(JsonObject json) {
        if (json == null || !json.has("ackStage")) {
            return "";
        }
        return json.get("ackStage").getAsString();
    }

    public static boolean acked(String clientRole, String stage) {
        JsonObject json = readJson(observedFile(clientRole));
        return sameRun(json) && stage.equals(ackStageOf(json));
    }

    public static void writeClientAck(String clientRole, String stage, JsonObject extras) {
        JsonObject json = extras == null ? new JsonObject() : extras.deepCopy();
        json.addProperty("role", clientRole);
        json.addProperty("ackStage", stage);
        JsonObject state = readJson(stateFile());
        json.addProperty("seq", seqOf(state));
        require(state != null && state.has("run"), "Dedicated fixture has not published its run");
        json.add("run", state.get("run"));
        writeAtomic(observedFile(clientRole), json);
    }

    public static boolean safetyWaiting() {
        return awaitingSafety(ROLE_ALPHA) || awaitingSafety(ROLE_BETA);
    }

    private static boolean awaitingSafety(String clientRole) {
        JsonObject json = readJson(observedFile(clientRole));
        return sameRun(json) && json.has("awaitingSafety") && json.get("awaitingSafety").getAsBoolean();
    }

    /** Written by the operator only after explicit approval of this particular native prompt. */
    public static Path safetyApprovalFile(String clientRole, int occurrence) {
        JsonObject state = readJson(stateFile());
        require(state != null && state.has("run"), "Safety approval has no active dedicated fixture");
        return previewDir().resolve(state.get("run").getAsString() + "-" + clientRole
                + "-safety-" + occurrence + ".approved");
    }

    public static void setAwaitingSafety(String clientRole, boolean awaiting) {
        JsonObject json = readJson(observedFile(clientRole));
        if (!sameRun(json)) json = new JsonObject();
        JsonObject state = readJson(stateFile());
        require(state != null && state.has("run"), "Safety prompt has no active dedicated fixture");
        json.addProperty("role", clientRole);
        json.addProperty("awaitingSafety", awaiting);
        json.add("run", state.get("run"));
        writeAtomic(observedFile(clientRole), json);
    }

    private static boolean sameRun(JsonObject json) {
        JsonObject state = readJson(stateFile());
        return json != null && state != null && state.has("run") && state.get("run").equals(json.get("run"));
    }

    public static void pass(String who, String stage, String detail) {
        SAOMenu.LOGGER.info("[SAOMenu] multiplayer {} PASS: {} {}", who, stage, detail);
    }

    public static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
