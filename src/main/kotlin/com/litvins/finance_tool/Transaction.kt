package com.litvins.finance_tool

import java.math.BigDecimal
import java.time.LocalDate

sealed class Transaction(
    val type: String,
    val date: LocalDate,
    val amount: BigDecimal,
    val currency: Currency,
    val category: Category,
) {

    class IncomingTransferTransaction(
        date: LocalDate,
        amount: BigDecimal,
        currency: Currency,
    ) : Transaction("Incoming transaction", date, amount, currency, Category.UNKNOWN)

    class OutgoingTransferTransaction(
        date: LocalDate,
        amount: BigDecimal,
        currency: Currency,
    ) : Transaction("Outgoing transaction", date, amount, currency, Category.UNKNOWN)

    class ShoppingTransaction(
        date: LocalDate,
        amount: BigDecimal,
        currency: Currency,
        category: Category,
        val merchant: String,
    ) : Transaction("Payment", date, amount, currency, category){
        override fun toString(): String {
            return "$type: (date=$date, amount=$amount, currency=$currency, category=$category, merchant=$merchant)"
        }
    }

    class CashWithdrawalTransaction(
        date: LocalDate,
        amount: BigDecimal,
        currency: Currency,
    ) : Transaction(
        "Cash withdrawal",
        date,
        amount,
        currency,
        Category.CASH,
    )

    enum class Currency {
        GEL,
        USD,
        EUR;

        companion object {
            fun parse(value: String): Currency {
                return Currency.entries.firstOrNull { value == it.name }
                    ?: throw IllegalArgumentException("ATATAT! $value")
            }

            fun String.toCurrency(): Currency {
                return Currency.parse(this)
            }
        }
    }

    enum class Category {
        CASH,
        UNKNOWN,
        OTHER,
        GROCERY,
        EAT_OUT,
        SUBSCRIPTIONS
    }

    override fun toString(): String {
        return "$type: (date=$date, amount=$amount, currency=$currency, category=$category)"
    }
}