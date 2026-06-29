package com.wallet.wallet_service.repository;

import com.wallet.wallet_service.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionIdAndUserId(String transactionId, String userId);


    Page<Transaction> findByUserIdOrderByCreatedAtDesc(
            String userId,
            Pageable pageable);

    List<Transaction> findByUserIdAndCreatedAtBetween(
            String userId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    List<Transaction> findByTransferId(String transferId);
}