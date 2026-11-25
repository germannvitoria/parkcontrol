package br.edu.ifrs.parkcontrol.component;

import br.edu.ifrs.parkcontrol.model.Ticket;
import br.edu.ifrs.parkcontrol.repository.TicketRepository;
import br.edu.ifrs.parkcontrol.repository.impl.ArquivoTicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Teste de componente do repositorio de tickets em arquivo, cobrindo
 * persistencia de datas/horas (LocalDateTime) e valores (BigDecimal),
 * e a busca especifica por "ticket aberto" (sem saida registrada).
 */
class ArquivoTicketRepositoryTest {

    @TempDir
    Path tempDir;

    private TicketRepository repository;

    @BeforeEach
    void setUp() {
        repository = new ArquivoTicketRepository(tempDir.resolve("tickets.json"));
    }

    @Test
    @DisplayName("Ticket aberto (sem saida) e encontrado pela busca por placa")
    void encontraTicketAbertoPorPlaca() {
        Ticket ticket = new Ticket("t1", "ABC1D23", 1, LocalDateTime.of(2026, 1, 10, 8, 0), false);
        repository.salvar(ticket);

        Optional<Ticket> encontrado = repository.buscarTicketAbertoPorPlaca("ABC1D23");

        assertTrue(encontrado.isPresent());
        assertEquals("t1", encontrado.get().getId());
        assertTrue(encontrado.get().isAberto());
    }

    @Test
    @DisplayName("Ticket com saida registrada nao e retornado pela busca de ticket aberto")
    void ticketComSaidaNaoERetornadoComoAberto() {
        Ticket ticket = new Ticket("t1", "ABC1D23", 1, LocalDateTime.of(2026, 1, 10, 8, 0), false);
        ticket.registrarSaida(LocalDateTime.of(2026, 1, 10, 10, 0), new BigDecimal("12.00"));
        repository.salvar(ticket);

        assertFalse(repository.buscarTicketAbertoPorPlaca("ABC1D23").isPresent());
    }

    @Test
    @DisplayName("Dados completos do ticket (datas e valor) persistem corretamente entre instancias")
    void persisteDatasEValorCorretamenteEntreInstancias() {
        Path arquivo = tempDir.resolve("tickets2.json");
        TicketRepository repo = new ArquivoTicketRepository(arquivo);

        Ticket ticket = new Ticket("t1", "ABC1D23", 3, LocalDateTime.of(2026, 1, 10, 8, 0), true);
        ticket.registrarSaida(LocalDateTime.of(2026, 1, 10, 11, 30), new BigDecimal("9.00"));
        repo.salvar(ticket);

        TicketRepository novaInstancia = new ArquivoTicketRepository(arquivo);
        List<Ticket> todos = novaInstancia.listarTodos();

        assertEquals(1, todos.size());
        Ticket recuperado = todos.get(0);
        assertEquals(LocalDateTime.of(2026, 1, 10, 8, 0), recuperado.getEntrada());
        assertEquals(LocalDateTime.of(2026, 1, 10, 11, 30), recuperado.getSaida());
        assertEquals(new BigDecimal("9.00"), recuperado.getValorCobrado());
        assertTrue(recuperado.isMensalista());
    }

    @Test
    @DisplayName("Salvar um ticket ja existente (mesmo id) atualiza o registro, nao duplica")
    void salvarTicketExistenteAtualizaSemDuplicar() {
        Ticket ticket = new Ticket("t1", "ABC1D23", 1, LocalDateTime.of(2026, 1, 10, 8, 0), false);
        repository.salvar(ticket);

        ticket.registrarSaida(LocalDateTime.of(2026, 1, 10, 9, 0), new BigDecimal("6.00"));
        repository.salvar(ticket);

        List<Ticket> todos = repository.listarTodos();
        assertEquals(1, todos.size());
        assertFalse(todos.get(0).isAberto());
    }

    @Test
    @DisplayName("Busca por placa sem ticket aberto retorna Optional vazio")
    void buscaPorPlacaSemTicketRetornaVazio() {
        assertTrue(repository.buscarTicketAbertoPorPlaca("NAOEXISTE").isEmpty());
    }
}
