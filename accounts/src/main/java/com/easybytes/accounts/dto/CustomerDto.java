package com.easybytes.accounts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(name = "Customer", description = "Schema to hold Customer and Account information")
public class CustomerDto {

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

    @Schema(description = "Customer Account")
    private AccountsDto accountsDto;
}
