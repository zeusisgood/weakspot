# 弱点破壊 Mod 追加仕様書（v1.9.4）

`SPEC_v1.9.3.md`（Mod 1.9.3）に対する**パッチ**の仕様。ここに書かれていないことは、それと現行実装のままとする。
この仕様書は `doc/spec/SPEC_v1.9.4.md`。

- 対象: Minecraft Java Edition 1.12.2 / Forge 14.23.5.2860
- 前提: 1.9.3 がリリース済み
- この仕様書の内容は、Mod のバージョン **1.9.4** として、リリースする

---

## 0. バージョンと互換性

- バージョンは 1.9.3 → **1.9.4**（`build.gradle` の `version` と `WeakSpotMod.VERSION`）
- 表示の名前（翻訳）と、Mod の情報（`mcmod.info`・ロゴの画像）だけを変える。計算・通信内容・保存データ・設定は変えない。1.9.x 同士はそのまま接続できる

## 1. やりたいこと（ユーザーの報告と判断）

- 統計画面の全期間の欄で「1 ブロックあたりの平均ヒット数 17.17」「1 ブロックでの最多ヒット数 1」と出て、食い違って見えた
- 原因: 平均は `MiningStats.averageHitsPerBlock()` = 採掘ヒット数（壊しきらなかったブロックへのヒットも含む）÷ 壊したブロック数。最多は壊したブロックだけで数える
- 相談の結果（案 A）: この版では、名前を計算の中身どおりにする。計算を直す（壊したブロックへのヒットだけで割る）のは、統計の送る内容が増えるので 1.10.0 に回す（`SPEC_v1.10.0.md` の 2b）

## 2. 名前

| キー | 今 | 新（日本語） | 新（英語） |
|---|---|---|---|
| `weakspot.stats.averageHits` | 1ブロックあたりの平均ヒット数 / Average hits per block | 採掘ヒット数 ÷ 壊したブロック数 | Mining hits ÷ blocks broken |
| `weakspot.command.stats.session` / `.total` の `avg hits %s` | 平均 / avg hits | `ヒット÷ブロック %s` | `hits/blocks %s` |

- 統計画面の列の幅に収まること（右の数字の列と重ならない）を確かめる
- `doc/play.md` の統計の表（「1 ブロックあたりの平均ヒット数｜採掘ヒット数 ÷ 壊したブロック数」）の名前も合わせ、「壊しきらなかったブロックへのヒットも数える」と書き足す

## 2b. ゲーム内の Mods の一覧（ユーザーの判断）

- 配布サイトと同じアイコン（`doc/images/icon.png`。石のブロックと弱点の円）を、ゲーム内の Mods の一覧のロゴにする
  - 背景を透過した版（`doc/images/icon-transparent.png`。ユーザーが切り抜いた）を `src/main/resources/logo.png` に置き（256×256 に縮めて jar を軽くする）、`mcmod.info` の `logoFile` を `"logo.png"` にする
- `mcmod.info` のほかの項目も埋める
  - `description`: 英語にする（Mods の一覧は言語で切り替わらないため）。例 `Fortnite-style weak spots for mining, farming, travel and more. Aim at the glowing circle to speed things up.`
  - `url`: `https://github.com/zeusisgood/weakspot`
  - `authorList`: `["zeusisgood"]`
- ユーザーに、Mods の一覧でロゴと説明が出ることを確かめてもらう

## 3. README・doc・お知らせ

- `CHANGELOG.md`・README の「最近の更新」: 統計の「1 ブロックあたりの平均ヒット数」の名前を、計算の中身どおり「採掘ヒット数 ÷ 壊したブロック数」に変更。ゲーム内の Mods の一覧にロゴと英語の説明を追加。通信内容・保存データ・設定に変更なし
- 更新のお知らせ `weakspot.news.1.9.4`: 「統計の平均ヒット数の名前を直した」

## 4. `CLAUDE.md` に書くこと

- 現行を 1.9.4 に

## 5. ユーザーに確認してもらうこと

- 統計画面（K キー）の「統計」タブで、新しい名前が数字と重ならずに出ること（日本語・英語）
- タイトル画面の「Mods」で Weak Spot Mining を選ぶと、ロゴ・英語の説明・作者・URL が出ること
