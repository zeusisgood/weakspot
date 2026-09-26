# 弱点破壊 Mod 追加仕様書（v1.9.1）

`SPEC_v1.9.0.md`（Mod 1.9.0）に対する**パッチ**の仕様。ここに書かれていないことは、それと現行実装のままとする。
この仕様書は `doc/spec/SPEC_v1.9.1.md`。

- 対象: Minecraft Java Edition 1.12.2 / Forge 14.23.5.2860
- 前提: 1.9.0 がリリース済み
- この仕様書の内容は、Mod のバージョン **1.9.1** として、リリースする

---

## 0. バージョンと互換性

- バージョンは 1.9.0 → **1.9.1**（`build.gradle` の `version` と `WeakSpotMod.VERSION`）
- クライアントの中だけの変更（`/weakspot bug` の文章と本文）。通信内容・保存データ・設定は変わらない。1.9.x 同士は、そのまま接続できる

## 1. やりたいこと（ユーザーの報告と判断）

- テストで立てた不具合報告（issue #3）の環境が「Windows 8.1 6.3」になっていたが、実際は Windows 11
  - 原因: Minecraft のランチャーが同梱する古い Java 8（8u51）は Windows 10・11 を知らず、`os.name` を「Windows 8.1」と答える（Windows 10 は 8u60、Windows 11 は 8u321 から正しく答える）
- 報告の文章（チャットの案内と issue の本文）の言葉遣いを、一般的な不具合報告のテンプレートに合わせて、もう少し固くする

## 2. OS 名の注記（ユーザーの判断: 案 C。判定はせず、注記だけ）

- 環境の行の OS は、今までどおり Java の答え（`os.name` `os.version` `os.arch`）のまま
- **Windows で、Java が 8u321 より古いとき**（`java.version` が `1.8.0_N` で N < 321）は、その行の末尾に注記を付ける
  - 日本語: 「（古い Java のため OS 名が不正確な可能性あり。Windows 10・11 も 8.1 と表示される）」
  - 英語: " (may be inaccurate on this old Java; Windows 10 and 11 show as 8.1)"
- 判定（Windows か、Java の版が古いか）は `common/BugReport` に置き、単体テストを書く（`1.8.0_51` → 注記あり、`1.8.0_321` → なし、Windows 以外 → なし、`1.8.0_51` 以外の形の版 → なし）

## 3. issue の本文の見出し

一般的な不具合報告のテンプレート（GitHub の既定の雛形の Describe the bug / To Reproduce / Expected behavior / Environment / Additional context、日本語のリポジトリで多い 概要 / 再現手順 / 期待される動作 / 実際の動作 / 環境 / 補足）に合わせる。

| 順 | 日本語 | 英語 | 中身 |
|---|---|---|---|
| 1 | 概要 | Summary | 空欄 |
| 2 | 再現手順 | Steps to reproduce | `1. ` を入れておく |
| 3 | 実際の動作 | Actual behavior | 空欄 |
| 4 | 期待される動作 | Expected behavior | 空欄 |
| 5 | 環境（自動入力） | Environment (auto-filled) | 今と同じ環境の行と Mod の一覧（長すぎれば「Mod の一覧はクリップボードにコピー済み。ここに貼り付けてください」） |
| 6 | 補足（ログ・スクリーンショット） | Additional context (logs, screenshots) | 空欄 |

- `BugReport.body` は、環境の前と後ろに見出しを置ける形にする（今は環境が最後）
- issue のタイトルは今のまま（`[1.9.1] ` を入れておく）

## 4. チャットの案内の言葉遣い

| キー | 今 | 新（日本語） | 新（英語） |
|---|---|---|---|
| `weakspot.bug.title` | [弱点] 不具合の報告の方法 | [弱点] 不具合報告 | [Weak Spot] Bug report |
| `step1` | ここから報告できます: %s（環境を書き込んだ状態で開きます） | 報告先: %s（環境情報を記入済みの状態で開きます） | Report here: %s (opens with your environment filled in) |
| `openIssue` | [GitHub の issue を開く] | [GitHub で issue を作成] | [Create a GitHub issue] |
| `step2` | 書いてほしいこと: 何をしたか / 何が起きたか / どうなるはずだったか | 記載事項: 概要・再現手順・実際の動作・期待される動作 | Please include: summary, steps to reproduce, actual and expected behavior |
| `step3` | 今の環境（クリップボードにもコピーしました）: %s | 環境情報（クリップボードにコピー済み）: %s | Environment (copied to the clipboard): %s |
| `step4` | あると助かるもの: … | 添付推奨: クラッシュ時は crash-reports フォルダーの最新ファイル、それ以外は logs/latest.log、スクリーンショット（F2） | Recommended attachments: the newest file in crash-reports after a crash, otherwise logs/latest.log, and a screenshot (F2) |
| `step5` | 報告には GitHub のアカウントが要ります。… | 報告には GitHub アカウントが必要です。ログ・クラッシュレポートは自動送信されません | A GitHub account is required. Logs and crash reports are never sent automatically |

- 番号（1.〜5.）の並びは今のまま

## 5. GitHub の issue の雛形（ゲームを通さずに報告する人向け）

- `.github/ISSUE_TEMPLATE/bug_report.md` を足す。見出しは 3 と同じ（日本語と英語を併記。例「## 概要 / Summary」）。環境の節には「ゲーム内で `/weakspot bug` を使うと、環境情報が自動で入ります」と書く
- `.github/ISSUE_TEMPLATE/config.yml` で空の issue を許可する（`blank_issues_enabled: true`）。ゲームからのリンク（`issues/new?title=…&body=…`）で本文が消えないようにするため

## 6. README・doc・お知らせ

- `doc/play.md` の「不具合報告（`/weakspot bug`）」の節: 本文の見出しと、古い Java での OS 名の注記を書き足す
- `CHANGELOG.md`・README の「最近の更新」: 報告の文章を整理、古い Java での OS 名の注記。通信内容・設定に変更なし。1.9.x 同士はそのまま接続可能
- 更新のお知らせ `weakspot.news.1.9.1`: 「不具合報告の文章を整理した」
- ガイドの本の「困ったとき」の `/weakspot bug` の文は、そのままでよい（遊び方は変わらない）

## 7. `CLAUDE.md`・`doc/architecture.md` に書くこと

- 現行を 1.9.1 に
- `doc/architecture.md` の `/weakspot bug` の項目に、見出しの並びと OS 名の注記（`BugReport` の判定）を足す

## 8. ユーザーに確認してもらうこと

- `/weakspot bug` のチャットの案内が、新しい言葉遣いで出ること
- 開いた issue の本文が、概要・再現手順（`1. `）・実際の動作・期待される動作・環境・補足の順で、環境の OS の行に注記が付くこと（ランチャーの古い Java の場合）
- GitHub の「New issue」から、雛形（不具合報告）が選べること。ゲームからのリンクでは、本文が入った状態で開くこと
