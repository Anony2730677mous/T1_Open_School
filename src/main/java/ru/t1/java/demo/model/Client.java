package ru.t1.java.demo.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.AbstractPersistable;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "client")
public class Client extends AbstractPersistable<Long> {

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "middle_name")
    private String middleName;

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
    private List<ClientAccount> accounts = new ArrayList<>();
    public void addClientAccount(ClientAccount clientAccount) {
        if (accounts == null) {
            accounts = new ArrayList<>();
        }
        this.accounts.add(clientAccount);
        clientAccount.setClient(this);
    }
    public void removeClientAccount(ClientAccount clientAccount) {
        accounts.remove(clientAccount);
        clientAccount.setClient(null);
    }

    @Override
    public Long getId() {
        return super.getId();
    }

    @Override
    public void setId(Long id) {
        super.setId(id);
    }
}