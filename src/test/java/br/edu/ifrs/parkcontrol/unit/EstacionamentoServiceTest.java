package br.edu.ifrs.parkcontrol.unit;

import br.edu.ifrs.parkcontrol.exception.TicketNaoEncontradoException;
import br.edu.ifrs.parkcontrol.exception.VagaIndisponivelException;
import br.edu.ifrs.parkcontrol.exception.VeiculoJaEstacionadoException;
import br.edu.ifrs.parkcontrol.model.Ticket;
import br.edu.ifrs.parkcontrol.model.TipoVaga;
import br.edu.ifrs.parkcontrol.model.Vaga;
import br.edu.ifrs.parkcontrol.model.Veiculo;
import br.edu.ifrs.parkcontrol.repository.TicketRepository;
import br.edu.ifrs.parkcontrol.repository.VagaRepository;
import br.edu.ifrs.parkcontrol.service.CalculadoraDeValor;
import br.edu.ifrs.parkcontrol.service.EstacionamentoService;
import br.edu.ifrs.parkcontrol.service.TarifaConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitarios de EstacionamentoService, isolando as dependencias
 * externas de persistencia (VagaRepository, TicketRepository) com Mockito.
 * A CalculadoraDeValor e usada em sua implementacao real: e um colaborador
 * puro (sem I/O), ja validado isoladamente em CalculadoraDeValorTest, e seu
 * uso aqui torna estes testes uma verificacao mais fiel da orquestracao real.
 */
@ExtendWith(MockitoExtension.class)
class EstacionamentoServiceTest {

    private static final Instant AGORA = Instant.parse("2026-01-10T08:00:00Z");
    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");

    @Mock
    private VagaRepository vagaRepository;

    @Mock
    private TicketRepository ticketRepository;

    private EstacionamentoService service;

    @BeforeEach
    void setUp() {
        CalculadoraDeValor calculadoraDeValor = new CalculadoraDeValor(TarifaConfig.padrao());
        Clock clockFixo = Clock.fixed(AGORA, ZONA);
        service = new EstacionamentoService(vagaRepository, ticketRepository, calculadoraDeValor, clockFixo);
    }

    @Test
    @DisplayName("Registra entrada com sucesso quando ha vaga compativel disponivel")
    void registrarEntradaComSucesso() {
        Veiculo veiculo = new Veiculo("ABC1D23", TipoVaga.COMUM, false);
        Vaga vagaLivre = new Vaga(1, TipoVaga.COMUM);

        when(ticketRepository.buscarTicketAbertoPorPlaca("ABC1D23")).thenReturn(Optional.empty());
        when(vagaRepository.listarDisponiveisPorTipo(TipoVaga.COMUM)).thenReturn(List.of(vagaLivre));

        Ticket ticket = service.registrarEntrada(veiculo);

        assertEquals("ABC1D23", ticket.getPlaca());
        assertEquals(1, ticket.getNumeroVaga());
        verify(vagaRepository).salvar(vagaLivre);
        verify(ticketRepository).salvar(ticket);
    }

    @Test
    @DisplayName("Veiculo mensalista e alocado em vaga do tipo MENSALISTA, nao no seu tipo de cliente")
    void registrarEntradaMensalistaUsaVagaMensalista() {
        Veiculo veiculo = new Veiculo("MEN1S23", TipoVaga.COMUM, true);
        Vaga vagaMensalista = new Vaga(20, TipoVaga.MENSALISTA);

        when(ticketRepository.buscarTicketAbertoPorPlaca("MEN1S23")).thenReturn(Optional.empty());
        when(vagaRepository.listarDisponiveisPorTipo(TipoVaga.MENSALISTA)).thenReturn(List.of(vagaMensalista));

        Ticket ticket = service.registrarEntrada(veiculo);

        assertEquals(20, ticket.getNumeroVaga());
        verify(vagaRepository, never()).listarDisponiveisPorTipo(TipoVaga.COMUM);
    }

    @Test
    @DisplayName("Rejeita entrada se o veiculo ja possui ticket aberto")
    void registrarEntradaRejeitaVeiculoJaEstacionado() {
        Veiculo veiculo = new Veiculo("ABC1D23", TipoVaga.COMUM, false);
        Ticket ticketExistente = new Ticket("t1", "ABC1D23", 1, AGORA.atZone(ZONA).toLocalDateTime(), false);

        when(ticketRepository.buscarTicketAbertoPorPlaca("ABC1D23")).thenReturn(Optional.of(ticketExistente));

        assertThrows(VeiculoJaEstacionadoException.class, () -> service.registrarEntrada(veiculo));
        verify(vagaRepository, never()).salvar(any());
        verify(ticketRepository, never()).salvar(any());
    }

    @Test
    @DisplayName("Rejeita entrada se nao ha vaga disponivel do tipo exigido")
    void registrarEntradaRejeitaSemVagaDisponivel() {
        Veiculo veiculo = new Veiculo("ABC1D23", TipoVaga.PCD, false);

        when(ticketRepository.buscarTicketAbertoPorPlaca("ABC1D23")).thenReturn(Optional.empty());
        when(vagaRepository.listarDisponiveisPorTipo(TipoVaga.PCD)).thenReturn(List.of());

        assertThrows(VagaIndisponivelException.class, () -> service.registrarEntrada(veiculo));
        verify(ticketRepository, never()).salvar(any());
    }

    @Test
    @DisplayName("Registra saida com sucesso, calcula valor e libera a vaga")
    void registrarSaidaComSucesso() {
        var entradaEm = AGORA.minusSeconds(3 * 3600).atZone(ZONA).toLocalDateTime(); // 3h atras
        Ticket ticketAberto = new Ticket("t1", "ABC1D23", 5, entradaEm, false);
        Vaga vagaOcupada = new Vaga(5, TipoVaga.COMUM);
        vagaOcupada.ocupar("ABC1D23");

        when(ticketRepository.buscarTicketAbertoPorPlaca("ABC1D23")).thenReturn(Optional.of(ticketAberto));
        when(vagaRepository.buscarPorNumero(5)).thenReturn(Optional.of(vagaOcupada));

        Ticket ticketFinalizado = service.registrarSaida("ABC1D23");

        assertEquals(new BigDecimal("18.00"), ticketFinalizado.getValorCobrado());
        verify(vagaRepository).salvar(vagaOcupada);
        verify(ticketRepository).salvar(ticketAberto);
    }

    @Test
    @DisplayName("Rejeita saida se nao ha ticket aberto para a placa")
    void registrarSaidaRejeitaTicketNaoEncontrado() {
        when(ticketRepository.buscarTicketAbertoPorPlaca("XYZ9W88")).thenReturn(Optional.empty());

        assertThrows(TicketNaoEncontradoException.class, () -> service.registrarSaida("XYZ9W88"));
    }

    @Test
    @DisplayName("Consulta de disponibilidade delega para o repositorio e retorna a contagem")
    void consultarDisponibilidadeDelegaParaRepositorio() {
        when(vagaRepository.listarDisponiveisPorTipo(TipoVaga.IDOSO))
                .thenReturn(List.of(new Vaga(7, TipoVaga.IDOSO), new Vaga(8, TipoVaga.IDOSO)));

        long disponiveis = service.consultarDisponibilidade(TipoVaga.IDOSO);

        assertEquals(2, disponiveis);
    }
}
