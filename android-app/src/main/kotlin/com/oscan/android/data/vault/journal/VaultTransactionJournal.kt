package com.oscan.android.data.vault.journal

import java.io.File

enum class TransactionType {
    MOVE_IN,
    MOVE_OUT,
    CHANGE_PASSCODE,
    DISABLE_MIGRATE
}

enum class TransactionStatus {
    STAGED_WRITE,
    VERIFYING,
    COMMITTING,
    COMPLETED
}

data class TransactionRecord(
    val id: String,
    val type: TransactionType,
    val documentId: String?,
    val status: TransactionStatus,
    val createdAtEpochMillis: Long
)

class VaultTransactionJournal(private val journalDir: File) {
    private val journalFile = File(journalDir, "vault_journal.json")

    init {
        journalDir.mkdirs()
    }

    @Synchronized
    fun recordTransaction(record: TransactionRecord) {
        val records = loadRecords().toMutableList()
        records.removeAll { it.id == record.id }
        if (record.status != TransactionStatus.COMPLETED) {
            records.add(record)
        }
        saveRecords(records)
    }

    @Synchronized
    fun clearTransaction(id: String) {
        val records = loadRecords().filterNot { it.id == id }
        saveRecords(records)
    }

    @Synchronized
    fun getPendingTransactions(): List<TransactionRecord> {
        return loadRecords()
    }

    @Synchronized
    fun performStartupRecovery(
        cleanStagedTempFiles: () -> Unit
    ) {
        val pending = loadRecords()
        if (pending.isNotEmpty()) {
            cleanStagedTempFiles()
            saveRecords(emptyList())
        }
    }

    private fun loadRecords(): List<TransactionRecord> {
        if (!journalFile.exists()) return emptyList()
        return runCatching {
            val content = journalFile.readText(Charsets.UTF_8)
            parseRecords(content)
        }.getOrDefault(emptyList())
    }

    private fun saveRecords(records: List<TransactionRecord>) {
        if (records.isEmpty()) {
            if (journalFile.exists()) journalFile.delete()
            return
        }
        val serialized = serializeRecords(records)
        val tempFile = File(journalDir, "vault_journal.json.tmp")
        tempFile.writeText(serialized, Charsets.UTF_8)
        if (tempFile.exists()) {
            tempFile.renameTo(journalFile)
        }
    }

    private fun parseRecords(json: String): List<TransactionRecord> {
        // Minimal json parser to avoid heavyweight dependencies
        if (json.isBlank()) return emptyList()
        val records = mutableListOf<TransactionRecord>()
        val blocks = json.split("---RECORD---").filter { it.isNotBlank() }
        for (block in blocks) {
            val lines = block.lines().associate { line ->
                val parts = line.split("=", limit = 2)
                if (parts.size == 2) parts[0].trim() to parts[1].trim() else "" to ""
            }
            val id = lines["id"] ?: continue
            val type = runCatching { TransactionType.valueOf(lines["type"] ?: "") }.getOrNull() ?: continue
            val docId = lines["documentId"]?.takeIf { it.isNotBlank() }
            val status = runCatching { TransactionStatus.valueOf(lines["status"] ?: "") }.getOrNull() ?: TransactionStatus.STAGED_WRITE
            val time = lines["createdAtEpochMillis"]?.toLongOrNull() ?: 0L
            records.add(TransactionRecord(id, type, docId, status, time))
        }
        return records
    }

    private fun serializeRecords(records: List<TransactionRecord>): String {
        return records.joinToString("\n---RECORD---\n") { r ->
            "id=${r.id}\ntype=${r.type}\ndocumentId=${r.documentId ?: ""}\nstatus=${r.status}\ncreatedAtEpochMillis=${r.createdAtEpochMillis}"
        }
    }
}
