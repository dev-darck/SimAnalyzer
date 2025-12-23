package com.project.analyzer.navigation.impl

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.StateObject
import androidx.savedstate.compose.serialization.serializers.SnapshotStateListSerializer
import com.project.analyzer.navigation.api.Route
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = BackStackSerializer::class)
class BackStack<T : Route>(internal val stack: SnapshotStateList<T>) :
    MutableList<T> by stack,
    StateObject by stack,
    RandomAccess by stack

internal class BackStackSerializer<T : Route>(elementSerializer: KSerializer<T>) :
    KSerializer<BackStack<T>> {

    private val delegate = SnapshotStateListSerializer(elementSerializer)

    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor =
        SerialDescriptor("com.project.analyzer.navigation.impl.BackStack", delegate.descriptor)

    override fun serialize(encoder: Encoder, value: BackStack<T>) {
        encoder.encodeSerializableValue(serializer = delegate, value = value.stack)
    }

    override fun deserialize(decoder: Decoder): BackStack<T> {
        return BackStack(stack = decoder.decodeSerializableValue(deserializer = delegate))
    }
}
