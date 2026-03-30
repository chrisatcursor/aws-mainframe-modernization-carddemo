package com.carddemo.common;

/**
 * Session/navigation state replacing the CICS COMMAREA (COCOM01Y).
 * Passed between controllers via HTTP session to track user context.
 */
public record SessionContext(
    GeneralInfo generalInfo,
    CustomerInfo customerInfo,
    AccountInfo accountInfo,
    CardInfo cardInfo,
    ScreenInfo screenInfo
) {

    public record GeneralInfo(
        String fromTransId,
        String fromProgram,
        String toTransId,
        String toProgram,
        String userId,
        String userType,
        int programContext
    ) {
        public static final int PGM_ENTER = 0;
        public static final int PGM_REENTER = 1;

        public boolean isAdmin() {
            return "A".equalsIgnoreCase(userType);
        }
    }

    public record CustomerInfo(
        long customerId,
        String firstName,
        String middleName,
        String lastName
    ) {}

    public record AccountInfo(
        long accountId,
        String accountStatus
    ) {}

    public record CardInfo(
        long cardNumber
    ) {}

    public record ScreenInfo(
        String lastMap,
        String lastMapset
    ) {}
}
