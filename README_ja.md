# 航空券予約システム (ATRS)

ATRSは、Macchinetta Server Framework(TERASOLUNA Server Framework)とMacchinetta Client Librariesを使用したアプリケーション開発を学ぶための、リファレンス実装アプリケーションです。

## 📋 目次

- [プロジェクト概要](#プロジェクト概要)
- [技術スタック](#技術スタック)
- [前提条件](#前提条件)
- [セットアップ手順](#セットアップ手順)
- [プロジェクト構成](#プロジェクト構成)
- [トラブルシューティング](#トラブルシューティング)
- [ライセンス](#ライセンス)

## プロジェクト概要

ATRSは、航空券の検索・予約機能を持つWebアプリケーションです。以下の機能を提供します:

- ✈️ フライト検索
- 🎫 チケット予約
- 👤 会員登録・更新
- 📊 予約履歴レポート
- 🔐 認証・認可
- 🌐 REST API

## 技術スタック

### フレームワーク・ライブラリ
- **TERASOLUNA Server Framework**: 5.10.0.RELEASE
- **Spring Framework**: 6.2.1
- **Spring Security**: 6.4.1
- **MyBatis**: 3.5.16
- **MapStruct**: 1.5.5.Final *(注: 1.6.3にはバグがあるため1.5.5を使用)*

### インフラストラクチャ
- **Java**: 17以上
- **Maven**: 3.6以上
- **PostgreSQL**: 15以上推奨
- **Apache Tomcat**: 10.1.x (Cargo Mavenプラグインで自動起動)
- **Apache ActiveMQ Artemis**: 2.39.0 (組み込みメッセージブローカー)

### アプリケーションサーバー
- **Servlet**: Jakarta Servlet 6.0
- **JSP**: Jakarta Server Pages 3.1
- **JSTL**: Jakarta Standard Tag Library 3.0

## 前提条件

以下のソフトウェアが必要です:

1. **JDK 17以上**
   ```powershell
   java -version
   # java version "17.0.1" 以上が必要
   ```

2. **Apache Maven 3.6以上**
   ```powershell
   mvn -version
   # Apache Maven 3.6.0 以上が必要
   ```

3. **PostgreSQL 15以上** (インストールと起動)
   - デフォルトユーザー: `postgres`
   - デフォルトパスワード: `postgres`
   - デフォルトポート: `5432`

## セットアップ手順

### 1. ソースコードの取得

GitHubからクローンするか、[リリースページ](https://github.com/Macchinetta/atrs/tags)からダウンロードしてください。

```powershell
git clone https://github.com/Macchinetta/atrs.git
cd atrs
```

### 2. PostgreSQLのインストールと起動

PostgreSQLをインストールし、サービスを起動してください。

**重要**: デフォルトでは以下の設定を想定しています:
- ユーザー名: `postgres`
- パスワード: `postgres`
- ホスト: `localhost`
- ポート: `5432`

別のパスワードを使用する場合は、以下のファイルを編集してください:
- `atrs-env/src/main/resources/META-INF/spring/atrs-infra.properties`

### 3. データベースの作成

PostgreSQLに接続し、`atrs`データベースを作成します:

```sql
CREATE DATABASE atrs;
```

または、psqlコマンドラインから:

```powershell
psql -U postgres
# パスワード入力後
CREATE DATABASE atrs;
\q
```

### 4. テストデータの投入

プロジェクトのルートディレクトリで以下のコマンドを実行します:

```powershell
mvn sql:execute -f atrs-initdb/pom.xml
```

このコマンドにより、以下のデータが投入されます:
- 空港マスタ (20空港)
- 路線マスタ (20路線)
- フライトマスタ (126便)
- 会員情報
- ピーク時期設定 (16期間)
- 搭乗クラス、運賃タイプなど

**重要**: フライトデータは**実行日から120日間**のデータが生成されます。
- 例: 2025年11月27日実行 → 2025年11月27日〜2026年3月27日のフライトが投入されます
- 過去のデータしかない場合は、このコマンドを再実行して最新データに更新してください

### 5. アプリケーションのビルドと起動

#### 方法1: 一括ビルド後に起動 (推奨)

```powershell
# 全モジュールをビルド
mvn clean install -P default

# Webアプリケーションを起動
mvn cargo:run -P default -f atrs-web/pom.xml
```

#### 方法2: ワンコマンドでビルドと起動

```powershell
cd atrs-web
mvn clean package cargo:run -DskipTests
```

起動には約15〜20秒かかります。以下のメッセージが表示されたら起動完了です:

```
[INFO] Tomcat 10.1.34 started on port [8080]
[INFO] Press Ctrl-C to stop the container...
```

### 6. Webアクセス

ブラウザで以下のURLにアクセスしてください:

**メインページ**: <http://localhost:8080/atrs/>

**REST API**: <http://localhost:8080/atrs/api/v1/>

### 7. アプリケーションの停止

ターミナルで `Ctrl+C` を押してTomcatを停止します。

## プロジェクト構成

このプロジェクトはMavenマルチモジュール構成です:

```
atrs/
├── atrs-domain/          # ドメイン層 (ビジネスロジック、リポジトリ)
├── atrs-env/             # 環境設定 (データソース、トランザクション)
├── atrs-initdb/          # データベース初期化SQLスクリプト
├── atrs-web/             # プレゼンテーション層 (Controller, View, REST API)
├── pom.xml               # 親POM
└── README.md             # このファイル
```

### モジュール詳細

#### atrs-domain
- エンティティ、DTO、リポジトリ
- ビジネスロジック (Service層)
- MyBatisマッパーXML

#### atrs-env
- データソース設定
- トランザクション管理
- ログ設定 (Logback)

#### atrs-initdb
- データベーススキーマ定義 (DDL)
- 初期データ投入 (DML)
- PostgreSQL用SQLスクリプト

#### atrs-web
- Spring MVC Controller
- JSP/JSTLビュー
- REST APIコントローラー
- MapStructマッパー (DTO変換)
- フォームバリデーション
- 静的リソース (CSS, JavaScript)

## デフォルトユーザー

テストデータ投入後、以下のユーザーでログインできます:

| ユーザーID | パスワード | 役割 |
|-----------|---------|------|
| 0000000001 | (設定されたパスワード) | 一般会員 |

詳細は `atrs-initdb/src/sqls/integration-test-postgres/00230_insert_member.sql` を参照してください。

## 動作確認用のフライト検索

### 検索可能なフライト日付範囲

データベース初期化後、**実行日から120日間**のフライトデータが利用可能です。

```powershell
# フライトの日付範囲を確認
psql -U postgres -d atrs -c "SELECT MIN(departure_date) as 最初のフライト, MAX(departure_date) as 最後のフライト, COUNT(*) as 総フライト数 FROM flight;"
```

### 利用可能な主要空港

| 空港コード | 空港名 |
|-----------|--------|
| HND | 東京(羽田) |
| ITM | 大阪(伊丹) |
| KIX | 大阪(関西) |
| FUK | 福岡 |
| HKD | 函館 |

全20空港の一覧は以下のコマンドで確認できます:

```powershell
psql -U postgres -d atrs -c "SELECT airport_cd, airport_name FROM airport ORDER BY airport_cd;"
```

### フライト検索の例

#### Web UI
<http://localhost:8080/atrs/> にアクセスし、以下の条件で検索:

- **出発空港**: HND (羽田)
- **到着空港**: ITM (伊丹)
- **搭乗日**: 今日から120日以内の任意の日付
- **フライトタイプ**: 往復(RT) または 片道(OW)

#### REST API
```bash
curl "http://localhost:8080/atrs/api/v1/flight?depAirportCd=HND&arrAirportCd=ITM&depDate=2025-11-27&flightType=RT"
```

### フライトデータの再投入

検索しても結果が出ない場合、フライトデータが古い可能性があります。以下のコマンドで最新データに更新してください:

```powershell
mvn sql:execute -f atrs-initdb/pom.xml
```

## トラブルシューティング

### ポート8080が既に使用されている

別のアプリケーションがポート8080を使用している場合、以下のファイルでポートを変更できます:

`atrs-web/pom.xml`:
```xml
<cargo.servlet.port>8080</cargo.servlet.port>
<!-- 例: 8081に変更 -->
```

### データベース接続エラー

以下を確認してください:

1. PostgreSQLサービスが起動しているか
2. `atrs`データベースが作成されているか
3. `atrs-env/src/main/resources/META-INF/spring/atrs-infra.properties`の接続設定が正しいか

### フライト検索で結果が出ない

以下を確認してください:

1. **フライトデータの日付範囲を確認**:
   ```powershell
   psql -U postgres -d atrs -c "SELECT MIN(departure_date), MAX(departure_date) FROM flight;"
   ```

2. **データが古い場合は再投入**:
   ```powershell
   mvn sql:execute -f atrs-initdb/pom.xml
   ```

3. **空港コードが正しいか確認** (例: HND, ITM など3文字コード)

4. **検索日付が有効範囲内か確認** (実行日から120日以内)

### MapStructの生成エラー

このプロジェクトでは**MapStruct 1.5.5.Final**を使用しています。
1.6.3にはバグがあり、同一パッケージ内のクラス参照で問題が発生します。

`pom.xml`で以下のようにバージョンが固定されていることを確認してください:

```xml
<properties>
    <mapstruct.version>1.5.5.Final</mapstruct.version>
</properties>
```

### テストスキップ

ビルド時間を短縮したい場合は、テストをスキップできます:

```powershell
mvn clean package -DskipTests
```

### クリーンビルド

問題が発生した場合は、クリーンビルドを試してください:

```powershell
mvn clean install -P default
```

## 開発環境

### IDEでの開発

このプロジェクトは以下のIDEで開発できます:

- **Eclipse**: Spring Tool Suite (STS) 推奨
- **IntelliJ IDEA**: Ultimate Edition推奨
- **Visual Studio Code**: Java Extension Pack + Spring Boot Extension Pack

### ホットリロード

開発中にJavaコードを変更した場合は、再ビルドが必要です:

```powershell
# atrs-web モジュールのみ再ビルド
mvn clean package -pl atrs-web -am -DskipTests
```

JSP/CSS/JavaScriptの変更は、ブラウザをリロードするだけで反映されます。

## REST APIの使用例

### フライト検索API

```bash
curl "http://localhost:8080/atrs/api/v1/flight?depAirportCd=HND&arrAirportCd=ITM&depDate=2025-12-01&flightType=RT"
```

### チケット予約API

```bash
curl -X POST http://localhost:8080/atrs/api/v1/ticket \
  -H "Content-Type: application/json" \
  -d '{
    "flightId": "1",
    "passengers": [...]
  }'
```

詳細なAPI仕様は、Swagger/OpenAPIドキュメント(準備中)を参照してください。

## ライセンス

このプロジェクトは[Apache License 2.0](LICENSE.txt)の下でライセンスされています。

## 開発者

- **Macchinetta Development Team**
  - Organization: [Macchinetta](http://macchinetta.github.io)
  - Repository: [GitHub](https://github.com/Macchinetta/atrs)

## 参考資料

- [TERASOLUNA Server Framework ガイドライン](https://terasoluna-batch.github.io/guideline/)
- [Macchinetta公式サイト](http://macchinetta.github.io)
- [Spring Framework Documentation](https://spring.io/projects/spring-framework)

## サポート

問題が発生した場合は、[GitHubのIssue](https://github.com/Macchinetta/atrs/issues)を作成してください。

---

**作成日**: 2025年11月27日  
**バージョン**: 1.11.0.RELEASE  
**フレームワーク**: TERASOLUNA Server Framework 5.10.0.RELEASE
