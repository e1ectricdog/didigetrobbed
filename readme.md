# DidIGetRobbed

A Minecraft Fabric mod that helps you detect if items are missing from your chests, barrels, and shulker boxes.

## Features

- **Missing Item Detection**: Alerts you when items are missing from previously opened containers
- **Visual Overlay**: Shows ghost items in slots where items are missing
- **Chat Notifications**: Displays a detailed list of missing items in chat with enchantment support
- **Multi-World Support**: Works in multiplayer and singleplayer (because feeling protected on singleplayer is important!)

## Data Storage

The mod stores chest data in JSON files:

### Singleplayer
```
<world_folder>/didigetrobbed/chests.json
```

### Multiplayer
```
.minecraft/didigetrobbed/multiplayer/<server_hash>/<world_name>.json
```

## Known Issues / Limitations
- Double chests are tracked as a single container 