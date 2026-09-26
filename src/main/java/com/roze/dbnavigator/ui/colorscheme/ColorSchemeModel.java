package com.roze.dbnavigator.ui.colorscheme;

import com.roze.dbnavigator.db.AppSettingsStore.ColorSchemeAttribute;

import java.util.*;

/**
 * DataGrip-identical Color Scheme model:
 * Organizes color scheme elements, categories, default color palettes,
 * and attribute inheritance resolution.
 */
public final class ColorSchemeModel {

    public static class ColorSchemeElement {
        private final String id;
        private final String name;
        private final String page;
        private final String category;
        private final String subCategory;
        private final List<String> categoryPath;
        private final ColorSchemeAttribute defaultAttr;
        private final String inheritFromKey;
        private final String inheritFromDisplay;

        public ColorSchemeElement(String id, String name, String category, String subCategory,
                                  ColorSchemeAttribute defaultAttr,
                                  String inheritFromKey, String inheritFromDisplay) {
            this.id = id;
            this.name = name;
            this.page = "General";
            this.category = category;
            this.subCategory = subCategory;
            this.categoryPath = subCategory != null ? List.of(category, subCategory, name) : List.of(category, name);
            this.defaultAttr = defaultAttr;
            this.inheritFromKey = inheritFromKey;
            this.inheritFromDisplay = inheritFromDisplay;
        }

        public ColorSchemeElement(String id, String name, List<String> categoryPath,
                                  ColorSchemeAttribute defaultAttr,
                                  String inheritFromKey, String inheritFromDisplay) {
            this(id, name, categoryPath, defaultAttr, inheritFromKey, inheritFromDisplay, "Language Defaults");
        }

        public ColorSchemeElement(String id, String name, List<String> categoryPath,
                                  ColorSchemeAttribute defaultAttr,
                                  String inheritFromKey, String inheritFromDisplay,
                                  String page) {
            this.id = id;
            this.name = name;
            this.page = page != null ? page : "General";
            this.categoryPath = categoryPath != null ? List.copyOf(categoryPath) : List.of(name);
            this.category = !this.categoryPath.isEmpty() ? this.categoryPath.get(0) : "";
            this.subCategory = this.categoryPath.size() > 2 ? this.categoryPath.get(1) : null;
            this.defaultAttr = defaultAttr;
            this.inheritFromKey = inheritFromKey;
            this.inheritFromDisplay = inheritFromDisplay;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getPage() { return page; }
        public String getCategory() { return category; }
        public String getSubCategory() { return subCategory; }
        public List<String> getCategoryPath() { return categoryPath; }
        public ColorSchemeAttribute getDefaultAttr() { return defaultAttr; }
        public String getInheritFromKey() { return inheritFromKey; }
        public String getInheritFromDisplay() { return inheritFromDisplay; }
        public boolean hasInheritance() { return inheritFromKey != null && !inheritFromKey.isBlank(); }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final List<ColorSchemeElement> ALL_ELEMENTS = new ArrayList<>();
    private static final Map<String, ColorSchemeElement> ELEMENT_MAP = new LinkedHashMap<>();

    static {
        // =========================================================================
        // 1. CODE
        // =========================================================================
        register(new ColorSchemeElement("code.identifier_caret", "Identifier under caret", "Code", null,
                new ColorSchemeAttribute(false, false, null, false, "344134", true, null, false, null, false, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("code.identifier_caret_write", "Identifier under caret (write)", "Code", null,
                new ColorSchemeAttribute(false, false, null, false, "40332B", true, null, false, null, false, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("code.injected_fragment", "Injected language fragment", "Code", null,
                new ColorSchemeAttribute(false, false, null, false, "363636", true, null, false, null, false, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("code.line_number", "Line number", "Code", null,
                new ColorSchemeAttribute(false, false, "4B5059", true, null, false, null, false, null, false, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("code.line_number_caret", "Line number on caret row", "Code", null,
                new ColorSchemeAttribute(false, false, "A4A3A3", true, null, false, null, false, null, false, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("code.matched_brace", "Matched brace", "Code", null,
                new ColorSchemeAttribute(false, false, null, false, "3B514D", true, null, false, "FFEF28", true, "Bold Underscored", false, null), null, null));
        register(new ColorSchemeElement("code.method_separator", "Method separator color", "Code", null,
                new ColorSchemeAttribute(false, false, "43454B", true, null, false, null, false, null, false, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("code.todo", "TODO defaults", "Code", null,
                new ColorSchemeAttribute(false, true, "A8C023", true, null, false, "73AD2B", true, null, false, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("code.unmatched_brace", "Unmatched brace", "Code", null,
                new ColorSchemeAttribute(false, false, "FF6B68", true, null, false, null, false, "FF6B68", true, "Underwaved", false, null), null, null));

        // =========================================================================
        // 2. EDITOR
        // =========================================================================
        register(new ColorSchemeElement("editor.bookmarks", "Bookmarks", "Editor", null,
                new ColorSchemeAttribute(false, false, null, false, null, false, "028567", true, null, false, "Underscored", false, null), null, null));

        // Breadcrumbs subcategory
        register(new ColorSchemeElement("editor.breadcrumbs.border", "Border", "Editor", "Breadcrumbs",
                new ColorSchemeAttribute(false, false, null, false, "323232", true, null, false, null, false, "Bordered", false, null), null, null));
        register(new ColorSchemeElement("editor.breadcrumbs.current", "Current", "Editor", "Breadcrumbs",
                new ColorSchemeAttribute(false, false, "DFE1E5", true, "2B2D30", true, null, false, null, false, "Bordered", false, null), null, null));
        register(new ColorSchemeElement("editor.breadcrumbs.default", "Default", "Editor", "Breadcrumbs",
                new ColorSchemeAttribute(false, false, "787878", true, "2B2D30", true, null, false, null, false, "Bordered", false, null), null, null));
        register(new ColorSchemeElement("editor.breadcrumbs.hovered", "Hovered", "Editor", "Breadcrumbs",
                new ColorSchemeAttribute(false, false, "DFE1E5", true, "383A3C", true, null, false, null, false, "Bordered", false, null), null, null));
        register(new ColorSchemeElement("editor.breadcrumbs.inactive", "Inactive", "Editor", "Breadcrumbs",
                new ColorSchemeAttribute(false, false, "5A5D60", true, "2B2D30", true, null, false, null, false, "Bordered", false, null), null, null));

        register(new ColorSchemeElement("editor.caret", "Caret", "Editor", null,
                new ColorSchemeAttribute(false, false, "FFFFFF", true, null, false, null, false, null, false, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("editor.caret_row", "Caret row", "Editor", null,
                new ColorSchemeAttribute(false, false, null, false, "26282E", true, null, false, null, false, "Underscored", false, null), null, null));

        // Guides subcategory
        register(new ColorSchemeElement("editor.guides.hard_wrap", "Hard wrap guide", "Editor", "Guides",
                new ColorSchemeAttribute(false, false, "393B40", true, "393B40", true, null, false, null, false, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("editor.guides.indent", "Indent guide", "Editor", "Guides",
                new ColorSchemeAttribute(false, false, null, false, "313438", true, null, false, null, false, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("editor.guides.indent_selected", "Indent guide selected", "Editor", "Guides",
                new ColorSchemeAttribute(false, false, null, false, "4B5059", true, null, false, null, false, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("editor.guides.matched_brace", "Matched brace guide", "Editor", "Guides",
                new ColorSchemeAttribute(false, false, null, false, "3B514D", true, null, false, null, false, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("editor.guides.visual", "Visual guides", "Editor", "Guides",
                new ColorSchemeAttribute(false, false, "323232", true, null, false, null, false, null, false, "Underscored", false, null), null, null));

        register(new ColorSchemeElement("editor.gutter_bg", "Gutter background", "Editor", null,
                new ColorSchemeAttribute(false, false, null, false, "1E1F22", true, null, false, null, false, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("editor.notification_bg", "Notification background", "Editor", null,
                new ColorSchemeAttribute(false, false, null, false, "2D2F31", true, null, false, null, false, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("editor.selection_bg", "Selection background", "Editor", null,
                new ColorSchemeAttribute(false, false, null, false, "214283", true, null, false, null, false, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("editor.selection_fg", "Selection foreground", "Editor", null,
                new ColorSchemeAttribute(false, false, null, false, null, false, null, false, null, false, "Underscored", false, null), null, null));

        // Sticky Lines subcategory
        register(new ColorSchemeElement("editor.sticky_lines.bg", "Background", "Editor", "Sticky Lines",
                new ColorSchemeAttribute(false, false, null, false, "26282E", true, null, false, null, false, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("editor.sticky_lines.border", "Border", "Editor", "Sticky Lines",
                new ColorSchemeAttribute(false, false, null, false, "393B40", true, null, false, null, false, "Underscored", true, "editor.guides.hard_wrap"),
                "editor.guides.hard_wrap", "Editor → Guides → Hard wrap guide (General)"));
        register(new ColorSchemeElement("editor.sticky_lines.hovered", "Hovered", "Editor", "Sticky Lines",
                new ColorSchemeAttribute(false, false, null, false, "303236", true, null, false, null, false, "Underscored", false, null), null, null));

        // Tabs subcategory
        register(new ColorSchemeElement("editor.tabs.modified_icon", "Modified icon color", "Editor", "Tabs",
                new ColorSchemeAttribute(false, false, "4083C9", true, null, false, null, false, null, false, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("editor.tabs.selected", "Selected Tab", "Editor", "Tabs",
                new ColorSchemeAttribute(false, false, "DFE1E5", true, "1E1F22", true, null, false, null, false, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("editor.tabs.selected_inactive", "Selected Tab inactive", "Editor", "Tabs",
                new ColorSchemeAttribute(false, false, "868A91", true, "2B2D30", true, null, false, null, false, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("editor.tabs.underline", "Underline", "Editor", "Tabs",
                new ColorSchemeAttribute(false, false, "3574F0", true, null, false, null, false, null, false, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("editor.tabs.underline_inactive", "Underline inactive", "Editor", "Tabs",
                new ColorSchemeAttribute(false, false, "5A5D60", true, null, false, null, false, null, false, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("editor.tabs.tear_line", "Tear line", "Editor", "Tabs",
                new ColorSchemeAttribute(false, false, "43454A", true, null, false, null, false, null, false, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("editor.tabs.tear_line_selection", "Tear line selection", "Editor", "Tabs",
                new ColorSchemeAttribute(false, false, "3574F0", true, null, false, null, false, null, false, "Underscored", false, null), null, null));

        // Vertical Scrollbar subcategory
        register(new ColorSchemeElement("editor.scrollbar.thumb", "Thumb", "Editor", "Vertical Scrollbar",
                new ColorSchemeAttribute(false, false, null, false, "4E5157", true, null, false, null, false, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("editor.scrollbar.thumb_scrolling", "Thumb while scrolling", "Editor", "Vertical Scrollbar",
                new ColorSchemeAttribute(false, false, null, false, "FFFFFF", true, null, false, null, false, "Underscored", false, null), null, null));

        // =========================================================================
        // 3. ERRORS AND WARNINGS
        // =========================================================================
        register(new ColorSchemeElement("errors.deprecated", "Deprecated symbol", "Errors and Warnings", null,
                new ColorSchemeAttribute(false, false, null, false, null, false, null, false, "868A91", true, "Strikeout", false, null), null, null));
        register(new ColorSchemeElement("errors.deprecated_marked_for_removal", "Deprecated symbol marked for removal", "Errors and Warnings", null,
                new ColorSchemeAttribute(false, false, null, false, null, false, "F75464", true, "F75464", true, "Strikeout", false, null), null, null));
        register(new ColorSchemeElement("errors.duplicate_from_server", "Duplicate from server", "Errors and Warnings", null,
                new ColorSchemeAttribute(false, false, null, false, "544630", true, null, false, null, false, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("errors.error", "Error", "Errors and Warnings", null,
                new ColorSchemeAttribute(false, false, null, false, null, false, "BC3F3C", true, "BC3F3C", true, "Underwaved", false, null), null, null));
        register(new ColorSchemeElement("errors.grammar_error", "Grammar error", "Errors and Warnings", null,
                new ColorSchemeAttribute(false, false, null, false, null, false, "659C6B", true, "659C6B", true, "Underwaved", false, null), null, null));
        register(new ColorSchemeElement("errors.problem_from_server", "Problem from server", "Errors and Warnings", null,
                new ColorSchemeAttribute(false, false, null, false, null, false, null, false, "F4AF3D", true, "Underwaved", false, null), null, null));
        register(new ColorSchemeElement("errors.runtime_problem", "Runtime problem", "Errors and Warnings", null,
                new ColorSchemeAttribute(false, false, null, false, null, false, "E05555", true, "E05555", true, "Underwaved", false, null), null, null));
        register(new ColorSchemeElement("errors.text_style_error", "Text style error", "Errors and Warnings", null,
                new ColorSchemeAttribute(false, false, null, false, null, false, "BC3F3C", true, "BC3F3C", true, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("errors.text_style_suggestion", "Text style suggestion", "Errors and Warnings", null,
                new ColorSchemeAttribute(false, false, null, false, null, false, null, false, "868A91", true, "Dotted line", false, null), null, null));
        register(new ColorSchemeElement("errors.text_style_warning", "Text style warning", "Errors and Warnings", null,
                new ColorSchemeAttribute(false, false, null, false, null, false, "F4AF3D", true, "F4AF3D", true, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("errors.typo", "Typo", "Errors and Warnings", null,
                new ColorSchemeAttribute(false, false, null, false, null, false, "659C6B", true, "659C6B", true, "Underwaved", false, null), null, null));
        register(new ColorSchemeElement("errors.unknown_symbol", "Unknown symbol", "Errors and Warnings", null,
                new ColorSchemeAttribute(false, false, "BC3F3C", true, null, false, "BC3F3C", true, "BC3F3C", true, "Underwaved", false, null), null, null));
        register(new ColorSchemeElement("errors.unused", "Unused code", "Errors and Warnings", null,
                new ColorSchemeAttribute(false, false, "72737A", true, null, false, null, false, null, false, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("errors.warning", "Warning", "Errors and Warnings", null,
                new ColorSchemeAttribute(false, false, null, false, null, false, "F4AF3D", true, "F4AF3D", true, "Underwaved", false, null), null, null));
        register(new ColorSchemeElement("errors.weak_warning", "Weak Warning", "Errors and Warnings", null,
                new ColorSchemeAttribute(false, false, null, false, null, false, "756D56", true, "756D56", true, "Underwaved", false, null), null, null));

        // =========================================================================
        // 4. HYPERLINKS
        // =========================================================================
        register(new ColorSchemeElement("hyperlinks.followed", "Followed", "Hyperlinks", null,
                new ColorSchemeAttribute(false, false, "68418C", true, null, false, null, false, "68418C", true, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("hyperlinks.inactive", "Inactive", "Hyperlinks", null,
                new ColorSchemeAttribute(false, false, null, false, null, false, null, false, "6B6C73", true, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("hyperlinks.reference", "Reference", "Hyperlinks", null,
                new ColorSchemeAttribute(false, false, "589DF6", true, null, false, null, false, "589DF6", true, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("hyperlinks.unfollowed", "Unfollowed", "Hyperlinks", null,
                new ColorSchemeAttribute(false, false, "287BDE", true, null, false, null, false, "287BDE", true, "Underscored", false, null), null, null));

        // =========================================================================
        // 5. IDENTIFIERS
        // =========================================================================
        register(new ColorSchemeElement("identifiers.reassigned_local_variable", "Reassigned local variable", "Identifiers", null,
                new ColorSchemeAttribute(false, false, null, false, null, false, null, false, null, false, "Bordered", false, null), null, null));

        // =========================================================================
        // 6. LINE COVERAGE
        // =========================================================================
        register(new ColorSchemeElement("coverage.full", "Full", "Line Coverage", null,
                new ColorSchemeAttribute(false, false, "485848", true, null, false, null, false, null, false, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("coverage.partial", "Partial", "Line Coverage", null,
                new ColorSchemeAttribute(true, false, "5E4D33", true, null, false, null, false, null, false, "Bordered", false, null), null, null));
        register(new ColorSchemeElement("coverage.uncovered", "Uncovered", "Line Coverage", null,
                new ColorSchemeAttribute(false, false, "704040", true, null, false, null, false, null, false, "Underscored", false, null), null, null));

        // =========================================================================
        // 7. LIVE TEMPLATES
        // =========================================================================
        register(new ColorSchemeElement("templates.active", "Active Segment", "Live Templates", null,
                new ColorSchemeAttribute(false, false, null, false, null, false, null, false, "3574F0", true, "Bordered", false, null), null, null));
        register(new ColorSchemeElement("templates.inactive", "Inactive Segment", "Live Templates", null,
                new ColorSchemeAttribute(false, false, null, false, null, false, null, false, "9DA0A8", true, "Bordered", false, null), null, null));
        register(new ColorSchemeElement("templates.variable", "Template Variable", "Live Templates", null,
                new ColorSchemeAttribute(false, true, "9876AA", true, null, false, null, false, null, false, "Underscored", false, null), null, null));

        // =========================================================================
        // 8. POPUPS AND HINTS (11 items in exact DataGrip alphabetical order)
        // =========================================================================
        register(new ColorSchemeElement("popups.code_lens", "Code lens", "Popups and Hints", null,
                new ColorSchemeAttribute(false, false, "787878", true, null, false, null, false, null, false, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("popups.completion", "Completion", "Popups and Hints", null,
                new ColorSchemeAttribute(false, false, null, false, "2B2D30", true, null, false, null, false, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("popups.doc", "Documentation", "Popups and Hints", null,
                new ColorSchemeAttribute(false, false, "DFE1E5", true, "2B2D30", true, null, false, null, false, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("popups.error_hint", "Error hint", "Popups and Hints", null,
                new ColorSchemeAttribute(false, false, null, false, "40252B", true, null, false, null, false, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("popups.hint_border", "Hint border", "Popups and Hints", null,
                new ColorSchemeAttribute(false, false, null, false, "323232", true, null, false, null, false, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("popups.info_hint", "Information hint", "Popups and Hints", null,
                new ColorSchemeAttribute(false, false, null, false, "2B2D30", true, null, false, null, false, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("popups.promotion_pane", "Promotion pane", "Popups and Hints", null,
                new ColorSchemeAttribute(false, false, null, false, "2B2D30", true, null, false, null, false, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("popups.question_hint", "Question hint", "Popups and Hints", null,
                new ColorSchemeAttribute(false, false, null, false, "25324D", true, null, false, null, false, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("popups.recent_locations_selection", "Recent locations selection", "Popups and Hints", null,
                new ColorSchemeAttribute(false, false, null, false, "26282E", true, null, false, null, false, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("popups.tooltip", "Tooltip", "Popups and Hints", null,
                new ColorSchemeAttribute(false, false, "DFE1E5", true, "2B2D30", true, null, false, null, false, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("popups.warning_hint", "Warning hint", "Popups and Hints", null,
                new ColorSchemeAttribute(false, false, null, false, "3B3525", true, null, false, null, false, "Underscored", false, null), null, null));

        // =========================================================================
        // 9. PREVIEW (Background, Border inheriting from Indent guide)
        // =========================================================================
        register(new ColorSchemeElement("preview.background", "Background", "Preview", null,
                new ColorSchemeAttribute(false, false, null, false, "1E1F22", true, null, false, null, false, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("preview.border", "Border", "Preview", null,
                new ColorSchemeAttribute(false, false, null, false, "313438", true, null, false, null, false, "Underscored", true, "editor.guides.indent"),
                "editor.guides.indent", "Editor → Guides → Indent guide (General)"));

        // =========================================================================
        // 10. SEARCH RESULTS
        // =========================================================================
        register(new ColorSchemeElement("search.result", "Search result", "Search Results", null,
                new ColorSchemeAttribute(false, false, null, false, "32593D", true, "57965C", true, null, false, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("search.result_write", "Search result (write access)", "Search Results", null,
                new ColorSchemeAttribute(false, false, null, false, "66313F", true, "FA7DB1", true, null, false, "Bordered", false, null), null, null));
        register(new ColorSchemeElement("search.text_result", "Text search result", "Search Results", null,
                new ColorSchemeAttribute(false, false, null, false, "32593D", true, "57965C", true, null, false, "Underscored", false, null), null, null));

        // =========================================================================
        // 11. TEXT (all 9 items in exact DataGrip alphabetical order)
        // =========================================================================
        register(new ColorSchemeElement("text.readonly_bg", "Background in read-only files", "Text", null,
                new ColorSchemeAttribute(false, false, null, false, "2B2D30", true, null, false, null, false, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("text.default", "Default text", "Text", null,
                new ColorSchemeAttribute(false, false, "A9B7C6", true, "1E1F22", true, null, false, null, false, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("text.deleted", "Deleted text", "Text", null,
                new ColorSchemeAttribute(false, false, "CC666E", true, null, false, "CC666E", true, "CC666E", true, "Strikeout", false, null), null, null));
        register(new ColorSchemeElement("text.folded", "Folded text", "Text", null,
                new ColorSchemeAttribute(false, false, "8C8C8C", true, "3A3A3A", true, null, false, null, false, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("text.folded_highlighted", "Folded text with highlighting", "Text", null,
                new ColorSchemeAttribute(false, false, null, false, "2B2D30", true, null, false, null, false, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("text.readonly_fragment_bg", "Read-only fragment background", "Text", null,
                new ColorSchemeAttribute(false, false, null, false, "282A2E", true, null, false, null, false, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("text.soft_wrap_sign", "Soft wrap sign", "Text", null,
                new ColorSchemeAttribute(false, false, "4B5059", true, null, false, null, false, null, false, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("text.tabs", "Tabs", "Text", null,
                new ColorSchemeAttribute(false, false, "4B5059", true, null, false, null, false, null, false, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("text.whitespaces", "Whitespaces", "Text", null,
                new ColorSchemeAttribute(false, false, "4B5059", true, null, false, null, false, null, false, "Underscored", false, null), null, null));

        // =========================================================================
        // LANGUAGE DEFAULTS (Exact DataGrip taxonomy and palettes)
        // =========================================================================

        // 1. Bad character
        register(new ColorSchemeElement("lang.bad_character", "Bad character",
                List.of("Bad character"),
                new ColorSchemeAttribute(false, false, "F75464", true, null, false, null, false, null, false, "Underwaved", false, null), null, null));

        // 2. Braces and Operators
        register(new ColorSchemeElement("lang.braces_and_operators.braces", "Braces",
                List.of("Braces and Operators", "Braces"),
                new ColorSchemeAttribute(false, false, "BCBEC4", true, null, false, null, false, null, false, "Bordered", false, null), null, null));
        register(new ColorSchemeElement("lang.braces_and_operators.brackets", "Brackets",
                List.of("Braces and Operators", "Brackets"),
                new ColorSchemeAttribute(false, false, "BCBEC4", true, null, false, null, false, null, false, "Bordered", false, null), null, null));
        register(new ColorSchemeElement("lang.braces_and_operators.comma", "Comma",
                List.of("Braces and Operators", "Comma"),
                new ColorSchemeAttribute(false, false, "BCBEC4", true, null, false, null, false, null, false, "Bordered", false, null), null, null));
        register(new ColorSchemeElement("lang.braces_and_operators.dot", "Dot",
                List.of("Braces and Operators", "Dot"),
                new ColorSchemeAttribute(false, false, "BCBEC4", true, null, false, null, false, null, false, "Bordered", false, null), null, null));
        register(new ColorSchemeElement("lang.braces_and_operators.operation_sign", "Operation sign",
                List.of("Braces and Operators", "Operation sign"),
                new ColorSchemeAttribute(false, false, "BCBEC4", true, null, false, null, false, null, false, "Bordered", false, null), null, null));
        register(new ColorSchemeElement("lang.braces_and_operators.parentheses", "Parentheses",
                List.of("Braces and Operators", "Parentheses"),
                new ColorSchemeAttribute(false, false, "BCBEC4", true, null, false, null, false, null, false, "Bordered", false, null), null, null));
        register(new ColorSchemeElement("lang.braces_and_operators.semicolon", "Semicolon",
                List.of("Braces and Operators", "Semicolon"),
                new ColorSchemeAttribute(false, false, "BCBEC4", true, null, false, null, false, null, false, "Bordered", false, null), null, null));

        // 3. Classes
        register(new ColorSchemeElement("lang.classes.class_name", "Class name",
                List.of("Classes", "Class name"),
                new ColorSchemeAttribute(false, false, "56A8F5", true, null, false, null, false, null, false, "Bordered", false, null), null, null));
        register(new ColorSchemeElement("lang.classes.class_reference", "Class reference",
                List.of("Classes", "Class reference"),
                new ColorSchemeAttribute(false, false, "BCBEC4", true, null, false, null, false, null, false, "Bordered", false, null), null, null));
        register(new ColorSchemeElement("lang.classes.instance_field", "Instance field",
                List.of("Classes", "Instance field"),
                new ColorSchemeAttribute(false, false, "C77DBB", true, null, false, null, false, null, false, "Bordered", true, "lang.identifiers.default"),
                "lang.identifiers.default", "Identifiers → Default (Language Defaults)"));
        register(new ColorSchemeElement("lang.classes.instance_method", "Instance method",
                List.of("Classes", "Instance method"),
                new ColorSchemeAttribute(false, false, "56A8F5", true, null, false, null, false, null, false, "Bordered", false, null), null, null));
        register(new ColorSchemeElement("lang.classes.interface_name", "Interface name",
                List.of("Classes", "Interface name"),
                new ColorSchemeAttribute(false, false, "BCBEC4", true, null, false, null, false, null, false, "Bordered", false, null), null, null));
        register(new ColorSchemeElement("lang.classes.static_field", "Static field",
                List.of("Classes", "Static field"),
                new ColorSchemeAttribute(false, true, "C77DBB", true, null, false, null, false, null, false, "Bordered", false, null), null, null));
        register(new ColorSchemeElement("lang.classes.static_method", "Static method",
                List.of("Classes", "Static method"),
                new ColorSchemeAttribute(false, true, "56A8F5", true, null, false, null, false, null, false, "Bordered", false, null), null, null));

        // 4. Comments
        register(new ColorSchemeElement("lang.comments.block_comment", "Block comment",
                List.of("Comments", "Block comment"),
                new ColorSchemeAttribute(false, false, "7A7E85", true, null, false, null, false, null, false, "Bordered", false, null), null, null));
        register(new ColorSchemeElement("lang.comments.doc.code_block", "Code block",
                List.of("Comments", "Doc comment", "Code block"),
                new ColorSchemeAttribute(false, false, "BCBEC4", true, null, false, null, false, null, false, "Bordered", false, null), null, null));
        register(new ColorSchemeElement("lang.comments.doc.inline_code", "Inline code fragment",
                List.of("Comments", "Doc comment", "Inline code fragment"),
                new ColorSchemeAttribute(false, false, "BCBEC4", true, null, false, null, false, null, false, "Bordered", false, null), null, null));
        register(new ColorSchemeElement("lang.comments.doc.link", "Link in rendered view",
                List.of("Comments", "Doc comment", "Link in rendered view"),
                new ColorSchemeAttribute(false, false, "3887A1", true, null, false, null, false, null, false, "Underscored", false, null), null, null));
        register(new ColorSchemeElement("lang.comments.doc.markup", "Markup",
                List.of("Comments", "Doc comment", "Markup"),
                new ColorSchemeAttribute(false, false, "629755", true, null, false, null, false, null, false, "Bordered", false, null), null, null));
        register(new ColorSchemeElement("lang.comments.doc.shortcut", "Shortcut",
                List.of("Comments", "Doc comment", "Shortcut"),
                new ColorSchemeAttribute(false, false, "7A7E85", true, null, false, null, false, null, false, "Bordered", false, null), null, null));
        register(new ColorSchemeElement("lang.comments.doc.tag", "Tag",
                List.of("Comments", "Doc comment", "Tag"),
                new ColorSchemeAttribute(false, true, "629755", true, null, false, null, false, null, false, "Bordered", false, null), null, null));
        register(new ColorSchemeElement("lang.comments.doc.tag_value", "Tag value",
                List.of("Comments", "Doc comment", "Tag value"),
                new ColorSchemeAttribute(false, false, "629755", true, null, false, null, false, null, false, "Bordered", false, null), null, null));
        register(new ColorSchemeElement("lang.comments.doc.text", "Text",
                List.of("Comments", "Doc comment", "Text"),
                new ColorSchemeAttribute(false, true, "629755", true, null, false, null, false, null, false, "Bordered", false, null), null, null));
        register(new ColorSchemeElement("lang.comments.doc.guide", "Vertical guide for rendered view",
                List.of("Comments", "Doc comment", "Vertical guide for rendered view"),
                new ColorSchemeAttribute(false, false, "393B40", true, null, false, null, false, null, false, "Bordered", false, null), null, null));
        register(new ColorSchemeElement("lang.comments.line_comment", "Line comment",
                List.of("Comments", "Line comment"),
                new ColorSchemeAttribute(false, false, "7A7E85", true, null, false, null, false, null, false, "Bordered", false, null), null, null));

        // 5. Identifiers
        register(new ColorSchemeElement("lang.identifiers.constant", "Constant",
                List.of("Identifiers", "Constant"),
                new ColorSchemeAttribute(false, true, "C77DBB", true, null, false, null, false, null, false, "Bordered", false, null), null, null));
        register(new ColorSchemeElement("lang.identifiers.default", "Default",
                List.of("Identifiers", "Default"),
                new ColorSchemeAttribute(false, false, "BCBEC4", true, null, false, null, false, null, false, "Bordered", true, "text.default"),
                "text.default", "Text → Default text (General)"));
        register(new ColorSchemeElement("lang.identifiers.function_call", "Function call",
                List.of("Identifiers", "Function call"),
                new ColorSchemeAttribute(false, false, "56A8F5", true, null, false, null, false, null, false, "Bordered", false, null), null, null));
        register(new ColorSchemeElement("lang.identifiers.function_declaration", "Function declaration",
                List.of("Identifiers", "Function declaration"),
                new ColorSchemeAttribute(false, false, "56A8F5", true, null, false, null, false, null, false, "Bordered", false, null), null, null));
        register(new ColorSchemeElement("lang.identifiers.global_variable", "Global variable",
                List.of("Identifiers", "Global variable"),
                new ColorSchemeAttribute(false, false, "BCBEC4", true, null, false, null, false, null, false, "Bordered", false, null), null, null));
        register(new ColorSchemeElement("lang.identifiers.label", "Label",
                List.of("Identifiers", "Label"),
                new ColorSchemeAttribute(false, false, "BCBEC4", true, null, false, null, false, null, false, "Bordered", false, null), null, null));
        register(new ColorSchemeElement("lang.identifiers.local_variable", "Local variable",
                List.of("Identifiers", "Local variable"),
                new ColorSchemeAttribute(false, false, "BCBEC4", true, null, false, null, false, null, false, "Bordered", false, null), null, null));
        register(new ColorSchemeElement("lang.identifiers.parameter", "Parameter",
                List.of("Identifiers", "Parameter"),
                new ColorSchemeAttribute(false, false, "BCBEC4", true, null, false, null, false, null, false, "Bordered", false, null), null, null));
        register(new ColorSchemeElement("lang.identifiers.predefined_symbol", "Predefined symbol",
                List.of("Identifiers", "Predefined symbol"),
                new ColorSchemeAttribute(false, false, "56A8F5", true, null, false, null, false, null, false, "Bordered", false, null), null, null));
        register(new ColorSchemeElement("lang.identifiers.reassigned_local_variable", "Reassigned local variable",
                List.of("Identifiers", "Reassigned local variable"),
                new ColorSchemeAttribute(false, false, "BCBEC4", true, null, false, null, false, "3574F0", true, "Bordered", false, null), null, null));
        register(new ColorSchemeElement("lang.identifiers.reassigned_parameter", "Reassigned parameter",
                List.of("Identifiers", "Reassigned parameter"),
                new ColorSchemeAttribute(false, false, "BCBEC4", true, null, false, null, false, "3574F0", true, "Bordered", false, null), null, null));

        // 6. Inline hints
        register(new ColorSchemeElement("lang.inline_hints.code_vision", "Code vision group",
                List.of("Inline hints", "Code vision group"),
                new ColorSchemeAttribute(false, false, "868A91", true, null, false, null, false, null, false, "Bordered", false, null), null, null));
        register(new ColorSchemeElement("lang.inline_hints.current_param", "Current parameter hint",
                List.of("Inline hints", "Current parameter hint"),
                new ColorSchemeAttribute(false, false, "DFE1E5", true, "2E436E", true, null, false, null, false, "Bordered", false, null), null, null));
        register(new ColorSchemeElement("lang.inline_hints.default", "Default",
                List.of("Inline hints", "Default"),
                new ColorSchemeAttribute(false, false, "868A91", true, "393B40", true, null, false, null, false, "Bordered", false, null), null, null));
        register(new ColorSchemeElement("lang.inline_hints.parameter_hint", "Parameter hint",
                List.of("Inline hints", "Parameter hint"),
                new ColorSchemeAttribute(false, false, "868A91", true, "393B40", true, null, false, null, false, "Bordered", false, null), null, null));

        // 7. Keyword
        register(new ColorSchemeElement("lang.keyword", "Keyword",
                List.of("Keyword"),
                new ColorSchemeAttribute(false, false, "CF8E6D", true, null, false, null, false, null, false, "Bordered", false, null), null, null));

        // 8. Markup
        register(new ColorSchemeElement("lang.markup.attribute", "Attribute",
                List.of("Markup", "Attribute"),
                new ColorSchemeAttribute(false, false, "BCBEC4", true, null, false, null, false, null, false, "Bordered", false, null), null, null));
        register(new ColorSchemeElement("lang.markup.entity", "Entity",
                List.of("Markup", "Entity"),
                new ColorSchemeAttribute(false, false, "2AACB8", true, null, false, null, false, null, false, "Bordered", false, null), null, null));
        register(new ColorSchemeElement("lang.markup.tag", "Tag",
                List.of("Markup", "Tag"),
                new ColorSchemeAttribute(false, false, "CF8E6D", true, null, false, null, false, null, false, "Bordered", false, null), null, null));

        // 9. Metadata
        register(new ColorSchemeElement("lang.metadata", "Metadata",
                List.of("Metadata"),
                new ColorSchemeAttribute(false, false, "B3AE60", true, null, false, null, false, null, false, "Bordered", false, null), null, null));

        // 10. Number
        register(new ColorSchemeElement("lang.number", "Number",
                List.of("Number"),
                new ColorSchemeAttribute(false, false, "2AACB8", true, null, false, null, false, null, false, "Bordered", false, null), null, null));

        // 11. Semantic highlighting
        register(new ColorSchemeElement("lang.semantic_highlighting", "Semantic highlighting",
                List.of("Semantic highlighting"),
                new ColorSchemeAttribute(false, false, "BCBEC4", true, null, false, null, false, null, false, "Bordered", false, null), null, null));

        // 12. String
        register(new ColorSchemeElement("lang.string.escape.invalid", "Invalid",
                List.of("String", "Escape sequence", "Invalid"),
                new ColorSchemeAttribute(false, false, "CF8E6D", true, null, false, null, false, "F75464", true, "Underwaved", false, null), null, null));
        register(new ColorSchemeElement("lang.string.escape.valid", "Valid",
                List.of("String", "Escape sequence", "Valid"),
                new ColorSchemeAttribute(false, false, "CF8E6D", true, null, false, null, false, null, false, "Bordered", false, null), null, null));
        register(new ColorSchemeElement("lang.string.text", "String text",
                List.of("String", "String text"),
                new ColorSchemeAttribute(false, false, "6AAB73", true, null, false, null, false, null, false, "Bordered", false, null), null, null));

        // 13. Template language
        register(new ColorSchemeElement("lang.template_language", "Template language",
                List.of("Template language"),
                new ColorSchemeAttribute(false, false, "E09553", true, null, false, null, false, null, false, "Bordered", false, null), null, null));

        // =========================================================================
        // CONSOLE COLORS (DataGrip Alignment)
        // =========================================================================
        // 1. ANSI colors
        register(new ColorSchemeElement("console.ansi.black", "Black",
                List.of("ANSI colors", "Black"),
                new ColorSchemeAttribute(false, false, "000000", true, "000000", true, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.ansi.blue", "Blue",
                List.of("ANSI colors", "Blue"),
                new ColorSchemeAttribute(false, false, "3574F0", true, "244B9E", true, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.ansi.bright_black", "Bright Black",
                List.of("ANSI colors", "Bright Black"),
                new ColorSchemeAttribute(false, false, "6C707E", true, "4E5157", true, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.ansi.bright_blue", "Bright Blue",
                List.of("ANSI colors", "Bright Blue"),
                new ColorSchemeAttribute(false, false, "589DF6", true, "3574F0", true, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.ansi.bright_cyan", "Bright Cyan",
                List.of("ANSI colors", "Bright Cyan"),
                new ColorSchemeAttribute(false, false, "46B8DF", true, "288DB3", true, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.ansi.bright_green", "Bright Green",
                List.of("ANSI colors", "Bright Green"),
                new ColorSchemeAttribute(false, false, "4FC414", false, "458500", false, null, false, null, false, "Underscored", true, "console.ansi.green"),
                "console.ansi.green", "ANSI colors → Green (Console Colors)", "Console Colors"));
        register(new ColorSchemeElement("console.ansi.bright_magenta", "Bright Magenta",
                List.of("ANSI colors", "Bright Magenta"),
                new ColorSchemeAttribute(false, false, "C77DBB", true, "8E5283", true, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.ansi.bright_red", "Bright Red",
                List.of("ANSI colors", "Bright Red"),
                new ColorSchemeAttribute(false, false, "F75464", true, "B33644", true, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.ansi.bright_white", "Bright White",
                List.of("ANSI colors", "Bright White"),
                new ColorSchemeAttribute(false, false, "FFFFFF", true, "DFE1E5", true, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.ansi.bright_yellow", "Bright Yellow",
                List.of("ANSI colors", "Bright Yellow"),
                new ColorSchemeAttribute(false, false, "E5B842", true, "A68326", true, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.ansi.cyan", "Cyan",
                List.of("ANSI colors", "Cyan"),
                new ColorSchemeAttribute(false, false, "2BBAC5", true, "1B7C84", true, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.ansi.green", "Green",
                List.of("ANSI colors", "Green"),
                new ColorSchemeAttribute(false, false, "57965C", true, "32593D", true, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.ansi.magenta", "Magenta",
                List.of("ANSI colors", "Magenta"),
                new ColorSchemeAttribute(false, false, "AE67A0", true, "743F6A", true, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.ansi.red", "Red",
                List.of("ANSI colors", "Red"),
                new ColorSchemeAttribute(false, false, "BC3F3C", true, "7A2826", true, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.ansi.white", "White (Gray)",
                List.of("ANSI colors", "White (Gray)"),
                new ColorSchemeAttribute(false, false, "A9B7C6", true, "5A5D60", true, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.ansi.yellow", "Yellow",
                List.of("ANSI colors", "Yellow"),
                new ColorSchemeAttribute(false, false, "C19C00", true, "806700", true, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));

        // 2. Console
        register(new ColorSchemeElement("console.background", "Background",
                List.of("Console", "Background"),
                new ColorSchemeAttribute(false, false, null, false, "1E1F22", true, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.error_output", "Error output",
                List.of("Console", "Error output"),
                new ColorSchemeAttribute(false, false, "F75464", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.standard_output", "Standard output",
                List.of("Console", "Standard output"),
                new ColorSchemeAttribute(false, false, "BCBEC4", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.system_output", "System output",
                List.of("Console", "System output"),
                new ColorSchemeAttribute(false, false, "A9B7C6", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.user_input", "User input",
                List.of("Console", "User input"),
                new ColorSchemeAttribute(false, true, "6AAB73", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));

        // 3. Log console
        register(new ColorSchemeElement("console.log.error", "Error",
                List.of("Log console", "Error"),
                new ColorSchemeAttribute(false, false, "F75464", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.log.warning", "Warning",
                List.of("Log console", "Warning"),
                new ColorSchemeAttribute(false, false, "E5B842", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.log.info", "Info",
                List.of("Log console", "Info"),
                new ColorSchemeAttribute(false, false, "E5B842", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.log.verbose", "Verbose",
                List.of("Log console", "Verbose"),
                new ColorSchemeAttribute(false, false, "589DF6", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.log.debug", "Debug",
                List.of("Log console", "Debug"),
                new ColorSchemeAttribute(false, false, "2BBAC5", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.log.expired", "Expired entry",
                List.of("Log console", "Expired entry"),
                new ColorSchemeAttribute(false, false, "6C707E", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));

        // 4. Reworked terminal (all 34 items in DataGrip alphabetical order)
        register(new ColorSchemeElement("console.reworked_terminal.black", "Black",
                List.of("Reworked terminal", "Black"),
                new ColorSchemeAttribute(false, false, "000000", true, "000000", true, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.reworked_terminal.blue", "Blue",
                List.of("Reworked terminal", "Blue"),
                new ColorSchemeAttribute(false, false, "5594FA", true, "134EBF", true, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.reworked_terminal.bright_black", "Bright Black",
                List.of("Reworked terminal", "Bright Black"),
                new ColorSchemeAttribute(false, false, "6C707E", true, "4E5157", true, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.reworked_terminal.bright_blue", "Bright Blue",
                List.of("Reworked terminal", "Bright Blue"),
                new ColorSchemeAttribute(false, false, "589DF6", true, "3574F0", true, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.reworked_terminal.bright_cyan", "Bright Cyan",
                List.of("Reworked terminal", "Bright Cyan"),
                new ColorSchemeAttribute(false, false, "46B8DF", true, "288DB3", true, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.reworked_terminal.bright_green", "Bright Green",
                List.of("Reworked terminal", "Bright Green"),
                new ColorSchemeAttribute(false, false, "4FC414", true, "458500", true, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.reworked_terminal.bright_magenta", "Bright Magenta",
                List.of("Reworked terminal", "Bright Magenta"),
                new ColorSchemeAttribute(false, false, "C77DBB", true, "8E5283", true, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.reworked_terminal.bright_red", "Bright Red",
                List.of("Reworked terminal", "Bright Red"),
                new ColorSchemeAttribute(false, false, "F75464", true, "B33644", true, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.reworked_terminal.bright_white", "Bright White",
                List.of("Reworked terminal", "Bright White"),
                new ColorSchemeAttribute(false, false, "FFFFFF", true, "DFE1E5", true, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.reworked_terminal.bright_yellow", "Bright Yellow",
                List.of("Reworked terminal", "Bright Yellow"),
                new ColorSchemeAttribute(false, false, "E5B842", true, "A68326", true, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.reworked_terminal.command", "Command",
                List.of("Reworked terminal", "Command"),
                new ColorSchemeAttribute(false, false, "BCBEC4", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.reworked_terminal.current_search_entry", "Current search entry",
                List.of("Reworked terminal", "Current search entry"),
                new ColorSchemeAttribute(false, false, null, false, "214283", true, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.reworked_terminal.cyan", "Cyan",
                List.of("Reworked terminal", "Cyan"),
                new ColorSchemeAttribute(false, false, "2BBAC5", true, "1B7C84", true, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.reworked_terminal.default_background", "Default background",
                List.of("Reworked terminal", "Default background"),
                new ColorSchemeAttribute(false, false, null, false, "1E1F22", true, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.reworked_terminal.default_foreground", "Default foreground",
                List.of("Reworked terminal", "Default foreground"),
                new ColorSchemeAttribute(false, false, "BCBEC4", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.reworked_terminal.error_block_border", "Error block border",
                List.of("Reworked terminal", "Error block border"),
                new ColorSchemeAttribute(false, false, null, false, null, false, null, false, "F75464", true, "Bordered", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.reworked_terminal.generate_command_caret_color", "Generate command caret color",
                List.of("Reworked terminal", "Generate command caret color"),
                new ColorSchemeAttribute(false, false, "DFE1E5", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.reworked_terminal.generate_command_placeholder_foreground", "Generate command placeholder foreground",
                List.of("Reworked terminal", "Generate command placeholder foreground"),
                new ColorSchemeAttribute(false, false, "6C707E", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.reworked_terminal.generate_command_prompt_text", "Generate command prompt text",
                List.of("Reworked terminal", "Generate command prompt text"),
                new ColorSchemeAttribute(false, true, "7A7E85", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.reworked_terminal.green", "Green",
                List.of("Reworked terminal", "Green"),
                new ColorSchemeAttribute(false, false, "57965C", true, "32593D", true, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.reworked_terminal.inactive_selected_block_background", "Inactive selected block background",
                List.of("Reworked terminal", "Inactive selected block background"),
                new ColorSchemeAttribute(false, false, null, false, "2B2D30", true, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.reworked_terminal.inactive_selected_block_border", "Inactive selected block border",
                List.of("Reworked terminal", "Inactive selected block border"),
                new ColorSchemeAttribute(false, false, null, false, null, false, null, false, "393B40", true, "Bordered", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.reworked_terminal.magenta", "Magenta",
                List.of("Reworked terminal", "Magenta"),
                new ColorSchemeAttribute(false, false, "AE67A0", true, "743F6A", true, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.reworked_terminal.prompt_separator_color", "Prompt separator color",
                List.of("Reworked terminal", "Prompt separator color"),
                new ColorSchemeAttribute(false, false, "43454A", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.reworked_terminal.red", "Red",
                List.of("Reworked terminal", "Red"),
                new ColorSchemeAttribute(false, false, "BC3F3C", true, "7A2826", true, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.reworked_terminal.reworked_background_gradient_end", "Reworked background gradient end",
                List.of("Reworked terminal", "Reworked background gradient end"),
                new ColorSchemeAttribute(false, false, null, false, "1E1F22", true, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.reworked_terminal.reworked_background_gradient_start", "Reworked background gradient start",
                List.of("Reworked terminal", "Reworked background gradient start"),
                new ColorSchemeAttribute(false, false, null, false, "25262A", true, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.reworked_terminal.reworked_hovered_background_gradient_end", "Reworked hovered background gradient end",
                List.of("Reworked terminal", "Reworked hovered background gradient end"),
                new ColorSchemeAttribute(false, false, null, false, "2B2D30", true, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.reworked_terminal.reworked_hovered_background_gradient_start", "Reworked hovered background gradient start",
                List.of("Reworked terminal", "Reworked hovered background gradient start"),
                new ColorSchemeAttribute(false, false, null, false, "323438", true, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.reworked_terminal.search_entry", "Search entry",
                List.of("Reworked terminal", "Search entry"),
                new ColorSchemeAttribute(false, false, null, false, "32593D", true, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.reworked_terminal.selected_block_background", "Selected block background",
                List.of("Reworked terminal", "Selected block background"),
                new ColorSchemeAttribute(false, false, null, false, "2E436E", true, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.reworked_terminal.selected_block_border", "Selected block border",
                List.of("Reworked terminal", "Selected block border"),
                new ColorSchemeAttribute(false, false, null, false, null, false, null, false, "3574F0", true, "Bordered", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.reworked_terminal.white", "White",
                List.of("Reworked terminal", "White"),
                new ColorSchemeAttribute(false, false, "CED0D6", true, "CED0D6", true, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));
        register(new ColorSchemeElement("console.reworked_terminal.yellow", "Yellow",
                List.of("Reworked terminal", "Yellow"),
                new ColorSchemeAttribute(false, false, "C19C00", true, "806700", true, null, false, null, false, "Underscored", false, null), null, null, "Console Colors"));

        // 5. Terminal (Exact DataGrip item)
        register(new ColorSchemeElement("console.terminal.command_to_run_using_ide", "Command to run using IDE",
                List.of("Terminal", "Command to run using IDE"),
                new ColorSchemeAttribute(false, false, null, false, "40503C", true, null, false, null, false, "Bordered", false, null), null, null, "Console Colors"));

        // =========================================================================
        // DEBUGGER (11 items from DataGrip in exact order)
        // =========================================================================
        register(new ColorSchemeElement("debugger.breakpoint_line", "Breakpoint line",
                List.of("Debugger", "Breakpoint line"),
                new ColorSchemeAttribute(false, false, null, false, "3A2323", true, "E05555", true, null, false, "Underscored", false, null), null, null, "Debugger"));
        register(new ColorSchemeElement("debugger.evaluated_expression_text", "Evaluated expression text",
                List.of("Debugger", "Evaluated expression text"),
                new ColorSchemeAttribute(false, true, "868A91", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "Debugger"));
        register(new ColorSchemeElement("debugger.evaluated_expression_text_execution_line", "Evaluated expression text for execution line",
                List.of("Debugger", "Evaluated expression text for execution line"),
                new ColorSchemeAttribute(false, true, "868A91", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "Debugger"));
        register(new ColorSchemeElement("debugger.execution_point", "Execution point",
                List.of("Debugger", "Execution point"),
                new ColorSchemeAttribute(false, false, null, false, "2A5091", true, "3574F0", true, null, false, "Bordered", false, null), null, null, "Debugger"));
        register(new ColorSchemeElement("debugger.inlined_stack_frames", "Inline stack frames",
                List.of("Debugger", "Inline stack frames"),
                new ColorSchemeAttribute(false, false, "868A91", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "Debugger"));
        register(new ColorSchemeElement("debugger.inlined_modified_values", "Inlined modified values",
                List.of("Debugger", "Inlined modified values"),
                new ColorSchemeAttribute(false, true, "868A91", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "Debugger"));
        register(new ColorSchemeElement("debugger.inlined_values", "Inlined values",
                List.of("Debugger", "Inlined values"),
                new ColorSchemeAttribute(false, true, "868A91", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "Debugger"));
        register(new ColorSchemeElement("debugger.inlined_values_execution_line", "Inlined values for execution line",
                List.of("Debugger", "Inlined values for execution line"),
                new ColorSchemeAttribute(false, true, "868A91", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "Debugger"));
        register(new ColorSchemeElement("debugger.not_top_frame", "Not top frame",
                List.of("Debugger", "Not top frame"),
                new ColorSchemeAttribute(false, false, null, false, "25324D", true, null, false, null, false, "Underscored", false, null), null, null, "Debugger"));
        register(new ColorSchemeElement("debugger.smart_step_into_selection", "Smart step into selection",
                List.of("Debugger", "Smart step into selection"),
                new ColorSchemeAttribute(false, false, null, false, "2E436E", true, null, false, null, false, "Underscored", false, null), null, null, "Debugger"));
        register(new ColorSchemeElement("debugger.smart_step_into_target", "Smart step into target",
                List.of("Debugger", "Smart step into target"),
                new ColorSchemeAttribute(false, false, null, false, "2E436E", true, null, false, null, false, "Underscored", false, null), null, null, "Debugger"));

        // =========================================================================
        // DIFF & MERGE (Changed lines and Folded unchanged fragments)
        // =========================================================================
        register(new ColorSchemeElement("diff.changed", "Changed",
                List.of("Changed lines", "Changed"),
                new ColorSchemeAttribute(false, false, null, false, "2E436E", true, "385570", true, null, false, "Underscored", false, null, "2E3540", false, true), null, null, "Diff & Merge"));
        register(new ColorSchemeElement("diff.conflict", "Conflict",
                List.of("Changed lines", "Conflict"),
                new ColorSchemeAttribute(false, false, null, false, "5E3838", true, "D54040", true, null, false, "Underscored", false, null, "402828", false, true), null, null, "Diff & Merge"));
        register(new ColorSchemeElement("diff.deleted", "Deleted",
                List.of("Changed lines", "Deleted"),
                new ColorSchemeAttribute(false, false, null, false, "484A4A", true, "656E76", true, null, false, "Underscored", false, null, "383838", false, true), null, null, "Diff & Merge"));
        register(new ColorSchemeElement("diff.inserted", "Inserted",
                List.of("Changed lines", "Inserted"),
                new ColorSchemeAttribute(false, false, null, false, "294436", true, "436946", true, null, false, "Underscored", false, null, "203328", false, true), null, null, "Diff & Merge"));
        register(new ColorSchemeElement("diff.folded_wave", "Wave",
                List.of("Folded unchanged fragments", "Wave"),
                new ColorSchemeAttribute(false, false, "555555", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "Diff & Merge"));

        // =========================================================================
        // USER-DEFINED FILE TYPES (10 items in exact alphabetical order)
        // =========================================================================
        register(new ColorSchemeElement("user_types.block_comment", "Block comment",
                List.of("User-Defined File Types", "Block comment"),
                new ColorSchemeAttribute(false, false, "7A7E85", true, null, false, null, false, null, false, "Underscored", true, "lang.comments.block"),
                "lang.comments.block", "Comments → Block comment (Language Defaults)", "User-Defined File Types"));
        register(new ColorSchemeElement("user_types.invalid_string_escape", "Invalid string escape",
                List.of("User-Defined File Types", "Invalid string escape"),
                new ColorSchemeAttribute(false, false, "CF8E6D", true, null, false, null, false, "F75464", true, "Underwaved", false, null), null, null, "User-Defined File Types"));
        register(new ColorSchemeElement("user_types.keyword1", "Keyword1",
                List.of("User-Defined File Types", "Keyword1"),
                new ColorSchemeAttribute(false, false, "CF8E6D", true, null, false, null, false, null, false, "Bordered", false, "lang.keyword"),
                "lang.keyword", "Keyword (Language Defaults)", "User-Defined File Types"));
        register(new ColorSchemeElement("user_types.keyword2", "Keyword2",
                List.of("User-Defined File Types", "Keyword2"),
                new ColorSchemeAttribute(false, false, "C77DBB", true, null, false, null, false, null, false, "Bordered", false, null), null, null, "User-Defined File Types"));
        register(new ColorSchemeElement("user_types.keyword3", "Keyword3",
                List.of("User-Defined File Types", "Keyword3"),
                new ColorSchemeAttribute(false, false, "56A8F5", true, null, false, null, false, null, false, "Bordered", false, null), null, null, "User-Defined File Types"));
        register(new ColorSchemeElement("user_types.keyword4", "Keyword4",
                List.of("User-Defined File Types", "Keyword4"),
                new ColorSchemeAttribute(false, false, "2BBAC5", true, null, false, null, false, null, false, "Bordered", false, null), null, null, "User-Defined File Types"));
        register(new ColorSchemeElement("user_types.line_comment", "Line comment",
                List.of("User-Defined File Types", "Line comment"),
                new ColorSchemeAttribute(false, false, "7A7E85", true, null, false, null, false, null, false, "Underscored", true, "lang.comments.line"),
                "lang.comments.line", "Comments → Line comment (Language Defaults)", "User-Defined File Types"));
        register(new ColorSchemeElement("user_types.number", "Number",
                List.of("User-Defined File Types", "Number"),
                new ColorSchemeAttribute(false, false, "2AACB8", true, null, false, null, false, null, false, "Underscored", true, "lang.number"),
                "lang.number", "Numbers (Language Defaults)", "User-Defined File Types"));
        register(new ColorSchemeElement("user_types.string", "String",
                List.of("User-Defined File Types", "String"),
                new ColorSchemeAttribute(false, false, "6AAB73", true, null, false, null, false, null, false, "Underscored", true, "lang.string"),
                "lang.string", "String (Language Defaults)", "User-Defined File Types"));
        register(new ColorSchemeElement("user_types.valid_string_escape", "Valid string escape",
                List.of("User-Defined File Types", "Valid string escape"),
                new ColorSchemeAttribute(false, false, "CF8E6D", true, null, false, null, false, null, false, "Underscored", true, "lang.string.valid_escape"),
                "lang.string.valid_escape", "String → Valid escape sequence (Language Defaults)", "User-Defined File Types"));

        // =========================================================================
        // VCS (Editor Gutter: 9 items, VCS Annotations: 7 items)
        // =========================================================================
        register(new ColorSchemeElement("vcs.gutter.added_ignored_lines_border", "Added ignored lines border",
                List.of("Editor Gutter", "Added ignored lines border"),
                new ColorSchemeAttribute(false, false, null, false, null, false, null, false, "436946", true, "Bordered", false, null), null, null, "VCS"));
        register(new ColorSchemeElement("vcs.gutter.added_lines", "Added lines",
                List.of("Editor Gutter", "Added lines"),
                new ColorSchemeAttribute(false, false, null, false, "436946", true, null, false, null, false, "Underscored", false, null), null, null, "VCS"));
        register(new ColorSchemeElement("vcs.gutter.border", "Border",
                List.of("Editor Gutter", "Border"),
                new ColorSchemeAttribute(false, false, null, false, null, false, null, false, "393B40", true, "Bordered", false, null), null, null, "VCS"));
        register(new ColorSchemeElement("vcs.gutter.changed_lines_popup", "Changed lines popup",
                List.of("Editor Gutter", "Changed lines popup"),
                new ColorSchemeAttribute(false, false, null, false, "2B2D30", true, null, false, null, false, "Underscored", false, null), null, null, "VCS"));
        register(new ColorSchemeElement("vcs.gutter.deleted_ignored_lines_border", "Deleted ignored lines border",
                List.of("Editor Gutter", "Deleted ignored lines border"),
                new ColorSchemeAttribute(false, false, null, false, null, false, null, false, "868A91", true, "Bordered", false, null), null, null, "VCS"));
        register(new ColorSchemeElement("vcs.gutter.deleted_lines", "Deleted lines",
                List.of("Editor Gutter", "Deleted lines"),
                new ColorSchemeAttribute(false, false, null, false, "868A91", true, null, false, null, false, "Underscored", false, null), null, null, "VCS"));
        register(new ColorSchemeElement("vcs.gutter.modified_ignored_lines_border", "Modified ignored lines border",
                List.of("Editor Gutter", "Modified ignored lines border"),
                new ColorSchemeAttribute(false, false, null, false, null, false, null, false, "385570", true, "Bordered", false, null), null, null, "VCS"));
        register(new ColorSchemeElement("vcs.gutter.modified_lines", "Modified lines",
                List.of("Editor Gutter", "Modified lines"),
                new ColorSchemeAttribute(false, false, null, false, "385570", true, null, false, null, false, "Underscored", false, null), null, null, "VCS"));
        register(new ColorSchemeElement("vcs.gutter.whitespace_modified_lines", "Whitespace-modified lines",
                List.of("Editor Gutter", "Whitespace-modified lines"),
                new ColorSchemeAttribute(false, false, null, false, "4B5059", true, null, false, null, false, "Underscored", false, null), null, null, "VCS"));

        register(new ColorSchemeElement("vcs.annotations.bg_color_1", "Background color #1",
                List.of("VCS Annotations", "Background color #1"),
                new ColorSchemeAttribute(false, false, null, false, "25324D", true, null, false, null, false, "Underscored", false, null), null, null, "VCS"));
        register(new ColorSchemeElement("vcs.annotations.bg_color_2", "Background color #2",
                List.of("VCS Annotations", "Background color #2"),
                new ColorSchemeAttribute(false, false, null, false, "2E3A4D", true, null, false, null, false, "Underscored", false, null), null, null, "VCS"));
        register(new ColorSchemeElement("vcs.annotations.bg_color_3", "Background color #3",
                List.of("VCS Annotations", "Background color #3"),
                new ColorSchemeAttribute(false, false, null, false, "384659", true, null, false, null, false, "Underscored", false, null), null, null, "VCS"));
        register(new ColorSchemeElement("vcs.annotations.bg_color_4", "Background color #4",
                List.of("VCS Annotations", "Background color #4"),
                new ColorSchemeAttribute(false, false, null, false, "425266", true, null, false, null, false, "Underscored", false, null), null, null, "VCS"));
        register(new ColorSchemeElement("vcs.annotations.bg_color_5", "Background color #5",
                List.of("VCS Annotations", "Background color #5"),
                new ColorSchemeAttribute(false, false, null, false, "4C5E73", true, null, false, null, false, "Underscored", false, null), null, null, "VCS"));
        register(new ColorSchemeElement("vcs.annotations.foreground", "Foreground",
                List.of("VCS Annotations", "Foreground"),
                new ColorSchemeAttribute(false, false, "868A91", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "VCS"));
        register(new ColorSchemeElement("vcs.annotations.foreground_last_commit", "Foreground for last commit",
                List.of("VCS Annotations", "Foreground for last commit"),
                new ColorSchemeAttribute(false, false, "DFE1E5", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "VCS"));

        // =========================================================================
        // HTML (15 items)
        // =========================================================================
        register(new ColorSchemeElement("html.attribute_name", "Attribute name",
                List.of("Attribute name"),
                new ColorSchemeAttribute(false, false, "BABABA", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "HTML"));
        register(new ColorSchemeElement("html.attribute_value", "Attribute value",
                List.of("Attribute value"),
                new ColorSchemeAttribute(false, false, "6A8759", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "HTML"));
        register(new ColorSchemeElement("html.comment", "Comment",
                List.of("Comment"),
                new ColorSchemeAttribute(false, false, "808080", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "HTML"));
        register(new ColorSchemeElement("html.custom_tag_name", "Custom Tag Name",
                List.of("Custom Tag Name"),
                new ColorSchemeAttribute(false, false, "2FBAA3", true, null, false, null, false, null, false, "Bordered", false, null),
                "html.tag_name", "Tag name\n(HTML)", "HTML"));
        register(new ColorSchemeElement("html.entity_reference", "Entity reference",
                List.of("Entity reference"),
                new ColorSchemeAttribute(false, false, "6D9CBE", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "HTML"));
        register(new ColorSchemeElement("html.html_code", "HTML code",
                List.of("HTML code"),
                new ColorSchemeAttribute(false, false, null, false, null, false, null, false, null, false, "Underscored", false, null), null, null, "HTML"));
        register(new ColorSchemeElement("html.injected_language_fragment", "Injected Language Fragment",
                List.of("Injected Language Fragment"),
                new ColorSchemeAttribute(false, false, null, false, "363636", true, null, false, null, false, "Underscored", false, null), null, null, "HTML"));
        register(new ColorSchemeElement("html.tag", "Tag",
                List.of("Tag"),
                new ColorSchemeAttribute(false, false, "E8BF6A", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "HTML"));
        register(new ColorSchemeElement("html.tag_name", "Tag name",
                List.of("Tag name"),
                new ColorSchemeAttribute(false, false, "E8BF6A", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "HTML"));
        register(new ColorSchemeElement("html.tag_tree_1", "Tag tree (level 1)",
                List.of("Tag tree (level 1)"),
                new ColorSchemeAttribute(false, false, null, false, "313A31", true, null, false, null, false, "Underscored", false, null), null, null, "HTML"));
        register(new ColorSchemeElement("html.tag_tree_2", "Tag tree (level 2)",
                List.of("Tag tree (level 2)"),
                new ColorSchemeAttribute(false, false, null, false, "323A3E", true, null, false, null, false, "Underscored", false, null), null, null, "HTML"));
        register(new ColorSchemeElement("html.tag_tree_3", "Tag tree (level 3)",
                List.of("Tag tree (level 3)"),
                new ColorSchemeAttribute(false, false, null, false, "3B333B", true, null, false, null, false, "Underscored", false, null), null, null, "HTML"));
        register(new ColorSchemeElement("html.tag_tree_4", "Tag tree (level 4)",
                List.of("Tag tree (level 4)"),
                new ColorSchemeAttribute(false, false, null, false, "3A3832", true, null, false, null, false, "Underscored", false, null), null, null, "HTML"));
        register(new ColorSchemeElement("html.tag_tree_5", "Tag tree (level 5)",
                List.of("Tag tree (level 5)"),
                new ColorSchemeAttribute(false, false, null, false, "323838", true, null, false, null, false, "Underscored", false, null), null, null, "HTML"));
        register(new ColorSchemeElement("html.tag_tree_6", "Tag tree (level 6)",
                List.of("Tag tree (level 6)"),
                new ColorSchemeAttribute(false, false, null, false, "383338", true, null, false, null, false, "Underscored", false, null), null, null, "HTML"));

        // =========================================================================
        // DATA EDITOR AND VIEWER (Data Grid: 4 items)
        // =========================================================================
        register(new ColorSchemeElement("data_editor.alternating_row_color", "Alternating row color",
                List.of("Data Grid", "Alternating row color"),
                new ColorSchemeAttribute(false, false, null, false, "25272B", true, null, false, null, false, "Underscored", false, null), null, null, "Data Editor and Viewer"));
        register(new ColorSchemeElement("data_editor.error_data", "Error data",
                List.of("Data Grid", "Error data"),
                new ColorSchemeAttribute(false, false, null, false, null, false, "D6405B", true, "FA6675", true, "Underwaved", true, "errors.error"),
                "errors.error", "Errors and Warnings->Error\n(General)", "Data Editor and Viewer"));
        register(new ColorSchemeElement("data_editor.image_data", "Image data",
                List.of("Data Grid", "Image data"),
                new ColorSchemeAttribute(false, false, null, false, "393B40", true, null, false, null, false, "Underscored", false, null), null, null, "Data Editor and Viewer"));
        register(new ColorSchemeElement("data_editor.null_data", "Null data",
                List.of("Data Grid", "Null data"),
                new ColorSchemeAttribute(false, true, "7A7E85", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "Data Editor and Viewer"));

        // =========================================================================
        // DIAGRAMS (25 items)
        // =========================================================================
        register(new ColorSchemeElement("diagrams.bends.bend", "Bend",
                List.of("Bends", "Bend"),
                new ColorSchemeAttribute(false, false, "808080", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "Diagrams"));
        register(new ColorSchemeElement("diagrams.bends.bend_selection", "Bend selection",
                List.of("Bends", "Bend selection"),
                new ColorSchemeAttribute(false, false, "CC7832", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "Diagrams"));

        register(new ColorSchemeElement("diagrams.coarse_grid", "Coarse grid",
                List.of("Coarse grid"),
                new ColorSchemeAttribute(false, false, "2B2D30", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "Diagrams"));

        register(new ColorSchemeElement("diagrams.edges.annotation_edge", "Annotation edge",
                List.of("Edges", "Annotation edge"),
                new ColorSchemeAttribute(false, false, "707070", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "Diagrams"));
        register(new ColorSchemeElement("diagrams.edges.bad_edge", "Bad edge",
                List.of("Edges", "Bad edge"),
                new ColorSchemeAttribute(false, false, "FF6B68", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "Diagrams"));
        register(new ColorSchemeElement("diagrams.edges.default_edge", "Default edge",
                List.of("Edges", "Default edge"),
                new ColorSchemeAttribute(false, false, "A9B7C6", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "Diagrams"));
        register(new ColorSchemeElement("diagrams.edges.edge_selection", "Edge selection",
                List.of("Edges", "Edge selection"),
                new ColorSchemeAttribute(false, false, "CC7832", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "Diagrams"));
        register(new ColorSchemeElement("diagrams.edges.generalization_edge", "Generalization edge",
                List.of("Edges", "Generalization edge"),
                new ColorSchemeAttribute(false, false, "A9B7C6", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "Diagrams"));
        register(new ColorSchemeElement("diagrams.edges.inner_class_edge", "Inner class edge",
                List.of("Edges", "Inner class edge"),
                new ColorSchemeAttribute(false, false, "A9B7C6", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "Diagrams"));
        register(new ColorSchemeElement("diagrams.edges.realization_edge", "Realization edge",
                List.of("Edges", "Realization edge"),
                new ColorSchemeAttribute(false, false, "A9B7C6", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "Diagrams"));

        register(new ColorSchemeElement("diagrams.fine_grid", "Fine grid",
                List.of("Fine grid"),
                new ColorSchemeAttribute(false, false, "26282A", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "Diagrams"));
        register(new ColorSchemeElement("diagrams.hot_spots", "Hot spots",
                List.of("Hot spots"),
                new ColorSchemeAttribute(false, false, "393B40", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "Diagrams"));

        register(new ColorSchemeElement("diagrams.nodes.highlighted_node_border", "Highlighted node border",
                List.of("Nodes", "Highlighted node border"),
                new ColorSchemeAttribute(false, false, "4A88C7", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "Diagrams"));
        register(new ColorSchemeElement("diagrams.nodes.node_background", "Node background",
                List.of("Nodes", "Node background"),
                new ColorSchemeAttribute(false, false, null, false, "313335", true, null, false, null, false, "Underscored", false, null), null, null, "Diagrams"));
        register(new ColorSchemeElement("diagrams.nodes.node_border", "Node border",
                List.of("Nodes", "Node border"),
                new ColorSchemeAttribute(false, false, "393B40", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "Diagrams"));
        register(new ColorSchemeElement("diagrams.nodes.node_header", "Node header",
                List.of("Nodes", "Node header"),
                new ColorSchemeAttribute(false, false, null, false, "3C3F41", true, null, false, null, false, "Underscored", false, null), null, null, "Diagrams"));
        register(new ColorSchemeElement("diagrams.nodes.overview_node_background", "Overview node background",
                List.of("Nodes", "Overview node background"),
                new ColorSchemeAttribute(false, false, null, false, "2B2D30", true, null, false, null, false, "Underscored", false, null), null, null, "Diagrams"));
        register(new ColorSchemeElement("diagrams.nodes.selected_node_border", "Selected node border",
                List.of("Nodes", "Selected node border"),
                new ColorSchemeAttribute(false, false, "214283", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "Diagrams"));

        register(new ColorSchemeElement("diagrams.notes.note_background", "Note background",
                List.of("Notes", "Note background"),
                new ColorSchemeAttribute(false, false, null, false, "393B40", true, null, false, null, false, "Underscored", false, null), null, null, "Diagrams"));
        register(new ColorSchemeElement("diagrams.notes.note_border", "Note border",
                List.of("Notes", "Note border"),
                new ColorSchemeAttribute(false, false, "555555", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "Diagrams"));
        register(new ColorSchemeElement("diagrams.notes.overview_note_background", "Overview note background",
                List.of("Notes", "Overview note background"),
                new ColorSchemeAttribute(false, false, null, false, "2B2D30", true, null, false, null, false, "Underscored", false, null), null, null, "Diagrams"));

        register(new ColorSchemeElement("diagrams.port", "Port",
                List.of("Port"),
                new ColorSchemeAttribute(false, false, "808080", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "Diagrams"));

        register(new ColorSchemeElement("diagrams.selection_box.background", "Selection box background",
                List.of("Selection Box", "Selection box background"),
                new ColorSchemeAttribute(false, false, null, false, "214283", true, null, false, null, false, "Underscored", false, null), null, null, "Diagrams"));
        register(new ColorSchemeElement("diagrams.selection_box.border", "Selection box border",
                List.of("Selection Box", "Selection box border"),
                new ColorSchemeAttribute(false, false, "214283", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "Diagrams"));

        register(new ColorSchemeElement("diagrams.snapping_lines", "Snapping lines",
                List.of("Snapping lines"),
                new ColorSchemeAttribute(false, false, "CC7832", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "Diagrams"));

        // =========================================================================
        // JSON (14 items)
        // =========================================================================
        register(new ColorSchemeElement("json.block_comment", "Block comment",
                List.of("Block comment"),
                new ColorSchemeAttribute(false, false, "7A7E85", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "JSON"));
        register(new ColorSchemeElement("json.braces", "Braces",
                List.of("Braces"),
                new ColorSchemeAttribute(false, false, "BCBEC4", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "JSON"));
        register(new ColorSchemeElement("json.brackets", "Brackets",
                List.of("Brackets"),
                new ColorSchemeAttribute(false, false, "BCBEC4", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "JSON"));
        register(new ColorSchemeElement("json.colon", "Colon",
                List.of("Colon"),
                new ColorSchemeAttribute(false, false, "BCBEC4", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "JSON"));
        register(new ColorSchemeElement("json.comma", "Comma",
                List.of("Comma"),
                new ColorSchemeAttribute(false, false, "BCBEC4", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "JSON"));
        register(new ColorSchemeElement("json.invalid_escape", "Invalid escape sequence",
                List.of("Invalid escape sequence"),
                new ColorSchemeAttribute(false, false, "FA6675", true, null, false, null, false, "FA6675", true, "Underwaved", false, null), null, null, "JSON"));
        register(new ColorSchemeElement("json.keyword", "Keyword",
                List.of("Keyword"),
                new ColorSchemeAttribute(false, false, "CF8E6D", true, null, false, null, false, null, false, "Bordered", true, "lang.keyword"),
                "lang.keyword", "Keyword\n(Language Defaults)", "JSON"));
        register(new ColorSchemeElement("json.line_comment", "Line comment",
                List.of("Line comment"),
                new ColorSchemeAttribute(false, false, "7A7E85", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "JSON"));
        register(new ColorSchemeElement("json.number", "Number",
                List.of("Number"),
                new ColorSchemeAttribute(false, false, "2AACB8", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "JSON"));
        register(new ColorSchemeElement("json.parameter", "Parameter",
                List.of("Parameter"),
                new ColorSchemeAttribute(false, false, "BCBEC4", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "JSON"));
        register(new ColorSchemeElement("json.property_key", "Property key",
                List.of("Property key"),
                new ColorSchemeAttribute(false, false, "C792EA", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "JSON"));
        register(new ColorSchemeElement("json.semantic_highlighting", "Semantic highlighting",
                List.of("Semantic highlighting"),
                new ColorSchemeAttribute(false, false, null, false, null, false, null, false, null, false, "Underscored", false, null), null, null, "JSON"));
        register(new ColorSchemeElement("json.string", "String",
                List.of("String"),
                new ColorSchemeAttribute(false, false, "6A8759", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "JSON"));
        register(new ColorSchemeElement("json.valid_escape", "Valid escape sequence",
                List.of("Valid escape sequence"),
                new ColorSchemeAttribute(false, false, "CF8E6D", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "JSON"));

        // =========================================================================
        // DATABASE (Console: 1 item, Parameters: 2 items)
        // =========================================================================
        register(new ColorSchemeElement("database.statement_to_execute", "Statement to execute",
                List.of("Console", "Statement to execute"),
                new ColorSchemeAttribute(false, false, null, false, null, false, null, false, "3D7A49", true, "Bordered", false, null), null, null, "Database"));
        register(new ColorSchemeElement("database.parameter", "Parameter",
                List.of("Parameters", "Parameter"),
                new ColorSchemeAttribute(false, false, "3D7A49", true, null, false, null, false, null, false, "Underscored", false, null), null, null, "Database"));
        register(new ColorSchemeElement("database.parameter_usage", "Parameter usage",
                List.of("Parameters", "Parameter usage"),
                new ColorSchemeAttribute(false, false, null, false, "264D3B", true, null, false, "3D7A49", true, "Bordered", false, null), null, null, "Database"));
    }

    private static void register(ColorSchemeElement element) {
        ALL_ELEMENTS.add(element);
        ELEMENT_MAP.put(element.getId(), element);
    }

    public static List<ColorSchemeElement> getAllElements() {
        return Collections.unmodifiableList(ALL_ELEMENTS);
    }

    public static List<ColorSchemeElement> getGeneralElements() {
        List<ColorSchemeElement> list = new ArrayList<>();
        for (ColorSchemeElement el : ALL_ELEMENTS) {
            if ("General".equals(el.getPage())) {
                list.add(el);
            }
        }
        return Collections.unmodifiableList(list);
    }

    public static List<ColorSchemeElement> getLanguageDefaultElements() {
        List<ColorSchemeElement> list = new ArrayList<>();
        for (ColorSchemeElement el : ALL_ELEMENTS) {
            if ("Language Defaults".equals(el.getPage())) {
                list.add(el);
            }
        }
        return Collections.unmodifiableList(list);
    }

    public static List<ColorSchemeElement> getConsoleColorElements() {
        List<ColorSchemeElement> list = new ArrayList<>();
        for (ColorSchemeElement el : ALL_ELEMENTS) {
            if ("Console Colors".equals(el.getPage())) {
                list.add(el);
            }
        }
        return Collections.unmodifiableList(list);
    }

    public static List<ColorSchemeElement> getDebuggerElements() {
        List<ColorSchemeElement> list = new ArrayList<>();
        for (ColorSchemeElement el : ALL_ELEMENTS) {
            if ("Debugger".equals(el.getPage())) {
                list.add(el);
            }
        }
        return Collections.unmodifiableList(list);
    }

    public static List<ColorSchemeElement> getDiffMergeElements() {
        List<ColorSchemeElement> list = new ArrayList<>();
        for (ColorSchemeElement el : ALL_ELEMENTS) {
            if ("Diff & Merge".equals(el.getPage())) {
                list.add(el);
            }
        }
        return Collections.unmodifiableList(list);
    }

    public static List<ColorSchemeElement> getUserDefinedFileTypeElements() {
        List<ColorSchemeElement> list = new ArrayList<>();
        for (ColorSchemeElement el : ALL_ELEMENTS) {
            if ("User-Defined File Types".equals(el.getPage())) {
                list.add(el);
            }
        }
        return Collections.unmodifiableList(list);
    }

    public static List<ColorSchemeElement> getVcsElements() {
        List<ColorSchemeElement> list = new ArrayList<>();
        for (ColorSchemeElement el : ALL_ELEMENTS) {
            if ("VCS".equals(el.getPage())) {
                list.add(el);
            }
        }
        return Collections.unmodifiableList(list);
    }

    public static List<ColorSchemeElement> getHtmlElements() {
        List<ColorSchemeElement> list = new ArrayList<>();
        for (ColorSchemeElement el : ALL_ELEMENTS) {
            if ("HTML".equals(el.getPage())) {
                list.add(el);
            }
        }
        return Collections.unmodifiableList(list);
    }

    public static List<ColorSchemeElement> getDataEditorViewerElements() {
        List<ColorSchemeElement> list = new ArrayList<>();
        for (ColorSchemeElement el : ALL_ELEMENTS) {
            if ("Data Editor and Viewer".equals(el.getPage())) {
                list.add(el);
            }
        }
        return Collections.unmodifiableList(list);
    }

    public static List<String> getDataEditorViewerCategories() {
        Set<String> set = new LinkedHashSet<>();
        for (ColorSchemeElement el : ALL_ELEMENTS) {
            if ("Data Editor and Viewer".equals(el.getPage())) {
                set.add(el.getCategory());
            }
        }
        return new ArrayList<>(set);
    }

    public static List<ColorSchemeElement> getDiagramsElements() {
        List<ColorSchemeElement> list = new ArrayList<>();
        for (ColorSchemeElement el : ALL_ELEMENTS) {
            if ("Diagrams".equals(el.getPage())) {
                list.add(el);
            }
        }
        return Collections.unmodifiableList(list);
    }

    public static List<String> getDiagramsCategories() {
        Set<String> set = new LinkedHashSet<>();
        for (ColorSchemeElement el : ALL_ELEMENTS) {
            if ("Diagrams".equals(el.getPage())) {
                set.add(el.getCategory());
            }
        }
        return new ArrayList<>(set);
    }

    public static List<ColorSchemeElement> getJsonElements() {
        List<ColorSchemeElement> list = new ArrayList<>();
        for (ColorSchemeElement el : ALL_ELEMENTS) {
            if ("JSON".equals(el.getPage())) {
                list.add(el);
            }
        }
        return Collections.unmodifiableList(list);
    }

    public static List<ColorSchemeElement> getDatabaseElements() {
        List<ColorSchemeElement> list = new ArrayList<>();
        for (ColorSchemeElement el : ALL_ELEMENTS) {
            if ("Database".equals(el.getPage())) {
                list.add(el);
            }
        }
        return Collections.unmodifiableList(list);
    }

    public static List<String> getDatabaseCategories() {
        Set<String> set = new LinkedHashSet<>();
        for (ColorSchemeElement el : ALL_ELEMENTS) {
            if ("Database".equals(el.getPage())) {
                set.add(el.getCategory());
            }
        }
        return new ArrayList<>(set);
    }

    public static ColorSchemeElement getElement(String id) {
        if (id == null) return null;
        ColorSchemeElement el = ELEMENT_MAP.get(id);
        if (el != null) return el;
        // Aliases for compatibility
        if ("errors.marked_for_removal".equals(id)) return ELEMENT_MAP.get("errors.deprecated_marked_for_removal");
        if ("hyperlink.link".equals(id)) return ELEMENT_MAP.get("hyperlinks.unfollowed");
        if ("hyperlink.inactive".equals(id)) return ELEMENT_MAP.get("hyperlinks.inactive");
        if ("hyperlink.followed".equals(id)) return ELEMENT_MAP.get("hyperlinks.followed");
        if ("editor.scrollbar.track".equals(id)) return ELEMENT_MAP.get("editor.scrollbar.thumb_scrolling");
        if ("preview.folded".equals(id)) return ELEMENT_MAP.get("text.folded");
        if ("preview.folded_highlighted".equals(id)) return ELEMENT_MAP.get("text.folded_highlighted");
        if ("preview.deleted".equals(id)) return ELEMENT_MAP.get("text.deleted");
        if ("popups.param_hint".equals(id)) return ELEMENT_MAP.get("popups.code_lens");
        if ("popups.inline_hint".equals(id)) return ELEMENT_MAP.get("popups.info_hint");
        if ("search.result_write_access".equals(id)) return ELEMENT_MAP.get("search.result_write");
        if ("coverage.full_coverage".equals(id)) return ELEMENT_MAP.get("coverage.full");
        if ("coverage.partial_coverage".equals(id)) return ELEMENT_MAP.get("coverage.partial");
        if ("text.background_readonly".equals(id)) return ELEMENT_MAP.get("text.readonly_bg");
        if ("text.read_only_fragment_background".equals(id)) return ELEMENT_MAP.get("text.readonly_fragment_bg");
        if ("bad_character".equals(id)) return ELEMENT_MAP.get("lang.bad_character");
        if ("keyword".equals(id)) return ELEMENT_MAP.get("lang.keyword");
        if ("brackets".equals(id)) return ELEMENT_MAP.get("lang.braces_and_operators.brackets");
        if ("instance_field".equals(id)) return ELEMENT_MAP.get("lang.classes.instance_field");
        if ("doc_link".equals(id)) return ELEMENT_MAP.get("lang.comments.doc.link");
        if ("template_language".equals(id)) return ELEMENT_MAP.get("lang.template_language");
        if ("console.error".equals(id)) return ELEMENT_MAP.get("console.error_output");
        if ("console.standard".equals(id)) return ELEMENT_MAP.get("console.standard_output");
        if ("console.system".equals(id)) return ELEMENT_MAP.get("console.system_output");
        if ("console.user".equals(id)) return ELEMENT_MAP.get("console.user_input");
        if ("console.reworked_terminal.cursor".equals(id)) return ELEMENT_MAP.get("console.reworked_terminal.generate_command_caret_color");
        if ("console.reworked_terminal.selection".equals(id)) return ELEMENT_MAP.get("console.reworked_terminal.selected_block_background");
        if ("console.terminal.cursor".equals(id)) return ELEMENT_MAP.get("console.terminal.command_to_run_using_ide");
        if ("console.terminal.selection".equals(id)) return ELEMENT_MAP.get("console.terminal.command_to_run_using_ide");
        if ("html.custom_tag".equals(id)) return ELEMENT_MAP.get("html.custom_tag_name");
        if ("data_editor.error".equals(id)) return ELEMENT_MAP.get("data_editor.error_data");
        if ("data_editor.null".equals(id)) return ELEMENT_MAP.get("data_editor.null_data");
        if ("json.property".equals(id)) return ELEMENT_MAP.get("json.property_key");
        if ("database.statement".equals(id)) return ELEMENT_MAP.get("database.statement_to_execute");
        return null;
    }

    public static List<String> getCategories() {
        Set<String> set = new LinkedHashSet<>();
        for (ColorSchemeElement el : ALL_ELEMENTS) {
            if ("General".equals(el.getPage())) {
                set.add(el.getCategory());
            }
        }
        return new ArrayList<>(set);
    }

    public static List<String> getLanguageDefaultCategories() {
        Set<String> set = new LinkedHashSet<>();
        for (ColorSchemeElement el : ALL_ELEMENTS) {
            if ("Language Defaults".equals(el.getPage())) {
                set.add(el.getCategory());
            }
        }
        return new ArrayList<>(set);
    }

    public static List<String> getConsoleColorCategories() {
        Set<String> set = new LinkedHashSet<>();
        for (ColorSchemeElement el : ALL_ELEMENTS) {
            if ("Console Colors".equals(el.getPage())) {
                set.add(el.getCategory());
            }
        }
        return new ArrayList<>(set);
    }

    public static List<String> getDiffMergeCategories() {
        Set<String> set = new LinkedHashSet<>();
        for (ColorSchemeElement el : ALL_ELEMENTS) {
            if ("Diff & Merge".equals(el.getPage())) {
                set.add(el.getCategory());
            }
        }
        return new ArrayList<>(set);
    }

    public static List<String> getVcsCategories() {
        Set<String> set = new LinkedHashSet<>();
        for (ColorSchemeElement el : ALL_ELEMENTS) {
            if ("VCS".equals(el.getPage())) {
                set.add(el.getCategory());
            }
        }
        return new ArrayList<>(set);
    }

    public static List<ColorSchemeElement> getElementsByCategory(String category) {
        List<ColorSchemeElement> list = new ArrayList<>();
        for (ColorSchemeElement el : ALL_ELEMENTS) {
            if (category.equals(el.getCategory())) {
                list.add(el);
            }
        }
        return list;
    }

    /**
     * Resolves effective attributes taking overrides and inheritance into account.
     */
    public static ColorSchemeAttribute resolveAttribute(String schemeName,
                                                        String elementId,
                                                        Map<String, Map<String, ColorSchemeAttribute>> overrides) {
        return resolveAttributeInternal(schemeName, elementId, overrides, new HashSet<>());
    }

    private static ColorSchemeAttribute resolveAttributeInternal(String schemeName,
                                                                 String elementId,
                                                                 Map<String, Map<String, ColorSchemeAttribute>> overrides,
                                                                 Set<String> visited) {
        if (elementId == null || !visited.add(elementId)) {
            return new ColorSchemeAttribute();
        }
        ColorSchemeElement el = getElement(elementId);
        if (el == null) return new ColorSchemeAttribute();
        String canonicalId = el.getId();

        // Check override
        if (overrides != null && overrides.containsKey(schemeName)) {
            Map<String, ColorSchemeAttribute> schemeOverrides = overrides.get(schemeName);
            if (schemeOverrides != null) {
                ColorSchemeAttribute override = schemeOverrides.get(canonicalId);
                if (override == null) {
                    override = schemeOverrides.get(elementId);
                }
                if (override != null) {
                    if (override.inherit && el.hasInheritance()) {
                        return resolveAttributeInternal(schemeName, el.getInheritFromKey(), overrides, visited);
                    }
                    return override.copy();
                }
            }
        }

        // Check default inheritance
        if (el.getDefaultAttr().inherit && el.hasInheritance()) {
            return resolveAttributeInternal(schemeName, el.getInheritFromKey(), overrides, visited);
        }

        // Return base default
        return el.getDefaultAttr().copy();
    }
}
