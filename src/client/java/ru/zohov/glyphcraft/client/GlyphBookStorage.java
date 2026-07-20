package ru.zohov.glyphcraft.client;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import ru.zohov.glyphcraft.data.GlyphBook;

public final class GlyphBookStorage {
    private GlyphBookStorage() { }

    public static Path folder() {
        Path path = Minecraft.getInstance().gameDirectory.toPath().resolve("config/glyphcraft/books");
        try { Files.createDirectories(path); } catch (IOException ignored) { }
        return path;
    }

    public static void save(String fileName, GlyphBook book) throws IOException {
        Files.writeString(folder().resolve(safe(fileName) + ".json"), book.toJson(), StandardCharsets.UTF_8);
    }

    public static GlyphBook load(String fileName) throws IOException {
        Path file = folder().resolve(safe(fileName) + ".json");
        if (!Files.exists(file)) throw new IOException("Файл не найден: " + file.getFileName());
        return GlyphBook.fromJson(Files.readString(file, StandardCharsets.UTF_8));
    }

    public static List<String> list() throws IOException {
        try (Stream<Path> files = Files.list(folder())) {
            return files.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .map(p -> p.getFileName().toString().replaceFirst("\\.json$", ""))
                    .toList();
        }
    }

    public static void delete(String fileName) throws IOException {
        Files.deleteIfExists(folder().resolve(safe(fileName) + ".json"));
    }

    private static String safe(String value) {
        String clean = value == null ? "book" : value.trim();
        // Keep ordinary spaces in the file name so the title shown in the
        // load list matches what the player typed. Path separators and other
        // filesystem punctuation are still replaced.
        clean = clean.replaceAll("[^\\p{L}\\p{N}._ -]+", "_")
                .replaceAll("\\s+", " ").trim();
        if (clean.isBlank()) clean = "book";
        return clean.substring(0, Math.min(64, clean.length()));
    }
}
