package com.easybytes.cards.repository;

import com.easybytes.cards.entity.Cards;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CardsRepository extends JpaRepository<Cards, Long> {
    Optional<Cards> findByMobileNumber(String mobileNumber);

    boolean existsByCardNumber(String cardNumber);

    Optional<Cards> findByCardNumber(String cardNumber);
}
