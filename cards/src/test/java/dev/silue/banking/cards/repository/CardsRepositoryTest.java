package dev.silue.banking.cards.repository;

import dev.silue.banking.cards.audit.AuditAwareImpl;
import dev.silue.banking.cards.config.JpaAuditingConfig;
import dev.silue.banking.cards.entity.Cards;
import dev.silue.banking.cards.support.MySqlTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository integration tests against a real MySQL (Testcontainers), with the schema
 * created by {@code src/main/resources/schema.sql}.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({MySqlTestContainerConfig.class, JpaAuditingConfig.class, AuditAwareImpl.class})
class CardsRepositoryTest {

    @Autowired
    private CardsRepository cardsRepository;

    private Cards saveCard(String mobile, String cardNumber) {
        Cards l = new Cards();
        l.setMobileNumber(mobile);
        l.setCardNumber(cardNumber);
        l.setCardType("CREDIT");
        l.setTotalLimit(100_000);
        l.setAmountUsed(0);
        l.setAvailableAmount(100_000);
        return cardsRepository.saveAndFlush(l);
    }

    @Test
    void savedCardGetsIdAndAuditColumns() {
        Cards saved = saveCard("0611111111", "1111222233334444");

        assertThat(saved.getCardId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getCreatedBy()).isEqualTo("CARDS_SERVICE");
    }

    @Test
    void findsByMobileNumber() {
        saveCard("0622222222", "2222333344445555");

        assertThat(cardsRepository.findByMobileNumber("0622222222"))
                .get().extracting(Cards::getCardNumber).isEqualTo("2222333344445555");
        assertThat(cardsRepository.findByMobileNumber("0699999999")).isEmpty();
    }

    @Test
    void findsAndChecksExistenceByCardNumber() {
        saveCard("0633333333", "3333444455556666");

        assertThat(cardsRepository.existsByCardNumber("3333444455556666")).isTrue();
        assertThat(cardsRepository.existsByCardNumber("0000000000000000")).isFalse();
        assertThat(cardsRepository.findByCardNumber("3333444455556666"))
                .get().extracting(Cards::getMobileNumber).isEqualTo("0633333333");
    }

    @Test
    void persistsUpdatedAmounts() {
        Cards card = saveCard("0644444444", "4444555566667777");
        card.setAmountUsed(30_000);
        card.setAvailableAmount(70_000);
        cardsRepository.saveAndFlush(card);

        Cards reloaded = cardsRepository.findById(card.getCardId()).orElseThrow();
        assertThat(reloaded.getAmountUsed()).isEqualTo(30_000);
        assertThat(reloaded.getAvailableAmount()).isEqualTo(70_000);
    }
}
