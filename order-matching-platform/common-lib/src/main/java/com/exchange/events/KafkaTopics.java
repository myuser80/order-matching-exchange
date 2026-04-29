package com.exchange.events;

public final class KafkaTopics {
    private KafkaTopics() {}

    public static final String ORDER_SUBMITTED   = "order.submitted";
    public static final String TRADE_EXECUTED    = "trade.executed";
    public static final String EXECUTION_REPORT  = "execution.report";
    public static final String ORDER_STATUS      = "order.status";
}
