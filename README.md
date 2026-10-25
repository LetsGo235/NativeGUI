# NativeGUI — Native Custom GUI Resource-Pack Support

**Minecraft 1.21.1 · NeoForge · client-side · 1.0.0-rc.1**

An independent NeoForge mod that displays custom inventory/container GUIs from
resource packs using supported **OptiFine Custom GUIs**, historical **OptiGUI 2 INI**,
and **OptiGUI 3 JSON** rules. No Fabric, Sinytra Connector, or OptiGUI required.

> **Release candidate, not a final release.** This source has not been compiled
> or launched in the packaging environment. The tester verified the same Java
> implementation in 0.3.0-beta.1, and the Java source is unchanged in RC1.
> RC1 still needs a successful Gradle build and a clean-profile check.

## Verified manually on v0.3.0-beta.1 (October 25, 2026)

- Colorful Containers – Glass Edition (an existing resource pack whose listing
  explicitly names OptiGUI) worked across the vanilla inventory and block GUIs
  the tester tried, plus villagers and horse, mule and llama inventories.
- The supplied diagnostic pack: red/purple shulker-specific GUI selection,
  JSON custom chest-title matching, and a nonmatching shulker with the same
  custom name (no false chest match).
- Previous tests of a desert-biome chest, JSON furnace screen, default/named
  chest, barrel, anvil, and player inventory passed.
- Multiple resource packs followed their expected priority order, and repeated
  F3+T reloads did not break visual replacements.

**Important distinction:** a GUI changing in the glass pack demonstrates that
its texture can render, not necessarily that all conditional entity/profession/
level/variant matching works. Donkey not specifically tested. Clean minimal
profile is still unverified. A subsequent multiplayer test on an independent
server and additional OptiGUI-required/recommended packs succeeded. Compatibility with one
third-party pack does NOT imply complete OptiGUI feature parity.

## Download / build

This ZIP contains **source**, not an installable JAR. To build:

1. Install **JDK 21**.
2. Run `build-windows.bat` on Windows or `bash build-linux.sh` on Linux.
3. Confirm output says **BUILD SUCCESSFUL**.
4. Copy `build/libs/nativegui-1.0.0-rc.1.jar` to `mods/` in a Minecraft 1.21.1
   NeoForge installation. **Remove all older NativeGUI JARs** first.
5. Enable an OptiGUI-dependent resource pack, optionally the bundled
   `release-gate-pack` (ZIP contents must be zipped into a resource pack).
6. Complete `RELEASE_GATE.md` before publishing a release.

The included GitHub Actions workflow runs Gradle verification and produces a
build artifact if you push the project to a GitHub repository and trigger it.

## Compatibility / limits

See `COMPATIBILITY.md` for supported, partially supported and unsupported
capabilities. Supported format parsing deliberately rejects unknown conditions
rather than applying incorrect textures. Intercepts standard `GuiGraphics.blit`
texture paths; does not guarantee third-party custom renderers or sprite-atlas
widgets will be affected. Client side only; installed on the client, not server.

For diagnostics only: add JVM argument `-Dnativegui.debug=true`, then look for
NativeGUI diagnostic logs when opening a screen. Off by default.

## License and independence

MIT licensed, wholly independent source and test textures. Does not bundle any
OptiGUI, OptiFine, Mojang, or third-party resource-pack code or art.
Not affiliated with those projects. Existing OptiGUI/OptiFine rule *formats* are
used as interoperability targets. Compatible with NeoForge 1.21.1 only.

Further details: `CHANGELOG.md`, `RELEASE_GATE.md`, `COMPATIBILITY.md`,
`MODRINTH_DRAFT.md`, `release-gate-pack/TESTING.txt`.

## Publication and attribution

See `PUBLISHING.md` before public publication. This implementation was substantially
developed with generative AI assistance and manually directed/tested by the project
owner. Follow each hosting platform's current rules and make appropriate
disclosures; as currently authored, a public Modrinth submission may not be
eligible under its August 2026 rules. GitHub source/release distribution can be
set up independently.

NOT AN OFFICIAL MINECRAFT MOD. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.
