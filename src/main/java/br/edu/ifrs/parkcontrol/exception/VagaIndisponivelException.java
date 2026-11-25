package br.edu.ifrs.parkcontrol.exception;

public class VagaIndisponivelException extends RuntimeException {
    public VagaIndisponivelException(String message) {
        super(message);
    }
}
