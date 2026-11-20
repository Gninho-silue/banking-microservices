package dev.silue.banking.accounts.mapper;

import dev.silue.banking.accounts.dto.AccountsDto;
import dev.silue.banking.accounts.entity.Accounts;

public class AccountsMapper {

    public static AccountsDto toAccountsDto(Accounts accounts, AccountsDto accountsDto) {
        accountsDto.setAccountNumber(accounts.getAccountNumber());
        accountsDto.setAccountType(accounts.getAccountType());
         accountsDto.setBranchAddress(accounts.getBranchAddress());
        accountsDto.setBalance(accounts.getBalance());
         return accountsDto;

    }

    public static Accounts toAccounts(AccountsDto accountsDto,  Accounts accounts) {
        accounts.setAccountNumber(accountsDto.getAccountNumber());
        accounts.setAccountType(accountsDto.getAccountType());
        accounts.setBranchAddress(accountsDto.getBranchAddress());
        // balance is deliberately not copied: it only changes through deposits and transfers

        return accounts;
    }
}
