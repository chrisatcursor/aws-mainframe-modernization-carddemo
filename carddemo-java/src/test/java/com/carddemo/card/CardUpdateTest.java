package com.carddemo.card;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CardController.class)
class CardUpdateTest {

    private static final String CARD_NUMBER = "9680294154603697";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CardService cardService;

    @MockBean
    private CardRepository cardRepository;

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateCard_success_redirectsWithFlash() throws Exception {
        Card card = entityForEdit();
        when(cardRepository.findById(CARD_NUMBER)).thenReturn(Optional.of(card));
        when(cardService.updateCard(eq(CARD_NUMBER), any(CardUpdateRequest.class))).thenReturn(card);

        mockMvc.perform(post("/card/" + CARD_NUMBER + "/edit")
                        .with(csrf())
                        .param("embossedName", "Updated Name")
                        .param("expirationDate", "2030-06-15")
                        .param("activeStatus", "Y")
                        .param("version", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/card/" + CARD_NUMBER))
                .andExpect(flash().attribute("successMessage", "Card updated successfully."));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateCard_validationErrors_returnsForm() throws Exception {
        Card card = entityForEdit();
        when(cardRepository.findById(CARD_NUMBER)).thenReturn(Optional.of(card));

        mockMvc.perform(post("/card/" + CARD_NUMBER + "/edit")
                        .with(csrf())
                        .param("embossedName", "")
                        .param("expirationDate", "bad-date")
                        .param("activeStatus", "Y")
                        .param("version", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("card/edit"))
                .andExpect(model().attributeHasFieldErrors("request", "embossedName", "expirationDate"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateCard_optimisticLockConflict_returnsFormWithMessage() throws Exception {
        Card card = entityForEdit();
        Card fresh = entityForEdit();
        fresh.setVersion(2L);
        fresh.setEmbossedName("Server Name");

        when(cardRepository.findById(CARD_NUMBER)).thenReturn(Optional.of(card), Optional.of(fresh));
        when(cardService.updateCard(eq(CARD_NUMBER), any(CardUpdateRequest.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Card.class, CARD_NUMBER));

        var mvcResult = mockMvc.perform(post("/card/" + CARD_NUMBER + "/edit")
                        .with(csrf())
                        .param("embossedName", "Client Name")
                        .param("expirationDate", "2030-06-15")
                        .param("activeStatus", "Y")
                        .param("version", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("card/edit"))
                .andExpect(model().attributeExists("optimisticLockError"))
                .andExpect(model().attribute("card", fresh))
                .andReturn();

        CardUpdateRequest bound = (CardUpdateRequest) mvcResult.getModelAndView().getModel().get("request");
        assertThat(bound.getVersion()).isEqualTo(2L);
        assertThat(bound.getEmbossedName()).isEqualTo("Server Name");
    }

    private static Card entityForEdit() {
        Card card = new Card();
        card.setCardNumber(CARD_NUMBER);
        card.setAccountId(1L);
        card.setEmbossedName("Original");
        card.setExpirationDate("2028-12-31");
        card.setActiveStatus("Y");
        card.setVersion(1L);
        return card;
    }
}
