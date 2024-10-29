package ru.t1.java.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.t1.java.demo.model.CorrectionTransaction;

import java.util.Optional;

public interface CorrectionTransactionRepository extends JpaRepository<CorrectionTransaction, Long> {
    Optional<CorrectionTransaction> findByCorrectionalTransactionalId(String transactionId);

}
