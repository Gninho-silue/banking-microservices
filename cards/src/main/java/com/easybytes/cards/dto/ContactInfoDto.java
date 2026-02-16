package com.easybytes.cards.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "cards")
@Getter
@Setter
public class ContactInfoDto {
        private String message;
        private Map<String, String> contactDetails;
        private List<String> onCallSupport;

}
