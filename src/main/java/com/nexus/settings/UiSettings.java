package com.nexus.settings;

public final class UiSettings {
    private final String theme;
    private final String fontFamily;
    private final int fontSize;
    private final String accentColor;

    public UiSettings(String theme, String fontFamily, int fontSize, String accentColor) {
        this.theme = theme == null || theme.isBlank() ? "Dark" : theme;
        this.fontFamily = fontFamily == null || fontFamily.isBlank() ? "Segoe UI" : fontFamily;
        this.fontSize = fontSize <= 0 ? 14 : fontSize;
        this.accentColor = accentColor == null || accentColor.isBlank() ? "Blue" : accentColor;
    }

    public String getTheme() {
        return theme;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public int getFontSize() {
        return fontSize;
    }

    public String getAccentColor() {
        return accentColor;
    }

    public String themeStyleClass() {
        return "Light".equalsIgnoreCase(theme) ? "theme-light" : "theme-dark";
    }

    public String accentHex() {
        return switch (accentColor) {
            case "Graphite" -> "#4A5568";
            case "Teal" -> "#2C7A7B";
            case "Plum" -> "#6B46C1";
            default -> "#25304A";
        };
    }

    public String accentHoverHex() {
        return switch (accentColor) {
            case "Graphite" -> "#5F6B7D";
            case "Teal" -> "#319795";
            case "Plum" -> "#7F56D9";
            default -> "#31405F";
        };
    }

    public String toInlineStyle() {
        return "-fx-font-family: \"" + escape(fontFamily) + "\";" +
                "-fx-font-size: " + fontSize + "px;" +
                "-nexus-accent: " + accentHex() + ";" +
                "-nexus-accent-hover: " + accentHoverHex() + ";";
    }

    private String escape(String value) {
        return value.replace("\"", "");
    }
}