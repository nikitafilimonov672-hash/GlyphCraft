package ru.zohov.glyphcraft.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GlyphSymbols {
    public record Symbol(String value, String label) { }
    private static final Map<String, List<Symbol>> CATEGORIES = create();

    private GlyphSymbols() { }
    public static List<String> categories() { return List.copyOf(CATEGORIES.keySet()); }
    public static List<Symbol> symbols(String category) { return CATEGORIES.getOrDefault(category, List.of()); }

    private static Map<String, List<Symbol>> create() {
        Map<String, List<Symbol>> out = new LinkedHashMap<>();
        out.put("Рамки", range("╔╗╚╝╠╣╦╩╬═║╭╮╰╯┌┐└┘├┤┬┴┼━┃┏┓┗┛┣┫┳┻╋╱╲╳╴╵╶╷╸╹╺╻╼╽╾╿"));
        out.put("Стрелки", range("←↑→↓↔↕↖↗↘↙⇐⇑⇒⇓⇔⇕➜➤➥➦➧➨➳➵➸➹➷➻⟵⟶⟷⟸⟹⟺⟻⟼⟿"));
        out.put("Фигуры", range("■□▪▫●○◆◇◈◉◌◍◎★☆✦✧✩✪✫✬✭✮✯✰❖⬢⬡◼◻◾◽🔶🔷"));
        out.put("Значки", range("⚑⚐⚔⚒⚙⚡☀☁☂☃☄★☆☘☕♠♣♥♦✓✔✕✖✚✦✧✿❀❁❂❃❄❅❆❇❈❉❊"));
        out.put("Музыка", range("♪♫♬♩♭♮♯𝄞𝄢♤♧♡♢"));
        out.put("Алфавит", range("αβγδεζηθικλμνξοπρστυφχψωΑΒΓΔΕΖΗΘΙΚΛΜΝΞΟΠΡΣΤΥΦΧΨΩАБВГДЕЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ"));
        out.put("Математика", range("∞≠≈≤≥±×÷√∑∏∫∂∆∇∈∉⊂⊃⊆⊇∩∪∧∨¬∀∃∴∵∝∠⊥∥⌈⌉⌊⌋"));
        out.put("Карточки", range("♠♣♥♦♡♢♤♧🂡🂱🃁🃑"));
        out.put("Блоки", range("█▓▒░▀▄▌▐▖▗▘▝▞▚▙▟▛▜▝▘"));
        out.put("Орнаменты", range("✠✡✢✣✤✥✦✧✩✪✫✬✭✮✯✰❂❃❇❈❉❊❋❖❘❙❚❯❮❰❱"));
        out.put("Звёзды", range("★☆✦✧✶✷✸✹✺✻✼✽✾✿❀❁❂❃❄❅❆❇❈❉❊❋⭑⭒"));
        out.put("Геометрия", range("◊◈◉◌◍◎◐◑◒◓◔◕◖◗◚◛◜◝◞◟◠◡◢◣◤◥◦◧◨◩◪◫◬◭◮◯"));
        out.put("Ромбы", range("◆◇◈◉⬢⬡⬣⬥⬦⬧⬨⬩⬪⬫⟐⟡⟢⟣⟤⟥⧫⧪⧫"));
        out.put("Техно", range("⌁⌂⌘⌚⌛⌕⌖⌗⌬⎈⎔⏣⏥⏦⏧⏱⏲⏳⏸⏹⏺⏻⏼⏽⏾⚙⚛⚡"));
        out.put("Особые знаки", range("☥☦☧☨☩☪☫☬☮☯☸♁♆♇⚚⚕⚜⚝⚘⚚"));
        out.put("Знаки", range("©®™℗℠№℡℮℀℁℅℆ℇ℈℉℔℥℧℩℮Ⅎ⅀⅁⅂⅃⅄"));
        out.put("Маркеры", range("⁅⁆⁑⁂⁙⁘⁚⁛⁜⁝⁞※⁕⁖⁗⁘⁙⁜⁝⁞❮❯❰❱"));
        out.put("Скобки", range("⟅⟆⟦⟧⟨⟩⟪⟫⟮⟯⦃⦄⦅⦆⦇⦈⦉⦊⦋⦌⦍⦎⦏⦐"));
        out.put("Стрелки 2", range("↢↣↤↥↦↧↨↩↪↫↬↭↮↯↰↱↲↳↴↵↶↷↸↹↺↻↼↽↾↿⇀⇁⇂⇃⇄⇅⇆⇇⇈⇉⇊⇋⇌⇍⇎⇏⇐⇑⇒⇓"));
        out.put("Линии", range("─━│┃┄┅┆┇┈┉┊┋┌┍┎┏┐┑┒┓└┕┖┗┘┙┚┛├┝┞┟┠┡┢┣┤┥┦┧┨┩┪┫┬┭┮┯┰┱┲┳┴┵┶┷┸┹┺┻┼┽┾┿╀╁╂╃╄╅╆╇╈╉╊╋"));
        out.put("Указатели", range("☞☜☝☟☛☚➔➙➛➜➝➞➟➠➡➢➣➤➥➦➧➨➩➪➫➬➭➮➯➱➲➳➵➸➹➺➻➼➽➾"));
        out.put("Юникод", unicodeRanges());

        Map<String, List<Symbol>> unique = new LinkedHashMap<>();
        Set<String> seen = new HashSet<>();
        for (Map.Entry<String, List<Symbol>> entry : out.entrySet()) {
            List<Symbol> symbols = new ArrayList<>();
            for (Symbol symbol : entry.getValue()) {
                if (seen.add(symbol.value())) symbols.add(symbol);
            }
            if (!symbols.isEmpty()) unique.put(entry.getKey(), symbols);
        }
        return unique;
    }

    private static List<Symbol> range(String value) {
        List<Symbol> result = new ArrayList<>();
        value.codePoints().filter(cp -> cp <= 0xFFFF && !Character.isISOControl(cp))
                .forEach(cp -> result.add(new Symbol(new String(Character.toChars(cp)), "U+" + Integer.toHexString(cp).toUpperCase())));
        return result;
    }

    private static List<Symbol> unicodeRanges() {
        List<Symbol> result = new ArrayList<>();
        int[][] ranges = {{0x2500,0x257F},{0x2580,0x259F},{0x2190,0x21FF},{0x2200,0x22FF},{0x2300,0x23FF},{0x25A0,0x25FF},{0x2600,0x26FF},{0x2700,0x27BF}};
        for (int[] r : ranges) for (int cp = r[0]; cp <= r[1]; cp++) {
            if (Character.isDefined(cp) && !Character.isISOControl(cp)) result.add(new Symbol(new String(Character.toChars(cp)), "U+" + Integer.toHexString(cp).toUpperCase()));
        }
        return result;
    }
}
