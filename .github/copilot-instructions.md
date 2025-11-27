# ATRS プロジェクト - AI コーディングエージェント向けガイド

## プロジェクト概要

航空券予約システム (ATRS) - TERASOLUNA Server Framework 5.10.0 と Spring Framework 6.2.1 を使用したマルチモジュール Maven プロジェクト。PostgreSQL データベースと Apache ActiveMQ Artemis メッセージングを統合した、エンタープライズ Java Web アプリケーションのリファレンス実装。

## アーキテクチャ: 4 層構造のマルチモジュール

```
atrs/                      # 親 POM (TERASOLUNA 5.10.0)
├── atrs-env/              # 環境設定層 - データソース、JMS、ログ設定
├── atrs-domain/           # ドメイン層 - ビジネスロジック、リポジトリ (MyBatis)
├── atrs-web/              # プレゼンテーション層 - MVC + REST API
└── atrs-initdb/           # DB 初期化 - 120 日分のフライトデータ生成
```

**依存関係の流れ**: `atrs-web` → `atrs-domain` → `atrs-env` (各層は下位層に依存)

**重要**: 各モジュールには固有の `pom.xml` があり、親 POM は共通依存関係とプラグイン設定を管理。

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

### ドメイン層 (`atrs-domain`)

```
jp.co.ntt.atrs.domain/
├── service/
│   ├── a0/  # 会員共通サービス (MembershipSharedService)
│   ├── a1/  # 認証ログインサービス (AuthLoginService)
│   ├── b0/  # チケット共通サービス (TicketSharedService)
│   ├── b1/  # 空席照会サービス (TicketSearchService)
│   ├── b2/  # チケット予約サービス (TicketReserveService)
│   ├── c1/  # 会員登録サービス (MemberRegisterService)
│   └── d1/  # 予約履歴レポートサービス (ReservationHistoryReportService)
├── repository/  # MyBatis リポジトリインターフェース
│   ├── flight/  # FlightRepository + FlightRepository.xml
│   ├── member/
│   └── reservation/
└── model/       # ドメインモデル (Flight, Route, Member など)
```

**パターン**: サービスは機能別に `[a-z][0-9]` パッケージで分離 (例: `b1` = チケット検索機能)

### Web 層 (`atrs-web`)

```
jp.co.ntt.atrs/
├── app/         # Spring MVC コントローラー (JSP ビュー)
│   ├── a1/      # AuthLoginController (ログイン画面)
│   ├── b1/      # TicketSearchController + TicketSearchHelper + B1Mapper
│   └── c2/      # MemberUpdateController
├── api/         # REST API コントローラー (@RestController)
│   ├── flight/  # FlightRestController + FlightMapper (MapStruct)
│   └── ticket/  # TicketRestController + TicketMapper
└── config/
    ├── app/     # ApplicationContextConfig (Spring 設定)
    └── web/     # SpringMvcConfig (MVC 設定)
```

**パターン**: 
- `app/` = JSP ビュー用 MVC コントローラー + Helper + Mapper (MapStruct)
- `api/` = JSON REST API 専用 (@RestController)
- **Helper クラス**: コントローラーのビジネスロジック補助 (例: `TicketSearchHelper`)
- **MapStruct マッパー**: `@Mapper(componentModel = "spring")` で DTO ↔ ドメインモデル変換

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

```java
@RestController
@RequestMapping("/flight")
public class FlightRestController {
    @RequestMapping(method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public List<FlightResource> getFlights(@Validated FlightSearchQuery query) {
        // MapStruct で DTO 変換
        TicketSearchCriteriaDto dto = beanMapper.map(query);
        return ticketSearchService.searchFlight(dto);
    }
}
```

**エンドポイント例**: `GET /atrs/api/v1/flight?depAirportCd=HND&arrAirportCd=ITM&depDate=2025-12-01`

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

**現在の状態**: このプロジェクトには既存のテストファイルが含まれていないため、以下は新規作成時の推奨パターン。

```
atrs-domain/src/test/java/
└── jp/co/ntt/atrs/domain/
    ├── service/
    │   ├── b1/TicketSearchServiceImplTest.java  # サービス層単体テスト
    │   └── b2/TicketReserveServiceImplTest.java
    └── repository/
        └── flight/FlightRepositoryTest.java     # MyBatis リポジトリテスト

atrs-web/src/test/java/
└── jp/co/ntt/atrs/
    ├── app/b1/TicketSearchControllerTest.java   # MVC コントローラーテスト
    └── api/flight/FlightRestControllerTest.java # REST API テスト
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

### 推奨テストフレームワーク

- **JUnit**: 既に依存関係に含まれている (`junit:junit`)
- **Mockito**: サービス層の依存コンポーネントをモック化
- **Spring Test**: `@SpringBootTest`, `@WebMvcTest` などの統合テスト
- **DBUnit**: データベーステスト用初期データ投入

### テスト作成の基本パターン

**サービス層テスト例**:

```java
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ApplicationContextConfig.class})
public class TicketSearchServiceImplTest {
    
    @Inject
    TicketSearchService ticketSearchService;
    
    @Test
    public void testSearchFlight_正常系() {
        // Given
        TicketSearchCriteriaDto criteria = new TicketSearchCriteriaDto();
        criteria.setDepAirportCd("HND");
        criteria.setArrAirportCd("ITM");
        
        // When
        List<FlightVacantInfoDto> result = ticketSearchService.searchFlight(criteria);
        
        // Then
        assertThat(result, is(notNullValue()));
        assertThat(result.size(), is(greaterThan(0)));
    }
}
```

**重要**: 
- テストデータは `mvn sql:execute -f atrs-initdb/pom.xml` で投入されたものを使用
- PostgreSQL が起動している必要がある
- テスト用プロファイル設定は `src/test/resources` に配置

## コード変更時の再ビルド

- **Java 変更**: 該当モジュールの再ビルドが必要
- **JSP/CSS/JS 変更**: ブラウザリロードのみで反映 (Cargo 起動中)
