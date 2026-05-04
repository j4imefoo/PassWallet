package org.ligi.passandroid.ui.rendering

import org.ligi.passandroid.model.pass.Pass
import org.ligi.passandroid.model.pass.PassField
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.model.pass.PassVisualType

interface PassRenderer {
    fun listTitle(pass: Pass): String?
    fun listMeta(pass: Pass): String?
    fun detailLabel(field: PassField): String?
    fun detailValue(field: PassField): String?
    fun showOnDetailFront(field: PassField): Boolean
}

object PassRenderers {
    fun forPass(pass: Pass): PassRenderer {
        if (pass.visualType == PassVisualType.CREDENTIAL) return CredentialPassRenderer

        return when (pass.type) {
            PassType.EVENT -> EventTicketRenderer
            PassType.BOARDING, PassType.PKBOARDING -> BoardingPassRenderer
            else -> GenericPassRenderer
        }
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

object CredentialPassRenderer : PassRenderer {
    override fun listTitle(pass: Pass): String? {
        return pass.frontFieldMatching("nombre", "name", "titular", "holder")?.value.cleanPassText()
            ?: pass.frontField("secondaryFields")?.value.cleanPassText()
            ?: pass.description
    }

    override fun listMeta(pass: Pass): String? {
        val credentialId = pass.frontFieldMatching("colegiado", "colegiada", "member", "membership")
            ?: pass.frontFieldMatching("nif", "dni", "d.n.i", "id")
        return credentialId?.let { joinLabelValueWithKeyFallback(it) }
            ?: pass.creator
            ?: pass.description
    }

    override fun detailLabel(field: PassField): String? {
        return field.label.cleanPassText() ?: field.key.toDisplayKey()
    }

    override fun detailValue(field: PassField): String? = field.value.cleanPassText()

    override fun showOnDetailFront(field: PassField): Boolean {
        if (field.hide || field.hint == null || isGeneratedTypeField(field)) return false
        return field.hint in listOf("headerFields", "primaryFields", "secondaryFields", "auxiliaryFields")
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

object BoardingPassRenderer : PassRenderer {
    override fun listTitle(pass: Pass): String? {
        val route = pass.frontFields("primaryFields")
            .mapNotNull { it.value.cleanPassText() }
            .take(2)
            .joinToString(" → ")
        return route.ifBlank {
            pass.frontField("primaryFields")?.value.cleanPassText()
                ?: pass.description
        }
    }

    override fun listMeta(pass: Pass): String? {
        val travelMeta = pass.fields
            .asSequence()
            .filter { !it.hide && it.hint in listOf("headerFields", "secondaryFields", "auxiliaryFields") }
            .filter { it.looksLikeTravelTimeOrCode() }
            .mapNotNull { joinLabelValue(it) }
            .take(2)
            .joinToString(" · ")

        return travelMeta.ifBlank {
            pass.frontField("headerFields")?.let { joinLabelValue(it) }
                ?: pass.creator
                ?: pass.description
        }
    }

    override fun detailLabel(field: PassField): String? = field.label.cleanPassText()
    override fun detailValue(field: PassField): String? = field.value.cleanPassText()

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

private fun Pass.frontFields(hint: String): List<PassField> {
    return fields.filter { !it.hide && it.hint == hint && !it.value.isNullOrBlank() }
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

private fun joinLabelValueWithKeyFallback(field: PassField): String? {
    val label = field.label.cleanPassText() ?: field.key.toDisplayKey()
    val value = field.value.cleanPassText()
    return when {
        label.isNullOrBlank() -> value
        value.isNullOrBlank() -> label
        else -> "$label: $value"
    }
}

private fun Pass.frontFieldMatching(vararg needles: String): PassField? {
    val normalizedNeedles = needles.map { it.normalizedForMatching() }
    return fields.firstOrNull { field ->
        !field.hide && !field.value.isNullOrBlank() &&
            listOfNotNull(field.key, field.label)
                .joinToString(" ")
                .normalizedForMatching()
                .let { text -> normalizedNeedles.any { text.contains(it) } }
    }
}

private fun String?.toDisplayKey(): String? {
    val clean = cleanPassText() ?: return null
    return clean
        .replace('_', ' ')
        .replace('-', ' ')
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .joinToString(" ") { word -> word.replaceFirstChar { char -> char.uppercase() } }
}

private fun String.normalizedForMatching(): String {
    return lowercase()
        .replace('á', 'a')
        .replace('é', 'e')
        .replace('í', 'i')
        .replace('ó', 'o')
        .replace('ú', 'u')
        .replace('ü', 'u')
        .replace('ñ', 'n')
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun PassField.looksLikeTravelTimeOrCode(): Boolean {
    val text = listOfNotNull(key, label).joinToString(" ").lowercase()
    return listOf(
        "date", "time", "valid", "departure", "arrival", "boarding", "gate", "seat", "code",
        "fecha", "hora", "salida", "llegada", "embarque", "puerta", "asiento", "codigo", "código",
        "datum", "zeit", "gültig", "gueltig"
    ).any { text.contains(it) }
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
