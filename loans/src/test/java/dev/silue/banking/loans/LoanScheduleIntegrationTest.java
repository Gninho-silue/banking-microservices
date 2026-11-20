package dev.silue.banking.loans;

import dev.silue.banking.loans.support.MySqlTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP + MySQL end to end: create a loan (default terms), shorten it to 12 months, read its schedule.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(MySqlTestContainerConfig.class)
class LoanScheduleIntegrationTest {

    private static final String MOBILE = "0698765432";

    @Autowired
    private MockMvc mockMvc;

    private final JsonMapper json = JsonMapper.builder().build();

    private JsonNode getJson(String url) throws Exception {
        return json.readTree(mockMvc.perform(get(url).header("easybank-correlation-id", "it"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    @Test
    void scheduleOfA100000LoanAt5PercentOver12Months() throws Exception {
        mockMvc.perform(post("/api/create").param("mobileNumber", MOBILE)).andExpect(status().isCreated());

        JsonNode loan = getJson("/api/fetch?mobileNumber=" + MOBILE);
        assertThat(loan.get("annualInterestRate").decimalValue()).isEqualByComparingTo("0.05");
        assertThat(loan.get("termMonths").asInt()).isEqualTo(60);
        assertThat(loan.get("startDate").asString()).isEqualTo(LocalDate.now().toString());

        String update = """
                {"mobileNumber":"%s","loanNumber":"%s","loanType":"PERSONAL",
                 "totalLoan":100000.00,"amountPaid":0,"outstandingAmount":100000.00,"termMonths":12}
                """.formatted(MOBILE, loan.get("loanNumber").asString());
        mockMvc.perform(put("/api/update").contentType(MediaType.APPLICATION_JSON).content(update))
                .andExpect(status().isOk());

        JsonNode schedule = getJson("/api/loans/schedule?mobileNumber=" + MOBILE);

        assertThat(schedule.get("monthlyPayment").decimalValue()).isEqualByComparingTo("8560.75");
        JsonNode installments = schedule.get("installments");
        assertThat(installments.size()).isEqualTo(12);
        BigDecimal principals = BigDecimal.ZERO;
        for (JsonNode installment : installments) {
            principals = principals.add(installment.get("principal").decimalValue());
        }
        assertThat(principals).isEqualByComparingTo("100000.00");
        assertThat(installments.get(11).get("remaining").decimalValue()).isEqualByComparingTo("0.00");
        assertThat(schedule.get("totalPayment").decimalValue())
                .isEqualByComparingTo(new BigDecimal("100000.00").add(schedule.get("totalInterest").decimalValue()));
    }
}
