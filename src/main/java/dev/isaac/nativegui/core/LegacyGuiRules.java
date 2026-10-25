package dev.isaac.nativegui.core;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * An independent implementation of the PUBLIC OptiFine custom-GUI properties format.
 * No OptiGUI implementation code or assets are used.
 *
 * This class intentionally has zero Minecraft/NeoForge dependencies so its behavior
 * can be checked with plain Java 21 tests.
 */
public final class LegacyGuiRules {
    private LegacyGuiRules() {}

    /** Context values unavailable for virtual menus are null, not guessed. */
    public record Context(String container, String title, String biome, Integer y,
                          boolean large, boolean trapped, boolean ender, boolean christmas,
                          String color, String variant, Integer beaconLevel,
                          String villagerProfession, Integer villagerLevel, String sourceId,
                          String titleTranslationKey) {
        /** Compatibility constructor for callers that only know the displayed title. */
        public Context(String container, String title, String biome, Integer y,
                       boolean large, boolean trapped, boolean ender, boolean christmas,
                       String color, String variant, Integer beaconLevel,
                       String villagerProfession, Integer villagerLevel, String sourceId) {
            this(container, title, biome, y, large, trapped, ender, christmas, color,
                    variant, beaconLevel, villagerProfession, villagerLevel, sourceId, null);
        }
        public static Context basic(String container, String title) {
            return new Context(container, title, null, null, false, false, false, false,
                    null, null, null, null, null, null);
        }
    }

    private static final Map<String, String> DEFAULT_GUI = Map.ofEntries(
            Map.entry("chest", "container/generic_54"),
            Map.entry("shulker_box", "container/shulker_box"),
            Map.entry("anvil", "container/anvil"),
            Map.entry("beacon", "container/beacon"),
            Map.entry("brewing_stand", "container/brewing_stand"),
            Map.entry("crafting", "container/crafting_table"),
            Map.entry("dispenser", "container/dispenser"),
            Map.entry("enchantment", "container/enchanting_table"),
            Map.entry("furnace", "container/furnace"),
            Map.entry("hopper", "container/hopper"),
            Map.entry("horse", "container/horse"),
            Map.entry("villager", "container/villager"),
            Map.entry("inventory", "container/inventory"),
            Map.entry("blast_furnace", "container/blast_furnace"),
            Map.entry("smoker", "container/smoker"),
            Map.entry("grindstone", "container/grindstone"),
            Map.entry("stonecutter", "container/stonecutter"),
            Map.entry("loom", "container/loom"),
            Map.entry("smithing_table", "container/smithing"),
            Map.entry("cartography_table", "container/cartography_table"),
            Map.entry("crafter", "container/crafter")
    );

    public record Rule(String source, String container, Map<String,String> explicitTextures,
                       String defaultTexture, Properties properties) {

        public static Optional<Rule> parse(String namespace, String path, Properties p) {
            String container = p.getProperty("container", "").strip();
            if (container.isEmpty()) return Optional.empty();
            String def = image(namespace, path, p.getProperty("texture"));
            Map<String,String> explicit = new HashMap<>();
            for (String key : p.stringPropertyNames()) {
                if (!key.startsWith("texture.") || key.length() == 8) continue;
                String id = key.substring(8);
                String texture = image(namespace, path, p.getProperty(key));
                if (texture == null) continue;
                if (!id.endsWith(".png")) id += ".png";
                if (id.startsWith("/")) id = id.substring(1);
                // The texture.PATH key is relative to textures/gui/ (OptiFine spec).
                explicit.put("minecraft:textures/gui/" + id, texture);
            }
            if (def == null && explicit.isEmpty()) return Optional.empty();
            return Optional.of(new Rule(namespace + ":" + path, container,
                    Map.copyOf(explicit), def, p));
        }

        public boolean matches(Context c) {
            if (c == null || !container.equals(c.container())) return false;
            String origin = properties.getProperty("nativegui.origin");
            if (origin != null && !origin.equals(c.variant())) return false;
            String name = properties.getProperty("name");
            if (name != null && !matchName(name, c.title())) return false;
            String biomes = properties.getProperty("biomes");
            if (biomes != null && !wordList(biomes, c.biome(), true)) return false;
            String heights = properties.getProperty("heights");
            if (heights != null && (c.y() == null || !intRanges(heights, c.y()))) return false;

            if (container.equals("chest")) {
                // Default values of the chest flags are false per the published format.
                if (getBoolean("large", false) != c.large()) return false;
                if (getBoolean("trapped", false) != c.trapped()) return false;
                if (getBoolean("ender", false) != c.ender()) return false;
                if (getBoolean("christmas", false) != c.christmas()) return false;
            }
            String colors = properties.getProperty("colors");
            if (colors != null && !wordList(colors, c.color(), false)) return false;
            String variants = properties.getProperty("variants");
            if (variants != null && !wordList(variants, c.variant(), false)) return false;
            String levels = properties.getProperty("levels");
            if (levels != null && (c.beaconLevel() == null || !intRanges(levels, c.beaconLevel()))) return false;
            String professions = properties.getProperty("professions");
            if (professions != null && !matchProfessions(professions, c.villagerProfession(), c.villagerLevel())) return false;
            return true;
        }

        private boolean getBoolean(String key, boolean defaultValue) {
            String value = properties.getProperty(key);
            return value == null ? defaultValue : Boolean.parseBoolean(value.strip());
        }

        public String replacement(String original, Context context) {
            if (!matches(context)) return null;
            String explicit = explicitTextures.get(original);
            if (explicit != null) return explicit;
            // Avoid accidentally repainting widgets and other unrelated GUI textures.
            String canonical = DEFAULT_GUI.get(container);
            if (defaultTexture != null && canonical != null &&
                    original.equals("minecraft:textures/gui/" + canonical + ".png")) {
                return defaultTexture;
            }
            return null;
        }
    }

    /** First rule wins. Caller orders rules by pack priority and then file path. */
    public static String choose(String original, Context context, List<Rule> rules) {
        for (Rule rule : rules) {
            String replacement = rule.replacement(original, context);
            if (replacement != null) return replacement;
        }
        return original;
    }

    /** Returns a texture resource ID including the .png extension, or null on bad inputs. */
    public static String image(String ownerNamespace, String propertiesPath, String raw) {
        if (raw == null || raw.isBlank()) return null;
        String path = raw.strip().replace('\\', '/');
        String namespace = ownerNamespace;
        int colon = path.indexOf(':');
        if (colon >= 0) {
            namespace = path.substring(0, colon);
            path = path.substring(colon + 1);
        }
        if (path.startsWith("~/")) { // OptiFine's shortcut for optifine/
            namespace = "minecraft";
            path = "optifine/" + path.substring(2);
        } else if (path.startsWith("/")) {
            path = path.substring(1);
        } else if (colon < 0 && !path.startsWith("textures/") &&
                !path.startsWith("optifine/") && !path.startsWith("assets/")) {
            String base = propertiesPath.substring(0, propertiesPath.lastIndexOf('/') + 1);
            path = base + path;
        }
        if (path.startsWith("assets/")) return null; // not a valid namespace-relative path
        try {
            path = normalize(path);
        } catch (IllegalArgumentException ex) { return null; }
        if (!path.endsWith(".png")) path += ".png";
        if (!namespace.matches("[a-z0-9_.-]+") || !path.matches("[a-z0-9_./-]+")) return null;
        return namespace + ":" + path;
    }

    private static String normalize(String path) {
        Deque<String> parts = new ArrayDeque<>();
        for (String part : path.split("/")) {
            if (part.isEmpty() || part.equals(".")) continue;
            if (part.equals("..")) {
                if (parts.isEmpty()) throw new IllegalArgumentException("Path escapes namespace");
                parts.removeLast();
            } else parts.addLast(part);
        }
        return String.join("/", parts);
    }

    public static boolean wordList(String list, String actual, boolean namespaceAware) {
        if (actual == null) return false;
        String normalized = namespaceAware && actual.startsWith("minecraft:") ? actual.substring(10) : actual;
        for (String word : list.strip().split("[\\s,]+")) {
            String option = namespaceAware && word.startsWith("minecraft:") ? word.substring(10) : word;
            if (normalized.equals(option)) return true;
        }
        return false;
    }

    public static boolean intRanges(String spec, int value) {
        for (String range : spec.strip().split("[\\s,]+")) {
            if (range.isEmpty()) continue;
            try {
                if (range.matches("-?\\d+")) {
                    if (Integer.parseInt(range) == value) return true;
                } else {
                    // Parentheses disambiguate negative endpoints: (-8)-(-1)
                    var pattern = Pattern.compile("\\(?(-?\\d+)\\)?-\\(?(-?\\d+)\\)?").matcher(range);
                    if (pattern.matches()) {
                        int min = Integer.parseInt(pattern.group(1));
                        int max = Integer.parseInt(pattern.group(2));
                        if (value >= min && value <= max) return true;
                    }
                }
            } catch (NumberFormatException ignored) { /* Ignore malformed token. */ }
        }
        return false;
    }

    private static boolean matchProfessions(String list, String profession, Integer level) {
        if (profession == null) return false;
        for (String token : list.strip().split("\\s+")) {
            // Strip the optional minecraft: namespace before parsing the optional :level list.
            String candidate = token.startsWith("minecraft:") ? token.substring(10) : token;
            String[] segments = candidate.split(":", 2);
            if (!profession.equals(segments[0])) continue;
            if (segments.length == 1) return true;
            if (level != null && intRanges(segments[1], level)) return true;
        }
        return false;
    }

    public static boolean matchName(String expression, String title) {
        if (title == null) return false;
        String matcher = expression.strip();
        boolean negate = matcher.startsWith("!");
        if (negate) matcher = matcher.substring(1);
        boolean result;
        try {
            if (matcher.startsWith("regex:")) result = Pattern.matches(matcher.substring(6), title);
            else if (matcher.startsWith("iregex:"))
                result = Pattern.compile(matcher.substring(7), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                        .matcher(title).matches();
            else if (matcher.startsWith("pattern:") || matcher.startsWith("ipattern:")) {
                boolean insensitive = matcher.startsWith("ipattern:");
                String wildcard = matcher.substring(insensitive ? 9 : 8);
                StringBuilder regex = new StringBuilder("^");
                for (char ch : wildcard.toCharArray()) {
                    if (ch == '*') regex.append(".*");
                    else if (ch == '?') regex.append('.');
                    else regex.append(Pattern.quote(String.valueOf(ch)));
                }
                regex.append('$');
                result = Pattern.compile(regex.toString(), insensitive ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : 0)
                        .matcher(title).matches();
            } else result = matcher.equals(title);
        } catch (PatternSyntaxException ex) { return false; }
        return negate != result;
    }
}
