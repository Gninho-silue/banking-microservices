package com.easybytes.cards.controller;

import com.easybytes.cards.constants.CardsConstants;
import com.easybytes.cards.dto.CardsDto;
import com.easybytes.cards.dto.ContactInfoDto;
import com.easybytes.cards.dto.ErrorResponseDto;
import com.easybytes.cards.dto.ResponseDto;
import com.easybytes.cards.service.ICardsService;
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

@Tag(name = "Cards", description = "Cards REST API in EasyBank")
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping(path = "/api", produces = {MediaType.APPLICATION_JSON_VALUE})
public class CardsController {

    private final ICardsService cardsService;
    private final ContactInfoDto contactInfoDto;
    private Logger logger = LoggerFactory.getLogger(CardsController.class);

    @Operation(summary = "Create a new card for a customer",
            description = "Creates a new card for the given customer mobile number")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Card created successfully"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            )
    })
    @PostMapping(value = "/create")
    public ResponseEntity<ResponseDto> createCard(
            @RequestParam
            @Pattern(regexp = "(^$|[0-9]{10})", message = "Mobile number must be 10 digits")
            String mobileNumber) {
        cardsService.createCard(mobileNumber);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseDto(CardsConstants.STATUS_201, CardsConstants.MESSAGE_201));
    }

    @Operation(summary = "Get card details for a customer",
            description = "Retrieves the card details associated with the given mobile number")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Card details retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            )
    })
    @GetMapping("/fetch")
    public ResponseEntity<CardsDto> fetchCardDetails(
            @RequestHeader("easybank-correlation-id")
            String correlationId,
            @RequestParam
            @Pattern(regexp = "(^$|[0-9]{10})", message = "Mobile number must be 10 digits")
            String mobileNumber) {
        logger.debug("fetchCardDetails method start: ");
        CardsDto cardsDto = cardsService.fetchCard(mobileNumber);
        logger.debug("fetchCardDetails method end: ");
        return ResponseEntity.status(HttpStatus.OK).body(cardsDto);
    }

    @Operation(summary = "Update card details for a customer",
            description = "Updates the card details for an existing customer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Card details updated successfully"),
            @ApiResponse(responseCode = "417", description = "Expectation Failed"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            )
    })
    @PutMapping(value = "/update", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseDto> updateCard(@RequestBody @Valid CardsDto cardsDto) {
        boolean updated = cardsService.updateCard(cardsDto);
        if (updated) {
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseDto(CardsConstants.STATUS_200, CardsConstants.MESSAGE_200));
        } else {
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
                    .body(new ResponseDto(CardsConstants.STATUS_417, CardsConstants.MESSAGE_417_UPDATE));
        }
    }

    @Operation(summary = "Delete card for a customer",
            description = "Deletes the card associated with the given mobile number")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Card deleted successfully"),
            @ApiResponse(responseCode = "417", description = "Expectation Failed"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            )
    })
    @DeleteMapping(value = "/delete")
    public ResponseEntity<ResponseDto> deleteCard(
            @RequestParam @Pattern(regexp = "(^$|[0-9]{10})", message = "Mobile number must be 10 digits") String mobileNumber) {
        boolean deleted = cardsService.deleteCard(mobileNumber);
        if (deleted) {
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ResponseDto(CardsConstants.STATUS_200, CardsConstants.MESSAGE_200));
        } else {
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
                    .body(new ResponseDto(CardsConstants.STATUS_417, CardsConstants.MESSAGE_417_DELETE));
        }
    }

    @GetMapping("/contact")
    public ResponseEntity<ContactInfoDto> getContactInfo(){
        return ResponseEntity.status(HttpStatus.OK).body(contactInfoDto);

    }
}
