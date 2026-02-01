/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.economy;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Objects;

/**
 * Represents a currency within the economy system.
 *
 * <p>A currency defines how monetary values are displayed, formatted, and handled
 * within the economy. Each currency has a unique identifier, display properties,
 * and precision settings for decimal handling.
 *
 * <h2>Currency Properties</h2>
 * <ul>
 *   <li><b>Identifier:</b> Unique key for the currency (e.g., "coins", "dollars")</li>
 *   <li><b>Name:</b> Display names (singular and plural) for messages</li>
 *   <li><b>Symbol:</b> Currency symbol (e.g., "$", "C")</li>
 *   <li><b>Decimals:</b> Number of decimal places for precision</li>
 *   <li><b>Format:</b> How the currency is displayed in text</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Create a simple coin currency
 * Currency coins = Currency.builder("coins")
 *     .nameSingular("Coin")
 *     .namePlural("Coins")
 *     .symbol("C")
 *     .decimals(0)
 *     .formatPattern("#,##0")
 *     .symbolPosition(Currency.SymbolPosition.SUFFIX)
 *     .build();
 *
 * // Create a dollar currency with cents
 * Currency dollars = Currency.builder("dollars")
 *     .nameSingular("Dollar")
 *     .namePlural("Dollars")
 *     .symbol("$")
 *     .decimals(2)
 *     .formatPattern("#,##0.00")
 *     .symbolPosition(Currency.SymbolPosition.PREFIX)
 *     .build();
 *
 * // Format an amount
 * String formatted = dollars.format(new BigDecimal("1234.56"));
 * // Result: "$1,234.56"
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Currency instances are immutable and thread-safe.
 *
 * @since 1.0.0
 * @author Supatuck
 * @see EconomyService
 */
public final class Currency {

    private final String identifier;
    private final String nameSingular;
    private final String namePlural;
    private final String symbol;
    private final int decimals;
    private final String formatPattern;
    private final SymbolPosition symbolPosition;
    private final boolean defaultCurrency;
    private final BigDecimal startingBalance;
    private final BigDecimal minBalance;
    private final BigDecimal maxBalance;
    private final Locale locale;

    private final DecimalFormat decimalFormat;

    private Currency(@NotNull Builder builder) {
        this.identifier = builder.identifier;
        this.nameSingular = builder.nameSingular;
        this.namePlural = builder.namePlural;
        this.symbol = builder.symbol;
        this.decimals = builder.decimals;
        this.formatPattern = builder.formatPattern;
        this.symbolPosition = builder.symbolPosition;
        this.defaultCurrency = builder.defaultCurrency;
        this.startingBalance = builder.startingBalance;
        this.minBalance = builder.minBalance;
        this.maxBalance = builder.maxBalance;
        this.locale = builder.locale;

        // Initialize decimal format
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(locale);
        this.decimalFormat = new DecimalFormat(formatPattern, symbols);
        this.decimalFormat.setRoundingMode(RoundingMode.HALF_UP);
    }

    /**
     * Creates a new currency builder with the specified identifier.
     *
     * @param identifier the unique identifier for the currency
     * @return a new currency builder
     * @throws NullPointerException if identifier is null
     * @since 1.0.0
     */
    @NotNull
    public static Builder builder(@NotNull String identifier) {
        return new Builder(Objects.requireNonNull(identifier, "identifier cannot be null"));
    }

    /**
     * Creates a default currency with common settings.
     *
     * <p>Creates a currency with:
     * <ul>
     *   <li>Name: Dollar/Dollars</li>
     *   <li>Symbol: $</li>
     *   <li>Decimals: 2</li>
     *   <li>Format: #,##0.00</li>
     * </ul>
     *
     * @param identifier the currency identifier
     * @return a new default currency
     * @since 1.0.0
     */
    @NotNull
    public static Currency createDefault(@NotNull String identifier) {
        return builder(identifier)
                .names("Dollar", "Dollars")
                .symbol("$")
                .decimals(2)
                .formatPattern("#,##0.00")
                .symbolPosition(SymbolPosition.PREFIX)
                .defaultCurrency(true)
                .build();
    }

    /**
     * Returns the unique identifier for this currency.
     *
     * <p>The identifier is used to reference this currency in code and configuration.
     * It should be lowercase and contain no spaces (e.g., "coins", "gems", "dollars").
     *
     * @return the currency identifier
     * @since 1.0.0
     */
    @NotNull
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Returns the singular name of this currency.
     *
     * <p>Used when displaying amounts of exactly 1 (e.g., "1 Dollar").
     *
     * @return the singular currency name
     * @since 1.0.0
     */
    @NotNull
    public String getNameSingular() {
        return nameSingular;
    }

    /**
     * Returns the plural name of this currency.
     *
     * <p>Used when displaying amounts other than 1 (e.g., "5 Dollars", "0 Dollars").
     *
     * @return the plural currency name
     * @since 1.0.0
     */
    @NotNull
    public String getNamePlural() {
        return namePlural;
    }

    /**
     * Returns the appropriate name for the given amount.
     *
     * <p>Returns singular if the absolute value equals 1, otherwise returns plural.
     *
     * @param amount the amount to get the name for
     * @return the appropriate name (singular or plural)
     * @since 1.0.0
     */
    @NotNull
    public String getName(@NotNull BigDecimal amount) {
        Objects.requireNonNull(amount, "amount cannot be null");
        if (amount.abs().compareTo(BigDecimal.ONE) == 0) {
            return nameSingular;
        }
        return namePlural;
    }

    /**
     * Returns the currency symbol.
     *
     * <p>The symbol is used in formatted output (e.g., "$", "C", "G").
     *
     * @return the currency symbol
     * @since 1.0.0
     */
    @NotNull
    public String getSymbol() {
        return symbol;
    }

    /**
     * Returns the number of decimal places for this currency.
     *
     * <p>This determines the precision of monetary calculations:
     * <ul>
     *   <li>0 - Whole numbers only (e.g., coins, gems)</li>
     *   <li>2 - Standard currency with cents (e.g., dollars)</li>
     *   <li>4+ - High precision for crypto-style currencies</li>
     * </ul>
     *
     * @return the number of decimal places
     * @since 1.0.0
     */
    public int getDecimals() {
        return decimals;
    }

    /**
     * Returns the format pattern used for displaying amounts.
     *
     * <p>Uses {@link DecimalFormat} pattern syntax.
     *
     * @return the format pattern
     * @since 1.0.0
     */
    @NotNull
    public String getFormatPattern() {
        return formatPattern;
    }

    /**
     * Returns the position of the symbol relative to the amount.
     *
     * @return the symbol position
     * @since 1.0.0
     */
    @NotNull
    public SymbolPosition getSymbolPosition() {
        return symbolPosition;
    }

    /**
     * Returns whether this is the default currency.
     *
     * <p>The default currency is used when no currency is explicitly specified.
     *
     * @return true if this is the default currency
     * @since 1.0.0
     */
    public boolean isDefaultCurrency() {
        return defaultCurrency;
    }

    /**
     * Returns the starting balance for new accounts.
     *
     * <p>This is the initial balance given to players when their account is created.
     *
     * @return the starting balance
     * @since 1.0.0
     */
    @NotNull
    public BigDecimal getStartingBalance() {
        return startingBalance;
    }

    /**
     * Returns the minimum allowed balance.
     *
     * <p>Transactions that would result in a balance below this are rejected.
     * Set to a negative value to allow debt.
     *
     * @return the minimum balance
     * @since 1.0.0
     */
    @NotNull
    public BigDecimal getMinBalance() {
        return minBalance;
    }

    /**
     * Returns the maximum allowed balance.
     *
     * <p>Transactions that would result in a balance above this are rejected.
     *
     * @return the maximum balance
     * @since 1.0.0
     */
    @NotNull
    public BigDecimal getMaxBalance() {
        return maxBalance;
    }

    /**
     * Returns the locale used for formatting.
     *
     * @return the formatting locale
     * @since 1.0.0
     */
    @NotNull
    public Locale getLocale() {
        return locale;
    }

    /**
     * Formats an amount using this currency's format settings.
     *
     * <p>Applies the format pattern, symbol, and symbol position.
     *
     * @param amount the amount to format
     * @return the formatted amount string
     * @since 1.0.0
     */
    @NotNull
    public String format(@NotNull BigDecimal amount) {
        Objects.requireNonNull(amount, "amount cannot be null");
        String formatted;
        synchronized (decimalFormat) {
            formatted = decimalFormat.format(amount.setScale(decimals, RoundingMode.HALF_UP));
        }

        return switch (symbolPosition) {
            case PREFIX -> symbol + formatted;
            case SUFFIX -> formatted + symbol;
            case PREFIX_SPACE -> symbol + " " + formatted;
            case SUFFIX_SPACE -> formatted + " " + symbol;
            case NONE -> formatted;
        };
    }

    /**
     * Formats an amount with the currency name.
     *
     * <p>Uses singular or plural name based on the amount.
     *
     * @param amount the amount to format
     * @return the formatted amount with currency name
     * @since 1.0.0
     */
    @NotNull
    public String formatWithName(@NotNull BigDecimal amount) {
        Objects.requireNonNull(amount, "amount cannot be null");
        String formatted;
        synchronized (decimalFormat) {
            formatted = decimalFormat.format(amount.setScale(decimals, RoundingMode.HALF_UP));
        }
        return formatted + " " + getName(amount);
    }

    /**
     * Rounds an amount to this currency's precision.
     *
     * @param amount the amount to round
     * @return the rounded amount
     * @since 1.0.0
     */
    @NotNull
    public BigDecimal round(@NotNull BigDecimal amount) {
        Objects.requireNonNull(amount, "amount cannot be null");
        return amount.setScale(decimals, RoundingMode.HALF_UP);
    }

    /**
     * Validates that an amount is within the allowed range.
     *
     * @param amount the amount to validate
     * @return true if the amount is valid
     * @since 1.0.0
     */
    public boolean isValidBalance(@NotNull BigDecimal amount) {
        Objects.requireNonNull(amount, "amount cannot be null");
        return amount.compareTo(minBalance) >= 0 && amount.compareTo(maxBalance) <= 0;
    }

    /**
     * Clamps an amount to the allowed range.
     *
     * @param amount the amount to clamp
     * @return the clamped amount
     * @since 1.0.0
     */
    @NotNull
    public BigDecimal clamp(@NotNull BigDecimal amount) {
        Objects.requireNonNull(amount, "amount cannot be null");
        if (amount.compareTo(minBalance) < 0) {
            return minBalance;
        }
        if (amount.compareTo(maxBalance) > 0) {
            return maxBalance;
        }
        return amount;
    }

    /**
     * Creates a new builder based on this currency for modification.
     *
     * @return a new builder with this currency's values
     * @since 1.0.0
     */
    @NotNull
    public Builder toBuilder() {
        return new Builder(identifier)
                .nameSingular(nameSingular)
                .namePlural(namePlural)
                .symbol(symbol)
                .decimals(decimals)
                .formatPattern(formatPattern)
                .symbolPosition(symbolPosition)
                .defaultCurrency(defaultCurrency)
                .startingBalance(startingBalance)
                .minBalance(minBalance)
                .maxBalance(maxBalance)
                .locale(locale);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Currency currency = (Currency) o;
        return identifier.equals(currency.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier);
    }

    @Override
    public String toString() {
        return "Currency{" +
                "identifier='" + identifier + '\'' +
                ", name='" + nameSingular + "/" + namePlural + '\'' +
                ", symbol='" + symbol + '\'' +
                ", decimals=" + decimals +
                ", default=" + defaultCurrency +
                '}';
    }

    /**
     * Position of the currency symbol in formatted output.
     *
     * @since 1.0.0
     */
    public enum SymbolPosition {
        /**
         * Symbol before the amount with no space (e.g., "$100").
         */
        PREFIX,

        /**
         * Symbol after the amount with no space (e.g., "100C").
         */
        SUFFIX,

        /**
         * Symbol before the amount with a space (e.g., "$ 100").
         */
        PREFIX_SPACE,

        /**
         * Symbol after the amount with a space (e.g., "100 C").
         */
        SUFFIX_SPACE,

        /**
         * No symbol displayed.
         */
        NONE
    }

    /**
     * Builder for creating {@link Currency} instances.
     *
     * @since 1.0.0
     */
    public static final class Builder {

        private final String identifier;
        private String nameSingular;
        private String namePlural;
        private String symbol = "";
        private int decimals = 2;
        private String formatPattern = "#,##0.00";
        private SymbolPosition symbolPosition = SymbolPosition.PREFIX;
        private boolean defaultCurrency = false;
        private BigDecimal startingBalance = BigDecimal.ZERO;
        private BigDecimal minBalance = BigDecimal.ZERO;
        private BigDecimal maxBalance = new BigDecimal("999999999999999");
        private Locale locale = Locale.US;

        private Builder(@NotNull String identifier) {
            this.identifier = identifier;
            this.nameSingular = identifier;
            this.namePlural = identifier;
        }

        /**
         * Sets the singular name of the currency.
         *
         * @param nameSingular the singular name
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder nameSingular(@NotNull String nameSingular) {
            this.nameSingular = Objects.requireNonNull(nameSingular, "nameSingular cannot be null");
            return this;
        }

        /**
         * Sets the plural name of the currency.
         *
         * @param namePlural the plural name
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder namePlural(@NotNull String namePlural) {
            this.namePlural = Objects.requireNonNull(namePlural, "namePlural cannot be null");
            return this;
        }

        /**
         * Sets both singular and plural names.
         *
         * @param singular the singular name
         * @param plural   the plural name
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder names(@NotNull String singular, @NotNull String plural) {
            this.nameSingular = Objects.requireNonNull(singular, "singular cannot be null");
            this.namePlural = Objects.requireNonNull(plural, "plural cannot be null");
            return this;
        }

        /**
         * Sets the currency symbol.
         *
         * @param symbol the symbol
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder symbol(@NotNull String symbol) {
            this.symbol = Objects.requireNonNull(symbol, "symbol cannot be null");
            return this;
        }

        /**
         * Sets the number of decimal places.
         *
         * @param decimals the number of decimals (0-8)
         * @return this builder
         * @throws IllegalArgumentException if decimals is negative or greater than 8
         * @since 1.0.0
         */
        @NotNull
        public Builder decimals(int decimals) {
            if (decimals < 0 || decimals > 8) {
                throw new IllegalArgumentException("decimals must be between 0 and 8");
            }
            this.decimals = decimals;
            return this;
        }

        /**
         * Sets the format pattern for displaying amounts.
         *
         * @param formatPattern the format pattern
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder formatPattern(@NotNull String formatPattern) {
            this.formatPattern = Objects.requireNonNull(formatPattern, "formatPattern cannot be null");
            return this;
        }

        /**
         * Sets the position of the symbol.
         *
         * @param symbolPosition the symbol position
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder symbolPosition(@NotNull SymbolPosition symbolPosition) {
            this.symbolPosition = Objects.requireNonNull(symbolPosition, "symbolPosition cannot be null");
            return this;
        }

        /**
         * Sets whether this is the default currency.
         *
         * @param defaultCurrency true if this is the default currency
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder defaultCurrency(boolean defaultCurrency) {
            this.defaultCurrency = defaultCurrency;
            return this;
        }

        /**
         * Sets the starting balance for new accounts.
         *
         * @param startingBalance the starting balance
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder startingBalance(@NotNull BigDecimal startingBalance) {
            this.startingBalance = Objects.requireNonNull(startingBalance, "startingBalance cannot be null");
            return this;
        }

        /**
         * Sets the starting balance for new accounts.
         *
         * @param startingBalance the starting balance
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder startingBalance(double startingBalance) {
            this.startingBalance = BigDecimal.valueOf(startingBalance);
            return this;
        }

        /**
         * Sets the minimum allowed balance.
         *
         * @param minBalance the minimum balance
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder minBalance(@NotNull BigDecimal minBalance) {
            this.minBalance = Objects.requireNonNull(minBalance, "minBalance cannot be null");
            return this;
        }

        /**
         * Sets the minimum allowed balance.
         *
         * @param minBalance the minimum balance
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder minBalance(double minBalance) {
            this.minBalance = BigDecimal.valueOf(minBalance);
            return this;
        }

        /**
         * Sets the maximum allowed balance.
         *
         * @param maxBalance the maximum balance
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder maxBalance(@NotNull BigDecimal maxBalance) {
            this.maxBalance = Objects.requireNonNull(maxBalance, "maxBalance cannot be null");
            return this;
        }

        /**
         * Sets the maximum allowed balance.
         *
         * @param maxBalance the maximum balance
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder maxBalance(double maxBalance) {
            this.maxBalance = BigDecimal.valueOf(maxBalance);
            return this;
        }

        /**
         * Sets the locale for formatting.
         *
         * @param locale the locale
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder locale(@NotNull Locale locale) {
            this.locale = Objects.requireNonNull(locale, "locale cannot be null");
            return this;
        }

        /**
         * Configures this currency to allow negative balances (debt).
         *
         * @param allowDebt true to allow negative balances
         * @return this builder
         * @since 1.0.0
         */
        @NotNull
        public Builder allowDebt(boolean allowDebt) {
            if (allowDebt) {
                this.minBalance = new BigDecimal("-999999999999999");
            } else {
                this.minBalance = BigDecimal.ZERO;
            }
            return this;
        }

        /**
         * Builds the currency.
         *
         * @return the new currency instance
         * @since 1.0.0
         */
        @NotNull
        public Currency build() {
            return new Currency(this);
        }
    }
}
