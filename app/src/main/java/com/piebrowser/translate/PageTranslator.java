package com.piebrowser.translate;

import android.content.Context;
import android.webkit.WebView;

import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * PageTranslator — translates web page content using ML Kit on-device translation.
 *
 * Features:
 * - Fully offline translation (after model download)
 * - 50+ supported languages
 * - Translates visible text on the page via JavaScript injection
 * - No data sent to any server
 *
 * Supported target languages include:
 * English, Spanish, French, German, Italian, Portuguese,
 * Chinese, Japanese, Korean, Arabic, Hindi, and 40+ more.
 */
public class PageTranslator {

    public interface TranslationCallback {
        void onReady();
        void onError(String error);
    }

    private final Map<String, Translator> translators = new HashMap<>();
    private static PageTranslator instance;

    private PageTranslator() {}

    public static synchronized PageTranslator getInstance() {
        if (instance == null) instance = new PageTranslator();
        return instance;
    }

    /**
     * Download the translation model if needed, then translate the page.
     * @param webView The WebView containing the page
     * @param targetLanguage ML Kit language code (e.g. TranslateLanguage.SPANISH)
     */
    public void translatePage(WebView webView, String targetLanguage, TranslationCallback cb) {
        String key = TranslateLanguage.ENGLISH + "_" + targetLanguage;
        Translator translator = translators.get(key);

        if (translator == null) {
            TranslatorOptions options = new TranslatorOptions.Builder()
                    .setSourceLanguage(TranslateLanguage.ENGLISH)
                    .setTargetLanguage(targetLanguage)
                    .build();
            translator = Translation.getClient(options);
            translators.put(key, translator);
        }

        Translator finalTranslator = translator;

        // Download model if not present
        finalTranslator.downloadModelIfNeeded()
            .addOnSuccessListener(v -> {
                // Inject JS to extract text nodes and translate them
                injectTranslationScript(webView, finalTranslator);
                cb.onReady();
            })
            .addOnFailureListener(e -> cb.onError("Could not download translation model: " + e.getMessage()));
    }

    /**
     * Inject JavaScript to walk the DOM, extract text nodes, batch-translate, then replace.
     * This approach preserves page layout while translating text content.
     */
    private void injectTranslationScript(WebView webView, Translator translator) {
        // Extract all text node content via JS
        webView.evaluateJavascript(
            "(function() {" +
            "  var walker = document.createTreeWalker(" +
            "    document.body, NodeFilter.SHOW_TEXT, null, false);" +
            "  var nodes = [];" +
            "  var texts = [];" +
            "  var node;" +
            "  while (node = walker.nextNode()) {" +
            "    var t = node.textContent.trim();" +
            "    if (t.length > 3) { nodes.push(node); texts.push(t); }" +
            "  }" +
            "  window.__pieTranslateNodes = nodes;" +
            "  return JSON.stringify(texts.slice(0, 100));" + // translate first 100 nodes
            "})()",
            textsJson -> {
                try {
                    org.json.JSONArray arr = new org.json.JSONArray(textsJson);
                    for (int i = 0; i < arr.length(); i++) {
                        final int idx = i;
                        final String text = arr.getString(i);
                        translator.translate(text)
                            .addOnSuccessListener(translated -> {
                                // Replace text node at index idx
                                String safe = translated.replace("'", "\\'")
                                                        .replace("\n", "\\n");
                                webView.evaluateJavascript(
                                    "if (window.__pieTranslateNodes && " +
                                    "    window.__pieTranslateNodes[" + idx + "]) {" +
                                    "  window.__pieTranslateNodes[" + idx + "].textContent = '" +
                                    safe + "';" +
                                    "}", null);
                            });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        );
    }

    /**
     * Map of display names to ML Kit language codes.
     */
    public static Map<String, String> getSupportedLanguages() {
        Map<String, String> langs = new HashMap<>();
        langs.put("Spanish",    TranslateLanguage.SPANISH);
        langs.put("French",     TranslateLanguage.FRENCH);
        langs.put("German",     TranslateLanguage.GERMAN);
        langs.put("Italian",    TranslateLanguage.ITALIAN);
        langs.put("Portuguese", TranslateLanguage.PORTUGUESE);
        langs.put("Chinese",    TranslateLanguage.CHINESE);
        langs.put("Japanese",   TranslateLanguage.JAPANESE);
        langs.put("Korean",     TranslateLanguage.KOREAN);
        langs.put("Arabic",     TranslateLanguage.ARABIC);
        langs.put("Hindi",      TranslateLanguage.HINDI);
        langs.put("Russian",    TranslateLanguage.RUSSIAN);
        langs.put("Turkish",    TranslateLanguage.TURKISH);
        langs.put("Dutch",      TranslateLanguage.DUTCH);
        langs.put("Polish",     TranslateLanguage.POLISH);
        langs.put("Swedish",    TranslateLanguage.SWEDISH);
        return langs;
    }

    public void cleanup() {
        for (Translator t : translators.values()) t.close();
        translators.clear();
    }
}
