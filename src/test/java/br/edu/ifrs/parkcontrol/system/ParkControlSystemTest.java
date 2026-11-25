package br.edu.ifrs.parkcontrol.system;

import br.edu.ifrs.parkcontrol.cli.ParkControlApp;
import br.edu.ifrs.parkcontrol.model.TipoVaga;
import br.edu.ifrs.parkcontrol.model.Vaga;
import br.edu.ifrs.parkcontrol.repository.TicketRepository;
import br.edu.ifrs.parkcontrol.repository.VagaRepository;
import br.edu.ifrs.parkcontrol.repository.impl.ArquivoTicketRepository;
import br.edu.ifrs.parkcontrol.repository.impl.ArquivoVagaRepository;
import br.edu.ifrs.parkcontrol.service.CalculadoraDeValor;
import br.edu.ifrs.parkcontrol.service.EstacionamentoService;
import br.edu.ifrs.parkcontrol.service.TarifaConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Teste de sistema: exercita o ParkControl de ponta a ponta do ponto de vista
 * do usuario final, exatamente como ele seria operado na pratica - enviando
 * comandos pela entrada padrao (stdin) e verificando o que aparece na saida
 * padrao (stdout). Usa a persistencia em arquivo real (nao mocks) para que o
 * fluxo seja identico ao de producao; o unico elemento controlado e o
 * relogio (Clock fixo), para que o calculo de valor seja deterministico.
 */
class ParkControlSystemTest {

    private static final Instant AGORA = Instant.parse("2026-01-10T12:00:00Z");
    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");

    @TempDir
    Path tempDir;

    private VagaRepository vagaRepository;
    private TicketRepository ticketRepository;

    @BeforeEach
    void setUp() {
        vagaRepository = new ArquivoVagaRepository(tempDir.resolve("vagas.json"));
        ticketRepository = new ArquivoTicketRepository(tempDir.resolve("tickets.json"));

        // Cenario de teste: estacionamento pequeno e previsivel
        vagaRepository.salvar(new Vaga(1, TipoVaga.COMUM));
        vagaRepository.salvar(new Vaga(2, TipoVaga.COMUM));
        vagaRepository.salvar(new Vaga(3, TipoVaga.PCD));
        vagaRepository.salvar(new Vaga(4, TipoVaga.MENSALISTA));
    }

    private String executar(String comandos) {
        return executar(comandos, Clock.fixed(AGORA, ZONA));
    }

    /**
     * Executa uma sessao da CLI com um relogio especifico. Usado para simular
     * a passagem de tempo real entre duas interacoes do usuario (por exemplo,
     * "entrada" em um horario e "saida" tres horas depois), reaproveitando o
     * mesmo estado persistido em arquivo entre as sessoes - assim como
     * aconteceria em uso real, onde cada comando pode ocorrer em um momento
     * diferente.
     */
    private String executar(String comandos, Clock clock) {
        ByteArrayOutputStream bufferSaida = new ByteArrayOutputStream();
        PrintStream saida = new PrintStream(bufferSaida, true, StandardCharsets.UTF_8);
        Scanner entrada = new Scanner(new ByteArrayInputStream(comandos.getBytes(StandardCharsets.UTF_8)));

        CalculadoraDeValor calculadoraDeValor = new CalculadoraDeValor(TarifaConfig.padrao());
        EstacionamentoService service = new EstacionamentoService(
                vagaRepository, ticketRepository, calculadoraDeValor, clock);

        ParkControlApp app = new ParkControlApp(service, entrada, saida);
        app.executar();

        return bufferSaida.toString(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("Fluxo principal: entrada, consulta de disponibilidade e saida com cobranca")
    void fluxoPrincipalCompleto() {
        String saida = executar("""
                disponibilidade COMUM
                entrada ABC1D23 COMUM n
                disponibilidade COMUM
                saida ABC1D23
                sair
                """);

        assertTrue(saida.contains("Vagas disponiveis do tipo COMUM: 2"));
        assertTrue(saida.contains("Entrada registrada. Placa=ABC1D23 vaga=1"));
        assertTrue(saida.contains("Vagas disponiveis do tipo COMUM: 1"));
        assertTrue(saida.contains("Saida registrada. Placa=ABC1D23 valor cobrado=R$"));
        assertTrue(saida.contains("Encerrando ParkControl"));
    }

    @Test
    @DisplayName("Fluxo alternativo: tentar entrada de veiculo ja estacionado deve ser rejeitada")
    void fluxoAlternativoVeiculoJaEstacionado() {
        String saida = executar("""
                entrada ABC1D23 COMUM n
                entrada ABC1D23 COMUM n
                sair
                """);

        assertTrue(saida.contains("ERRO:"));
        assertTrue(saida.contains("ja esta estacionado"));
    }

    @Test
    @DisplayName("Fluxo alternativo: saida de veiculo sem entrada registrada deve ser rejeitada")
    void fluxoAlternativoSaidaSemEntrada() {
        String saida = executar("""
                saida NAOEXISTE
                sair
                """);

        assertTrue(saida.contains("ERRO:"));
        assertTrue(saida.contains("Nenhum ticket aberto"));
    }

    @Test
    @DisplayName("Fluxo alternativo: entrada rejeitada quando nao ha vaga do tipo disponivel")
    void fluxoAlternativoSemVagaDisponivel() {
        // So existe 1 vaga PCD (numero 3); apos ocupa-la, a proxima entrada deve falhar
        String saida = executar("""
                entrada PCD0001 PCD n
                entrada PCD0002 PCD n
                sair
                """);

        assertTrue(saida.contains("Entrada registrada. Placa=PCD0001"));
        assertTrue(saida.contains("ERRO:"));
        assertTrue(saida.contains("Nao ha vagas disponiveis do tipo PCD"));
    }

    @Test
    @DisplayName("Veiculo mensalista permanece 3h e e cobrado com desconto na saida")
    void fluxoMensalistaComDesconto() {
        String saidaEntrada = executar("""
                entrada MEN0001 COMUM s
                sair
                """, Clock.fixed(AGORA, ZONA));
        assertTrue(saidaEntrada.contains("Entrada registrada. Placa=MEN0001 vaga=4"));

        // Mesmo veiculo, 3 horas depois (mesmo estado persistido, sessao da CLI diferente)
        String saidaSaida = executar("""
                saida MEN0001
                sair
                """, Clock.fixed(AGORA.plusSeconds(3 * 3600), ZONA));

        // 3h * R$ 6,00/h = R$ 18,00; mensalista tem 50% de desconto = R$ 9,00
        assertTrue(saidaSaida.contains("Saida registrada. Placa=MEN0001 valor cobrado=R$ 9.00"));
    }

    @Test
    @DisplayName("Comando desconhecido exibe mensagem de erro amigavel e nao encerra o programa")
    void comandoDesconhecidoExibeMensagem() {
        String saida = executar("""
                xyz
                ajuda
                sair
                """);

        assertTrue(saida.contains("Comando desconhecido: 'xyz'"));
        assertTrue(saida.contains("Comandos disponiveis:"));
    }
}
