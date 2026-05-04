package org.ligi.passandroid.unittest

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.model.pass.PassField
import org.ligi.passandroid.model.pass.PassImpl
import org.ligi.passandroid.model.pass.PassType
import org.ligi.passandroid.model.pass.PassVisualClassifier
import org.ligi.passandroid.model.pass.PassVisualType
import org.ligi.passandroid.ui.rendering.PassRenderers

class ThePassVisualClassifier {

    @Test
    fun detectsColegioCredentialEvenWhenPassKitTypeIsEvent() {
        val pass = PassImpl("credential").apply {
            type = PassType.EVENT
            description = "Carnet colegial digital"
            passIdent = "pass.com.adasistemas.carnetcolegial"
            fields = mutableListOf(
                PassField("colegiado", null, "0004055", false, "headerFields"),
                PassField("nombre", "NOMBRE", "JAIME RUIZ FRONTERA", false, "secondaryFields"),
                PassField("nif", "D.N.I/N.I.F", "25191643L", false, "auxiliaryFields")
            )
        }

        assertThat(PassVisualClassifier.classify(pass)).isEqualTo(PassVisualType.CREDENTIAL)
    }

    @Test
    fun credentialRendererUsesHolderNameAndCredentialNumber() {
        val pass = PassImpl("credential").apply {
            visualType = PassVisualType.CREDENTIAL
            description = "Carnet colegial digital"
            fields = mutableListOf(
                PassField("colegiado", null, "0004055", false, "headerFields"),
                PassField("nombre", "NOMBRE", "JAIME RUIZ FRONTERA", false, "secondaryFields"),
                PassField("nif", "D.N.I/N.I.F", "25191643L", false, "auxiliaryFields")
            )
        }

        val renderer = PassRenderers.forPass(pass)

        assertThat(renderer.listTitle(pass)).isEqualTo("JAIME RUIZ FRONTERA")
        assertThat(renderer.listMeta(pass)).isEqualTo("Colegiado: 0004055")
    }

    @Test
    fun doesNotTreatBoardingPassAsCredential() {
        val pass = PassImpl("boarding").apply {
            type = PassType.PKBOARDING
            description = "Membership boarding pass"
            fields = mutableListOf(PassField("member", "Member", "42", false, "headerFields"))
        }

        assertThat(PassVisualClassifier.classify(pass)).isEqualTo(PassVisualType.DEFAULT)
    }
}
