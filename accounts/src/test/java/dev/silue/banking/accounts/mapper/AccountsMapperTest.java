package dev.silue.banking.accounts.mapper;

import dev.silue.banking.accounts.dto.AccountsDto;
import dev.silue.banking.accounts.entity.Accounts;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class AccountsMapperTest {

    @Test
    void exposesBalanceInDto() {
        Accounts account = new Accounts();
        account.setAccountNumber(1_000_000_001L);
        account.setBalance(new BigDecimal("42.00"));

        AccountsDto dto = AccountsMapper.toAccountsDto(account, new AccountsDto());

        assertThat(dto.getBalance()).isEqualTo(new BigDecimal("42.00"));
    }

    @Test
    void updateFromDtoNeverOverwritesBalance() {
        Accounts account = new Accounts();
        account.setAccountNumber(1_000_000_001L);
        account.setBalance(new BigDecimal("42.00"));
        AccountsDto dto = new AccountsDto();
        dto.setAccountNumber(1_000_000_001L);
        dto.setAccountType("CURRENT");
        dto.setBranchAddress("1 New Street");
        dto.setBalance(new BigDecimal("1000000.00"));

        AccountsMapper.toAccounts(dto, account);

        assertThat(account.getAccountType()).isEqualTo("CURRENT");
        assertThat(account.getBalance()).isEqualTo(new BigDecimal("42.00"));
    }
}
