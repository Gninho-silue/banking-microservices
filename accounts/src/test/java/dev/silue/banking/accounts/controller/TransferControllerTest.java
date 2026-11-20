package com.easybytes.accounts.controller;

import com.easybytes.accounts.dto.TransferDto;
import com.easybytes.accounts.dto.TransferRequestDto;
import com.easybytes.accounts.exception.IdempotencyKeyConflictException;
import com.easybytes.accounts.exception.InsufficientFundsException;
import com.easybytes.accounts.exception.InvalidTransferException;
import com.easybytes.accounts.exception.ResourceNotFoundException;
import com.easybytes.accounts.service.ITransferService;
import com.easybytes.accounts.service.TransferResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransferController.class)
@ActiveProfiles("test")
class TransferControllerTest {

    private static final String KEY_HEADER = "Idempotency-Key";
    private static final String BODY = """
            {"fromAccount":1000000001,"toAccount":1000000002,"amount":150.00}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ITransferService transferService;

    private static TransferDto transferDto() {
        TransferDto dto = new TransferDto();
        dto.setId(7L);
        dto.setFromAccount(1_000_000_001L);
        dto.setToAccount(1_000_000_002L);
        dto.setAmount(new BigDecimal("150.00"));
        dto.setStatus("COMPLETED");
        dto.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        return dto;
    }

    @Test
    void createsTransferAndReturns201WithLocation() throws Exception {
        when(transferService.transfer(eq("k-1"), any())).thenReturn(new TransferResult(transferDto(), false));

        mockMvc.perform(post("/api/transfers").header(KEY_HEADER, "k-1")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/transfers/7"))
                .andExpect(header().doesNotExist("Idempotent-Replayed"))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.amount").value(150.00))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        ArgumentCaptor<TransferRequestDto> captor = ArgumentCaptor.forClass(TransferRequestDto.class);
        verify(transferService).transfer(eq("k-1"), captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("150");
    }

    @Test
    void replayReturnsTheSameResponseFlaggedAsReplayed() throws Exception {
        when(transferService.transfer(eq("k-1"), any())).thenReturn(new TransferResult(transferDto(), true));

        mockMvc.perform(post("/api/transfers").header(KEY_HEADER, "k-1")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/transfers/7"))
                .andExpect(header().string("Idempotent-Replayed", "true"))
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void missingIdempotencyKeyIs400() throws Exception {
        mockMvc.perform(post("/api/transfers").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(transferService);
    }

    @Test
    void tooLongIdempotencyKeyIs400() throws Exception {
        mockMvc.perform(post("/api/transfers").header(KEY_HEADER, "k".repeat(101))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("Idempotency-Key must be at most 100 characters"));

        verifyNoInteractions(transferService);
    }

    @Test
    void invalidBodyIs400WithFieldErrors() throws Exception {
        String invalid = """
                {"fromAccount":null,"toAccount":1000000002,"amount":0.001}
                """;

        mockMvc.perform(post("/api/transfers").header(KEY_HEADER, "k-1")
                        .contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fromAccount").value("Source account is required"))
                .andExpect(jsonPath("$.amount").exists());

        verifyNoInteractions(transferService);
    }

    @Test
    void businessRuleViolationIs400() throws Exception {
        when(transferService.transfer(eq("k-1"), any()))
                .thenThrow(new InvalidTransferException("Source and destination accounts must be different"));

        mockMvc.perform(post("/api/transfers").header(KEY_HEADER, "k-1")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("400 BAD_REQUEST"))
                .andExpect(jsonPath("$.errorMessage").value("Source and destination accounts must be different"));
    }

    @Test
    void insufficientFundsIs422() throws Exception {
        when(transferService.transfer(eq("k-1"), any())).thenThrow(new InsufficientFundsException(1_000_000_001L));

        mockMvc.perform(post("/api/transfers").header(KEY_HEADER, "k-1")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.apiPath").value("uri=/api/transfers"))
                .andExpect(jsonPath("$.errorCode").value("422 UNPROCESSABLE_CONTENT"))
                .andExpect(jsonPath("$.errorMessage").value("Insufficient funds on account 1000000001"));
    }

    @Test
    void reusedKeyWithDifferentPayloadIs409() throws Exception {
        when(transferService.transfer(eq("k-1"), any())).thenThrow(new IdempotencyKeyConflictException("k-1"));

        mockMvc.perform(post("/api/transfers").header(KEY_HEADER, "k-1")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("409 CONFLICT"));
    }

    @Test
    void unknownAccountIs404() throws Exception {
        when(transferService.transfer(eq("k-1"), any()))
                .thenThrow(new ResourceNotFoundException("Account", "accountNumber", "1000000002"));

        mockMvc.perform(post("/api/transfers").header(KEY_HEADER, "k-1")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void getsTransferById() throws Exception {
        when(transferService.getTransfer(7L)).thenReturn(transferDto());

        mockMvc.perform(get("/api/transfers/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromAccount").value(1_000_000_001L));
    }

    @Test
    void unknownTransferIs404() throws Exception {
        when(transferService.getTransfer(99L)).thenThrow(new ResourceNotFoundException("Transfer", "id", "99"));

        mockMvc.perform(get("/api/transfers/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listsTransfersOfAnAccount() throws Exception {
        when(transferService.getTransfersForAccount(1_000_000_001L)).thenReturn(List.of(transferDto(), transferDto()));

        mockMvc.perform(get("/api/accounts/1000000001/transfers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }
}
