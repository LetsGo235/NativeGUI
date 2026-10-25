package dev.isaac.nativegui.client;

import dev.isaac.nativegui.NativeGui;
import dev.isaac.nativegui.core.GuiRuleOrder;
import dev.isaac.nativegui.core.GuiScanPaths;
import dev.isaac.nativegui.core.JsonGuiRules;
import dev.isaac.nativegui.core.LegacyGuiRules;
import dev.isaac.nativegui.core.OptiGuiIniSubset;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** All formats are indexed once per resource reload, never every frame. */
public final class GuiPackReload extends SimplePreparableReloadListener<List<GuiPackReload.ActiveRule>> {
    public static final GuiPackReload INSTANCE = new GuiPackReload();
    private static final byte[] PNG_SIGNATURE = {(byte)137, 80, 78, 71, 13, 10, 26, 10};
    private volatile List<ActiveRule> rules = List.of();

    private GuiPackReload() {}
    public List<ActiveRule> rules() { return rules; }

    public record ActiveRule(LegacyGuiRules.Rule legacy, JsonGuiRules.Rule modern) {
        public String replacement(String original, LegacyGuiRules.Context context) {
            return legacy != null ? legacy.replacement(original, context) : modern.replacement(original, context);
        }
        static ActiveRule old(LegacyGuiRules.Rule r) { return new ActiveRule(r, null); }
        static ActiveRule modern(JsonGuiRules.Rule r) { return new ActiveRule(null, r); }
    }
    private record Loaded(GuiRuleOrder.Key order, ActiveRule rule) {}

    @Override
    protected List<ActiveRule> prepare(ResourceManager manager, ProfilerFiller profiler) {
        try { return scanRules(manager); }
        catch (RuntimeException ex) {
            // A malformed pack or unexpected resource-manager exception must never
            // crash Minecraft or provoke a second global reload.
            NativeGui.LOGGER.error("NativeGUI scanner failed; reverting to vanilla GUI textures", ex);
            return List.of();
        }
    }

    private static List<ActiveRule> scanRules(ResourceManager manager) {
        List<String> packs = manager.listPacks().map(pack -> pack.packId()).toList();
        Map<String,Integer> priority = new HashMap<>();
        for (int i = 0; i < packs.size(); i++) priority.put(packs.get(i), i);
        List<Loaded> loaded = new ArrayList<>();

        // Scanner roots must NEVER end with '/', see the alpha.1 startup regression.
        var legacy = manager.listResourceStacks(GuiScanPaths.OPTIFINE_SCAN_ROOT,
                id -> GuiScanPaths.isLegacyRule(id.getPath()));
        for (var entry : legacy.entrySet()) {
            ResourceLocation id = entry.getKey();
            for (Resource resource : entry.getValue()) {
                try (var stream = resource.open()) {
                    Properties p = new Properties();
                    p.load(stream);
                    LegacyGuiRules.Rule.parse(id.getNamespace(), id.getPath(), p).ifPresent(rule ->
                            loaded.add(new Loaded(new GuiRuleOrder.Key(
                                    priority.getOrDefault(resource.sourcePackId(), -1), 0, id.toString()),
                                    ActiveRule.old(rule))));
                } catch (IOException | RuntimeException ex) {
                    NativeGui.LOGGER.warn("Skipping invalid OptiFine GUI rule {} ({})", id, ex.toString());
                }
            }
        }

        var ini = manager.listResourceStacks(GuiScanPaths.OPTIGUI_SCAN_ROOT,
                id -> GuiScanPaths.isIniRule(id.getNamespace(), id.getPath()));
        for (var entry : ini.entrySet()) {
            ResourceLocation id = entry.getKey();
            for (Resource resource : entry.getValue()) {
                try (var stream = resource.open()) {
                    String contents = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                    List<LegacyGuiRules.Rule> converted = OptiGuiIniSubset.parse(id.getPath(), contents);
                    for (int i = 0; i < converted.size(); i++) {
                        var rule = converted.get(i);
                        int rank = parsePriority(rule.properties().getProperty("nativegui.priority"));
                        loaded.add(new Loaded(new GuiRuleOrder.Key(
                                priority.getOrDefault(resource.sourcePackId(), -1), rank,
                                id + "#" + String.format(Locale.ROOT, "%05d", i)), ActiveRule.old(rule)));
                    }
                } catch (IOException | RuntimeException ex) {
                    NativeGui.LOGGER.warn("Skipping invalid historical OptiGUI INI {} ({})", id, ex.toString());
                }
            }
        }

        // Modern OptiGUI 3 resources can reside in ANY namespace.
        var modern = manager.listResourceStacks(GuiScanPaths.OPTIGUI3_SCAN_ROOT,
                id -> GuiScanPaths.isJsonRule(id.getPath()));
        for (var entry : modern.entrySet()) {
            ResourceLocation id = entry.getKey();
            for (Resource resource : entry.getValue()) {
                try (var stream = resource.open()) {
                    String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                    JsonGuiRules.parse(id.getNamespace(), id.getPath(), json).ifPresent(rule ->
                            loaded.add(new Loaded(new GuiRuleOrder.Key(
                                    priority.getOrDefault(resource.sourcePackId(), -1), 0, id.toString()),
                                    ActiveRule.modern(rule))));
                } catch (IOException | RuntimeException ex) {
                    NativeGui.LOGGER.warn("Skipping invalid OptiGUI 3 JSON {} ({})", id, ex.toString());
                }
            }
        }

        loaded.sort(Comparator.comparing(Loaded::order, GuiRuleOrder.COMPARATOR));
        List<ActiveRule> valid = new ArrayList<>();
        // Avoid opening/checking identical textures repeatedly across many rules.
        Map<String, Boolean> found = new HashMap<>();
        for (Loaded load : loaded) {
            ActiveRule rule = load.rule();
            if (rule.legacy() != null) {
                var original = rule.legacy();
                String main = original.defaultTexture();
                if (main != null && !isPng(manager, found, main)) main = null;
                Map<String,String> targets = new HashMap<>();
                for (var entry : original.explicitTextures().entrySet()) {
                    if (isPng(manager, found, entry.getValue())) targets.put(entry.getKey(), entry.getValue());
                }
                if (main != null || !targets.isEmpty()) {
                    valid.add(ActiveRule.old(new LegacyGuiRules.Rule(original.source(), original.container(),
                            Map.copyOf(targets), main, original.properties())));
                }
            } else {
                var original = rule.modern();
                Map<String,String> targets = new HashMap<>();
                for (var entry : original.textures().entrySet()) {
                    if (isPng(manager, found, entry.getValue())) targets.put(entry.getKey(), entry.getValue());
                }
                if (!targets.isEmpty()) valid.add(ActiveRule.modern(original.withTextures(targets)));
            }
        }
        return List.copyOf(valid);
    }

    private static int parsePriority(String value) {
        if (value == null) return 0;
        try { return Integer.parseInt(value.strip()); }
        catch (NumberFormatException ex) { return 0; }
    }

    private static boolean isPng(ResourceManager manager, Map<String,Boolean> found, String texture) {
        return found.computeIfAbsent(texture, name -> {
            ResourceLocation id = ResourceLocation.tryParse(name);
            if (id == null || !id.getPath().endsWith(".png")) return false;
            Optional<Resource> resource = manager.getResource(id);
            if (resource.isEmpty()) return false;
            try (var stream = resource.get().open()) {
                return Arrays.equals(PNG_SIGNATURE, stream.readNBytes(PNG_SIGNATURE.length));
            } catch (IOException ex) {
                NativeGui.LOGGER.warn("Unreadable PNG for custom GUI: {}", name, ex);
                return false;
            }
        });
    }

    @Override
    protected void apply(List<ActiveRule> prepared, ResourceManager manager, ProfilerFiller profiler) {
        rules = prepared; // Volatile replacement invalidates all per-screen caches atomically.
        NativeGui.LOGGER.info("NativeGUI indexed {} supported custom GUI rules", prepared.size());
    }
}
