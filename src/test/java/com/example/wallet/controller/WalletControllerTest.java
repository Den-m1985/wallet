package com.example.wallet.controller;

import com.example.wallet.dto.TransferRequest;
import com.example.wallet.dto.WalletRequest;
import com.example.wallet.enums.OperationType;
import com.example.wallet.model.User;
import com.example.wallet.model.Wallet;
import com.example.wallet.repository.UserRepository;
import com.example.wallet.repository.WalletRepository;
import com.example.wallet.repository.WalletTransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WalletControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String endpointUpdateWallet = "/api/v1/wallets";
    private final String endpointTransferAmmount = "/api/v1/wallets/transfer";
    private UUID walletId;
    private final BigDecimal initialBalance = new BigDecimal("1000.00");

    @BeforeEach
    void setUp() {
        walletRepository.deleteAll();
        walletTransactionRepository.deleteAll();
        Wallet wallet = createWallet(initialBalance);
        walletId = wallet.getId();
        assertNotNull(wallet);
    }

    public Wallet createWallet(BigDecimal initialBalance) {
        User user = userRepository.findById(UUID.fromString("11111111-1111-1111-1111-111111111111")).orElseThrow();
        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(initialBalance);
        return walletRepository.save(wallet);
    }

    @Test
    @Order(1)
    void shouldUpdate() throws Exception {
        BigDecimal deposit = new BigDecimal("100.00");
        WalletRequest walletRequest = new WalletRequest(walletId, OperationType.DEPOSIT, deposit);
        mockMvc.perform(post(endpointUpdateWallet)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(walletRequest)))
                .andExpect(status().isOk());
        Optional<Wallet> updatedWallet = walletRepository.findById(walletId);
        assertTrue(updatedWallet.isPresent());
        assertEquals(initialBalance.add(deposit), updatedWallet.get().getBalance());
    }

    @Test
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void concurrentDepositsShouldNotLoseUpdates() throws InterruptedException {
        int threadCount = 100;
        int requestsPerThread = 30;
        AtomicInteger successCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.execute(() -> {
                for (int j = 0; j < requestsPerThread; j++) {
                    try {
                        WalletRequest request = new WalletRequest(
                                walletId,
                                OperationType.DEPOSIT,
                                new BigDecimal("1.00")
                        );
                        mockMvc.perform(post(endpointUpdateWallet)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        // Ignore individual failures for this test
                    }
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        // Verify all requests were processed
        assertEquals(threadCount * requestsPerThread, successCount.get());

        // Verify final balance
        BigDecimal expectedBalance = initialBalance.add(
                new BigDecimal(threadCount * requestsPerThread * 1.00)
        );

        Optional<Wallet> finalWallet = walletRepository.findById(walletId);
        assertTrue(finalWallet.isPresent());
        assertEquals(expectedBalance, finalWallet.get().getBalance());
    }


    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void shouldSimulateDeadlockBetweenTwoWallets() throws Exception {
        Wallet wallet2 = createWallet(initialBalance);

        // Создаем два потока для имитации deadlock
        CountDownLatch latch = new CountDownLatch(2);
        AtomicBoolean deadlockOccurred = new AtomicBoolean(false);

        // Поток 1: Переводим с wallet1 на wallet2
        Thread thread1 = new Thread(() -> {
            try {
                TransferRequest transferRequest1 = new TransferRequest(walletId, wallet2.getId(), new BigDecimal("100.00"));
                mockMvc.perform(post(endpointTransferAmmount)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(transferRequest1)))
                        .andExpect(status().isOk());
            } catch (Exception e) {
                if (e.getCause() instanceof PessimisticLockingFailureException) {
                    deadlockOccurred.set(true);
                }
            }
            latch.countDown();
        });

        // Поток 2: Переводим с wallet2 на wallet1 (обратное направление)
        Thread thread2 = new Thread(() -> {
            try {
                TransferRequest transferRequest1 = new TransferRequest(wallet2.getId(), walletId, new BigDecimal("100.00"));
                mockMvc.perform(post(endpointTransferAmmount)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(transferRequest1)))
                        .andExpect(status().isOk());
            } catch (Exception e) {
                if (e.getCause() instanceof PessimisticLockingFailureException) {
                    deadlockOccurred.set(true);
                }
            }
            latch.countDown();
        });
        thread1.start();
        thread2.start();

        // Ждем завершения или таймаута
        latch.await();

        assertFalse(deadlockOccurred.get(), "Deadlock should have occurred");

        Wallet wallet1 = walletRepository.findById(walletId).orElseThrow();
        assertEquals(initialBalance, wallet1.getBalance());
        Wallet walletSecond = walletRepository.findById(wallet2.getId()).orElseThrow();
        assertEquals(initialBalance, walletSecond.getBalance());
    }
}
