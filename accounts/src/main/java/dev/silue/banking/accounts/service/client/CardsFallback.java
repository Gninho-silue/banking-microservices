package dev.silue.banking.accounts.service.client;

import dev.silue.banking.accounts.dto.CardsDto;
import dev.silue.banking.accounts.dto.LoansDto;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class CardsFallback implements CardsFeignClient {

    /**
     * @param correlationId
     * @param mobileNumber
     * @return
     */
    @Override
    public ResponseEntity<CardsDto> fetchCardDetails(String correlationId, String mobileNumber) {
        // Return null to indicate service is unavailable
        // The calling service should handle this gracefully
        return ResponseEntity.ok(null);
    }
}
