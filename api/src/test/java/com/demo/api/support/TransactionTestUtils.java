package com.demo.api.support;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

/**
 * Utilities to simulate transactional callbacks when exercising @Transactional methods in plain unit tests.
 */
public final class TransactionTestUtils {

    private TransactionTestUtils() {
    }

    public static void begin() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
    }

    public static void runAfterCommitCallbacks() {
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        synchronizations.forEach(TransactionSynchronization::afterCommit);
    }

    public static void clear() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }
}

