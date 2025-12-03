# ATRS プロジェクト - AI コーディングエージェント向けガイド

## プロジェクト概要

航空券予約システム (ATRS) - TERASOLUNA Server Framework 5.10.0 と Spring Framework 6.2.1 を使用したマルチモジュール Maven プロジェクト。PostgreSQL データベースと Apache ActiveMQ Artemis メッセージングを統合した、エンタープライズ Java Web アプリケーションのリファレンス実装。

## アーキテクチャ: 4 層構造のマルチモジュール

### モジュール構成とディレクトリ構造

```
atrs/                                    # 親 POM (TERASOLUNA 5.10.0)
├── pom.xml                              # 親POM: 共通依存関係・プラグイン管理
├── atrs-env/                            # 環境設定層 - データソース、JMS、ログ設定
│   ├── pom.xml                          # 環境設定依存関係
│   └── src/main/
│       ├── java/jp/co/ntt/atrs/domain/
│       │   └── common/
│       │       └── logging/             # ログ出力設定
│       │           ├── LogMessages.java
│       │           └── MessageFormatter.java
│       └── resources/
│           ├── logback.xml              # Logback設定 (ログレベル、出力先)
│           └── META-INF/spring/
│               ├── atrs-env.xml         # Spring設定: データソース、トランザクション
│               ├── atrs-infra.properties # DB接続情報 (URL, user, password)
│               └── atrs-codelist.xml    # コードリスト定義 (空港、運賃種別など)
│
├── atrs-domain/                         # ドメイン層 - ビジネスロジック、リポジトリ
│   ├── pom.xml                          # ドメイン依存関係 (MyBatis, JMS, テスト)
│   └── src/
│       ├── main/
│       │   ├── java/jp/co/ntt/atrs/domain/
│       │   │   ├── common/              # 共通機能
│       │   │   │   ├── codelist/        # コードリスト (JdbcCodeList)
│       │   │   │   ├── exception/       # 例外クラス (AtrsBusinessException)
│       │   │   │   ├── masterdata/      # マスタデータプロバイダー
│       │   │   │   │   ├── RouteProvider.java
│       │   │   │   │   ├── FlightMasterProvider.java
│       │   │   │   │   ├── FareTypeProvider.java
│       │   │   │   │   └── BoardingClassProvider.java
│       │   │   │   ├── message/         # メッセージ定義 (エラーコードenum)
│       │   │   │   ├── security/        # セキュリティ (UserDetails実装)
│       │   │   │   ├── util/            # ユーティリティ (DateTimeUtil, StringUtil)
│       │   │   │   └── validate/        # カスタムバリデーター
│       │   │   ├── model/               # ドメインモデル (エンティティ)
│       │   │   │   ├── Flight.java      # フライト情報
│       │   │   │   ├── FlightMaster.java # フライトマスタ
│       │   │   │   ├── Route.java       # 路線
│       │   │   │   ├── Airport.java     # 空港
│       │   │   │   ├── Member.java      # 会員
│       │   │   │   ├── Reservation.java # 予約
│       │   │   │   ├── BoardingClass.java # 搭乗クラス (N:普通席, S:特別席)
│       │   │   │   ├── FareType.java    # 運賃種別 (OW:片道, RT:往復)
│       │   │   │   └── *Cd.java         # Enum型コード (BoardingClassCd, FareTypeCd)
│       │   │   ├── repository/          # MyBatis リポジトリ
│       │   │   │   ├── flight/
│       │   │   │   │   └── FlightRepository.java # フライト検索リポジトリ
│       │   │   │   ├── member/
│       │   │   │   │   └── MemberRepository.java
│       │   │   │   └── reservation/
│       │   │   │       ├── ReservationRepository.java
│       │   │   │       └── ReserveFlightRepository.java
│       │   │   └── service/             # ビジネスロジック (機能別パッケージ)
│       │   │       ├── a0/              # 会員共通サービス
│       │   │       │   ├── MembershipSharedService.java
│       │   │       │   └── MembershipSharedServiceImpl.java
│       │   │       ├── a1/              # 認証ログインサービス
│       │   │       │   ├── AuthLoginService.java
│       │   │       │   └── AuthLoginServiceImpl.java
│       │   │       ├── b0/              # チケット共通サービス
│       │   │       │   ├── TicketSharedService.java        # 共通処理インターフェース
│       │   │       │   ├── TicketSharedServiceImpl.java    # 実装 (運賃計算、USD変換)
│       │   │       │   └── InvalidFlightException.java     # フライト不正例外
│       │   │       ├── b1/              # 空席照会サービス
│       │   │       │   ├── TicketSearchService.java
│       │   │       │   ├── TicketSearchServiceImpl.java
│       │   │       │   ├── TicketSearchCriteriaDto.java # 検索条件DTO
│       │   │       │   ├── FlightVacantInfoDto.java     # 検索結果DTO
│       │   │       │   ├── FareTypeVacantInfoDto.java   # 運賃種別情報DTO (fareUsdフィールド含む)
│       │   │       │   └── FlightNotFoundException.java # フライト検索例外
│       │   │       ├── b2/              # チケット予約サービス
│       │   │       │   ├── TicketReserveService.java
│       │   │       │   ├── TicketReserveServiceImpl.java
│       │   │       │   └── TicketReserveDto.java
│       │   │       ├── c1/              # 会員登録サービス
│       │   │       │   ├── MemberRegisterService.java
│       │   │       │   └── MemberRegisterServiceImpl.java
│       │   │       └── d1/              # 予約履歴レポートサービス
│       │   │           ├── ReservationHistoryReportService.java
│       │   │           └── ReservationHistoryReportServiceImpl.java
│       │   └── resources/
│       │       ├── META-INF/spring/
│       │       │   └── atrs.properties  # ドメイン層プロパティ
│       │       └── jp/co/ntt/atrs/domain/repository/
│       │           ├── flight/
│       │           │   └── FlightRepository.xml # MyBatis SQLマッパー
│       │           ├── member/
│       │           │   └── MemberRepository.xml
│       │           └── reservation/
│       │               ├── ReservationRepository.xml
│       │               └── ReserveFlightRepository.xml
│       └── test/
│           └── java/jp/co/ntt/atrs/domain/
│               └── service/
│                   └── b1/
│                       └── TicketSearchServiceImplTest.java # ✅ 単体テスト (14ケース、93%カバレッジ)
│
├── atrs-web/                            # プレゼンテーション層 - MVC + REST API
│   ├── pom.xml                          # Web依存関係 (Spring MVC, Security, MapStruct)
│   └── src/main/
│       ├── java/jp/co/ntt/atrs/
│       │   ├── app/                     # Spring MVC コントローラー (JSPビュー)
│       │   │   ├── a0/                  # 共通機能
│       │   │   │   └── ErrorResultDto.java      # エラーレスポンス用DTO
│       │   │   ├── a1/                  # ログイン機能
│       │   │   │   ├── AuthLoginController.java
│       │   │   │   └── LoginForm.java
│       │   │   ├── b1/                  # チケット検索機能
│       │   │   │   ├── TicketSearchController.java
│       │   │   │   ├── TicketSearchHelper.java   # ビジネスロジック補助
│       │   │   │   ├── TicketSearchForm.java     # フォームオブジェクト
│       │   │   │   ├── FlightsApiController.java # REST API (GET /api/flights)
│       │   │   │   ├── FlightSearchCriteriaForm.java # REST API用フォーム
│       │   │   │   ├── FlightSearchCriteriaValidator.java # バリデーター
│       │   │   │   └── B1Mapper.java             # MapStruct DTO変換
│       │   │   ├── b2/                  # チケット予約機能
│       │   │   │   ├── TicketReserveController.java
│       │   │   │   └── TicketReserveForm.java
│       │   │   ├── c1/                  # 会員登録機能
│       │   │   │   ├── MemberRegisterController.java
│       │   │   │   └── MemberRegisterForm.java
│       │   │   └── c2/                  # 会員情報更新機能
│       │   │       ├── MemberUpdateController.java
│       │   │       └── MemberUpdateForm.java
│       │   ├── api/                     # REST API コントローラー (@RestController)
│       │   │   ├── flight/              # フライト検索API
│       │   │   │   ├── FlightRestController.java
│       │   │   │   ├── FlightMapper.java         # MapStruct変換
│       │   │   │   ├── FlightResource.java       # レスポンスDTO
│       │   │   │   └── FlightSearchQuery.java    # リクエストDTO
│       │   │   └── ticket/              # チケット予約API
│       │   │       ├── TicketRestController.java
│       │   │       ├── TicketMapper.java
│       │   │       └── TicketResource.java
│       │   ├── config/                  # Spring設定クラス
│       │   │   ├── app/
│       │   │   │   ├── ApplicationContextConfig.java   # アプリケーション全体設定
│       │   │   │   └── AtrsInfrastructureConfig.java   # インフラ設定 (JMS等)
│       │   │   └── web/
│       │   │       ├── SpringMvcConfig.java            # Spring MVC設定
│       │   │       └── SpringSecurityConfig.java       # Spring Security設定
│       │   └── listener/
│       │       └── SetupListener.java   # アプリケーション起動時処理
│       ├── resources/
│       │   ├── ValidationMessages.properties # Bean Validationメッセージ
│       │   └── i18n/
│       │       ├── atrs-messages_ja.properties # 画面メッセージ (日本語)
│       │       └── atrs-fields_ja.properties   # フィールド名定義 (日本語)
│       └── webapp/
│           ├── resources/               # 静的リソース
│           │   ├── css/                 # スタイルシート
│           │   ├── js/                  # JavaScript
│           │   ├── img/                 # 画像
│           │   └── vendor/              # サードパーティライブラリ (Bootstrap等)
│           └── WEB-INF/
│               ├── views/               # JSPビュー
│               │   ├── a1/              # ログイン画面
│               │   ├── b1/              # チケット検索画面
│               │   ├── b2/              # チケット予約画面
│               │   ├── c1/              # 会員登録画面
│               │   ├── c2/              # 会員情報更新画面
│               │   └── common/          # 共通JSP (ヘッダー、フッター、エラーページ)
│               └── web.xml              # サーブレット設定、エラーページマッピング
│
└── atrs-initdb/                         # DB 初期化 - テストデータ生成
    ├── pom.xml                          # SQL Mavenプラグイン設定
    └── src/sqls/integration-test-postgres/
        ├── 00000_drop_all_tables.sql    # テーブル削除
        ├── 00100_create_all_tables.sql  # DDL: テーブル作成
        ├── 00200_insert_fixed_value.sql # DML: 固定マスタデータ
        ├── 00210_insert_route.sql       # DML: 路線マスタ (20路線)
        ├── 00220_insert_flight_master.sql # DML: フライトマスタ (126便)
        ├── 00230_insert_member.sql      # DML: 会員テストデータ
        ├── 00240_insert_peak_time.sql   # DML: ピーク時期設定 (16期間)
        └── 00250_insert_flight.sql      # DML: フライトデータ (実行日+120日分)
```

**依存関係の流れ**: 
```
atrs-web (プレゼンテーション層)
  ↓ 依存
atrs-domain (ドメイン層)
  ↓ 依存
atrs-env (環境設定層)
```

**重要な設計原則**:
1. **レイヤー分離**: 各層は下位層のみに依存、上位層への依存は禁止
2. **機能別パッケージ**: サービス層は `[a-z][0-9]` で機能を分離
3. **DTO分離**: サービスは独自のDTO (例: `TicketSearchCriteriaDto`) を持ち、モデルとフォームを直接やり取りしない
4. **Interface/Impl分離**: サービス・リポジトリはインターフェースと実装を分離

## ビルド & 起動のワークフロー

```powershell
# 全モジュールビルド (推奨: 依存関係の整合性確保)
mvn clean install -P default

# DB 初期化 (実行日 + 120 日分のフライトデータ生成)
mvn sql:execute -f atrs-initdb/pom.xml

# Tomcat 10.1 で起動 (Cargo Maven プラグイン)
mvn cargo:run -P default -f atrs-web/pom.xml
# → http://localhost:8080/atrs/
```

**注意事項**:
- **MapStruct 1.5.5.Final 固定**: バージョン 1.6.3 にはバグがあり、同一パッケージ内のクラス参照で問題発生
- **フライトデータの有効期限**: 検索結果が空の場合は `mvn sql:execute` を再実行してデータを更新
- **Java 17 必須**: Jakarta EE 10 および Spring 6.x 互換性のため

## コーディング規約: パッケージ命名パターン

### 機能別パッケージ命名規則

**パターン**: `[a-z][0-9]` で機能を識別 (例: `b1` = チケット検索、`c2` = 会員情報更新)

| パッケージ | 機能名 | 主要クラス | 説明 |
|----------|--------|----------|------|
| **a0** | 会員共通 | MembershipSharedService | 会員関連の共通処理 (認証、権限チェック) |
| **a1** | ログイン | AuthLoginService, AuthLoginController | 認証・ログイン処理 |
| **b0** | チケット共通 | TicketSharedService | チケット関連共通処理 (運賃計算、空席検証) |
| **b1** | 空席照会 | TicketSearchService, TicketSearchController | フライト検索・空席照会 |
| **b2** | チケット予約 | TicketReserveService, TicketReserveController | チケット予約・購入処理 |
| **c1** | 会員登録 | MemberRegisterService, MemberRegisterController | 新規会員登録 |
| **c2** | 会員情報更新 | MemberUpdateService, MemberUpdateController | 会員情報変更 |
| **d1** | 予約履歴 | ReservationHistoryReportService | 予約履歴レポート生成 |

### ドメイン層のクラス命名パターン

#### サービス層 (`atrs-domain/src/main/java/.../service/`)

```java
// パターン1: ビジネスロジック実装
{機能パッケージ}/
├── {機能名}Service.java          // インターフェース
├── {機能名}ServiceImpl.java      // 実装クラス (@Service)
├── {機能名}CriteriaDto.java      // 入力DTO (検索条件など)
└── {機能名}ResultDto.java         // 出力DTO (検索結果など)

// 例: チケット検索 (b1)
b1/
├── TicketSearchService.java
├── TicketSearchServiceImpl.java
├── TicketSearchCriteriaDto.java
└── FlightVacantInfoDto.java
```

**命名規則**:
- サービスインターフェース: `{機能名}Service`
- サービス実装: `{機能名}ServiceImpl` (必ず`Impl`サフィックス)
- DTO: `{用途}{型名}Dto` (例: `TicketSearchCriteriaDto`, `FlightVacantInfoDto`)

#### リポジトリ層 (`atrs-domain/src/main/java/.../repository/`)

```java
// パターン2: データアクセス (MyBatis)
{エンティティ名小文字}/
└── {エンティティ名}Repository.java  // インターフェース (@Repository不要)

// 対応するXMLマッパー: src/main/resources/.../repository/{エンティティ名小文字}/{エンティティ名}Repository.xml

// 例: フライトリポジトリ
flight/
└── FlightRepository.java           // Java
flight/
└── FlightRepository.xml            // XML (resources下)
```

**XMLマッパーの重要ルール**:
```xml
<mapper namespace="jp.co.ntt.atrs.domain.repository.flight.FlightRepository">
  <!-- namespaceはJavaインターフェースの完全修飾名と完全一致必須 -->
  <select id="findByVacantSeatSearchCriteria" resultMap="flight-map">
    <!-- idはインターフェースのメソッド名と完全一致必須 -->
  </select>
</mapper>
```

#### モデル層 (`atrs-domain/src/main/java/.../model/`)

```java
// パターン3: ドメインモデル (エンティティ)
{エンティティ名}.java              // テーブル対応エンティティ
{エンティティ名}Cd.java            // Enum型コード定義

// 例:
Flight.java           // flightテーブル
FlightMaster.java     // flight_masterテーブル
BoardingClassCd.java  // 搭乗クラスコード enum (N, S)
FareTypeCd.java       // 運賃種別コード enum (OW, RT)
```

**Enum型コードの実装パターン**:
```java
public enum BoardingClassCd implements CodeListItem {
    N("N"),  // 普通席 (Normal)
    S("S");  // 特別席 (Special)
    
    private final String code;
    
    private BoardingClassCd(String code) {
        this.code = code;
    }
    
    @Override
    public String getCodeValue() {
        return code;
    }
}
```

### Web 層のクラス命名パターン

#### MVC コントローラー (`atrs-web/src/main/java/.../app/`)

```java
// パターン4: Spring MVC (JSPビュー)
{機能パッケージ}/
├── {機能名}Controller.java        // コントローラー (@Controller)
├── {機能名}Helper.java            // ビジネスロジック補助
├── {機能名}Form.java              // フォームオブジェクト (@ModelAttribute)
└── {大文字パッケージ名}Mapper.java  // MapStruct DTO変換

// 例: チケット検索 (b1)
b1/
├── TicketSearchController.java    // @Controller
├── TicketSearchHelper.java        // @Component
├── TicketSearchForm.java          // フォーム
└── B1Mapper.java                  // @Mapper(componentModel = "spring")
```

**コントローラーの責務**:
- `Controller`: HTTPリクエスト処理、画面遷移制御のみ
- `Helper`: 画面特有のビジネスロジック (リスト変換、ページング等)
- `Mapper` (MapStruct): DTO変換専用

#### REST API コントローラー (`atrs-web/src/main/java/.../api/` と `.../app/{機能}/`)

**パターン5-1: 独立したREST APIパッケージ** (将来の拡張用):
```java
api/{リソース名}/
├── {リソース名}RestController.java  // @RestController
├── {リソース名}Mapper.java          # MapStruct変換
├── {リソース名}Resource.java        # レスポンスDTO
└── {リソース名}Query.java           # リクエストDTO
```

**パターン5-2: 機能パッケージ内のREST API** (現行実装):
```java
app/b1/  # チケット検索機能
├── TicketSearchController.java     # @Controller (JSP)
├── FlightsApiController.java       # @Controller + @ResponseBody (REST)
├── FlightSearchCriteriaForm.java   # REST API用フォーム
├── FlightSearchCriteriaValidator.java
└── B1Mapper.java                   # MapStruct変換
```

**例: FlightsApiController** (GET `/api/flights`):
```java
@Controller
@RequestMapping("api")
public class FlightsApiController {
    @RequestMapping(value = "flights", method = RequestMethod.GET)
    @ResponseBody
    public List<FlightVacantInfoDto> getFlights(
            @Validated FlightSearchCriteriaForm form) {
        // ドメインサービスを呼び出し、USD変換済みのDTOを返却
    }
    
    @ExceptionHandler(FlightNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public ErrorResultDto handleFlightNotFoundException(...) { ... }
}
```

**REST APIの命名規則**:
- コントローラー: `{リソース名}RestController` (必ず`RestController`サフィックス)
- レスポンスDTO: `{リソース名}Resource` (リクエストと区別)
- リクエストDTO: `{リソース名}Query` または `{リソース名}Request`

### JSPビューのパス規則 (`atrs-web/src/main/webapp/WEB-INF/views/`)

```
views/
├── {機能パッケージ}/
│   ├── {画面名}.jsp              // メイン画面
│   └── {画面名}Complete.jsp      // 完了画面
└── common/
    ├── include.jsp               # 共通インクルード
    ├── header.jsp                # ヘッダー
    ├── footer.jsp                # フッター
    └── error/
        ├── systemError.jsp       # システムエラー画面
        └── businessError.jsp     # ビジネスエラー画面

// 例: チケット検索画面
b1/
├── search.jsp                    # 検索画面
├── select.jsp                    # 選択画面
└── searchComplete.jsp            # 検索結果画面
```

**ビューリゾルバー設定** (`SpringMvcConfig.java`):
```java
@Bean
public InternalResourceViewResolver viewResolver() {
    InternalResourceViewResolver resolver = new InternalResourceViewResolver();
    resolver.setPrefix("/WEB-INF/views/");
    resolver.setSuffix(".jsp");
    return resolver;
}

// コントローラーで "b1/search" を返すと /WEB-INF/views/b1/search.jsp が表示される
```

## データアクセス: MyBatis パターン

**リポジトリ定義の場所**:
- Java インターフェース: `atrs-domain/src/main/java/.../repository/`
- XML マッパー: `atrs-domain/src/main/resources/.../repository/`

**例**: `FlightRepository.java` (インターフェース) + `FlightRepository.xml` (SQL 定義)

```xml
<!-- atrs-domain/src/main/resources/.../repository/flight/FlightRepository.xml -->
<mapper namespace="jp.co.ntt.atrs.domain.repository.flight.FlightRepository">
    <select id="findByVacantSeatSearchCriteria" resultMap="flight-map">
        SELECT f.departure_date, f.flight_name, f.vacant_num
        FROM flight f WHERE f.departure_date = #{criteria.depDate}
    </select>
</mapper>
```

**重要**: namespace はリポジトリインターフェースの完全修飾名と一致させる。

## 依存性注入: Jakarta Inject

```java
@Service
public class TicketSearchServiceImpl implements TicketSearchService {
    @Inject  // ← Jakarta Inject (@Autowired ではなく)
    ClockFactory dateFactory;
    
    @Inject
    FlightRepository flightRepository;
}
```

**パターン**: `@Inject` (Jakarta EE 標準) を使用。Spring の `@Autowired` は使用しない。

## 環境設定: プロファイルとプロパティ

**データベース接続**: `atrs-env/src/main/resources/META-INF/spring/atrs-infra.properties`

```properties
database.url=jdbc:postgresql://localhost:5432/atrs
database.username=postgres
database.password=postgres
jms.mq.host=localhost
jms.mq.port=61616
```

**Cargo 起動設定**: `atrs-web/pom.xml` で Tomcat ポート変更可能

```xml
<cargo.servlet.port>8080</cargo.servlet.port>  <!-- デフォルト -->
```

## テストとデバッグ

```powershell
# 単体テストスキップ (ビルド高速化)
mvn clean package -DskipTests

# 特定モジュールのみ再ビルド
mvn clean package -pl atrs-web -am -DskipTests

# フライトデータの日付範囲確認
psql -U postgres -d atrs -c "SELECT MIN(departure_date), MAX(departure_date) FROM flight;"
```

**検索テスト用データ**:
- 空港コード: HND (羽田), ITM (伊丹), FUK (福岡) など 20 空港
- フライトデータ: 実行日から 120 日間分が自動生成される

## REST API 規約

### 現行実装: FlightsApiController

**エンドポイント**: `GET /atrs/api/flights`

```java
@Controller
@RequestMapping("api")
public class FlightsApiController {
    @Inject
    TicketSearchService ticketSearchService;
    
    @Inject
    B1Mapper beanMapper;
    
    @RequestMapping(value = "flights", method = RequestMethod.GET)
    @ResponseBody
    public List<FlightVacantInfoDto> getFlights(
            @Validated FlightSearchCriteriaForm form) {
        // MapStruct で DTO 変換
        TicketSearchCriteriaDto dto = beanMapper.map(form);
        // USD変換済みのFlightVacantInfoDtoを返却
        return ticketSearchService.searchFlight(dto);
    }
    
    // 例外ハンドリング
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ErrorResultDto handleMethodArgumentNotValidException(...) { ... }
    
    @ExceptionHandler(FlightNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public ErrorResultDto handleFlightNotFoundException(...) { ... }
}
```

**リクエスト例**:
```bash
GET /atrs/api/flights?depAirportCd=HND&arrAirportCd=ITM&depDate=2025-12-01&boardingClassCd=N&flightType=RT
```

**レスポンス例** (JSON):
```json
[
  {
    "flightName": "NTT001",
    "depAirportName": "東京(羽田)",
    "arrAirportName": "大阪(伊丹)",
    "depTime": "09:00",
    "arrTime": "10:30",
    "depDate": "2025-12-01",
    "boardingClassCd": "N",
    "fareTypes": {
      "RT": {
        "fareTypeName": "往復",
        "fare": "8,000",
        "fareUsd": "$54",
        "vacantNum": 50
      },
      "OW": {
        "fareTypeName": "片道",
        "fare": "10,000",
        "fareUsd": "$67",
        "vacantNum": 50
      }
    }
  }
]
```

### USD表示機能

**実装場所**:
- `TicketSharedService.convertYenToUsd(int yenFare)`: 円→ドル変換ロジック
- `TicketSearchServiceImpl.searchFlight()`: 検索結果にUSD運賃を追加
- `FareTypeVacantInfoDto.fareUsd`: ドル建て運賃フィールド (例: "$54")

**変換ロジック** (`TicketSharedServiceImpl`):
```java
public int convertYenToUsd(int yenFare) {
    // 固定レート: 1 USD = 148.5 JPY
    return (int) Math.ceil(yenFare / 148.5);
}
```

**注意事項**:
- 為替レートは固定値 (148.5円/ドル)
- 切り上げ処理 (Math.ceil)
- ドル表示は「$」記号付きで整形 (例: "$54")

## トラブルシューティング

1. **MapStruct 生成エラー**: `pom.xml` で `mapstruct.version` が 1.5.5.Final であることを確認
2. **空席検索結果なし**: フライトデータが古い可能性 → `mvn sql:execute -f atrs-initdb/pom.xml` で再投入
3. **ポート競合**: `atrs-web/pom.xml` の `<cargo.servlet.port>` を変更

## エラーハンドリング: 3 層構造の例外処理

### 例外クラス階層

```java
// ドメイン層: ビジネス例外の基底クラス
public class AtrsBusinessException extends BusinessException {
    public AtrsBusinessException(AtrsErrorCode errorCode, Object... args) {
        super(ResultMessages.danger().add(ResultMessage.fromCode(errorCode.code(), args)));
    }
}

// エラーコードは enum で機能別に定義
public enum TicketReserveErrorCode implements AtrsErrorCode {
    E_AR_B2_2001("e.ar.b2.2001"),  // 往復フライト時間間隔エラー
    E_AR_B2_2009("e.ar.b2.2009");  // 残席不足エラー
    
    private final String code;
    public String code() { return code; }
}
```

**パターン**: 
- エラーコードは各サービスパッケージ (`b1`, `b2`, `c2` など) に対応した enum で定義
- メッセージは `i18n/atrs-messages_ja.properties` で国際化対応

### Web 層での例外ハンドリング

**MVC コントローラー (JSP ビュー)**:

```java
@Controller
public class TicketSearchController {
    @RequestMapping(method = RequestMethod.POST)
    public String search(@Validated TicketSearchForm form, BindingResult result) {
        try {
            List<Flight> flights = ticketSearchService.searchFlight(dto);
        } catch (BusinessException e) {
            model.addAttribute(e.getResultMessages());
            return "ticket/search";  // エラーメッセージ付きで再表示
        }
    }
}
```

**REST API コントローラー**:

```java
@RestController
@RequestMapping("/flight")
public class FlightRestController {
    // @ControllerAdvice で一括ハンドリング (ApiGlobalExceptionHandler)
    @RequestMapping(method = RequestMethod.GET)
    public List<FlightResource> getFlights(@Validated FlightSearchQuery query) {
        return ticketSearchService.searchFlight(dto);  // 例外は自動で JSON 化
    }
}
```

### グローバル例外ハンドラー

**REST API 用** (`ApiGlobalExceptionHandler`):

```java
@ControllerAdvice
public class ApiGlobalExceptionHandler extends ResponseEntityExceptionHandler {
    
    @ExceptionHandler(AtrsBusinessException.class)
    public ResponseEntity<Object> handleAtrsBusinessException(AtrsBusinessException ex, ...) {
        return handleExceptionInternal(ex, apiError, headers, HttpStatus.CONFLICT, request);
    }
    
    @ExceptionHandler(ResourceNotFoundException.class)  // HTTP 404
    @ExceptionHandler(OptimisticLockingFailureException.class)  // HTTP 409
    @ExceptionHandler(Exception.class)  // HTTP 500 (システムエラー)
}
```

### エラーページマッピング (`web.xml`)

```xml
<error-page>
    <error-code>400</error-code>
    <location>/WEB-INF/views/common/error/badRequest-error.jsp</location>
</error-page>
<error-page>
    <error-code>404</error-code>
    <location>/WEB-INF/views/common/error/notFound-error.jsp</location>
</error-page>
<error-page>
    <exception-type>java.lang.Exception</exception-type>
    <location>/WEB-INF/views/common/error/system-error.jsp</location>
</error-page>
```

**重要**: 
- MVC コントローラーは `try-catch` で個別ハンドリング
- REST API は `@ControllerAdvice` で一括ハンドリング
- エラーコードは `ApplicationContextConfig` の `ExceptionCodeResolver` で管理

## テスト規約

### テストの種類と配置

**現在の実装状況**: `atrs-domain` モジュールに `TicketSearchServiceImplTest` を実装済み (14テストケース、93%カバレッジ達成)。

```
atrs-domain/src/test/java/
└── jp/co/ntt/atrs/domain/
    ├── service/
    │   ├── b1/TicketSearchServiceImplTest.java  ✅ 実装済み (14テスト、93%カバレッジ)
    │   └── b2/TicketReserveServiceImplTest.java  ← 今後追加予定
    └── repository/
        └── flight/FlightRepositoryTest.java      ← 今後追加予定

atrs-web/src/test/java/
└── jp/co/ntt/atrs/
    ├── app/b1/TicketSearchControllerTest.java    ← 今後追加予定
    └── api/flight/FlightRestControllerTest.java  ← 今後追加予定
```

### テスト実行コマンド

```powershell
# 全テスト実行
mvn test

# テストスキップ (ビルド時間短縮)
mvn clean install -DskipTests

# 特定モジュールのみテスト
mvn test -pl atrs-domain

# 特定テストクラスのみ実行
mvn test -Dtest=TicketSearchServiceImplTest
```

### 使用しているテストフレームワーク

#### 単体テスト依存関係 (atrs-domain/pom.xml)

```xml
<dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <version>4.13.2</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <version>5.8.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <version>3.25.1</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-test</artifactId>
    <scope>test</scope>
</dependency>
```

#### カバレッジ測定 (JaCoCo)

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

**カバレッジレポート**: `atrs-domain/target/site/jacoco/index.html`

### テスト作成の基本パターン

**サービス層モック単体テスト** (推奨パターン - DB不要):

```java
@RunWith(MockitoJUnitRunner.class)
public class TicketSearchServiceImplTest {
    
    @Mock
    private FlightRepository flightRepository;
    
    @Mock
    private RouteProvider routeProvider;
    
    @Mock
    private FareTypeProvider fareTypeProvider;
    
    @Mock
    private FlightMasterProvider flightMasterProvider;
    
    @Mock
    private BoardingClassProvider boardingClassProvider;
    
    @Mock
    private TicketSharedService ticketSharedService;
    
    @Mock
    private ClockFactory dateFactory;
    
    @InjectMocks
    private TicketSearchServiceImpl target;  // テスト対象
    
    @Before
    public void setUp() {
        // 固定日時でテストを決定的に
        Clock fixedClock = Clock.fixed(
            Instant.parse("2025-11-27T00:00:00Z"), 
            ZoneId.systemDefault()
        );
        when(dateFactory.tick()).thenReturn(fixedClock);
    }
    
    @Test
    public void testSearchFlight_正常系_基本検索() {
        // Given: テストデータとモックの準備
        TicketSearchCriteriaDto criteria = createSearchCriteria();
        Route mockRoute = createMockRoute();
        List<Flight> mockFlights = createMockFlights();
        
        when(routeProvider.getRouteByAirportCd("HND", "ITM"))
            .thenReturn(mockRoute);
        when(flightRepository.findByVacantSeatSearchCriteria(any()))
            .thenReturn(mockFlights);
        when(fareTypeProvider.getFareType(any()))
            .thenReturn(mockFareType);
        when(ticketSharedService.calculateBasicFare(anyInt(), any(), any()))
            .thenReturn(10000);
        when(ticketSharedService.calculateFare(anyInt(), anyInt()))
            .thenReturn(8000);
        
        // When: テスト実行
        List<FlightVacantInfoDto> result = target.searchFlight(criteria);
        
        // Then: AssertJで検証
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFlightName()).isEqualTo("NTT001");
        assertThat(result.get(0).getDepAirportName()).isEqualTo("東京(羽田)");
        
        // モック呼び出しの検証
        verify(flightRepository).findByVacantSeatSearchCriteria(any());
        verify(ticketSharedService).validateDepatureDate(any());
    }
    
    @Test(expected = AtrsBusinessException.class)
    public void testSearchFlight_異常系_路線が存在しない() {
        // Given
        TicketSearchCriteriaDto criteria = createSearchCriteria();
        when(routeProvider.getRouteByAirportCd("HND", "ITM"))
            .thenReturn(null);  // 路線なし
        
        // When/Then: 例外がスローされることを期待
        target.searchFlight(criteria);
    }
}
```

**テストパターンのポイント**:

1. **@RunWith(MockitoJUnitRunner.class)**: Mockitoの自動初期化
2. **@Mock**: 依存コンポーネントをモック化 (DB不要)
3. **@InjectMocks**: テスト対象にモックを自動注入
4. **@Before setUp()**: 各テスト前の共通初期化 (固定Clock設定など)
5. **Given-When-Then**: テスト構造を明確に分離
6. **AssertJ**: `assertThat()` で流暢なアサーション
7. **verify()**: モックメソッドの呼び出し検証
8. **thenAnswer()**: 複雑な戻り値の動的生成

**日付型の注意点**:
- `java.util.Date` を使用 (実装に合わせる)
- `java.sql.Date` は `toInstant()` が未サポートのため避ける
- テストでは `Date.from(LocalDate.of(...).atStartOfDay(ZoneId.systemDefault()).toInstant())` で生成

**複数フライトのテスト**:
- 実装は `departureTime` をキーとして `LinkedHashMap` でグループ化
- 異なる出発時刻のフライトは別々のエントリとして返される
- 同じ出発時刻は1エントリに集約され、`fareTypes` マップに運賃種別が追加される
- `FlightMasterProvider` のモック設定で各フライト名に対応した `FlightMaster` を返す必要がある

**カバレッジ目標**:
- サービス層: 90%以上
- ドメインモデル: 80%以上
- コントローラー層: 70%以上

**USD変換テストの例** (`TicketSearchServiceImplTest`):
```java
@Test
public void testSearchFlight_正常系_USD表示() {
    // Given: USD変換モックの設定
    when(ticketSharedService.convertYenToUsd(8000)).thenReturn(54);
    
    // When: フライト検索実行
    List<FlightVacantInfoDto> result = target.searchFlight(criteria);
    
    // Then: USD表示の検証
    FareTypeVacantInfoDto fareInfo = result.get(0).getFareTypes().get("RT");
    assertThat(fareInfo.getFareUsd()).isEqualTo("$54");
    verify(ticketSharedService).convertYenToUsd(8000);
}
```

## コード変更時の再ビルド

- **Java 変更**: 該当モジュールの再ビルドが必要
- **JSP/CSS/JS 変更**: ブラウザリロードのみで反映 (Cargo 起動中)
