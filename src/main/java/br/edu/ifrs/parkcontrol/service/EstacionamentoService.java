package br.edu.ifrs.parkcontrol.service;

import br.edu.ifrs.parkcontrol.exception.TicketNaoEncontradoException;
import br.edu.ifrs.parkcontrol.exception.VagaIndisponivelException;
import br.edu.ifrs.parkcontrol.exception.VeiculoJaEstacionadoException;
import br.edu.ifrs.parkcontrol.model.Ticket;
import br.edu.ifrs.parkcontrol.model.TipoVaga;
import br.edu.ifrs.parkcontrol.model.Vaga;
import br.edu.ifrs.parkcontrol.model.Veiculo;
import br.edu.ifrs.parkcontrol.repository.TicketRepository;
import br.edu.ifrs.parkcontrol.repository.VagaRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Orquestra as regras de negocio do estacionamento: entrada, saida e
 * consulta de disponibilidade de vagas.
 */
public class EstacionamentoService {

    private final VagaRepository vagaRepository;
    private final TicketRepository ticketRepository;
    private final CalculadoraDeValor calculadoraDeValor;
    private final Clock clock;

    public EstacionamentoService(VagaRepository vagaRepository, TicketRepository ticketRepository,
                                  CalculadoraDeValor calculadoraDeValor, Clock clock) {
        this.vagaRepository = Objects.requireNonNull(vagaRepository);
        this.ticketRepository = Objects.requireNonNull(ticketRepository);
        this.calculadoraDeValor = Objects.requireNonNull(calculadoraDeValor);
        this.clock = Objects.requireNonNull(clock);
    }

    /**
     * Registra a entrada de um veiculo, alocando automaticamente uma vaga
     * compativel com o tipo de cliente.
     *
     * @return o ticket criado para essa permanencia
     * @throws VeiculoJaEstacionadoException se ja existir um ticket aberto para a placa
     * @throws VagaIndisponivelException     se nao houver vaga disponivel do tipo exigido
     */
    public Ticket registrarEntrada(Veiculo veiculo) {
        Objects.requireNonNull(veiculo);

        if (ticketRepository.buscarTicketAbertoPorPlaca(veiculo.placa()).isPresent()) {
            throw new VeiculoJaEstacionadoException(
                    "Veiculo de placa " + veiculo.placa() + " ja esta estacionado");
        }

        TipoVaga tipoNecessario = veiculo.mensalista() ? TipoVaga.MENSALISTA : veiculo.tipoCliente();
        List<Vaga> disponiveis = vagaRepository.listarDisponiveisPorTipo(tipoNecessario);
        if (disponiveis.isEmpty()) {
            throw new VagaIndisponivelException(
                    "Nao ha vagas disponiveis do tipo " + tipoNecessario + " no momento");
        }

        Vaga vaga = disponiveis.get(0);
        vaga.ocupar(veiculo.placa());
        vagaRepository.salvar(vaga);

        Ticket ticket = new Ticket(
                UUID.randomUUID().toString(),
                veiculo.placa(),
                vaga.getNumero(),
                LocalDateTime.now(clock),
                veiculo.mensalista());
        ticketRepository.salvar(ticket);
        return ticket;
    }

    /**
     * Registra a saida do veiculo de placa informada, calculando o valor devido
     * e liberando a vaga ocupada.
     *
     * @return o ticket atualizado, com saida e valor cobrado preenchidos
     * @throws TicketNaoEncontradoException se nao existir ticket aberto para a placa
     */
    public Ticket registrarSaida(String placa) {
        Objects.requireNonNull(placa);

        Ticket ticket = ticketRepository.buscarTicketAbertoPorPlaca(placa)
                .orElseThrow(() -> new TicketNaoEncontradoException(
                        "Nenhum ticket aberto encontrado para a placa " + placa));

        Vaga vaga = vagaRepository.buscarPorNumero(ticket.getNumeroVaga())
                .orElseThrow(() -> new IllegalStateException(
                        "Vaga " + ticket.getNumeroVaga() + " referenciada pelo ticket nao existe"));

        LocalDateTime agora = LocalDateTime.now(clock);
        BigDecimal valor = calculadoraDeValor.calcular(ticket.getEntrada(), agora, ticket.isMensalista());

        ticket.registrarSaida(agora, valor);
        ticketRepository.salvar(ticket);

        vaga.liberar();
        vagaRepository.salvar(vaga);

        return ticket;
    }

    public long consultarDisponibilidade(TipoVaga tipo) {
        Objects.requireNonNull(tipo);
        return vagaRepository.listarDisponiveisPorTipo(tipo).size();
    }

    public List<Vaga> listarTodasAsVagas() {
        return vagaRepository.listarTodas();
    }
}
