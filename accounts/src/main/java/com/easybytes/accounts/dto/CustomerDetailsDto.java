package com.easybytes.accounts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "CustomerDetails", description = "Schema to hold Customer, Account. Cards and Loans information")
public class CustomerDetailsDto {

    @Schema(description = "Customer Name", example = "Dev Tech")
    @NotBlank(message = "Customer Name is required")
    @Size(min = 3, max = 100, message = "Customer Name must be between 3 and 100 characters")
    private String name;

    @Schema(description = "Customer Email", example = "dev225tech@gmail.com")
    @NotBlank(message = "Customer Email is required")
    @Email(message = "Invalid Email")
    private String email;
    @Schema(description = "Customer mobile number", example = "0612345678")
    @NotBlank(message = "Customer mobile number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must  10 digits")
    private String mobileNumber;

    @Schema(description = "Customer Account's details")
    private AccountsDto accountsDto;

    @Schema(description = "Customer's Cards details")
    private CardsDto cardsDto;

    @Schema(description = "Customer's Loans details")
    private LoansDto loansDto;
}
