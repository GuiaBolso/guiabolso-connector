package br.com.guiabolso.connector.misc

import org.jeasy.random.EasyRandom
import org.jeasy.random.EasyRandomParameters

object EasyRandomWrapper {

    val easyRandom = EasyRandom(
        EasyRandomParameters()
            .seed(System.currentTimeMillis())
            .objectPoolSize(20)
            .randomizationDepth(3)
            .charset(Charsets.UTF_8)
            .stringLengthRange(5, 15)
            .collectionSizeRange(1, 10)
            .scanClasspathForConcreteTypes(true)
            .overrideDefaultInitialization(false)
            .ignoreRandomizationErrors(true)
    )

    inline fun <reified T> nextObject(): T = easyRandom.nextObject(T::class.java)
}
