package dev.isaac.nativegui.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Original, conservative implementation of the publicly documented OptiGUI 3
 * JSON resource-pack INTERFACE. This is not derived from OptiGUI source code.
 *
 * Supported: exact vanilla/modded container IDs, original->replacement PNG maps,
 * unconditional rules, biome matching and screen-title matching. Unsupported filters
 * reject the entire rule rather than silently match all GUIs.
 */
public final class JsonGuiRules {
    private JsonGuiRules() {}

    @FunctionalInterface
    public interface Filter {
        boolean matches(LegacyGuiRules.Context context);
    }

    public record Rule(String source, Set<String> containers, Map<String, String> textures, Filter filter) {
        public Rule {
            containers = Set.copyOf(containers);
            textures = Map.copyOf(textures);
        }

        public String replacement(String original, LegacyGuiRules.Context context) {
            if (context == null || context.sourceId() == null || !containers.contains(context.sourceId())) return null;
            if (filter != null && !filter.matches(context)) return null;
            return textures.get(original);
        }

        public Rule withTextures(Map<String,String> replacements) {
            return new Rule(source, containers, replacements, filter);
        }
    }

    public static Optional<Rule> parse(String namespace, String path, String json) {
        JsonElement document = JsonParser.parseString(json);
        if (!document.isJsonObject()) return Optional.empty();
        JsonObject object = document.getAsJsonObject();
        if (!object.has("containers") || !object.has("textures")) return Optional.empty();
        // 'if' is evaluated against runtime/mod state when resources are loaded.
        // Do not load a conditional resource without evaluating the condition.
        if (object.has("if") && !isEmptyFilter(object.get("if"))) return Optional.empty();

        Set<String> containers = parseContainerIds(object.get("containers"));
        if (containers.isEmpty()) return Optional.empty();
        if (!object.get("textures").isJsonObject()) return Optional.empty();
        Map<String,String> textures = new LinkedHashMap<>();
        for (var entry : object.getAsJsonObject("textures").entrySet()) {
            if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isString())
                return Optional.empty();
            String from = identifier(entry.getKey());
            String to = identifier(entry.getValue().getAsString());
            if (from == null || to == null || !from.endsWith(".png") || !to.endsWith(".png"))
                return Optional.empty();
            textures.put(from, to);
        }
        if (textures.isEmpty()) return Optional.empty();

        Filter filter = null;
        if (object.has("match")) {
            filter = parseFilter(object.get("match"));
            if (filter == null) return Optional.empty();
        }
        return Optional.of(new Rule(namespace + ":" + path, containers, textures, filter));
    }

    private static boolean isEmptyFilter(JsonElement element) {
        return element != null && element.isJsonObject() && element.getAsJsonObject().size() == 0;
    }

    private static Set<String> parseContainerIds(JsonElement value) {
        List<JsonElement> candidates = new ArrayList<>();
        if (value.isJsonArray()) value.getAsJsonArray().forEach(candidates::add);
        else candidates.add(value);
        Set<String> found = new LinkedHashSet<>();
        for (JsonElement item : candidates) {
            if (!item.isJsonPrimitive() || !item.getAsJsonPrimitive().isString()) return Set.of();
            String id = identifier(item.getAsString());
            if (id == null) return Set.of();
            found.add(id);
        }
        return found;
    }

    /** Any-of arrays, all-of objects; safe subset of documented @biome NBT filters. */
    private static Filter parseFilter(JsonElement value) {
        if (value.isJsonArray()) {
            JsonArray entries = value.getAsJsonArray();
            if (entries.size() == 0) return null;
            List<Filter> children = new ArrayList<>();
            for (JsonElement entry : entries) {
                Filter child = parseFilter(entry);
                if (child == null) return null;
                children.add(child);
            }
            return context -> children.stream().anyMatch(child -> child.matches(context));
        }
        if (!value.isJsonObject()) return null;
        JsonObject object = value.getAsJsonObject();
        if (object.size() == 0) return context -> true;
        List<Filter> children = new ArrayList<>();
        for (var entry : object.entrySet()) {
            Filter child = switch (entry.getKey()) {
                case "@biome" -> parseStringMatcher(entry.getValue(), LegacyGuiRules.Context::biome);
                case "@screen" -> parseScreenMatcher(entry.getValue());
                default -> null;
            };
            if (child == null) return null;
            children.add(child);
        }
        return context -> children.stream().allMatch(child -> child.matches(context));
    }

    /**
     * Published OptiGUI 3 NBT shape: match.@screen.@title.@text or @translate.
     * Never equate a translated vanilla title with a literal custom name.
     * Arrays of possible titles follow the documented any-of semantics.
     */
    private static Filter parseScreenMatcher(JsonElement value) {
        if (!value.isJsonObject()) return null;
        JsonObject screen = value.getAsJsonObject();
        if (screen.size() != 1 || !screen.has("@title")) return null;
        return parseTitleMatcher(screen.get("@title"));
    }

    private static Filter parseTitleMatcher(JsonElement value) {
        if (value.isJsonArray()) {
            List<Filter> options = new ArrayList<>();
            for (JsonElement item : value.getAsJsonArray()) {
                Filter option = parseTitleMatcher(item);
                if (option == null) return null;
                options.add(option);
            }
            return options.isEmpty() ? null : ctx -> options.stream().anyMatch(f -> f.matches(ctx));
        }
        if (!value.isJsonObject()) return null;
        JsonObject fields = value.getAsJsonObject();
        if (fields.size() == 0) return null;
        List<Filter> tests = new ArrayList<>();
        for (var field : fields.entrySet()) {
            Filter test = switch (field.getKey()) {
                case "@text" -> parseStringMatcher(field.getValue(),
                        c -> c.titleTranslationKey() == null ? c.title() : null);
                case "@translate" -> parseStringMatcher(field.getValue(), LegacyGuiRules.Context::titleTranslationKey);
                default -> null;
            };
            if (test == null) return null;
            tests.add(test);
        }
        return c -> tests.stream().allMatch(test -> test.matches(c));
    }

    /** A conservative subset of the published string-valued NBT matchers. */
    private static Filter parseStringMatcher(JsonElement filter,
                                              java.util.function.Function<LegacyGuiRules.Context, String> selector) {
        if (filter.isJsonPrimitive() && filter.getAsJsonPrimitive().isString()) {
            String expected = filter.getAsString();
            return context -> expected.equals(selector.apply(context));
        }
        if (!filter.isJsonObject()) return null;
        JsonObject matchers = filter.getAsJsonObject();
        if (matchers.size() == 0) return null;
        List<Filter> conditions = new ArrayList<>();
        for (var entry : matchers.entrySet()) {
            if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isString()) return null;
            String key = entry.getKey(), expected = entry.getValue().getAsString();
            Filter test;
            try {
                test = switch (key) {
                    case "=" -> c -> expected.equals(selector.apply(c));
                    case "=*" -> c -> selector.apply(c) != null && expected.equalsIgnoreCase(selector.apply(c));
                    case "!=" -> c -> selector.apply(c) != null && !expected.equals(selector.apply(c));
                    case "!=*" -> c -> selector.apply(c) != null && !expected.equalsIgnoreCase(selector.apply(c));
                    case "wildcard", "wildcard*" -> {
                        StringBuilder regex = new StringBuilder("^");
                        for (char ch : expected.toCharArray()) {
                            if (ch == '*') regex.append(".*");
                            else if (ch == '?') regex.append('.');
                            else regex.append(Pattern.quote(String.valueOf(ch)));
                        }
                        regex.append('$');
                        Pattern pattern = Pattern.compile(regex.toString(),
                                key.endsWith("*") ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : 0);
                        yield c -> selector.apply(c) != null && pattern.matcher(selector.apply(c)).matches();
                    }
                    case "regex", "regex*" -> {
                        Pattern pattern = Pattern.compile(expected,
                                key.endsWith("*") ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : 0);
                        yield c -> selector.apply(c) != null && pattern.matcher(selector.apply(c)).matches();
                    }
                    default -> null;
                };
            } catch (PatternSyntaxException ex) { return null; }
            if (test == null) return null;
            conditions.add(test);
        }
        return context -> conditions.stream().allMatch(condition -> condition.matches(context));
    }

    /** Minecraft resource identifiers with the documented implicit minecraft: namespace. */
    private static String identifier(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String normalized = raw.contains(":") ? raw : "minecraft:" + raw;
        if (!normalized.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) return null;
        String resourcePath = normalized.substring(normalized.indexOf(':') + 1);
        if (resourcePath.startsWith("/") || resourcePath.endsWith("/") ||
                Arrays.stream(resourcePath.split("/", -1)).anyMatch(segment ->
                        segment.isEmpty() || segment.equals(".") || segment.equals(".."))) return null;
        return normalized;
    }

}
