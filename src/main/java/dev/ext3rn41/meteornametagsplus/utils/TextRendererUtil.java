package dev.ext3rn41.meteornametagsplus.utils;

import meteordevelopment.meteorclient.renderer.Fonts;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.renderer.text.VanillaTextRenderer;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.utils.render.color.Color;

public interface TextRendererUtil {
    static TextRenderer get() {
        return Config.get().customFont.get() ? Fonts.RENDERER : VanillaTextRenderer.INSTANCE;
    }
}