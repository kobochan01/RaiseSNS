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
