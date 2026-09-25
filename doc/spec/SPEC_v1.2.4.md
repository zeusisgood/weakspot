# 弱点破壊 Mod 追加仕様書（v1.2.4）

`SPEC_v1.2.3.md`（Mod 1.2.3）に対する**パッチ**の仕様。ここに書かれていないことは、それと現行実装のままとする。
仕様書は、リポジトリの `doc/` ディレクトリに置く（この仕様書は `doc/spec/SPEC_v1.2.4.md`）。

- 対象: Minecraft Java Edition 1.12.2 / Forge 14.23.5.2860
- 内容: キノコが育つ確率 `mushroomGrowChance` の初期値を 0.1 → **0.2** にする（ユーザーが遊んで決めた値）
- この仕様書の内容は、Mod のバージョン **1.2.4** として、リリースする

## 0. バージョンとパッチの制約

- バージョンは 1.2.3 → **1.2.4**。`ACCEPTED_VERSIONS`（`[1.2,1.3)`）は変えない
- 通信内容・保存データ・設定キーは変えない（`mushroomGrowChance` はサーバーだけが使う）

## 1. 変更

- `mushroomGrowChance` の初期値を **0.2** にする
- 既存の設定ファイルの移行（1.2.2 の `configVersion` の仕組みを使う）
  - `configVersion` が 2 未満なら、`mushroomGrowChance` が古い初期値 0.1 のときだけ 0.2 にする（ほかの値は、管理者が変えたものとして残す）。`configVersion` を 2 にして保存する
  - 1.2.1 以前の設定ファイル（`configVersion` が 0）は、1 の移行（キノコを除外リストから取り除く）に続けて、2 の移行も行う（`mushroomGrowChance` がないので、初期値 0.2 のまま）
- README（設定表・説明・更新履歴）の値を直す

## 2. 確認事項

- ビルド・テストが通ること
- `configVersion=1`・`mushroomGrowChance=0.1` の設定ファイルが、起動後に `configVersion=2`・`mushroomGrowChance=0.2` になること。0.3 などは変わらないこと
