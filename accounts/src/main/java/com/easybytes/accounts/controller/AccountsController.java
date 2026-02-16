package com.easybytes.accounts.controller;

import com.easybytes.accounts.constants.AccountsConstants;
import com.easybytes.accounts.dto.ContactInfoDto;
import com.easybytes.accounts.dto.CustomerDto;
import com.easybytes.accounts.dto.ResponseDto;
import com.easybytes.accounts.dto.ErrorResponseDto;
import com.easybytes.accounts.service.IAccountsService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Accounts", description = "Accounts REST API in EasyBank")
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping(path = "/api", produces = {MediaType.APPLICATION_JSON_VALUE})
public class AccountsController {

    private final IAccountsService iAccountsService;
    private final ContactInfoDto contactInfoDto;
    private final Environment environment;
    private final Logger logger = LoggerFactory.getLogger(AccountsController.class);

    @Operation(summary = "Create a new account for a customer",
            description = "Creates a new account for the given customer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Account created successfully"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            )
    })
    @PostMapping(value = "/create")
    public ResponseEntity<ResponseDto> createAccount(@RequestBody @Valid CustomerDto customerDto) {
        iAccountsService.createAccount(customerDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseDto(AccountsConstants.STATUS_201, AccountsConstants.MESSAGE_201));
    }

    @Operation(summary = "Get account details for a customer",
            description = "Retrieves the account details associated with the given mobile number")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account details retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            )
    })
    @GetMapping("/fetch")
    public ResponseEntity<CustomerDto> getAccountDetails(
            @RequestParam
            @Pattern(regexp = "^[0-9]{10}$", message = "Mobile Number must 10 digits")
            String mobileNumber) {

        CustomerDto customerDto = iAccountsService.getAccountDetails(mobileNumber);
        return ResponseEntity.status(HttpStatus.OK).body(customerDto);
    }

    @Operation(summary = "Update account details for a customer",
            description = "Updates the account details for an existing customer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account details updated successfully"),
            @ApiResponse(responseCode = "417", description = "Expectation Failed"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            )
    })
    @PutMapping(value = "/update")
    public ResponseEntity<ResponseDto> updateAccount(
            @RequestBody @Valid CustomerDto customerDto) {
        boolean updated = iAccountsService.updateAccount(customerDto);
        if (updated) {
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseDto(AccountsConstants.STATUS_200, AccountsConstants.MESSAGE_200));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ResponseDto(AccountsConstants.STATUS_417, AccountsConstants.MESSAGE_417_UPDATE)
            );
        }
    }

    @Operation(summary = "Delete account for a customer",
            description = "Deletes the account associated with the given mobile number")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account deleted successfully"),
            @ApiResponse(responseCode = "417", description = "Expectation Failed"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            )
    })
    @DeleteMapping(value = "/delete")
    public ResponseEntity<ResponseDto> deleteAccount(
            @RequestParam @Pattern(regexp = "^[0-9]{10}$", message = "Mobile Number must 10 digits") String mobileNumber) {

        boolean deleted = iAccountsService.deleteAccount(mobileNumber);
        if (deleted) {
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseDto(AccountsConstants.STATUS_200, AccountsConstants.MESSAGE_200));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new ResponseDto(AccountsConstants.STATUS_417, AccountsConstants.MESSAGE_417_DELETE)
            );
        }
    }

    @Retry(name = "contactInfoRetry", fallbackMethod = "fallbackContactInfo")
    @GetMapping("/contact")
    public ResponseEntity<ContactInfoDto> getContactInfo() {
        logger.debug("Contact Info API called");
        return ResponseEntity.status(HttpStatus.OK).body(contactInfoDto);

    }

    private ResponseEntity<ContactInfoDto> fallbackContactInfo(Throwable throwable) {
        logger.error("Contact Info API failed with error: {}", throwable.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ContactInfoDto());
    }

    @RateLimiter(name = "rateLimiter", fallbackMethod = "fallbackRateLimiter")
    @GetMapping("/java-version")
    public ResponseEntity<String> getJavaVersion() {
        return ResponseEntity.ok().body(environment.getProperty("JAVA_HOME"));
    }

    private ResponseEntity<String> fallbackRateLimiter(Throwable throwable) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Rate limit exceeded");
    }
}
