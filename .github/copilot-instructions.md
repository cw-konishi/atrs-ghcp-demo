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

## コード変更時の再ビルド

- **Java 変更**: 該当モジュールの再ビルドが必要
- **JSP/CSS/JS 変更**: ブラウザリロードのみで反映 (Cargo 起動中)
