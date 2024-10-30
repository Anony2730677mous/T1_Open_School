package ru.t1.java.demo.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.t1.java.demo.model.Transaction;

import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionalId(String transaction);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Transaction a WHERE a.id = :transactionId")
    Optional<Transaction> findTransactionalByIdForUpdate(@Param("transactionId") String transactionId);
}