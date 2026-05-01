package org.ligi.passandroid.ui.rendering

import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.model.pass.PassField
import org.ligi.passandroid.model.pass.PassType

interface PassRenderer {
    fun listTitle(pass: Pass): String?
    fun listMeta(pass: Pass): String?
    fun detailLabel(field: PassField): String?
    fun detailValue(field: PassField): String?
    fun showOnDetailFront(field: PassField): Boolean
}

object PassRenderers {
    fun forPass(pass: Pass): PassRenderer = when (pass.type) {
        PassType.EVENT -> EventTicketRenderer
        else -> GenericPassRenderer
    }
}

object GenericPassRenderer : PassRenderer {
    override fun listTitle(pass: Pass): String? {
        return pass.frontField("primaryFields")?.value.cleanPassText()
            ?: pass.description
    }

    override fun listMeta(pass: Pass): String? {
        return pass.frontField("headerFields")?.value.cleanPassText()
            ?: pass.frontField("secondaryFields")?.let { joinLabelValue(it) }
    }

    override fun detailLabel(field: PassField): String? = field.label.cleanPassText()
    override fun detailValue(field: PassField): String? = field.value.cleanPassText()

    override fun showOnDetailFront(field: PassField): Boolean {
        return !field.hide && field.hint != null && !isGeneratedTypeField(field)
    }
}

object EventTicketRenderer : PassRenderer {
    override fun listTitle(pass: Pass): String? {
        return pass.frontField("primaryFields")?.value.cleanPassText()
            ?: pass.description
    }

    override fun listMeta(pass: Pass): String? {
        val venue = pass.frontField("primaryFields")?.label.cleanPassText()
        val date = pass.frontField("headerFields")?.value.cleanPassText()
        val combined = listOfNotNull(venue, date).joinToString(" · ")
        return combined.ifBlank { pass.description.orEmpty() }.ifBlank { null }
    }

    override fun detailLabel(field: PassField): String? = field.label.cleanPassText()

    override fun detailValue(field: PassField): String? {
        val label = field.label.cleanPassText().orEmpty().lowercase()
        val value = field.value.cleanPassText()
        return when {
            value == null -> null
            label.contains("sala") && value.endsWith("-") -> value.dropLast(1).trim().ifBlank { value }
            label.contains("fila") && label.contains("asiento") -> compactSeatList(value)
            else -> value
        }
    }

    override fun showOnDetailFront(field: PassField): Boolean {
        if (field.hide || field.hint == null || isGeneratedTypeField(field)) return false
        return when (field.hint) {
            "headerFields", "primaryFields", "secondaryFields", "auxiliaryFields" -> true
            else -> false
        }
    }
}

private fun Pass.frontField(hint: String): PassField? {
    return fields.firstOrNull { !it.hide && it.hint == hint && !it.value.isNullOrBlank() }
}

private fun joinLabelValue(field: PassField): String? {
    val label = field.label.cleanPassText()
    val value = field.value.cleanPassText()
    return when {
        label.isNullOrBlank() -> value
        value.isNullOrBlank() -> label
        else -> "$label: $value"
    }
}

private fun isGeneratedTypeField(field: PassField): Boolean {
    return field.key.isNullOrBlank() && field.hint == null && field.label.equals("Tipo", ignoreCase = true)
}

private fun String?.cleanPassText(): String? {
    if (this == null) return null
    return trim()
        .replace(Regex("\\s+"), " ")
        .replace(Regex("\\)(?=\\d{1,2}:\\d{2})"), ") ")
        .replace(Regex("\\s+-\\s*$"), "")
        .ifBlank { null }
}

private fun compactSeatList(value: String): String {
    val matches = Regex("(\\d+)\\s*-\\s*(\\d+)").findAll(value).map { match ->
        match.groupValues[1].toInt() to match.groupValues[2].toInt()
    }.toList()

    if (matches.size < 2 || matches.size != value.split(',').size) return value

    val rows = matches.map { it.first }.distinct()
    if (rows.size != 1) return value

    val seats = matches.map { it.second }.sorted()
    val compactSeats = if (seats.zipWithNext().all { it.second == it.first + 1 }) {
        "${seats.first()}-${seats.last()}"
    } else {
        seats.joinToString(", ")
    }

    return "Fila ${rows.first()} · Asientos $compactSeats"
}
