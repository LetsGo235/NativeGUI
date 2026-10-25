package dev.isaac.nativegui.core;

import java.util.*;

/**
 * Independently written, conservative reader for the published OptiGUI 2.x INI format.
 * Unsupported selectors make a group ineligible, so a conditional skin is never
 * incorrectly applied to an unrelated container.
 */
public final class OptiGuiIniSubset {
    private OptiGuiIniSubset() {}

    private static final Set<String> SUPPORTED = Set.of(
            "replacement", "interaction.texture", "load.priority",
            "name", "name.wildcard", "name.wildcard.ignore_case", "name.regex", "name.regex.ignore_case",
            "biomes", "heights", "chest.large", "shulker_box.colors", "llama.colors",
            "beacon.levels", "villager.professions"
    );

    public static List<LegacyGuiRules.Rule> parse(String sourcePath, String text) {
        List<LegacyGuiRules.Rule> rules = new ArrayList<>();
        String[] lines = text.replace("\r", "").split("\n");
        String group = null;
        Map<String,String> values = new LinkedHashMap<>();
        int ordinal = 0;
        for (String raw : lines) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) continue;
            if (line.startsWith("[") && line.endsWith("]")) {
                if (group != null) rules.addAll(convert(sourcePath, group, values, ordinal++));
                group = line.substring(1, line.length()-1).strip();
                values = new LinkedHashMap<>();
                continue;
            }
            if (group == null) continue;
            int equals = line.indexOf('=');
            if (equals <= 0) continue;
            values.put(line.substring(0, equals).strip(), line.substring(equals+1).strip());
        }
        if (group != null) rules.addAll(convert(sourcePath, group, values, ordinal));
        return List.copyOf(rules);
    }

    private static List<LegacyGuiRules.Rule> convert(String file, String header, Map<String,String> ini, int ordinal) {
        if (!ini.keySet().stream().allMatch(SUPPORTED::contains)) return List.of();
        long nameSelectors = ini.keySet().stream().filter(k -> k.equals("name") || k.startsWith("name.")).count();
        if (nameSelectors > 1) return List.of(); // Avoid weakening simultaneous selectors.
        String replacement = ini.get("replacement");
        if (replacement == null || replacement.isBlank()) return List.of();
        List<LegacyGuiRules.Rule> out = new ArrayList<>();
        for (String raw : header.split("\\s+")) {
            if (raw.isEmpty() || raw.startsWith("#")) continue;
            String block = raw.startsWith("minecraft:") ? raw.substring(10) : raw;
            // Currently vanilla only. Unknown/modded containers are not guessed.
            if (block.contains(":")) continue;
            Properties p = new Properties();
            String container = toContainer(block);
            if (container == null) continue;
            p.setProperty("container", container);
            p.setProperty("texture", replacement);
            if (container.equals("chest")) {
                if (block.equals("barrel")) p.setProperty("nativegui.origin", "barrel");
                else if (block.equals("trapped_chest")) {
                    p.setProperty("nativegui.origin", "chest");
                    p.setProperty("trapped", "true");
                } else if (block.equals("ender_chest")) {
                    p.setProperty("nativegui.origin", "ender_chest");
                    p.setProperty("ender", "true");
                }
                else p.setProperty("nativegui.origin", "chest");
            }
            if (block.equals("dropper")) p.setProperty("variants", "dropper");
            if (block.equals("dispenser")) p.setProperty("variants", "dispenser");
            copy(ini, p, "name", "name");
            copyTransform(ini, p, "name.wildcard", "name", "pattern:");
            copyTransform(ini, p, "name.wildcard.ignore_case", "name", "ipattern:");
            copyTransform(ini, p, "name.regex", "name", "regex:");
            copyTransform(ini, p, "name.regex.ignore_case", "name", "iregex:");
            copy(ini, p, "biomes", "biomes");
            copy(ini, p, "heights", "heights");
            copy(ini, p, "chest.large", "large");
            copy(ini, p, "shulker_box.colors", "colors");
            copy(ini, p, "llama.colors", "colors");
            copy(ini, p, "beacon.levels", "levels");
            copy(ini, p, "villager.professions", "professions");
            copy(ini, p, "load.priority", "nativegui.priority");
            // Preserve the exact source texture when explicitly given, not only the vanilla GUI.
            String target = ini.get("interaction.texture");
            if (target != null && !target.isBlank()) {
                String full = LegacyGuiRules.image("optigui", file, target);
                if (full == null || !full.startsWith("minecraft:textures/gui/")) continue;
                p.remove("texture");
                String pathKey = full.substring("minecraft:textures/gui/".length());
                p.setProperty("texture." + pathKey, replacement);
            }
            LegacyGuiRules.Rule.parse("optigui", file, p).ifPresent(out::add);
        }
        return out;
    }

    private static String toContainer(String block) {
        return switch (block) {
            case "chest", "barrel", "trapped_chest", "ender_chest" -> "chest";
            case "shulker_box" -> "shulker_box";
            case "anvil" -> "anvil";
            case "beacon", "brewing_stand", "dispenser", "hopper", "furnace" -> block;
            case "dropper" -> "dispenser";
            case "crafting_table" -> "crafting";
            case "enchanting_table" -> "enchantment";
            case "horse" -> "horse";
            case "villager" -> "villager";
            case "inventory" -> "inventory";
            case "blast_furnace", "smoker", "grindstone", "stonecutter", "loom",
                    "smithing_table", "cartography_table", "crafter" -> block;
            default -> null;
        };
    }
    private static void copy(Map<String,String> in, Properties out, String from, String to) {
        if (in.containsKey(from)) out.setProperty(to, in.get(from));
    }
    private static void copyTransform(Map<String,String> in, Properties out, String from, String to, String prefix) {
        if (in.containsKey(from)) out.setProperty(to, prefix + in.get(from));
    }
}
