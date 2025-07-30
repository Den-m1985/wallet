package com.example.wallet.controller.interfaces;

import com.example.wallet.dto.TransferRequest;
import com.example.wallet.dto.WalletRequest;
import com.example.wallet.dto.WalletResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@Tag(name = "Wallet Controller")
public interface WalletControllerApi {

    @Operation(summary = "Update wallet")
    ResponseEntity<WalletResponse> updateWallet(@Valid @RequestBody WalletRequest request);

    @Operation(summary = "Transfer amount between wallet")
    ResponseEntity<Void> transferBetweenWallets(@Valid @RequestBody TransferRequest request);

    @Operation(summary = "Get balance from wallet")
    ResponseEntity<WalletResponse> getBalance(@PathVariable UUID walletId);
}
