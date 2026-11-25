package br.edu.ifrs.parkcontrol.component;

import br.edu.ifrs.parkcontrol.model.TipoVaga;
import br.edu.ifrs.parkcontrol.model.Vaga;
import br.edu.ifrs.parkcontrol.repository.VagaRepository;
import br.edu.ifrs.parkcontrol.repository.impl.ArquivoVagaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Teste de componente: valida a implementacao de persistencia em arquivo
 * isoladamente, usando um arquivo real em um diretorio temporario (I/O real,
 * sem mocks), verificando que o componente "repositorio de vagas" funciona
 * de ponta a ponta (escreve, le, filtra).
 */
class ArquivoVagaRepositoryTest {

    @TempDir
    Path tempDir;

    private VagaRepository repository;
    private Path arquivoVagas;

    @BeforeEach
    void setUp() {
        arquivoVagas = tempDir.resolve("vagas.json");
        repository = new ArquivoVagaRepository(arquivoVagas);
    }

    @Test
    @DisplayName("Arquivo e criado automaticamente (vazio) se nao existir")
    void criaArquivoAutomaticamenteSeNaoExistir() {
        assertTrue(Files.exists(arquivoVagas));
        assertTrue(repository.listarTodas().isEmpty());
    }

    @Test
    @DisplayName("Vaga salva e persistida e pode ser recuperada por numero")
    void salvaERecuperaVagaPorNumero() {
        Vaga vaga = new Vaga(1, TipoVaga.COMUM);
        repository.salvar(vaga);

        Optional<Vaga> encontrada = repository.buscarPorNumero(1);

        assertTrue(encontrada.isPresent());
        assertEquals(TipoVaga.COMUM, encontrada.get().getTipo());
        assertFalse(encontrada.get().isOcupada());
    }

    @Test
    @DisplayName("Dados persistem entre instancias diferentes do repositorio (arquivo real)")
    void dadosPersistemEntreInstancias() {
        repository.salvar(new Vaga(1, TipoVaga.PCD));

        VagaRepository novaInstancia = new ArquivoVagaRepository(arquivoVagas);
        Optional<Vaga> encontrada = novaInstancia.buscarPorNumero(1);

        assertTrue(encontrada.isPresent());
        assertEquals(TipoVaga.PCD, encontrada.get().getTipo());
    }

    @Test
    @DisplayName("Atualizar uma vaga existente substitui o registro, nao duplica")
    void atualizarVagaExistenteNaoDuplica() {
        Vaga vaga = new Vaga(1, TipoVaga.COMUM);
        repository.salvar(vaga);

        vaga.ocupar("ABC1D23");
        repository.salvar(vaga);

        List<Vaga> todas = repository.listarTodas();
        assertEquals(1, todas.size());
        assertTrue(todas.get(0).isOcupada());
        assertEquals("ABC1D23", todas.get(0).getPlacaVeiculoAtual());
    }

    @Test
    @DisplayName("Lista apenas vagas disponiveis (nao ocupadas) do tipo solicitado")
    void listaApenasDisponiveisDoTipoSolicitado() {
        Vaga comumLivre = new Vaga(1, TipoVaga.COMUM);
        Vaga comumOcupada = new Vaga(2, TipoVaga.COMUM);
        comumOcupada.ocupar("XYZ9W88");
        Vaga pcdLivre = new Vaga(3, TipoVaga.PCD);

        repository.salvar(comumLivre);
        repository.salvar(comumOcupada);
        repository.salvar(pcdLivre);

        List<Vaga> comunsDisponiveis = repository.listarDisponiveisPorTipo(TipoVaga.COMUM);

        assertEquals(1, comunsDisponiveis.size());
        assertEquals(1, comunsDisponiveis.get(0).getNumero());
    }

    @Test
    @DisplayName("Busca por numero inexistente retorna Optional vazio")
    void buscaPorNumeroInexistenteRetornaVazio() {
        assertTrue(repository.buscarPorNumero(999).isEmpty());
    }
}
