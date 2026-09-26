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
            this.id = id;
            this.name = name;
            this.page = "Language Defaults";
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
