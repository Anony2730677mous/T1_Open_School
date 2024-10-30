package ru.t1.java.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.t1.java.demo.model.CorrectionTransaction;

import java.util.Optional;

@Repository
public interface CorrectionTransactionRepository extends JpaRepository<CorrectionTransaction, Long> {
    Optional<CorrectionTransaction> findByCorrectionalTransactionalId(String transactionId);

}
