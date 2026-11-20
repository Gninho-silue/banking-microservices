package com.easybytes.accounts.controller;

import com.easybytes.accounts.dto.CustomerDetailsDto;
import com.easybytes.accounts.exception.ResourceNotFoundException;
import com.easybytes.accounts.service.ICustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
@ActiveProfiles("test")
class CustomerControllerTest {

    private static final String MOBILE = "0612345678";
    private static final String CORRELATION_HEADER = "easybank-correlation-id";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ICustomerService customerService;

    @Test
    void fetchCustomerDetailsPassesCorrelationIdToService() throws Exception {
        CustomerDetailsDto dto = new CustomerDetailsDto();
        dto.setName("Jane Doe");
        dto.setMobileNumber(MOBILE);
        when(customerService.fetchCustomerDetails(MOBILE, "corr-1")).thenReturn(dto);

        mockMvc.perform(get("/api/fetchCustomerDetails")
                        .header(CORRELATION_HEADER, "corr-1")
                        .param("mobileNumber", MOBILE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jane Doe"))
                .andExpect(jsonPath("$.mobileNumber").value(MOBILE));
    }

    @Test
    void missingCorrelationHeaderReturns400() throws Exception {
        mockMvc.perform(get("/api/fetchCustomerDetails").param("mobileNumber", MOBILE))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(customerService);
    }

    @Test
    void unknownCustomerReturns404() throws Exception {
        when(customerService.fetchCustomerDetails(MOBILE, "corr-1"))
                .thenThrow(new ResourceNotFoundException("Customer", "mobileNumber", MOBILE));

        mockMvc.perform(get("/api/fetchCustomerDetails")
                        .header(CORRELATION_HEADER, "corr-1")
                        .param("mobileNumber", MOBILE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("404 NOT_FOUND"));
    }
}
