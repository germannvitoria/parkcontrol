package br.edu.ifrs.parkcontrol.repository;

import br.edu.ifrs.parkcontrol.model.Ticket;

import java.util.List;
import java.util.Optional;

public interface TicketRepository {

    void salvar(Ticket ticket);

    Optional<Ticket> buscarTicketAbertoPorPlaca(String placa);

    List<Ticket> listarTodos();
}
