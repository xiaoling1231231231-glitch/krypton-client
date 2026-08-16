# Krypton Client

A macOS Minecraft client (launcher + in-game Fabric mod) in the style of Lunar/Feather — called **Krypton Client**.

## What's included

**Launcher** (`launcher/`) — a Node.js app with a Krypton-themed web UI:
- Browses every Minecraft release and snapshot from Mojang's manifest (906 versions indexed)
- Installs any version, with **Fabric** (Fabric Loader for 1.14+, Legacy Fabric for 1.8.9)
- Auto-downloads the client jar, libraries, assets, Fabric loader, Fabric API and the Krypton mod
- Launches the game with correct classpath/natives/JVM args (`-XstartOnFirstThread` for macOS)
- Per-version mods folder so each Minecraft version gets its own mods
- Offline accounts (Microsoft auth is a stub seam)

**Mod** (`mod/`) — a Fabric mod (`krypton 1.0.0`) that IS the in-game client:
- Module system + ClickGUI (toggle with **Right Shift**, keybinds per module)
- HUD with FPS, coordinates, session info and draggable keystrokes (edit mode with **Right Control**)
- Combat modules: AutoClicker, AimAssist, TriggerBot, Reach, HitBox
- Movement: AutoSprint, NoFall
- Render: FullBright, NoHurtCam, ViewModel
- Misc: custom title screen + gradient, screenshot naming
- Config saved to `config/krypton/config.json`

## Custom title screen (gradient flags)

The `CustomTitle` module (Misc) replaces the Minecraft logo on the title screen with your own text, colored with a gradient palette. In the ClickGUI:
1. Enable **CustomTitle**
2. Click **Title** to type your text (e.g. `KRYPTON CLIENT`)
3. Cycle **Colors** through: `Default` (minecraft gray), `Trans`, `Gay`, `Pansexual`, `Lesbian`, `Bisexual`, `Nonbinary`, `Asexual`, `Aromantic`, `Genderfluid`

## Requirements

- macOS
- Node.js >= 18
- A JDK (auto-detected via `JAVA_HOME`/`/usr/libexec/java_home`; 1.8.9 needs Java 8-ish runtimes, modern versions need Java 21+)
- Gradle 9.5 (cached) to build the mod

## Build the mod

```sh
cd mod
gradle build
# -> build/libs/krypton-client-1.0.0.jar
```

The launcher auto-installs this jar into the game's mods folder when you install a version.

## Run the launcher

```sh
cd launcher
npm start        # (no deps needed — plain Node http server)
# open http://localhost:5757
```

In the UI:
1. **Versions** tab → pick a version (Fabric on by default)
2. **Play** tab → add an offline username, then **Install & Play**

First launch downloads everything; subsequent launches reuse the install. Game directory:
`~/Library/Application Support/KryptonClient/minecraft`

## krypton — Modrinth mods in your terminal

A lightweight Modrinth client that downloads mods straight into your game's mods folder
(same version-scoped layout the launcher uses), so they're picked up on next launch.

```sh
cd cli
./install.sh                # symlinks `krypton` into /usr/local/bin (run once)

krypton search sodium       # search Modrinth mods
krypton info sodium         # show mod details
krypton install sodium      # install latest fabric version for your game version
krypton install sodium --mc 1.21.11 --loader fabric
krypton remove sodium       # uninstall
krypton list                # show installed mods
```

The CLI auto-detects your game version from the installed versions folder
(override with `--mc`). Installed mods go to
`~/Library/Application Support/KryptonClient/minecraft/mods/<version>/` and are
staged into the active mods folder when you launch.

## In-game controls

| Key | Action |
|-----|--------|
| Right Shift | Open/close ClickGUI |
| Right Control | Toggle HUD edit mode (drag boxes) |
| Y | Open ClickGUI (legacy binding) |

## Project layout

```
krypton-client/
  launcher/
    server.js            # web server + install/launch API
    lib/                 # manifest, installer, auth, launch, paths
    public/              # the Krypton web UI
  cli/
    krypton              # Modrinth mod manager CLI (search/install/remove)
    lib/modrinth.js      # Modrinth API client
    install.sh           # symlink `krypton` into /usr/local/bin
  mod/
    build.gradle         # Fabric Loom build (targets 1.21.11)
    src/main/java/...    # module system, ClickGUI, HUD, modules, mixins
    src/main/resources/  # fabric.mod.json, krypton.mixins.json
```

## Notes & roadmap

- The mod is currently compiled for **1.21.11** (Yarn mappings). To target another version, change `gradle.properties` and rebuild — the launcher already supports installing any version.
- Microsoft account auth, more combat modules (velocity, reach smoothing), ESP/nametag rendering and a command system are natural next steps.