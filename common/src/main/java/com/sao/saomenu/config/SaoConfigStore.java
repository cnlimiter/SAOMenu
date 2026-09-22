package com.sao.saomenu.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * {@code saomenu.json} 的读写。不解析业务默认值,不猜测 Minecraft 目录。
 */
final class SaoConfigStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private SaoConfigStore() {
    }

    static SaoConfigData read(Path file) throws IOException, JsonSyntaxException {
        String json = Files.readString(file, StandardCharsets.UTF_8);
        return GSON.fromJson(json, SaoConfigData.class);
    }

    static void write(Path file, SaoConfigData data) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(file, GSON.toJson(data), StandardCharsets.UTF_8);
    }
}
