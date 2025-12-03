# Plan: 香港ドル運賃表示機能の追加

ATRSの空席照会・予約機能に香港ドル (HKD) 運賃を追加します。現行のUSD表示実装パターンを踏襲し、`FareTypeVacantInfoDto`に`fareHkd`フィールドを追加、為替換算ロジックを`TicketSharedService`に実装、JSPとREST APIの両方で表示対応します。

## Steps

1. **[`TicketSharedService.java`](atrs-domain/src/main/java/jp/co/ntt/atrs/domain/service/b0/TicketSharedService.java)にHKD変換メソッド追加** - `convertYenToHkd(int yenFare)`シグネチャを定義（84-89行目のUSDメソッドと同様のパターン）

2. **[`TicketSharedServiceImpl.java`](atrs-domain/src/main/java/jp/co/ntt/atrs/domain/service/b0/TicketSharedServiceImpl.java)にHKD変換ロジック実装** - 72-75行目に`@Value("${atrs.exchangeRateHkd}")`でHKDレート注入、359-362行目と同様の`Math.ceil()`による切り上げ処理を実装

3. **[`atrs.properties`](atrs-domain/src/main/resources/META-INF/spring/atrs.properties)にHKD為替レート追加** - 23-24行目の`atrs.exchangeRate`に続けて`atrs.exchangeRateHkd=11.65`を定義（1 HKD = 11.65 JPY想定）

4. **[`FareTypeVacantInfoDto.java`](atrs-domain/src/main/java/jp/co/ntt/atrs/domain/service/b1/FareTypeVacantInfoDto.java)に`fareHkd`フィールド追加** - 26-42行目のコンストラクタに`String fareHkd`引数を追加、フィールド定義・getterを実装（`fareUsd`と同じイミュータブル設計）

5. **[`TicketSearchServiceImpl.java`](atrs-domain/src/main/java/jp/co/ntt/atrs/domain/service/b1/TicketSearchServiceImpl.java)でHKD変換呼び出し** - 177-183行目の`convertYenToUsd()`に続けて`convertYenToHkd()`を呼び出し、`FareTypeVacantInfoDto`に`"HK$" + fareFormatter.format(fareHkd)`を渡す

6. **[`flightSearch.jsp`](atrs-web/src/main/webapp/WEB-INF/views/b1/flightSearch.jsp)にHKD表示追加** - 182行目の`fareUsd`表示の下に`<span class="fare-hkd text-muted small"><@- flightClass.fareHkd @></span>`を追加

7. **[`TicketSearchServiceImplTest.java`](atrs-domain/src/test/java/jp/co/ntt/atrs/domain/service/b1/TicketSearchServiceImplTest.java)にHKD変換テスト追加** - 686-719行目のUSDテストを参考に`testSearchFlight_正常系_HKD換算が正しい()`を実装、`when(ticketSharedService.convertYenToHkd(8000)).thenReturn(687)`のモック設定とAssertJによる`fareHkd`検証

## Further Considerations

1. **為替レートの更新頻度** - 固定値 (atrs.properties) を維持するか、外部API連携 (実装時間+30%) で動的更新するか？推奨は固定値で開始し、将来拡張

2. **通貨の拡張性** - 3通貨目以降の追加を見越して`Map<String, String> fares`方式へのリファクタリング（+50%工数）を検討するか、現行のフィールド追加方式を継続するか？

3. **小数点処理の統一** - USDと同じ`Math.ceil()`による切り上げで良いか、HKDは小数点2桁表示（例: HK$687.25）にするか？
