# 新プロジェクト セットアップチェックリスト

> PersonalTemplate をコピーしたら、このファイルを開いて手順を進めてください。
> すべて完了したら **このファイルを削除** してください。

---

## 1. プレースホルダーの置き換え

- [ ] フォルダ名を `<プロジェクト名>` に変更する
- [ ] `CLAUDE.md` の `<プロジェクト名>` を実際のプロジェクト名に変更する
- [ ] `README.md` の `<プロジェクト名>` を実際のプロジェクト名に変更する

## 2. CLAUDE.md を埋める

- [ ] `## 環境・起動` に起動・終了・再起動コマンドを記載する
- [ ] `## 技術スタック固有のルール` にコード品質ルールを記載する（不要なら削除）
- [ ] `## ドキュメント更新対象` を実際のファイルリストに合わせる

## 3. README.md を埋める

- [ ] 概要（何をするアプリか・なぜ作るか）を記載する
- [ ] 技術スタックを記載する
- [ ] セットアップ手順を記載する

## 4. docs/ を埋める

- [ ] `docs/requirements.md` に目的・ユーザーストーリー・機能要件を記載する
- [ ] `docs/design.md` は設計が固まってから記載する（空のままでも可）

## 5. Git 初期化

```powershell
cd C:\Projects\<プロジェクト名>
git init
git add .
git commit -m "chore: プロジェクトを初期化する"
```

## 6. GitHub リポジトリの作成

```powershell
gh repo create <プロジェクト名> --private --source=. --remote=origin --push
```

## 7. GitHub 設定

- [ ] ラベルを作成する

```powershell
gh label create "enhancement" --color "a2eeef"
gh label create "documentation" --color "0075ca"
gh label create "chore" --color "e4e669"
# "bug" はデフォルトで存在する
```

- [ ] ブランチ保護ルールを設定する（GitHub の Settings → Branches → main への直接 push を禁止）

## 8. 動作確認

- [ ] `.claude/settings.json` の Stop フックが動作するか確認する
  - テスト: コードファイルだけコミットして Claude を停止させ、警告が出ることを確認
- [ ] Claude Code でプロジェクトを開き、CLAUDE.md が読み込まれることを確認する

## 9. 後片付け

- [ ] **この SETUP.md を削除する**

---

完了したら最初の Issue を作成してブランチを切り、開発を開始してください。
