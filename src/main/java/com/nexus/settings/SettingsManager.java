package com.nexus.settings;

import java.util.prefs.Preferences;

public class SettingsManager {
    private static final String KEY_THEME = "theme";
    private static final String KEY_FONT_FAMILY = "fontFamily";
    private static final String KEY_FONT_SIZE = "fontSize";
    private static final String KEY_ACCENT = "accentColor";

    private final Preferences preferences = Preferences.userNodeForPackage(SettingsManager.class);

    public UiSettings load() {
        return new UiSettings(
                preferences.get(KEY_THEME, "Dark"),
                preferences.get(KEY_FONT_FAMILY, "Segoe UI"),
                preferences.getInt(KEY_FONT_SIZE, 14),
                preferences.get(KEY_ACCENT, "Blue")
        );
    }

    public void save(UiSettings settings) {
        preferences.put(KEY_THEME, settings.getTheme());
        preferences.put(KEY_FONT_FAMILY, settings.getFontFamily());
        preferences.putInt(KEY_FONT_SIZE, settings.getFontSize());
        preferences.put(KEY_ACCENT, settings.getAccentColor());
    }
}