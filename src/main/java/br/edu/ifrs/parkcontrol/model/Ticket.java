package br.edu.ifrs.parkcontrol.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Registro de uma permanencia de um veiculo no estacionamento,
 * desde a entrada ate a saida (quando ocorrer).
 */
public class Ticket {

    private final String id;
    private final String placa;
    private final int numeroVaga;
    private final LocalDateTime entrada;
    private final boolean mensalista;
    private LocalDateTime saida;
    private BigDecimal valorCobrado;

    public Ticket(String id, String placa, int numeroVaga, LocalDateTime entrada, boolean mensalista) {
        this.id = Objects.requireNonNull(id);
        this.placa = Objects.requireNonNull(placa);
        this.numeroVaga = numeroVaga;
        this.entrada = Objects.requireNonNull(entrada);
        this.mensalista = mensalista;
    }

    public String getId() {
        return id;
    }

    public String getPlaca() {
        return placa;
    }

    public int getNumeroVaga() {
        return numeroVaga;
    }

    public LocalDateTime getEntrada() {
        return entrada;
    }

    public LocalDateTime getSaida() {
        return saida;
    }

    public BigDecimal getValorCobrado() {
        return valorCobrado;
    }

    public boolean isAberto() {
        return saida == null;
    }

    public boolean isMensalista() {
        return mensalista;
    }

    public void registrarSaida(LocalDateTime saida, BigDecimal valorCobrado) {
        if (!isAberto()) {
            throw new IllegalStateException("Ticket " + id + " ja possui saida registrada");
        }
        if (saida.isBefore(entrada)) {
            throw new IllegalArgumentException("Hora de saida nao pode ser anterior a hora de entrada");
        }
        this.saida = Objects.requireNonNull(saida);
        this.valorCobrado = Objects.requireNonNull(valorCobrado);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Ticket ticket)) return false;
        return Objects.equals(id, ticket.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Ticket{id='" + id + "', placa='" + placa + "', vaga=" + numeroVaga
                + ", entrada=" + entrada + ", saida=" + saida + ", valor=" + valorCobrado + '}';
    }
}
