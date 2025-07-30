package com.example.wallet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(

        @JsonProperty("from_wallet_id")
        @NotNull(message = "Source wallet ID cannot be null")
        UUID fromWalletId,

        @JsonProperty("to_wallet_id")
        @NotNull(message = "Target wallet ID cannot be null")
        UUID toWalletId,

        @NotNull(message = "Amount cannot be null")
        @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
        @DecimalMax(value = "1000000", message = "Amount cannot exceed 1,000,000")
        BigDecimal amount
) {
}
