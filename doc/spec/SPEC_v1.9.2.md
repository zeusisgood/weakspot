# 弱点破壊 Mod 追加仕様書（v1.9.2）

`SPEC_v1.9.1.md`（Mod 1.9.1）に対する**パッチ**の仕様。ここに書かれていないことは、それと現行実装のままとする。
この仕様書は `doc/spec/SPEC_v1.9.2.md`。

- 対象: Minecraft Java Edition 1.12.2 / Forge 14.23.5.2860
- 前提: 1.9.1 がリリース済み
- この仕様書の内容は、Mod のバージョン **1.9.2** として、リリースする

---

## 0. バージョンと互換性

- バージョンは 1.9.1 → **1.9.2**（`build.gradle` の `version` と `WeakSpotMod.VERSION`）
- クライアントの中だけの変更（新しい版の確認と、チャットの通知）。通信内容・サーバーの保存データは変わらない。1.9.x 同士は、そのまま接続できる
- 設定は `[クライアント]` の項目を 2 つ足すだけ（既存のキーは変えない。移行は要らない）
- 1.9.1 以前には `updateJSON` がないので、通知は 1.9.2 を入れた人から始まる（1.9.2 自体の公開は、これまでどおり更新のお知らせで知らせる）

## 1. やりたいこと（ユーザーの判断）

- 新しい版が公開されていたら、ゲーム内のチャットで知らせる
- 相談で並べた案（A: Forge 標準の更新確認、B: 自前で GitHub API、C: Modrinth の API）のうち、**A**。以下の決めごとも、すべて相談のおすすめのとおり

## 2. 版の確認（Forge 標準の更新確認）

- `@Mod` に `updateJSON = "https://raw.githubusercontent.com/zeusisgood/weakspot/main/update.json"` を足す
  - Forge が起動時に別スレッドで取りに行く。Mod 一覧の★印も、これで付く
  - Forge の `forge.cfg` の `versionCheck=false` のときは、Forge が取りに行かない（通知も出ない）
- `update.json` はリポジトリの直下に置く（Forge の形式）

```json
{
  "homepage": "https://github.com/zeusisgood/weakspot/releases",
  "promos": {
    "1.12.2-latest": "1.9.2",
    "1.12.2-recommended": "1.9.2"
  },
  "1.12.2": {
    "1.9.2": "Chat notice when a new version is out"
  }
}
```

- `promos` の 2 つには、最新の版を同じ値で入れる。`"1.12.2"` の中には、版ごとに英語のお知らせ `weakspot.news.<版>`（`en_us.lang`）の 1 行を足していく（Mod 一覧の変更点に出る）
- 置き場所は `main`。**リリースのたびに「Release X.Y.Z」のコミットで Claude が書き換える**（リリースの流れに足す）。Merge から Actions が Release を作り終えるまでの数分だけ、リンク先がまだない時間ができるが、よしとする

## 3. チャットの通知

- 出すとき: ワールドに入ってから **60 tick 後**（更新のお知らせ `UpdateNotes` の 40 tick より後）に、1 起動につき 1 回まで確かめる
  - `ForgeVersion.getResult(自分の ModContainer)` の状態が `OUTDATED` か `BETA_OUTDATED` で、`target`（新しい版）が `lastNotifiedVersion` と違うときだけ出す
  - 出したら `lastNotifiedVersion` に新しい版を書いて保存する（**同じ新版については 1 回だけ**。さらに新しい版が出たら、また出す）
  - 確認がまだ終わっていない（`PENDING`）ときは、そのワールドにいる間、200 tick まで待ってから諦める（次の起動でまた確かめる）。`FAILED`（オフライン・取得の失敗）、`UP_TO_DATE`、`AHEAD`（開発版）などは何も出さない
- 文面（1 行目は金色 `#FFAA00`、リンクは更新のお知らせと同じ水色 `#55FFFF` の下線）

| キー | 日本語 | 英語 |
|---|---|---|
| `weakspot.updateCheck.available` | [弱点] 新しい版 %s が公開されています（今は %s） | [Weak Spot] Version %s is available (you have %s) |
| `weakspot.updateCheck.link` | [ダウンロード] | [Download] |
| `weakspot.updateCheck.minor` | サーバーと同じ版に揃えないと接続できません。更新するときは、サーバーの管理者にも伝えてください | Servers must use the same version to connect. Let the server admin know when you update |

- `[ダウンロード]` は **GitHub の Release ページ**（`https://github.com/zeusisgood/weakspot/releases/tag/v<新しい版>`。`UpdateNotes.RELEASES_URL` を使う）を開く。配布サイトができたら差し替える
- **マイナーが違う新版**（例: 今が 1.9.x で、新しい版が 1.10.0）のときは、2 行目に `weakspot.updateCheck.minor` を灰色で出す
- 判定（通知するか・マイナーが違うか）は Minecraft に依存しない形で `common/UpdateCheck` に置き、単体テストを書く
  - 通知する: 状態が新版あり、かつ新しい版 ≠ `lastNotifiedVersion`（`lastNotifiedVersion` が空でも出す）
  - マイナーが違う: `1.9.1` と `1.10.0` → 違う、`1.9.1` と `1.9.2` → 同じ、`1.9.1` と `2.0.0` → 違う、数字でない形 → 違わない（添えない）

## 4. 設定（`[クライアント]`。`general` に並べる）

| キー | 初期値 | 説明 |
|---|---|---|
| `checkForUpdates` | true | 新しい版が公開されていたら、ワールドに入ったときにチャットで知らせるか（Forge の `versionCheck` が false のときは出ない） |
| `lastNotifiedVersion` | （空） | 最後に通知した新しい版。Mod が書き換えるため、編集しないでください |

- 設定画面の説明 `weakspot.general.checkforupdates.tooltip` / `weakspot.general.lastnotifiedversion.tooltip` を `en_us.lang` と `ja_jp.lang` に足す
- `SyncedSettings` には入れない（クライアントだけで使う）

## 5. README・doc・お知らせ

- `doc/play.md`: 更新のお知らせの節に、新しい版の通知（1 回だけ、止め方は `checkForUpdates`）を書き足す
- `doc/config.md`: 4 の 2 項目を表に足す
- `doc/install.md`: 更新時の注意に、「新しい版はゲーム内のチャットでも知らせる（GitHub から版の情報だけを取りに行く。止めるには `checkForUpdates=false`）」を書き足す
- ガイドの本: 「困ったとき」などに、新しい版はチャットで知らせる旨を 1 文足す（ページの行数に収まる範囲で。収まらなければ見送る）
- `CHANGELOG.md`・README の「最近の更新」: 新しい版の公開をチャットで通知。通信内容に変更なし。1.9.x 同士はそのまま接続可能
- 更新のお知らせ `weakspot.news.1.9.2`: 「新しい版が出たらチャットで知らせるようにした」／「Chat notice when a new version is out」
- 配布サイトの説明文 `doc/store/description.md`: 主な機能ではないので直さない

## 6. `CLAUDE.md`・`doc/architecture.md` に書くこと

- 現行を 1.9.2 に
- リリースの流れに「`update.json` の `promos` の 2 つを新しい版にし、`"1.12.2"` に英語のお知らせの 1 行を足す」を足す
- `doc/architecture.md` のクライアントの項目に、新しい版の通知（`client/UpdateCheckNotice` など、`common/UpdateCheck`、`updateJSON`、`lastNotifiedVersion`）を足す

## 7. ユーザーに確認してもらうこと

- 1.9.2 を入れた状態で、`update.json` の版を一時的に上げたもの（例: 1.9.3。確認用のブランチの URL に向けた jar を Claude が用意する）でワールドに入ると、チャットに通知と `[ダウンロード]` が出ること。もう一度起動しても、同じ版では出ないこと
- `1.10.0` にしたときは、2 行目の「サーバーと同じ版に揃えて…」が出ること
- Mod 一覧で、Weak Spot Mining に更新の★印が付くこと
- `checkForUpdates=false` にすると出ないこと
