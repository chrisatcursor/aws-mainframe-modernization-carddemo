package com.carddemo.integration.card;

import com.carddemo.card.api.CardUpdateRequest;
import com.carddemo.card.model.Card;
import com.carddemo.card.repository.CardRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "carddemo.bootstrap.enabled=true")
class CardControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getCardDetail_returnsCardAndCustomer() throws Exception {
        Card card = cardRepository.findAll().stream().findFirst().orElseThrow();

        mockMvc.perform(get("/api/cards/{cardNumber}", card.getCardNumber())
                        .param("accountId", String.valueOf(card.getAccountId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardNumber").value(card.getCardNumber()))
                .andExpect(jsonPath("$.accountId").value(card.getAccountId()))
                .andExpect(jsonPath("$.customerId").isNumber());
    }

    @Test
    void listCards_returnsPagedItems() throws Exception {
        Card card = cardRepository.findAll().stream().findFirst().orElseThrow();

        mockMvc.perform(get("/api/cards")
                        .param("accountId", String.valueOf(card.getAccountId()))
                        .param("page", "0")
                        .param("size", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].accountId").value(card.getAccountId()));
    }

    @Test
    void updateCard_persistsChangedFields() throws Exception {
        Card card = cardRepository.findAll().stream().findFirst().orElseThrow();
        CardUpdateRequest request = new CardUpdateRequest(
                "Migrated Name",
                "Y",
                LocalDate.of(2029, 12, 31),
                card.getEmbossedName(),
                card.getActiveStatus(),
                card.getExpirationDate()
        );

        mockMvc.perform(put("/api/cards/{cardNumber}", card.getCardNumber())
                        .param("accountId", String.valueOf(card.getAccountId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardNumber").value(card.getCardNumber()))
                .andExpect(jsonPath("$.embossedName").value("Migrated Name"))
                .andExpect(jsonPath("$.activeStatus").value("Y"));

        Card updated = cardRepository.findById(card.getCardNumber()).orElseThrow();
        assertThat(updated.getEmbossedName()).isEqualTo("Migrated Name");
        assertThat(updated.getActiveStatus()).isEqualTo("Y");
        assertThat(updated.getExpirationDate()).isEqualTo(LocalDate.of(2029, 12, 31));
    }
}
