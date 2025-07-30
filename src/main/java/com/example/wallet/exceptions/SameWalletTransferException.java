package com.example.wallet.exceptions;

public class SameWalletTransferException extends RuntimeException{
    public SameWalletTransferException(String message) {
        super(message);
    }
}
