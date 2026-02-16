package com.easybytes.cards.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(name = "Response", description = "Schema to hold Response information")
public class ResponseDto {
    @Schema(description = "Status Code")
    private String statusCode;
    @Schema(description = "Status Message")
    private String statusMessage;
}
