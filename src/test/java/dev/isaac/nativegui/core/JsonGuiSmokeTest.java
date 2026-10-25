package dev.isaac.nativegui.core;

/** Exercises the live Gson-backed JSON adapter during the Gradle verification step. */
public final class JsonGuiSmokeTest {
    private static int assertions;
    public static void main(String[] args) {
        String base = """
              {"containers":["chest", "example:modded_chest"],
               "textures":{"textures/gui/container/generic_54.png":"minecraft:optigui/gui/modern.png",
                "example:textures/gui/box.png":"example:textures/gui/new_box.png"}}
              """;
        var parsed = JsonGuiRules.parse("example", "optigui/gui/demo.json", base).orElseThrow();
        eq("minecraft:optigui/gui/modern.png", parsed.replacement(
                "minecraft:textures/gui/container/generic_54.png", context("minecraft:chest", "minecraft:plains")));
        eq("example:textures/gui/new_box.png", parsed.replacement(
                "example:textures/gui/box.png", context("example:modded_chest", "minecraft:plains")));
        eq(null, parsed.replacement("minecraft:textures/gui/container/generic_54.png",
                context("minecraft:barrel", "minecraft:plains")));

        String biomeRule = """
              {"containers":"minecraft:chest", "textures":{"textures/gui/container/generic_54.png":"minecraft:optigui/gui/desert.png"},
                "match":{"@biome":{"wildcard":"minecraft:*desert*"}}}
              """;
        var desert = JsonGuiRules.parse("minecraft", "optigui/gui/desert.json", biomeRule).orElseThrow();
        eq("minecraft:optigui/gui/desert.png", desert.replacement(
                "minecraft:textures/gui/container/generic_54.png", context("minecraft:chest", "minecraft:desert")));
        eq(null, desert.replacement("minecraft:textures/gui/container/generic_54.png",
                context("minecraft:chest", "minecraft:plains")));
        eq(null, desert.replacement("minecraft:textures/gui/container/generic_54.png",
                context("minecraft:chest", null)));

        // Genuine OptiGUI 3 screen-title condition: literal titles and translations
        // are distinct NBT nodes. Matching both is an OR of independently typed titles.
        String nameRule = """
              {"containers":"minecraft:chest",
               "textures":{"textures/gui/container/generic_54.png":"minecraft:optigui/gui/named.png"},
               "match":{"@screen":{"@title":[
                 {"@text":{"wildcard":"NativeGUI *"}},
                 {"@translate":"container.chest"}
               ]}}}
              """;
        var titled = JsonGuiRules.parse("minecraft", "optigui/gui/name.json", nameRule).orElseThrow();
        String standard = "minecraft:textures/gui/container/generic_54.png";
        eq("minecraft:optigui/gui/named.png", titled.replacement(standard, titled("NativeGUI JSON Test", null)));
        eq("minecraft:optigui/gui/named.png", titled.replacement(standard, titled("Chest", "container.chest")));
        eq(null, titled.replacement(standard, titled("Ordinary Named Box", null)));
        eq(null, titled.replacement(standard, titled("NativeGUI JSON Test", "custom.translation.key")));
        empty(JsonGuiRules.parse("minecraft", "optigui/gui/unknown_screen.json", """
              {"containers":"chest","textures":{"textures/gui/container/generic_54.png":"optigui/gui/a.png"},
               "match":{"@screen":{"@player_name":"Alex"}}}
              """));
        empty(JsonGuiRules.parse("minecraft", "optigui/gui/unknown_title.json", """
              {"containers":"chest","textures":{"textures/gui/container/generic_54.png":"optigui/gui/a.png"},
               "match":{"@screen":{"@title":{"@raw":"Alex"}}}}
              """));

        // Unknown NBT selectors must NOT accidentally turn into an unconditional rule.
        empty(JsonGuiRules.parse("minecraft", "optigui/gui/unsupported.json", """
             {"containers":"chest", "textures":{"textures/gui/container/generic_54.png":"optigui/gui/a.png"},
               "match":{"@block_entity":{"@SomeUnknownTag":7}}}
             """));
        empty(JsonGuiRules.parse("minecraft", "optigui/gui/unknown_if.json", """
             {"containers":"chest", "textures":{"textures/gui/container/generic_54.png":"optigui/gui/a.png"},
               "if":{"@mods":{"@foo":"bar"}}}
             """));
        empty(JsonGuiRules.parse("minecraft", "optigui/gui/bad_uri.json", """
             {"containers":"chest", "textures":{"textures/gui/container/generic_54.png":"../../bad.png"}}
             """));
        empty(JsonGuiRules.parse("minecraft", "optigui/gui/bad_texture.json", """
             {"containers":"chest", "textures":{"textures/gui/container/generic_54.png":"example:optigui/gui/no.png.txt"}}
             """));
        System.out.println("Passed " + assertions + " Gson-backed OptiGUI 3 JSON checks.");
    }

    private static LegacyGuiRules.Context titled(String label, String translationKey) {
        return new LegacyGuiRules.Context("chest", label, "minecraft:plains", 64,
                false, false, false, false, null, null, null, null, null,
                "minecraft:chest", translationKey);
    }

    private static LegacyGuiRules.Context context(String id, String biome) {
        return new LegacyGuiRules.Context("chest", "Chest", biome, 70,
                false, false, false, false, null, null, null, null, null, id);
    }
    private static void empty(java.util.Optional<?> value) {
        assertions++;
        if (value.isPresent()) throw new AssertionError("Unexpected resource accepted: " + value);
    }
    private static void eq(Object expected, Object actual) {
        assertions++;
        if (!java.util.Objects.equals(expected, actual))
            throw new AssertionError("Expected " + expected + " but got " + actual);
    }
}
