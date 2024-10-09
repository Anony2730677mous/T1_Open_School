package ru.t1.java.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.t1.java.demo.model.ClientAccount;

@Repository
public interface ClientAccountRepository extends JpaRepository<ClientAccount, Long> {

}
