package com.divudi.core.data;

/**
 * Currency enum for multi-currency support
 * @author HMIS Development Team
 */
public enum Currency {
    UGX("Ugandan Shilling", "UGX", "USh"),
    USD("US Dollar", "USD", "$"),
    EUR("Euro", "EUR", "€"),
    GBP("British Pound", "GBP", "£"),
    KES("Kenyan Shilling", "KES", "KSh"),
    TZS("Tanzanian Shilling", "TZS", "TSh"),
    RWF("Rwandan Franc", "RWF", "FRw"),
    SSP("South Sudanese Pound", "SSP", "SS£"),
    ZAR("South African Rand", "ZAR", "R");

    private final String displayName;
    private final String code;
    private final String symbol;

    Currency(String displayName, String code, String symbol) {
        this.displayName = displayName;
        this.code = code;
        this.symbol = symbol;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCode() {
        return code;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getLabel() {
        return displayName + " (" + symbol + ")";
    }

    @Override
    public String toString() {
        return displayName;
    }
}
