package com.microswitch.infrastructure.deployment;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class DeploymentResult<R> {

    private boolean success;

    private R result;

    private String errorMessage;

    private Exception exception;

    private String executedMethod;

    private Instant startTime;

    private Instant endTime;

    private Long durationMs;

    public static <R> DeploymentResult<R> success(R result, String executedMethod, 
                                                 Instant startTime, Instant endTime) {
        return DeploymentResult.<R>builder()
                .success(true)
                .result(result)
                .executedMethod(executedMethod)
                .startTime(startTime)
                .endTime(endTime)
                .durationMs(endTime.toEpochMilli() - startTime.toEpochMilli())
                .build();
    }

    public static <R> DeploymentResult<R> failure(String errorMessage, Exception exception,
                                                  Instant startTime, Instant endTime) {
        return DeploymentResult.<R>builder()
                .success(false)
                .errorMessage(errorMessage)
                .exception(exception)
                .startTime(startTime)
                .endTime(endTime)
                .durationMs(endTime.toEpochMilli() - startTime.toEpochMilli())
                .build();
    }
}
