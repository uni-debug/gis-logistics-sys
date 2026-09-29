package com.gis.logistics.domain.payment;

public interface PaymentGateway {
    String newTxnNo();
    InitiateResponse initiate(Payment payment);

    record InitiateResponse(String channelUrl) {}
}