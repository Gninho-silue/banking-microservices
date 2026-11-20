package com.easybytes.cards.mapper;

import com.easybytes.cards.dto.CardsDto;
import com.easybytes.cards.entity.Cards;

public class CardsMapper {

    public static CardsDto toCardsDto(Cards cards, CardsDto dto) {
        dto.setMobileNumber(cards.getMobileNumber());
        dto.setCardNumber(cards.getCardNumber());
        dto.setCardType(cards.getCardType());
        dto.setTotalLimit(cards.getTotalLimit());
        dto.setAmountUsed(cards.getAmountUsed());
        dto.setAvailableAmount(cards.getAvailableAmount());
        return dto;
    }

    public static Cards toCards(CardsDto dto, Cards entity) {
        entity.setMobileNumber(dto.getMobileNumber());
        entity.setCardNumber(dto.getCardNumber());
        entity.setCardType(dto.getCardType());
        entity.setTotalLimit(dto.getTotalLimit());
        entity.setAmountUsed(dto.getAmountUsed());
        entity.setAvailableAmount(dto.getAvailableAmount());
        return entity;
    }
}
