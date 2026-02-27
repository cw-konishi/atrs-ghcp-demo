# Copilot Instructions for ATRS Project

## プロジェクト概要

ATRS（Airline Ticket Reservation System）は、航空券予約システムのサンプルアプリケーションです。Spring Framework と PostgreSQL を使用した Maven マルチモジュールプロジェクトで、レイヤードアーキテクチャパターンで設計されています。

### 主要機能

- **会員管理**: 会員登録、会員情報更新、ログイン・ログアウト
- **航空券検索**: 路線・日付・搭乗クラスによる空席照会
- **航空券予約**: 予約登録、予約内容確認
- **予約履歴管理**: 予約履歴の照会・印刷

## プロジェクト構造

### モジュール構成

- **atrs-domain**: ドメイン層（ビジネスロジック、エンティティ、リポジトリ）
  - ビジネスロジックを実装するサービスクラス
  - データベースアクセスを行うリポジトリ（MyBatis Mapper）
  - ドメインモデル（エンティティ）
  - ドメイン共通処理
  
- **atrs-web**: プレゼンテーション層（コントローラー、ビュー、フォーム）
  - Spring MVC コントローラー
  - JSP ビューテンプレート
  - フォームクラス（入力値のバインディングとバリデーション）
  - ヘルパークラス
  
- **atrs-env**: 環境設定（インフラストラクチャ設定）
  - データソース設定
  - 環境別プロパティファイル
  - ログ設定（Logback）
  
- **atrs-initdb**: データベース初期化スクリプト
  - DDL スクリプト（テーブル作成）
  - DML スクリプト（初期データ投入）

### 画面機能一覧（パッケージ構成）

- **A0**: トップページ、API認証系
- **A1**: ログイン・ログアウト
- **B1**: 航空券検索
- **B2**: 航空券予約
- **C1**: 会員登録
- **C2**: 会員情報更新
- **D1**: 予約履歴照会・レポート出力

## 技術スタック

### バックエンド

- **Java**: JDK 8 以上
- **Spring Framework**: Spring MVC, Spring Security
- **MyBatis**: O/R マッパー（SQL マッパー）
- **Maven**: ビルド・依存関係管理
- **Logback**: ロギングフレームワーク
- **Bean Validation**: 入力バリデーション

### フロントエンド

- **JSP**: ビューテンプレート
- **Bootstrap**: CSS フレームワーク
- **jQuery**: JavaScript ライブラリ
- **Moment.js**: 日付操作ライブラリ
- **Parsley.js**: クライアントサイドバリデーション

### データベース・サーバー

- **PostgreSQL**: データベース
- **Tomcat 10**: サーブレットコンテナ

## アーキテクチャパターン

### レイヤードアーキテクチャ

```
プレゼンテーション層 (atrs-web)
    ↓
ドメイン層 (atrs-domain)
    ↓
データアクセス層 (atrs-domain - MyBatis Mapper)
    ↓
データベース (PostgreSQL)
```

### レイヤー責務

#### プレゼンテーション層（atrs-web）

- **Controller**: HTTP リクエストの受け取り、フォームのバインディング、サービスの呼び出し、ビューの選択
- **Form**: 入力データのバインディング、バリデーションルールの定義
- **View (JSP)**: データの表示、HTML レンダリング
- **Helper**: プレゼンテーション層の共通処理

#### ドメイン層（atrs-domain）

- **Service**: ビジネスロジックの実装、トランザクション境界
- **Entity**: ドメインモデル（会員、予約、フライトなど）
- **Repository (Mapper Interface)**: データアクセスインターフェース
- **共通処理**: ドメイン横断的な処理（マスタデータ提供など）

#### データアクセス層

- **MyBatis Mapper XML**: SQL 定義
- **Repository 実装**: MyBatis による自動実装

## コーディング規約

### パッケージ構造

- **基本パッケージ**: `jp.co.ntt.atrs.*`
- **ドメイン層サービス**: `jp.co.ntt.atrs.domain.service.{機能コード}`
  - 例: `jp.co.ntt.atrs.domain.service.b1` (航空券検索)
- **ドメイン層リポジトリ**: `jp.co.ntt.atrs.domain.repository.{エンティティ名}`
- **ドメインモデル**: `jp.co.ntt.atrs.domain.model`
- **Web層コントローラー**: `jp.co.ntt.atrs.app.{機能コード}`
- **Web層フォーム**: `jp.co.ntt.atrs.app.{機能コード}`

### 命名規則

#### クラス名

- **Controller**: `{機能名}Controller` (例: `MemberRegisterController`)
- **Service Interface**: `{機能名}Service` (例: `MemberRegisterService`)
- **Service Implementation**: `{機能名}ServiceImpl` (例: `MemberRegisterServiceImpl`)
- **Repository Interface**: `{エンティティ名}Repository` (例: `MemberRepository`)
- **Entity**: エンティティを表す名詞 (例: `Member`, `Reservation`)
- **Form**: `{機能名}Form` (例: `MemberRegisterForm`)

#### メソッド名

- 動詞で始める: `register()`, `find()`, `update()`, `delete()`
- boolean を返す場合: `is*()`, `has*()`, `can*()`
- 検索系: `find*()`, `search*()`, `get*()`

#### 定数

- `UPPER_SNAKE_CASE` で命名
- 例: `MAX_RETRY_COUNT`, `DEFAULT_PAGE_SIZE`

### アノテーション使用規則

#### Spring アノテーション

- **@Controller**: プレゼンテーション層のコントローラー
- **@Service**: ドメイン層のサービス実装
- **@Repository**: データアクセス層のリポジトリ（MyBatis では通常不要）
- **@Transactional**: トランザクション境界（主にサービスクラスのメソッド）
- **@Inject**: 依存性注入（フィールドインジェクション）

#### バリデーションアノテーション

- **@NotNull**: NULL チェック
- **@NotEmpty**: 空文字チェック
- **@Size**: サイズチェック
- **@Pattern**: 正規表現チェック
- **@Email**: メールアドレス形式チェック
- カスタムバリデーション: `@{検証名}` (例: `@MemberAvailable`)

### トランザクション管理

- サービス層のメソッドに `@Transactional` を付与
- 読み取り専用の場合: `@Transactional(readOnly = true)`
- 例外発生時は自動ロールバック（RuntimeException の場合）
- チェック例外でロールバックする場合: `@Transactional(rollbackFor = Exception.class)`

### エラーハンドリング

- ビジネスエラー: `BusinessException` をスローし、コントローラーでキャッチ
- システムエラー: 共通エラーハンドラーでハンドリング
- バリデーションエラー: Bean Validation の仕組みを利用
- メッセージは `i18n/atrs-messages_ja.properties` で管理

## 開発時の注意事項

### モジュール配置ルール

#### atrs-domain モジュール

- **新しいエンティティ**: `jp.co.ntt.atrs.domain.model` パッケージ
- **新しいリポジトリ**: `jp.co.ntt.atrs.domain.repository` パッケージ
- **新しいサービス**: `jp.co.ntt.atrs.domain.service.{機能コード}` パッケージ
- **MyBatis Mapper XML**: `src/main/resources/jp/co/ntt/atrs/domain/repository/{エンティティ名}` ディレクトリ

#### atrs-web モジュール

- **新しいコントローラー**: `jp.co.ntt.atrs.app.{機能コード}` パッケージ
- **新しいフォーム**: `jp.co.ntt.atrs.app.{機能コード}` パッケージ（コントローラーと同じパッケージ）
- **新しいJSPビュー**: `src/main/webapp/WEB-INF/views/{機能コード}/` ディレクトリ
- **JavaScript**: `src/main/webapp/resources/js/` ディレクトリ
- **CSS**: `src/main/webapp/resources/css/` ディレクトリ

#### atrs-initdb モジュール

- **DDLスクリプト**: `src/sqls/integration-test-postgres/00100_create_all_tables.sql` に追加
- **初期データ**: `src/sqls/integration-test-postgres/002*.sql` にテーブルごとにファイル分割

#### atrs-env モジュール

- **環境別設定**: `src/main/resources/META-INF/spring/atrs-infra.properties`
- **ログ設定**: `src/main/resources/logback.xml`

### プロパティファイル管理

#### メッセージプロパティ

- **バリデーションメッセージ**: `atrs-web/src/main/resources/ValidationMessages.properties`
  - Bean Validation のエラーメッセージ
  - キー形式: `{制約アノテーション名}.{フォームクラス名}.{フィールド名}`
  
- **画面表示メッセージ**: `atrs-web/src/main/resources/i18n/atrs-messages_ja.properties`
  - 業務エラーメッセージ
  - 画面表示用のラベル・メッセージ
  
- **フィールド名定義**: `atrs-web/src/main/resources/i18n/atrs-fields_ja.properties`
  - フォームフィールドの表示名

#### アプリケーション設定

- **ドメイン層設定**: `atrs-domain/src/main/resources/META-INF/spring/atrs.properties`
- **インフラ層設定**: `atrs-env/src/main/resources/META-INF/spring/atrs-infra.properties`

### 依存関係ルール

- プレゼンテーション層（atrs-web）→ ドメイン層（atrs-domain）の依存は可
- ドメイン層 → プレゼンテーション層の依存は禁止
- すべてのモジュール → 環境設定（atrs-env）の依存は可

### バリデーション実装

#### サーバーサイドバリデーション

1. フォームクラスにバリデーションアノテーションを付与
2. コントローラーメソッドの引数に `@Validated` を付与
3. `BindingResult` で検証結果を受け取る
4. エラーがある場合は入力画面に戻る

```java
@PostMapping("register")
public String register(@Validated MemberRegisterForm form, 
                      BindingResult result, Model model) {
    if (result.hasErrors()) {
        return "C1/memberRegisterForm";
    }
    // ... 処理
}
```

#### クライアントサイドバリデーション

- Parsley.js を使用
- `data-parsley-*` 属性でバリデーションルールを定義
- カスタムバリデータは `/resources/js/parsley-validator-*.js` に実装

## ビルドとテスト

### Maven コマンド

```bash
# プロジェクト全体のクリーンビルド
mvn clean install

# 特定モジュールのみビルド
cd atrs-web
mvn clean package

# テスト実行
mvn test

# テストをスキップしてビルド
mvn clean install -DskipTests

# 依存関係ツリーの表示
mvn dependency:tree

# 依存関係の更新
mvn clean install -U
```

### アプリケーション起動

```bash
# Tomcat での起動（開発用）
cd atrs-web
mvn tomcat:run

# ポート変更する場合
mvn tomcat:run -Dmaven.tomcat.port=8081

# デバッグモードで起動
mvnDebug tomcat:run
```

### テスト作成ガイドライン

#### 単体テスト

- JUnit を使用
- サービスクラスのテストを優先的に作成
- モックには Mockito を使用
- テストクラス名: `{対象クラス名}Test`
- テストメソッド名: `test{メソッド名}_{条件}_{期待結果}`

#### 統合テスト

- Spring Test と TestRestTemplate を使用
- データベースはテストデータを投入して実行
- トランザクションはテストごとにロールバック

## データベース

### データベース構成

- **DBMS**: PostgreSQL
- **データベース名**: `atrs`
- **スキーマ**: `public`（デフォルト）
- **文字エンコーディング**: UTF-8

### 主要テーブル

#### 会員関連

- **MEMBER**: 会員情報
  - 会員番号、氏名、生年月日、性別、連絡先、クレジットカード情報
- **MEMBER_LOGIN**: 会員ログイン情報
  - 会員番号、パスワード、ログイン日時、ログインフラグ

#### 予約関連

- **RESERVATION**: 予約情報
  - 予約番号、搭乗者情報、搭乗日、フライト情報
- **PASSENGER**: 搭乗者情報
  - 搭乗者氏名、年齢、性別

#### フライト関連

- **FLIGHT**: フライト運航情報
  - フライト番号、出発日、空席数、運賃
- **FLIGHT_MASTER**: フライトマスタ
  - 基本フライト情報、出発時刻、到着時刻
- **ROUTE**: 路線情報
  - 路線コード、出発空港、到着空港、基本運賃

#### マスタデータ

- **AIRPORT**: 空港マスタ
- **BOARDING_CLASS**: 搭乗クラスマスタ（普通席、特別席）
- **PEAK_TIME**: ピーク時期マスタ

### 初期化スクリプト実行順序

1. `00000_drop_all_tables.sql` - 既存テーブル削除
2. `00100_create_all_tables.sql` - テーブル作成
3. `00200_insert_fixed_value.sql` - 固定マスタデータ投入
4. `00210_insert_route.sql` - 路線データ投入
5. `00220_insert_flight_master.sql` - フライトマスタ投入
6. `00230_insert_member.sql` - 会員テストデータ投入
7. `00240_insert_peak_time.sql` - ピーク時期データ投入
8. `00250_insert_flight.sql` - フライト運航データ投入

### MyBatis Mapper 作成ガイドライン

#### Mapper Interface

```java
@Mapper
public interface MemberRepository {
    Member findByCustomerNo(String customerNo);
    void insert(Member member);
    void update(Member member);
    void delete(String customerNo);
}
```

#### Mapper XML

- `src/main/resources/jp/co/ntt/atrs/domain/repository/` に配置
- ファイル名: `{エンティティ名}Repository.xml`
- namespace: Mapper インターフェースの完全修飾名
- SQL ID: インターフェースのメソッド名と一致させる

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" 
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="jp.co.ntt.atrs.domain.repository.MemberRepository">
    <select id="findByCustomerNo" resultType="Member">
        SELECT * FROM MEMBER WHERE CUSTOMER_NO = #{customerNo}
    </select>
</mapper>
```

## セキュリティ

### Spring Security 設定

- **認証方式**: フォームベース認証
- **パスワードハッシュ**: BCrypt アルゴリズム
- **セッション管理**: サーバーサイドセッション
- **CSRF 対策**: Spring Security の CSRF トークン機能を使用
- **XSS 対策**: JSP の `<c:out>` タグでエスケープ

### 認証・認可フロー

1. ユーザーがログインフォームに会員番号とパスワードを入力
2. Spring Security が認証処理を実行
3. 認証成功時、セッションに認証情報を保存
4. 保護されたリソースへのアクセス時、認証済みかチェック
5. ログアウト時、セッションを破棄

### セキュアコーディング

- **機密情報のログ出力禁止**: パスワード、クレジットカード番号など
- **SQL インジェクション対策**: MyBatis のプレースホルダー（`#{}`）を使用
- **CSRF トークンの付与**: フォームに `<form:form>` タグを使用
- **権限チェック**: `@PreAuthorize` アノテーションまたは `<sec:authorize>` タグ

## コード生成時のベストプラクティス

### 1. 既存コードスタイルの遵守

- 既存のクラス構造、メソッドシグネチャを参考にする
- インデントはスペース4つ
- 行の最大長は120文字を推奨

### 2. ログ出力の実装

```java
// クラスレベルでロガーを定義
private static final Logger logger = LoggerFactory.getLogger(ClassName.class);

// 適切なレベルでログ出力
logger.debug("Debug message: {}", variable);
logger.info("Info message: {}", variable);
logger.warn("Warning message: {}", variable);
logger.error("Error occurred", exception);
```

### 3. 例外処理

- **ビジネス例外**: `BusinessException` を継承した独自例外クラスを作成
- **システム例外**: `SystemException` としてラップ
- **例外の再スロー**: catch したらログを出力してから再スロー
- **リソースのクローズ**: try-with-resources を使用

```java
try {
    // ビジネスロジック
} catch (BusinessException e) {
    logger.warn("Business error occurred: {}", e.getMessage());
    throw e;
} catch (Exception e) {
    logger.error("System error occurred", e);
    throw new SystemException("システムエラーが発生しました", e);
}
```

### 4. テストコードの作成

- すべてのサービスクラスに対応する単体テストを作成
- 正常系と異常系の両方をテスト
- テストメソッドには日本語の説明コメントを付与

```java
@Test
public void testRegisterMember_正常系_会員登録が成功する() {
    // テストコード
}
```

### 5. JavaDoc コメント

- public メソッドには必ず JavaDoc を記述
- パラメータ、戻り値、例外を明記

```java
/**
 * 会員情報を登録する。
 * 
 * @param member 登録する会員情報
 * @return 登録された会員情報
 * @throws BusinessException 会員番号が既に存在する場合
 */
public Member registerMember(Member member) throws BusinessException {
    // 実装
}
```

### 6. NULL チェック

- メソッドの引数は必ず NULL チェックを行う
- Spring の `Assert` クラスを活用

```java
Assert.notNull(member, "member must not be null");
Assert.hasText(customerNo, "customerNo must not be empty");
```

### 7. 定数の使用

- マジックナンバー、マジックストリングは定数化
- 定数クラスまたはEnum を使用

```java
public class Constants {
    public static final int MAX_PASSENGER_COUNT = 10;
    public static final String DATE_FORMAT = "yyyy-MM-dd";
}
```

### 8. トランザクション境界

- サービス層のpublicメソッドにトランザクションを設定
- 読み取り専用の場合は `readOnly = true` を指定
- 複数のリポジトリを呼び出す場合は、サービス層で統括

## 新規機能追加の手順

### 1. 要件定義

- 機能コード（例: E1）を決定
- 画面遷移図を作成
- データモデルを設計

### 2. データベース設計

1. ER図を作成
2. DDLスクリプトを `atrs-initdb/src/sqls/` に追加
3. テストデータのDMLスクリプトを作成

### 3. ドメイン層の実装

1. エンティティクラスを作成（`domain.model`）
2. リポジトリインターフェースを作成（`domain.repository`）
3. MyBatis Mapper XML を作成
4. サービスインターフェースを作成（`domain.service.{機能コード}`）
5. サービス実装クラスを作成
6. 単体テストを作成

### 4. プレゼンテーション層の実装

1. フォームクラスを作成（`app.{機能コード}`）
2. コントローラークラスを作成
3. JSPビューを作成（`/WEB-INF/views/{機能コード}/`）
4. JavaScript（必要に応じて）
5. CSS（必要に応じて）

### 5. メッセージ定義

1. バリデーションメッセージを `ValidationMessages.properties` に追加
2. 画面表示メッセージを `atrs-messages_ja.properties` に追加
3. フィールド名を `atrs-fields_ja.properties` に追加

### 6. テスト・デバッグ

1. 単体テストを実行
2. アプリケーションを起動して動作確認
3. 統合テストを実施

## トラブルシューティング

### よくある問題と解決方法

#### ビルドエラー

- **問題**: 依存関係が解決できない
- **解決**: `mvn clean install -U` で依存関係を更新

#### データベース接続エラー

- **問題**: PostgreSQL に接続できない
- **確認事項**:
  - PostgreSQL が起動しているか
  - `atrs-infra.properties` のDB接続情報が正しいか
  - データベース `atrs` が作成されているか

#### Spring Security による403エラー

- **問題**: フォーム送信時に403 Forbidden
- **解決**: フォームに CSRF トークンが含まれているか確認（`<form:form>` タグを使用）

#### MyBatis の Mapper が見つからない

- **問題**: `org.apache.ibatis.binding.BindingException`
- **確認事項**:
  - Mapper XML の namespace が正しいか
  - Mapper XML が正しい場所に配置されているか
  - `@Mapper` アノテーションが付与されているか

## 参考情報

### コードレビューチェックリスト

- [ ] 命名規則に従っているか
- [ ] レイヤー間の依存関係が適切か
- [ ] トランザクション境界が適切に設定されているか
- [ ] NULL チェックが実装されているか
- [ ] 例外処理が適切に実装されているか
- [ ] ログ出力が適切なレベルで実装されているか
- [ ] JavaDoc コメントが記述されているか
- [ ] テストコードが作成されているか
- [ ] 機密情報がログ出力されていないか
- [ ] SQL インジェクション対策がされているか

### デバッグのヒント

- **ブレークポイント設定推奨箇所**:
  - コントローラーのメソッド入口
  - サービスのメソッド入口
  - 例外ハンドラー
  
- **ログレベルの調整**: `logback.xml` で特定パッケージのログレベルを DEBUG に変更

```xml
<logger name="jp.co.ntt.atrs" level="DEBUG" />
```

## 開発環境セットアップ

### 必要なソフトウェア

1. **JDK**: OpenJDK 8 以上または Oracle JDK 8 以上
2. **Maven**: 3.6 以上
3. **PostgreSQL**: 12 以上
4. **IDE**: Eclipse, IntelliJ IDEA, VS Code（Java Extension Pack）

### セットアップ手順

#### 1. リポジトリのクローン

```bash
git clone <repository-url>
cd atrs-ghcp-demo
```

#### 2. データベースの作成

```sql
-- PostgreSQL にログイン
psql -U postgres

-- データベース作成
CREATE DATABASE atrs ENCODING 'UTF8';

-- ユーザー作成（必要に応じて）
CREATE USER atrs_user WITH PASSWORD 'password';
GRANT ALL PRIVILEGES ON DATABASE atrs TO atrs_user;
```

#### 3. 初期データの投入

```bash
cd atrs-initdb/src/sqls/integration-test-postgres
psql -U postgres -d atrs -f 00100_create_all_tables.sql
psql -U postgres -d atrs -f 00200_insert_fixed_value.sql
psql -U postgres -d atrs -f 00210_insert_route.sql
psql -U postgres -d atrs -f 00220_insert_flight_master.sql
psql -U postgres -d atrs -f 00230_insert_member.sql
psql -U postgres -d atrs -f 00240_insert_peak_time.sql
psql -U postgres -d atrs -f 00250_insert_flight.sql
```

#### 4. プロジェクトのビルド

```bash
cd <project-root>
mvn clean install
```

#### 5. アプリケーションの起動

```bash
cd atrs-web
mvn tomcat7:run
```

ブラウザで `http://localhost:8080/atrs` にアクセス

### IDE 設定

#### Eclipse

- **Maven プロジェクトとしてインポート**: File > Import > Existing Maven Projects
- **Tomcat サーバー構成**: Servers ビューから Tomcat 10 を追加
- **フォーマッター設定**: Java 標準コーディング規約に従う

#### IntelliJ IDEA

- **Maven プロジェクトとして開く**: Open > pom.xml を選択
- **Run Configuration**: Maven goal として `tomcat7:run` を設定
- **Code Style**: Java 標準に準拠

#### VS Code

- **拡張機能**:
  - Java Extension Pack
  - Spring Boot Extension Pack
  - Maven for Java
- **tasks.json** でMavenタスクを設定

## よく使うコマンド集

### Maven

```bash
# 全モジュールのコンパイル
mvn compile

# 特定モジュールのみコンパイル
mvn compile -pl atrs-web

# クリーン
mvn clean

# パッケージング（JAR/WAR作成）
mvn package

# インストール（ローカルリポジトリに配置）
mvn install

# 依存関係の解析
mvn dependency:analyze

# 有効なプロファイル確認
mvn help:active-profiles

# プロパティ一覧表示
mvn help:effective-pom
```

### Git

```bash
# ブランチ作成
git checkout -b feature/new-feature

# 変更のコミット
git add .
git commit -m "Add new feature"

# リモートへプッシュ
git push origin feature/new-feature

# マージ
git checkout main
git merge feature/new-feature
```

### PostgreSQL

```bash
# データベース接続
psql -U postgres -d atrs

# テーブル一覧表示
\dt

# テーブル構造表示
\d table_name

# SQL実行
\i script.sql

# クエリ実行結果を CSV に出力
\copy (SELECT * FROM member) TO 'output.csv' CSV HEADER;
```

## パフォーマンス最適化

### データベース最適化

- **インデックスの適切な設定**: 検索条件に使用するカラムにインデックスを作成
- **N+1問題の回避**: MyBatis の association/collection を活用してJOINで取得
- **コネクションプーリング**: HikariCP の設定を最適化

### アプリケーション最適化

- **キャッシュの活用**: マスタデータは起動時にメモリにキャッシュ
- **遅延ロード**: 大量データは必要になるまでロードしない
- **ページング**: 検索結果は適切にページング処理

### 設定例（atrs-infra.properties）

```properties
# コネクションプール設定
database.maximumPoolSize=20
database.minimumIdle=5
database.connectionTimeout=30000

# MyBatis 設定
mybatis.configuration.defaultFetchSize=100
mybatis.configuration.defaultStatementTimeout=30
```

## 用語集

- **ATRS**: Airline Ticket Reservation System（航空券予約システム）
- **DTO**: Data Transfer Object（データ転送オブジェクト）
- **DAO**: Data Access Object（データアクセスオブジェクト）
- **POJO**: Plain Old Java Object（単純なJavaオブジェクト）
- **DI**: Dependency Injection（依存性注入）
- **AOP**: Aspect Oriented Programming（アスペクト指向プログラミング）
- **ORM**: Object-Relational Mapping（オブジェクト関係マッピング）
- **CSRF**: Cross-Site Request Forgery（クロスサイトリクエストフォージェリ）
- **XSS**: Cross-Site Scripting（クロスサイトスクリプティング）

## 関連ドキュメント

- **Spring Framework**: https://spring.io/projects/spring-framework
- **Spring Security**: https://spring.io/projects/spring-security
- **MyBatis**: https://mybatis.org/mybatis-3/
- **Bean Validation**: https://beanvalidation.org/
- **Bootstrap**: https://getbootstrap.com/
- **Parsley.js**: https://parsleyjs.org/

---

**最終更新日**: 2026年2月27日  
**バージョン**: 1.0  
**プロジェクト**: ATRS (Airline Ticket Reservation System)
