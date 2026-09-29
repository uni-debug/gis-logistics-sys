package com.gis.logistics.domain.payment;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SandboxPaymentGateway implements PaymentGateway {
    @Override
    public String newTxnNo() {
        return "SANDBOX-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    @Override
    public InitiateResponse initiate(Payment payment) {
        return new InitiateResponse("sandbox://pay/" + payment.getTxnNo());
    }
}