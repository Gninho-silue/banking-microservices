package com.easybytes.cards.service.impl;

import com.easybytes.cards.constants.CardsConstants;
import com.easybytes.cards.dto.CardsDto;
import com.easybytes.cards.entity.Cards;
import com.easybytes.cards.exception.CardAlreadyExistsException;
import com.easybytes.cards.exception.ResourceNotFoundException;
import com.easybytes.cards.mapper.CardsMapper;
import com.easybytes.cards.repository.CardsRepository;
import com.easybytes.cards.service.ICardsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class CardsServiceImpl implements ICardsService {

    private final CardsRepository cardsRepository;

    @Override
    public void createCard(String mobileNumber) {
        Optional<Cards> existing = cardsRepository.findByMobileNumber(mobileNumber);
        if (existing.isPresent()) {
            throw new CardAlreadyExistsException("Card already exists for mobile: " + mobileNumber);
        }

        Cards card = new Cards();
        card.setMobileNumber(mobileNumber);
        card.setCardType(CardsConstants.CREDIT_CARD); // default type; can be adjusted later
        card.setTotalLimit(CardsConstants.NEW_CARD_LIMIT);
        card.setAmountUsed(0);
        card.setAvailableAmount(CardsConstants.NEW_CARD_LIMIT);
        card.setCardNumber(generateUniqueCardNumber());
        cardsRepository.save(card);
    }

    @Override
    public CardsDto fetchCard(String mobileNumber) {
        Cards cards = cardsRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Cards not found for mobile: " + mobileNumber));
        return CardsMapper.toCardsDto(cards, new CardsDto());
    }

    @Override
    public boolean updateCard(CardsDto cardsDto) {
        Cards cards = cardsRepository.findByCardNumber(cardsDto.getCardNumber()).orElseThrow(
                () -> new ResourceNotFoundException("Cards not found for card number: " + cardsDto.getCardNumber())
        );

        CardsMapper.toCards(cardsDto, cards);
        cardsRepository.save(cards);
        return true;
    }

    @Override
    public boolean deleteCard(String mobileNumber) {
       Cards cards = cardsRepository.findByMobileNumber(mobileNumber).orElseThrow(
               () -> new ResourceNotFoundException("Cards not found for mobile: " + mobileNumber)
       );
       cardsRepository.deleteById(cards.getCardId());
       return true;
    }

    private String generateUniqueCardNumber() {
        String number;
        int attempts = 0;
        do {
            number = generate16DigitNumber();
            attempts++;
            if (attempts > 10) {
                // fallback to 19 digits if collision persists
                number = generate19DigitNumber();
            }
        } while (cardsRepository.existsByCardNumber(number));
        return number;
    }

    private String generate16DigitNumber() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private String generate19DigitNumber() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(19);
        for (int i = 0; i < 19; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
