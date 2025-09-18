# NeoForge 1.21.1 Migration Notes

## Tooling & environment
- Minecraft 1.21.1 ships with Java 21; devShell now provisions Temurin 21 and exports it via `JAVA_HOME`.
- Replace ForgeGradle (6.x) with NeoForge's Gradle plugin (`net.neoforged.moddev` 2.0.14-beta) running on Gradle 8.8.
- NeoForge encourages the `moddev` plugin to manage runs and mappings; verify latest setup docs.
- Current mapping plan: official 1.21 + parchment `2024.11.10` for method names.
- NeoForge uses `META-INF/neoforge.mods.toml`; dependency syntax may differ (`cloth_config` ID check required).

## Required libraries
- **Cloth Config / AutoConfig**: confirm an updated Cloth Config build targeting 1.21.x. If unavailable, consider alternative config UI or conditional compilation.
- **Mixin**: still needed, but confirm compatibility with NeoForge's bundled Mixin version (likely 0.8.7+). Adjust `annotationProcessor` dependency accordingly.

## Networking snapshot (Forge 1.18.2)
- Simple channel defined in `NetworkHandler` with version `2.16.4`.
  - S2C `ChestCavityUpdatePacket`: syncs chest cavity open state and organ score map to the local player (sent via `NetworkUtil.SendS2CChestCavityUpdatePacket` and from `MixinLivingEntity` on clone events).
  - S2C `OrganDataPacket`: pushed during login handshake (`MixinHanshake.sendServerPackets` intercepts `NetworkHooks.sendMCRegistryPackets`) to replicate server-side organ definitions.
  - C2S `ChestCavityHotkeyPacket`: triggered by client keybindings to execute organ abilities (`OrganActivationListeners.activate`).
- Payloads rely on `FriendlyByteBuf` with ad-hoc serialization; handshake uses `PacketDistributor.NMLIST`/`Connection` objects.

### Networking migration considerations
- NeoForge 1.21.1 retires `SimpleChannel` in favor of `CustomPacketPayload` + `StreamCodec` registered during `RegisterPayloadHandlersEvent` (`PayloadRegistrar`). Clientbound handlers register via `RegisterClientPayloadHandlersEvent`.
- Choose appropriate registration phase: configuration (login/handshake) for organ catalog sync, play phase for regular updates/hotkeys. Use `registrar.configurationToClient` for login payloads and `registrar.playToClient`/`playToServer` for runtime updates.
- Ensure payload handlers specify the desired `HandlerThread` (main vs network) when registering.
- Replace `PacketDistributor` usages with context-aware send helpers (`IPayloadContext#reply` or direct `ServerPlayer#connection.send` wrappers).
- Handshake mixin into `NetworkHooks.sendMCRegistryPackets` should be replaced with proper registration of configuration payloads; investigate if we can enqueue organ sync during `RegisterPayloadHandlersEvent` or player login events without mixins.
- Update serialization to `StreamCodec` composites mirroring current packet structure (organ IDs, floats, etc.).

## NeoForge API changes to plan for
- Package rename: most `net.minecraftforge.*` imports become `net.neoforged.*` with API shifts (event bus, registries, networking, logical sides, etc.).
- Deferred registers move to `net.neoforged.neoforge.registries`; check updated helper methods.
- Event bus registration uses `ModLoadingContext`/`IEventBus` from NeoForge; lifecycle events renamed (`FMLClientSetupEvent` still present but ensure usage matches new functional style).
- Client screen registration uses `MenuScreens.register` inside the client setup event and may require enqueuing via `event.enqueueWork`.

## Gameplay systems impacted
- Mixins targeting `net.minecraft` classes must be reviewed for signature changes between 1.18.2 and 1.21.1 (entities, hunger manager, status effects, etc.). Expect major rewrites under new Mojang names.
- Inventory/menu classes changed post-1.20 (data components, menu constructors). `ChestCavityScreenHandler` will need adjustments.
- `Item`/`FoodProperties` builder API evolved; verify organ items and custom food components still align.
- Status effects and attributes underwent registry/data changes; confirm custom mob effects still register correctly.
- Data pack format bumped (1.21). Validate JSON schemas for loot tables, tags, and recipes.

## Configuration pathway
- AutoConfig should continue working but confirm initialization order under NeoForge. If Cloth Config lacks 1.21.x support, plan fallback configuration handling.

## Optional integrations
- Optional dependencies commented in `build.gradle` must be revisited: ensure updated mod IDs/versions exist for NeoForge 1.21.1 or mark as unsupported.

## Current compile blockers
- Numerous Forge imports no longer resolve (e.g., `net.minecraftforge.*`, `RegistryObject`, event bus classes); need systematic migration to `net.neoforged.*` packages.
- Creative tab creation now uses the data-driven tab system; replace `CreativeModeTab` fields and `.tab(...)` item properties with NeoForge creative tab registration + content callbacks.
- Text API updates remove `TextComponent`; migrate to `Component.literal` / `Component.translatable`.
- Brewing/damage APIs renamed (`IndirectEntityDamageSource`, `PotionUtils`); review 1.21 replacements.
- Networking scaffolding still references deprecated Forge `SimpleChannel`; rewrite to NeoForge payload system as planned above.

## Outstanding research questions
- Confirm availability/versions of Cloth Config + AutoConfig for NeoForge 1.21.1.
- Are there upstream ports (Fabric/NeoForge) of Chest Cavity that already target newer Minecraft? Review for API insights.
- Determine replacement for `NetworkHooks.sendMCRegistryPackets` mixin (maybe a configuration payload triggered during login events).
- Identify updated method names for key mixin targets (`LivingEntity`, `HungerManager`, etc.) to plan reimplementation.

Keep this document updated as you discover concrete API changes or confirm library availability.
