package br.edu.ifrs.parkcontrol.repository.impl;

import br.edu.ifrs.parkcontrol.model.Ticket;
import br.edu.ifrs.parkcontrol.repository.TicketRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementacao de {@link TicketRepository} com persistencia em arquivo JSON.
 */
public class ArquivoTicketRepository implements TicketRepository {

    private final Path arquivo;
    private final ObjectMapper objectMapper;

    public ArquivoTicketRepository(Path arquivo) {
        this.arquivo = Objects.requireNonNull(arquivo);
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        inicializarArquivoSeNecessario();
    }

    private void inicializarArquivoSeNecessario() {
        try {
            if (Files.notExists(arquivo)) {
                if (arquivo.getParent() != null) {
                    Files.createDirectories(arquivo.getParent());
                }
                objectMapper.writeValue(arquivo.toFile(), new ArrayList<TicketDTO>());
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao inicializar arquivo de tickets: " + arquivo, e);
        }
    }

    @Override
    public void salvar(Ticket ticket) {
        List<TicketDTO> dtos = lerTodos();
        dtos.removeIf(dto -> dto.id.equals(ticket.getId()));
        dtos.add(TicketDTO.de(ticket));
        escreverTodos(dtos);
    }

    @Override
    public Optional<Ticket> buscarTicketAbertoPorPlaca(String placa) {
        return lerTodos().stream()
                .filter(dto -> dto.placa.equals(placa) && dto.saida == null)
                .map(TicketDTO::paraDominio)
                .findFirst();
    }

    @Override
    public List<Ticket> listarTodos() {
        return lerTodos().stream()
                .map(TicketDTO::paraDominio)
                .collect(Collectors.toList());
    }

    private List<TicketDTO> lerTodos() {
        try {
            TicketDTO[] array = objectMapper.readValue(arquivo.toFile(), TicketDTO[].class);
            List<TicketDTO> lista = new ArrayList<>();
            for (TicketDTO dto : array) {
                lista.add(dto);
            }
            return lista;
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler arquivo de tickets: " + arquivo, e);
        }
    }

    private void escreverTodos(List<TicketDTO> dtos) {
        try {
            objectMapper.writeValue(arquivo.toFile(), dtos);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao escrever arquivo de tickets: " + arquivo, e);
        }
    }

    static class TicketDTO {
        public String id;
        public String placa;
        public int numeroVaga;
        public LocalDateTime entrada;
        public LocalDateTime saida;
        public BigDecimal valorCobrado;
        public boolean mensalista;

        public TicketDTO() {
            // necessario para deserializacao do Jackson
        }

        static TicketDTO de(Ticket ticket) {
            TicketDTO dto = new TicketDTO();
            dto.id = ticket.getId();
            dto.placa = ticket.getPlaca();
            dto.numeroVaga = ticket.getNumeroVaga();
            dto.entrada = ticket.getEntrada();
            dto.saida = ticket.getSaida();
            dto.valorCobrado = ticket.getValorCobrado();
            dto.mensalista = ticket.isMensalista();
            return dto;
        }

        Ticket paraDominio() {
            Ticket ticket = new Ticket(id, placa, numeroVaga, entrada, mensalista);
            if (saida != null) {
                ticket.registrarSaida(saida, valorCobrado);
            }
            return ticket;
        }
    }
}
