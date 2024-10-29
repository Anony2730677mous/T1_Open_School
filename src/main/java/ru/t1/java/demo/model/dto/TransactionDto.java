package ru.t1.java.demo.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public class TransactionDto {
    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("client_Id")
    private Long clientId;

    @JsonProperty("account_Id")
    private Long accountId;

    @JsonProperty("transaction_Id")
    private String transactionId;

    @JsonProperty("transaction_action")
    private String transactionAction;

    @JsonProperty("transaction_state")
    private String transactionState;

}
