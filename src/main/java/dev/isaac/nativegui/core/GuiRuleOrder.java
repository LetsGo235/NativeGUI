package dev.isaac.nativegui.core;

import java.util.Comparator;

/** Deterministic cross-format rule order: top pack, numeric rank, then filename. */
public final class GuiRuleOrder {
    private GuiRuleOrder() {}
    public record Key(int packPriority, int rulePriority, String path) {}
    public static final Comparator<Key> COMPARATOR =
            Comparator.comparingInt(Key::packPriority).reversed()
                    .thenComparing(Comparator.comparingInt(Key::rulePriority).reversed())
                    .thenComparing(Key::path);
}
