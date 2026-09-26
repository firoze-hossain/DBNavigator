package com.roze.dbnavigator.ui.action;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility for formatting, parsing, and resolving keystrokes and shortcuts
 * matching DataGrip / IntelliJ style with macOS glyphs and Windows notations.
 */
public final class KeyStrokeFormatter {

    private static final boolean IS_MAC =
            System.getProperty("os.name", "").toLowerCase().contains("mac");

    // Known macOS system shortcuts for conflict detection (as in DataGrip)
    private static final Map<String, String> MACOS_SYSTEM_SHORTCUTS = new LinkedHashMap<>();

    static {
        MACOS_SYSTEM_SHORTCUTS.put("⇧⌘A", "Search man Page Index in Terminal in macOS shortcuts");
        MACOS_SYSTEM_SHORTCUTS.put("⌘Space", "Spotlight Search in macOS shortcuts");
        MACOS_SYSTEM_SHORTCUTS.put("⌥⌘Space", "Finder Search in macOS shortcuts");
        MACOS_SYSTEM_SHORTCUTS.put("⌃Space", "Select next input source in macOS shortcuts");
        MACOS_SYSTEM_SHORTCUTS.put("⌘H", "Hide DBNavigator in macOS shortcuts");
        MACOS_SYSTEM_SHORTCUTS.put("⌥⌘H", "Hide Others in macOS shortcuts");
        MACOS_SYSTEM_SHORTCUTS.put("⌘M", "Minimize in macOS shortcuts");
        MACOS_SYSTEM_SHORTCUTS.put("⌥⌘M", "Minimize All in macOS shortcuts");
        MACOS_SYSTEM_SHORTCUTS.put("⇧⌘3", "Capture Entire Screen in macOS shortcuts");
        MACOS_SYSTEM_SHORTCUTS.put("⇧⌘4", "Capture Selected Window in macOS shortcuts");
        MACOS_SYSTEM_SHORTCUTS.put("⇧⌘5", "Screenshot and Recording Options in macOS shortcuts");
        MACOS_SYSTEM_SHORTCUTS.put("⌃↑", "Mission Control in macOS shortcuts");
        MACOS_SYSTEM_SHORTCUTS.put("⌃↓", "Application Windows in macOS shortcuts");
        MACOS_SYSTEM_SHORTCUTS.put("⌃←", "Move Left a Space in macOS shortcuts");
        MACOS_SYSTEM_SHORTCUTS.put("⌃→", "Move Right a Space in macOS shortcuts");
        MACOS_SYSTEM_SHORTCUTS.put("⌃⌘F", "Enter Full Screen in macOS shortcuts");
        MACOS_SYSTEM_SHORTCUTS.put("⌃⌘Q", "Lock Screen in macOS shortcuts");
        MACOS_SYSTEM_SHORTCUTS.put("⇧⌘Q", "Log Out in macOS shortcuts");
        MACOS_SYSTEM_SHORTCUTS.put("⌥⌘Esc", "Force Quit Applications in macOS shortcuts");
        MACOS_SYSTEM_SHORTCUTS.put("⌘Tab", "Application Switcher in macOS shortcuts");
        MACOS_SYSTEM_SHORTCUTS.put("⌘`", "Move Focus to Next Window in macOS shortcuts");
    }

    private KeyStrokeFormatter() {}

    public static boolean isMac() {
        return IS_MAC;
    }

    public static Map<String, String> getMacOsSystemShortcuts() {
        return MACOS_SYSTEM_SHORTCUTS;
    }

    /**
     * Formats a KeyCombination into display string based on current OS.
     */
    public static String format(KeyCombination combo) {
        if (combo == null) return "";
        if (combo instanceof KeyCodeCombination kcc) {
            boolean ctrl = kcc.getControl() == KeyCombination.ModifierValue.DOWN;
            boolean alt = kcc.getAlt() == KeyCombination.ModifierValue.DOWN;
            boolean shift = kcc.getShift() == KeyCombination.ModifierValue.DOWN;
            boolean meta = kcc.getMeta() == KeyCombination.ModifierValue.DOWN ||
                    kcc.getShortcut() == KeyCombination.ModifierValue.DOWN;
            return format(ctrl, alt, shift, meta, kcc.getCode());
        }
        return combo.getDisplayText();
    }

    /**
     * Formats a raw KeyEvent into a shortcut string, or returns null if only modifiers are pressed.
     */
    public static String formatFromEvent(KeyEvent event) {
        if (event == null) return null;
        KeyCode code = event.getCode();
        if (code == null || isModifierKey(code)) {
            return null;
        }

        boolean ctrl = event.isControlDown();
        boolean alt = event.isAltDown();
        boolean shift = event.isShiftDown();
        boolean meta = event.isMetaDown();

        return format(ctrl, alt, shift, meta, code);
    }

    public static boolean isModifierKey(KeyCode code) {
        return code == KeyCode.SHIFT || code == KeyCode.CONTROL ||
               code == KeyCode.ALT || code == KeyCode.META ||
               code == KeyCode.COMMAND || code == KeyCode.WINDOWS ||
               code == KeyCode.ALT_GRAPH;
    }

    /**
     * Formats modifier flags and keycode into OS-styled shortcut string.
     */
    public static String format(boolean ctrl, boolean alt, boolean shift, boolean meta, KeyCode code) {
        if (code == null) return "";

        String keyName = formatKey(code);

        if (IS_MAC) {
            // DataGrip macOS modifier order: ⌃ ⌥ ⇧ ⌘ or ⇧ ⌘
            StringBuilder sb = new StringBuilder();
            if (ctrl) sb.append("⌃");
            if (alt) sb.append("⌥");
            if (shift) sb.append("⇧");
            if (meta) sb.append("⌘");
            sb.append(keyName);
            return sb.toString();
        } else {
            List<String> parts = new ArrayList<>();
            if (ctrl) parts.add("Ctrl");
            if (alt) parts.add("Alt");
            if (shift) parts.add("Shift");
            if (meta) parts.add("Win");
            parts.add(keyName);
            return String.join("+", parts);
        }
    }

    public static String formatKey(KeyCode code) {
        if (code == null) return "";
        return switch (code) {
            case ENTER -> IS_MAC ? "↵" : "Enter";
            case BACK_SPACE -> IS_MAC ? "⌫" : "Backspace";
            case DELETE -> IS_MAC ? "⌦" : "Delete";
            case TAB -> IS_MAC ? "⇥" : "Tab";
            case ESCAPE -> "Esc";
            case SPACE -> "Space";
            case UP -> IS_MAC ? "↑" : "Up";
            case DOWN -> IS_MAC ? "↓" : "Down";
            case LEFT -> IS_MAC ? "←" : "Left";
            case RIGHT -> IS_MAC ? "→" : "Right";
            case PAGE_UP -> "Page Up";
            case PAGE_DOWN -> "Page Down";
            case HOME -> "Home";
            case END -> "End";
            case INSERT -> "Insert";
            case COMMA -> ",";
            case PERIOD -> ".";
            case SLASH -> "/";
            case BACK_SLASH -> "\\";
            case OPEN_BRACKET -> "[";
            case CLOSE_BRACKET -> "]";
            case BACK_QUOTE -> "`";
            case MINUS -> "-";
            case EQUALS -> "=";
            case SEMICOLON -> ";";
            case QUOTE -> "'";
            default -> {
                String name = code.getName();
                if (name != null && !name.isBlank()) {
                    yield name;
                }
                yield code.name();
            }
        };
    }

    /**
     * Checks if a given shortcut string conflicts with macOS system shortcuts.
     */
    public static String checkMacConflict(String shortcut) {
        if (shortcut == null || shortcut.isBlank()) return null;
        return MACOS_SYSTEM_SHORTCUTS.get(shortcut.trim());
    }
}
