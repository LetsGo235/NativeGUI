# RC1 prepublication release gate — Minecraft 1.21.1 / NeoForge

**Candidate:** `nativegui-1.0.0-rc.1.jar` built from this source.
**Snapshot:** October 25, 2026. Manual verification was performed against
`0.3.0-beta.1` with the same Java sources, not against a packaged RC1 JAR.

## Confirmed by the tester on earlier beta releases

- [x] Regular and renamed chest, anvil, player inventory, barrel and basic GUIs.
- [x] OptiGUI 3 JSON furnace and renamed chest; renamed shulker not a false hit.
- [x] Purple and red shulker color rules, each with correct textures.
- [x] Desert-only chest condition and resource-pack priority overrides.
- [x] Glass Edition custom GUI pack across tested vanilla block screens.
- [x] Villager inventory/trading; llama, mule, horse inventories render Glass.
- [x] Multiple reloads with F3+T; stacked packs respect priority.

## Further checks recommended before wide distribution

- [ ] JDK 21 `build-windows.bat` completes with `BUILD SUCCESSFUL` (runs
      the legacy and Gson-backed JSON test suites).
- [ ] Verify final rc.1 JAR file exists and only ONE NativeGUI JAR is installed.
- [ ] Launch fresh NeoForge 1.21.1 profile with **NativeGUI only** and the test
      pack. Verify startup/menu/world/new world and no NativeGUI errors.
- [x] Joined an independent public server as a NativeGUI client; resource pack
      GUIs displayed correctly. Specific named-chest / reconnect scenarios were
      not fully verified on that server.
- [ ] Confirm at least one F3+T reload on this final RC1 JAR and rule ordering.
- [ ] Check no client crashes, reload loops, or excessive NativeGUI log noise.
- [ ] Confirm full disclosure of *partial* INI/JSON and unsupported NBT,
      animations and atlas sprites in published page.

## Optional but worthwhile

- [ ] Verify one actual donkey inventory.
- [ ] Test additional independent OptiGUI-dependent resource packs and unusual
      modded containers.
- [ ] Check vanilla player-inventory fallback when no matching rules exist.

For debug: `-Dnativegui.debug=true` prints screen context and selected
rules to `latest.log`. Keep debug OFF for publication.

**If any mandatory test fails, publish as beta instead of claiming RC stability.**

## Hosting-platform policy

Before publishing publicly to Modrinth, review the August 2026 generative AI
content restrictions: AI-prompted/tested projects without significant genuine
human authored content are not eligible even with disclosure. See `PUBLISHING.md`.
