package br.edu.ifrs.parkcontrol.exception;

public class TicketNaoEncontradoException extends RuntimeException {
    public TicketNaoEncontradoException(String message) {
        super(message);
    }
}
