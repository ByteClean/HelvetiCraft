package com.HelvetiCraft.util;

import java.util.UUID;
import com.HelvetiCraft.finance.FinanceManager;
import com.HelvetiCraft.requests.TransactionLogRequests;

public class FinanceTransactionLogger {

    private final FinanceManager financeManager;

    public FinanceTransactionLogger(FinanceManager financeManager) {
        this.financeManager = financeManager;
    }

    public void logTransaction(String transactionType, UUID fromUuid, UUID toUuid, long sumTotal) {

        // Handle exceptional null cases safely
        if (fromUuid == null && toUuid == null) {
            // Nothing to modify, but still log
            TransactionLogRequests.prepareRequest(transactionType, null, null, sumTotal);
            return;
        }

        // Case 1 → only TO exists (incoming money, e.g. GOV gives money)
        if (fromUuid == null) {
            financeManager.addToMain(toUuid, sumTotal);
        }

        // Case 2 → only FROM exists (money removed, e.g. tax, fee)
        else if (toUuid == null) {
            financeManager.addToMain(fromUuid, -sumTotal);
        }

        // Case 3 → standard transfer between both accounts
        else {
            financeManager.addToMain(fromUuid, -sumTotal);
            financeManager.addToMain(toUuid, sumTotal);
        }

        // Log transaction (always)
        TransactionLogRequests.prepareRequest(
                transactionType,
                fromUuid,
                toUuid,
                sumTotal
        );
    }
}
