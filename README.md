# RaiseSNS

## 概要

X（旧Twitter）のタイムライン形式を模した、学習目的の SNS 風アプリです。複数ユーザーでの利用を想定し、投稿・コメント・いいねを他ユーザーと行き来できる構成にしています。

## 機能一覧

| 機能 | 状態 |
|---|---|
| 会員登録・ログイン・ログアウト | 完了 |
| 全体タイムライン表示 | 完了 |
| フォロー中タイムライン表示 | 完了 |
| ユーザー検索 | 完了 |
| フォロー／フォロー解除 | 完了 |
| 投稿作成・編集・削除 | 完了(画像添付は任意) |
| コメント作成・削除・返信(ネスト) | 完了 |
| いいね・いいね解除 | 完了 |
| 画像投稿 | 完了 |
| プロフィール編集・閲覧 | 完了(アイコン画像アップロード含む) |

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

### AWS S3のセットアップ（画像アップロード機能に必要）

画像投稿・プロフィールアイコンのアップロード先として、実際のAWS S3バケットを使用します。以下はユーザー自身のAWSアカウントで一度だけ行う手順です。

```bash
# 1. S3バケットを作成
aws s3 mb s3://<バケット名> --region ap-northeast-1

# 2. パブリックアクセスブロックを一部解除（バケットポリシーでの公開を許可するため）
aws s3api put-public-access-block --bucket <バケット名> --public-access-block-configuration \
  BlockPublicAcls=false,IgnorePublicAcls=false,BlockPublicPolicy=false,RestrictPublicBuckets=false

# 3. posts/・avatars/ 配下を公開読み取り可能にするバケットポリシーを設定
cat <<'EOF' > bucket-policy.json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "PublicReadImages",
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": [
        "arn:aws:s3:::<バケット名>/posts/*",
        "arn:aws:s3:::<バケット名>/avatars/*"
      ]
    }
  ]
}
EOF
aws s3api put-bucket-policy --bucket <バケット名> --policy file://bucket-policy.json

# 4. アプリ用IAMユーザーを作成し、PutObjectのみを許可する最小権限ポリシーをアタッチ
#    （s3:GetObjectは上記でパブリックにしているため不要）
```

発行したアクセスキーを `.env` の以下の項目に設定してください（`.env` はgitignore対象）。

```
AWS_ACCESS_KEY_ID=<発行したアクセスキーID>
AWS_SECRET_ACCESS_KEY=<発行したシークレットキー>
AWS_REGION=ap-northeast-1
AWS_S3_BUCKET=<バケット名>
```

## API仕様書

バックエンド起動中、以下のURLからSwagger UI経由でAPI仕様を確認・試行できます。

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

Cookie認証が必要なAPIは、先にSwagger UI上で `POST /api/auth/login` を実行してください。同一ブラウザにCookieが発行され、以降のAPIもそのまま試行できます。

## ドキュメント

- [要件定義書](docs/requirements.md)
- [機能要件](docs/functional-requirements.md)
- [機能定義書（機能単位）](docs/features/)
- [技術スタック](docs/tech-stack.md)
- [画面設計書](docs/screen-design.md)
- [データベース設計](docs/database-design.md)
- [開発ログ](DEVELOPMENT.md)
