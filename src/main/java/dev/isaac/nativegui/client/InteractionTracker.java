package dev.isaac.nativegui.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.*;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * A short-lived immutable snapshot of the last real client interaction.
 * Do not retain Entity references, which could keep a disconnected world alive.
 */
public final class InteractionTracker {
    private InteractionTracker() {}

    public record EntitySnapshot(BlockPos pos, String id, String variant,
                                 String villagerProfession, Integer villagerLevel) {}

    private static volatile BlockPos lastClicked;
    private static volatile EntitySnapshot lastEntity;
    // Allow slower integrated servers to open the menu before its color is sampled.
    // Long-expired interactions must never control subsequent unrelated GUIs.
    private static final long MAX_AGE_NANOS = 5_000_000_000L;
    private static volatile long lastNanos;

    public static void rightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide()) return;
        lastClicked = event.getPos().immutable();
        lastEntity = null;
        lastNanos = System.nanoTime();
    }

    public static void entityClick(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) snapshotEntity(event.getTarget());
    }

    /** Some interactive mobs consume EntityInteractSpecific before EntityInteract fires. */
    public static void specificEntityClick(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getLevel().isClientSide()) snapshotEntity(event.getTarget());
    }

    private static void snapshotEntity(Entity entity) {
        String variant = null;
        if (entity instanceof Donkey) variant = "donkey";
        else if (entity instanceof Mule) variant = "mule";
        else if (entity instanceof Llama) variant = "llama";
        else if (entity instanceof Horse) variant = "horse";
        String profession = null;
        Integer level = null;
        if (entity instanceof Villager villager) {
            profession = BuiltInRegistries.VILLAGER_PROFESSION
                    .getKey(villager.getVillagerData().getProfession()).getPath();
            level = villager.getVillagerData().getLevel();
        }
        lastEntity = new EntitySnapshot(entity.blockPosition().immutable(),
                BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(),
                variant, profession, level);
        lastClicked = null;
        lastNanos = System.nanoTime();
    }

    public static BlockPos recentClick() { return fresh() ? lastClicked : null; }
    public static EntitySnapshot recentEntity() { return fresh() ? lastEntity : null; }

    private static boolean fresh() {
        boolean active = System.nanoTime() - lastNanos <= MAX_AGE_NANOS;
        if (!active) { lastEntity = null; lastClicked = null; }
        return active;
    }
}
