/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.format.DateFormat;
import com.networknt.schema.format.DateTimeFormat;
import com.networknt.schema.format.DurationFormat;
import com.networknt.schema.format.EmailFormat;
import com.networknt.schema.format.IPv6Format;
import com.networknt.schema.format.IdnEmailFormat;
import com.networknt.schema.format.IdnHostnameFormat;
import com.networknt.schema.format.IriFormat;
import com.networknt.schema.format.IriReferenceFormat;
import com.networknt.schema.format.PatternFormat;
import com.networknt.schema.format.RegexFormat;
import com.networknt.schema.format.TimeFormat;
import com.networknt.schema.format.UriFormat;
import com.networknt.schema.format.UriReferenceFormat;
import com.networknt.schema.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class JsonMetaSchema {
    private static final Logger logger = LoggerFactory.getLogger(JsonMetaSchema.class);
    private static Map<String, String> UNKNOWN_KEYWORDS = new ConcurrentHashMap<>();

    static PatternFormat pattern(String name, String regex, String messageKey) {
        return PatternFormat.of(name, regex, messageKey);
    }

    static PatternFormat pattern(String name, String regex) {
        return pattern(name, regex, null);
    }

    public static final List<Format> COMMON_BUILTIN_FORMATS = new ArrayList<>();
    
    // this section contains formats common to all dialects.
    static {
        COMMON_BUILTIN_FORMATS.add(pattern("hostname", "^([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])(\\.([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9]))*$", "format.hostname"));
        COMMON_BUILTIN_FORMATS.add(pattern("ipv4", "^(([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])$", "format.ipv4"));
        COMMON_BUILTIN_FORMATS.add(new IPv6Format());
        COMMON_BUILTIN_FORMATS.add(pattern("json-pointer", "^(/([^/#~]|[~](?=[01]))*)*$", "format.json-pointer"));
        COMMON_BUILTIN_FORMATS.add(pattern("relative-json-pointer", "^(0|([1-9]\\d*))(#|(/([^/#~]|[~](?=[01]))*)*)$", "format.relative-json-pointer"));
        COMMON_BUILTIN_FORMATS.add(pattern("uri-template", "^([^\\p{Cntrl}\"'%<>\\^`\\{|\\}]|%\\p{XDigit}{2}|\\{[+#./;?&=,!@|]?((\\w|%\\p{XDigit}{2})(\\.?(\\w|%\\p{XDigit}{2}))*(:[1-9]\\d{0,3}|\\*)?)(,((\\w|%\\p{XDigit}{2})(\\.?(\\w|%\\p{XDigit}{2}))*(:[1-9]\\d{0,3}|\\*)?))*\\})*$", "format.uri-template"));
        COMMON_BUILTIN_FORMATS.add(pattern("uuid", "^\\p{XDigit}{8}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{12}$", "format.uuid"));
        COMMON_BUILTIN_FORMATS.add(new DateFormat());
        COMMON_BUILTIN_FORMATS.add(new DateTimeFormat());
        COMMON_BUILTIN_FORMATS.add(new EmailFormat());
        COMMON_BUILTIN_FORMATS.add(new IdnEmailFormat());
        COMMON_BUILTIN_FORMATS.add(new IdnHostnameFormat());
        COMMON_BUILTIN_FORMATS.add(new IriFormat());
        COMMON_BUILTIN_FORMATS.add(new IriReferenceFormat());
        COMMON_BUILTIN_FORMATS.add(new RegexFormat());
        COMMON_BUILTIN_FORMATS.add(new TimeFormat());
        COMMON_BUILTIN_FORMATS.add(new UriFormat());
        COMMON_BUILTIN_FORMATS.add(new UriReferenceFormat());
        COMMON_BUILTIN_FORMATS.add(new DurationFormat());
        
        // The following formats do not appear in any draft
        COMMON_BUILTIN_FORMATS.add(pattern("alpha", "^[a-zA-Z]+$"));
        COMMON_BUILTIN_FORMATS.add(pattern("alphanumeric", "^[a-zA-Z0-9]+$"));
        COMMON_BUILTIN_FORMATS.add(pattern("color", "(#?([0-9A-Fa-f]{3,6})\\b)|(aqua)|(black)|(blue)|(fuchsia)|(gray)|(green)|(lime)|(maroon)|(navy)|(olive)|(orange)|(purple)|(red)|(silver)|(teal)|(white)|(yellow)|(rgb\\(\\s*\\b([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])\\b\\s*,\\s*\\b([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])\\b\\s*,\\s*\\b([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])\\b\\s*\\))|(rgb\\(\\s*(\\d?\\d%|100%)+\\s*,\\s*(\\d?\\d%|100%)+\\s*,\\s*(\\d?\\d%|100%)+\\s*\\))"));
        COMMON_BUILTIN_FORMATS.add(pattern("ip-address", "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"));
        COMMON_BUILTIN_FORMATS.add(pattern("phone", "^\\+(?:[0-9] ?){6,14}[0-9]$"));
        COMMON_BUILTIN_FORMATS.add(pattern("style", "\\s*(.+?):\\s*([^;]+);?"));
        COMMON_BUILTIN_FORMATS.add(pattern("utc-millisec", "^[0-9]+(\\.?[0-9]+)?$"));
    }
    
    public interface FormatKeywordFactory {
        FormatKeyword newInstance(Map<String, Format> formats);
    }

    public static class Builder {
        private VersionFlag specification = null;
        private Map<String, Keyword> keywords = new HashMap<>();
        private Map<String, Format> formats = new HashMap<>();
        private Map<String, Boolean> vocabularies = new HashMap<>();
        private String uri;
        private String idKeyword = "id";
        private FormatKeywordFactory formatKeywordFactory = null;

        public Builder(String uri) {
            this.uri = uri;
        }

        private Map<String, Keyword> createKeywordsMap(Map<String, Keyword> kwords, Map<String, Format> formats) {
            Map<String, Keyword> map = new HashMap<>();
            for (Map.Entry<String, Keyword> type : kwords.entrySet()) {
                String keywordName = type.getKey();
                Keyword keyword = type.getValue();
                if (ValidatorTypeCode.FORMAT.getValue().equals(keywordName)) {
                    if (!(keyword instanceof FormatKeyword)) {
                        throw new IllegalArgumentException("Overriding the keyword 'format' is not supported. Use the formatKeywordFactory and extend the FormatKeyword.");
                    }
                    // ignore - format keyword will be created again below.
                } else {
                    map.put(keyword.getValue(), keyword);
                }
            }
            final FormatKeyword formatKeyword = formatKeywordFactory != null ? formatKeywordFactory.newInstance(formats)
                    : new FormatKeyword(formats);
            map.put(formatKeyword.getValue(), formatKeyword);
            return map;
        }

        public Builder formatKeywordFactory(FormatKeywordFactory formatKeywordFactory) {
            this.formatKeywordFactory = formatKeywordFactory;
            return this;
        }

        public Builder formats(Consumer<Map<String, Format>> customizer) {
            customizer.accept(this.formats);
            return this;
        }

        public Builder keywords(Consumer<Map<String, Keyword>> customizer) {
            customizer.accept(this.keywords);
            return this;
        }


        public Builder addKeyword(Keyword keyword) {
            this.keywords.put(keyword.getValue(), keyword);
            return this;
        }

        public Builder addKeywords(Collection<? extends Keyword> keywords) {
            for (Keyword keyword : keywords) {
                this.keywords.put(keyword.getValue(), keyword);
            }
            return this;
        }

        public Builder addFormat(Format format) {
            this.formats.put(format.getName(), format);
            return this;
        }

        public Builder addFormats(Collection<? extends Format> formats) {
            for (Format format : formats) {
                addFormat(format);
            }
            return this;
        }

        public Builder vocabulary(String vocabulary) {
            return vocabulary(vocabulary, true);
        }

        public Builder vocabulary(String vocabulary, boolean enabled) {
            this.vocabularies.put(vocabulary, enabled);
            return this;
        }

        public Builder vocabularies(Map<String, Boolean> vocabularies) {
            this.vocabularies = vocabularies;
            return this;
        }

        public Builder specification(VersionFlag specification) {
            this.specification = specification;
            return this;
        }

        public Builder idKeyword(String idKeyword) {
            this.idKeyword = idKeyword;
            return this;
        }

        public JsonMetaSchema build() {
            // create builtin keywords with (custom) formats.
            Map<String, Keyword> kwords = createKeywordsMap(this.keywords, this.formats);
            if (this.specification != null) {
                if (this.specification.getVersionFlagValue() >= SpecVersion.VersionFlag.V201909.getVersionFlagValue()) {
                    if (!this.uri.equals(this.specification.getId())) {
                        // The current design is such that the keyword map can contain things that aren't actually keywords
                        // This means need to remove what can't be found instead of creating from scratch
                        Map<String, Boolean> vocabularies = JsonSchemaFactory.checkVersion(this.specification)
                                .getInstance().getVocabularies();
                        Set<String> current = this.vocabularies.keySet();
                        Map<String, String> format = new HashMap<>();
                        format.put(Vocabulary.V202012_FORMAT_ANNOTATION.getId(), Vocabulary.V202012_FORMAT_ASSERTION.getId());
                        format.put(Vocabulary.V202012_FORMAT_ASSERTION.getId(), Vocabulary.V202012_FORMAT_ANNOTATION.getId());
                        for (String vocabularyId : vocabularies.keySet()) {
                            if (!current.contains(vocabularyId)) {
                                String formatVocab = format.get(vocabularyId);
                                if (formatVocab != null) {
                                    if (current.contains(formatVocab)) {
                                        // Skip as the assertion and annotation keywords are the same
                                        continue;
                                    }
                                }
                                Vocabulary vocabulary = Vocabularies.getVocabulary(vocabularyId);
                                for (String keywordToRemove : vocabulary.getKeywords()) {
                                    kwords.remove(keywordToRemove);
                                }
                            }
                        }
                    }
                }
            }
            return new JsonMetaSchema(this.uri, this.idKeyword, kwords, this.vocabularies, this.specification);
        }
    }

    private final String uri;
    private final String idKeyword;
    private Map<String, Keyword> keywords;
    private Map<String, Boolean> vocabularies;
    private final VersionFlag specification;

    JsonMetaSchema(String uri, String idKeyword, Map<String, Keyword> keywords, Map<String, Boolean> vocabularies, VersionFlag specification) {
        if (StringUtils.isBlank(uri)) {
            throw new IllegalArgumentException("uri must not be null or blank");
        }
        if (StringUtils.isBlank(idKeyword)) {
            throw new IllegalArgumentException("idKeyword must not be null or blank");
        }
        if (keywords == null) {
            throw new IllegalArgumentException("keywords must not be null ");
        }

        this.uri = uri;
        this.idKeyword = idKeyword;
        this.keywords = keywords;
        this.specification = specification;
        this.vocabularies = vocabularies;
    }

    public static JsonMetaSchema getV4() {
        return new Version4().getInstance();
    }

    public static JsonMetaSchema getV6() {
        return new Version6().getInstance();
    }

    public static JsonMetaSchema getV7() {
        return new Version7().getInstance();
    }

    public static JsonMetaSchema getV201909() {
        return new Version201909().getInstance();
    }

    public static JsonMetaSchema getV202012() {
        return new Version202012().getInstance();
    }

    /**
     * Builder without keywords or formats.
     * <p>
     * Use {@link #getV4()} for the Draft 4 Metaschema, or if you need a builder based on Draft4, use
     *
     * <code>
     * JsonMetaSchema.builder("http://your-metaschema-uri", JsonSchemaFactory.getDraftV4()).build();
     * </code>
     *
     * @param uri the URI of the metaschema that will be defined via this builder.
     * @return a builder instance without any keywords or formats - usually not what one needs.
     */
    public static Builder builder(String uri) {
        return new Builder(uri);
    }

    /**
     * @param uri       the URI of your new JsonMetaSchema that will be defined via this builder.
     * @param blueprint the JsonMetaSchema to base your custom JsonMetaSchema on.
     * @return a builder instance preconfigured to be the same as blueprint, but with a different uri.
     */
    public static Builder builder(String uri, JsonMetaSchema blueprint) {
        FormatKeyword formatKeyword = (FormatKeyword) blueprint.keywords.get(ValidatorTypeCode.FORMAT.getValue());
        if (formatKeyword == null) {
            throw new IllegalArgumentException("The formatKeyword did not exist - blueprint is invalid.");
        }
        Map<String, Boolean> vocabularies = new HashMap<>(blueprint.getVocabularies());
        return builder(uri)
                .idKeyword(blueprint.idKeyword)
                .addKeywords(blueprint.keywords.values())
                .addFormats(formatKeyword.getFormats())
                .specification(blueprint.getSpecification())
                .vocabularies(vocabularies)
                ;
    }

    public String getIdKeyword() {
        return this.idKeyword;
    }

    public String readId(JsonNode schemaNode) {
        return readText(schemaNode, this.idKeyword);
    }

    public String readAnchor(JsonNode schemaNode) {
        boolean supportsAnchor = this.keywords.containsKey("$anchor");
        if (supportsAnchor) {
            return readText(schemaNode, "$anchor");
        }
        return null;
    }

    public String readDynamicAnchor(JsonNode schemaNode) {
        boolean supportsDynamicAnchor = this.keywords.containsKey("$dynamicAnchor");
        if (supportsDynamicAnchor) {
            return readText(schemaNode, "$dynamicAnchor");
        }
        return null;
    }

    private static String readText(JsonNode node, String field) {
        JsonNode idNode = node.get(field);
        if (idNode == null || !idNode.isTextual()) {
            return null;
        }
        return idNode.textValue();
    }

    public String getUri() {
        return this.uri;
    }

    public Map<String, Keyword> getKeywords() {
        return this.keywords;
    }

    public Map<String, Boolean> getVocabularies() {
        return this.vocabularies;
    }
    
    public VersionFlag getSpecification() {
        return this.specification;
    }

    public JsonValidator newValidator(ValidationContext validationContext, SchemaLocation schemaLocation, JsonNodePath evaluationPath, String keyword /* keyword */, JsonNode schemaNode,
                                      JsonSchema parentSchema) {

        try {
            Keyword kw = this.keywords.get(keyword);
            if (kw == null) {
                if ("message".equals(keyword) && validationContext.getConfig().isCustomMessageSupported()) {
                    return null;
                }
                if (ValidatorTypeCode.DISCRIMINATOR.getValue().equals(keyword)
                        && validationContext.getConfig().isOpenAPI3StyleDiscriminators()) {
                    return ValidatorTypeCode.DISCRIMINATOR.newValidator(schemaLocation, evaluationPath, schemaNode,
                            parentSchema, validationContext);
                }
                if (UNKNOWN_KEYWORDS.put(keyword, keyword) == null) {
                    logger.warn("Unknown keyword {} - you should define your own Meta Schema. If the keyword is irrelevant for validation, just use a NonValidationKeyword", keyword);
                }
                return null;
            }
            return kw.newValidator(schemaLocation, evaluationPath, schemaNode, parentSchema, validationContext);
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof JsonSchemaException) {
                logger.error("Error:", e);
                throw (JsonSchemaException) e.getTargetException();
            }
            logger.warn("Could not load validator {}", keyword);
            throw new JsonSchemaException(e.getTargetException());
        } catch (JsonSchemaException e) {
            throw e;
        } catch (Exception e) {
            logger.warn("Could not load validator {}", keyword);
            throw new JsonSchemaException(e);
        }
    }

    @Override
    public String toString() {
        return this.uri;
    }
}
