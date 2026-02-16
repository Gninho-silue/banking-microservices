package com.easybytes.cards.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(name = "Card", description = "Schema to hold Card information")
public class CardsDto {

    @Schema(description = "Customer Mobile Number", example = "0612345678")
    @NotBlank(message = "Customer mobile number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must 10 digits")
    private String mobileNumber;

    @Schema(description = "Card Number", example = "4591123412341234")
    @Digits(integer = 19, fraction = 0, message = "Card Number must be numeric")
    @Size(min = 12, max = 19, message = "Card Number must be between 12 and 19 digits")
    private String cardNumber;

    @Schema(description = "Type of the card", example = "CREDIT")
    @NotBlank(message = "Card Type is required")
    @Pattern(regexp = "CREDIT|DEBIT", message = "Card Type must be CREDIT or DEBIT")
    private String cardType;

    @Schema(description = "Total credit/debit limit")
    @NotNull
    @Min(value = 0, message = "Total limit must be >= 0")
    private Integer totalLimit;

    @Schema(description = "Amount already used")
    @NotNull
    @Min(value = 0, message = "Amount used must be >= 0")
    private Integer amountUsed;

    @Schema(description = "Available amount")
    @NotNull
    @Min(value = 0, message = "Available amount must be >= 0")
    private Integer availableAmount;
}
