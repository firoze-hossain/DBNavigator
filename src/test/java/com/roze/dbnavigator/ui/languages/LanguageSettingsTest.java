package com.roze.dbnavigator.ui.languages;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roze.dbnavigator.db.AppSettingsStore;
import com.roze.dbnavigator.ui.SettingsDialog;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class LanguageSettingsTest {

    @BeforeAll
    public static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // JavaFX platform already started
        }
    }

    @Test
    public void testMarkdownSettingsDefaultsAndCopy() {
        AppSettingsStore.MarkdownSettingsConfig cfg = new AppSettingsStore.MarkdownSettingsConfig();

        assertEquals("Chromium browser", cfg.getPreviewRenderingEngine());
        assertEquals("Editor and Preview", cfg.getDefaultLayout());
        assertEquals("Split vertically", cfg.getPreviewLayout());
        assertEquals(13, cfg.getPreviewFontSize());

        assertTrue(cfg.isSyncScroll());
        assertTrue(cfg.isInjectLanguagesInCodeFences());
        assertTrue(cfg.isShowProblemsInCodeFences());
        assertFalse(cfg.isGroupDocumentsWithSameName());
        assertTrue(cfg.isDetectCommandsCanRun());

        assertFalse(cfg.isPlantUmlEnabled());
        assertFalse(cfg.isCustomCssLoadFromEnabled());
        assertEquals("", cfg.getCustomCssPath());
        assertFalse(cfg.isCustomCssRulesEnabled());
        assertEquals("", cfg.getCustomCssRules());

        assertEquals("", cfg.getPandocExecutablePath());
        assertEquals("", cfg.getPandocWordImagesDir());

        // Test copy independence
        AppSettingsStore.MarkdownSettingsConfig copy = cfg.copy();
        copy.setPreviewRenderingEngine("JavaFX WebView");
        copy.setPreviewFontSize(18);
        copy.setPlantUmlEnabled(true);
        copy.setCustomCssPath("/path/to/custom.css");

        assertEquals("Chromium browser", cfg.getPreviewRenderingEngine());
        assertEquals(13, cfg.getPreviewFontSize());
        assertFalse(cfg.isPlantUmlEnabled());
        assertEquals("", cfg.getCustomCssPath());

        assertEquals("JavaFX WebView", copy.getPreviewRenderingEngine());
        assertEquals(18, copy.getPreviewFontSize());
        assertTrue(copy.isPlantUmlEnabled());
        assertEquals("/path/to/custom.css", copy.getCustomCssPath());
    }

    @Test
    public void testSchemasDtdsDefaultsAndCopy() {
        AppSettingsStore.SchemasDtdsConfig cfg = new AppSettingsStore.SchemasDtdsConfig();

        assertNotNull(cfg.getExternalResources());
        assertTrue(cfg.getExternalResources().isEmpty());

        assertNotNull(cfg.getIgnoredResources());
        assertEquals(8, cfg.getIgnoredResources().size());
        assertTrue(cfg.getIgnoredResources().contains("http://exslt.org/common"));
        assertTrue(cfg.getIgnoredResources().contains("http://exslt.org/dates-and-times"));
        assertTrue(cfg.getIgnoredResources().contains("http://exslt.org/dynamic"));
        assertTrue(cfg.getIgnoredResources().contains("http://exslt.org/math"));
        assertTrue(cfg.getIgnoredResources().contains("http://exslt.org/sets"));
        assertTrue(cfg.getIgnoredResources().contains("http://exslt.org/strings"));
        assertTrue(cfg.getIgnoredResources().contains("http://relaxng.org/ns/compatibility/annotations/1.0"));
        assertTrue(cfg.getIgnoredResources().contains("urn:idea:xslt-plugin#extensions"));

        // ExternalResourceMapping tests
        AppSettingsStore.ExternalResourceMapping map1 = new AppSettingsStore.ExternalResourceMapping("http://custom.org/spec", "/local/spec.xsd");
        AppSettingsStore.ExternalResourceMapping map2 = map1.copy();
        assertEquals(map1, map2);
        assertEquals(map1.hashCode(), map2.hashCode());
        assertEquals("http://custom.org/spec", map2.getUri());
        assertEquals("/local/spec.xsd", map2.getLocation());

        cfg.getExternalResources().add(map1);
        assertEquals(1, cfg.getExternalResources().size());

        // Test copy independence
        AppSettingsStore.SchemasDtdsConfig copy = cfg.copy();
        assertEquals(1, copy.getExternalResources().size());
        assertEquals(8, copy.getIgnoredResources().size());

        copy.getExternalResources().clear();
        copy.getIgnoredResources().remove(0);
        assertEquals(1, cfg.getExternalResources().size());
        assertEquals(8, cfg.getIgnoredResources().size());
    }

    @Test
    public void testDefaultXmlSchemasDefaultsAndCopy() {
        AppSettingsStore.DefaultXmlSchemasConfig cfg = new AppSettingsStore.DefaultXmlSchemasConfig();

        assertEquals("HTML 5", cfg.getHtmlLanguageLevel());
        assertEquals("", cfg.getHtmlOtherDoctype());
        assertEquals("1.0", cfg.getXmlSchemaVersion());

        AppSettingsStore.DefaultXmlSchemasConfig copy = cfg.copy();
        copy.setHtmlLanguageLevel("HTML 4");
        copy.setHtmlOtherDoctype("<!DOCTYPE html SYSTEM \"custom.dtd\">");
        copy.setXmlSchemaVersion("1.1");

        assertEquals("HTML 5", cfg.getHtmlLanguageLevel());
        assertEquals("", cfg.getHtmlOtherDoctype());
        assertEquals("1.0", cfg.getXmlSchemaVersion());

        assertEquals("HTML 4", copy.getHtmlLanguageLevel());
        assertEquals("<!DOCTYPE html SYSTEM \"custom.dtd\">", copy.getHtmlOtherDoctype());
        assertEquals("1.1", copy.getXmlSchemaVersion());
    }

    @Test
    public void testJsonSerializationRoundTrip() throws Exception {
        AppSettingsStore.Settings original = new AppSettingsStore.Settings();
        original.getMarkdown().setPreviewRenderingEngine("JavaFX WebView");
        original.getMarkdown().setPreviewFontSize(16);
        original.getMarkdown().setPlantUmlEnabled(true);
        original.getMarkdown().setCustomCssRules("body { color: red; }");
        original.getMarkdown().setPandocExecutablePath("/usr/local/bin/pandoc");

        original.getSchemasDtds().getExternalResources().add(
                new AppSettingsStore.ExternalResourceMapping("http://test.com/schema", "/opt/schema.xsd")
        );
        original.getSchemasDtds().getIgnoredResources().add("http://custom.org/ignore");

        original.getDefaultXmlSchemas().setHtmlLanguageLevel("HTML 4");
        original.getDefaultXmlSchemas().setXmlSchemaVersion("1.1");

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(original);
        assertNotNull(json);

        AppSettingsStore.Settings loaded = mapper.readValue(json, AppSettingsStore.Settings.class);
        assertNotNull(loaded);
        assertEquals("JavaFX WebView", loaded.getMarkdown().getPreviewRenderingEngine());
        assertEquals(16, loaded.getMarkdown().getPreviewFontSize());
        assertTrue(loaded.getMarkdown().isPlantUmlEnabled());
        assertEquals("body { color: red; }", loaded.getMarkdown().getCustomCssRules());
        assertEquals("/usr/local/bin/pandoc", loaded.getMarkdown().getPandocExecutablePath());

        assertEquals(1, loaded.getSchemasDtds().getExternalResources().size());
        assertEquals("http://test.com/schema", loaded.getSchemasDtds().getExternalResources().get(0).getUri());
        assertEquals("/opt/schema.xsd", loaded.getSchemasDtds().getExternalResources().get(0).getLocation());
        assertTrue(loaded.getSchemasDtds().getIgnoredResources().contains("http://custom.org/ignore"));

        assertEquals("HTML 4", loaded.getDefaultXmlSchemas().getHtmlLanguageLevel());
        assertEquals("1.1", loaded.getDefaultXmlSchemas().getXmlSchemaVersion());
    }

    @Test
    public void testMarkdownPanelCreationAndInputs() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        MarkdownSettingsPanel panel = new MarkdownSettingsPanel(settings, inputs, null);
        Node node = panel.createPanel();
        assertNotNull(node);

        assertTrue(inputs.containsKey("languages_markdown_previewRenderingEngine"));
        assertTrue(inputs.containsKey("languages_markdown_defaultLayout"));
        assertTrue(inputs.containsKey("languages_markdown_previewLayout"));
        assertTrue(inputs.containsKey("languages_markdown_previewFontSize"));
        assertTrue(inputs.containsKey("languages_markdown_syncScroll"));
        assertTrue(inputs.containsKey("languages_markdown_injectLanguagesInCodeFences"));
        assertTrue(inputs.containsKey("languages_markdown_showProblemsInCodeFences"));
        assertTrue(inputs.containsKey("languages_markdown_groupDocumentsWithSameName"));
        assertTrue(inputs.containsKey("languages_markdown_detectCommandsCanRun"));
        assertTrue(inputs.containsKey("languages_markdown_plantUmlEnabled"));
        assertTrue(inputs.containsKey("languages_markdown_customCssLoadFromEnabled"));
        assertTrue(inputs.containsKey("languages_markdown_customCssPath"));
        assertTrue(inputs.containsKey("languages_markdown_customCssRulesEnabled"));
        assertTrue(inputs.containsKey("languages_markdown_customCssRules"));
        assertTrue(inputs.containsKey("languages_markdown_pandocExecutablePath"));
        assertTrue(inputs.containsKey("languages_markdown_pandocWordImagesDir"));
    }

    @Test
    public void testSchemasDtdsPanelCreationAndInputs() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        SchemasDtdsPanel panel = new SchemasDtdsPanel(settings, inputs);
        Node node = panel.createPanel();
        assertNotNull(node);

        assertTrue(inputs.containsKey("languages_schemas_externalResources"));
        assertTrue(inputs.containsKey("languages_schemas_ignoredResources"));

        Object extObj = inputs.get("languages_schemas_externalResources");
        assertInstanceOf(ObservableList.class, extObj);

        Object ignObj = inputs.get("languages_schemas_ignoredResources");
        assertInstanceOf(ObservableList.class, ignObj);
        ObservableList<?> ignList = (ObservableList<?>) ignObj;
        assertEquals(8, ignList.size());
    }

    @Test
    public void testDefaultXmlSchemasPanelCreationAndInputs() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        DefaultXmlSchemasPanel panel = new DefaultXmlSchemasPanel(settings, inputs);
        Node node = panel.createPanel();
        assertNotNull(node);

        assertTrue(inputs.containsKey("languages_xml_htmlLanguageGroup"));
        assertTrue(inputs.containsKey("languages_xml_htmlOtherDoctype"));
        assertTrue(inputs.containsKey("languages_xml_schemaVersionGroup"));

        ToggleGroup htmlGroup = (ToggleGroup) inputs.get("languages_xml_htmlLanguageGroup");
        assertNotNull(htmlGroup.getSelectedToggle());
        assertTrue(((RadioButton) htmlGroup.getSelectedToggle()).getText().startsWith("HTML 5"));

        ToggleGroup xmlGroup = (ToggleGroup) inputs.get("languages_xml_schemaVersionGroup");
        assertNotNull(xmlGroup.getSelectedToggle());
        assertTrue(((RadioButton) xmlGroup.getSelectedToggle()).getText().contains("1.0"));
    }

    @Test
    public void testSettingsDialogRoutingForLanguages() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        Node mdNode = SettingsDialog.buildPanelForPath("Languages / Markdown", settings, inputs, null);
        assertNotNull(mdNode, "Languages / Markdown route must return a panel");

        Node schemasNode = SettingsDialog.buildPanelForPath("Languages / Schemas and DTDs", settings, inputs, null);
        assertNotNull(schemasNode, "Languages / Schemas and DTDs route must return a panel");

        Node defaultXmlNode = SettingsDialog.buildPanelForPath("Languages / Schemas and DTDs / Default XML Schemas", settings, inputs, null);
        assertNotNull(defaultXmlNode, "Languages / Schemas and DTDs / Default XML Schemas route must return a panel");
    }

    @Test
    public void testSettingsDialogApplySettingsForLanguages() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        // 1. Markdown inputs
        ComboBox<String> engineCombo = new ComboBox<>();
        engineCombo.setValue("JavaFX WebView");
        inputs.put("languages_markdown_previewRenderingEngine", engineCombo);

        ComboBox<String> defLayoutCombo = new ComboBox<>();
        defLayoutCombo.setValue("Preview only");
        inputs.put("languages_markdown_defaultLayout", defLayoutCombo);

        ComboBox<String> prevLayoutCombo = new ComboBox<>();
        prevLayoutCombo.setValue("Split horizontally");
        inputs.put("languages_markdown_previewLayout", prevLayoutCombo);

        Spinner<Integer> fontSpinner = new Spinner<>(8, 72, 16);
        inputs.put("languages_markdown_previewFontSize", fontSpinner);

        CheckBox syncScroll = new CheckBox();
        syncScroll.setSelected(false);
        inputs.put("languages_markdown_syncScroll", syncScroll);

        CheckBox plantUml = new CheckBox();
        plantUml.setSelected(true);
        inputs.put("languages_markdown_plantUmlEnabled", plantUml);

        CheckBox loadCss = new CheckBox();
        loadCss.setSelected(true);
        inputs.put("languages_markdown_customCssLoadFromEnabled", loadCss);

        TextField cssPath = new TextField("/home/user/style.css");
        inputs.put("languages_markdown_customCssPath", cssPath);

        CheckBox cssRules = new CheckBox();
        cssRules.setSelected(true);
        inputs.put("languages_markdown_customCssRulesEnabled", cssRules);

        TextArea rulesArea = new TextArea("h1 { color: #f00; }");
        inputs.put("languages_markdown_customCssRules", rulesArea);

        TextField pandocPath = new TextField("/usr/bin/pandoc");
        inputs.put("languages_markdown_pandocExecutablePath", pandocPath);

        TextField wordImages = new TextField("/home/user/word-images");
        inputs.put("languages_markdown_pandocWordImagesDir", wordImages);

        // 2. Schemas and DTDs inputs
        List<AppSettingsStore.ExternalResourceMapping> extList = new ArrayList<>();
        extList.add(new AppSettingsStore.ExternalResourceMapping("http://custom.org/spec", "/local/spec.xsd"));
        inputs.put("languages_schemas_externalResources", extList);

        List<String> ignList = new ArrayList<>();
        ignList.add("http://exslt.org/common");
        ignList.add("http://custom.org/ignored");
        inputs.put("languages_schemas_ignoredResources", ignList);

        // 3. Default XML Schemas inputs
        ToggleGroup htmlGroup = new ToggleGroup();
        RadioButton html4 = new RadioButton("HTML 4 (\"http://www.w3.org/TR/html4/loose.dtd\")");
        html4.setToggleGroup(htmlGroup);
        html4.setSelected(true);
        inputs.put("languages_xml_htmlLanguageGroup", htmlGroup);

        ToggleGroup xmlGroup = new ToggleGroup();
        RadioButton xml11 = new RadioButton("XML Schema 1.1");
        xml11.setToggleGroup(xmlGroup);
        xml11.setSelected(true);
        inputs.put("languages_xml_schemaVersionGroup", xmlGroup);

        // Apply settings
        SettingsDialog.applySettings(null, settings, inputs);

        // Verify markdown
        assertEquals("JavaFX WebView", settings.getMarkdown().getPreviewRenderingEngine());
        assertEquals("Preview only", settings.getMarkdown().getDefaultLayout());
        assertEquals("Split horizontally", settings.getMarkdown().getPreviewLayout());
        assertEquals(16, settings.getMarkdown().getPreviewFontSize());
        assertFalse(settings.getMarkdown().isSyncScroll());
        assertTrue(settings.getMarkdown().isPlantUmlEnabled());
        assertTrue(settings.getMarkdown().isCustomCssLoadFromEnabled());
        assertEquals("/home/user/style.css", settings.getMarkdown().getCustomCssPath());
        assertTrue(settings.getMarkdown().isCustomCssRulesEnabled());
        assertEquals("h1 { color: #f00; }", settings.getMarkdown().getCustomCssRules());
        assertEquals("/usr/bin/pandoc", settings.getMarkdown().getPandocExecutablePath());
        assertEquals("/home/user/word-images", settings.getMarkdown().getPandocWordImagesDir());

        // Verify schemas
        assertEquals(1, settings.getSchemasDtds().getExternalResources().size());
        assertEquals("http://custom.org/spec", settings.getSchemasDtds().getExternalResources().get(0).getUri());
        assertEquals("/local/spec.xsd", settings.getSchemasDtds().getExternalResources().get(0).getLocation());
        assertEquals(2, settings.getSchemasDtds().getIgnoredResources().size());
        assertTrue(settings.getSchemasDtds().getIgnoredResources().contains("http://custom.org/ignored"));

        // Verify default xml schemas
        assertEquals("HTML 4", settings.getDefaultXmlSchemas().getHtmlLanguageLevel());
        assertEquals("1.1", settings.getDefaultXmlSchemas().getXmlSchemaVersion());

        // 4. JSON Schema Mappings inputs
        List<AppSettingsStore.JsonSchemaMappingItem> jsonMappings = new ArrayList<>();
        AppSettingsStore.JsonSchemaMappingItem item = new AppSettingsStore.JsonSchemaMappingItem("Package Schema", "http://json.schemastore.org/package", "JSON Schema v7");
        item.getPatterns().add(new AppSettingsStore.JsonSchemaPatternMapping(AppSettingsStore.JsonSchemaPatternMapping.PatternKind.FILE, "package.json"));
        jsonMappings.add(item);
        inputs.put("languages_json_schemaMappings", jsonMappings);

        // 5. Remote JSON Schemas inputs
        CheckBox allowRemote = new CheckBox();
        allowRemote.setSelected(false);
        inputs.put("languages_remoteJson_allowDownload", allowRemote);

        CheckBox useCatalog = new CheckBox();
        useCatalog.setSelected(false);
        inputs.put("languages_remoteJson_useSchemaStore", useCatalog);

        CheckBox alwaysRecent = new CheckBox();
        alwaysRecent.setSelected(true);
        inputs.put("languages_remoteJson_alwaysDownloadMostRecent", alwaysRecent);

        // 6. XML Catalog inputs
        TextField catalogFile = new TextField("/opt/xml/catalog.xml");
        inputs.put("languages_xmlCatalog_propertyFile", catalogFile);

        // 7. XSLT inputs
        CheckBox showXsltFiles = new CheckBox();
        showXsltFiles.setSelected(false);
        inputs.put("languages_xslt_showAssociatedFiles", showXsltFiles);

        // Apply
        SettingsDialog.applySettings(null, settings, inputs);

        // Verify JSON Schema Mappings
        assertEquals(1, settings.getJsonSchemaMappings().getMappings().size());
        assertEquals("Package Schema", settings.getJsonSchemaMappings().getMappings().get(0).getName());
        assertEquals("http://json.schemastore.org/package", settings.getJsonSchemaMappings().getMappings().get(0).getSchemaFileOrUrl());
        assertEquals("JSON Schema v7", settings.getJsonSchemaMappings().getMappings().get(0).getSchemaVersion());
        assertEquals(1, settings.getJsonSchemaMappings().getMappings().get(0).getPatterns().size());
        assertEquals("package.json", settings.getJsonSchemaMappings().getMappings().get(0).getPatterns().get(0).getPathOrPattern());

        // Verify Remote JSON Schemas
        assertFalse(settings.getRemoteJsonSchemas().isAllowDownloadRemoteSchemas());
        assertFalse(settings.getRemoteJsonSchemas().isUseSchemaStoreCatalog());
        assertTrue(settings.getRemoteJsonSchemas().isAlwaysDownloadMostRecent());

        // Verify XML Catalog
        assertEquals("/opt/xml/catalog.xml", settings.getXmlCatalog().getCatalogPropertyFile());

        // Verify XSLT
        assertFalse(settings.getXslt().isShowAssociatedFilesInProjectView());
    }

    @Test
    public void testJsonSchemaMappingsDefaultsAndCopy() {
        AppSettingsStore.JsonSchemaMappingsConfig cfg = new AppSettingsStore.JsonSchemaMappingsConfig();
        assertNotNull(cfg.getMappings());
        assertTrue(cfg.getMappings().isEmpty());

        AppSettingsStore.JsonSchemaMappingItem item = new AppSettingsStore.JsonSchemaMappingItem("My Schema", "/path/to/schema.json", "JSON Schema v4");
        item.getPatterns().add(new AppSettingsStore.JsonSchemaPatternMapping(AppSettingsStore.JsonSchemaPatternMapping.PatternKind.FILE_PATH_PATTERN, "*.config.json"));
        cfg.getMappings().add(item);

        assertEquals(1, cfg.getMappings().size());
        assertEquals("My Schema", cfg.getMappings().get(0).getName());
        assertEquals(1, cfg.getMappings().get(0).getPatterns().size());

        AppSettingsStore.JsonSchemaMappingsConfig copy = cfg.copy();
        assertEquals(1, copy.getMappings().size());
        assertEquals("My Schema", copy.getMappings().get(0).getName());

        copy.getMappings().get(0).setName("Modified");
        assertEquals("My Schema", cfg.getMappings().get(0).getName());
        assertEquals("Modified", copy.getMappings().get(0).getName());

        AppSettingsStore.JsonSchemaPatternMapping p1 = new AppSettingsStore.JsonSchemaPatternMapping(AppSettingsStore.JsonSchemaPatternMapping.PatternKind.FILE, "a.json");
        AppSettingsStore.JsonSchemaPatternMapping p2 = p1.copy();
        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    public void testRemoteJsonSchemasDefaultsAndCopy() {
        AppSettingsStore.RemoteJsonSchemasConfig cfg = new AppSettingsStore.RemoteJsonSchemasConfig();
        assertTrue(cfg.isAllowDownloadRemoteSchemas());
        assertTrue(cfg.isUseSchemaStoreCatalog());
        assertFalse(cfg.isAlwaysDownloadMostRecent());

        AppSettingsStore.RemoteJsonSchemasConfig copy = cfg.copy();
        copy.setAllowDownloadRemoteSchemas(false);
        copy.setAlwaysDownloadMostRecent(true);

        assertTrue(cfg.isAllowDownloadRemoteSchemas());
        assertFalse(cfg.isAlwaysDownloadMostRecent());
        assertFalse(copy.isAllowDownloadRemoteSchemas());
        assertTrue(copy.isAlwaysDownloadMostRecent());
    }

    @Test
    public void testXmlCatalogDefaultsAndCopy() {
        AppSettingsStore.XmlCatalogConfig cfg = new AppSettingsStore.XmlCatalogConfig();
        assertEquals("", cfg.getCatalogPropertyFile());

        AppSettingsStore.XmlCatalogConfig copy = cfg.copy();
        copy.setCatalogPropertyFile("/path/to/catalog.xml");
        assertEquals("", cfg.getCatalogPropertyFile());
        assertEquals("/path/to/catalog.xml", copy.getCatalogPropertyFile());
    }

    @Test
    public void testXsltDefaultsAndCopy() {
        AppSettingsStore.XsltConfig cfg = new AppSettingsStore.XsltConfig();
        assertTrue(cfg.isShowAssociatedFilesInProjectView());

        AppSettingsStore.XsltConfig copy = cfg.copy();
        copy.setShowAssociatedFilesInProjectView(false);
        assertTrue(cfg.isShowAssociatedFilesInProjectView());
        assertFalse(copy.isShowAssociatedFilesInProjectView());
    }

    @Test
    public void testFullLanguageJsonSerializationRoundTrip() throws Exception {
        AppSettingsStore.Settings original = new AppSettingsStore.Settings();
        original.getRemoteJsonSchemas().setAllowDownloadRemoteSchemas(false);
        original.getRemoteJsonSchemas().setAlwaysDownloadMostRecent(true);
        original.getXmlCatalog().setCatalogPropertyFile("/etc/xml/catalog.xml");
        original.getXslt().setShowAssociatedFilesInProjectView(false);

        AppSettingsStore.JsonSchemaMappingItem item = new AppSettingsStore.JsonSchemaMappingItem("TSConfig", "http://json.schemastore.org/tsconfig", "JSON Schema 2020-12");
        item.getPatterns().add(new AppSettingsStore.JsonSchemaPatternMapping(AppSettingsStore.JsonSchemaPatternMapping.PatternKind.FILE, "tsconfig.json"));
        original.getJsonSchemaMappings().getMappings().add(item);

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(original);
        assertNotNull(json);

        AppSettingsStore.Settings loaded = mapper.readValue(json, AppSettingsStore.Settings.class);
        assertNotNull(loaded);
        assertFalse(loaded.getRemoteJsonSchemas().isAllowDownloadRemoteSchemas());
        assertTrue(loaded.getRemoteJsonSchemas().isAlwaysDownloadMostRecent());
        assertEquals("/etc/xml/catalog.xml", loaded.getXmlCatalog().getCatalogPropertyFile());
        assertFalse(loaded.getXslt().isShowAssociatedFilesInProjectView());

        assertEquals(1, loaded.getJsonSchemaMappings().getMappings().size());
        AppSettingsStore.JsonSchemaMappingItem loadedItem = loaded.getJsonSchemaMappings().getMappings().get(0);
        assertEquals("TSConfig", loadedItem.getName());
        assertEquals("JSON Schema 2020-12", loadedItem.getSchemaVersion());
        assertEquals(1, loadedItem.getPatterns().size());
        assertEquals("tsconfig.json", loadedItem.getPatterns().get(0).getPathOrPattern());
    }

    @Test
    public void testRemainingPanelsCreationAndInputs() {
        AppSettingsStore.Settings settings = new AppSettingsStore.Settings();
        Map<String, Object> inputs = new HashMap<>();

        // 1. JSON Schema Mappings panel
        JsonSchemaMappingsPanel jsonPanel = new JsonSchemaMappingsPanel(settings, inputs);
        Node jsonNode = jsonPanel.createPanel();
        assertNotNull(jsonNode);
        assertTrue(inputs.containsKey("languages_json_schemaMappings"));

        // 2. Remote JSON Schemas panel
        RemoteJsonSchemasPanel remotePanel = new RemoteJsonSchemasPanel(settings, inputs);
        Node remoteNode = remotePanel.createPanel();
        assertNotNull(remoteNode);
        assertTrue(inputs.containsKey("languages_remoteJson_allowDownload"));
        assertTrue(inputs.containsKey("languages_remoteJson_useSchemaStore"));
        assertTrue(inputs.containsKey("languages_remoteJson_alwaysDownloadMostRecent"));

        // 3. XML Catalog panel
        XmlCatalogPanel catalogPanel = new XmlCatalogPanel(settings, inputs);
        Node catalogNode = catalogPanel.createPanel();
        assertNotNull(catalogNode);
        assertTrue(inputs.containsKey("languages_xmlCatalog_propertyFile"));

        // 4. XSLT panel
        XsltSettingsPanel xsltPanel = new XsltSettingsPanel(settings, inputs);
        Node xsltNode = xsltPanel.createPanel();
        assertNotNull(xsltNode);
        assertTrue(inputs.containsKey("languages_xslt_showAssociatedFiles"));

        // 5. XSLT File Associations panel
        XsltFileAssociationsPanel assocPanel = new XsltFileAssociationsPanel(settings, inputs);
        Node assocNode = assocPanel.createPanel();
        assertNotNull(assocNode);
        assertTrue(inputs.containsKey("languages_xslt_fileAssociations"));

        // Test SettingsDialog routing
        assertNotNull(SettingsDialog.buildPanelForPath("Languages / Schemas and DTDs / JSON Schema Mappings", settings, inputs, null));
        assertNotNull(SettingsDialog.buildPanelForPath("Languages / Schemas and DTDs / Remote JSON Schemas", settings, inputs, null));
        assertNotNull(SettingsDialog.buildPanelForPath("Languages / Schemas and DTDs / XML Catalog", settings, inputs, null));
        assertNotNull(SettingsDialog.buildPanelForPath("Languages / XSLT", settings, inputs, null));
        assertNotNull(SettingsDialog.buildPanelForPath("Languages / XSLT File Associations", settings, inputs, null));
    }

    @Test
    public void testXsltFileAssociationsDefaultsAndCopy() {
        AppSettingsStore.XsltFileAssociationsConfig cfg = new AppSettingsStore.XsltFileAssociationsConfig();
        assertNotNull(cfg.getEntries());
        assertEquals(1, cfg.getEntries().size());
        assertEquals("Db  ~/DBNavigatorProjects/Db", cfg.getEntries().get(0).getXsltFileOrDir());

        AppSettingsStore.XsltAssociationEntry entry = cfg.getEntries().get(0);
        entry.getAssociatedFiles().add("/path/to/doc.xml");

        AppSettingsStore.XsltFileAssociationsConfig copy = cfg.copy();
        assertEquals(1, copy.getEntries().size());
        assertEquals(1, copy.getEntries().get(0).getAssociatedFiles().size());

        copy.getEntries().get(0).getAssociatedFiles().clear();
        assertEquals(1, cfg.getEntries().get(0).getAssociatedFiles().size());
        assertEquals(0, copy.getEntries().get(0).getAssociatedFiles().size());
    }
}
