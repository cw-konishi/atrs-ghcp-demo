package jp.co.ntt.atrs.domain.service.b2;

import jp.co.ntt.atrs.domain.common.masterdata.BoardingClassProvider;
import jp.co.ntt.atrs.domain.common.masterdata.FareTypeProvider;
import jp.co.ntt.atrs.domain.common.masterdata.FlightMasterProvider;
import jp.co.ntt.atrs.domain.common.masterdata.RouteProvider;
import jp.co.ntt.atrs.domain.common.util.DateTimeUtil;
import jp.co.ntt.atrs.domain.repository.member.MemberRepository;
import jp.co.ntt.atrs.domain.repository.reservation.ReservationRepository;
import jp.co.ntt.atrs.domain.service.b0.TicketSharedService;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * TicketReserveServiceImplの単体テストクラス (スケルトン)
 * 
 * <p>
 * TODO: TDD実演用テストケース実装予定
 * - calculateTotalFare() の各種テスト
 * - validateReservation() の各種テスト
 * - registerReservation() の各種テスト
 * - findMember() の各種テスト
 * </p>
 */
@RunWith(MockitoJUnitRunner.class)
public class TicketReserveServiceImplTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private TicketSharedService ticketSharedService;

    @Mock
    private RouteProvider routeProvider;

    @Mock
    private FareTypeProvider fareTypeProvider;

    @Mock
    private FlightMasterProvider flightMasterProvider;

    @Mock
    private BoardingClassProvider boardingClassProvider;

    @Mock
    private DateTimeUtil dateTimeUtil;

    @InjectMocks
    private TicketReserveServiceImpl target;

    @Before
    public void setUp() {
        // TODO: 共通セットアップ処理
    }
}
