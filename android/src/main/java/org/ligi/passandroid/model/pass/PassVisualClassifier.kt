package org.ligi.passandroid.model.pass

object PassVisualClassifier {

    fun classify(pass: Pass): PassVisualType {
        return if (looksLikeCredential(pass)) PassVisualType.CREDENTIAL else PassVisualType.DEFAULT
    }

    private fun looksLikeCredential(pass: Pass): Boolean {
        if (pass.type == PassType.BOARDING || pass.type == PassType.PKBOARDING) return false

        val metadataText = listOfNotNull(pass.passIdent, pass.description, pass.creator)
            .joinToString(" ")
            .normalizedForClassification()

        val fieldText = pass.fields
            .joinToString(" ") { listOfNotNull(it.key, it.label, it.value).joinToString(" ") }
            .normalizedForClassification()

        val explicitCredentialMetadata = listOf(
            "carnet",
            "credential",
            "identity",
            "identificacion",
            "identification",
            "colegial",
            "membership",
            "member card"
        ).any { metadataText.contains(it) }

        val hasCredentialIdentifier = listOf(
            "colegiado",
            "colegiada",
            "nif",
            "dni",
            "d.n.i",
            "member number",
            "membership number",
            "license number",
            "licence number"
        ).any { fieldText.contains(it) }

        val hasHolderName = pass.fields.any { field ->
            listOfNotNull(field.key, field.label)
                .joinToString(" ")
                .normalizedForClassification()
                .let { it == "nombre" || it == "name" || it.contains("holder") || it.contains("titular") }
        }

        return explicitCredentialMetadata && (hasCredentialIdentifier || hasHolderName)
    }

    private fun String.normalizedForClassification(): String {
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
}
