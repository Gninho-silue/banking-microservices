package dev.silue.banking.accounts.functions;

import dev.silue.banking.accounts.service.IAccountsService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AccountsFunctionsTest {

    @Test
    void updateCommunicationConsumerFlagsTheAccount() {
        IAccountsService accountsService = mock(IAccountsService.class);

        new AccountsFunctions().updateCommunication(accountsService).accept(1_234_567_890L);

        verify(accountsService).updateCommunicationStatus(1_234_567_890L);
    }
}
