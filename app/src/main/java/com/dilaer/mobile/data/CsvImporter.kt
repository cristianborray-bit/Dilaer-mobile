package com.dilaer.mobile.data

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader

object CsvImporter {

    private val phoneCleanupRegex = Regex("[^+0-9]")

    fun importFromUri(context: Context, uri: Uri): Result<List<Contact>> = runCatching {
        context.contentResolver.openInputStream(uri).use { stream ->
            requireNotNull(stream) { "No se pudo abrir el archivo" }
            BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).useLines { lines ->
                parseLines(lines)
            }
        }
    }

    private fun parseLines(lines: Sequence<String>): List<Contact> {
        val contacts = mutableListOf<Contact>()
        var headerSkipped = false
        var nextId = 1

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            val tokens = splitCsv(line)
            if (tokens.isEmpty()) continue

            if (!headerSkipped) {
                headerSkipped = true
                if (looksLikeHeader(tokens)) continue
            }

            val (name, phone) = extractNameAndPhone(tokens) ?: continue
            if (phone.isEmpty()) continue

            contacts += Contact(id = nextId++, name = name, phone = phone)
        }
        return contacts
    }

    private fun splitCsv(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        for (c in line) {
            when {
                c == '"' -> inQuotes = !inQuotes
                (c == ',' || c == ';' || c == '\t') && !inQuotes -> {
                    result += current.toString().trim()
                    current.clear()
                }
                else -> current.append(c)
            }
        }
        result += current.toString().trim()
        return result
    }

    private fun looksLikeHeader(tokens: List<String>): Boolean {
        val joined = tokens.joinToString(" ").lowercase()
        if (joined.any { it.isDigit() }) return false
        return tokens.any {
            val low = it.lowercase()
            low.contains("nombre") || low.contains("name") ||
                low.contains("telefono") || low.contains("teléfono") ||
                low.contains("phone") || low.contains("numero") || low.contains("número")
        }
    }

    private fun extractNameAndPhone(tokens: List<String>): Pair<String, String>? {
        if (tokens.size == 1) {
            val phone = sanitizePhone(tokens[0])
            return if (phone.isNotEmpty()) "" to phone else null
        }
        // Heuristic: pick the token that looks most like a phone number; the rest is the name.
        val phoneIndex = tokens.indexOfFirst { looksLikePhone(it) }
            .takeIf { it >= 0 }
            ?: return null
        val phone = sanitizePhone(tokens[phoneIndex])
        val name = tokens.filterIndexed { idx, _ -> idx != phoneIndex }
            .joinToString(" ")
            .trim()
        return name to phone
    }

    private fun looksLikePhone(token: String): Boolean {
        val digits = token.count { it.isDigit() }
        return digits >= 6
    }

    private fun sanitizePhone(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        val hasPlus = trimmed.startsWith("+")
        val digits = trimmed.replace(phoneCleanupRegex, "")
        return if (hasPlus && !digits.startsWith("+")) "+$digits" else digits
    }
}
