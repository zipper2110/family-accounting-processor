package com.litvins.finance_tool

import com.litvins.finance_tool.Transaction.Currency.Companion.toCurrency
import org.apache.poi.hpsf.Decimal
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.xmlbeans.impl.store.Cur
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Component
import java.io.File
import java.io.FileInputStream
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.system.exitProcess


@Component
class ExcelProcessor(
    @Value("\${filename}")
    private val filename: String,
    private val matchers: List<TransactionMatcher>,
) : ApplicationListener<ContextRefreshedEvent> {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun onApplicationEvent(event: ContextRefreshedEvent) {
        logger.info("Filename=$filename")

        val file = FileInputStream(File(filename))
        val workbook = XSSFWorkbook(file)

        val sheet = workbook.getSheet("Sheet1")

        val messages = readExcelMessages(sheet)

        val transactions = parseTransactions(messages)

        transactions.forEach { println(it) }

        exitProcess(0)
    }

    fun readExcelMessages(sheet: XSSFSheet): List<SmsRawData> {
        val allSms = mutableListOf<SmsRawData>()

        val rowIterator = sheet.rowIterator()
        while (rowIterator.hasNext()) {
            val row = rowIterator.next()

            row.getCell(row.firstCellNum.toInt()) ?: break
            row.getCell(row.firstCellNum.toInt() + 1) ?: break

            val smsData = SmsRawData(
                dateTime = row.getCell(row.firstCellNum.toInt()).stringCellValue,
                text = row.getCell(row.firstCellNum.toInt() + 1).stringCellValue,
                sender = row.getCell(row.firstCellNum.toInt() + 2)?.numericCellValue,
            )
            allSms += smsData
        }

        return allSms
    }

    fun parseTransactions(messages: List<SmsRawData>): List<Transaction> {
        return messages.mapNotNull { parseTransaction(it) }
    }

    fun parseTransaction(message: SmsRawData): Transaction? {
        val matcher = matchers.firstOrNull { it.matches(message.text) }

        if (matcher == null) {
//            logger.warn("Unknown transaction type: $message")
        }

        try {
            val transaction = matcher?.parseTransaction(message)
            return transaction
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        return null
    }
}

interface TransactionMatcher {
    fun matches(input: String): Boolean

    fun parseTransaction(message: SmsRawData): Transaction

    fun parseDate(dateString: String): LocalDate {
        return LocalDate.parse(
            dateString,
            DateTimeFormatter.ofPattern(
                "MMMM d, yyyy 'at' hh:mma",
                Locale.ENGLISH,
            ))
    }
}

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

@Component
class OutgoingTransactionMatcher : TransactionMatcher {
    override fun matches(input: String): Boolean {
        return input.startsWith("Amount transfer")
    }

    override fun parseTransaction(message: SmsRawData): Transaction {
        TODO("Not yet implemented")
    }
}

@Component
class ShoppingTransactionMatcher : TransactionMatcher {
    override fun matches(input: String): Boolean {
        return input.startsWith("Payment:")
    }

    override fun parseTransaction(message: SmsRawData): Transaction {
        val regex = Regex("Payment: ([\\d\\.]+) (\\w+).*\\n.*\\n(.*).*")

        val result = regex.find(message.text)?.groupValues

        val amount = result?.get(1) ?: throw IllegalArgumentException("EFKJSGFLKJ $message")

        val currency = result[2]


        return Transaction.ShoppingTransaction(
            date = parseDate(message.dateTime),
            amount = BigDecimal(amount),
            currency = currency.toCurrency(),
            category = Transaction.Category.OTHER,
            merchant = result[3],
        )
    }
}

@Component
class CashTransactionMatcher : TransactionMatcher {
    override fun matches(input: String): Boolean {
        return input.startsWith("Amount transfer")
    }

    override fun parseTransaction(message: SmsRawData): Transaction {
        TODO("Not yet implemented")
    }
}

data class SmsRawData(
    val dateTime: String,
    val text: String,
    val sender: Double?,
)

sealed class Transaction(
    val date: LocalDate,
    val amount: BigDecimal,
    val currency: Currency,
    val category: Category,
) {



    class IncomingTransferTransaction(
        date: LocalDate,
        amount: BigDecimal,
        currency: Currency,
    ) : Transaction(date, amount, currency, Category.UNKNOWN)

    class OutgoingTransferTransaction(
        date: LocalDate,
        amount: BigDecimal,
        currency: Currency,
    ) : Transaction(date, amount, currency, Category.UNKNOWN)

    class ShoppingTransaction(
        date: LocalDate,
        amount: BigDecimal,
        currency: Currency,
        category: Category,
        val merchant: String,
    ) : Transaction(date, amount, currency, category){
        override fun toString(): String {
            return "Transaction(date=$date, amount=$amount, currency=$currency, category=$category, merchant=$merchant)"

        }
    }

    class CashWithdrawalTransaction(
        date: LocalDate,
        amount: BigDecimal,
        currency: Currency,
    ) : Transaction(
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
    }

    override fun toString(): String {
        return "Transaction(date=$date, amount=$amount, currency=$currency, category=$category)"
    }
}

