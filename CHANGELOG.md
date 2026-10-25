# Changelog

## 1.0.0-rc.1 — source-frozen release candidate (October 25, 2026)

**No Java logic changes relative to 0.3.0-beta.1.** Only the version, docs,
QA/release metadata, and CI pack-validation step were changed.

Tester-confirmed in-game successes from 0.3.0-beta.1:
- Purple/red shulker color rules with distinct skins and correct nonmatches.
- OptiGUI 3 JSON screen-title matching for renamed chests; a shulker renamed
  identically does not erroneously receive the chest skin.
- Colorful Containers – Glass Edition renders with tested vanilla menus,
  villager GUI and tested horse/llama/mule inventories.
- Repeated F3+T resource reloads and multi-pack priority checks passed.

RC publication remains conditional on local `BUILD SUCCESSFUL`, clean minimal
NeoForge startup, and multiplayer client test. These have not been independently
confirmed for rc.1 in the packaging environment.

## 0.3.0-beta.1

- Support for OptiGUI 3 `@screen/@title` conditions (subset of matchers).
- Improved entity interaction context and opt-in screen diagnostics.
- Improved runtime resilience and added release-gate diagnostic resource pack.
- Added conditional shulker and JSON regression tests.

## 0.2.0-alpha.1

- Initial OptiGUI 3 JSON subset, broader vanilla GUIs, pack priority,
  and reload tests.

## 0.1.0-alpha.2

- Fixed startup-blocking trailing-slash bug in resource-pack scanning.
- Isolated malformed pack/rule scanner failures.
