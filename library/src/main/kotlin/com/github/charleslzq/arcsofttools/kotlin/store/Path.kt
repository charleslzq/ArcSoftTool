package com.github.charleslzq.dicom.store

import java.io.File

class Path(private val absolutePath: String) {
    fun toFile() = File(absolutePath)
}

object Paths {
    fun get(vararg path: String?) = Path(path.filterNotNull().joinToString(File.separator))
}