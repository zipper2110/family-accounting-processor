package com.litvins.finance_tool.matchers

import com.litvins.finance_tool.SmsRawData
import com.litvins.finance_tool.Transaction
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

interface TransactionMatcher {
    fun matches(input: String): Boolean

    fun parseTransaction(message: SmsRawData): Transaction

    fun parseDate(dateString: String): LocalDate {
        return LocalDate.parse(
            dateString,
            DateTimeFormatter.ofPattern(
                "MMMM d, yyyy 'at' hh:mma",
                Locale.ENGLISH,
            )
        )
    }
}