package com.easybytes.accounts.controller;
import com.easybytes.accounts.dto.ErrorResponseDto;

import com.easybytes.accounts.dto.CustomerDetailsDto;
import com.easybytes.accounts.service.ICustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Customers", description = "Customer REST API in EasyBank")
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping(path = "/api", produces = {MediaType.APPLICATION_JSON_VALUE})
public class CustomerController {

    private final ICustomerService customerService;
    private final Logger logger = LoggerFactory.getLogger(CustomerController.class);

    @Operation(summary = "Get Customer details",
            description = "Retrieves the customer details associated with the given phone number")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Customer details retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            )
    })
    @GetMapping("/fetchCustomerDetails")
    public ResponseEntity<CustomerDetailsDto> fetchCustomer(@RequestHeader("easybank-correlation-id")
                                                            String correlationId, @RequestParam
                                                            @Pattern(regexp = "^[0-9]{10}$", message = "Phone Number must 10 digits")
                                                            String mobileNumber) {
        logger.debug("fetchCustomerDetails method start: ");
        CustomerDetailsDto customerDetailsDto = customerService.fetchCustomerDetails(mobileNumber, correlationId);
        logger.debug("fetchCustomerDetails method end: ");
        return ResponseEntity.status(HttpStatus.OK).body(customerDetailsDto);

    }
}
