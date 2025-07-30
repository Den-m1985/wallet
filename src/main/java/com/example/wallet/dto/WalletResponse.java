package com.example.wallet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletResponse(

        @JsonProperty("wallet_id")
        UUID walletId,

        BigDecimal balance
) {
}
