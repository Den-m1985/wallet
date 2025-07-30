package com.example.wallet.service;

import com.example.wallet.dto.TransferRequest;
import com.example.wallet.dto.WalletRequest;
import com.example.wallet.dto.WalletResponse;
import com.example.wallet.enums.OperationType;
import com.example.wallet.exceptions.InsufficientFundsException;
import com.example.wallet.exceptions.SameWalletTransferException;
import com.example.wallet.model.Wallet;
import com.example.wallet.model.WalletTransaction;
import com.example.wallet.repository.WalletRepository;
import com.example.wallet.repository.WalletTransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    @Transactional
    public WalletResponse updateWallet(WalletRequest request) {
        Wallet wallet = getWalletForUpdate(request.walletId());
        validateOperation(wallet, request);
        applyUpdateOperation(wallet, request);

        WalletTransaction transaction = createTransaction(wallet, request);
        wallet.getWalletTransactions().add(transaction);
        return new WalletResponse(wallet.getId(), wallet.getBalance());
    }

    public Wallet getWalletForUpdate(UUID id) {
        return walletRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("Wallet not found"));
    }

    private void validateOperation(Wallet wallet, WalletRequest request) {
        if (request.operationType() == OperationType.WITHDRAW
                && wallet.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientFundsException(wallet.getId().toString(), wallet.getBalance(), request.amount());
        }
    }

    private void applyUpdateOperation(Wallet wallet, WalletRequest request) {
        BigDecimal walletBalance = wallet.getBalance();
        BigDecimal amount = request.amount();
        if (request.operationType() == OperationType.DEPOSIT) {
            wallet.setBalance(walletBalance.add(amount));
        } else if (request.operationType() == OperationType.WITHDRAW) {
            wallet.setBalance(walletBalance.subtract(amount));
        }
    }

    private WalletTransaction createTransaction(Wallet wallet, WalletRequest request) {
        WalletTransaction transaction = new WalletTransaction(wallet, request.operationType(), request.amount());
        return walletTransactionRepository.save(transaction);
    }

    @Transactional(readOnly = true)
    public WalletResponse getBalance(UUID walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new EntityNotFoundException("Wallet not found"));
        return new WalletResponse(wallet.getId(), wallet.getBalance());
    }

    /**
     * <a href="https://medium.com/@AlexanderObregon/using-springs-retryable-annotation-for-automatic-retries-c1d197bc199f">...</a>
     */
    @Retryable(
            retryFor = {CannotAcquireLockException.class, PessimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2)
    )
    @Transactional
    public void transferBetweenWallets(TransferRequest request) {
        if (request.fromWalletId().equals(request.toWalletId())) {
            String message = "wallet from and wallet to has the same id: " + request.fromWalletId();
            throw new SameWalletTransferException(message);
        }
        UUID fromId = request.fromWalletId();
        UUID toId = request.toWalletId();

        Wallet firstToLock;
        Wallet secondToLock;

        if (fromId.compareTo(toId) < 0) {
            firstToLock = getWalletForUpdate(fromId);
            secondToLock = getWalletForUpdate(toId);
        } else {
            secondToLock = getWalletForUpdate(toId);
            firstToLock = getWalletForUpdate(fromId);
        }

        Wallet walletFrom = fromId.equals(firstToLock.getId()) ? firstToLock : secondToLock;
        Wallet walletTo = toId.equals(firstToLock.getId()) ? firstToLock : secondToLock;

        if (walletFrom.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientFundsException(walletFrom.getId().toString(), walletFrom.getBalance(), request.amount());
        }
        walletFrom.setBalance(walletFrom.getBalance().subtract(request.amount()));
        walletTo.setBalance(walletTo.getBalance().add(request.amount()));

        WalletTransaction debitTransaction = new WalletTransaction(walletFrom, OperationType.WITHDRAW, request.amount());
        WalletTransaction creditTransaction = new WalletTransaction(walletTo, OperationType.DEPOSIT, request.amount());

        walletFrom.getWalletTransactions().add(debitTransaction);
        walletTo.getWalletTransactions().add(creditTransaction);

        walletTransactionRepository.saveAll(List.of(debitTransaction, creditTransaction));

        log.info("debitTransaction id {} creditTransaction id {} from wallet {} to {}",
                debitTransaction.getId(), creditTransaction.getId(), fromId, toId);
    }
}
