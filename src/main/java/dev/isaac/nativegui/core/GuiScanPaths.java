package dev.isaac.nativegui.core;

/**
 * ResourceManager.listResourceStacks requires directory roots with NO trailing slash.
 * Keep the scanned root separate from the file-prefix filter, which DOES end in '/'.
 * This class is Minecraft-independent to make the contract regression-testable.
 */
public final class GuiScanPaths {
    private GuiScanPaths() {}

    public static final String OPTIFINE_SCAN_ROOT = "optifine/gui/container";
    public static final String OPTIFINE_FILE_PREFIX = OPTIFINE_SCAN_ROOT + "/";
    public static final String OPTIGUI_SCAN_ROOT = "gui";
    public static final String OPTIGUI_FILE_PREFIX = OPTIGUI_SCAN_ROOT + "/";
    public static final String OPTIGUI3_SCAN_ROOT = "optigui/gui";
    public static final String OPTIGUI3_FILE_PREFIX = OPTIGUI3_SCAN_ROOT + "/";

    public static boolean isLegacyRule(String path) {
        return path.startsWith(OPTIFINE_FILE_PREFIX) && path.endsWith(".properties");
    }

    public static boolean isIniRule(String namespace, String path) {
        return "optigui".equals(namespace) && path.startsWith(OPTIGUI_FILE_PREFIX)
                && path.endsWith(".ini");
    }

    public static boolean isJsonRule(String path) {
        return path.startsWith(OPTIGUI3_FILE_PREFIX) && path.endsWith(".json");
    }

    static {
        for (String root : new String[] {OPTIFINE_SCAN_ROOT, OPTIGUI_SCAN_ROOT, OPTIGUI3_SCAN_ROOT}) {
            if (root.isBlank() || root.endsWith("/")) {
                throw new ExceptionInInitializerError("Invalid Minecraft resource scan root: " + root);
            }
        }
    }
}
