package com.credit.engine.srm.currency.internal.adapter.out.http;

import java.time.Duration;

@FunctionalInterface
interface RetrySleeper {

    void sleep(Duration duration) throws InterruptedException;
}
