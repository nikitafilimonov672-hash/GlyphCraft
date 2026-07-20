package ru.zohov.glyphcraft;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GlyphCraftMod implements ModInitializer {
    public static final String MOD_ID = "glyphcraft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public GlyphCraftMod() { }

    @Override
    public void onInitialize() {
        LOGGER.info("GlyphCraft core initialized");
    }
}
