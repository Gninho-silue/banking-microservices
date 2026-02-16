package com.easybytes.cards.service;

import com.easybytes.cards.dto.CardsDto;

public interface ICardsService {

    /**
     * Creates a new card for the given customer mobile number.
     *
     * @param mobileNumber 10-digit mobile number
     */
    void createCard(String mobileNumber);

    /**
     * Retrieves the card details associated with the given mobile number.
     *
     * @param mobileNumber 10-digit mobile number
     * @return the card details
     */
    CardsDto fetchCard(String mobileNumber);

    /**
     * Updates the card details for an existing customer mobile.
     *
     * @param cardsDto updated details
     * @return true if updated
     */
    boolean updateCard(CardsDto cardsDto);

    /**
     * Deletes the card associated with the given mobile number.
     *
     * @param mobileNumber 10-digit mobile number
     * @return true if deleted
     */
    boolean deleteCard(String mobileNumber);
}
