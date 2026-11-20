package dev.silue.banking.accounts.controller;

import dev.silue.banking.accounts.dto.ErrorResponseDto;
import dev.silue.banking.accounts.dto.TransferDto;
import dev.silue.banking.accounts.dto.TransferRequestDto;
import dev.silue.banking.accounts.service.ITransferService;
import dev.silue.banking.accounts.service.TransferResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@Tag(name = "Transfers", description = "Idempotent money transfers between EasyBank accounts")
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping(path = "/api", produces = {MediaType.APPLICATION_JSON_VALUE})
public class TransferController {

    static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    static final String REPLAYED_HEADER = "Idempotent-Replayed";

    private final ITransferService transferService;

    @Operation(summary = "Transfer money between two accounts",
            description = "Debits the source account and credits the destination account atomically. "
                    + "The request must carry a unique Idempotency-Key: replaying the same key with the same "
                    + "payload returns the original response (flagged with Idempotent-Replayed: true) without "
                    + "moving money again.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Transfer completed (or replayed)",
                    headers = @Header(name = REPLAYED_HEADER, description = "true when the key was already processed")),
            @ApiResponse(responseCode = "400", description = "Invalid request or missing Idempotency-Key",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Unknown account",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "409", description = "Idempotency-Key reused with a different payload",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "422", description = "Insufficient funds",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    @PostMapping(value = "/transfers", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TransferDto> createTransfer(
            @Parameter(description = "Client-generated unique key (e.g. a UUID), at most 100 characters")
            @RequestHeader(IDEMPOTENCY_KEY_HEADER)
            @NotBlank(message = "Idempotency-Key must not be blank")
            @Size(max = 100, message = "Idempotency-Key must be at most 100 characters")
            String idempotencyKey,
            @RequestBody @Valid TransferRequestDto request) {
        TransferResult result = transferService.transfer(idempotencyKey, request);
        ResponseEntity.BodyBuilder response = ResponseEntity
                .created(URI.create("/api/transfers/" + result.transfer().getId()));
        if (result.replayed()) {
            response.header(REPLAYED_HEADER, "true");
        }
        return response.body(result.transfer());
    }

    @Operation(summary = "Get a transfer", description = "Retrieves a completed transfer by its id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transfer found"),
            @ApiResponse(responseCode = "404", description = "Unknown transfer",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    @GetMapping("/transfers/{id}")
    public ResponseEntity<TransferDto> getTransfer(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(transferService.getTransfer(id));
    }

    @Operation(summary = "List the transfers of an account",
            description = "Transfers where the account is the source or the destination, newest first")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transfers listed"),
            @ApiResponse(responseCode = "404", description = "Unknown account",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    @GetMapping("/accounts/{accountNumber}/transfers")
    public ResponseEntity<List<TransferDto>> getTransfersForAccount(@PathVariable Long accountNumber) {
        return ResponseEntity.status(HttpStatus.OK).body(transferService.getTransfersForAccount(accountNumber));
    }
}
