package dev.silue.banking.loans.controller;

import dev.silue.banking.loans.constants.LoansConstants;
import dev.silue.banking.loans.dto.ContactInfoDto;
import dev.silue.banking.loans.dto.ErrorResponseDto;
import dev.silue.banking.loans.dto.LoanScheduleDto;
import dev.silue.banking.loans.dto.LoansDto;
import dev.silue.banking.loans.dto.ResponseDto;
import dev.silue.banking.loans.service.ILoansService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Loans", description = "Loans REST API in EasyBank")
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping(path = "/api", produces = {MediaType.APPLICATION_JSON_VALUE})
public class LoansController {

    private final ILoansService loansService;
    private final ContactInfoDto contactInfoDto;
    private final Logger logger = LoggerFactory.getLogger(LoansController.class);

    @Operation(summary = "Create a new loan for a customer",
            description = "Creates a new loan for the given customer mobile number")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Loan created successfully"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            )
    })
    @PostMapping(value = "/create")
    public ResponseEntity<ResponseDto> createLoan(
            @RequestParam
            @Pattern(regexp = "(^$|[0-9]{10})", message = "Mobile number must be 10 digits")
            String mobileNumber) {
        loansService.createLoan(mobileNumber);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseDto(LoansConstants.STATUS_201, LoansConstants.MESSAGE_201));
    }

    @Operation(summary = "Get loan details for a customer",
            description = "Retrieves the loan details associated with the given mobile number")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Loan details retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            )
    })
    @GetMapping("/fetch")
    public ResponseEntity<LoansDto> fetchLoanDetails(
            @RequestHeader("easybank-correlation-id")
            String correlationId,
            @RequestParam
            @Pattern(regexp = "(^$|[0-9]{10})", message = "Mobile number must be 10 digits")
            String mobileNumber) {
        logger.debug("fetchLoanDetails method start: ");
        LoansDto loansDto = loansService.fetchLoan(mobileNumber);
        logger.debug("fetchLoanDetails method end: ");
        return ResponseEntity.status(HttpStatus.OK).body(loansDto);
    }

    @Operation(summary = "Update loan details for a customer",
            description = "Updates the loan details for an existing customer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Loan details updated successfully"),
            @ApiResponse(responseCode = "417", description = "Expectation Failed"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            )
    })
    @PutMapping(value = "/update", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseDto> updateLoan(@RequestBody @Valid LoansDto loansDto) {
        boolean updated = loansService.updateLoan(loansDto);
        if (updated) {
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseDto(LoansConstants.STATUS_200, LoansConstants.MESSAGE_200));
        } else {
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
                    .body(new ResponseDto(LoansConstants.STATUS_417, LoansConstants.MESSAGE_417_UPDATE));
        }
    }

    @Operation(summary = "Delete loan for a customer",
            description = "Deletes the loan associated with the given mobile number")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Loan deleted successfully"),
            @ApiResponse(responseCode = "417", description = "Expectation Failed"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            )
    })
    @DeleteMapping(value = "/delete")
    public ResponseEntity<ResponseDto> deleteLoan(
            @RequestParam @Pattern(regexp = "(^$|[0-9]{10})", message = "Mobile number must be 10 digits") String mobileNumber) {
        boolean deleted = loansService.deleteLoan(mobileNumber);
        if (deleted) {
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseDto(LoansConstants.STATUS_200, LoansConstants.MESSAGE_200));
        } else {
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
                    .body(new ResponseDto(LoansConstants.STATUS_417, LoansConstants.MESSAGE_417_DELETE));
        }
    }

    @Operation(summary = "Get the amortization schedule of a customer's loan",
            description = "Constant monthly payments M = P·r / (1 − (1+r)^−n) with r = annual rate / 12, "
                    + "amounts rounded to 2 decimals (HALF_EVEN); the last installment absorbs the rounding "
                    + "difference so the remaining capital ends at exactly 0.00")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Schedule computed"),
            @ApiResponse(responseCode = "400", description = "Invalid mobile number or loan terms",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "No loan for this mobile number",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    @GetMapping("/loans/schedule")
    public ResponseEntity<LoanScheduleDto> getSchedule(
            @RequestParam
            @Pattern(regexp = "(^$|[0-9]{10})", message = "Mobile number must be 10 digits")
            String mobileNumber) {
        return ResponseEntity.status(HttpStatus.OK).body(loansService.getSchedule(mobileNumber));
    }

    @GetMapping("/contact")
    public ResponseEntity<ContactInfoDto> getContactInfo() {
        return ResponseEntity.status(HttpStatus.OK).body(contactInfoDto);

    }
}
