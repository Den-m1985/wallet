package com.example.wallet.controller;

import com.example.wallet.controller.interfaces.WalletControllerApi;
import com.example.wallet.dto.TransferRequest;
import com.example.wallet.dto.WalletRequest;
import com.example.wallet.dto.WalletResponse;
import com.example.wallet.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController implements WalletControllerApi {
    private final WalletService walletService;

    @PostMapping
    public ResponseEntity<WalletResponse> updateWallet(@Valid @RequestBody WalletRequest request) {
        return ResponseEntity.ok(walletService.updateWallet(request));
    }

    @PostMapping("/transfer")
    public ResponseEntity<Void> transferBetweenWallets(@Valid @RequestBody TransferRequest request) {
        walletService.transferBetweenWallets(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{walletId}")
    public ResponseEntity<WalletResponse> getBalance(@PathVariable UUID walletId) {
        return ResponseEntity.ok(walletService.getBalance(walletId));
    }
}
