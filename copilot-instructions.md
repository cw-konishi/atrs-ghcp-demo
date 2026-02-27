# Copilot Instructions for ATRS Project

## プロジェクト概要

ATRS（Airline Ticket Reservation System）は、航空券予約システムのサンプルアプリケーションです。Spring Framework と PostgreSQL を使用した Maven マルチモジュールプロジェクトです。

## プロジェクト構造

- **atrs-domain**: ドメイン層（ビジネスロジック、エンティティ、リポジトリ）
- **atrs-web**: プレゼンテーション層（コントローラー、ビュー、フォーム）
- **atrs-env**: 環境設定（インフラストラクチャ設定）
- **atrs-initdb**: データベース初期化スクリプト

## 技術スタック

- Java
- Spring Framework (Spring MVC, Spring Security)
- Maven
- PostgreSQL
- Tomcat 10
- Bootstrap (フロントエンド)
- jQuery

## コーディング規約

1. **パッケージ構造**: `jp.co.ntt.atrs.*` を基本パッケージとする
2. **命名規則**: Javaの標準命名規則に従う
   - クラス名: PascalCase
   - メソッド/変数名: camelCase
   - 定数: UPPER_SNAKE_CASE
3. **レイヤー分離**: ドメイン層とプレゼンテーション層を明確に分離
4. **国際化**: メッセージは `i18n/*.properties` ファイルで管理

## 開発時の注意事項

- 新しいエンティティやリポジトリは `atrs-domain` モジュールに配置
- コントローラーやフォームは `atrs-web` モジュールに配置
- データベーススキーマの変更は `atrs-initdb/src/sqls/` に SQL スクリプトを追加
- 環境固有の設定は `atrs-env` モジュールで管理
- Validation メッセージは `ValidationMessages.properties` に定義
- 画面表示用メッセージは `i18n/atrs-messages_ja.properties` に定義

## ビルドとテスト

```bash
# プロジェクト全体のビルド
mvn clean install

# テスト実行
mvn test

# Web アプリケーションの起動
cd atrs-web
mvn tomcat:run
```

## データベース

- DBMS: PostgreSQL
- 初期化スクリプト: `atrs-initdb/src/sqls/integration-test-postgres/`
- スキーマ作成: `00100_create_all_tables.sql`
- 初期データ投入: `00200_*.sql` ファイル群

## セキュリティ

- Spring Security を使用した認証・認可
- フォームベース認証
- CSRF 保護を有効化

## コード生成時のベストプラクティス

1. 既存のコードスタイルに合わせる
2. ログ出力は適切なレベル（DEBUG, INFO, WARN, ERROR）で実装
3. 例外処理を適切に行う
4. テストコードも併せて作成する
5. JavaDoc コメントを適切に記述する
