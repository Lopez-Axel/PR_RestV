package com.example.demo_pam.exception;

// Representa un pedido o pago que no puede completarse por una regla de negocio del módulo de pedidos
public class InvalidOrderException extends RuntimeException {

    public InvalidOrderException(String message) {
        super(message);
    }
}