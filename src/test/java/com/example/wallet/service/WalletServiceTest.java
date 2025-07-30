package com.example.wallet.service;

import com.example.wallet.dto.TransferRequest;
import com.example.wallet.dto.WalletRequest;
import com.example.wallet.enums.OperationType;
import com.example.wallet.model.User;
import com.example.wallet.model.Wallet;
import com.example.wallet.repository.UserRepository;
import com.example.wallet.repository.WalletRepository;
import com.example.wallet.repository.WalletTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class WalletServiceTest {
    @Autowired
    private WalletService walletService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    private Wallet wallet;
    private final BigDecimal initialBalance = new BigDecimal("1000.00");

    @BeforeEach
    void setUp() {
        walletRepository.deleteAll();
        walletTransactionRepository.deleteAll();
        wallet = createWallet(initialBalance);
        assertNotNull(wallet);
    }

    public Wallet createWallet(BigDecimal initialBalance) {
        User user = userRepository.findById(UUID.fromString("11111111-1111-1111-1111-111111111111")).orElseThrow();
        Wallet newWallet = new Wallet();
        newWallet.setUser(user);
        newWallet.setBalance(initialBalance);
        return walletRepository.save(newWallet);
    }

    @Test
    void shouldAddAmountToWallet() {
        BigDecimal deposit = new BigDecimal("100.00");
        WalletRequest walletRequest = new WalletRequest(wallet.getId(), OperationType.DEPOSIT, deposit);
        walletService.updateWallet(walletRequest);
        Optional<Wallet> updatedWallet = walletRepository.findById(wallet.getId());
        assertTrue(updatedWallet.isPresent());
        assertEquals(initialBalance.add(deposit), updatedWallet.get().getBalance());
    }

    @Test
    void shouldTransferBetweenWalletsSuccessfully() {
        Wallet toWallet = createWallet(new BigDecimal("100.00"));

        TransferRequest request = new TransferRequest(wallet.getId(), toWallet.getId(), new BigDecimal("200.00"));
        walletService.transferBetweenWallets(request);

        Wallet updatedFrom = walletRepository.findById(wallet.getId()).orElseThrow();
        Wallet updatedTo = walletRepository.findById(toWallet.getId()).orElseThrow();

        assertEquals(new BigDecimal("800.00"), updatedFrom.getBalance());
        assertEquals(new BigDecimal("300.00"), updatedTo.getBalance());
    }

    @Test
    void shouldThrowWhenInsufficientFunds() {
        Wallet toWallet = createWallet(new BigDecimal("100.00"));

        TransferRequest request = new TransferRequest(wallet.getId(), toWallet.getId(), new BigDecimal("2000.00"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            walletService.transferBetweenWallets(request);
        });
        assertTrue(ex.getMessage().contains("Insufficient funds in wallet"));
    }
}
