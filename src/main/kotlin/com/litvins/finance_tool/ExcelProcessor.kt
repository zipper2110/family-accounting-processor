package com.litvins.finance_tool

import com.litvins.finance_tool.matchers.TransactionMatcher
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Component
import java.io.File
import java.io.FileInputStream
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

data class SmsRawData(
    val dateTime: String,
    val text: String,
    val sender: Double?,
)