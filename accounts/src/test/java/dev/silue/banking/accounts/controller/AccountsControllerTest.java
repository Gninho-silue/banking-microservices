package dev.silue.banking.accounts.controller;

import dev.silue.banking.accounts.dto.AccountBalanceDto;
import dev.silue.banking.accounts.dto.AccountsDto;
import dev.silue.banking.accounts.dto.CustomerDto;
import dev.silue.banking.accounts.exception.CustomerAlreadyExistsException;
import dev.silue.banking.accounts.exception.ResourceNotFoundException;
import dev.silue.banking.accounts.service.IAccountsService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountsController.class)
@ActiveProfiles("test")
class AccountsControllerTest {

    private static final String MOBILE = "0612345678";
    private static final String VALID_CUSTOMER = """
            {"name":"Jane Doe","email":"jane@example.com","mobileNumber":"0612345678"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IAccountsService accountsService;

    @Test
    void createReturns201() throws Exception {
        mockMvc.perform(post("/api/create").contentType(MediaType.APPLICATION_JSON).content(VALID_CUSTOMER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value("201"))
                .andExpect(jsonPath("$.statusMessage").value("Account created successfully"));

        verify(accountsService).createAccount(any(CustomerDto.class));
    }

    @Test
    void createWithInvalidBodyReturns400WithFieldErrors() throws Exception {
        String invalid = """
                {"name":"J","email":"not-an-email","mobileNumber":"123"}
                """;

        mockMvc.perform(post("/api/create").contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").value("Customer Name must be between 3 and 100 characters"))
                .andExpect(jsonPath("$.email").value("Invalid Email"))
                .andExpect(jsonPath("$.mobileNumber").value("Mobile number must  10 digits"));

        verifyNoInteractions(accountsService);
    }

    @Test
    void createForExistingCustomerReturns400WithErrorResponse() throws Exception {
        org.mockito.Mockito.doThrow(new CustomerAlreadyExistsException("Customer already registered"))
                .when(accountsService).createAccount(any());

        mockMvc.perform(post("/api/create").contentType(MediaType.APPLICATION_JSON).content(VALID_CUSTOMER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.apiPath").value("uri=/api/create"))
                .andExpect(jsonPath("$.errorCode").value("400 BAD_REQUEST"))
                .andExpect(jsonPath("$.errorMessage").value("Customer already registered"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void fetchReturnsAccountDetails() throws Exception {
        CustomerDto dto = new CustomerDto();
        dto.setName("Jane Doe");
        dto.setEmail("jane@example.com");
        dto.setMobileNumber(MOBILE);
        AccountsDto accountsDto = new AccountsDto();
        accountsDto.setAccountNumber(1_234_567_890L);
        accountsDto.setAccountType("Savings");
        dto.setAccountsDto(accountsDto);
        when(accountsService.getAccountDetails(MOBILE)).thenReturn(dto);

        mockMvc.perform(get("/api/fetch").param("mobileNumber", MOBILE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jane Doe"))
                .andExpect(jsonPath("$.accountsDto.accountNumber").value(1_234_567_890L));
    }

    @Test
    void fetchUnknownCustomerReturns404() throws Exception {
        when(accountsService.getAccountDetails(MOBILE))
                .thenThrow(new ResourceNotFoundException("Customer", "mobileNumber", MOBILE));

        mockMvc.perform(get("/api/fetch").param("mobileNumber", MOBILE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("404 NOT_FOUND"))
                .andExpect(jsonPath("$.errorMessage").value(containsString(MOBILE)));
    }

    @Test
    void fetchWithMalformedMobileNumberReturns400() throws Exception {
        mockMvc.perform(get("/api/fetch").param("mobileNumber", "123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.apiPath").value("uri=/api/fetch"))
                .andExpect(jsonPath("$.errorCode").value("400 BAD_REQUEST"))
                .andExpect(jsonPath("$.errorMessage").value("Mobile Number must 10 digits"));

        verifyNoInteractions(accountsService);
    }

    @Test
    void deleteWithMalformedMobileNumberReturns400() throws Exception {
        mockMvc.perform(delete("/api/delete").param("mobileNumber", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("Mobile Number must 10 digits"));

        verifyNoInteractions(accountsService);
    }

    @Test
    void updateReturns200WhenUpdated() throws Exception {
        when(accountsService.updateAccount(any())).thenReturn(true);

        mockMvc.perform(put("/api/update").contentType(MediaType.APPLICATION_JSON).content(VALID_CUSTOMER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("200"));
    }

    @Test
    void updateReturns500With417BodyWhenNotUpdated() throws Exception {
        when(accountsService.updateAccount(any())).thenReturn(false);

        mockMvc.perform(put("/api/update").contentType(MediaType.APPLICATION_JSON).content(VALID_CUSTOMER))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.statusCode").value("417"))
                .andExpect(jsonPath("$.statusMessage").value(containsString("Update operation failed")));
    }

    @Test
    void updateUnknownAccountReturns404() throws Exception {
        when(accountsService.updateAccount(any()))
                .thenThrow(new ResourceNotFoundException("Account", "AccountNumber", "1"));

        mockMvc.perform(put("/api/update").contentType(MediaType.APPLICATION_JSON).content(VALID_CUSTOMER))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReturns200WhenDeleted() throws Exception {
        when(accountsService.deleteAccount(MOBILE)).thenReturn(true);

        mockMvc.perform(delete("/api/delete").param("mobileNumber", MOBILE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusMessage").value("Request processed successfully"));
    }

    @Test
    void deleteReturns500With417BodyWhenNotDeleted() throws Exception {
        when(accountsService.deleteAccount(MOBILE)).thenReturn(false);

        mockMvc.perform(delete("/api/delete").param("mobileNumber", MOBILE))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.statusCode").value("417"));
    }

    @Test
    void unexpectedErrorReturns500WithErrorResponse() throws Exception {
        when(accountsService.getAccountDetails(MOBILE)).thenThrow(new IllegalStateException("boom"));

        mockMvc.perform(get("/api/fetch").param("mobileNumber", MOBILE))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("500 INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.errorMessage").value("boom"));
    }

    @Test
    void depositReturnsNewBalance() throws Exception {
        when(accountsService.deposit(eq(1_000_000_001L), any()))
                .thenReturn(new AccountBalanceDto(1_000_000_001L, new BigDecimal("500.00")));

        mockMvc.perform(post("/api/accounts/1000000001/deposits")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":500}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value(1_000_000_001L))
                .andExpect(jsonPath("$.balance").value(500.00));
    }

    @Test
    void depositWithNonPositiveAmountIs400() throws Exception {
        mockMvc.perform(post("/api/accounts/1000000001/deposits")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.amount").value("Amount must be greater than 0"));

        verifyNoInteractions(accountsService);
    }

    @Test
    void depositOnUnknownAccountIs404() throws Exception {
        when(accountsService.deposit(eq(1_000_000_001L), any()))
                .thenThrow(new ResourceNotFoundException("Account", "accountNumber", "1000000001"));

        mockMvc.perform(post("/api/accounts/1000000001/deposits")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":5}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void contactInfoReturns200() throws Exception {
        mockMvc.perform(get("/api/contact"))
                .andExpect(status().isOk());
    }
}
