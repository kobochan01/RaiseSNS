# 開発ログ

## エラーログ

| 日付 | エラー内容 | 原因 | 解決策 |
|---|---|---|---|
| 2026-09-02 | TestcontainersがDocker Desktopに接続できない（`Could not find a valid Docker environment`） | Docker Desktopの既定パイプ経由だと内部プロキシ(`docker_cli`)に接続され空応答が返る。また同梱docker-javaの既定APIバージョン(1.32)がDocker Desktopの最小要求(1.40)を下回る | `DOCKER_HOST`に生のエンジンパイプ`npipe:////./pipe/docker_engine_linux`を指定し、Gradleの`test`タスクに`systemProperty 'api.version', '1.41'`を設定 |
| 2026-09-02 | `docker compose up`のDBに`bootRun`から接続すると`パスワード認証に失敗`になる | ホストのポート5432に別プロジェクト用のネイティブPostgreSQLが既に待受しており、Dockerのポートフォワードと衝突していた | `docker-compose.yml`・`.env.example`・`application.yml`の既定ポートを5433に変更（他プロジェクトと同じ回避策に合わせた） |
| 2026-09-02 | `./gradlew test`を全クラス一括実行すると一部の統合テストが断続的に`Connection refused`で失敗 | Testcontainersの静的コンテナをテストクラス間で共有すると、Windows版Docker DesktopのWSL2ポートフォワーディングが長時間接続で不安定になる | 各統合テストクラスごとに独立したPostgreSQLコンテナを起動する構成に変更（`AbstractIntegrationTest`から共有フィールドを削除し、各クラスで`@Container`を宣言） |

## 作業記録

| 日付 | Issue | 内容 |
|---|---|---|
| 2026-09-02 | #3 | 会員登録・ログイン機能（JWT認証、MyBatis、Flyway）のバックエンド実装。`backend/`をGradleプロジェクトとして新規作成 |
| 2026-09-08 | #21 | フォロー中タイムライン機能のバックエンド実装。フロントエンドは事前に対応済みだったため、`PostMapper`/`PostService`にフォロー中スコープ用のクエリを追加するのみで完結 |
| 2026-09-08 | #24 | springdoc-openapiを導入しSwagger UI/OpenAPI JSONを自動生成できるようにした。Spring Boot 3.5.0同梱のSpring Web 6.2.7に起因するパスパターン解析のバグを避けるため、springdocは2.8.13に固定した |
| 2026-09-09 | #26 | 単体テストのギャップ埋め。既存のController/Service/Mapperテストは401/403/404/400のエラー系・所有者チェック・limitクランプまで網羅した高品質なものが揃っていたため全面書き直しはせず、唯一未カバーだった`RefreshTokenMapper`とAuth/Followの一部エッジケース、フロントエンドのAPIクライアント層(`src/api/*.ts`)・`AuthContext`・`utils`のテストを追加した。テスト用DBはH2を使わず既存のTestcontainers+実PostgreSQLコンテナ方針を継続(FlywayマイグレーションがPostgreSQL方言のSQLを使っており完全互換ではないため) |
