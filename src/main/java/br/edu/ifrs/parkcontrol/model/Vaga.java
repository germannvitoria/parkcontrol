package br.edu.ifrs.parkcontrol.model;

import java.util.Objects;

/**
 * Representa uma vaga do estacionamento.
 */
public class Vaga {

    private final int numero;
    private final TipoVaga tipo;
    private boolean ocupada;
    private String placaVeiculoAtual;

    public Vaga(int numero, TipoVaga tipo) {
        if (numero <= 0) {
            throw new IllegalArgumentException("Numero da vaga deve ser positivo");
        }
        this.numero = numero;
        this.tipo = Objects.requireNonNull(tipo, "Tipo da vaga nao pode ser nulo");
        this.ocupada = false;
        this.placaVeiculoAtual = null;
    }

    public int getNumero() {
        return numero;
    }

    public TipoVaga getTipo() {
        return tipo;
    }

    public boolean isOcupada() {
        return ocupada;
    }

    public String getPlacaVeiculoAtual() {
        return placaVeiculoAtual;
    }

    public void ocupar(String placa) {
        if (ocupada) {
            throw new IllegalStateException("Vaga " + numero + " ja esta ocupada");
        }
        this.ocupada = true;
        this.placaVeiculoAtual = Objects.requireNonNull(placa);
    }

    public void liberar() {
        this.ocupada = false;
        this.placaVeiculoAtual = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vaga vaga)) return false;
        return numero == vaga.numero;
    }

    @Override
    public int hashCode() {
        return Objects.hash(numero);
    }

    @Override
    public String toString() {
        return "Vaga{numero=" + numero + ", tipo=" + tipo + ", ocupada=" + ocupada + '}';
    }
}
