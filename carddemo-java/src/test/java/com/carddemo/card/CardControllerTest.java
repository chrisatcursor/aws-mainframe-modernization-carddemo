package com.carddemo.card;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import java.util.Optional;

import com.carddemo.common.PaginationConstants;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CardController.class)
class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CardService cardService;

    @MockBean
    private CardRepository cardRepository;

    @Test
    @WithMockUser(roles = "ADMIN")
    void list_admin_rendersPage() throws Exception {
        CardDto dto = new CardDto("9680294154603697", 1L, "Name", "2025-01-01", "Y");
        Pageable pageable = PageRequest.of(0, PaginationConstants.DEFAULT_PAGE_SIZE, Sort.by("cardNumber"));
        when(cardService.listCards(any(CardSearchCriteria.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dto), pageable, 1));

        mockMvc.perform(get("/card/list"))
                .andExpect(status().isOk())
                .andExpect(view().name("card/list"))
                .andExpect(model().attributeExists("page"))
                .andExpect(model().attributeExists("criteria"))
                .andExpect(model().attribute("adminBrowse", true));
    }

    @Test
    @WithMockUser(username = "USER0001", roles = "USER")
    void list_regularUser_rendersPage() throws Exception {
        CardDto dto = new CardDto("9680294154603697", 1L, "Name", "2025-01-01", "Y");
        Pageable pageable = PageRequest.of(0, PaginationConstants.DEFAULT_PAGE_SIZE, Sort.by("cardNumber"));
        when(cardService.listCards(any(CardSearchCriteria.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dto), pageable, 1));

        mockMvc.perform(get("/card/list"))
                .andExpect(status().isOk())
                .andExpect(view().name("card/list"))
                .andExpect(model().attribute("adminBrowse", false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void detail_found_rendersTemplate() throws Exception {
        CardDto dto = new CardDto("9680294154603697", 1L, "Name", "2025-01-01", "Y");
        when(cardService.findByCardNumber("9680294154603697")).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/card/9680294154603697"))
                .andExpect(status().isOk())
                .andExpect(view().name("card/detail"))
                .andExpect(model().attribute("card", dto));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void detail_missing_returns404() throws Exception {
        when(cardService.findByCardNumber("0000000000000000")).thenReturn(Optional.empty());

        mockMvc.perform(get("/card/0000000000000000"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "USER0001", roles = "USER")
    void detail_wrongAccount_returns404() throws Exception {
        CardDto dto = new CardDto("0923877193247330", 2L, "Other", "2025-01-01", "Y");
        when(cardService.findByCardNumber("0923877193247330")).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/card/0923877193247330"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void detailByQuery_noCardNumber_showsPrompt() throws Exception {
        mockMvc.perform(get("/card/detail"))
                .andExpect(status().isOk())
                .andExpect(view().name("card/detail"))
                .andExpect(model().attribute("promptForId", true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void detailByQuery_withCardNumber_rendersDetail() throws Exception {
        CardDto dto = new CardDto("9680294154603697", 1L, "Name", "2025-01-01", "Y");
        when(cardService.findByCardNumber("9680294154603697")).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/card/detail").param("cardNumber", "9680294154603697"))
                .andExpect(status().isOk())
                .andExpect(view().name("card/detail"))
                .andExpect(model().attribute("card", dto));
    }

    @Test
    @WithMockUser(username = "USER0001", roles = "USER")
    void detail_ownAccount_ok() throws Exception {
        CardDto dto = new CardDto("9680294154603697", 1L, "Name", "2025-01-01", "Y");
        when(cardService.findByCardNumber("9680294154603697")).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/card/9680294154603697"))
                .andExpect(status().isOk())
                .andExpect(view().name("card/detail"));
    }
}
