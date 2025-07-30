package com.example.wallet.exceptions;

import java.math.BigDecimal;

public class InsufficientFundsException extends RuntimeException{
    public InsufficientFundsException(String walletId, BigDecimal balance, BigDecimal amount) {
        super(String.format("Insufficient funds in wallet %s. Current balance: %.2f, requested amount: %.2f",
                walletId, balance, amount));
    }
}
