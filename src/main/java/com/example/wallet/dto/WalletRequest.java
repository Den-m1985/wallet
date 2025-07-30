package com.example.wallet.dto;

import com.example.wallet.enums.OperationType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletRequest(

        @JsonProperty("wallet_id")
        @NotNull(message = "Wallet ID cannot be null")
        UUID walletId,

        @JsonProperty("operation_type")
        @NotNull(message = "Operation type cannot be null")
        OperationType operationType,

        @NotNull(message = "Amount cannot be null")
        @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
        @DecimalMax(value = "1000000", message = "Amount cannot exceed 1,000,000")
        BigDecimal amount
) {
}
