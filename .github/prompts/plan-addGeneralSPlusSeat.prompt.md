# Plan: 座席クラス「一般Ｓ席」の追加実装

現在の座席クラス(N:一般席、S:特別席)に、一般席の料金に10%上乗せの「一般Ｓ席(NS)」を追加します。データベーススキーマ変更、ドメイン層のEnum追加、料金計算ロジックの修正、テストコード追加を含む包括的な実装です。

## Steps

1. **データベーススキーマの拡張**: [`00100_create_all_tables.sql`](c:\Users\chiru\atrs-ghcp-demo\atrs-initdb\src\sqls\integration-test-postgres\00100_create_all_tables.sql)で`boarding_class_cd`列を`VARCHAR(1)`→`VARCHAR(2)`に変更、`flight.boarding_class_cd`も同様に拡張

2. **マスタデータの追加**: [`00200_insert_fixed_value.sql`](c:\Users\chiru\atrs-ghcp-demo\atrs-initdb\src\sqls\integration-test-postgres\00200_insert_fixed_value.sql)に`boarding_class`テーブルへ`('NS', '一般Ｓ席', 0, 3)`レコードを追加、[`00250_insert_flight.sql`](c:\Users\chiru\atrs-ghcp-demo\atrs-initdb\src\sqls\integration-test-postgres\00250_insert_flight.sql)でNS席のフライトデータ生成

3. **Enum定義の拡張**: [`BoardingClassCd.java`](c:\Users\chiru\atrs-ghcp-demo\atrs-domain\src\main\java\jp\co\ntt\atrs\domain\model\BoardingClassCd.java)に`NS`値を追加

4. **料金計算ロジックの修正**: [`TicketSharedServiceImpl.java`](c:\Users\chiru\atrs-ghcp-demo\atrs-domain\src\main\java\jp\co\ntt\atrs\domain\service\b0\TicketSharedServiceImpl.java)の`calculateBasicFare()`メソッドで`BoardingClassCd.NS`の場合に基本運賃を1.10倍に修正

5. **テストケースの追加**: [`TicketSearchServiceImplTest.java`](c:\Users\chiru\atrs-ghcp-demo\atrs-domain\src\test\java\jp\co\ntt\atrs\domain\service\b1\TicketSearchServiceImplTest.java)に一般Ｓ席の検索テストを追加、`TicketSharedServiceImplTest.java`を新規作成して10%加算の単体テスト実装

6. **ビルドとテスト実行**: `mvn clean install -P default`で全モジュールをビルドし、単体テストを実行してカバレッジレポート(`atrs-domain/target/site/jacoco/index.html`)を確認

7. **データベース初期化**: `mvn sql:execute -f atrs-initdb/pom.xml`でDBを再初期化し、NS席のマスタデータとフライトデータが正しく投入されることを確認

8. **アプリケーション起動と動作確認**: `mvn cargo:run -P default -f atrs-web/pom.xml`でTomcatを起動し、以下の観点で動作確認:
   - チケット検索画面(http://localhost:8080/atrs/ticket/search)で「一般Ｓ席」が選択肢に表示されるか
   - NS席を選択して検索実行時、検索結果が正しく表示されるか
   - NS席の料金が一般席(N)より約10%高く表示されるか
   - NS席を選択して予約完了まで進めるか
   - 予約完了画面で料金が正しく計算されているか

9. **REST API動作確認**: `GET http://localhost:8080/atrs/api/v1/flight?depAirportCd=HND&arrAirportCd=ITM&depDate=2025-12-01&boardingClassCd=NS`でAPI経由の検索も確認

10. **データベース整合性確認**: PostgreSQLクライアントで以下のSQLを実行し、データの整合性を確認:
    ```sql
    -- NS席のマスタデータ確認
    SELECT * FROM boarding_class WHERE boarding_class_cd = 'NS';
    
    -- NS席のフライトデータ確認
    SELECT COUNT(*) FROM flight WHERE boarding_class_cd = 'NS';
    
    -- NS席の予約データ確認(予約実行後)
    SELECT r.*, rf.boarding_class_cd 
    FROM reservation r 
    JOIN reserve_flight rf ON r.reserve_no = rf.reserve_no 
    WHERE rf.boarding_class_cd = 'NS';
    ```

## Further Considerations

1. **料金加算タイミング**: ピーク時期係数(100%〜150%)を適用した後に10%加算するか、適用前に加算するか？現在の調査では適用後を推奨していますが、ビジネス要件の確認が必要です

2. **小児運賃への適用**: 一般Ｓ席の10%加算は小児運賃(基本運賃の50%)にも適用されるか？`(基本運賃 × 1.10) × 0.50`と`基本運賃 × 0.50 × 1.10`は同じですが、割引運賃種別との組み合わせで確認が必要です

3. **既存予約データへの影響**: 本番環境に既存の予約データがある場合、`boarding_class_cd VARCHAR(2)`への変更時に外部キー制約の再作成やデータ移行スクリプトが必要です
