# Contributing to NativeGUI

Please open an Issue before beginning a major feature, and include a clear reproduction case for compatibility bugs.

## Development
- Java 21, Minecraft 1.21.1, NeoForge 21.1.x.
- Build using `build-windows.bat` / `build-linux.sh` or Gradle 9.2.1.
- Run `python3 tools/validate_release_gate_pack.py` and `gradle --no-daemon build`.
- Include a test pack or minimal rule file for compatibility fixes.
- Avoid loading new unverified rules silently: fail closed when conditions are unsupported.
- Avoid breaking existing working resource packs.

## Pull requests
Include tested resource pack names, screenshots if useful, logs with secrets removed, and clear notes about new format support or changes.

Original OptiGUI, OptiFine, Minecraft, and third-party resource pack code or assets must not be copied into this repository. If you contribute third-party material, document its license/permission. Disclose meaningful AI assistance in your contribution.
