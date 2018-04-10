package com.github.charleslzq.faceengine.store

import com.github.charleslzq.faceengine.support.callNullableOnIo
import com.github.charleslzq.faceengine.support.callOnIo
import com.github.charleslzq.faceengine.support.runOnCompute
import com.github.charleslzq.faceengine.support.runOnIo
import com.github.charleslzq.facestore.FaceStoreChangeListener
import com.github.charleslzq.facestore.ListenableReadWriteFaceStore
import com.github.charleslzq.facestore.Meta
import com.github.charleslzq.facestore.ReadOnlyFaceStore

/**
 * Created by charleslzq on 18-3-13.
 */
open class ReadOnlyFaceStoreRxDelegate<P : Meta, F : Meta, out D : ReadOnlyFaceStore<P, F>>(
        protected val delegate: D
) : ReadOnlyFaceStore<P, F> {
    final override val faceClass: Class<F>
        get() = delegate.faceClass
    final override val personClass: Class<P>
        get() = delegate.personClass

    final override fun getPersonIds() = callOnIo { delegate.getPersonIds() }

    final override fun getPerson(personId: String) =
            callNullableOnIo { delegate.getPerson(personId) }

    final override fun getFaceIdList(personId: String) =
            callOnIo { delegate.getFaceIdList(personId) }

    final override fun getFace(personId: String, faceId: String) =
            callNullableOnIo { delegate.getFace(personId, faceId) }
}

open class ReadWriteFaceStoreRxDelegate<P : Meta, F : Meta, out D : ListenableReadWriteFaceStore<P, F>>(
        delegate: D
) : ReadOnlyFaceStoreRxDelegate<P, F, D>(delegate), ListenableReadWriteFaceStore<P, F> {
    final override val listeners = delegate.listeners

    final override fun savePerson(person: P) {
        runOnIo { delegate.savePerson(person) }
    }

    final override fun saveFace(personId: String, face: F) {
        runOnIo { delegate.saveFace(personId, face) }
    }

    final override fun deletePerson(personId: String) {
        runOnIo { delegate.deletePerson(personId) }
    }

    final override fun deleteFace(personId: String, faceId: String) {
        runOnIo { delegate.deleteFace(personId, faceId) }
    }
}

class FaceStoreChangeListenerRxDelegate<in P : Meta, in F : Meta>(
        private val delegate: FaceStoreChangeListener<P, F>
) : FaceStoreChangeListener<P, F> {
    override fun onPersonUpdate(person: P) {
        runOnCompute { delegate.onPersonUpdate(person) }
    }

    override fun onFaceUpdate(personId: String, face: F) {
        runOnCompute { delegate.onFaceUpdate(personId, face) }
    }

    override fun onFaceDelete(personId: String, faceId: String) {
        runOnCompute { delegate.onFaceDelete(personId, faceId) }
    }

    override fun onPersonDelete(personId: String) {
        runOnCompute { delegate.onPersonDelete(personId) }
    }
}