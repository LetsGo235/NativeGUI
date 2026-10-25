# Compatibility matrix — 1.0.0-rc.1

This is an initial interoperability implementation, **not a full OptiGUI port**.
Results marked "Observed" are user-reported in-game tests of identical Java
implementation in 0.3.0-beta.1, not a formal automated game test.

| Capability | Status | Evidence / limitation |
| --- | --- | --- |
| Native NeoForge 1.21.1 client | Observed | Launch and test reported; RC not independently built here |
| OptiFine `.properties` basic custom GUIs | Observed | Multiple standard inventories with Glass Edition |
| Renamed chest / name-specific texture | Observed | Treasure Chest and JSON named-chest test |
| Colored shulker boxes | Observed | Purple and red matched, negative control did not mismatch |
| Biome-selective GUI | Observed | Desert-specific chest test |
| Multiple resource packs / priority | Observed | Priority override test and stacked test packs |
| Resource reload (F3+T) | Observed | Repeated manual reloads |
| OptiGUI 3 JSON *subset* | Observed | JSON furnace, renamed-chest and biome tests |
| Historic OptiGUI 2 INI *subset* | Partial | Basic barrel test; unrecognized conditions skipped |
| Villager trades | Observed rendering | Tested with Glass Edition; specialized professions not proven |
| Horse / mule / llama inventories | Observed rendering | Tested with Glass Edition; variant-specific rules not proven |
| Donkey-specific UI | Untested | Similar family, but not assumed identical |
| Container conditions: biome, height, name, chest flags, color | Partial | Common properties implemented; not every combination verified |
| Advanced OptiGUI 3 nested conditions / arbitrary NBT | Unsupported | Fail-closed to avoid wrong GUI |
| Animated OptiFine GUI sequences | Unsupported | No special animation engine |
| Arbitrary GUI sprite-atlas replacement | Unsupported | Standard `GuiGraphics.blit` texture routing only |
| Modded menus with unusual rendering | Best-effort | No universal guarantees |
| Multiplayer | Observed client-only | Connected to an independent public server without server installation; tested compatible pack interfaces; not exhaustive |
| All OptiGUI packs / all versions | Not claimed | Test packs case by case |

A resource pack that states "requires OptiGUI" *may* work if its rules fall
inside these supported subsets. Unsupported rules are not guaranteed to work.

## Additional October 25, 2026 confirmation

The tester reported successful client-side use on a public multiplayer server
and successful rendering with additional third-party packs labeled
"OptiGUI Required" or "OptiGUI Recommended". Exact pack names and versions
were not recorded, so these do not yet constitute individually reproducible pack certifications.
