package com.github.charleslzq.faceengine.core.kotlin.store

import com.fatboyindustrial.gsonjodatime.Converters
import com.google.gson.GsonBuilder
import java.io.File
import java.util.*

/**
 * Created by charleslzq on 18-3-1.
 */
open class FaceFileStore<P : Meta, F : Meta>(
    private val directory: String,
    private val faceDataType: FaceDataType<P, F>,
    private val listeners: MutableList<FaceStoreChangeListener<P, F>> = mutableListOf()
) : ReadWriteFaceStore<P, F> {
    private val gson = Converters.registerLocalDateTime(GsonBuilder()).create()

    init {
        File(directory).mkdirs()
    }

    override fun getPersonIds() = listValidSubDirs(directory)

    override fun getFaceData(personId: String) = getPerson(personId)?.run {
        FaceData(this, getFaceIdList(personId).mapNotNull { getFace(personId, it) })
    }

    override fun getPerson(personId: String) =
        loadDataFile(faceDataType.personClass, directory, personId)

    override fun getFaceIdList(personId: String) = listValidSubDirs(directory, personId)

    override fun getFace(personId: String, faceId: String) =
        loadDataFile(faceDataType.faceClass, directory, personId, faceId)

    override fun savePerson(person: P) {
        val oldData = getPerson(person.id)
        if (oldData == null || oldData.updateTime.isBefore(person.updateTime)) {
            saveDataFile(person, directory, person.id)
            listeners.forEach { it.onPersonUpdate(person) }
        }
    }

    override fun saveFace(personId: String, face: F) {
        val oldData = getFace(personId, face.id)
        if (oldData == null || oldData.updateTime.isBefore(face.updateTime)) {
            saveDataFile(face, directory, personId, face.id)
            listeners.forEach { it.onFaceUpdate(personId, face) }
        }
    }


    override fun saveFaceData(faceData: FaceData<P, F>) {
        savePerson(faceData.person)
        faceData.faces.forEach { saveFace(faceData.person.id, it) }
    }

    override fun deleteFaceData(personId: String) {
        Paths.get(directory, personId).toFile().deleteRecursively()
        listeners.forEach { it.onFaceDataDelete(personId) }
    }

    override fun deleteFace(personId: String, faceId: String) {
        Paths.get(directory, personId, faceId).toFile().deleteRecursively()
        listeners.forEach { it.onFaceDelete(personId, faceId) }
    }

    override fun clearFace(personId: String) {
        getFaceIdList(personId).forEach {
            Paths.get(directory, personId, it).toFile().deleteRecursively()
        }
        listeners.forEach { it.onPersonFaceClear(personId) }
    }

    private fun listValidSubDirs(vararg paths: String) =
        Paths.get(*paths).toFile().list(this::dataFileExists).toList()

    private fun dataFileExists(dir: File, name: String) =
        Paths.get(dir.absolutePath, name).toFile().let {
            it.exists() && it.isDirectory && it.list({ _, nm -> DATA_FILE_NAME == nm }).isNotEmpty()
        }

    private fun <T> loadDataFile(clazz: Class<T>, vararg dirNames: String): T? {
        Paths.get(*dirNames, DATA_FILE_NAME).toFile().run {
            if (exists() && isFile) {
                Scanner(this).useDelimiter("\n").use {
                    return gson.fromJson(it.next(), clazz)
                }
            } else {
                return null
            }
        }
    }

    private fun saveDataFile(target: Any, vararg path: String) {
        Paths.get(*path).toFile().mkdirs()
        Paths.get(*path, DATA_FILE_NAME).toFile().writeText(gson.toJson(target))
    }

    companion object {
        const val DATA_FILE_NAME = "data.json"
    }
}