package dev.silue.banking.cards.service.impl;

import dev.silue.banking.cards.constants.CardsConstants;
import dev.silue.banking.cards.dto.CardsDto;
import dev.silue.banking.cards.entity.Cards;
import dev.silue.banking.cards.exception.CardAlreadyExistsException;
import dev.silue.banking.cards.exception.ResourceNotFoundException;
import dev.silue.banking.cards.repository.CardsRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardsServiceImplTest {

    private static final String MOBILE = "0612345678";

    @Mock
    private CardsRepository cardsRepository;

    @InjectMocks
    private CardsServiceImpl service;

    private static Cards card(long id, String cardNumber) {
        Cards l = new Cards();
        l.setCardId(id);
        l.setMobileNumber(MOBILE);
        l.setCardNumber(cardNumber);
        l.setCardType(CardsConstants.CREDIT_CARD);
        l.setTotalLimit(100_000);
        l.setAmountUsed(0);
        l.setAvailableAmount(100_000);
        return l;
    }

    @Nested
    class CreateCard {

        @Test
        void createsDefaultCreditCardWith16DigitNumber() {
            when(cardsRepository.findByMobileNumber(MOBILE)).thenReturn(Optional.empty());
            when(cardsRepository.existsByCardNumber(anyString())).thenReturn(false);

            service.createCard(MOBILE);

            ArgumentCaptor<Cards> captor = ArgumentCaptor.forClass(Cards.class);
            verify(cardsRepository).save(captor.capture());
            Cards saved = captor.getValue();
            assertThat(saved.getMobileNumber()).isEqualTo(MOBILE);
            assertThat(saved.getCardType()).isEqualTo(CardsConstants.CREDIT_CARD);
            assertThat(saved.getTotalLimit()).isEqualTo(CardsConstants.NEW_CARD_LIMIT);
            assertThat(saved.getAmountUsed()).isZero();
            assertThat(saved.getAvailableAmount()).isEqualTo(CardsConstants.NEW_CARD_LIMIT);
            assertThat(saved.getCardNumber()).matches("\\d{16}");
        }

        @Test
        void retriesWhenGeneratedNumberAlreadyExists() {
            when(cardsRepository.findByMobileNumber(MOBILE)).thenReturn(Optional.empty());
            when(cardsRepository.existsByCardNumber(anyString())).thenReturn(true, true, false);

            service.createCard(MOBILE);

            verify(cardsRepository, times(3)).existsByCardNumber(anyString());
        }

        @Test
        void fallsBackTo19DigitNumberAfterTenCollisions() {
            when(cardsRepository.findByMobileNumber(MOBILE)).thenReturn(Optional.empty());
            // 10 collisions on 16-digit numbers, then the 11th attempt (19 digits) is free
            when(cardsRepository.existsByCardNumber(anyString()))
                    .thenReturn(true, true, true, true, true, true, true, true, true, true, false);

            service.createCard(MOBILE);

            ArgumentCaptor<Cards> captor = ArgumentCaptor.forClass(Cards.class);
            verify(cardsRepository).save(captor.capture());
            assertThat(captor.getValue().getCardNumber()).matches("\\d{19}");
        }

        @Test
        void rejectsSecondCardForSameMobile() {
            when(cardsRepository.findByMobileNumber(MOBILE)).thenReturn(Optional.of(card(1L, "1234567890123456")));

            assertThatThrownBy(() -> service.createCard(MOBILE))
                    .isInstanceOf(CardAlreadyExistsException.class)
                    .hasMessage("Card already exists for mobile: " + MOBILE);
            verify(cardsRepository, never()).save(any());
        }
    }

    @Nested
    class FetchCard {

        @Test
        void returnsCardDetails() {
            when(cardsRepository.findByMobileNumber(MOBILE)).thenReturn(Optional.of(card(1L, "1234567890123456")));

            CardsDto dto = service.fetchCard(MOBILE);

            assertThat(dto.getMobileNumber()).isEqualTo(MOBILE);
            assertThat(dto.getCardNumber()).isEqualTo("1234567890123456");
            assertThat(dto.getCardType()).isEqualTo("CREDIT");
            assertThat(dto.getTotalLimit()).isEqualTo(100_000);
            assertThat(dto.getAmountUsed()).isZero();
            assertThat(dto.getAvailableAmount()).isEqualTo(100_000);
        }

        @Test
        void throwsWhenNoCardForMobile() {
            when(cardsRepository.findByMobileNumber(MOBILE)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.fetchCard(MOBILE))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Cards not found for mobile: " + MOBILE);
        }
    }

    @Nested
    class UpdateCard {

        @Test
        void updatesAmountsOfExistingCard() {
            Cards existing = card(1L, "1234567890123456");
            when(cardsRepository.findByCardNumber("1234567890123456")).thenReturn(Optional.of(existing));
            CardsDto dto = new CardsDto();
            dto.setMobileNumber(MOBILE);
            dto.setCardNumber("1234567890123456");
            dto.setCardType("DEBIT");
            dto.setTotalLimit(100_000);
            dto.setAmountUsed(25_000);
            dto.setAvailableAmount(75_000);

            assertThat(service.updateCard(dto)).isTrue();

            assertThat(existing.getCardType()).isEqualTo("DEBIT");
            assertThat(existing.getAmountUsed()).isEqualTo(25_000);
            assertThat(existing.getAvailableAmount()).isEqualTo(75_000);
            verify(cardsRepository).save(existing);
        }

        @Test
        void throwsWhenCardNumberUnknown() {
            when(cardsRepository.findByCardNumber("999")).thenReturn(Optional.empty());
            CardsDto dto = new CardsDto();
            dto.setCardNumber("999");

            assertThatThrownBy(() -> service.updateCard(dto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Cards not found for card number: 999");
            verify(cardsRepository, never()).save(any());
        }
    }

    @Nested
    class DeleteCard {

        @Test
        void deletesCardById() {
            when(cardsRepository.findByMobileNumber(MOBILE)).thenReturn(Optional.of(card(5L, "1234567890123456")));

            assertThat(service.deleteCard(MOBILE)).isTrue();

            verify(cardsRepository).deleteById(5L);
        }

        @Test
        void throwsWhenNoCardForMobile() {
            when(cardsRepository.findByMobileNumber(MOBILE)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteCard(MOBILE))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Cards not found for mobile: " + MOBILE);
            verify(cardsRepository, never()).deleteById(any());
        }
    }
}
