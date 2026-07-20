package ru.zohov.glyphcraft.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.List;

public final class GlyphBook {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public String name = "Новая книга";
    public List<Page> pages = new ArrayList<>();

    public GlyphBook() {
        pages.add(new Page());
    }

    public static GlyphBook fromJson(String json) {
        try {
            GlyphBook book = GSON.fromJson(json, GlyphBook.class);
            if (book == null) book = new GlyphBook();
            if (book.pages == null || book.pages.isEmpty()) book.pages = new ArrayList<>(List.of(new Page()));
            return book;
        } catch (RuntimeException ignored) {
            return new GlyphBook();
        }
    }

    public String toJson() { return GSON.toJson(this); }

    public static final class Page {
        public String title = "Страница";
        public String text = "";
        public String color = "#F5F7FF";
        public boolean bold;
        public boolean italic;

        public Page copy() {
            Page p = new Page();
            p.title = title;
            p.text = text;
            p.color = color;
            p.bold = bold;
            p.italic = italic;
            return p;
        }
    }
}
