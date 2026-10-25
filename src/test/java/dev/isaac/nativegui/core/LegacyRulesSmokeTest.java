package dev.isaac.nativegui.core;

import java.util.List;
import java.util.Properties;

/** Run without NeoForge: javac ... && java -ea dev.isaac.nativegui.core.LegacyRulesSmokeTest */
public final class LegacyRulesSmokeTest {
    static int checks = 0;
    public static void main(String[] args) {
        // Regression: the alpha.1 slash suffix aborted startup's resource reload.
        no(GuiScanPaths.OPTIFINE_SCAN_ROOT.endsWith("/"));
        no(GuiScanPaths.OPTIGUI_SCAN_ROOT.endsWith("/"));
        yes(GuiScanPaths.isLegacyRule("optifine/gui/container/chest/name.properties"));
        no(GuiScanPaths.isLegacyRule("optifine/gui/container_bak/chest/name.properties"));
        yes(GuiScanPaths.isIniRule("optigui", "gui/barrel.ini"));
        no(GuiScanPaths.isIniRule("minecraft", "gui/barrel.ini"));
        no(GuiScanPaths.isIniRule("optigui", "gui_backup/barrel.ini"));
        eq("minecraft:optifine/gui/container/test.png",
                LegacyGuiRules.image("minecraft", "optifine/gui/container/rule.properties", "test"));
        eq("minecraft:optifine/gui/container/other.png",
                LegacyGuiRules.image("minecraft", "optifine/gui/container/sub/a.properties", "../other.png"));
        eq("minecraft:optifine/gui/container/a.png",
                LegacyGuiRules.image("minecraft", "optifine/gui/container/b.properties", "~/gui/container/a"));
        eq("custom:gui/my.png",
                LegacyGuiRules.image("minecraft", "optifine/gui/container/b.properties", "custom:gui/my.png"));
        yes(LegacyGuiRules.intRanges("(-64)-(-1) 10-30", -8));
        no(LegacyGuiRules.intRanges("(-64)-(-1) 10-30", 5));
        yes(LegacyGuiRules.matchName("pattern:Treasure *", "Treasure Box"));
        yes(LegacyGuiRules.matchName("iregex:my.*chest", "MY SUPER CHEST"));
        no(LegacyGuiRules.matchName("regex:my.*", "MY stuff"));
        yes(LegacyGuiRules.matchName("!ipattern:secret*", "Ordinary Chest"));

        Properties p = new Properties();
        p.setProperty("container", "chest");
        p.setProperty("texture", "azure_chest");
        p.setProperty("name", "pattern:Azure*");
        LegacyGuiRules.Rule named = LegacyGuiRules.Rule.parse("minecraft", "optifine/gui/container/chest.properties", p).orElseThrow();
        var chest = LegacyGuiRules.Context.basic("chest", "Azure Storage");
        eq("minecraft:optifine/gui/container/azure_chest.png", named.replacement(
                "minecraft:textures/gui/container/generic_54.png", chest));
        eq(null, named.replacement("minecraft:textures/gui/container/hopper.png", chest));
        eq(null, named.replacement("minecraft:textures/gui/container/generic_54.png",
                LegacyGuiRules.Context.basic("chest", "Wrong Name")));
        eq(null, named.replacement("minecraft:textures/gui/container/generic_54.png",
                LegacyGuiRules.Context.basic("inventory", "Azure Storage")));

        Properties explicit = new Properties();
        explicit.setProperty("container", "creative");
        explicit.setProperty("texture.container/creative_inventory/tab_inventory", "~/gui/container/creative_tab.png");
        var creative = LegacyGuiRules.Rule.parse("minecraft", "optifine/gui/container/creative/tabs.properties", explicit).orElseThrow();
        eq("minecraft:optifine/gui/container/creative_tab.png", creative.replacement(
                "minecraft:textures/gui/container/creative_inventory/tab_inventory.png",
                LegacyGuiRules.Context.basic("creative", "Creative")));
        eq(null, creative.replacement("minecraft:textures/gui/container/creative_inventory/tab_items.png",
                LegacyGuiRules.Context.basic("creative", "Creative")));

        Properties biome = new Properties();
        biome.setProperty("container", "chest");
        biome.setProperty("texture", "desert.png");
        biome.setProperty("biomes", "desert minecraft:badlands");
        biome.setProperty("heights", "60-100");
        var desert = LegacyGuiRules.Rule.parse("minecraft", "optifine/gui/container/desert.properties", biome).orElseThrow();
        var inDesert = new LegacyGuiRules.Context("chest", "Chest", "minecraft:desert", 70,
                false, false, false, false, null, null, null, null, null, null);
        yes(desert.matches(inDesert));
        no(desert.matches(LegacyGuiRules.Context.basic("chest", "Chest")));
        eq("minecraft:optifine/gui/container/desert.png", LegacyGuiRules.choose(
                "minecraft:textures/gui/container/generic_54.png", inDesert, List.of(desert,named)));

        Properties doubleChest = new Properties();
        doubleChest.setProperty("container", "chest");
        doubleChest.setProperty("texture", "big");
        doubleChest.setProperty("large", "true");
        var big = LegacyGuiRules.Rule.parse("minecraft", "optifine/gui/container/big.properties", doubleChest).orElseThrow();
        no(big.matches(inDesert));
        yes(big.matches(new LegacyGuiRules.Context("chest", "Chest", null,null,true,false,false,false,null,null,null,null,null,null)));

        String ini = """
            [minecraft:barrel]
            replacement = barrel.png
            name.wildcard.ignore_case = loot*
            load.priority = 9

            [minecraft:chest #2]
            replacement = special.png
            chest.large = true

            [minecraft:dropper]
            replacement = dropper.png

            [minecraft:chest #3]
            replacement = dont_load.png
            player.name = unsupported-safeguard
            """;
        List<LegacyGuiRules.Rule> parsedIni = OptiGuiIniSubset.parse("gui/demo.ini", ini);
        eq(3, parsedIni.size());
        eq("optigui:gui/barrel.png", parsedIni.get(0).defaultTexture());
        eq("9", parsedIni.get(0).properties().getProperty("nativegui.priority"));
        yes(parsedIni.get(0).matches(new LegacyGuiRules.Context("chest", "LOOT Storage", null, null,
                false, false, false, false, null, "barrel", null, null, null, null)));
        no(parsedIni.get(0).matches(new LegacyGuiRules.Context("chest", "LOOT Storage", null, null,
                false, false, false, false, null, "chest", null, null, null, null)));
        yes(parsedIni.get(1).matches(new LegacyGuiRules.Context("chest", "Chest", null, null,
                true, false, false, false, null, "chest", null, null, null, null)));
        no(parsedIni.get(1).matches(new LegacyGuiRules.Context("chest", "Chest", null, null,
                false, false, false, false, null, "chest", null, null, null, null)));
        yes(parsedIni.get(2).matches(new LegacyGuiRules.Context("dispenser", "Dropper", null, null,
                false, false, false, false, null, "dropper", null, null, null, null)));
        no(parsedIni.get(2).matches(new LegacyGuiRules.Context("dispenser", "Dropper", null, null,
                false, false, false, false, null, "dispenser", null, null, null, null)));

        // Release gate: actual color conditions, not just a resource image.
        Properties purpleShulker = new Properties();
        purpleShulker.setProperty("container", "shulker_box");
        purpleShulker.setProperty("colors", "purple");
        purpleShulker.setProperty("texture", "purple");
        var purple = LegacyGuiRules.Rule.parse("minecraft",
                "optifine/gui/container/shulker/purple.properties", purpleShulker).orElseThrow();
        var purpleContext = new LegacyGuiRules.Context("shulker_box", "Shulker Box", null, 64,
                false, false, false, false, "purple", null, null, null, null, "minecraft:purple_shulker_box");
        var redContext = new LegacyGuiRules.Context("shulker_box", "Shulker Box", null, 64,
                false, false, false, false, "red", null, null, null, null, "minecraft:red_shulker_box");
        eq("minecraft:optifine/gui/container/shulker/purple.png", purple.replacement(
                "minecraft:textures/gui/container/shulker_box.png", purpleContext));
        eq(null, purple.replacement("minecraft:textures/gui/container/shulker_box.png", redContext));
        eq(null, purple.replacement("minecraft:textures/gui/container/shulker_box.png",
                LegacyGuiRules.Context.basic("shulker_box", "Shulker Box")));

        // Test pack-vs-rule priority semantics and deterministic filename ordering.
        var highPack = new GuiRuleOrder.Key(3, 0, "z.properties");
        var lowPack = new GuiRuleOrder.Key(2, 900, "a.properties");
        yes(GuiRuleOrder.COMPARATOR.compare(highPack, lowPack) < 0);
        yes(GuiRuleOrder.COMPARATOR.compare(new GuiRuleOrder.Key(3, 5, "z.ini"), highPack) < 0);
        yes(GuiRuleOrder.COMPARATOR.compare(new GuiRuleOrder.Key(3, 0, "a.properties"), highPack) < 0);
        yes(GuiScanPaths.isJsonRule("optigui/gui/myrule.json"));
        yes(GuiScanPaths.isJsonRule("optigui/gui/subdir/test.json"));
        no(GuiScanPaths.isJsonRule("optigui/gui_backup/test.json"));
        no(GuiScanPaths.OPTIGUI3_SCAN_ROOT.endsWith("/"));
        System.out.println("Passed " + checks + " standalone legacy-format compatibility checks.");
    }
    static void yes(boolean value) { checks++; if (!value) throw new AssertionError("Expected true"); }
    static void no(boolean value) { checks++; if (value) throw new AssertionError("Expected false"); }
    static void eq(Object a, Object b) { checks++; if (!java.util.Objects.equals(a,b)) throw new AssertionError("Expected "+a+" but got "+b); }
}
