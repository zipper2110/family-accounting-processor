package com.litvins.finance_tool.matchers

import com.litvins.finance_tool.SmsRawData
import com.litvins.finance_tool.Transaction
import com.litvins.finance_tool.Transaction.Currency.Companion.toCurrency
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class IncomingTransactionMatcher : TransactionMatcher {

    override fun matches(input: String): Boolean {
        return input.startsWith("Incoming transfer")
    }

    override fun parseTransaction(message: SmsRawData): Transaction {
        val regex = Regex(".*Amount ([\\d\\.]+) (\\w+);.*")

        val result = regex.find(message.text)?.groupValues

        val amount = result?.get(1) ?: throw IllegalArgumentException("EFKJSGFLKJ $message")

        val currency = result[2]

        return Transaction.IncomingTransferTransaction(
            date = parseDate(message.dateTime),
            amount = BigDecimal(amount),
            currency = currency.toCurrency(),
        )
    }
}