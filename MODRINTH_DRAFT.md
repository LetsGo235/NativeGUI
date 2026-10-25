# Modrinth listing draft — DO NOT PUBLICLY SUBMIT UNTIL ELIGIBLE UNDER AI POLICY

## Title
NativeGUI

## Short description
Use supported OptiFine and OptiGUI custom container GUI resource packs natively
on NeoForge 1.21.1 — without Fabric or Sinytra Connector.

## Extended description
NativeGUI is a lightweight, client-side compatibility mod that lets Minecraft
NeoForge 1.21.1 display custom container GUI textures supplied by existing
resource packs. It independently understands a subset of OptiFine custom-GUI
`.properties`, historical OptiGUI `.ini`, and OptiGUI 3 `.json` rule formats.

Features include support for common vanilla inventory screens, renamed
containers, selected biome/color conditions, pack precedence, and F3+T reloads.

**Limitations:** OptiGUI 2/3 feature coverage is partial. Advanced NBT matching,
custom GUI animations, and arbitrary sprite-atlas replacements are not yet
supported. Some OptiGUI-dependent packs can work, but compatibility with any
specific pack is not guaranteed. See `COMPATIBILITY.md` for the tested scope.

Requirements: Minecraft 1.21.1, NeoForge, Java 21. Install NativeGUI on the
client. No Fabric, OptiGUI, or Connector dependency.

Compatibility demonstrated in local testing with Colorful Containers – Glass
Edition and NativeGUI's own condition test packs. This mod is independent of
and not affiliated with OptiGUI or OptiFine.

Licensed under MIT. No assets or source from OptiGUI or third-party packs
are redistributed.

## Suggested version label
1.0.0-rc.1 (Beta channel)

## Eligibility and disclosure
**Current code was substantially authored using generative AI.** Modrinth's August 2026 policy prohibits projects primarily produced through AI prompting/testing; do not submit the present build for public listing. Meaningful genuine human authorship is required before publication there, not merely enabling a disclosure. If the project later qualifies, disclose AI assistance to the extent it was used, including any AI-generated listing text, and use original non-AI images only.

See `PUBLISHING.md` and `https://support.modrinth.com/en/articles/16551575-disclosure-and-usage-of-ai`.
