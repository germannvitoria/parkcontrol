package br.edu.ifrs.parkcontrol.repository.impl;

import br.edu.ifrs.parkcontrol.model.TipoVaga;
import br.edu.ifrs.parkcontrol.model.Vaga;
import br.edu.ifrs.parkcontrol.repository.VagaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementacao de {@link VagaRepository} com persistencia em arquivo JSON.
 *
 * O arquivo inteiro e lido/escrito a cada operacao; para o volume de dados
 * de um unico estacionamento isso e simples e suficiente.
 */
public class ArquivoVagaRepository implements VagaRepository {

    private final Path arquivo;
    private final ObjectMapper objectMapper;

    public ArquivoVagaRepository(Path arquivo) {
        this.arquivo = Objects.requireNonNull(arquivo);
        this.objectMapper = new ObjectMapper();
        inicializarArquivoSeNecessario();
    }

    private void inicializarArquivoSeNecessario() {
        try {
            if (Files.notExists(arquivo)) {
                if (arquivo.getParent() != null) {
                    Files.createDirectories(arquivo.getParent());
                }
                objectMapper.writeValue(arquivo.toFile(), new ArrayList<VagaDTO>());
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao inicializar arquivo de vagas: " + arquivo, e);
        }
    }

    @Override
    public void salvar(Vaga vaga) {
        List<VagaDTO> dtos = lerTodos();
        dtos.removeIf(dto -> dto.numero == vaga.getNumero());
        dtos.add(VagaDTO.de(vaga));
        escreverTodos(dtos);
    }

    @Override
    public Optional<Vaga> buscarPorNumero(int numero) {
        return lerTodos().stream()
                .filter(dto -> dto.numero == numero)
                .map(VagaDTO::paraDominio)
                .findFirst();
    }

    @Override
    public List<Vaga> listarTodas() {
        return lerTodos().stream()
                .map(VagaDTO::paraDominio)
                .collect(Collectors.toList());
    }

    @Override
    public List<Vaga> listarDisponiveisPorTipo(TipoVaga tipo) {
        return lerTodos().stream()
                .filter(dto -> dto.tipo == tipo && !dto.ocupada)
                .map(VagaDTO::paraDominio)
                .collect(Collectors.toList());
    }

    private List<VagaDTO> lerTodos() {
        try {
            VagaDTO[] array = objectMapper.readValue(arquivo.toFile(), VagaDTO[].class);
            List<VagaDTO> lista = new ArrayList<>();
            for (VagaDTO dto : array) {
                lista.add(dto);
            }
            return lista;
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler arquivo de vagas: " + arquivo, e);
        }
    }

    private void escreverTodos(List<VagaDTO> dtos) {
        try {
            objectMapper.writeValue(arquivo.toFile(), dtos);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao escrever arquivo de vagas: " + arquivo, e);
        }
    }

    /**
     * Objeto de transferencia usado apenas para serializacao JSON,
     * mantendo o modelo de dominio ({@link Vaga}) livre de anotacoes de infraestrutura.
     */
    static class VagaDTO {
        public int numero;
        public TipoVaga tipo;
        public boolean ocupada;
        public String placaVeiculoAtual;

        public VagaDTO() {
            // necessario para deserializacao do Jackson
        }

        static VagaDTO de(Vaga vaga) {
            VagaDTO dto = new VagaDTO();
            dto.numero = vaga.getNumero();
            dto.tipo = vaga.getTipo();
            dto.ocupada = vaga.isOcupada();
            dto.placaVeiculoAtual = vaga.getPlacaVeiculoAtual();
            return dto;
        }

        Vaga paraDominio() {
            Vaga vaga = new Vaga(numero, tipo);
            if (ocupada) {
                vaga.ocupar(placaVeiculoAtual);
            }
            return vaga;
        }
    }
}
