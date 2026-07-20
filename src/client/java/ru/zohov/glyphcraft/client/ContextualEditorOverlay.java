package ru.zohov.glyphcraft.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.network.chat.Component;
import ru.zohov.glyphcraft.data.GlyphBook;
import ru.zohov.glyphcraft.mixin.BookEditScreenAccessor;
import ru.zohov.glyphcraft.mixin.MultiLineEditBoxAccessor;
import ru.zohov.glyphcraft.mixin.MultilineTextFieldAccessor;
import ru.zohov.glyphcraft.mixin.SignEditScreenAccessor;
import ru.zohov.glyphcraft.mixin.ChatScreenAccessor;

/**
 * Ненавязчивая панель, которая существует только поверх настоящего редактора
 * книги или таблички. В главном меню и на остальных экранах этот класс ничего
 * не рисует и не перехватывает.
 */
public final class ContextualEditorOverlay {
    private static final int BUTTON_BG = 0xFF6C6C6C;
    private static final int BUTTON_HOVER = 0xFF858585;
    private static final int BUTTON_ACTIVE = 0xFF777777;
    private static final int TEXT = 0xFFF0F5FF;
    private static final int MUTED = 0xFF91A6C4;
    private static final int ACCENT = 0xFF7BE0CC;

    private static final String[] BOOK_LEFT = {
            "Загрузить книгу", "Сохранить книгу", "Очистить книгу", "Копировать книгу", "Вставить книгу"
    };
    private static final String[] BOOK_RIGHT = {
            "Очистить страницу", "Копировать страницу", "Вставить страницу", "− Удалить страницу",
            "Выровнять область"
    };
    private static final String[] SIGN_LEFT = {
            "Загрузить табличку", "Сохранить табличку", "Копировать всё"
    };
    private static final String[] SIGN_RIGHT = {
            "Вставить всё", "Очистить табличку"
    };
    private static final String[] BOOK_MENU_LABELS = {
            "Сохранить книгу", "Загрузить книгу", "Очистить страницу", "Очистить книгу",
            "Вставить страницу", "Удалить страницу", "Выровнять область", "Закрыть"
    };
    private static final int[] COLORS = {
            0xFF000000, 0xFF0000AA, 0xFF00AA00, 0xFF00AAAA,
            0xFFAA0000, 0xFFAA00AA, 0xFFFFAA00, 0xFFAAAAAA,
            0xFF555555, 0xFF5555FF, 0xFF55FF55, 0xFF55FFFF,
            0xFFFF5555, 0xFFFF55FF, 0xFFFFFF55, 0xFFFFFFFF
    };
    private static final char[] COLOR_CODES = "0123456789abcdef".toCharArray();
    private static final String[] FORMAT_LABELS = {"B", "I", "U", "S", "R"};
    private static final String[] FORMAT_CODES = {"§l", "§o", "§n", "§m", "§r"};
    /** A five-pixel blank supported by an entirely vanilla client. A normal
     * space advances four pixels; bold makes that same blank advance five.
     * Together those two units can represent every sufficiently large target
     * width without storing unsupported zero-width Unicode glyphs. */
    private static final String FINE_PADDING = "§l §r";
    /** Padding written by the short-lived zero-width-glyph implementation. */
    private static final String LEGACY_FINE_PADDING = "§l\u200c§r";
    private static final char ALIGNMENT_SENTINEL = '\u0001';
    private static final List<GlyphSymbols.Symbol> CHAT_SYMBOLS = createChatSymbols();

    private static Screen lastScreen;
    private static BookEditScreenAccessor selectionScreen;
    private static int rememberedSelectionStart = -1;
    private static int rememberedSelectionEnd = -1;
    private static boolean aligningBook;
    private static boolean bookMenuOpen;
    private static boolean clearBookConfirm;
    private static boolean saveBookDialog;
    private static boolean loadBookDialog;
    private static boolean deleteSaveConfirm;
    private static boolean signSaveDialog;
    private static boolean signLoadDialog;
    private static boolean signDeleteConfirm;
    private static String deleteSaveTarget = "";
    private static EditBox saveNameBox;
    private static EditBox signSaveNameBox;
    private static List<String> loadFiles = List.of();
    private static List<String> signLoadFiles = List.of();
    private static int loadListPage;
    private static int categoryIndex;
    private static int symbolPage;
    private static int chatSymbolPage;
    private static String status = "";

    private ContextualEditorOverlay() { }

    public static void render(GuiGraphics g, Screen screen, int mouseX, int mouseY) {
        if (!active(screen)) return;
        if (screen != lastScreen) {
            lastScreen = screen;
            symbolPage = 0;
            bookMenuOpen = false;
            clearBookConfirm = false;
            saveBookDialog = false;
            loadBookDialog = false;
            deleteSaveConfirm = false;
            signSaveDialog = false;
            signLoadDialog = false;
            signDeleteConfirm = false;
            saveNameBox = null;
            signSaveNameBox = null;
            loadListPage = 0;
            chatSymbolPage = 0;
            selectionScreen = null;
            rememberedSelectionStart = -1;
            rememberedSelectionEnd = -1;
            status = screen instanceof BookEditScreen
                    ? "Редактор книги активен"
                    : screen instanceof AbstractSignEditScreen
                            ? "Редактор таблички активен"
                            : "Панель символов чата активна";
        }

        if (screen instanceof BookEditScreen book) {
            migrateLegacyBookPadding((BookEditScreenAccessor) book);
            rememberBookSelection((BookEditScreenAccessor) book);
        }

        if (screen instanceof ChatScreen chat) {
            drawChatPalette(g, chat, mouseX, mouseY);
            return;
        }

        Layout layout = Layout.of(screen);
        if (screen instanceof AbstractSignEditScreen sign) {
            moveSignDoneButton(sign, layout);
        }
        boolean bookModal = screen instanceof BookEditScreen
                && (clearBookConfirm || bookMenuOpen || saveBookDialog || loadBookDialog || deleteSaveConfirm);
        boolean signModal = screen instanceof AbstractSignEditScreen
                && (signSaveDialog || signLoadDialog || signDeleteConfirm);
        if (!bookModal && !signModal && layout.showSidePanels) {
            if (screen instanceof BookEditScreen) {
                BookEditScreenAccessor book = (BookEditScreenAccessor) screen;
                drawActionPanel(g, layout.leftX, layout.panelY, layout.panelW, "КНИГА", BOOK_LEFT, mouseX, mouseY);
                drawActionPanel(g, layout.rightX, layout.panelY, layout.panelW, "СТРАНИЦА", BOOK_RIGHT, mouseX, mouseY);
            } else {
                SignEditScreenAccessor sign = (SignEditScreenAccessor) screen;
                drawActionPanel(g, layout.leftX, layout.panelY, layout.panelW, "ТАБЛИЧКА · СТРОКА " + (sign.glyphcraft$getLine() + 1), SIGN_LEFT, mouseX, mouseY);
                drawActionPanel(g, layout.rightX, layout.panelY, layout.panelW, "СТРОКИ", SIGN_RIGHT, mouseX, mouseY);
            }
        }

        if (!bookModal && !signModal) drawBottomBar(g, screen, layout, mouseX, mouseY);
        if (screen instanceof BookEditScreen) {
            if (saveBookDialog) drawSaveBookDialog(g, (BookEditScreen) screen, mouseX, mouseY);
            else if (loadBookDialog) drawLoadBookDialog(g, (BookEditScreen) screen, mouseX, mouseY);
            else if (deleteSaveConfirm) drawDeleteSaveConfirm(g, screen, mouseX, mouseY);
            else if (clearBookConfirm) drawClearBookConfirm(g, screen, mouseX, mouseY);
            else if (bookMenuOpen) drawBookMenu(g, screen, mouseX, mouseY);
        } else if (screen instanceof AbstractSignEditScreen sign) {
            if (signSaveDialog) drawSignSaveDialog(g, sign, mouseX, mouseY);
            else if (signLoadDialog) drawSignLoadDialog(g, sign, mouseX, mouseY);
            else if (signDeleteConfirm) drawSignDeleteConfirm(g, sign, mouseX, mouseY);
        }
    }

    public static boolean click(Screen screen, double eventX, double eventY, int button) {
        if (!active(screen) || button != 0) return false;
        Layout layout = Layout.of(screen);
        double mouseX = eventX;
        double mouseY = eventY;

        if (screen instanceof ChatScreen chat) return clickChatPalette(chat, mouseX, mouseY);

        if (screen instanceof BookEditScreen book) {
            if (saveBookDialog) return handleSaveBookDialog(book, mouseX, mouseY);
            if (loadBookDialog) return handleLoadBookDialog(book, mouseX, mouseY);
            if (deleteSaveConfirm) return handleDeleteSaveConfirm(book, mouseX, mouseY);
            if (clearBookConfirm) return handleClearBookConfirm(book, mouseX, mouseY);
            if (bookMenuOpen) return handleBookMenu(book, mouseX, mouseY);
        }
        if (screen instanceof AbstractSignEditScreen sign) {
            if (signSaveDialog) return handleSignSaveDialog(sign, mouseX, mouseY);
            if (signLoadDialog) return handleLoadSignDialog(sign, mouseX, mouseY);
            if (signDeleteConfirm) return handleSignDeleteConfirm(sign, mouseX, mouseY);
        }

        if (layout.showSidePanels && !signModalOpen(screen)) {
            String[] left = screen instanceof BookEditScreen ? BOOK_LEFT : SIGN_LEFT;
            String[] right = screen instanceof BookEditScreen ? BOOK_RIGHT : SIGN_RIGHT;
            int hit = actionAt(mouseX, mouseY, layout.leftX, layout.panelY, layout.panelW, left.length);
            if (hit >= 0) {
                if (screen instanceof BookEditScreen book) runBookAction(book, true, hit);
                else runSignAction((AbstractSignEditScreen) screen, true, hit);
                return true;
            }
            hit = actionAt(mouseX, mouseY, layout.rightX, layout.panelY, layout.panelW, right.length);
            if (hit >= 0) {
                if (screen instanceof BookEditScreen book) runBookAction(book, false, hit);
                else runSignAction((AbstractSignEditScreen) screen, false, hit);
                return true;
            }
        }

        if (mouseY < layout.bottomY || mouseY >= layout.bottomY + layout.bottomH) return false;
        if (inside(mouseX, mouseY, layout.barX, layout.bottomY, 20, 20)) {
            categoryIndex = Math.floorMod(categoryIndex - 1, GlyphSymbols.categories().size());
            symbolPage = 0;
            return true;
        }
        if (inside(mouseX, mouseY, layout.barX + 116, layout.bottomY, 20, 20)) {
            categoryIndex = (categoryIndex + 1) % GlyphSymbols.categories().size();
            symbolPage = 0;
            return true;
        }

        String category = GlyphSymbols.categories().get(categoryIndex);
        List<GlyphSymbols.Symbol> symbols = GlyphSymbols.symbols(category);
        for (int i = 0; i < layout.symbolSlots; i++) {
            int x = layout.symbolStartX + i * 24;
            if (inside(mouseX, mouseY, x, layout.bottomY, 20, 20)) {
                int index = symbolPage * layout.symbolSlots + i;
                if (index < symbols.size()) {
                    insert(screen, symbols.get(index).value());
                    return true;
                }
            }
        }
        if (inside(mouseX, mouseY, layout.pageBackX, layout.bottomY, 20, 20)) {
            scrollSymbols(layout, -1);
            return true;
        }
        if (inside(mouseX, mouseY, layout.pageNextX, layout.bottomY, 20, 20)) {
            scrollSymbols(layout, 1);
            return true;
        }

        if (screen instanceof BookEditScreen && !layout.showSidePanels && inside(mouseX, mouseY,
                layout.bookMenuX, layout.bottomY + 24, layout.bookMenuW, 20)) {
            bookMenuOpen = true;
            clearBookConfirm = false;
            return true;
        }

        if (screen instanceof BookEditScreen book
                && !layout.showSidePanels
                && inside(mouseX, mouseY, layout.alignX, layout.bottomY + 24, layout.alignW, 20)) {
            status = alignSelectedBookColumns((BookEditScreenAccessor) book);
            return true;
        }

        if (screen instanceof BookEditScreen) {
            for (int i = 0; i < COLORS.length; i++) {
                int x = layout.colorStartX + i * 17;
                if (inside(mouseX, mouseY, x, layout.bottomY + 27, 14, 14)) {
                    applyFormatting(screen, "§" + COLOR_CODES[i]);
                    status = "Цвет применён к выделенному тексту";
                    return true;
                }
            }
        }
        int formatX = layout.formatStartX;
        for (int i = 0; i < FORMAT_LABELS.length; i++) {
            int x = formatX + i * 25;
            if (inside(mouseX, mouseY, x, layout.bottomY + 24, 22, 20)) {
                applyFormatting(screen, FORMAT_CODES[i]);
                status = i == 4 ? "Форматирование удалено" : "Стиль применён к выделенному тексту";
                return true;
            }
        }
        // The vanilla Done button used to sit underneath this toolbar. Consume
        // the complete sign toolbar rectangle, including gaps between controls,
        // so a click can never fall through and close the sign editor.
        if (screen instanceof AbstractSignEditScreen
                && mouseY >= layout.bottomY && mouseY < layout.bottomY + layout.bottomH) {
            return true;
        }
        return false;
    }

    /**
     * Handles keyboard input for the small modal windows which are drawn by
     * this overlay.  The name field is intentionally not added to the
     * Minecraft screen's widget list: doing so would make the overlay affect
     * other screens and would also alter vanilla focus navigation.  The
     * container mixin forwards only the events while a book modal is open.
     */
    public static boolean keyPressed(Screen screen, int key, int scanCode, int modifiers) {
        if (!active(screen)) return false;
        if (screen instanceof AbstractSignEditScreen sign && signModalOpen(screen)) {
            if (key == 256) {
                signSaveDialog = false;
                signLoadDialog = false;
                signDeleteConfirm = false;
                signSaveNameBox = null;
                return true;
            }
            if (signSaveDialog && signSaveNameBox != null && key == 257) {
                String name = signSaveNameBox.getValue().trim();
                if (!name.isBlank()) saveNamedSign(sign, name);
                else status = "Введите название таблички";
                return true;
            }
            return signSaveDialog && signSaveNameBox != null
                    && VersionInputBridge.keyPressed(signSaveNameBox, key, scanCode, modifiers);
        }
        if (!(screen instanceof BookEditScreen book)) return false;

        if (saveBookDialog) {
            if (key == 256) { // Escape
                saveBookDialog = false;
                saveNameBox = null;
                status = "Сохранение отменено";
                return true;
            }
            if (saveNameBox != null && key == 257) { // Enter
                String name = saveNameBox.getValue().trim();
                if (!name.isBlank()) saveNamedBook((BookEditScreenAccessor) book, name);
                else status = "Введите название книги";
                return true;
            }
            return saveNameBox != null && VersionInputBridge.keyPressed(saveNameBox, key, scanCode, modifiers);
        }

        if (deleteSaveConfirm) {
            if (key == 256) {
                deleteSaveConfirm = false;
                loadBookDialog = true;
            }
            return true;
        }
        if (loadBookDialog) {
            if (key == 256) loadBookDialog = false;
            return true;
        }
        if (clearBookConfirm) {
            if (key == 256) clearBookConfirm = false;
            return true;
        }
        if (bookMenuOpen) {
            if (key == 256) bookMenuOpen = false;
            return true;
        }
        return false;
    }

    /** Forwards text input to the save-name field while its modal is open. */
    public static boolean charTyped(Screen screen, int codepoint, int modifiers) {
        if (!active(screen)) return false;
        if (screen instanceof AbstractSignEditScreen && signSaveDialog) {
            return signSaveNameBox != null && VersionInputBridge.charTyped(signSaveNameBox, codepoint, modifiers);
        }
        if (!(screen instanceof BookEditScreen) || !saveBookDialog) return false;
        return saveNameBox != null && VersionInputBridge.charTyped(saveNameBox, codepoint, modifiers);
    }

    /** Last-resort guard used by the input mixin. A screen can disappear in
     * the same frame as a click (for example after a server-side sign update);
     * consuming that stale click is safer than letting it terminate the game. */
    public static void recoverFromInputFailure(RuntimeException failure) {
        status = "Клик пропущен — редактор сохранён";
        failure.printStackTrace();
    }

    private static boolean active(Screen screen) {
        Minecraft client = Minecraft.getInstance();
        return client.level != null
                && client.player != null
                && (screen instanceof BookEditScreen || screen instanceof AbstractSignEditScreen
                        || screen instanceof ChatScreen);
    }

    private static boolean signModalOpen(Screen screen) {
        return screen instanceof AbstractSignEditScreen
                && (signSaveDialog || signLoadDialog || signDeleteConfirm);
    }

    /** Converts old Unicode padding into normal vanilla spaces. All pages are
     * migrated when the book is opened, so clean clients never see tofu boxes. */
    private static void migrateLegacyBookPadding(BookEditScreenAccessor book) {
        MultiLineEditBox editor = book.glyphcraft$getPageEditor();
        MultilineTextField field = ((MultiLineEditBoxAccessor) editor).glyphcraft$getTextField();
        MultilineTextFieldAccessor access = (MultilineTextFieldAccessor) field;
        List<String> pages = book.glyphcraft$getPages();
        int currentPage = Math.max(0, Math.min(book.glyphcraft$getCurrentPage(), pages.size() - 1));
        boolean migratedAny = false;

        for (int i = 0; i < pages.size(); i++) {
            String original = i == currentPage ? field.value() : pages.get(i);
            String migrated = migrateLegacyPadding(original, true);
            migrated = normalizeStoredFrameRows(migrated);
            boolean expanded = true;
            if (migrated.length() > 1024) {
                migrated = migrateLegacyPadding(original, false);
                migrated = normalizeStoredFrameRows(migrated);
                expanded = false;
            }
            if (migrated.equals(original)) continue;

            pages.set(i, migrated);
            migratedAny = true;
            if (i == currentPage) {
                int cursor = expanded
                        ? mapLegacyPaddingIndex(original, access.glyphcraft$getCursorRaw())
                        : access.glyphcraft$getCursorRaw();
                int selection = expanded
                        ? mapLegacyPaddingIndex(original, access.glyphcraft$getSelectCursorRaw())
                        : access.glyphcraft$getSelectCursorRaw();
                access.glyphcraft$setValueRaw(migrated);
                access.glyphcraft$setCursorRaw(cursor);
                access.glyphcraft$setSelectCursorRaw(selection);
                access.glyphcraft$onValueChange();
            }
        }
        if (migratedAny) status = "Старые отступы исправлены для обычного Minecraft";
    }

    private static String migrateLegacyPadding(String value, boolean preservePixelWidth) {
        if (value == null || value.isEmpty()) return value == null ? "" : value;
        value = value.replace(LEGACY_FINE_PADDING, FINE_PADDING);
        StringBuilder migrated = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current == '\u200b' || current == '\u200c'
                    || current == '\u200d' || current == '\u2060') {
                // These characters are not guaranteed to exist in vanilla's
                // active font and become missing-glyph squares. They were
                // zero-width alignment markers, so removing them is lossless.
            } else if (current == '\u00a0') {
                // NBSP was used briefly as a five-pixel blank. It is not
                // present in every vanilla font bundle. Preserve its intended
                // five-pixel width with a bold normal space when possible.
                migrated.append(preservePixelWidth ? FINE_PADDING : " ");
            } else {
                migrated.append(current);
            }
        }
        return migrated.toString();
    }

    private static String normalizeStoredFrameRows(String value) {
        if (value == null || value.isEmpty()) return value == null ? "" : value;
        String[] lines = value.split("\\n", -1);
        StringBuilder normalized = new StringBuilder(value.length());
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            normalized.append(isPureFrameLine(line) ? normalizePureFrameLine(line) : line);
            if (i + 1 < lines.length) normalized.append('\n');
        }
        return normalized.toString();
    }

    private static int mapLegacyPaddingIndex(String value, int index) {
        int limit = Math.max(0, Math.min(index, value.length()));
        return migrateLegacyPadding(value.substring(0, limit), true).length();
    }

    private static List<GlyphSymbols.Symbol> createChatSymbols() {
        List<GlyphSymbols.Symbol> symbols = new ArrayList<>();
        for (String category : GlyphSymbols.categories()) symbols.addAll(GlyphSymbols.symbols(category));
        return List.copyOf(symbols);
    }

    /** One flat, category-free symbol strip used by the vanilla chat screen. */
    private static void drawChatPalette(GuiGraphics g, ChatScreen screen, int mouseX, int mouseY) {
        int width = Math.min(540, Math.max(100, screen.width - 20));
        int x = Math.max(4, screen.width - width - 10);
        int buttonSize = 18;
        int spacing = 21;
        int y = Math.max(8, screen.height - 34);
        int slots = Math.max(1, (width - 52) / spacing);
        int previousX = x;
        int nextX = x + width - buttonSize;
        int symbolX = x + 25;
        int start = chatSymbolPage * slots;

        // Compact HUD strip in the lower-right chat area, leaving the rest of
        // the chat input and the world untouched.
        g.fill(x - 2, y - 2, x + width + 2, y + buttonSize + 2, 0x880A0D12);
        drawChatButton(g, previousX, y, buttonSize, "‹", mouseX, mouseY);
        drawChatButton(g, nextX, y, buttonSize, "›", mouseX, mouseY);
        for (int i = 0; i < slots; i++) {
            int index = start + i;
            if (index >= CHAT_SYMBOLS.size()) break;
            int buttonX = symbolX + i * spacing;
            drawChatButton(g, buttonX, y, buttonSize, CHAT_SYMBOLS.get(index).value(), mouseX, mouseY);
        }
    }

    private static boolean clickChatPalette(ChatScreen screen, double mouseX, double mouseY) {
        int width = Math.min(540, Math.max(100, screen.width - 20));
        int x = Math.max(4, screen.width - width - 10);
        int buttonSize = 18;
        int spacing = 21;
        int y = Math.max(8, screen.height - 34);
        int slots = Math.max(1, (width - 52) / spacing);
        int pages = Math.max(1, (CHAT_SYMBOLS.size() + slots - 1) / slots);
        if (inside(mouseX, mouseY, x, y, buttonSize, buttonSize)) {
            chatSymbolPage = Math.floorMod(chatSymbolPage - 1, pages);
            return true;
        }
        if (inside(mouseX, mouseY, x + width - buttonSize, y, buttonSize, buttonSize)) {
            chatSymbolPage = (chatSymbolPage + 1) % pages;
            return true;
        }
        int start = chatSymbolPage * slots;
        for (int i = 0; i < slots; i++) {
            int index = start + i;
            if (index >= CHAT_SYMBOLS.size()) break;
            int buttonX = x + 25 + i * spacing;
            if (inside(mouseX, mouseY, buttonX, y, buttonSize, buttonSize)) {
                ((ChatScreenAccessor) screen).glyphcraft$insertText(CHAT_SYMBOLS.get(index).value(), false);
                return true;
            }
        }
        return inside(mouseX, mouseY, x - 2, y - 2, width + 4, buttonSize + 4);
    }

    private static void drawChatButton(GuiGraphics g, int x, int y, int size, String text, int mouseX, int mouseY) {
        boolean hover = inside(mouseX, mouseY, x, y, size, size);
        beveledButton(g, x, y, size, size, hover, false);
        int baseline = y + Math.max(4, (size - font().lineHeight) / 2);
        drawCentered(g, text, x + size / 2, baseline, hover ? TEXT : 0xFFD4E1F2);
    }

    private static void drawActionPanel(GuiGraphics g, int x, int y, int w, String title, String[] actions, int mouseX, int mouseY) {
        g.drawString(font(), trimToWidth(title, w), x + 2, y + 3, ACCENT, false);
        for (int i = 0; i < actions.length; i++) {
            int by = y + 16 + i * 25;
            boolean hover = inside(mouseX, mouseY, x, by, w, 21);
            beveledButton(g, x, by, w, 21, hover, false);
            drawCentered(g, actions[i], x + w / 2, by + 8, hover ? TEXT : 0xFFD4E1F2);
        }
    }

    private static void drawBottomBar(GuiGraphics g, Screen screen, Layout l, int mouseX, int mouseY) {
        String category = GlyphSymbols.categories().get(categoryIndex);
        drawMiniButton(g, l.barX, l.bottomY, 20, 20, "‹", mouseX, mouseY);
        beveledButton(g, l.barX + 24, l.bottomY, 88, 20, inside(mouseX, mouseY, l.barX + 24, l.bottomY, 88, 20), true);
        drawCentered(g, category, l.barX + 68, l.bottomY + 6, TEXT);
        drawMiniButton(g, l.barX + 116, l.bottomY, 20, 20, "›", mouseX, mouseY);

        List<GlyphSymbols.Symbol> symbols = GlyphSymbols.symbols(category);
        int start = symbolPage * l.symbolSlots;
        for (int i = 0; i < l.symbolSlots; i++) {
            if (start + i >= symbols.size()) continue;
            int x = l.symbolStartX + i * 24;
            boolean hover = inside(mouseX, mouseY, x, l.bottomY, 20, 20);
            beveledButton(g, x, l.bottomY, 20, 20, hover, false);
            drawCentered(g, symbols.get(start + i).value(), x + 10, l.bottomY + 6, TEXT);
        }
        drawMiniButton(g, l.pageBackX, l.bottomY, 20, 20, "◀", mouseX, mouseY);
        drawMiniButton(g, l.pageNextX, l.bottomY, 20, 20, "▶", mouseX, mouseY);

        if (screen instanceof BookEditScreen) {
            for (int i = 0; i < COLORS.length; i++) {
                int x = l.colorStartX + i * 17;
                colorCell(g, x, l.bottomY + 27, 14, COLORS[i], inside(mouseX, mouseY, x, l.bottomY + 27, 14, 14));
            }
        }
        if (screen instanceof BookEditScreen && !l.showSidePanels) {
            drawMiniButton(g, l.bookMenuX, l.bottomY + 24, l.bookMenuW, 20, "Кн", mouseX, mouseY);
            drawMiniButton(g, l.alignX, l.bottomY + 24, l.alignW, 20, "С", mouseX, mouseY);
        }
        int formatX = l.formatStartX;
        for (int i = 0; i < FORMAT_LABELS.length; i++) {
            int x = formatX + i * 25;
            drawMiniButton(g, x, l.bottomY + 24, 22, 20, FORMAT_LABELS[i], mouseX, mouseY);
        }
        if (screen instanceof BookEditScreen) {
            int statusX = l.colorStartX + COLORS.length * 17 + 8;
            int statusEnd = !l.showSidePanels ? l.bookMenuX : formatX;
            if (statusEnd - statusX > 30) {
                String shown = trimToWidth(status, statusEnd - statusX - 8);
                g.drawString(font(), shown, statusX, l.bottomY + 31, MUTED, false);
            }
        } else if (screen instanceof AbstractSignEditScreen) {
            String shown = trimToWidth(status, Math.max(40, l.barW - 12));
            g.drawString(font(), shown, l.barX + 6, l.bottomY + 31, MUTED, false);
        }
    }

    /** Keep the vanilla confirmation button in its own row above our palette.
     * AbstractSignEditScreen currently owns a single real Button (Done); using
     * the widget list also keeps this compatible with standing and hanging
     * sign editor subclasses. */
    private static void moveSignDoneButton(AbstractSignEditScreen screen, Layout layout) {
        int targetY = Math.max(6, layout.bottomY - 30);
        for (var child : screen.children()) {
            if (child instanceof Button button) {
                button.setX((screen.width - button.getWidth()) / 2);
                button.setY(targetY);
            }
        }
    }

    private static void runBookAction(BookEditScreen screen, boolean left, int index) {
        BookEditScreenAccessor book = (BookEditScreenAccessor) screen;
        MultiLineEditBox editor = book.glyphcraft$getPageEditor();
        List<String> pages = book.glyphcraft$getPages();
        int current = book.glyphcraft$getCurrentPage();
        syncCurrent(book);
        try {
            if (left) {
                switch (index) {
                    case 0 -> openLoadBookDialog((BookEditScreen) screen);
                    case 1 -> openSaveBookDialog((BookEditScreen) screen);
                    case 2 -> requestClearBook(book);
                    case 3 -> { clipboard(String.join("\n--- GLYPHCRAFT PAGE ---\n", pages)); status = "Книга скопирована"; }
                    case 4 -> { replaceBookFromText(book, clipboard()); status = "Книга вставлена"; }
                    default -> { }
                }
            } else {
                switch (index) {
                    case 0 -> { editor.setValue(""); status = "Страница очищена"; }
                    case 1 -> { clipboard(editor.getValue()); status = "Страница скопирована"; }
                    case 2 -> {
                        if (pages.size() < 100) {
                            pages.add(Math.min(current + 1, pages.size()), "");
                            book.glyphcraft$setCurrentPage(current + 1);
                            refresh(book);
                            status = "Страница добавлена после текущей";
                        }
                    }
                    case 3 -> {
                        if (pages.size() > 1) {
                            pages.remove(current);
                            book.glyphcraft$setCurrentPage(Math.min(current, pages.size() - 1));
                            refresh(book);
                            status = "Страница удалена";
                        } else {
                            editor.setValue("");
                            status = "Оставлена одна пустая страница";
                        }
                    }
                    case 4 -> status = alignSelectedBookColumns(book);
                    default -> { }
                }
            }
        } catch (RuntimeException e) {
            status = "Операция не выполнена";
        }
    }

    private static void requestClearBook(BookEditScreenAccessor book) {
        bookMenuOpen = false;
        clearBookConfirm = true;
    }

    private static void clearBook(BookEditScreenAccessor book) {
        syncCurrent(book);
        List<String> pages = book.glyphcraft$getPages();
        pages.clear();
        pages.add("");
        book.glyphcraft$setCurrentPage(0);
        refresh(book);
        status = "Книга полностью очищена";
    }

    private static boolean handleClearBookConfirm(BookEditScreen screen, double mouseX, double mouseY) {
        int width = 300;
        int height = 86;
        int x = (screen.width - width) / 2;
        int y = (screen.height - height) / 2;
        if (inside(mouseX, mouseY, x + 24, y + 52, 108, 22)) {
            clearBook((BookEditScreenAccessor) screen);
            clearBookConfirm = false;
            return true;
        }
        if (inside(mouseX, mouseY, x + width - 132, y + 52, 108, 22)) {
            clearBookConfirm = false;
            status = "Очистка отменена";
            return true;
        }
        return true;
    }

    private static void drawClearBookConfirm(GuiGraphics g, Screen screen, int mouseX, int mouseY) {
        int width = 300;
        int height = 86;
        int x = (screen.width - width) / 2;
        int y = (screen.height - height) / 2;
        g.fill(0, 0, screen.width, screen.height, 0x66000000);
        g.fill(x, y, x + width, y + height, 0xFF151B28);
        border(g, x, y, width, height, 0xFF8AA2C7);
        drawCentered(g, "Вы уверены очистить всю книгу?", x + width / 2, y + 14, TEXT);
        drawMiniButton(g, x + 24, y + 52, 108, 22, "Да", mouseX, mouseY);
        drawMiniButton(g, x + width - 132, y + 52, 108, 22, "Нет", mouseX, mouseY);
    }

    private static void drawBookMenu(GuiGraphics g, Screen screen, int mouseX, int mouseY) {
        int width = 300;
        int height = 136;
        int x = (screen.width - width) / 2;
        int y = Math.max(8, screen.height - 44 - height - 12);
        g.fill(0, 0, screen.width, screen.height, 0x55000000);
        g.fill(x, y, x + width, y + height, 0xFF151B28);
        border(g, x, y, width, height, 0xFF8AA2C7);
        drawCentered(g, "Управление книгой", x + width / 2, y + 7, TEXT);
        int buttonWidth = 140;
        for (int i = 0; i < BOOK_MENU_LABELS.length; i++) {
            int col = i % 2;
            int row = i / 2;
            int bx = x + 7 + col * (buttonWidth + 6);
            int by = y + 25 + row * 26;
            boolean hover = inside(mouseX, mouseY, bx, by, buttonWidth, 22);
            beveledButton(g, bx, by, buttonWidth, 22, hover, false);
            drawCentered(g, BOOK_MENU_LABELS[i], bx + buttonWidth / 2, by + 7, hover ? TEXT : 0xFFD4E1F2);
        }
    }

    private static boolean handleBookMenu(BookEditScreen screen, double mouseX, double mouseY) {
        int width = 300;
        int height = 136;
        int x = (screen.width - width) / 2;
        int y = Math.max(8, screen.height - 44 - height - 12);
        if (!inside(mouseX, mouseY, x, y, width, height)) {
            bookMenuOpen = false;
            return true;
        }
        int buttonWidth = 140;
        for (int i = 0; i < BOOK_MENU_LABELS.length; i++) {
            int col = i % 2;
            int row = i / 2;
            int bx = x + 7 + col * (buttonWidth + 6);
            int by = y + 25 + row * 26;
            if (!inside(mouseX, mouseY, bx, by, buttonWidth, 22)) continue;
            switch (i) {
                case 0 -> runBookAction(screen, true, 1);
                case 1 -> runBookAction(screen, true, 0);
                case 2 -> runBookAction(screen, false, 0);
                case 3 -> requestClearBook((BookEditScreenAccessor) screen);
                case 4 -> runBookAction(screen, false, 2);
                case 5 -> runBookAction(screen, false, 3);
                case 6 -> status = alignSelectedBookColumns((BookEditScreenAccessor) screen);
                case 7 -> { bookMenuOpen = false; return true; }
                default -> { }
            }
            if (!clearBookConfirm) bookMenuOpen = false;
            return true;
        }
        return true;
    }

    private static void openSaveBookDialog(BookEditScreen screen) {
        bookMenuOpen = false;
        loadBookDialog = false;
        deleteSaveConfirm = false;
        saveBookDialog = true;
        int x = (screen.width - 300) / 2 + 20;
        int y = (screen.height - 110) / 2 + 34;
        saveNameBox = new EditBox(Minecraft.getInstance().font, x, y, 260, 20, Component.literal("Название книги"));
        saveNameBox.setMaxLength(64);
        saveNameBox.setBordered(true);
        saveNameBox.setValue("");
        saveNameBox.setFocused(true);
    }

    private static void openLoadBookDialog(BookEditScreen screen) {
        bookMenuOpen = false;
        saveBookDialog = false;
        deleteSaveConfirm = false;
        loadFiles = bookFiles();
        loadListPage = 0;
        loadBookDialog = true;
    }

    private static List<String> bookFiles() {
        try {
            List<String> files = new ArrayList<>();
            for (String file : GlyphBookStorage.list()) {
                if (!file.equals("last_sign") && !file.equals("template") && !file.equals("last_book")) files.add(file);
            }
            return files;
        } catch (IOException e) {
            status = "Не удалось прочитать сохранения";
            return List.of();
        }
    }

    private static boolean handleSaveBookDialog(BookEditScreen screen, double mouseX, double mouseY) {
        int width = 300;
        int height = 110;
        int x = (screen.width - width) / 2;
        int y = (screen.height - height) / 2;
        if (inside(mouseX, mouseY, x + 20, y + 34, 260, 20)) {
            if (saveNameBox != null) saveNameBox.setFocused(true);
            return true;
        }
        if (inside(mouseX, mouseY, x + 20, y + 74, 120, 22)) {
            String name = saveNameBox == null ? "" : saveNameBox.getValue().trim();
            if (name.isBlank()) {
                status = "Введите название книги";
                return true;
            }
            saveNamedBook((BookEditScreenAccessor) screen, name);
            return true;
        }
        if (inside(mouseX, mouseY, x + 160, y + 74, 120, 22)) {
            saveBookDialog = false;
            saveNameBox = null;
            status = "Сохранение отменено";
            return true;
        }
        return true;
    }

    private static void drawSaveBookDialog(GuiGraphics g, BookEditScreen screen, int mouseX, int mouseY) {
        int width = 300;
        int height = 110;
        int x = (screen.width - width) / 2;
        int y = (screen.height - height) / 2;
        g.fill(0, 0, screen.width, screen.height, 0x66000000);
        g.fill(x, y, x + width, y + height, 0xFF151B28);
        border(g, x, y, width, height, 0xFF8AA2C7);
        drawCentered(g, "Сохранить книгу", x + width / 2, y + 8, TEXT);
        g.drawString(font(), "Название книги", x + 20, y + 26, MUTED, false);
        if (saveNameBox != null) saveNameBox.render(g, mouseX, mouseY, 0.0F);
        drawMiniButton(g, x + 20, y + 74, 120, 22, "Сохранить", mouseX, mouseY);
        drawMiniButton(g, x + 160, y + 74, 120, 22, "Отмена", mouseX, mouseY);
    }

    private static boolean handleLoadBookDialog(BookEditScreen screen, double mouseX, double mouseY) {
        int width = 400;
        int rows = loadRowsPerPage(screen);
        int start = loadListPage * rows;
        int visible = Math.min(rows, Math.max(0, loadFiles.size() - start));
        int height = 58 + rows * 26;
        int x = (screen.width - width) / 2;
        int y = Math.max(8, (screen.height - height) / 2);
        if (!inside(mouseX, mouseY, x, y, width, height)) {
            loadBookDialog = false;
            return true;
        }
        int rowY = y + 27;
        for (int i = 0; i < visible; i++) {
            int by = rowY + i * 26;
            int loadX = x + width - 125;
            int deleteX = x + width - 37;
            if (inside(mouseX, mouseY, loadX, by, 82, 22)) {
                loadNamedBook((BookEditScreenAccessor) screen, loadFiles.get(start + i));
                loadBookDialog = false;
                return true;
            }
            if (inside(mouseX, mouseY, deleteX, by, 22, 22)) {
                deleteSaveTarget = loadFiles.get(start + i);
                deleteSaveConfirm = true;
                loadBookDialog = false;
                return true;
            }
        }
        int pages = loadListPages(rows);
        if (inside(mouseX, mouseY, x + 20, y + height - 25, 22, 21)) {
            loadListPage = Math.floorMod(loadListPage - 1, pages);
            return true;
        }
        if (inside(mouseX, mouseY, x + 48, y + height - 25, 22, 21)) {
            loadListPage = (loadListPage + 1) % pages;
            return true;
        }
        if (inside(mouseX, mouseY, x + width - 120, y + height - 25, 100, 21)) {
            loadBookDialog = false;
            return true;
        }
        return true;
    }

    private static void drawLoadBookDialog(GuiGraphics g, BookEditScreen screen, int mouseX, int mouseY) {
        int width = 400;
        int rows = loadRowsPerPage(screen);
        int start = loadListPage * rows;
        int visible = Math.min(rows, Math.max(0, loadFiles.size() - start));
        int height = 58 + rows * 26;
        int x = (screen.width - width) / 2;
        int y = Math.max(8, (screen.height - height) / 2);
        g.fill(0, 0, screen.width, screen.height, 0x66000000);
        g.fill(x, y, x + width, y + height, 0xFF151B28);
        border(g, x, y, width, height, 0xFF8AA2C7);
        drawCentered(g, "Загрузить книгу", x + width / 2, y + 8, TEXT);
        if (loadFiles.isEmpty()) {
            drawCentered(g, "Сохранений пока нет", x + width / 2, y + 33, MUTED);
        } else {
            for (int i = 0; i < visible; i++) {
                int by = y + 27 + i * 26;
                int loadX = x + width - 125;
                int deleteX = x + width - 37;

                // The title is a dark, bounded input-like field. Its fixed
                // right edge leaves a real gap before the action buttons, so
                // a long name can never run underneath them.
                int fieldWidth = loadX - x - 16;
                drawBookNameField(g, x + 10, by, fieldWidth, 22,
                        inside(mouseX, mouseY, x + 10, by, fieldWidth, 22));
                g.drawString(font(), trimToWidth(loadFiles.get(start + i), loadX - x - 34),
                        x + 18, by + 7, TEXT, false);
                drawMiniButton(g, loadX, by, 82, 22, "Загрузить", mouseX, mouseY);
                drawMiniButton(g, deleteX, by, 22, 22, "×", mouseX, mouseY);
            }
        }
        int pages = loadListPages(rows);
        drawMiniButton(g, x + 20, y + height - 25, 22, 21, "‹", mouseX, mouseY);
        drawMiniButton(g, x + 48, y + height - 25, 22, 21, "›", mouseX, mouseY);
        g.drawString(font(), (loadListPage + 1) + " / " + pages, x + 78, y + height - 18, MUTED, false);
        drawMiniButton(g, x + width - 120, y + height - 25, 100, 21, "Закрыть", mouseX, mouseY);
    }

    private static void drawBookNameField(GuiGraphics g, int x, int y, int width, int height, boolean hover) {
        g.fill(x, y, x + width, y + height, 0xFF111111);
        g.fill(x + 1, y + 1, x + width - 1, y + height - 1, hover ? 0xFF252B34 : 0xFF1B2028);
        g.fill(x + 1, y + 1, x + width - 1, y + 2, 0xFFB8C4D8);
        g.fill(x + 1, y + 1, x + 2, y + height - 1, 0xFFA8B6CC);
        g.fill(x + 2, y + height - 2, x + width - 1, y + height - 1, 0xFF050505);
        g.fill(x + width - 2, y + 2, x + width - 1, y + height - 1, 0xFF050505);
    }

    private static int loadRowsPerPage(Screen screen) {
        return Math.max(1, Math.min(6, (screen.height - 92) / 26));
    }

    private static int loadListPages(int rows) {
        return Math.max(1, (loadFiles.size() + rows - 1) / rows);
    }

    private static boolean handleDeleteSaveConfirm(BookEditScreen screen, double mouseX, double mouseY) {
        int width = 320;
        int height = 92;
        int x = (screen.width - width) / 2;
        int y = (screen.height - height) / 2;
        if (inside(mouseX, mouseY, x + 24, y + 58, 112, 22)) {
            try {
                GlyphBookStorage.delete(deleteSaveTarget);
                status = "Сохранение удалено";
            } catch (IOException e) {
                status = "Не удалось удалить сохранение";
            }
            loadFiles = bookFiles();
            loadListPage = Math.min(loadListPage, loadListPages(loadRowsPerPage(screen)) - 1);
            deleteSaveConfirm = false;
            loadBookDialog = true;
            return true;
        }
        if (inside(mouseX, mouseY, x + width - 136, y + 58, 112, 22)) {
            deleteSaveConfirm = false;
            loadBookDialog = true;
            return true;
        }
        return true;
    }

    private static void drawDeleteSaveConfirm(GuiGraphics g, Screen screen, int mouseX, int mouseY) {
        int width = 320;
        int height = 92;
        int x = (screen.width - width) / 2;
        int y = (screen.height - height) / 2;
        g.fill(0, 0, screen.width, screen.height, 0x66000000);
        g.fill(x, y, x + width, y + height, 0xFF151B28);
        border(g, x, y, width, height, 0xFF8AA2C7);
        drawCentered(g, "Удалить сохранение?", x + width / 2, y + 9, TEXT);
        drawCentered(g, trimToWidth(deleteSaveTarget, width - 24), x + width / 2, y + 28, MUTED);
        drawMiniButton(g, x + 24, y + 58, 112, 22, "Да", mouseX, mouseY);
        drawMiniButton(g, x + width - 136, y + 58, 112, 22, "Нет", mouseX, mouseY);
    }

    private static void saveNamedBook(BookEditScreenAccessor screen, String name) {
        try {
            GlyphBook saved = snapshotBook(screen);
            saved.name = name;
            GlyphBookStorage.save(name, saved);
            saveBookDialog = false;
            saveNameBox = null;
            status = "Книга сохранена: " + name + ".json";
        } catch (IOException e) {
            status = "Ошибка сохранения книги";
        }
    }

    private static void loadNamedBook(BookEditScreenAccessor screen, String name) {
        try {
            GlyphBook loaded = GlyphBookStorage.load(name);
            List<String> pages = screen.glyphcraft$getPages();
            pages.clear();
            for (GlyphBook.Page page : loaded.pages) pages.add(page.text == null ? "" : page.text);
            if (pages.isEmpty()) pages.add("");
            screen.glyphcraft$setCurrentPage(0);
            refresh(screen);
            status = "Книга загружена: " + name + ".json";
        } catch (IOException e) {
            status = "Не удалось загрузить книгу";
        }
    }

    private static GlyphBook snapshotBook(BookEditScreenAccessor screen) {
        syncCurrent(screen);
        GlyphBook saved = new GlyphBook();
        saved.name = "GlyphCraft Book";
        saved.pages.clear();
        for (int i = 0; i < screen.glyphcraft$getPages().size(); i++) {
            GlyphBook.Page page = new GlyphBook.Page();
            page.title = "Страница " + (i + 1);
            page.text = screen.glyphcraft$getPages().get(i);
            saved.pages.add(page);
        }
        return saved;
    }

    private static void runSignAction(AbstractSignEditScreen screen, boolean left, int index) {
        SignEditScreenAccessor sign = (SignEditScreenAccessor) screen;
        try {
            if (left) {
                switch (index) {
                    case 0 -> openLoadSignDialog((AbstractSignEditScreen) screen);
                    case 1 -> openSaveSignDialog((AbstractSignEditScreen) screen);
                    case 2 -> { clipboard(String.join("\n", sign.glyphcraft$getMessages())); status = "Табличка скопирована"; }
                    default -> { }
                }
            } else {
                switch (index) {
                    case 0 -> { setSignLines(sign, clipboard().split("\\R", -1)); status = "Табличка вставлена"; }
                    case 1 -> { setSignLines(sign, new String[]{"", "", "", ""}); status = "Табличка очищена"; }
                    default -> { }
                }
            }
        } catch (RuntimeException e) {
            status = "Операция не выполнена";
        }
    }

    private static void insert(Screen screen, String value) {
        if (screen instanceof BookEditScreen bookScreen) {
            BookEditScreenAccessor book = (BookEditScreenAccessor) bookScreen;
            MultiLineEditBox editor = book.glyphcraft$getPageEditor();
            MultilineTextField field = ((MultiLineEditBoxAccessor) editor).glyphcraft$getTextField();
            field.insertText(value);
        } else if (screen instanceof AbstractSignEditScreen signScreen) {
            ((SignEditScreenAccessor) signScreen).glyphcraft$getSignField().insertText(value);
        }
    }

    private static String alignSelectedBookColumns(BookEditScreenAccessor book) {
        MultilineTextField field = ((MultiLineEditBoxAccessor) book.glyphcraft$getPageEditor()).glyphcraft$getTextField();
        MultilineTextFieldAccessor access = (MultilineTextFieldAccessor) field;
        int begin = Math.min(access.glyphcraft$getCursorRaw(), access.glyphcraft$getSelectCursorRaw());
        int end = Math.max(access.glyphcraft$getCursorRaw(), access.glyphcraft$getSelectCursorRaw());
        // A click on the overlay can move the vanilla caret before this
        // handler executes. Keep using the last non-empty drag selection.
        if (begin == end && selectionScreen == book
                && rememberedSelectionStart >= 0 && rememberedSelectionEnd > rememberedSelectionStart) {
            begin = rememberedSelectionStart;
            end = rememberedSelectionEnd;
        }
        if (begin == end) return "Сначала выделите область рамки";

        String value = field.value();
        int regionStart = value.lastIndexOf('\n', Math.max(0, begin - 1)) + 1;
        int regionEnd = value.indexOf('\n', end);
        if (regionEnd < 0) regionEnd = value.length();
        AlignmentResult result = alignBookColumns(book, regionStart, regionEnd);
        if (result.frameCount() < 2) return "В выделении не найдено двух рамок";
        return result.changed() ? "Выделенные столбцы выровнены" : "Столбцы уже выровнены";
    }

    private static void rememberBookSelection(BookEditScreenAccessor book) {
        MultilineTextField field = ((MultiLineEditBoxAccessor) book.glyphcraft$getPageEditor()).glyphcraft$getTextField();
        MultilineTextFieldAccessor access = (MultilineTextFieldAccessor) field;
        int begin = Math.min(access.glyphcraft$getCursorRaw(), access.glyphcraft$getSelectCursorRaw());
        int end = Math.max(access.glyphcraft$getCursorRaw(), access.glyphcraft$getSelectCursorRaw());
        if (end > begin) {
            selectionScreen = book;
            rememberedSelectionStart = begin;
            rememberedSelectionEnd = end;
        }
    }

    /** Align only the right-hand frame runs inside one selected book region.
     * Padding is inserted before each border, so all other page text remains
     * byte-for-byte unchanged. */
    private static AlignmentResult alignBookColumns(BookEditScreenAccessor book, int regionStart, int regionEnd) {
        if (aligningBook) return new AlignmentResult(false, 0);
        MultilineTextField field = ((MultiLineEditBoxAccessor) book.glyphcraft$getPageEditor()).glyphcraft$getTextField();
        String value = field.value();
        String[] lines = value.split("\\n", -1);
        List<FrameLine> frameLines = new ArrayList<>();
        int offset = 0;
        for (String line : lines) {
            boolean pureFrame = isPureFrameLine(line);
            String prepared = pureFrame ? normalizePureFrameLine(line) : line;
            FrameBounds bounds = trailingFrameBounds(prepared);
            int lineEnd = offset + line.length();
            if (bounds != null && lineEnd >= regionStart && offset <= regionEnd) {
                frameLines.add(new FrameLine(offset, prepared, bounds.start(), bounds.end(),
                        font().width(prepared.substring(0, bounds.end())), pureFrame));
            }
            offset += line.length() + 1;
        }
        if (frameLines.size() < 2) return new AlignmentResult(false, frameLines.size());

        int maximumWidth = frameLines.stream().mapToInt(FrameLine::width).max().orElse(0);
        int targetWidth = chooseColumnTarget(frameLines, maximumWidth);
        StringBuilder next = new StringBuilder(value.length() + 32);
        List<Insertion> insertions = new ArrayList<>();
        offset = 0;
        for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
            String line = lines[lineIndex];
            int lineOffset = offset;
            FrameLine frame = frameLines.stream()
                    .filter(candidate -> candidate.offset() == lineOffset)
                    .findFirst().orElse(null);
            if (frame != null) {
                if (frame.pure()) {
                    String adjusted = extendPureFrameLine(frame.raw(), frame, targetWidth);
                    next.append(adjusted);
                    if (adjusted.length() > line.length()) {
                        insertions.add(new Insertion(frame.offset() + frame.frameEnd() - 1,
                                adjusted.length() - line.length()));
                    }
                } else {
                    next.append(line, 0, frame.insertAt());
                    String padding = bestPadding(frame, targetWidth);
                    if (!padding.isEmpty()) {
                        next.append(padding);
                        insertions.add(new Insertion(frame.offset() + frame.insertAt(), padding.length()));
                    }
                    next.append(line, frame.insertAt(), line.length());
                }
            } else {
                next.append(line);
            }
            if (lineIndex + 1 < lines.length) next.append('\n');
            offset += line.length() + 1;
        }

        String replacement = next.toString();
        if (replacement.equals(value)) return new AlignmentResult(false, frameLines.size());
        if (replacement.length() > 1024) {
            status = "На странице недостаточно места для выравнивания";
            return new AlignmentResult(false, frameLines.size());
        }

        MultilineTextFieldAccessor access = (MultilineTextFieldAccessor) field;
        int cursor = mapIndex(access.glyphcraft$getCursorRaw(), insertions);
        int selectCursor = mapIndex(access.glyphcraft$getSelectCursorRaw(), insertions);
        aligningBook = true;
        try {
            access.glyphcraft$setValueRaw(replacement);
            access.glyphcraft$setCursorRaw(cursor);
            access.glyphcraft$setSelectCursorRaw(selectCursor);
            access.glyphcraft$onValueChange();
        } finally {
            aligningBook = false;
        }
        return new AlignmentResult(true, frameLines.size());
    }

    private static FrameBounds trailingFrameBounds(String line) {
        int contentEnd = line.length();
        boolean changed;
        do {
            changed = false;
            while (contentEnd >= 2 && isFormattingCodeAt(line, contentEnd - 2)) {
                contentEnd -= 2;
                changed = true;
            }
            while (contentEnd > 0 && isAlignmentPadding(line.charAt(contentEnd - 1))) {
                contentEnd--;
                changed = true;
            }
        } while (changed);

        int start = contentEnd;
        while (start > 0) {
            if (start >= 2 && isFormattingCodeAt(line, start - 2)) {
                start -= 2;
            } else if (isFrameCharacter(line.charAt(start - 1))) {
                start--;
            } else {
                break;
            }
        }
        if (start != contentEnd) return new FrameBounds(start, contentEnd);

        // A caret, a trailing marker, or a formatting suffix can follow the
        // right border in the editor. Find the last actual frame glyph instead
        // of rejecting the whole line in that case.
        int lastFrame = -1;
        for (int i = line.length() - 1; i >= 0; i--) {
            if (isFormattingCodeAt(line, i)) {
                i--;
                continue;
            }
            if (isFrameCharacter(line.charAt(i))) {
                lastFrame = i;
                break;
            }
        }
        if (lastFrame < 0) return null;
        int frameStart = lastFrame;
        while (frameStart > 0) {
            if (frameStart >= 2 && isFormattingCodeAt(line, frameStart - 2)) {
                frameStart -= 2;
            } else if (isFrameCharacter(line.charAt(frameStart - 1))) {
                frameStart--;
            } else {
                break;
            }
        }
        return new FrameBounds(frameStart, lastFrame + 1);
    }

    /** A border row contains only frame glyphs, formatting, and alignment blanks.
     * Such rows must never be padded with spaces: a space would create a visible
     * break in the horizontal border on a vanilla client. A side-only row such
     * as "║      ║" is deliberately not considered a border row: its spaces are
     * real content and must remain spaces. */
    private static boolean isPureFrameLine(String line) {
        boolean hasFrame = false;
        boolean hasHorizontal = false;
        for (int i = 0; i < line.length(); i++) {
            if (isFormattingCodeAt(line, i)) {
                i++;
                continue;
            }
            char value = line.charAt(i);
            if (isAlignmentPadding(value)) continue;
            if (!isFrameCharacter(value)) return false;
            hasFrame = true;
            if (isHorizontalFrameCharacter(value)) hasHorizontal = true;
        }
        return hasFrame && hasHorizontal;
    }

    private static String normalizePureFrameLine(String line) {
        int first = -1;
        int last = -1;
        char horizontal = '\u2500';
        for (int i = 0; i < line.length(); i++) {
            if (isFormattingCodeAt(line, i)) {
                i++;
                continue;
            }
            char value = line.charAt(i);
            if (!isFrameCharacter(value)) continue;
            if (first < 0) first = i;
            last = i;
            if (isHorizontalFrameCharacter(value)) horizontal = value;
        }
        if (first < 0 || last <= first) return line;
        StringBuilder result = new StringBuilder(line);
        for (int i = first + 1; i < last; i++) {
            if (isFormattingCodeAt(line, i)) {
                i++;
                continue;
            }
            if (isAlignmentPadding(line.charAt(i))) result.setCharAt(i, horizontal);
        }
        return result.toString();
    }

    private static boolean isHorizontalFrameCharacter(char value) {
        return "─━═╌╍┄┅┈┉╌╍╴╶╸╺-=_".indexOf(value) >= 0;
    }

    private static String extendPureFrameLine(String line, FrameLine frame, int targetWidth) {
        int corner = frame.frameEnd() - 1;
        while (corner >= 0 && isFormattingCodeAt(line, corner - 1)) corner -= 2;
        if (corner < 0 || !isFrameCharacter(line.charAt(corner))) return line;
        char horizontal = '\u2500';
        for (int i = frame.insertAt(); i < frame.frameEnd(); i++) {
            char value = line.charAt(i);
            if (isHorizontalFrameCharacter(value)) {
                horizontal = value;
                break;
            }
        }
        String best = line;
        int bestDifference = Math.abs(font().width(line.substring(0, frame.frameEnd())) - targetWidth);
        for (int count = 1; count <= 32; count++) {
            String candidate = line.substring(0, corner) + String.valueOf(horizontal).repeat(count) + line.substring(corner);
            int difference = Math.abs(font().width(candidate.substring(0, frame.frameEnd() + count)) - targetWidth);
            if (difference < bestDifference) {
                best = candidate;
                bestDifference = difference;
            }
        }
        return best;
    }

    private static boolean isAlignmentPadding(char value) {
        return value == ' ' || value == '\u00a0' || value == '\u200b' || value == '\u200c'
                || value == '\u200d' || value == '\u2060';
    }

    private static boolean isFrameCharacter(char value) {
        return (value >= 0x2500 && value <= 0x257F)
                || (value >= 0x2580 && value <= 0x259F)
                || "|+-=_/\\<>".indexOf(value) >= 0;
    }

    private static int chooseColumnTarget(List<FrameLine> lines, int maximumWidth) {
        int bestTarget = maximumWidth;
        int bestScore = Integer.MAX_VALUE;
        for (int target = maximumWidth; target <= maximumWidth + 24; target++) {
            int score = 0;
            for (FrameLine line : lines) score += alignmentDifference(line, target);
            if (score < bestScore) {
                bestScore = score;
                bestTarget = target;
                if (score == 0) break;
            }
        }
        return bestTarget;
    }

    private static int alignmentDifference(FrameLine line, int targetWidth) {
        if (!line.pure()) return bestPaddingDifference(line, targetWidth);
        String adjusted = extendPureFrameLine(line.raw(), line, targetWidth);
        int added = Math.max(0, adjusted.length() - line.raw().length());
        int measuredEnd = Math.min(adjusted.length(), line.frameEnd() + added);
        return Math.abs(font().width(adjusted.substring(0, measuredEnd)) - targetWidth);
    }

    private static String bestPadding(FrameLine line, int targetWidth) {
        String bestPadding = "";
        int bestDifference = Math.abs(line.width() - targetWidth);
        int bestWidth = line.width();
        int maxPixels = Math.max(8, targetWidth - line.width() + 8);
        // A normal space advances 4 px and FINE_PADDING is the same vanilla
        // space in bold at 5 px. Both are blank with or without GlyphCraft.
        String[] units = {" ", FINE_PADDING};
        int[] widths = new int[units.length];
        for (int i = 0; i < units.length; i++) widths[i] = font().width(units[i]);
        String[] combinations = new String[maxPixels + 1];
        combinations[0] = "";
        for (int pixels = 0; pixels <= maxPixels; pixels++) {
            if (combinations[pixels] == null) continue;
            for (int unit = 0; unit < units.length; unit++) {
                if (widths[unit] <= 0 || pixels + widths[unit] > maxPixels) continue;
                String candidate = combinations[pixels] + units[unit];
                if (combinations[pixels + widths[unit]] == null
                        || candidate.length() < combinations[pixels + widths[unit]].length()) {
                    combinations[pixels + widths[unit]] = candidate;
                }
            }
        }
        for (String padding : combinations) {
            if (padding == null || padding.isEmpty()) continue;
            String formattedPadding = formatAlignmentPadding(padding, line.prefix());
            int width = font().width(line.prefix() + formattedPadding + line.suffix());
            int difference = Math.abs(width - targetWidth);
            if (difference < bestDifference
                    || (difference == bestDifference && width >= targetWidth && bestWidth < targetWidth)) {
                bestDifference = difference;
                bestPadding = formattedPadding;
                bestWidth = width;
            }
        }
        return bestPadding;
    }

    private static String formatAlignmentPadding(String padding, String prefix) {
        if (padding == null || padding.isEmpty()) return "";
        // Reset before the blank units so an inherited bold style cannot make
        // ordinary spaces five pixels wide; restore the active style before
        // the actual border glyph.
        return "§r" + padding + activeLegacyFormatting(prefix);
    }

    private static String activeLegacyFormatting(String value) {
        char color = 0;
        StringBuilder styles = new StringBuilder();
        for (int i = 0; i + 1 < value.length(); i++) {
            if (!isFormattingCodeAt(value, i)) continue;
            char code = Character.toLowerCase(value.charAt(++i));
            if ("0123456789abcdef".indexOf(code) >= 0) {
                color = code;
                styles.setLength(0);
            } else if (code == 'r') {
                color = 0;
                styles.setLength(0);
            } else if ("klmno".indexOf(code) >= 0 && styles.indexOf(String.valueOf(code)) < 0) {
                styles.append(code);
            }
        }
        StringBuilder result = new StringBuilder(2 + styles.length() * 2);
        if (color != 0) result.append('§').append(color);
        for (int i = 0; i < styles.length(); i++) result.append('§').append(styles.charAt(i));
        return result.toString();
    }

    private static int bestPaddingDifference(FrameLine line, int targetWidth) {
        int bestDifference = Math.abs(line.width() - targetWidth);
        String padding = bestPadding(line, targetWidth);
        if (!padding.isEmpty()) bestDifference = Math.min(bestDifference,
                Math.abs(font().width(line.prefix() + padding + line.suffix()) - targetWidth));
        return bestDifference;
    }

    private static int mapIndex(int index, List<Insertion> insertions) {
        int mapped = index;
        for (Insertion insertion : insertions) if (insertion.position() <= index) mapped += insertion.count();
        return mapped;
    }

    private static void applyFormatting(Screen screen, String code) {
        if (screen instanceof BookEditScreen bookScreen) {
            BookEditScreenAccessor book = (BookEditScreenAccessor) bookScreen;
            MultilineTextField field = ((MultiLineEditBoxAccessor) book.glyphcraft$getPageEditor()).glyphcraft$getTextField();
            MultilineTextFieldAccessor access = (MultilineTextFieldAccessor) field;
            int begin = Math.min(access.glyphcraft$getCursorRaw(), access.glyphcraft$getSelectCursorRaw());
            int end = Math.max(access.glyphcraft$getCursorRaw(), access.glyphcraft$getSelectCursorRaw());
            String value = field.value();
            String selected = value.substring(begin, end);
            String replacement;
            if ("§r".equals(code)) {
                if (begin == end) {
                    replacement = code;
                } else {
                    int expandedBegin = expandFormattingPrefix(value, begin);
                    int expandedEnd = expandResetSuffix(value, end);
                    replacement = stripFormatting(value.substring(expandedBegin, expandedEnd));
                    begin = expandedBegin;
                    end = expandedEnd;
                }
            } else if (isColorCode(code) && begin != end) {
                int expandedBegin = expandFormattingPrefix(value, begin);
                int expandedEnd = expandResetSuffix(value, end);
                replacement = applyColorToEveryLine(value.substring(expandedBegin, expandedEnd), code);
                begin = expandedBegin;
                end = expandedEnd;
            } else if (begin != end) {
                int expandedBegin = expandFormattingPrefix(value, begin);
                int expandedEnd = expandResetSuffix(value, end);
                replacement = applyStylePreservingColors(value.substring(expandedBegin, expandedEnd), code);
                begin = expandedBegin;
                end = expandedEnd;
            } else {
                replacement = code;
            }
            replaceBookRange(field, begin, end, replacement);
        } else if (screen instanceof AbstractSignEditScreen signScreen) {
            SignEditScreenAccessor sign = (SignEditScreenAccessor) signScreen;
            int line = Math.max(0, Math.min(3, sign.glyphcraft$getLine()));
            String value = sign.glyphcraft$getMessages()[line];
            int cursor = Math.max(0, Math.min(value.length(), sign.glyphcraft$getSignField().getCursorPos()));
            int selection = Math.max(0, Math.min(value.length(), sign.glyphcraft$getSignField().getSelectionPos()));
            int begin = Math.min(cursor, selection);
            int end = Math.max(cursor, selection);
            String selected = value.substring(begin, end);
            String replacement;
            if ("§r".equals(code)) {
                if (begin == end) {
                    replacement = code;
                } else {
                    int expandedBegin = expandFormattingPrefix(value, begin);
                    int expandedEnd = expandResetSuffix(value, end);
                    replacement = stripFormatting(value.substring(expandedBegin, expandedEnd));
                    begin = expandedBegin;
                    end = expandedEnd;
                }
            } else if (isColorCode(code) && begin != end) {
                int expandedBegin = expandFormattingPrefix(value, begin);
                int expandedEnd = expandResetSuffix(value, end);
                replacement = applyColorToEveryLine(value.substring(expandedBegin, expandedEnd), code);
                begin = expandedBegin;
                end = expandedEnd;
            } else if (begin != end) {
                int expandedBegin = expandFormattingPrefix(value, begin);
                int expandedEnd = expandResetSuffix(value, end);
                replacement = applyStylePreservingColors(value.substring(expandedBegin, expandedEnd), code);
                begin = expandedBegin;
                end = expandedEnd;
            } else {
                replacement = code;
            }
            String next = value.substring(0, begin) + replacement + value.substring(end);
            sign.glyphcraft$setMessage(next);
            int nextCursor = begin + replacement.length();
            sign.glyphcraft$getSignField().setCursorPos(nextCursor, false);
            sign.glyphcraft$getSignField().setSelectionPos(nextCursor);
        }
    }

    private static void replaceBookRange(MultilineTextField field, int begin, int end, String replacement) {
        String value = field.value();
        String next = value.substring(0, begin) + replacement + value.substring(end);
        if (next.length() > 1024) {
            status = "Не хватает места на странице";
            return;
        }
        MultilineTextFieldAccessor access = (MultilineTextFieldAccessor) field;
        int cursor = begin + replacement.length();
        access.glyphcraft$setValueRaw(next);
        access.glyphcraft$setCursorRaw(cursor);
        access.glyphcraft$setSelectCursorRaw(cursor);
        access.glyphcraft$onValueChange();
    }

    private static String stripFormatting(String value) {
        if (value == null || value.isEmpty()) return "";
        return restoreAlignmentMarkers(protectAlignmentMarkers(value)
                .replaceAll("(?i)§[0-9A-FK-OR]", ""));
    }

    private static boolean isColorCode(String code) {
        return code != null && code.length() == 2 && code.charAt(0) == '§'
                && "0123456789abcdefABCDEF".indexOf(code.charAt(1)) >= 0;
    }

    private static boolean isFormattingCodeAt(String value, int index) {
        return value != null && index >= 0 && index + 1 < value.length()
                && value.charAt(index) == '§'
                && "0123456789abcdefABCDEFklmnorKLMNOR".indexOf(value.charAt(index + 1)) >= 0;
    }

    /** Include legacy codes immediately before a selection, so recolouring does
     * not leave the previous colour outside the replacement range. */
    private static int expandFormattingPrefix(String value, int begin) {
        while (begin >= 2 && isFormattingCodeAt(value, begin - 2)) begin -= 2;
        return begin;
    }

    /** Consume the reset belonging to the selected run. A following colour or
     * style code is left alone because it may belong to the next word. */
    private static int expandResetSuffix(String value, int end) {
        while (end + 1 < value.length() && value.charAt(end) == '§'
                && "rR".indexOf(value.charAt(end + 1)) >= 0) {
            end += 2;
        }
        return end;
    }

    private static String stripColorCodesAndResets(String value) {
        if (value == null || value.isEmpty()) return "";
        return value.replaceAll("(?i)§[0-9A-FR]", "");
    }

    /**
     * Minecraft's book editor renders every logical line separately, so one
     * colour code at the beginning of a multiline selection only affects the
     * first line. Prefix every selected line and carry active text styles to
     * the next line so the whole highlighted rectangle is formatted.
     */
    private static String applyColorToEveryLine(String value, String colorCode) {
        String clean = stripColorCodesAndResets(protectAlignmentMarkers(value));
        StringBuilder result = new StringBuilder(clean.length() + 16);
        StringBuilder activeStyles = new StringBuilder();
        result.append(colorCode);
        for (int i = 0; i < clean.length(); i++) {
            char current = clean.charAt(i);
            if (current == ALIGNMENT_SENTINEL) {
                result.append(current).append(colorCode).append(activeStyles);
            } else if (current == '§' && i + 1 < clean.length()) {
                char formatting = Character.toLowerCase(clean.charAt(++i));
                result.append('§').append(formatting);
                if ("klmno".indexOf(formatting) >= 0
                        && activeStyles.indexOf("§" + formatting) < 0) {
                    activeStyles.append('§').append(formatting);
                }
            } else if (current == '\n') {
                result.append('\n').append(colorCode).append(activeStyles);
            } else {
                result.append(current);
            }
        }
        return restoreAlignmentMarkers(result.append("§r").toString());
    }

    /** Legacy colour codes reset bold/italic/underline/strikethrough. Reinsert
     * the requested style after every colour/reset so mixed-colour selections
     * keep the chosen editing tool active throughout the visible text. */
    private static String applyStylePreservingColors(String value, String styleCode) {
        if (value == null || value.isEmpty()) return styleCode + "§r";
        value = protectAlignmentMarkers(value);
        StringBuilder result = new StringBuilder(value.length() + 8);
        char requested = Character.toLowerCase(styleCode.charAt(1));
        boolean styleActive = false;

        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current == ALIGNMENT_SENTINEL) {
                result.append(current);
                styleActive = false;
            } else if (current == '§' && i + 1 < value.length()) {
                char formatting = Character.toLowerCase(value.charAt(++i));
                if (formatting == requested) {
                    if (!styleActive) result.append(styleCode);
                    styleActive = true;
                } else {
                    result.append('§').append(formatting);
                    if ("0123456789abcdefr".indexOf(formatting) >= 0) styleActive = false;
                }
            } else {
                if (!styleActive) {
                    result.append(styleCode);
                    styleActive = true;
                }
                result.append(current);
            }
        }

        while (result.length() >= 2 && result.charAt(result.length() - 2) == '§'
                && Character.toLowerCase(result.charAt(result.length() - 1)) == 'r') {
            result.setLength(result.length() - 2);
        }
        return restoreAlignmentMarkers(result.append("§r").toString());
    }

    private static String protectAlignmentMarkers(String value) {
        return value == null ? "" : value.replace(FINE_PADDING, String.valueOf(ALIGNMENT_SENTINEL));
    }

    private static String restoreAlignmentMarkers(String value) {
        return value == null ? "" : value.replace(String.valueOf(ALIGNMENT_SENTINEL), FINE_PADDING);
    }

    private static void scrollSymbols(Layout layout, int direction) {
        List<String> categories = GlyphSymbols.categories();
        if (categories.isEmpty()) return;
        int categoryPages = pagesForCategory(categoryIndex, layout);
        if (direction > 0) {
            if (symbolPage + 1 < categoryPages) {
                symbolPage++;
            } else {
                categoryIndex = (categoryIndex + 1) % categories.size();
                symbolPage = 0;
            }
        } else if (symbolPage > 0) {
            symbolPage--;
        } else {
            categoryIndex = Math.floorMod(categoryIndex - 1, categories.size());
            symbolPage = pagesForCategory(categoryIndex, layout) - 1;
        }
    }

    private static int pagesForCategory(int index, Layout layout) {
        if (layout.symbolSlots <= 0) return 1;
        String category = GlyphSymbols.categories().get(Math.floorMod(index, GlyphSymbols.categories().size()));
        List<GlyphSymbols.Symbol> symbols = GlyphSymbols.symbols(category);
        return Math.max(1, (symbols.size() + layout.symbolSlots - 1) / layout.symbolSlots);
    }

    private static void openSaveSignDialog(AbstractSignEditScreen screen) {
        signLoadDialog = false;
        signDeleteConfirm = false;
        signSaveDialog = true;
        int x = (screen.width - 300) / 2 + 20;
        int y = (screen.height - 110) / 2 + 34;
        signSaveNameBox = new EditBox(Minecraft.getInstance().font, x, y, 260, 20,
                Component.literal("Название таблички"));
        signSaveNameBox.setMaxLength(64);
        signSaveNameBox.setBordered(true);
        signSaveNameBox.setValue("");
        signSaveNameBox.setFocused(true);
    }

    private static void openLoadSignDialog(AbstractSignEditScreen screen) {
        signSaveDialog = false;
        signDeleteConfirm = false;
        signLoadFiles = signFiles();
        loadListPage = 0;
        signLoadDialog = true;
    }

    private static List<String> signFiles() {
        try {
            return GlyphBookStorage.list().stream()
                    .filter(file -> file.startsWith("sign_"))
                    .toList();
        } catch (IOException e) {
            status = "Не удалось прочитать сохранения табличек";
            return List.of();
        }
    }

    private static String signDisplayName(String file) {
        return file.startsWith("sign_") ? file.substring(5) : file;
    }

    private static boolean handleSignSaveDialog(AbstractSignEditScreen screen, double mouseX, double mouseY) {
        int width = 300, height = 110;
        int x = (screen.width - width) / 2, y = (screen.height - height) / 2;
        if (inside(mouseX, mouseY, x + 20, y + 34, 260, 20)) {
            if (signSaveNameBox != null) signSaveNameBox.setFocused(true);
            return true;
        }
        if (inside(mouseX, mouseY, x + 20, y + 74, 120, 22)) {
            String name = signSaveNameBox == null ? "" : signSaveNameBox.getValue().trim();
            if (name.isBlank()) status = "Введите название таблички";
            else saveNamedSign(screen, name);
            return true;
        }
        if (inside(mouseX, mouseY, x + 160, y + 74, 120, 22)) {
            signSaveDialog = false;
            signSaveNameBox = null;
            return true;
        }
        return true;
    }

    private static boolean handleLoadSignDialog(AbstractSignEditScreen screen, double mouseX, double mouseY) {
        int width = 400, rows = loadRowsPerPage(screen), start = loadListPage * rows;
        int visible = Math.min(rows, Math.max(0, signLoadFiles.size() - start));
        int height = 58 + rows * 26, x = (screen.width - width) / 2;
        int y = Math.max(8, (screen.height - height) / 2);
        if (!inside(mouseX, mouseY, x, y, width, height)) { signLoadDialog = false; return true; }
        for (int i = 0; i < visible; i++) {
            int by = y + 27 + i * 26;
            if (inside(mouseX, mouseY, x + width - 125, by, 82, 22)) {
                loadNamedSign(screen, signLoadFiles.get(start + i));
                signLoadDialog = false;
                return true;
            }
            if (inside(mouseX, mouseY, x + width - 37, by, 22, 22)) {
                deleteSaveTarget = signLoadFiles.get(start + i);
                signLoadDialog = false;
                signDeleteConfirm = true;
                return true;
            }
        }
        int pages = Math.max(1, (signLoadFiles.size() + rows - 1) / rows);
        if (inside(mouseX, mouseY, x + 20, y + height - 25, 22, 21)) loadListPage = Math.floorMod(loadListPage - 1, pages);
        else if (inside(mouseX, mouseY, x + 48, y + height - 25, 22, 21)) loadListPage = (loadListPage + 1) % pages;
        else if (inside(mouseX, mouseY, x + width - 120, y + height - 25, 100, 21)) signLoadDialog = false;
        return true;
    }

    private static void drawSignSaveDialog(GuiGraphics g, AbstractSignEditScreen screen, int mouseX, int mouseY) {
        int width = 300, height = 110, x = (screen.width - width) / 2, y = (screen.height - height) / 2;
        g.fill(0, 0, screen.width, screen.height, 0x66000000);
        g.fill(x, y, x + width, y + height, 0xFF151B28); border(g, x, y, width, height, 0xFF8AA2C7);
        drawCentered(g, "Сохранить табличку", x + width / 2, y + 8, TEXT);
        g.drawString(font(), "Название таблички", x + 20, y + 26, MUTED, false);
        if (signSaveNameBox != null) signSaveNameBox.render(g, mouseX, mouseY, 0.0F);
        drawMiniButton(g, x + 20, y + 74, 120, 22, "Сохранить", mouseX, mouseY);
        drawMiniButton(g, x + 160, y + 74, 120, 22, "Отмена", mouseX, mouseY);
    }

    private static void drawSignLoadDialog(GuiGraphics g, AbstractSignEditScreen screen, int mouseX, int mouseY) {
        int width = 400, rows = loadRowsPerPage(screen), start = loadListPage * rows;
        int visible = Math.min(rows, Math.max(0, signLoadFiles.size() - start));
        int height = 58 + rows * 26, x = (screen.width - width) / 2, y = Math.max(8, (screen.height - height) / 2);
        g.fill(0, 0, screen.width, screen.height, 0x66000000); g.fill(x, y, x + width, y + height, 0xFF151B28);
        border(g, x, y, width, height, 0xFF8AA2C7); drawCentered(g, "Загрузить табличку", x + width / 2, y + 8, TEXT);
        if (signLoadFiles.isEmpty()) drawCentered(g, "Сохранений пока нет", x + width / 2, y + 33, MUTED);
        for (int i = 0; i < visible; i++) {
            int by = y + 27 + i * 26, loadX = x + width - 125, deleteX = x + width - 37;
            int fieldWidth = loadX - x - 16;
            drawBookNameField(g, x + 10, by, fieldWidth, 22, inside(mouseX, mouseY, x + 10, by, fieldWidth, 22));
            g.drawString(font(), trimToWidth(signDisplayName(signLoadFiles.get(start + i)), fieldWidth - 16), x + 18, by + 7, TEXT, false);
            drawMiniButton(g, loadX, by, 82, 22, "Загрузить", mouseX, mouseY);
            drawMiniButton(g, deleteX, by, 22, 22, "×", mouseX, mouseY);
        }
        int pages = Math.max(1, (signLoadFiles.size() + rows - 1) / rows);
        drawMiniButton(g, x + 20, y + height - 25, 22, 21, "‹", mouseX, mouseY);
        drawMiniButton(g, x + 48, y + height - 25, 22, 21, "›", mouseX, mouseY);
        g.drawString(font(), (loadListPage + 1) + " / " + pages, x + 78, y + height - 18, MUTED, false);
        drawMiniButton(g, x + width - 120, y + height - 25, 100, 21, "Закрыть", mouseX, mouseY);
    }

    private static boolean handleSignDeleteConfirm(AbstractSignEditScreen screen, double mouseX, double mouseY) {
        int width = 320, height = 92, x = (screen.width - width) / 2, y = (screen.height - height) / 2;
        if (inside(mouseX, mouseY, x + 24, y + 58, 112, 22)) {
            try { GlyphBookStorage.delete(deleteSaveTarget); status = "Сохранение таблички удалено"; }
            catch (IOException e) { status = "Не удалось удалить сохранение"; }
            signLoadFiles = signFiles(); signDeleteConfirm = false; signLoadDialog = true; return true;
        }
        if (inside(mouseX, mouseY, x + width - 136, y + 58, 112, 22)) { signDeleteConfirm = false; signLoadDialog = true; }
        return true;
    }

    private static void drawSignDeleteConfirm(GuiGraphics g, AbstractSignEditScreen screen, int mouseX, int mouseY) {
        int width = 320, height = 92, x = (screen.width - width) / 2, y = (screen.height - height) / 2;
        g.fill(0, 0, screen.width, screen.height, 0x66000000); g.fill(x, y, x + width, y + height, 0xFF151B28);
        border(g, x, y, width, height, 0xFF8AA2C7); drawCentered(g, "Удалить сохранение?", x + width / 2, y + 9, TEXT);
        drawCentered(g, trimToWidth(signDisplayName(deleteSaveTarget), width - 24), x + width / 2, y + 28, MUTED);
        drawMiniButton(g, x + 24, y + 58, 112, 22, "Да", mouseX, mouseY);
        drawMiniButton(g, x + width - 136, y + 58, 112, 22, "Нет", mouseX, mouseY);
    }

    private static void saveNamedSign(AbstractSignEditScreen screen, String name) {
        SignEditScreenAccessor sign = (SignEditScreenAccessor) screen;
        GlyphBook saved = new GlyphBook(); saved.name = name; saved.pages.getFirst().title = "Табличка";
        saved.pages.getFirst().text = String.join("\n", sign.glyphcraft$getMessages());
        try { GlyphBookStorage.save("sign_" + name, saved); signSaveDialog = false; signSaveNameBox = null; status = "Табличка сохранена: " + name; }
        catch (IOException e) { status = "Ошибка сохранения таблички"; }
    }

    private static void loadNamedSign(AbstractSignEditScreen screen, String file) {
        try {
            GlyphBook loaded = GlyphBookStorage.load(file);
            String value = loaded.pages.isEmpty() ? "" : loaded.pages.getFirst().text;
            setSignLines((SignEditScreenAccessor) screen, value.split("\\R", -1));
            status = "Табличка загружена: " + signDisplayName(file);
        } catch (IOException e) { status = "Не удалось загрузить табличку"; }
    }

    private static void saveSign(SignEditScreenAccessor sign) {
        // The vanilla text helper owns the active line. Commit it explicitly
        // before taking the four-line snapshot so a just-typed character is
        // never one frame behind the messages array.
        int activeLine = Math.max(0, Math.min(3, sign.glyphcraft$getLine()));
        sign.glyphcraft$setLine(activeLine);
        sign.glyphcraft$setMessage(sign.glyphcraft$getMessages()[activeLine]);
        GlyphBook saved = new GlyphBook();
        saved.name = "GlyphCraft Sign";
        saved.pages.getFirst().title = "Табличка";
        saved.pages.getFirst().text = String.join("\n", sign.glyphcraft$getMessages());
        try {
            GlyphBookStorage.save("last_sign", saved);
            status = "Сохранено: last_sign.json";
        } catch (IOException e) {
            status = "Ошибка сохранения таблички";
        }
    }

    private static void loadSign(SignEditScreenAccessor sign) {
        try {
            GlyphBook loaded = GlyphBookStorage.load("last_sign");
            String value = loaded.pages.isEmpty() ? "" : loaded.pages.getFirst().text;
            setSignLines(sign, value.split("\\R", -1));
            status = "Загружено: last_sign.json";
        } catch (IOException e) {
            status = "Сначала сохраните табличку";
        }
    }

    private static void setSignLines(SignEditScreenAccessor sign, String[] input) {
        int original = sign.glyphcraft$getLine();
        String[] messages = sign.glyphcraft$getMessages();
        for (int i = 0; i < 4; i++) {
            sign.glyphcraft$setLine(i);
            String value = i < input.length && input[i] != null ? input[i] : "";
            // Match vanilla's physical sign width. This prevents a large or
            // malformed clipboard value from reaching rendering/network code.
            String safe = font().plainSubstrByWidth(value.replace('\n', ' ').replace('\r', ' '), 90);
            // Update both vanilla data sources. setMessage normally does this,
            // but direct line switching through the accessor does not notify
            // the render model on every Minecraft 1.21.x build.
            messages[i] = safe;
            sign.glyphcraft$setMessage(safe);
        }
        int restored = Math.max(0, Math.min(3, original));
        sign.glyphcraft$setLine(restored);
        // setLine is an accessor, not vanilla's line-navigation callback, so
        // explicitly refresh TextFieldHelper with the restored line.
        sign.glyphcraft$setMessage(messages[restored]);
    }

    private static void replaceBookFromText(BookEditScreenAccessor screen, String value) {
        String[] split = value.split("\\R--- GLYPHCRAFT PAGE ---\\R", -1);
        List<String> pages = screen.glyphcraft$getPages();
        pages.clear();
        pages.addAll(Arrays.asList(split).subList(0, Math.min(100, split.length)));
        if (pages.isEmpty()) pages.add("");
        screen.glyphcraft$setCurrentPage(0);
        refresh(screen);
    }

    private static void syncCurrent(BookEditScreenAccessor screen) {
        List<String> pages = screen.glyphcraft$getPages();
        int current = screen.glyphcraft$getCurrentPage();
        if (current >= 0 && current < pages.size()) pages.set(current, screen.glyphcraft$getPageEditor().getValue());
    }

    private static void refresh(BookEditScreenAccessor screen) {
        screen.glyphcraft$updatePageContent();
        screen.glyphcraft$updateButtonVisibility();
    }

    private static void clipboard(String value) { Minecraft.getInstance().keyboardHandler.setClipboard(value == null ? "" : value); }
    private static String clipboard() { return Minecraft.getInstance().keyboardHandler.getClipboard(); }

    private static int actionAt(double mouseX, double mouseY, int x, int y, int w, int count) {
        for (int i = 0; i < count; i++) if (inside(mouseX, mouseY, x, y + 16 + i * 25, w, 21)) return i;
        return -1;
    }

    private static void drawMiniButton(GuiGraphics g, int x, int y, int w, int h, String text, int mouseX, int mouseY) {
        boolean hover = inside(mouseX, mouseY, x, y, w, h);
        beveledButton(g, x, y, w, h, hover, false);
        drawCentered(g, text, x + w / 2, y + 6, hover ? TEXT : 0xFFD1DCEC);
    }

    private static void beveledButton(GuiGraphics g, int x, int y, int w, int h, boolean hover, boolean active) {
        int bg = active ? BUTTON_ACTIVE : (hover ? BUTTON_HOVER : BUTTON_BG);
        g.fill(x, y, x + w, y + h, 0xFF151515);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, bg);
        g.fill(x + 1, y + 1, x + w - 2, y + 2, hover ? 0xFFE0E0E0 : 0xFFB8B8B8);
        g.fill(x + 1, y + 1, x + 2, y + h - 2, hover ? 0xFFD8D8D8 : 0xFFA8A8A8);
        g.fill(x + 2, y + h - 2, x + w - 1, y + h - 1, 0xFF303030);
        g.fill(x + w - 2, y + 2, x + w - 1, y + h - 1, 0xFF303030);
    }

    private static void colorCell(GuiGraphics g, int x, int y, int size, int color, boolean hover) {
        g.fill(x, y, x + size, y + size, 0xFF151515);
        g.fill(x + 1, y + 1, x + size - 1, y + size - 1, hover ? 0xFFFFFFFF : 0xFFB8B8B8);
        g.fill(x + 3, y + 3, x + size - 3, y + size - 3, color);
    }

    private static void drawCentered(GuiGraphics g, String text, int centerX, int y, int color) {
        g.drawString(font(), text, centerX - font().width(text) / 2, y, color, false);
    }

    private static String trimToWidth(String text, int width) {
        if (text == null || width <= 0) return "";
        if (font().width(text) <= width) return text;
        String result = text;
        while (!result.isEmpty() && font().width(result + "…") > width) result = result.substring(0, result.length() - 1);
        return result + "…";
    }

    private static Font font() { return Minecraft.getInstance().font; }
    private static boolean inside(double px, double py, int x, int y, int w, int h) { return px >= x && px < x + w && py >= y && py < y + h; }
    private static void border(GuiGraphics g, int x, int y, int w, int h, int color) { g.fill(x, y, x + w, y + 1, color); g.fill(x, y + h - 1, x + w, y + h, color); g.fill(x, y, x + 1, y + h, color); g.fill(x + w - 1, y, x + w, y + h, color); }

    private record FrameLine(int offset, String raw, int insertAt, int frameEnd, int width, boolean pure) {
        String prefix() { return raw.substring(0, insertAt); }
        String suffix() { return raw.substring(insertAt, frameEnd); }
    }

    private record FrameBounds(int start, int end) { }

    private record AlignmentResult(boolean changed, int frameCount) { }

    private record Insertion(int position, int count) { }

    private static final class Layout {
        final int leftX, rightX, panelY, panelW;
        final int barX, barW, bottomY, bottomH;
        final int symbolStartX, symbolSlots, pageBackX, pageNextX, colorStartX, bookMenuX, bookMenuW, alignX, alignW, formatStartX;
        final boolean showSidePanels;

        private Layout(int leftX, int rightX, int panelY, int panelW, int barX, int barW, int bottomY, int bottomH,
                       int symbolStartX, int symbolSlots, int pageBackX, int pageNextX, int colorStartX, int bookMenuX, int bookMenuW, int alignX, int alignW, int formatStartX,
                       boolean showSidePanels) {
            this.leftX = leftX; this.rightX = rightX; this.panelY = panelY; this.panelW = panelW;
            this.barX = barX; this.barW = barW; this.bottomY = bottomY; this.bottomH = bottomH;
            this.symbolStartX = symbolStartX; this.symbolSlots = symbolSlots; this.pageBackX = pageBackX; this.pageNextX = pageNextX;
            this.colorStartX = colorStartX; this.bookMenuX = bookMenuX; this.bookMenuW = bookMenuW;
            this.alignX = alignX; this.alignW = alignW; this.formatStartX = formatStartX;
            this.showSidePanels = showSidePanels;
        }

        static Layout of(Screen screen) {
            int centerKeepClear = screen instanceof BookEditScreen ? 228 : 300;
            boolean showSides = screen.width >= 470;
            int availableSide = (screen.width - centerKeepClear) / 2 - 8;
            int panelW = Math.min(140, Math.max(118, availableSide));
            int leftX = 10;
            int rightX = screen.width - panelW - 10;
            int barW = Math.min(screen.width - 24, 760);
            int barX = (screen.width - barW) / 2;
            int bottomH = 44;
            int bottomY = screen.height - bottomH - 6;
            int symbolStart = barX + 144;
            int slots = Math.max(4, (barW - 144 - 50) / 24);
            int pageNext = barX + barW - 20;
            int pageBack = pageNext - 24;
            int colorStart = barX + 4;
            int formatStart = screen instanceof AbstractSignEditScreen
                    ? barX + (barW - FORMAT_LABELS.length * 25) / 2
                    : barX + barW - FORMAT_LABELS.length * 25;
            int bookMenuW = 22;
            int alignW = 22;
            int statusX = colorStart + COLORS.length * 17 + 8;
            int bookMenuX = statusX;
            int alignX = bookMenuX + bookMenuW + 4;
            return new Layout(leftX, rightX, 24, panelW, barX, barW, bottomY, bottomH,
                    symbolStart, slots, pageBack, pageNext, colorStart, bookMenuX, bookMenuW, alignX, alignW, formatStart, showSides);
        }
    }
}
