package com.easybytes.cards.controller;

import com.easybytes.cards.dto.CardsDto;
import com.easybytes.cards.exception.CardAlreadyExistsException;
import com.easybytes.cards.exception.ResourceNotFoundException;
import com.easybytes.cards.service.ICardsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CardsController.class)
@ActiveProfiles("test")
class CardsControllerTest {

    private static final String MOBILE = "0612345678";
    private static final String CORRELATION_HEADER = "easybank-correlation-id";
    private static final String VALID_CARD = """
            {"mobileNumber":"0612345678","cardNumber":"1234567890123456","cardType":"DEBIT",
             "totalLimit":100000,"amountUsed":1000,"availableAmount":99000}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ICardsService cardsService;

    @Test
    void createReturns201() throws Exception {
        mockMvc.perform(post("/api/create").param("mobileNumber", MOBILE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value("201"))
                .andExpect(jsonPath("$.statusMessage").value("Card created successfully"));

        verify(cardsService).createCard(MOBILE);
    }

    @Test
    void createForExistingCardReturns400() throws Exception {
        doThrow(new CardAlreadyExistsException("Card already exists for mobile: " + MOBILE))
                .when(cardsService).createCard(MOBILE);

        mockMvc.perform(post("/api/create").param("mobileNumber", MOBILE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.apiPath").value("uri=/api/create"))
                .andExpect(jsonPath("$.errorCode").value("400 BAD_REQUEST"))
                .andExpect(jsonPath("$.errorMessage").value("Card already exists for mobile: " + MOBILE))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void createWithMalformedMobileNumberIsRejected() throws Exception {
        // Characterization of a known bug: the @Pattern violation surfaces as ConstraintViolationException,
        // which the catch-all handler maps to 500 instead of 400. Fixed in the following commit.
        mockMvc.perform(post("/api/create").param("mobileNumber", "123"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorMessage").value(containsString("Mobile number must be 10 digits")));

        verifyNoInteractions(cardsService);
    }

    @Test
    void fetchReturnsCard() throws Exception {
        CardsDto dto = new CardsDto();
        dto.setMobileNumber(MOBILE);
        dto.setCardNumber("1234567890123456");
        dto.setTotalLimit(100_000);
        when(cardsService.fetchCard(MOBILE)).thenReturn(dto);

        mockMvc.perform(get("/api/fetch").header(CORRELATION_HEADER, "corr-1").param("mobileNumber", MOBILE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardNumber").value("1234567890123456"))
                .andExpect(jsonPath("$.totalLimit").value(100_000));
    }

    @Test
    void fetchWithoutCorrelationHeaderReturns400() throws Exception {
        mockMvc.perform(get("/api/fetch").param("mobileNumber", MOBILE))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cardsService);
    }

    @Test
    void fetchUnknownCardReturns404() throws Exception {
        when(cardsService.fetchCard(MOBILE))
                .thenThrow(new ResourceNotFoundException("Cards not found for mobile: " + MOBILE));

        mockMvc.perform(get("/api/fetch").header(CORRELATION_HEADER, "corr-1").param("mobileNumber", MOBILE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("404 NOT_FOUND"))
                .andExpect(jsonPath("$.errorMessage").value("Cards not found for mobile: " + MOBILE));
    }

    @Test
    void updateReturns200WhenUpdated() throws Exception {
        when(cardsService.updateCard(any())).thenReturn(true);

        mockMvc.perform(put("/api/update").contentType(MediaType.APPLICATION_JSON).content(VALID_CARD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("200"));
    }

    @Test
    void updateReturns417WhenNotUpdated() throws Exception {
        when(cardsService.updateCard(any())).thenReturn(false);

        mockMvc.perform(put("/api/update").contentType(MediaType.APPLICATION_JSON).content(VALID_CARD))
                .andExpect(status().isExpectationFailed())
                .andExpect(jsonPath("$.statusCode").value("417"));
    }

    @Test
    void updateWithInvalidBodyReturns400WithFieldErrors() throws Exception {
        String invalid = """
                {"mobileNumber":"12","cardNumber":"1234567890123456","cardType":"BOAT",
                 "totalLimit":-1,"amountUsed":0,"availableAmount":0}
                """;

        mockMvc.perform(put("/api/update").contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mobileNumber").value("Mobile number must 10 digits"))
                .andExpect(jsonPath("$.cardType").value("Card Type must be CREDIT or DEBIT"))
                .andExpect(jsonPath("$.totalLimit").value("Total limit must be >= 0"));

        verifyNoInteractions(cardsService);
    }

    @Test
    void updateUnknownCardReturns404() throws Exception {
        when(cardsService.updateCard(any())).thenThrow(new ResourceNotFoundException("Card not found"));

        mockMvc.perform(put("/api/update").contentType(MediaType.APPLICATION_JSON).content(VALID_CARD))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReturns200WhenDeleted() throws Exception {
        when(cardsService.deleteCard(MOBILE)).thenReturn(true);

        mockMvc.perform(delete("/api/delete").param("mobileNumber", MOBILE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusMessage").value("Request processed successfully"));
    }

    @Test
    void deleteReturns417WhenNotDeleted() throws Exception {
        when(cardsService.deleteCard(MOBILE)).thenReturn(false);

        mockMvc.perform(delete("/api/delete").param("mobileNumber", MOBILE))
                .andExpect(status().isExpectationFailed())
                .andExpect(jsonPath("$.statusCode").value("417"));
    }

    @Test
    void unexpectedErrorReturns500WithErrorResponse() throws Exception {
        when(cardsService.fetchCard(MOBILE)).thenThrow(new IllegalStateException("boom"));

        mockMvc.perform(get("/api/fetch").header(CORRELATION_HEADER, "corr-1").param("mobileNumber", MOBILE))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("500 INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.errorMessage").value("boom"));
    }

    @Test
    void contactInfoReturns200() throws Exception {
        mockMvc.perform(get("/api/contact"))
                .andExpect(status().isOk());
    }
}
