package com.underart.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.underart.domain.model.User;
import com.underart.domain.port.in.FindUserUseCase;
import com.underart.domain.port.out.PaymentGatewayPort;
import com.underart.domain.port.out.UserRepositoryPort;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UpdatePaymentMethodServiceTest {

    private final FindUserUseCase users = mock(FindUserUseCase.class);
    private final UserRepositoryPort repository = mock(UserRepositoryPort.class);
    private final PaymentGatewayPort gateway = mock(PaymentGatewayPort.class);
    private final UpdatePaymentMethodService service = new UpdatePaymentMethodService(users, repository, gateway);

    @Test
    void storesTheTokenReturnedByTheGatewayNotTheRawReference() {
        UUID id = UUID.randomUUID();
        User user = new User(id, "ana", "ana@test.com", "hash", null, null, null, Instant.now());
        when(users.findById(id)).thenReturn(user);
        when(gateway.registerPaymentMethod("ana@test.com", "pm_card_visa")).thenReturn("cus_1:pm_card_visa");
        when(repository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User saved = service.updatePaymentToken(id, "  pm_card_visa ");

        assertThat(saved.paymentToken()).isEqualTo("cus_1:pm_card_visa");
        assertThat(saved.email()).isEqualTo("ana@test.com");
    }
}
