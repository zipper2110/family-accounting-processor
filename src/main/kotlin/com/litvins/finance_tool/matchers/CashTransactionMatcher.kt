package com.litvins.finance_tool.matchers

import com.litvins.finance_tool.SmsRawData
import com.litvins.finance_tool.Transaction
import com.litvins.finance_tool.Transaction.Currency.Companion.toCurrency
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class CashTransactionMatcher : TransactionMatcher {
    override fun matches(input: String): Boolean {
        return input.startsWith("Cash out")
    }

    override fun parseTransaction(message: SmsRawData): Transaction {
        val regex = Regex("Cash out: (\\d+.\\d{2}) (\\w+)")
        val result = regex.find(message.text)?.groupValues ?: throw IllegalArgumentException("Message could not be parsed: $message")
        val amount = result[1]
        val currency = result[2]

        return Transaction.CashWithdrawalTransaction(
            date = parseDate(message.dateTime),
            amount = BigDecimal(amount),
            currency = currency.toCurrency()
        )
    }
}