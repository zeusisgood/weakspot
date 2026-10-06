# 図 2　ヒットの流れ

[← 図の一覧に戻る](README.md)

- [2a　ヒット 1 回の流れ](#2a-ヒット-1-回の流れ)（すべての種類に共通）
- [2b　ヒットとは別の時刻に動く配信](#2b-ヒットとは別の時刻に動く配信)（頭の上のコンボ・弱点のマーク）
- [種類で違う箇所](#種類で違う箇所)

## 2a ヒット 1 回の流れ

```mermaid
sequenceDiagram
    autonumber
    actor P as プレイヤー
    participant AIM as 照準の部品<br/>ClientWeakSpotHandler・AimSpots<br/>ScreenSpots・FishingSpot
    participant OWN as OwnHits
    participant TH as HitMessage<br/>ServerThread
    participant HH as HitHandlers
    participant KH as 種類の処理<br/>各 *Hits
    participant HG as HitGate
    participant ST as ServerStats
    participant MR as MiningRewards<br/>WeakSpotAdvancements
    participant ME as 自分のクライアント<br/>受け取り
    participant OC as ほかのクライアント

    P->>AIM: 照準を弱点に重ねる
    Note over AIM: 毎フレーム（RenderWorldLastEvent）<br/>当たり判定と OwnHits.canHit（種類ごとの間隔）
    AIM->>OWN: register(kind)
    Note over OWN: STREAK.hit（コンボ +1）<br/>HitSounds.playHit・ComboHud.onHit<br/>間隔の記録・金の粒
    OWN-->>AIM: ヒット後のコンボ数 streak
    AIM->>TH: HitMessage（kind・pos・streak・entityId）
    Note over AIM: サーバーの返事を待たずに<br/>弱点を移す（relocate）か消す<br/>自分の側の効果もここで始める
    TH->>HH: サーバーのスレッドで onHit
    HH->>KH: 種類の表から処理を選ぶ（登録がなければ無視）
    KH->>HG: allowed（サーバーの設定・プレイヤーのオン・オフ・PlayerRules）
    KH->>KH: 種類ごとの条件と間隔（ready または intervalOk。設定の間隔 − 2 tick）
    alt 受け付けない
        Note over KH: 何もしない。クライアントに返事は送らない<br/>（自分の音・コンボの表示はそのまま）
    else 受け付ける
        KH->>HG: accept（player・kind・音の位置・streak）
        HG->>HG: mark（次の間隔の起点）
        opt 採掘以外
            HG->>ST: recordKindHit（今回と累計に +1）
            ST->>MR: onKindHit（種類ごとの節目・合計の節目）
            opt 節目に届いた
                MR->>ME: MilestoneMessage（KIND・TOTAL）と経験値
                MR->>OC: 知らせのチャット（PlayerText）
            end
        end
        HG->>ST: countStreak
        Note over ST: サーバー側の HitStreak.hit<br/>累計の maxStreak を更新
        opt 累計の最大を超えて、段階の数に届いた
            ST->>OC: broadcastCombo（チャット）
        end
        ST->>MR: WeakSpotAdvancements.onHit（初めての弱点・コンボ 100・300・すべての弱点）
        HG->>OC: OtherHitMessage（音の位置から 16 ブロック以内）
        Note over OC: HitSounds でほかの人の音を鳴らす<br/>音の高さはクライアントが送った streak
        HG-->>KH: サーバーのコンボ数
        KH->>KH: 効果（掛け数は ComboFactor.factor(コンボ数)）
    end
```

### 注記

- コンボは**両側で別々に数える**。クライアントは `OwnHits.STREAK`（音・表示）、サーバーは `ServerStats` の `HitStreak`（効果の掛け数・統計・知らせ・進捗）。`HitMessage` の `streak` は、ほかの人の音の高さにだけ使う。
- サーバーがヒットを受け付けなかったときも、クライアントの音・コンボの表示・弱点の移動は戻らない（返事のパケットがない）。
- `accept` の中の順序は「`mark` → 種類ごとの数と節目 → コンボ → 進捗 → `OtherHitMessage`」。効果はそのあと。
- 節目の知らせのチャットは、本人以外の全員に送る。`MilestoneMessage` は本人だけに送る（`MilestoneEffects.show`）。
- クライアント行きのメッセージは、`WeakSpotMod.proxy`（`ClientProxy`）経由でクライアントのスレッドに渡す。

## 2b ヒットとは別の時刻に動く配信

```mermaid
sequenceDiagram
    participant OM as 自分のクライアント<br/>OtherMarkers
    participant MM as MarkerMessage<br/>ServerThread
    participant RL as MarkerRelay
    participant CR as ComboRelay
    participant ST as ServerStats
    participant OC as ほかのクライアント<br/>OtherMarkers・OtherCombos

    rect rgb(235, 245, 255)
        Note over OM,OC: 弱点のマーク（採掘・成長・収穫・機械・動物）
        loop クライアントの tick ごと（END）
            OM->>OM: ClientWeakSpotHandler.spot から今のマークを作る<br/>（掘る前の弱点は送らない）
            OM->>MM: 変わったとき（markerSendMinIntervalTicks で間引く）と<br/>出ている間は 20 tick ごと。消えたら null
        end
        MM->>RL: onMarker（オフのプレイヤーのものは null に）
        loop サーバーの tick ごと（END）
            RL->>RL: 60 tick 届かない・切断したマークを消す
            RL->>OC: markerShareRange 以内に入った人と、変わったマーク → OtherMarkerMessage
            RL->>OC: 範囲から出た人 → OtherMarkerMessage（null）
        end
        Note over OC: 60 tick 更新がない・対象がなくなったマークは消す
    end

    rect rgb(255, 245, 230)
        Note over CR,OC: 頭の上のコンボ
        loop サーバーの tick ごと（END）
            CR->>ST: streak(player).count(今)（オフのプレイヤーは 0）
            CR->>OC: 数が変わったら OtherComboMessage（32 ブロック以内・1 人 4 tick に 1 回まで）
        end
        Note over OC: OtherCombos が 10 以上なら名前の上に「n HIT」。0 で消す
    end
```

### 注記

- マークは、サーバーが位置を確かめずにそのまま転送する（見えるだけで、ヒットや破壊には関係しない）。
- `markerShareRange` が 0 以下なら、クライアントは送らない。
- 頭の上のコンボは、ヒットの瞬間ではなくサーバーの tick の終わりにまとめて送る。途切れた（数が 0 になった）ことも同じ経路で届く。
- 的当ての頭の上の表示は `OtherTargetMessage` で、`TargetRounds` が直接送る（[図 3c](03-states.md#3c-的当てのラウンド)）。

## 種類で違う箇所

図 2a の共通の流れから外れる所です。リファクタのあとも、ここが同じかを確かめます。

| 種類 | 違い |
|---|---|
| 採掘 | 掘っていないとき（`Mining` がない・位置が違う・`isDestroyingBlock` が false）のヒットは、`ServerBoostTracker.RESERVED` に予約する（プレイヤーごとに 1 つ。`accept`・統計は呼ばない）。同じブロックを `PRE_DIG_TICKS`（20 tick）のうちに掘り始めたら（`LeftClickBlock`）、`apply` で確定する。クライアントは、掘る前に当てられるのは 1 回だけ（`MiningBoost.hasPreHit`）で、左クリックを押している間だけ当たる |
| 採掘 | `accept` は種類ごとの数を数えない。代わりに `accept` のあとで `ServerStats.record(recordHit)` と `MiningRewards.onMiningHit`（採掘の節目・合計の節目。経験値と道具の耐久回復）。耐久回復は `BreakEvent`（LOWEST）で精算する（`onBlockBroken`）。効果は `Mining.extraTicks` に貯めて `BreakSpeed` に掛ける（`BoostMath`） |
| 採掘 | クリエイティブのヒットは受け付けない |
| 成長・機械・収穫 | `RightClickHits`: 直前 10 tick にそのブロックを右クリックした記録・届く距離・`classify` が同じ種類か、を確かめる。間隔の起点は効果の成否の前に記録する（収穫が失敗しても間隔を待つ。クライアントが先に同じ間隔を守って送るので実害なし） |
| 収穫 | 効果（`HarvestHits.harvest`、コンボは `peekStreak` で先に見る）が成功してから `accept` |
| エンチャント | 効果（乱数の引き直し）が成功してから `accept` |
| 機械 | 効果は `MachineAccelerator.hit`（`accept` が返したコンボ数で倍率を決める） |
| 成長 | 効果のあと `GrowthWarnings`（自動の対象では呼ばない） |
| 動物・機械・釣り | ヒットの前に `QueryMessage` / `StateMessage` で状態を問い合わせ、返事が来るまでクライアントは弱点を出さない（動物・釣り）。機械はバーのためだけ。ヒットのときは即座に問い合わせ直す |
| はしご・エリトラ・走り・泳ぎ・落下 | `MoveHits` が、直前 10 tick のどこかでその状態だったかで確かめる |
| 動物・近接 | `HitMessage.entity` で相手のエンティティ ID を送る。音の位置は相手 |
| 釣り・弓・食事など | 位置を使わない（`BlockPos.ORIGIN`）。音の位置は浮き・プレイヤーなど種類ごと |
| 照準のまわり（`AimSpots`） | 当てたあと `keepAfterHit` なら移す、でなければ消す（弓は引き切って過剰チャージも上限なら消す） |
| 画面のマーカー（睡眠・エンチャント） | `ScreenSpots` から送る。睡眠はカーソルを重ねるだけ、エンチャントは左クリック（当てたクリックはキャンセル） |
| 的当て | `HitMessage` を使わない。`TargetActionMessage`（HIT / DECOY）→ `TargetRounds.onAction`。`accept`・コンボ・統計の種類別の数・節目は通らない。`OtherHitMessage` だけ同じ（✕ なら `miss`、streak はラウンドの中の数） |
