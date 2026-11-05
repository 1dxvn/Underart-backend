package com.underart.domain.port.in;

import com.underart.domain.model.User;
import java.util.UUID;

public interface UpdatePaymentMethodUseCase {

    User updatePaymentToken(UUID userId, String paymentToken);
}
