/*
 * Copyright(c) 2015 NTT Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package jp.co.ntt.atrs.domain.service.b1;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.terasoluna.gfw.common.time.ClockFactory;

import jp.co.ntt.atrs.domain.common.masterdata.BoardingClassProvider;
import jp.co.ntt.atrs.domain.common.masterdata.FareTypeProvider;
import jp.co.ntt.atrs.domain.common.masterdata.FlightMasterProvider;
import jp.co.ntt.atrs.domain.common.masterdata.RouteProvider;
import jp.co.ntt.atrs.domain.model.Airport;
import jp.co.ntt.atrs.domain.model.BoardingClass;
import jp.co.ntt.atrs.domain.model.BoardingClassCd;
import jp.co.ntt.atrs.domain.model.FareType;
import jp.co.ntt.atrs.domain.model.FareTypeCd;
import jp.co.ntt.atrs.domain.model.Flight;
import jp.co.ntt.atrs.domain.model.FlightMaster;
import jp.co.ntt.atrs.domain.model.FlightType;
import jp.co.ntt.atrs.domain.model.Route;
import jp.co.ntt.atrs.domain.repository.flight.FlightRepository;
import jp.co.ntt.atrs.domain.service.b0.TicketSharedService;

/**
 * TicketSearchServiceImpl のテストクラス。
 */
@RunWith(MockitoJUnitRunner.class)
public class TicketSearchServiceImplTest {

    @Mock
    private ClockFactory dateFactory;

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

    @InjectMocks
    private TicketSearchServiceImpl target;

    private Clock fixedClock;

    @Before
    public void setUp() {
        // 2025年11月27日に固定
        fixedClock = Clock.fixed(Instant.parse("2025-11-27T00:00:00Z"), ZoneId.systemDefault());
        when(dateFactory.tick()).thenReturn(fixedClock);
    }

    /**
     * フライト検索_正常系: 検索条件に合致するフライトが存在する場合
     */
    @Test
    public void testSearchFlight_正常系_フライトが見つかる() {
        // Given
        TicketSearchCriteriaDto criteria = createSearchCriteria();

        Route mockRoute = createMockRoute();
        when(routeProvider.getRouteByAirportCd("HND", "ITM")).thenReturn(mockRoute);

        List<Flight> mockFlights = createMockFlights();
        when(flightRepository.findByVacantSeatSearchCriteria(any())).thenReturn(mockFlights);

        // マスタデータのモック設定
        setupMasterDataMocks();

        // 運賃計算のモック設定
        when(ticketSharedService.calculateBasicFare(anyInt(), any(), any())).thenReturn(10000);
        when(ticketSharedService.calculateFare(anyInt(), anyInt())).thenReturn(8000);

        // When
        List<FlightVacantInfoDto> result = target.searchFlight(criteria);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(1);

        FlightVacantInfoDto flightInfo = result.get(0);
        assertThat(flightInfo.getFlightName()).isEqualTo("NTT001");
        assertThat(flightInfo.getDepAirportName()).isEqualTo("東京(羽田)");
        assertThat(flightInfo.getArrAirportName()).isEqualTo("大阪(伊丹)");

        // リポジトリが呼ばれたことを確認
        verify(flightRepository, times(1)).findByVacantSeatSearchCriteria(any());
        verify(ticketSharedService, times(1)).validateDepatureDate(any());
    }

    /**
     * フライト検索_異常系: 該当する路線が存在しない場合
     */
    @Test
    public void testSearchFlight_異常系_路線が存在しない() {
        // Given
        TicketSearchCriteriaDto criteria = createSearchCriteria();
        when(routeProvider.getRouteByAirportCd("HND", "ITM")).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> target.searchFlight(criteria))
                .isInstanceOf(Exception.class);

        verify(flightRepository, never()).findByVacantSeatSearchCriteria(any());
    }

    /**
     * フライト検索_異常系: 該当するフライトが存在しない場合
     */
    @Test
    public void testSearchFlight_異常系_フライトが見つからない() {
        // Given
        TicketSearchCriteriaDto criteria = createSearchCriteria();

        Route mockRoute = createMockRoute();
        when(routeProvider.getRouteByAirportCd("HND", "ITM")).thenReturn(mockRoute);

        // 空のリストを返す
        when(flightRepository.findByVacantSeatSearchCriteria(any())).thenReturn(Arrays.asList());

        // When & Then
        assertThatThrownBy(() -> target.searchFlight(criteria))
                .isInstanceOf(FlightNotFoundException.class);

        verify(flightRepository, times(1)).findByVacantSeatSearchCriteria(any());
    }

    /**
     * フライト検索_異常系: 検索条件がnullの場合
     */
    @Test
    public void testSearchFlight_異常系_検索条件がnull() {
        // When & Then
        assertThatThrownBy(() -> target.searchFlight(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("searchCriteria must not null");

        verify(flightRepository, never()).findByVacantSeatSearchCriteria(any());
    }

    /**
     * フライト検索_正常系: 片道フライトの検索
     */
    @Test
    public void testSearchFlight_正常系_片道フライト() {
        // Given
        TicketSearchCriteriaDto criteria = createSearchCriteria();
        criteria.setFlightType(FlightType.OW); // 片道に変更

        Route mockRoute = createMockRoute();
        when(routeProvider.getRouteByAirportCd("HND", "ITM")).thenReturn(mockRoute);

        List<Flight> mockFlights = createMockFlights();
        when(flightRepository.findByVacantSeatSearchCriteria(any())).thenReturn(mockFlights);

        setupMasterDataMocks();
        when(ticketSharedService.calculateBasicFare(anyInt(), any(), any())).thenReturn(10000);
        when(ticketSharedService.calculateFare(anyInt(), anyInt())).thenReturn(10000);

        // When
        List<FlightVacantInfoDto> result = target.searchFlight(criteria);

        // Then
        assertThat(result).isNotNull().isNotEmpty();
        verify(flightRepository, times(1)).findByVacantSeatSearchCriteria(any());
    }

    /**
     * フライト検索_正常系: 特別席の検索
     */
    @Test
    public void testSearchFlight_正常系_特別席() {
        // Given
        TicketSearchCriteriaDto criteria = createSearchCriteria();
        criteria.setBoardingClassCd(BoardingClassCd.S); // 特別席

        Route mockRoute = createMockRoute();
        when(routeProvider.getRouteByAirportCd("HND", "ITM")).thenReturn(mockRoute);

        List<Flight> mockFlights = createMockFlightsForSpecialClass();
        when(flightRepository.findByVacantSeatSearchCriteria(any())).thenReturn(mockFlights);

        setupMasterDataMocksForSpecialClass();
        when(ticketSharedService.calculateBasicFare(anyInt(), any(), any())).thenReturn(15000);
        when(ticketSharedService.calculateFare(anyInt(), anyInt())).thenReturn(12000);

        // When
        List<FlightVacantInfoDto> result = target.searchFlight(criteria);

        // Then
        assertThat(result).isNotNull().isNotEmpty();
        FlightVacantInfoDto flightInfo = result.get(0);
        assertThat(flightInfo.getBoardingClassCd()).isEqualTo(BoardingClassCd.S);
    }

    /**
     * フライト検索_正常系: 複数のフライトが存在する場合
     */
    @Test
    public void testSearchFlight_正常系_複数フライト() {
        // Given
        TicketSearchCriteriaDto criteria = createSearchCriteria();

        Route mockRoute = createMockRoute();
        when(routeProvider.getRouteByAirportCd("HND", "ITM")).thenReturn(mockRoute);

        List<Flight> mockFlights = createMultipleMockFlights();
        when(flightRepository.findByVacantSeatSearchCriteria(any())).thenReturn(mockFlights);

        setupMasterDataMocks();
        when(ticketSharedService.calculateBasicFare(anyInt(), any(), any())).thenReturn(10000);
        when(ticketSharedService.calculateFare(anyInt(), anyInt())).thenReturn(8000);

        // When
        List<FlightVacantInfoDto> result = target.searchFlight(criteria);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3); // 3つの異なる出発時刻
        assertThat(result.get(0).getFlightName()).isEqualTo("NTT001");
        assertThat(result.get(1).getFlightName()).isEqualTo("NTT002");
        assertThat(result.get(2).getFlightName()).isEqualTo("NTT003");
    }

    /**
     * フライト検索_正常系: 同じ出発時刻で異なる運賃種別のフライト
     */
    @Test
    public void testSearchFlight_正常系_同時刻複数運賃種別() {
        // Given
        TicketSearchCriteriaDto criteria = createSearchCriteria();

        Route mockRoute = createMockRoute();
        when(routeProvider.getRouteByAirportCd("HND", "ITM")).thenReturn(mockRoute);

        List<Flight> mockFlights = createMockFlightsWithMultipleFareTypes();
        when(flightRepository.findByVacantSeatSearchCriteria(any())).thenReturn(mockFlights);

        setupMasterDataMocksForMultipleFareTypes();
        when(ticketSharedService.calculateBasicFare(anyInt(), any(), any())).thenReturn(10000);
        when(ticketSharedService.calculateFare(eq(10000), eq(20))).thenReturn(8000);
        when(ticketSharedService.calculateFare(eq(10000), eq(0))).thenReturn(10000);

        // When
        List<FlightVacantInfoDto> result = target.searchFlight(criteria);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1); // 同じ出発時刻なので1つにまとめられる
        FlightVacantInfoDto flightInfo = result.get(0);
        assertThat(flightInfo.getFareTypes()).hasSize(2); // 2つの運賃種別
    }

    /**
     * フライト検索_異常系: 搭乗日がnullの場合
     */
    @Test
    public void testSearchFlight_異常系_搭乗日がnull() {
        // Given
        TicketSearchCriteriaDto criteria = createSearchCriteria();
        criteria.setDepDate(null);

        // When & Then
        assertThatThrownBy(() -> target.searchFlight(criteria))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("depDate must not null");
    }

    /**
     * フライト検索_異常系: 搭乗クラスがnullの場合
     */
    @Test
    public void testSearchFlight_異常系_搭乗クラスがnull() {
        // Given
        TicketSearchCriteriaDto criteria = createSearchCriteria();
        criteria.setBoardingClassCd(null);

        // When & Then
        assertThatThrownBy(() -> target.searchFlight(criteria))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("boardingClassCd must not null");
    }

    /**
     * フライト検索_異常系: 出発空港コードが空の場合
     */
    @Test
    public void testSearchFlight_異常系_出発空港コードが空() {
        // Given
        TicketSearchCriteriaDto criteria = createSearchCriteria();
        criteria.setDepartureAirportCd("");

        // When & Then
        assertThatThrownBy(() -> target.searchFlight(criteria))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("depAirportCd must have some text");
    }

    /**
     * フライト検索_異常系: 到着空港コードが空の場合
     */
    @Test
    public void testSearchFlight_異常系_到着空港コードが空() {
        // Given
        TicketSearchCriteriaDto criteria = createSearchCriteria();
        criteria.setArrivalAirportCd("");

        // When & Then
        assertThatThrownBy(() -> target.searchFlight(criteria))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("arrAirportCd must have some text");
    }

    /**
     * フライト検索_異常系: フライトタイプがnullの場合
     */
    @Test
    public void testSearchFlight_異常系_フライトタイプがnull() {
        // Given
        TicketSearchCriteriaDto criteria = createSearchCriteria();
        criteria.setFlightType(null);

        // When & Then
        assertThatThrownBy(() -> target.searchFlight(criteria))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("flightType must not null");
    }

    /**
     * フライト検索_正常系: 残席数が0のフライトも取得できる
     */
    @Test
    public void testSearchFlight_正常系_残席数0() {
        // Given
        TicketSearchCriteriaDto criteria = createSearchCriteria();

        Route mockRoute = createMockRoute();
        when(routeProvider.getRouteByAirportCd("HND", "ITM")).thenReturn(mockRoute);

        List<Flight> mockFlights = createMockFlightsWithZeroVacancy();
        when(flightRepository.findByVacantSeatSearchCriteria(any())).thenReturn(mockFlights);

        setupMasterDataMocks();
        when(ticketSharedService.calculateBasicFare(anyInt(), any(), any())).thenReturn(10000);
        when(ticketSharedService.calculateFare(anyInt(), anyInt())).thenReturn(8000);

        // When
        List<FlightVacantInfoDto> result = target.searchFlight(criteria);

        // Then
        assertThat(result).isNotNull().isNotEmpty();
        FlightVacantInfoDto flightInfo = result.get(0);
        assertThat(flightInfo.getFareTypes().values())
                .extracting(FareTypeVacantInfoDto::getVacantNum)
                .contains(0);
    }

    // ==================== ヘルパーメソッド ====================

    private TicketSearchCriteriaDto createSearchCriteria() {
        TicketSearchCriteriaDto criteria = new TicketSearchCriteriaDto();
        criteria.setDepartureAirportCd("HND");
        criteria.setArrivalAirportCd("ITM");
        criteria.setDepDate(Date.from(LocalDate.of(2025, 12, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
        criteria.setBoardingClassCd(BoardingClassCd.N);
        criteria.setFlightType(FlightType.RT);
        return criteria;
    }

    private Route createMockRoute() {
        Route route = new Route();
        route.setRouteNo(1);
        route.setBasicFare(10000);

        Airport depAirport = new Airport();
        depAirport.setCode("HND");
        depAirport.setName("東京(羽田)");
        route.setDepartureAirport(depAirport);

        Airport arrAirport = new Airport();
        arrAirport.setCode("ITM");
        arrAirport.setName("大阪(伊丹)");
        route.setArrivalAirport(arrAirport);

        return route;
    }

    private List<Flight> createMockFlights() {
        Flight flight = new Flight();

        FlightMaster flightMaster = new FlightMaster();
        flightMaster.setFlightName("NTT001");
        flightMaster.setDepartureTime("0800");
        flightMaster.setArrivalTime("0930");
        flightMaster.setRoute(createMockRoute());
        flight.setFlightMaster(flightMaster);

        FareType fareType = new FareType();
        fareType.setFareTypeCd(FareTypeCd.RT);
        fareType.setFareTypeName("往復割引");
        fareType.setDiscountRate(20);
        flight.setFareType(fareType);

        BoardingClass boardingClass = new BoardingClass();
        boardingClass.setBoardingClassCd(BoardingClassCd.N);
        boardingClass.setBoardingClassName("普通席");
        flight.setBoardingClass(boardingClass);

        flight.setVacantNum(50);
        flight.setDepartureDate(Date.from(LocalDate.of(2025, 12, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()));

        return Arrays.asList(flight);
    }

    private void setupMasterDataMocks() {
        FareType fareType = new FareType();
        fareType.setFareTypeCd(FareTypeCd.RT);
        fareType.setFareTypeName("往復割引");
        fareType.setDiscountRate(20);
        when(fareTypeProvider.getFareType(any())).thenReturn(fareType);

        // 各フライト名に対応した FlightMaster を返すように設定
        when(flightMasterProvider.getFlightMaster("NTT001")).thenAnswer(invocation -> {
            FlightMaster fm = new FlightMaster();
            fm.setFlightName("NTT001");
            fm.setDepartureTime("0800");
            fm.setArrivalTime("0930");
            fm.setRoute(createMockRoute());
            return fm;
        });
        when(flightMasterProvider.getFlightMaster("NTT002")).thenAnswer(invocation -> {
            FlightMaster fm = new FlightMaster();
            fm.setFlightName("NTT002");
            fm.setDepartureTime("1200");
            fm.setArrivalTime("1330");
            fm.setRoute(createMockRoute());
            return fm;
        });
        when(flightMasterProvider.getFlightMaster("NTT003")).thenAnswer(invocation -> {
            FlightMaster fm = new FlightMaster();
            fm.setFlightName("NTT003");
            fm.setDepartureTime("1800");
            fm.setArrivalTime("1930");
            fm.setRoute(createMockRoute());
            return fm;
        });

        BoardingClass boardingClass = new BoardingClass();
        boardingClass.setBoardingClassCd(BoardingClassCd.N);
        boardingClass.setBoardingClassName("普通席");
        when(boardingClassProvider.getBoardingClass(any())).thenReturn(boardingClass);
    }

    private List<Flight> createMockFlightsForSpecialClass() {
        Flight flight = new Flight();

        FlightMaster flightMaster = new FlightMaster();
        flightMaster.setFlightName("NTT001");
        flightMaster.setDepartureTime("0800");
        flightMaster.setArrivalTime("0930");
        flightMaster.setRoute(createMockRoute());
        flight.setFlightMaster(flightMaster);

        FareType fareType = new FareType();
        fareType.setFareTypeCd(FareTypeCd.RT);
        fareType.setFareTypeName("往復割引");
        fareType.setDiscountRate(20);
        flight.setFareType(fareType);

        BoardingClass boardingClass = new BoardingClass();
        boardingClass.setBoardingClassCd(BoardingClassCd.S);
        boardingClass.setBoardingClassName("特別席");
        flight.setBoardingClass(boardingClass);

        flight.setVacantNum(20);
        flight.setDepartureDate(Date.from(LocalDate.of(2025, 12, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()));

        return Arrays.asList(flight);
    }

    private void setupMasterDataMocksForSpecialClass() {
        FareType fareType = new FareType();
        fareType.setFareTypeCd(FareTypeCd.RT);
        fareType.setFareTypeName("往復割引");
        fareType.setDiscountRate(20);
        when(fareTypeProvider.getFareType(any())).thenReturn(fareType);

        FlightMaster flightMaster = new FlightMaster();
        flightMaster.setFlightName("NTT001");
        flightMaster.setDepartureTime("0800");
        flightMaster.setArrivalTime("0930");
        flightMaster.setRoute(createMockRoute());
        when(flightMasterProvider.getFlightMaster(anyString())).thenReturn(flightMaster);

        BoardingClass boardingClass = new BoardingClass();
        boardingClass.setBoardingClassCd(BoardingClassCd.S);
        boardingClass.setBoardingClassName("特別席");
        when(boardingClassProvider.getBoardingClass(any())).thenReturn(boardingClass);
    }

    private List<Flight> createMultipleMockFlights() {
        Flight flight1 = createFlightWithTime("NTT001", "0800", "0930");
        Flight flight2 = createFlightWithTime("NTT002", "1200", "1330");
        Flight flight3 = createFlightWithTime("NTT003", "1800", "1930");

        return Arrays.asList(flight1, flight2, flight3);
    }

    private Flight createFlightWithTime(String flightName, String depTime, String arrTime) {
        Flight flight = new Flight();

        FlightMaster flightMaster = new FlightMaster();
        flightMaster.setFlightName(flightName);
        flightMaster.setDepartureTime(depTime);
        flightMaster.setArrivalTime(arrTime);
        flightMaster.setRoute(createMockRoute());
        flight.setFlightMaster(flightMaster);

        FareType fareType = new FareType();
        fareType.setFareTypeCd(FareTypeCd.RT);
        fareType.setFareTypeName("往復割引");
        fareType.setDiscountRate(20);
        flight.setFareType(fareType);

        BoardingClass boardingClass = new BoardingClass();
        boardingClass.setBoardingClassCd(BoardingClassCd.N);
        boardingClass.setBoardingClassName("普通席");
        flight.setBoardingClass(boardingClass);

        flight.setVacantNum(50);
        flight.setDepartureDate(Date.from(LocalDate.of(2025, 12, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()));

        return flight;
    }

    private List<Flight> createMockFlightsWithMultipleFareTypes() {
        Flight flight1 = createFlightWithFareType(FareTypeCd.RT, "往復割引", 20);
        Flight flight2 = createFlightWithFareType(FareTypeCd.OW, "片道運賃", 0);

        return Arrays.asList(flight1, flight2);
    }

    private Flight createFlightWithFareType(FareTypeCd fareTypeCd, String fareTypeName, int discountRate) {
        Flight flight = new Flight();

        FlightMaster flightMaster = new FlightMaster();
        flightMaster.setFlightName("NTT001");
        flightMaster.setDepartureTime("0800");
        flightMaster.setArrivalTime("0930");
        flightMaster.setRoute(createMockRoute());
        flight.setFlightMaster(flightMaster);

        FareType fareType = new FareType();
        fareType.setFareTypeCd(fareTypeCd);
        fareType.setFareTypeName(fareTypeName);
        fareType.setDiscountRate(discountRate);
        flight.setFareType(fareType);

        BoardingClass boardingClass = new BoardingClass();
        boardingClass.setBoardingClassCd(BoardingClassCd.N);
        boardingClass.setBoardingClassName("普通席");
        flight.setBoardingClass(boardingClass);

        flight.setVacantNum(50);
        flight.setDepartureDate(Date.from(LocalDate.of(2025, 12, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()));

        return flight;
    }

    private void setupMasterDataMocksForMultipleFareTypes() {
        FareType fareTypeRT = new FareType();
        fareTypeRT.setFareTypeCd(FareTypeCd.RT);
        fareTypeRT.setFareTypeName("往復割引");
        fareTypeRT.setDiscountRate(20);

        FareType fareTypeOW = new FareType();
        fareTypeOW.setFareTypeCd(FareTypeCd.OW);
        fareTypeOW.setFareTypeName("片道運賃");
        fareTypeOW.setDiscountRate(0);

        when(fareTypeProvider.getFareType(FareTypeCd.RT)).thenReturn(fareTypeRT);
        when(fareTypeProvider.getFareType(FareTypeCd.OW)).thenReturn(fareTypeOW);

        FlightMaster flightMaster = new FlightMaster();
        flightMaster.setFlightName("NTT001");
        flightMaster.setDepartureTime("0800");
        flightMaster.setArrivalTime("0930");
        flightMaster.setRoute(createMockRoute());
        when(flightMasterProvider.getFlightMaster(anyString())).thenReturn(flightMaster);

        BoardingClass boardingClass = new BoardingClass();
        boardingClass.setBoardingClassCd(BoardingClassCd.N);
        boardingClass.setBoardingClassName("普通席");
        when(boardingClassProvider.getBoardingClass(any())).thenReturn(boardingClass);
    }

    private List<Flight> createMockFlightsWithZeroVacancy() {
        Flight flight = new Flight();

        FlightMaster flightMaster = new FlightMaster();
        flightMaster.setFlightName("NTT001");
        flightMaster.setDepartureTime("0800");
        flightMaster.setArrivalTime("0930");
        flightMaster.setRoute(createMockRoute());
        flight.setFlightMaster(flightMaster);

        FareType fareType = new FareType();
        fareType.setFareTypeCd(FareTypeCd.RT);
        fareType.setFareTypeName("往復割引");
        fareType.setDiscountRate(20);
        flight.setFareType(fareType);

        BoardingClass boardingClass = new BoardingClass();
        boardingClass.setBoardingClassCd(BoardingClassCd.N);
        boardingClass.setBoardingClassName("普通席");
        flight.setBoardingClass(boardingClass);

        flight.setVacantNum(0); // 残席なし
        flight.setDepartureDate(Date.from(LocalDate.of(2025, 12, 1).atStartOfDay(ZoneId.systemDefault()).toInstant()));

        return Arrays.asList(flight);
    }

    // ==================== USD換算テストケース ====================

    /**
     * ドル換算の正常系テスト - 基本的な換算が正しく行われることを確認
     */
    @Test
    public void testSearchFlight_正常系_ドル換算が正しい() {
        // Given: 検索条件とモックの準備
        TicketSearchCriteriaDto criteria = createSearchCriteria();
        Route mockRoute = createMockRoute();
        when(routeProvider.getRouteByAirportCd("HND", "ITM")).thenReturn(mockRoute);

        List<Flight> mockFlights = createMockFlights();
        when(flightRepository.findByVacantSeatSearchCriteria(any())).thenReturn(mockFlights);

        setupMasterDataMocks();
        when(ticketSharedService.calculateBasicFare(anyInt(), any(), any())).thenReturn(10000);
        when(ticketSharedService.calculateFare(anyInt(), anyInt())).thenReturn(8000);

        // ★ドル換算のモック: 8000円 ÷ 150 = 53.33... → 54ドル(切り上げ)
        when(ticketSharedService.convertYenToUsd(8000)).thenReturn(54);

        // When: 検索実行
        List<FlightVacantInfoDto> result = target.searchFlight(criteria);

        // Then: ドル換算結果の検証
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);

        FlightVacantInfoDto flightInfo = result.get(0);
        assertThat(flightInfo.getFareTypes()).isNotEmpty();

        FareTypeVacantInfoDto fareInfo = flightInfo.getFareTypes().values().iterator().next();
        assertThat(fareInfo.getFare()).isEqualTo("8,000"); // 円表示
        assertThat(fareInfo.getFareUsd()).isEqualTo("$54"); // ドル表示

        // モック呼び出し検証
        verify(ticketSharedService).convertYenToUsd(8000);
    }

    /**
     * ドル換算の正常系テスト - 複数の運賃種別でドル換算が正しく行われることを確認
     */
    @Test
    public void testSearchFlight_正常系_複数運賃種別のドル換算() {
        // Given: 複数運賃種別の検索条件
        TicketSearchCriteriaDto criteria = createSearchCriteria();
        Route mockRoute = createMockRoute();
        when(routeProvider.getRouteByAirportCd("HND", "ITM")).thenReturn(mockRoute);

        // 異なる運賃種別のフライト
        List<Flight> mockFlights = createMockFlightsWithMultipleFareTypes();
        when(flightRepository.findByVacantSeatSearchCriteria(any())).thenReturn(mockFlights);

        setupMasterDataMocksForMultipleFareTypes();
        when(ticketSharedService.calculateBasicFare(anyInt(), any(), any())).thenReturn(10000);

        // 運賃種別ごとの運賃とドル換算
        when(ticketSharedService.calculateFare(10000, 20)).thenReturn(8000); // RT: 20%割引
        when(ticketSharedService.calculateFare(10000, 0)).thenReturn(10000); // OW: 割引なし
        when(ticketSharedService.convertYenToUsd(8000)).thenReturn(54);  // $54
        when(ticketSharedService.convertYenToUsd(10000)).thenReturn(67); // $67

        // When: 検索実行
        List<FlightVacantInfoDto> result = target.searchFlight(criteria);

        // Then: 各運賃種別のドル換算を検証
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);

        FlightVacantInfoDto flightInfo = result.get(0);
        assertThat(flightInfo.getFareTypes()).hasSize(2);

        // RT運賃の検証
        FareTypeVacantInfoDto rtFare = flightInfo.getFareTypes().get(FareTypeCd.RT.name());
        assertThat(rtFare.getFare()).isEqualTo("8,000");
        assertThat(rtFare.getFareUsd()).isEqualTo("$54");

        // OW運賃の検証
        FareTypeVacantInfoDto owFare = flightInfo.getFareTypes().get(FareTypeCd.OW.name());
        assertThat(owFare.getFare()).isEqualTo("10,000");
        assertThat(owFare.getFareUsd()).isEqualTo("$67");

        // モック呼び出し検証
        verify(ticketSharedService).convertYenToUsd(8000);
        verify(ticketSharedService).convertYenToUsd(10000);
    }

    /**
     * ドル換算の正常系テスト - 端数切り上げの確認
     */
    @Test
    public void testSearchFlight_正常系_ドル換算の端数切り上げ() {
        // Given: 検索条件とモックの準備
        TicketSearchCriteriaDto criteria = createSearchCriteria();
        Route mockRoute = createMockRoute();
        when(routeProvider.getRouteByAirportCd("HND", "ITM")).thenReturn(mockRoute);

        List<Flight> mockFlights = createMockFlights();
        when(flightRepository.findByVacantSeatSearchCriteria(any())).thenReturn(mockFlights);

        setupMasterDataMocks();
        when(ticketSharedService.calculateBasicFare(anyInt(), any(), any())).thenReturn(10000);
        when(ticketSharedService.calculateFare(anyInt(), anyInt())).thenReturn(15050); // 端数あり

        // ★ドル換算のモック: 15050円 ÷ 150 = 100.33... → 101ドル(切り上げ)
        when(ticketSharedService.convertYenToUsd(15050)).thenReturn(101);

        // When: 検索実行
        List<FlightVacantInfoDto> result = target.searchFlight(criteria);

        // Then: 端数切り上げの検証
        assertThat(result).isNotNull();
        FareTypeVacantInfoDto fareInfo = result.get(0).getFareTypes().values().iterator().next();
        assertThat(fareInfo.getFare()).isEqualTo("15,050");
        assertThat(fareInfo.getFareUsd()).isEqualTo("$101"); // 切り上げ確認

        verify(ticketSharedService).convertYenToUsd(15050);
    }

    /**
     * 香港ドル換算の正常系テスト - HKD換算が正しく行われることを確認
     */
    @Test
    public void testSearchFlight_正常系_HKD換算が正しい() {
        // Given: 基本的な検索条件とモックデータの設定
        TicketSearchCriteriaDto criteria = createSearchCriteria();
        Route mockRoute = createMockRoute();
        when(routeProvider.getRouteByAirportCd("HND", "ITM")).thenReturn(mockRoute);

        List<Flight> mockFlights = createMockFlights();
        when(flightRepository.findByVacantSeatSearchCriteria(any())).thenReturn(mockFlights);

        setupMasterDataMocks();
        when(ticketSharedService.calculateBasicFare(anyInt(), any(), any())).thenReturn(10000);
        when(ticketSharedService.calculateFare(anyInt(), anyInt())).thenReturn(8000);

        // ★ドル換算のモック: 8000円 ÷ 150 = 53.33... → 54ドル(切り上げ)
        when(ticketSharedService.convertYenToUsd(8000)).thenReturn(54);
        
        // ★香港ドル換算のモック: 8000円 ÷ 19.35 = 413.43... → 414 HKD(切り上げ)
        when(ticketSharedService.convertYenToHkd(8000)).thenReturn(414);

        // When: 検索実行
        List<FlightVacantInfoDto> result = target.searchFlight(criteria);

        // Then: 香港ドル換算結果の検証
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);

        FlightVacantInfoDto flightInfo = result.get(0);
        assertThat(flightInfo.getFareTypes()).isNotEmpty();

        FareTypeVacantInfoDto fareInfo = flightInfo.getFareTypes().values().iterator().next();
        assertThat(fareInfo.getFare()).isEqualTo("8,000"); // 円表示
        assertThat(fareInfo.getFareUsd()).isEqualTo("$54"); // ドル表示
        assertThat(fareInfo.getFareHkd()).isEqualTo("HK$414"); // 香港ドル表示

        // モック呼び出し検証
        verify(ticketSharedService).convertYenToUsd(8000);
        verify(ticketSharedService).convertYenToHkd(8000);
    }

    /**
     * 香港ドル換算の正常系テスト - 複数運賃種別でHKD換算が正しく行われることを確認
     */
    @Test
    public void testSearchFlight_正常系_複数運賃種別のHKD換算() {
        // Given: 複数運賃種別の検索条件
        TicketSearchCriteriaDto criteria = createSearchCriteria();
        Route mockRoute = createMockRoute();
        when(routeProvider.getRouteByAirportCd("HND", "ITM")).thenReturn(mockRoute);

        // 異なる運賃種別のフライト
        List<Flight> mockFlights = createMockFlightsWithMultipleFareTypes();
        when(flightRepository.findByVacantSeatSearchCriteria(any())).thenReturn(mockFlights);

        setupMasterDataMocksForMultipleFareTypes();
        when(ticketSharedService.calculateBasicFare(anyInt(), any(), any())).thenReturn(10000);

        // 運賃種別ごとの運賃とドル・香港ドル換算
        when(ticketSharedService.calculateFare(10000, 20)).thenReturn(8000); // RT: 20%割引
        when(ticketSharedService.calculateFare(10000, 0)).thenReturn(10000); // OW: 割引なし
        when(ticketSharedService.convertYenToUsd(8000)).thenReturn(54);  // $54
        when(ticketSharedService.convertYenToUsd(10000)).thenReturn(67); // $67
        when(ticketSharedService.convertYenToHkd(8000)).thenReturn(414);  // HK$414
        when(ticketSharedService.convertYenToHkd(10000)).thenReturn(517); // HK$517

        // When: 検索実行
        List<FlightVacantInfoDto> result = target.searchFlight(criteria);

        // Then: 複数運賃種別の香港ドル換算結果検証
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);

        FlightVacantInfoDto flightInfo = result.get(0);
        Map<String, FareTypeVacantInfoDto> fareTypes = flightInfo.getFareTypes();
        assertThat(fareTypes).hasSize(2);

        // RT(往復割引)の検証
        FareTypeVacantInfoDto rtFare = fareTypes.get("RT");
        assertThat(rtFare.getFare()).isEqualTo("8,000");
        assertThat(rtFare.getFareUsd()).isEqualTo("$54");
        assertThat(rtFare.getFareHkd()).isEqualTo("HK$414");

        // OW(片道)の検証
        FareTypeVacantInfoDto owFare = fareTypes.get("OW");
        assertThat(owFare.getFare()).isEqualTo("10,000");
        assertThat(owFare.getFareUsd()).isEqualTo("$67");
        assertThat(owFare.getFareHkd()).isEqualTo("HK$517");

        // モック呼び出し検証
        verify(ticketSharedService, times(2)).convertYenToUsd(anyInt());
        verify(ticketSharedService, times(2)).convertYenToHkd(anyInt());
    }
}
