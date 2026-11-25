package br.edu.ifrs.parkcontrol.repository;

import br.edu.ifrs.parkcontrol.model.TipoVaga;
import br.edu.ifrs.parkcontrol.model.Vaga;

import java.util.List;
import java.util.Optional;

public interface VagaRepository {

    void salvar(Vaga vaga);

    Optional<Vaga> buscarPorNumero(int numero);

    List<Vaga> listarTodas();

    List<Vaga> listarDisponiveisPorTipo(TipoVaga tipo);
}
