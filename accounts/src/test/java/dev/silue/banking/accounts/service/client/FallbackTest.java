package com.easybytes.accounts.service.client;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class FallbackTest {

    @Test
    void cardsFallbackReturnsOkWithEmptyBody() {
        var response = new CardsFallback().fetchCardDetails("corr", "0612345678");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void loansFallbackReturnsOkWithEmptyBody() {
        var response = new LoansFallback().fetchLoanDetails("corr", "0612345678");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNull();
    }
}
