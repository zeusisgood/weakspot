# 図 1　全体構成

[← 図の一覧に戻る](README.md)

パッケージ（`io.github.zeusisgood.weakspot` の下）どうしの依存です。矢印は「使う側 → 使われる側」で、各ファイルの `import` から調べました。

```mermaid
flowchart TB
    subgraph CLIENT_SIDE["クライアントだけで動く"]
        C["client/<br/>ClientWeakSpotHandler・AimSpots・ScreenSpots・FishingSpot<br/>OwnHits・ComboHud・HitSounds・WeakSpotRenderer<br/>TargetPlay・StatsScreen・ClientSettings"]
    end

    subgraph BOTH_SIDES["両側で動く"]
        R["ルート<br/>WeakSpotMod・CommonProxy<br/>RightClickTargets・AnimalTargets・VehicleTargets・MeleeTargets<br/>PlayerRules・HeldItems・FallDamage<br/>BowDraw・EatDraw・UseTimeCut・MeleeCharge・ThrowCharge・HeldCharge<br/>Reflect・GuideBook・ItemTarget"]
        N["network/<br/>16 個のメッセージ・ServerThread"]
        CFGMC["config/（依存あり）<br/>WeakSpotConfig・ServerConfig・ClientConfig<br/>SyncedSettings"]
    end

    subgraph SERVER_SIDE["論理サーバーで動く"]
        S["server/<br/>HitHandlers・HitGate・各 *Hits<br/>ServerBoostTracker・ServerStats・MiningRewards<br/>TargetRounds・Leaderboard・MarkerRelay・ComboRelay<br/>SettingsSync・ServerSwitches・PlayerText"]
    end

    subgraph PURE["Minecraft・Forge に依存しない"]
        CFGP["config/（純粋）<br/>ComboPosition・HitSound・SettingGroups・SoundPreset"]
        CM["common/<br/>HitKind・HitStreak・ComboFactor・ComboTier・ComboMilestones<br/>MiningStats・Milestones・BoostMath・TargetRules<br/>MarkerMotion・WeakSpotPlacer・KindMask など"]
    end

    C --> R
    C --> N
    C --> CFGMC
    C --> CFGP
    C --> CM
    S --> R
    S --> N
    S --> CFGMC
    S --> CM
    R --> N
    R --> CFGMC
    R --> CM
    N --> CFGMC
    N --> CM
    CFGMC --> CM
    CFGP --> CM

    R -.->|"文字列だけ（@SidedProxy・guiFactory）"| C
    N -.->|"クライアント行きは proxy.onXxx 経由"| R

    C ==>|"MachineStates・MoveHits"| S
    R ==>|"*Hits・ServerSwitches・PlayerText・TargetRounds"| S
    N ==>|"受け取りの処理"| S
    CFGMC ==>|"EnchantHits・PortalHits・SettingsSync"| S

    classDef pure fill:#4CAF50,stroke:#2E7D32,color:#FFFFFF
    classDef mc fill:#FF9800,stroke:#E65100,color:#000000
    classDef clientOnly fill:#2196F3,stroke:#0D47A1,color:#FFFFFF
    class CM,CFGP pure
    class R,N,CFGMC,S mc
    class C clientOnly
```

## 凡例

| 見た目 | 意味 |
|---|---|
| 緑 `#4CAF50` | Minecraft・Forge のクラスを使わない（`java.util` と `common/` だけ） |
| 橙 `#FF9800` | Minecraft・Forge のクラスを使う（両側、またはサーバー） |
| 青 `#2196F3` | Minecraft・Forge を使い、クライアントにだけ読み込まれる（`@EventBusSubscriber(value = Side.CLIENT)`） |
| 実線の矢印 | 下の層へ向かう、ふつうの依存 |
| 太い矢印 | `server/` へ入る依存（`server/` 以外のパッケージが、サーバーの処理を直接呼ぶ） |
| 点線の矢印 | クラスを直接は参照しない依存（文字列・プロキシ経由） |

## 注記

- `common/` は `common/` の中と `java.util` だけを使う（MC・Forge・ほかのパッケージへの依存なし）。
- `config/` は 2 つに分かれる: 設定の本体（`@Config` の `WeakSpotConfig` など、Forge に依存）と、値や表だけのクラス（`SoundPreset` は `common/HitScale` だけを使う）。
- `server/` は `client/` を使わない。`client/` から `server/` へは 3 か所（`ClientWeakSpotHandler`・`OwnSpotBars` → `MachineStates.hasBar`、`SprintSpot` → `MoveHits`）。
- 太い矢印の内訳:
  - ルート → `server/`: `RightClickTargets` → `RightClickHits`・`ServerSwitches`・`PlayerText`、`AnimalTargets` → `AnimalHits`・`ServerSwitches`、`MeleeCharge` → `MeleeHits`、`ItemTarget` → `TargetRounds`、`WeakSpotMod` → `MachineAccelerator`・`MachineStates`・`VehicleHits`・`WeakSpotCommand`
  - `network/` → `server/`: `HitMessage` → `HitHandlers`、`MarkerMessage` → `MarkerRelay`・`ServerSwitches`、`QueryMessage` → `QueryHandlers`、`StatsRequestMessage` → `ServerStats`、`SwitchMessage` → `ServerSwitches`、`TargetActionMessage` → `TargetRounds`
  - `config/` → `server/`: `SyncedSettings` → `EnchantHits`・`PortalHits`（読めないときに false で送るため）、`WeakSpotConfig` → `SettingsSync`
- `network/` のクライアント行きのメッセージは、`WeakSpotMod.proxy.onXxx`（`ClientProxy`）経由で `client/` のクラスに届く（専用サーバーでもハンドラーが作られるため）。
- テスト: `common/` の単体テスト、`compat/`（`WireCompatTest`・`HitHandlersTest`・`QueryMessagesTest`）、`config/`（`ConfigMigrationTest`・`SettingGroupsTest`・`SoundPresetTest`・`SyncedSettingsTest`）、`resources/`（`LangFilesTest`・`ReleaseFilesTest`）、ルートの `HeldItemsTest`。
