package com.easybytes.cards.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Schema(name = "ErrorResponse", description = "Schema to hold Error Response information")
public class ErrorResponseDto {
    @Schema(description = "Api path invoked by client")
    private String apiPath;
    @Schema(description = "Status Code")
    private HttpStatus errorCode;
    @Schema(description = "Status Message")
    private String errorMessage;
    @Schema(description = "Timestamp")
    private LocalDateTime timestamp;
}
