package dev.isaac.nativegui.client;

import dev.isaac.nativegui.core.LegacyGuiRules;
import dev.isaac.nativegui.NativeGui;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DropperBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** GUI-time texture replacement, without intercepting inventory slots or game textures. */
public final class GuiTextureRouter {
    private GuiTextureRouter() {}
    private static final boolean DEBUG = Boolean.getBoolean("nativegui.debug");
    private static Screen lastScreen;
    private static LegacyGuiRules.Context lastContext;
    private static List<GuiPackReload.ActiveRule> lastRules;
    private static final Map<ResourceLocation, ResourceLocation> replacements = new HashMap<>();

    public static ResourceLocation route(ResourceLocation original) {
        // JSON packs may target modded texture namespaces. Only texture locations
        // can be rewritten and only when explicitly listed in an active rule.
        if (!original.getPath().startsWith("textures/")) return original;
        List<GuiPackReload.ActiveRule> rules = GuiPackReload.INSTANCE.rules();
        if (rules.isEmpty()) return original;
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = minecraft.screen;
        if (screen == null || !(screen instanceof AbstractContainerScreen<?>)) return original;

        // Capture location once when screen first opens; don't track the moving cursor.
        if (screen != lastScreen || rules != lastRules) {
            lastScreen = screen;
            lastRules = rules;
            replacements.clear();
            lastContext = makeContext(minecraft, screen);
            if (DEBUG) NativeGui.LOGGER.info("NativeGUI diagnostic: GUI={} context={} indexedRules={}",
                    screen.getClass().getName(), lastContext, rules.size());
        }
        if (lastContext == null) return original;
        // Per-screen resource IDs are stable, so regex/rule evaluation only occurs
        // once per texture rather than on every blit, every rendered frame.
        return replacements.computeIfAbsent(original, texture -> {
            for (GuiPackReload.ActiveRule rule : rules) {
                String replacement = rule.replacement(texture.toString(), lastContext);
                if (replacement == null) continue;
                ResourceLocation result = ResourceLocation.tryParse(replacement);
                if (result != null) {
                    if (DEBUG) NativeGui.LOGGER.info("NativeGUI diagnostic: {} => {} (rule={})",
                            texture, result, rule.legacy() != null ? rule.legacy().source() : rule.modern().source());
                    return result;
                }
            }
            if (DEBUG) NativeGui.LOGGER.info("NativeGUI diagnostic: no matching rule for {}", texture);
            return texture;
        });
    }

    private static LegacyGuiRules.Context makeContext(Minecraft mc, Screen screen) {
        String kind = classify(screen);
        // JSON compatibility can handle a modded container opened from a known
        // block even when its GUI uses a custom AbstractContainerScreen subclass.
        if (kind == null) kind = "modded";
        BlockPos pos = InteractionTracker.recentClick();
        // The inventory is associated with the player rather than any last clicked block.
        if ((kind.equals("inventory") || kind.equals("creative")) && mc.player != null)
            pos = mc.player.blockPosition();
        BlockState state = pos != null && mc.level != null ? mc.level.getBlockState(pos) : null;
        String sourceId = state == null ? null : BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        InteractionTracker.EntitySnapshot interactedEntity =
                (kind.equals("inventory") || kind.equals("creative"))
                ? null : InteractionTracker.recentEntity();
        if (interactedEntity != null) {
            pos = interactedEntity.pos();
            sourceId = interactedEntity.id();
            state = null;
        }
        if ((kind.equals("inventory") || kind.equals("creative")) && mc.player != null)
            sourceId = "minecraft:player";
        String biome = null;
        if (pos != null && mc.level != null) {
            biome = mc.level.getBiome(pos).unwrapKey()
                    .map(key -> key.location().toString()).orElse(null);
        }
        boolean large = false;
        boolean trapped = false;
        boolean ender = false;
        String color = null;
        String variant = null;
        Integer beaconLevel = screen instanceof BeaconScreen beacon ? beacon.getMenu().getLevels() : null;
        String villagerProfession = null;
        Integer villagerLevel = null;

        if (kind.equals("chest")) {
            if (screen instanceof ContainerScreen chestScreen)
                large = chestScreen.getMenu().getRowCount() >= 6;
            if (state != null) {
                trapped = state.is(Blocks.TRAPPED_CHEST);
                ender = state.is(Blocks.ENDER_CHEST);
                if (state.getBlock() instanceof BarrelBlock) variant = "barrel";
                else if (state.getBlock() instanceof ChestBlock) variant = "chest";
                else if (ender) variant = "ender_chest";
                if (state.getBlock() instanceof ChestBlock)
                    large = state.getValue(ChestBlock.TYPE) != ChestType.SINGLE;
            }
        }
        if (kind.equals("shulker_box") && state != null && state.getBlock() instanceof ShulkerBoxBlock block) {
            if (block.getColor() != null) color = block.getColor().getName();
        }
        if (kind.equals("dispenser") && state != null)
            variant = state.getBlock() instanceof DropperBlock ? "dropper" : "dispenser";
        if (interactedEntity != null) {
            if (kind.equals("horse")) variant = interactedEntity.variant();
            if (kind.equals("villager")) {
                villagerProfession = interactedEntity.villagerProfession();
                villagerLevel = interactedEntity.villagerLevel();
            }
        }
        boolean christmas = false;
        LocalDate date = LocalDate.now();
        if (date.getMonthValue() == 12 && state != null &&
                (state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST)))
            christmas = date.getDayOfMonth() >= 24 && date.getDayOfMonth() <= 26;
        String titleTranslationKey = screen.getTitle().getContents() instanceof TranslatableContents translated
                ? translated.getKey() : null;
        return new LegacyGuiRules.Context(kind, screen.getTitle().getString(), biome,
                pos == null ? null : pos.getY(), large, trapped, ender, christmas,
                color, variant, beaconLevel, villagerProfession, villagerLevel,
                sourceId, titleTranslationKey);
    }

    private static String classify(Screen screen) {
        if (screen instanceof ContainerScreen) return "chest";
        if (screen instanceof ShulkerBoxScreen) return "shulker_box";
        if (screen instanceof AnvilScreen) return "anvil";
        if (screen instanceof BeaconScreen) return "beacon";
        if (screen instanceof BrewingStandScreen) return "brewing_stand";
        if (screen instanceof CraftingScreen) return "crafting";
        if (screen instanceof DispenserScreen) return "dispenser";
        if (screen instanceof EnchantmentScreen) return "enchantment";
        if (screen instanceof FurnaceScreen) return "furnace";
        if (screen instanceof BlastFurnaceScreen) return "blast_furnace";
        if (screen instanceof SmokerScreen) return "smoker";
        if (screen instanceof GrindstoneScreen) return "grindstone";
        if (screen instanceof StonecutterScreen) return "stonecutter";
        if (screen instanceof LoomScreen) return "loom";
        if (screen instanceof SmithingScreen) return "smithing_table";
        if (screen instanceof CartographyTableScreen) return "cartography_table";
        if (screen instanceof CrafterScreen) return "crafter";
        if (screen instanceof HopperScreen) return "hopper";
        if (screen instanceof HorseInventoryScreen) return "horse";
        if (screen instanceof MerchantScreen) return "villager";
        if (screen instanceof CreativeModeInventoryScreen) return "creative";
        if (screen instanceof InventoryScreen) return "inventory";
        return null;
    }
}
