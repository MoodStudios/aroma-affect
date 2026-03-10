# ReplayCompat — Flashback Mod Compatibility Layer

## The Problem

AromaAffect is incompatible with [Flashback](https://github.com/moulberry/flashback) because during replay playback and scene editing:

1. **Entity IDs don't match** — Flashback replays recorded network packets, but entity IDs in the replay world differ from the original. Code that looks up entities by ID (e.g., opening a sniffer inventory) crashes with `IllegalStateException`.
2. **Inventory/menu operations fail** — Opening menus, syncing items, and container interactions require valid entities and real player connections that don't exist during replay.
3. **Packet sending fails** — Server-to-client packets sent during replay target fake replay players, causing errors or disconnects.
4. **Tick handlers cause side effects** — Server/client tick handlers run during replay and trigger unwanted logic (ability processing, scent triggers, WebSocket connections, etc.).

## The Solution

`ReplayCompat.java` uses **pure reflection** (no compile-time dependency) to detect Flashback and provides two guards:

- `ReplayCompat.isInReplay()` — Client-side: returns true during replay playback
- `ReplayCompat.isReplayServer(server)` — Server-side: returns true when server is a Flashback ReplayServer

**File:** `common/src/main/java/com/ovrtechnology/compat/ReplayCompat.java`

### Reflected Flashback Classes

| Class | Purpose |
|-------|---------|
| `com.moulberry.flashback.Flashback` | Presence check + `isInReplay()` static method |
| `com.moulberry.flashback.playback.ReplayServer` | `instanceof` check for server type |

## Guard Patterns

### Pattern 1: Server tick handler
```java
TickEvent.SERVER_POST.register(server -> {
    if (ReplayCompat.isReplayServer(server)) return;
    // ... normal logic
});
```

### Pattern 2: Client tick handler
```java
ClientTickEvent.CLIENT_POST.register(client -> {
    if (ReplayCompat.isInReplay()) return;
    // ... normal logic
});
```

### Pattern 3: Player join handler
```java
PlayerEvent.PLAYER_JOIN.register(player -> {
    if (ReplayCompat.isReplayServer(player.level().getServer())) return;
    // ... normal logic
});
```

### Pattern 4: Server-side method that sends packets
```java
public static void openSnifferMenu(ServerPlayer player, Sniffer sniffer) {
    if (ReplayCompat.isReplayServer(player.level().getServer())) return;
    // ... send packet
}
```

### Pattern 5: Client-side packet factory (for already-recorded packets)
When Flashback replays a recorded packet, the server guard won't help because the packet was already recorded. The client factory must handle missing entities gracefully:
```java
// WRONG — crashes during replay:
if (sniffer == null) {
    throw new IllegalStateException("Sniffer not found with id: " + snifferId);
}

// CORRECT — logs warning and returns null:
if (sniffer == null) {
    LOGGER.warn("Sniffer not found with id: {} (likely in replay mode)", snifferId);
    return null;
}
```

## Key Insight: Two Layers of Defense

The incompatibility has two distinct scenarios:

1. **Live interactions during replay** — The ReplayServer re-executes game logic. Fix: server-side guards (`isReplayServer`) prevent packets from being generated.

2. **Pre-recorded packets being replayed** — Flashback replays raw packets that were captured during the original session. Fix: client-side handlers must gracefully handle missing entities/invalid state instead of throwing exceptions. A thrown exception causes Fabric's networking layer to disconnect with "Network Protocol Error".

**Both layers are needed.** The server guard prevents new problems; the client guard handles problems already baked into the recording.

## All Usage Sites

### Client-Side (`isInReplay()`) — 11 call sites

| File | Context |
|------|---------|
| `AromaAffectClient.java` | Skips initial prefs sync |
| `websocket/OvrWebSocketClient.java` | Skips WebSocket queue + auto-connect |
| `menu/TrackingHud.java` | Skips tracking HUD tick |
| `guide/AromaGuideTracker.java` | Skips guide tracking |
| `trigger/ScentTriggerHandler.java` | Skips scent trigger processing |
| `tutorial/waypoint/client/TutorialWaypointRenderer.java` | Skips waypoint render |
| `tutorial/animation/client/TutorialAnimationRenderer.java` | Skips animation render |
| `tutorial/dream/client/TutorialDreamOverlayClient.java` | Skips dream overlay |
| `tutorial/portal/client/TutorialPortalOverlayClient.java` | Skips portal overlay |
| `tutorial/chest/client/TutorialChestRenderer.java` | Skips chest renderer |

### Server-Side (`isReplayServer()`) — 20+ call sites

| File | Context |
|------|---------|
| `ability/AbilityHandler.java` | Skips ability tick |
| `command/path/ActivePathManager.java` | Skips path tick |
| `entity/sniffer/SnifferSyncHandler.java` | Skips sniffer sync on join |
| `entity/sniffer/SnifferMenuRegistry.java` | Skips sending open-menu packet |
| `network/NoseRenderNetworking.java` | Skips nose render sync on join |
| `guide/AromaGuideFirstJoinHandler.java` | Skips guide first-join |
| `lookup/LookupManager.java` | Skips lookup tick |
| `lookup/worker/StructureSearchWorkerManager.java` | Skips structure search |
| `trigger/StructureSyncHandler.java` | Skips structure sync tick |
| `tutorial/*` (all handlers) | Skips all tutorial logic |

### Client-Side Graceful Handling (null returns)

| File | Context |
|------|---------|
| `entity/sniffer/SnifferMenuRegistry.java` | Returns null instead of throwing when sniffer entity not found |

## How to Apply in Another Branch

1. **Copy** `ReplayCompat.java` to `com.ovrtechnology.compat`
2. **Add server guards** to every tick handler, join handler, and method that sends packets:
   - Server ticks → `if (ReplayCompat.isReplayServer(server)) return;`
   - Client ticks → `if (ReplayCompat.isInReplay()) return;`
   - Player join → `if (ReplayCompat.isReplayServer(player.level().getServer())) return;`
   - Packet-sending methods → `if (ReplayCompat.isReplayServer(...)) return;`
3. **Add client-side graceful handling** for any packet receiver/factory that looks up entities by ID — return null or skip instead of throwing
4. No gradle dependency changes needed — it's all reflection-based
