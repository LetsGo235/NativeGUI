# NativeGUI — publication runbook (October 25, 2026)

## Release eligibility warning: Modrinth's AI content rules

Modrinth adopted new rules in August 2026: projects principally authored through AI prompts and testing are **not eligible for public publication**. Disclosure alone does not make them eligible. As the current NativeGUI project was largely coded with generative AI assistance, do not publicly submit this build on Modrinth as-is. Significant genuine human authorship/code contribution would be needed, plus an accurate disclosure. See:
- https://support.modrinth.com/en/articles/16551575-disclosure-and-usage-of-ai
- https://modrinth.com/legal/rules

GitHub can be used now for a public source repository and a pre-release binary, subject to normal platform and license rules. For Modrinth eligibility, assess the human-authored contribution fairly or ask Modrinth's moderators. Never hide or misrepresent AI involvement.

## Recommended order

1. **Keep the name NativeGUI.** Its `nativegui` mod ID and Java packages already exist.
2. **Build** from source with JDK 21: `build-windows.bat` (Windows) or `bash build-linux.sh` (Linux). Do not post a source ZIP as a mod JAR.
3. **Check** `BUILD SUCCESSFUL`, file exists in `build/libs/`, run release gate with the final JAR, and ensure no other NativeGUI JAR is installed.
4. **Record hashes**: Windows PowerShell `Get-FileHash .\build\libs\nativegui-1.0.0-rc.1.jar -Algorithm SHA256` or macOS/Linux `shasum -a 256 build/libs/nativegui-1.0.0-rc.1.jar`.
5. **GitHub**: create public repo `NativeGUI` without initializing README/license because this source contains them; upload **contents** of this folder including hidden `.github` templates. Or push with Git CLI after making a repository.
6. **Issues**: GitHub repo `Settings` > `General` > `Features` > enable Issues. Template forms will appear because they are under `.github/ISSUE_TEMPLATE/` on the default branch.
7. **CI**: open Actions; confirm the Build NativeGUI workflow passed and downloadable artifact appears. CI is a useful independent build check.
8. **GitHub Release**: use Releases > Draft a new release, tag `v1.0.0-rc.1`, title `NativeGUI 1.0.0-rc.1`, mark as **pre-release**, paste `RELEASE_NOTES_RC1.md`, attach the **installable JAR**. Optionally include checksum in notes. Only upload the JAR after a confirmed build.
9. **Media**: capture genuine in-game screenshots of old/new inventory screens. Draw any logo yourself, or use your own non-AI digital art. Don't use third-party pack art as if it's yours.
10. **Modrinth (only after eligibility resolved)**: Create a Mod project; title `NativeGUI`; summary should explain functionality. Loader `NeoForge`, game version `1.21.1`, environment `Client-side`, license `MIT`, first version `1.0.0-rc.1`, version channel **Beta**, dependencies none beyond Minecraft/NeoForge, link GitHub source + issues. Upload the JAR, not the source ZIP or diagnostic resource pack. Disclose AI involvement truthfully. Read `MODRINTH_DRAFT.md` and `COMPATIBILITY.md`.
11. **After launch**: collect issues, label bugs/compatibility/enhancements, ship patch releases with changelogs, link Modrinth and GitHub when eligible.

## Important notes

- A test on a random online server supports client-only function; it does not establish compatibility with all server software.
- Leave OptiGUI/OptiFine trademarks in *interoperability explanations*, not in a misleading name or logo.
- Minecraft's usage guidelines recommend conspicuous notice: `NOT AN OFFICIAL MINECRAFT MOD. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.`
