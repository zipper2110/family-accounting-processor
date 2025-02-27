package com.litvins.finance_tool.matchers

import com.litvins.finance_tool.SmsRawData
import com.litvins.finance_tool.Transaction
import com.litvins.finance_tool.Transaction.Currency.Companion.toCurrency
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class ShoppingTransactionMatcher : TransactionMatcher {
    override fun matches(input: String): Boolean {
        return input.startsWith("Payment:")
    }

    override fun parseTransaction(message: SmsRawData): Transaction {
        val regex = Regex("Payment: ([\\d\\.]+) (\\w+).*\\n.*\\n(.*) \\w+")

        val result = regex.find(message.text)?.groupValues ?: throw IllegalArgumentException("Message could not be parsed: $message")

        val amount = result[1]

        val currency = result[2]


        return Transaction.ShoppingTransaction(
            date = parseDate(message.dateTime),
            amount = BigDecimal(amount),
            currency = currency.toCurrency(),
            category = Transaction.Category.OTHER,
            merchant = result[3].trim(),
        )
    }
}