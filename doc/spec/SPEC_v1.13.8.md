# 弱点破壊 Mod 追加仕様書（v1.13.8）

`SPEC_v1.13.7.md`（Mod 1.13.7）に対する**パッチ**の仕様（外部の点検の 7 件への対応。採掘のヒットの座標の確かめ漏れの修正と、1.20.1・1.21.1 の共通化・テスト・統計の保存の整理）。ここに書かれていないことは、それと現行実装のままとする。
この仕様書は `doc/spec/SPEC_v1.13.8.md`。

- 対象: Minecraft 1.12.2 / Forge・1.20.1 / Forge・1.21.1 / NeoForge の 3 つ（決まりどおり同じ PR で）
- 前提: 1.13.7 が `main` に取り込み済み
- 経緯: 9481b06 の点検で 7 件の指摘があった。いったん 2 つの版（1.13.8・1.13.9）に分けたが、急ぐ理由が弱い（想定はシングルプレイか友人同士の小さなマルチ）ので、試すのが 1 回で済むよう 1 つの版にまとめた。原因を切り分けられるよう、コミットは節ごとに分ける
- 決めたこと:
  - 採掘のヒットと掘り始めも、ほかの入口と同じく、座標が読み込み済みか・届くかを確かめてからブロックを読む（§1）
  - 届く距離の余裕は、ほかの Mod で範囲を伸ばしても困らないよう、全部の入口（採掘・右クリック・機械の問い合わせ・動物）で **16 ブロック**に揃える（§1）
  - 1.20.1・1.21.1 で同じ中身のコードの置き場所は `mojmap/`（§2）
  - 点検で省いた 2 件（`network/Bytes` を `FriendlyByteBuf.readUtf` に置き換える＝通信内容が変わる、`MarkerRelay` が送らない分のメッセージまで作る＝量が小さい）は、今のまま
- 実装の順番（コミットの単位）: §1 → §2 → §3 → §4 → §5 → §6 → 版と文書

---

## 0. バージョンと互換性

- バージョンを 1.13.7 → **1.13.8**（パッチ）。版を書く 6 か所を揃える
- 通信内容・保存データ（NBT のキーと型）・設定の項目は変わらない。1.13.x 同士は接続できる（範囲 `"[1.13,1.14)"`・`PROTOCOL` `"1.13"` のまま）。1.13.7 までのワールドの統計は、そのまま読める
- 遊び方: 届く範囲を伸ばす Mod と一緒に使うと、遠くの弱点も受け付けるようになる（§1）。それ以外は変わらない

## 1. 座標と届く距離の確かめ方を揃える（3 つの版）

### 1.1 今

- 採掘のヒット（`server/ServerBoostTracker.onHit`）は、座標を確かめずにブロックを読む（1.20.1・1.21.1 は `getBlockState`、1.12.2 は `isAirBlock`）。サーバーの世界は、読み込まれていない座標を読むとチャンクを同期で読み込み、無ければ生成する。ヒットの間隔（`HitBudget`）はその後でしか効かないので、改造したクライアントが遠くの座標を送り続けると、チャンクの生成でサーバーが止まり、ワールドのファイルも膨らむ
- 採掘の掘り始め（`server/ServerBoostTracker.onLeftClickBlock`）も、イベントの座標を確かめずにブロックを読む（1.20.1・1.21.1 は `getBlockState`）。NeoForge 1.21.1 はこのイベントを、バニラの距離の確認（`canInteractWithBlock(pos, 1.0)`）より**前**に出す（仕様書のレビューで NeoForge の `ServerPlayerGameMode` のパッチを確かめた）。そのため、遠くの座標で掘り始めのバニラのパケットを送るだけでチャンクの読み込み・生成が走り、`HitBudget` もクリエイティブの確認も効かない。1.13.7 の `main` にもある穴で、この Mod の通信を使わずに突ける。1.20.1 の Forge も同じ順番と見ている（実装のときに Forge のパッチで確かめ、PR の本文に書く）。1.12.2 は、バニラ（`processPlayerDigging`）が距離を確かめてからイベントを出すので穴はない
- ほかの入口は確かめているが、届く距離の余裕が版と入口でばらばら:

| 入口 | 1.12.2 | 1.20.1 | 1.21.1 |
|---|---|---|---|
| 右クリック（`RightClickHits.onHit`） | `REACH_DISTANCE` + 3（足から中心） | `canReach(pos, 1.5)` | `canInteractWithBlock(pos, 1.0)` |
| 機械の問い合わせ（`MachineStates.onQuery`） | 読み込み + `REACH_DISTANCE` + 3 | 読み込み + `canReach(pos, 1.5)` | 読み込み + `canInteractWithBlock(pos, 1.0)` |
| 動物（`AnimalHits.onQuery` / `onHit`） | **6 ブロック固定**（属性を見ない） | `canReach(entity, 3.0)` | `canInteractWithEntity(entity, 1.0)` |
| 採掘のヒット（`ServerBoostTracker.onHit`） | なし | なし | なし |
| 採掘の掘り始め（`ServerBoostTracker.onLeftClickBlock`） | なし（バニラが先に確かめる） | なし | なし（バニラより前に来る） |

### 1.2 変更

- 各版の `server/` に、届く距離の確かめ方を 1 か所にまとめたクラス `Reach` を作る:
  - `static final double MARGIN = 16`（ブロック。届く距離の属性に足す余裕）
  - `static boolean block(プレイヤー, BlockPos)`: **先に読み込み済みかを確かめ**（1.20.1・1.21.1 は `level().isLoaded(pos)`、1.12.2 は `world.isBlockLoaded(pos)`）、読み込まれていなければ偽。そのあとに距離を確かめる
    - 1.20.1: `player.canReach(pos, MARGIN)`
    - 1.21.1: `player.canInteractWithBlock(pos, MARGIN)`
    - 1.12.2: 引数は `EntityPlayer` で受ける（掘り始めのイベントは `EntityPlayer` で来る。目の位置と届く距離の属性は `EntityPlayer` で読める。ほかの入口の `EntityPlayerMP` もそのまま渡せる。`Reach.entity` も同じ）。目の位置（`getPositionEyes(1)`）からブロックの中心までが `REACH_DISTANCE` の値 + `MARGIN` 未満
  - `static boolean entity(プレイヤー, Entity)`:
    - 1.20.1: `player.canReach(entity, MARGIN)`
    - 1.21.1: `player.canInteractWithEntity(entity, MARGIN)`
    - 1.12.2: `player.getDistanceSq(entity)` が (`REACH_DISTANCE` の値 + `MARGIN`)² 未満（固定の 6 ブロックをやめ、属性を見る）
- 上の表の全部の入口で、今の確かめ方を `Reach.block` / `Reach.entity` に置き換える。各クラスの `REACH_MARGIN`・`REACH_SQ`・`withinReach` は消す
  - 採掘のヒット（新しく足す）: `HitGate.allowed` のすぐ後、ブロックを読む前に `if (!Reach.block(player, pos)) return;`
  - 採掘の掘り始め（新しく足す。3 つの版）: 前の `Mining` を読み書きする前（`MINING.get` / `MINING.put` より前）に `if (!Reach.block(player, pos)) return;`。1.20.1・1.21.1 は START かを確かめた直後。1.12.2 はイベントに種類がなく掘り始めのときだけ来るので、`isRemote` / `isCanceled` の確認の直後。遠すぎる掘り始めはバニラも捨てるので、前の `Mining` を残したまま戻るのがバニラと同じ動き。1.12.2 は穴がないが、確かめ方を揃えるために同じ 1 行を足す
  - `keepDelayed` が読む前の掘り始めの座標（`previous.pos`）は、掘り始めを確かめるので、その時点で範囲内だったことが保証される（足さない）
  - 機械の問い合わせ: 今の「読み込み済みか」の確認は `Reach.block` に含まれるので、1 つにする
  - 右クリック: 座標はバニラが受け付けた右クリックの座標と同じでないと通らないので、読み込みの確認が増えても結果は変わらない
- 確かめに通らなかったヒット・問い合わせは、黙って捨てる（統計・コンボに数えない。返事も送らない。今と同じ）
- 余裕を 16 にしてよい理由（`doc/architecture.md` の「クライアントを信じる範囲」に書く）:
  - チャンクを作らせる攻撃を止めるのは、距離ではなく、先に読み込み済みかを確かめること。読み込まれていない座標は距離と関係なく弾く
  - 距離の確かめは、読み込まれた範囲の中で大きく外れていないかを見るだけ。届く距離の属性を読むので、属性で範囲を伸ばす Mod（エンチャント・装備・体の大きさを変える Mod など）にはそのまま付いていく。属性を使わずに範囲を伸ばす Mod も、16 ブロックまでは受け付ける
  - 改造したクライアントが少し遠くのブロックに当てたと送っても、ブロックでは得はない: 採掘のヒットは掘っているブロックと同じ座標でしか確定せず（掘れる距離はバニラが確かめる）、右クリックの系統はバニラが受け付けた右クリックの座標と同じでないと通らない。残るのは、ほかの人に聞こえる音と、機械の状態を少し遠くまで問い合わせられることだけ
  - 動物のヒットと問い合わせ（`AnimalHits.onHit` / `onQuery`）は、バニラが受け付けた操作に結び付いていないので、改造したクライアントなら届く距離の属性 + 16（初期値で 1.20.1・1.21.1 は 19、1.12.2 は 21 ブロック。今は 4〜6）先の動物に、壁越しにも効かせられる（ヒットは子の成長・繁殖の待ち・羊毛・卵・アルマジロの甲羅（1.21.1）・村人の取引の回復。問い合わせは動物のタイマーと村人の繁殖のヒント）。ただし間隔（`HitGate.ready`）は守られるので、得られるのはふつうに当て続けたときと同じまで。範囲を伸ばす Mod でも動物の弱点が効くことを優先し、生き物も 16 のままにする（ユーザーの判断）
  - どれも「照準をごまかせる」の範囲として**塞がない**
- コードのコメントは `Reach` に「余裕を大きく取る理由（範囲を伸ばす Mod）」と「読み込みの確認を先にする理由（チャンクの生成）」だけを書く
- `Reach` は版ごとに API が違うので、3 つの版それぞれに置く（§2 の `mojmap/` には置かない）
- 読み込まれていない座標の通信で、チャンクが増えないことは手では確かめにくい。PR の本文に `Reach.block` / `Reach.entity` を入れた場所（版・クラス・メソッド）の一覧を書き、レビューで漏れがないかを見る

## 2. 1.20.1・1.21.1 で同じファイルを `mojmap/` にまとめる

### 2.1 今

- 1.20.1 と 1.21.1 で、中身が 1 文字も違わない `.java` が 53 本ある（`diff -rqs` で確かめた。`client/` 31・`server/` 12・直下 6・`config/` 3・`network/` 1）。どちらの版も同じ変更を手で 2 回入れていて、片方だけ直し忘れても見つける仕組みがない
- テストの `config/SettingNamesTest` も、2 版で同じ中身

### 2.2 変更

- リポジトリの直下に `mojmap/src/main/java` と `mojmap/src/test/java` を作る。パッケージは今のまま（`io.github.zeusisgood.weakspot.…`）
- 移すもの: §1 を入れたあとに `diff -rqs` を取り、2 版で同じ `.java` を全部と、`SettingNamesTest`（`git mv` で 1.20.1 から移し、1.21.1 の同じファイルを消す）。点検の時点では 53 本。§1 で数が変わったら PR の本文に書く（注: 実装では 54 本。§1 で `server/MachineStates` が 2 版で同じ中身になった）
- 移さないもの: 1 文字でも違うファイル、`src/main/resources` の全部（AT・`mods.toml`／`neoforge.mods.toml`・アイコンなど）
- 読み込み: 1.20.1・1.21.1 の `build.gradle` の `sourceSets` に、`common` の 2 行の下へ次の 2 行を足す（Gradle のビルドは今のまま別々）
  - `main.java.srcDir '../mojmap/src/main/java'`
  - `test.java.srcDir '../mojmap/src/test/java'`
- `mojmap/` の約束（`doc/architecture.md` と `CLAUDE.md` に書く）:
  - Minecraft のクラスを使ってよい（`common/` との違い）。ただし、1.20.1（Forge・JDK 17）と 1.21.1（NeoForge・JDK 21）の**どちらでもそのままビルドが通る**ものだけを置く。Java の書き方は 17 まで
  - 版ごとに違いが要るようになったら、そのファイルを `mojmap/` から両方の版のディレクトリに戻して、それぞれ直す（`mojmap/` の中で版を分ける仕組みは作らない）
  - 名前で探すリフレクションを置くときは、SRG 名と Mojang の名前の両方を見る（1.20.1 は動いているとき SRG 名のため）
  - 機能を足す・直すときは、まず `mojmap/` にあるかを見る（あれば 1 回の変更で 2 版に効く）
- 1.12.2 は今のまま（MCP の名前で、API も違う）
- `ReleaseFilesTest` などがファイルの場所を決め打ちしていれば、`mojmap/` も見るように直す
- CI（`ci.yml`）は、パスで絞っていないので変えない（各版のビルドが `mojmap/` を読む）

## 3. 新しい 2 版のテストを足す

### 3.1 今

- `HitHandlers.has` と `QueryHandlers.handles` は「テスト用」だが、呼ぶテストは 1.12.2 にしかない。1.20.1・1.21.1 のテストは `SettingNamesTest` だけ
- 新しい `HitKind` を足して、新しい版だけ `HitHandlers` への登録を忘れると、その種類のヒットは黙って捨てられ、CI は緑のまま。`SyncedSettings` の項目の書き忘れは、起動したときの static の初期化で初めて分かる
- `LangFilesTest.keysWrittenInTheCodeExist`（コードに書いた翻訳キーが翻訳にあるか）は、`mc1.12.2/src/main/java` と `common/src/main/java` のコードだけを見ている。新しい 2 版にしかない物（ラクダ・メイス・試練のスポナーなど）の文の翻訳キーを書き間違えても、画面にキーの名前が出るだけでテストは通る（今あるキーは全部翻訳にあることを点検で確かめた）

### 3.2 変更

- `LangFilesTest.keysWrittenInTheCodeExist` の見る所に、`mc1.20.1/src/main/java`・`mc1.21.1/src/main/java`・`mojmap/src/main/java` を足す（作業ディレクトリはリポジトリの直下なので、1.12.2 のテストからそのまま読める）
- 1.12.2 の 3 本のテストを、新しい 2 版にも置く。置き場所は、テストするクラスが `mojmap/` にあるか（§2 の後）で決める:

| テスト | 中身 | 置き場所 |
|---|---|---|
| `compat/HitHandlersTest` | 全部の `HitKind` に `HitHandlers` の登録がある | `mojmap/src/test/java`（`HitHandlers` は 2 版で同じ） |
| `compat/QueryHandlersTest`（1.12.2 の `QueryMessagesTest` の登録の部分） | 問い合わせる種類（動物・釣り・機械）に `QueryHandlers` の登録がある | `mojmap/src/test/java`（`QueryHandlers` は 2 版で同じ） |
| `compat/QueryMessagesTest` の往復の部分 | `QueryMessage`・`StateMessage` を書いて読むと同じ中身に戻る | 各版の `src/test/java`（通信の部品が版で違う） |
| `config/SyncedSettingsTest` | 送る一覧（`WIRE`）と項目が一致する・全部の項目に設定がある・全部の種類に間隔がある | 各版の `src/test/java`（`SyncedSettings`・`WeakSpotConfig` が版で違う） |

- 書き方は各版の API に合わせる（1.12.2 の MCP の名前・`IMessage` を、Mojang の名前・各版の通信の部品にする）。確かめる中身とテストのメソッドの名前は 1.12.2 と同じにする。クラスの名前は、同じパッケージで重ならないよう、`mojmap/` に置く登録の部分だけ `QueryHandlersTest` にする（1.12.2 の `QueryMessagesTest` は分けない）
- テストの中で Minecraft の初期化（レジストリなど）が要って落ちるときは、確かめる中身を変えずに、初期化の要らない形にする（例: 登録の一覧だけを読む）。それでも置けないテストは置かず、PR の本文に理由を書く
- （注: 実装では、上の表の 4 本とも、書き換えた 1 つのファイルが 2 版でそのままビルドできたので、`mojmap/src/test/java` に 1.12.2 と同じクラスの名前（`HitHandlersTest`・`QueryMessagesTest`・`SyncedSettingsTest`）で置いた。`QueryMessagesTest` を分けないので、`QueryHandlersTest` は作らない。1.21.1 はテストのクラスパスに Minecraft がなく、処理の表のクラスを読み込めないので、`build.gradle` に `addModdingDependenciesTo sourceSets.test` を足した。1.20.1・1.21.1 だけの `SyncedSettings.tickingBlocks` はサーバーが作る一覧で設定がないので、`everyFieldHasAConfigEntry` では `serverVersion` と同じく外した）
- 確かめ方: 実装のときに、`HitHandlers` の登録を 1 つ消すと 2 版の `./gradlew test` が赤くなることを一度試す（コミットしない）

## 4. 統計の保存を `common/MiningStats` の 1 か所にまとめる

### 4.1 今

- 統計を NBT に書いて読む処理（`server/ServerStats.read` / `write`）が 3 つの版に手書きである。項目のキー・型・古いキー `critHits` の読み替えが 3 か所にある
- `MiningStats` に項目を足して、ある版の `write` だけ書き忘れると、その版では値がメモリーでは増えても、再ログインで 0 に戻る（保存データが消える）。どのテストも見ていない。通信の形（`writeTo` / `readFrom`）は `common/` に 1 か所あるのに、保存の形だけ 3 か所に分かれている

### 4.2 変更

- `common/MiningStats` に、保存先を表す小さなインターフェースと、2 つのメソッドを足す（Java 8 の書き方）:
  - `interface Store { boolean has(String key); long getLong(String key); double getDouble(String key); void putLong(String key, long value); void putDouble(String key, double value); }`
  - `void save(Store out)`: 今の `write` と同じキー・同じ型で全部の項目を書く（`savedTicks` だけ double）
  - `static MiningStats load(Store in)`: 今の `read` と同じ。近接は、新しいキー（`meleeHits`）がなく古いキー（`critHits`）があれば古いキーを読む（`legacySaveKey` の扱いをここへ移す）。ないキーは 0
- 各版の `ServerStats.read` / `write` は、NBT（1.12.2 は `NBTTagCompound`、1.20.1・1.21.1 は `CompoundTag`）を `Store` に包んで `load` / `save` を呼ぶだけにする（数行）。統計の置き場所（1.12.2・1.20.1 は持ち越されるプレイヤーのデータ、1.21.1 は死亡で消えないデータの添付）は変えない（注: 実装では、1.20.1・1.21.1 の受け渡しの部分が 2 版で同じ中身になったので、実装のレビューを受けて `mojmap/` の `server/StatsNbt` に移した。版ごとの `ServerStats` に残るのは置き場所だけ）
- キーの名前と型は今と同じ（古いワールドの統計はそのまま読め、1.13.8 で保存した統計は 1.13.7 でも読める）
- `common/` にテストを足す（`Store` は `HashMap` で作る）:
  - 全部の項目に違う値を入れて `save` → `load` すると、同じ値に戻る。項目は名前で並べず、`MiningStats` の public で static でない long / double のフィールドをリフレクションで全部取って値を入れる（種類ごとのヒット数は `HitKind.values()` で回す）。こうすると、項目を足して `save` / `load` を書き忘れたときに赤くなる
  - キーの一覧が今の形と同じ（`blocksBroken`・`hits`・`meleeHits`・`savedTicks` など。キーを変えると古いワールドが読めなくなるので、決め打ちで確かめる）
  - `critHits` だけがあるデータを読むと、近接の数になる。`meleeHits` もあれば `meleeHits` を読む
- 項目を足すときは `MiningStats` の `save` / `load`（と `writeTo` / `readFrom`）だけを直せばよくなる。`doc/architecture.md` の「種類を足すときに直す所」から、各版の `ServerStats` の読み書きを外す

## 5. `HitPitch` の、`HitScale.DEFAULT` を呼ぶだけのメソッドを消す（`common/`）

- 消す: `HitPitch.forStreak`・`forHit`・`resolveFor`・`breakNotes`・`twoOctave`（どれも `HitScale.DEFAULT` を呼ぶだけで、3 つの版のコードは `HitScale` を直接使っている。呼んでいるのはテストだけ）
- 残す: `withBell`・`chordOffsets`・`isSus4`・定数（`SCALE_LENGTH`・`BREAK_SOUND_FROM`・`BELL_FROM`・`TWO_OCTAVE_LENGTH` など）
- テスト（`HitPitchTest`・`HitStreakTest`・`HitScaleTest`）は `HitScale.DEFAULT.xxx` を呼ぶ形に書き換える。名前の対応: `forStreak` → `melody`、`forHit` → `forHit`、`resolveFor` → `resolveFor`、`breakNotes` → `breakNotes`、`twoOctave` → `twoOctave`。テストの名前と確かめる中身は変えない
- ただし `HitScaleTest.defaultMatchesHitPitch` の、`HitPitch` と `HitScale.DEFAULT` が同じかを比べる 2 行は、書き換えると自分自身と比べるだけになるので消す（同じテストの `cycleLength` の確認は残す）（注: 実装では、残った確認に合わせてテストの名前を `defaultCyclesThroughOneOctave` にした）

## 6. `ComboRelay` の毎 tick の掃除を消す（3 つの版。`ComboRelay` は 2 版で中身が違うので、`mojmap/` には移らない）

- 今: `ComboRelay` の tick の最後に、`SENT` と `OWN` の全部の要素について、プレイヤーがまだサーバーにいるかを調べて消している
- 消してよい理由: `HitGate` の退出の処理（ログアウト・リスポーン・ディメンションの移動。3 つの版とも `forgetAll` から）が `ComboRelay.forget` を呼び、ログアウトのときは `SENT` を、どの理由でも `OWN` を消している。いなくなったプレイヤーの要素は、毎 tick の掃除の前に必ず消えている
- 変更: その掃除（1.20.1 は `ComboRelay.java` の 77〜82 行目に当たる所。ほかの 2 版も同じ所）を消す
- 1 人用の世界を閉じて別の世界を開いたときも、ゲーム本体がログアウトの知らせを出すので残らない
- 確かめ方: クラウドの runServer にはクライアントが入れないので、実装のときに 3 つの版の退出の経路（ログアウト・リスポーン・ディメンションの移動 → `forgetAll` → `ComboRelay.forget`）をコードで読んで確かめる。実際の動きは §9 のマルチの項目（2 人で出入り）で確かめてもらう

## 7. README・doc

- `CHANGELOG.md`: 「## 1.13.8」は「サーバー側の不具合の修正と内部の整理」と、通信内容・保存データに変更がないことだけを書く（内部の事情なので、中身は書かない。ユーザーの判断。経緯はこの仕様書とコミットに残る）。README（日本語）の「最近の更新」も同じ書き方
- README（日英）の「最近の更新」に 1.13.8（新しい 3 件にする）。お知らせ `weakspot.news.1.13.8`: 日本語「サーバー側の不具合を直し、内部を整理しました」、英語「Server-side fixes and internal cleanup」（内部の事情なので、ゲームの中のお知らせには中身を書かない。ユーザーの判断）
- `doc/spec/README.md`: 表の一番上の 1.13.8 の行から「（下書き）」を外す
- `update.json` は「リリースの手順」どおり
- `doc/architecture.md`:
  - 「クライアントを信じる範囲」に、§1.2 の「余裕を 16 にしてよい理由」を短く足す（座標は読み込み済みかを先に確かめる。採掘の掘り始めのイベントは、ローダーによってはバニラの距離の確認より前に来るので、そこでも確かめる・距離は属性 + 16・動物のヒットと問い合わせは 16 ブロック先まで効くが間隔は守られるので塞がない・確かめ方は `Reach` にまとめる）
  - リポジトリの形に `mojmap/` と §2.2 の約束を足す
  - 「種類を足すときに直す所」のファイルの場所を、`mojmap/` に移ったものは `mojmap/` に直し、各版の `ServerStats` の読み書きを外す
  - 「共通の部品」に `server/Reach` と `MiningStats.save` / `load`（統計の保存の正本）を足す
- `doc/development.md`: 冒頭のリポジトリの形に `mojmap/` を足す
- 直さない文書: `doc/play.md`・ガイドの本（遊び方は変わらない。範囲を伸ばす Mod との組み合わせは更新履歴に書く）（注: 実装では、更新履歴にも中身を書かない判断のとおり書いていない）・`doc/config.md`（設定の項目は変わらない。余裕は設定にしない）・`doc/roadmap.md`・`doc/store/description.md`・`doc/install.md`

## 8. `CLAUDE.md` に書くこと

- 「バージョン」の現行と「次の作業」を 1.13.8 にする（範囲は変えない）
- 「開発環境・コマンド」のリポジトリの形に足す: 「`mojmap/`（1.20.1・1.21.1 で同じ中身のコード。両方のビルドがソースで読む。Minecraft のクラスを使ってよいが、両方でそのままビルドが通るものだけ。違いが要るようになったら両方の版に戻す）」
- 「アーキテクチャの約束事」に 2 行足す:
  - 「クライアントから来た座標・生き物は、`server/Reach`（読み込み済みか → 届く距離の属性 + 16）で確かめてから読む（読み込まれていない座標を読むと、チャンクの読み込み・生成が走る。採掘の掘り始めのように、バニラのパケットから来るイベントの座標も、ローダーがバニラの確認より前に出すことがあるので確かめる）」
  - 「統計の保存（NBT のキー）は `common/MiningStats.save` / `load` が正本。各版の `ServerStats` は受け渡すだけ。キーの名前を変えない」

## 9. ユーザーに確認してもらうこと

- 3 つの版: ふつうに採掘の弱点が当たって加速すること。走りながら・ジャンプしながら掘っても今までどおりか。ブロックをクリックし直したときに、見えない壁（壊れたように見えて残るブロック）が出ないこと
- 3 つの版: 成長・収穫・機械・動物の弱点が今までどおり当たること。機械のバー・動物のタイマーが今までどおり出ること
- 1.12.2: 動物の弱点が、今までどおりの距離で当たること（届く距離の属性を見るようになったので）
- 3 つの版: 1.13.7 で遊んだワールドを 1.13.8 で開いて、統計画面の値（累計・種類ごとのヒット数・的当ての記録）がそのまま残っていること。少し遊んでから入り直しても、増えた分が残ること
- 1.20.1・1.21.1: ひととおりの弱点（近接・弓・移動など）が今までどおり出て効くこと。設定画面・統計画面・音のタブが今までどおり開くこと
- マルチ（できれば）: 2 人で入って出たあと、コンボの表示（自分と相手）が今までどおり出ること
- 届く範囲を伸ばす Mod を持っていれば: 遠くのブロック・動物の弱点も当たって効くこと
