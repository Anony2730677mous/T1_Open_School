package ru.t1.java.demo.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.t1.java.demo.model.ClientAccount;

import java.util.Optional;

@Repository
public interface ClientAccountRepository extends JpaRepository<ClientAccount, Long> {

    Optional<ClientAccount> findAccountById(Long accountId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM ClientAccount a WHERE a.id = :accountId")
    Optional<ClientAccount> findAccountByIdForUpdate(@Param("accountId") Long accountId);

}
