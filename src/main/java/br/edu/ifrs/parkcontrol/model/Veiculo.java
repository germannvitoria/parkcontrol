package br.edu.ifrs.parkcontrol.model;

import java.util.Objects;

/**
 * Representa um veiculo que pode entrar no estacionamento.
 *
 * @param placa       identificador unico do veiculo (ex: "ABC1D23")
 * @param tipoCliente tipo de elegibilidade do cliente (define quais vagas pode ocupar)
 * @param mensalista  indica se o veiculo possui plano mensal contratado
 */
public record Veiculo(String placa, TipoVaga tipoCliente, boolean mensalista) {

    public Veiculo {
        Objects.requireNonNull(placa, "Placa nao pode ser nula");
        Objects.requireNonNull(tipoCliente, "Tipo de cliente nao pode ser nulo");
        if (placa.isBlank()) {
            throw new IllegalArgumentException("Placa nao pode ser vazia");
        }
        if (tipoCliente == TipoVaga.MENSALISTA) {
            throw new IllegalArgumentException(
                    "MENSALISTA nao e um tipo de cliente valido; use o campo 'mensalista' para marcar a assinatura");
        }
    }
}
