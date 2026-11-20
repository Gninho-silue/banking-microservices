package dev.silue.banking.accounts.service.client;

import dev.silue.banking.accounts.dto.LoansDto;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class LoansFallback implements LoansFeignClient {
    /**
     * @param correlationId
     * @param mobileNumber
     * @return
     */
    @Override
    public ResponseEntity<LoansDto> fetchLoanDetails(String correlationId, String mobileNumber) {
        // Return null to indicate service is unavailable
        // The calling service should handle this gracefully
        return ResponseEntity.ok(null);
    }
}
