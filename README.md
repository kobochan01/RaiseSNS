# RaiseSNS

## 概要

X（旧Twitter）のタイムライン形式を模した、学習目的の SNS 風アプリです。複数ユーザーでの利用を想定し、投稿・コメント・いいねを他ユーザーと行き来できる構成にしています。

## 機能一覧

| 機能 | 状態 |
|---|---|
| 会員登録・ログイン・ログアウト | 未着手 |
| 全体タイムライン表示 | 完了 |
| フォロー中タイムライン表示 | 未着手 |
| ユーザー検索 | 未着手 |
| フォロー／フォロー解除 | 未着手 |
| 投稿作成・編集・削除 | 完了(テキストのみ、画像は未着手) |
| コメント作成・編集・削除 | 未着手 |
| いいね・いいね解除 | 未着手 |
| 画像投稿 | 未着手 |
| プロフィール編集・閲覧 | 未着手 |

## 技術スタック

| 領域 | 技術 |
|---|---|
| フロントエンド | React（Vite, TypeScript） |
| バックエンド | Spring Boot（Gradle） |
| データベース | PostgreSQL |
| 画像ストレージ | AWS S3 |

バージョン等の詳細は [技術スタック](docs/tech-stack.md) を参照。

## セットアップ

```bash
# 依存パッケージのインストール（backend/）
./gradlew build

# 依存パッケージのインストール（frontend/）
npm install

# 起動はCLAUDE.mdの「環境・起動」を参照
```

## ドキュメント

- [要件定義書](docs/requirements.md)
- [機能要件](docs/functional-requirements.md)
- [機能定義書（機能単位）](docs/features/)
- [技術スタック](docs/tech-stack.md)
- [画面設計書](docs/screen-design.md)
- [データベース設計](docs/database-design.md)
- [開発ログ](DEVELOPMENT.md)
