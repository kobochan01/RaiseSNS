# RaiseSNS

## 環境・起動

```powershell
# 起動（バックエンド: backend/ ディレクトリで実行）
.\gradlew.bat bootRun

# 起動（フロントエンド: frontend/ ディレクトリで実行）
npm run dev

# 終了
# 各ターミナルで Ctrl+C

# 再起動
# 終了後に上記起動コマンドを再実行
```

## 技術スタック固有のルール

- バックエンド: Java / Spring Boot（Gradle）
  - DB更新を伴うServiceメソッドには `@Transactional` を付与する
  - Controller に業務ロジックを書かず、Service 層に集約する
  - パスワードは必ずハッシュ化（BCrypt等）して保存し、平文をログに出力しない
- フロントエンド: React（Vite, TypeScript）
  - API呼び出しはコンポーネント内に直書きせず、API クライアント層にまとめる
  - コンポーネントの状態管理は必要最小限に留め、過度なグローバル状態を避ける

## GitHub 設定

- ラベル: `bug` / `enhancement` / `documentation` / `chore`
- PR テンプレート: `.github/PULL_REQUEST_TEMPLATE.md`

## ドキュメント更新対象

<!-- コード変更時に必ず確認・更新する .md ファイルを列挙してください -->
- README.md
- DEVELOPMENT.md
- docs/requirements.md
- docs/design.md
