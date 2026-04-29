package org.ligi.passandroid.ui

class FileUnzipControllerSpec(val zipFileString: String, spec: UnzipControllerSpec, val source: String = "") : UnzipControllerSpec(spec.targetPath, spec.context, spec.passStore, spec.onSuccessCallback, spec.failCallback) {

    constructor(zipFileString: String, spec: UnzipPassController.InputStreamUnzipControllerSpec) : this(zipFileString, spec, spec.inputStreamWithSource.source)

    init {
        overwrite = spec.overwrite
    }
}
