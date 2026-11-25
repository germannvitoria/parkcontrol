package br.edu.ifrs.parkcontrol.cli;

import br.edu.ifrs.parkcontrol.exception.TicketNaoEncontradoException;
import br.edu.ifrs.parkcontrol.exception.VagaIndisponivelException;
import br.edu.ifrs.parkcontrol.exception.VeiculoJaEstacionadoException;
import br.edu.ifrs.parkcontrol.model.Ticket;
import br.edu.ifrs.parkcontrol.model.TipoVaga;
import br.edu.ifrs.parkcontrol.model.Vaga;
import br.edu.ifrs.parkcontrol.model.Veiculo;
import br.edu.ifrs.parkcontrol.service.EstacionamentoService;

import java.io.PrintStream;
import java.util.Locale;
import java.util.Scanner;

/**
 * Interface de linha de comando do ParkControl. Representa o "ponto de vista
 * do usuario final" do sistema: todo o comportamento observavel do software
 * passa por aqui, o que permite testes de sistema ponta-a-ponta simulando
 * entrada (stdin) e verificando a saida (stdout).
 */
public class ParkControlApp {

    static final String PROMPT = "parkcontrol> ";
    static final String MSG_AJUDA = """
            Comandos disponiveis:
              entrada <placa> <COMUM|PCD|IDOSO> <s|n>   - registra entrada (s/n = e mensalista?)
              saida <placa>                              - registra saida e calcula valor
              disponibilidade <COMUM|PCD|IDOSO|MENSALISTA> - consulta vagas livres do tipo
              listar                                      - lista todas as vagas e status
              ajuda                                       - mostra esta mensagem
              sair                                        - encerra o programa
            """;

    private final EstacionamentoService service;
    private final Scanner entrada;
    private final PrintStream saida;

    public ParkControlApp(EstacionamentoService service, Scanner entrada, PrintStream saida) {
        this.service = service;
        this.entrada = entrada;
        this.saida = saida;
    }

    /**
     * Executa o loop principal de leitura de comandos ate que o comando
     * "sair" seja recebido ou a entrada se esgote.
     */
    public void executar() {
        saida.println("=== ParkControl - Sistema de Controle de Estacionamento ===");
        saida.println(MSG_AJUDA);

        while (true) {
            saida.print(PROMPT);
            if (!entrada.hasNextLine()) {
                break;
            }
            String linha = entrada.nextLine().trim();
            if (linha.isEmpty()) {
                continue;
            }
            if (!processarComando(linha)) {
                break;
            }
        }
        saida.println("Encerrando ParkControl. Ate logo!");
    }

    /**
     * Processa uma linha de comando.
     *
     * @return false se o programa deve encerrar, true caso contrario
     */
    boolean processarComando(String linha) {
        String[] partes = linha.split("\\s+");
        String comando = partes[0].toLowerCase(Locale.ROOT);

        try {
            switch (comando) {
                case "sair" -> {
                    return false;
                }
                case "ajuda" -> saida.println(MSG_AJUDA);
                case "entrada" -> processarEntrada(partes);
                case "saida" -> processarSaida(partes);
                case "disponibilidade" -> processarDisponibilidade(partes);
                case "listar" -> processarListar();
                default -> saida.println("Comando desconhecido: '" + comando + "'. Digite 'ajuda' para ver os comandos.");
            }
        } catch (VeiculoJaEstacionadoException | VagaIndisponivelException
                 | TicketNaoEncontradoException e) {
            saida.println("ERRO: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            saida.println("ERRO: entrada invalida - " + e.getMessage());
        }
        return true;
    }

    private void processarEntrada(String[] partes) {
        if (partes.length != 4) {
            saida.println("Uso: entrada <placa> <COMUM|PCD|IDOSO> <s|n>");
            return;
        }
        String placa = partes[1].toUpperCase(Locale.ROOT);
        TipoVaga tipoCliente = TipoVaga.valueOf(partes[2].toUpperCase(Locale.ROOT));
        boolean mensalista = parseSimNao(partes[3]);

        Veiculo veiculo = new Veiculo(placa, tipoCliente, mensalista);
        Ticket ticket = service.registrarEntrada(veiculo);
        saida.println("Entrada registrada. Placa=" + ticket.getPlaca()
                + " vaga=" + ticket.getNumeroVaga() + " entrada=" + ticket.getEntrada());
    }

    private void processarSaida(String[] partes) {
        if (partes.length != 2) {
            saida.println("Uso: saida <placa>");
            return;
        }
        String placa = partes[1].toUpperCase(Locale.ROOT);
        Ticket ticket = service.registrarSaida(placa);
        saida.println("Saida registrada. Placa=" + ticket.getPlaca()
                + " valor cobrado=R$ " + ticket.getValorCobrado());
    }

    private void processarDisponibilidade(String[] partes) {
        if (partes.length != 2) {
            saida.println("Uso: disponibilidade <COMUM|PCD|IDOSO|MENSALISTA>");
            return;
        }
        TipoVaga tipo = TipoVaga.valueOf(partes[1].toUpperCase(Locale.ROOT));
        long disponiveis = service.consultarDisponibilidade(tipo);
        saida.println("Vagas disponiveis do tipo " + tipo + ": " + disponiveis);
    }

    private void processarListar() {
        for (Vaga vaga : service.listarTodasAsVagas()) {
            saida.println(vaga);
        }
    }

    private boolean parseSimNao(String valor) {
        String v = valor.toLowerCase(Locale.ROOT);
        if (v.equals("s")) {
            return true;
        }
        if (v.equals("n")) {
            return false;
        }
        throw new IllegalArgumentException("Esperado 's' ou 'n', recebido: " + valor);
    }
}
