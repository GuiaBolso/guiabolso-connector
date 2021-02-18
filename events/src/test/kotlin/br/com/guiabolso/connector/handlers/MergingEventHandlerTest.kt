package br.com.guiabolso.connector.handlers

import br.com.guiabolso.connector.datapackage.DataPackageService
import br.com.guiabolso.connector.datapackage.model.DataPackage
import br.com.guiabolso.connector.datapackage.model.PackageSource
import br.com.guiabolso.connector.event.cache.withCacheUsage
import br.com.guiabolso.connector.misc.EasyRandomWrapper.nextObject
import br.com.guiabolso.connector.misc.buildEvent
import br.com.guiabolso.events.builder.EventBuilder
import br.com.guiabolso.events.context.EventContext
import br.com.guiabolso.events.context.EventContextHolder
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MergingEventHandlerTest {

    private lateinit var dataPackageService: DataPackageService
    private lateinit var handler: MergingEventHandler

    private fun setUp(dataPackage: DataPackage) {
        EventContextHolder.setContext(
            context = EventContext(
                id = nextObject(),
                flowId = nextObject()
            )
        )

        dataPackageService = mock()
        handler = MergingEventHandler(dataPackage, dataPackageService)
    }

    @Test
    fun `should handle event successfully`() {
        val source1 = nextObject<PackageSource>().copy(statusKey = "STATUS.SOURCE.1")
        val source2 = nextObject<PackageSource>().copy(statusKey = "STATUS.SOURCE.2")
        val dataPackage = DataPackage(
            publish = nextObject(),
            sources = listOf(source1, source2)
        )

        setUp(dataPackage)

        val userId = nextObject<String>()
        val event = buildEvent(userId = userId)

        val response = EventBuilder.responseFor(event) {}
            .withCacheUsage(event, false)

        whenever(dataPackageService.handleDataPackage(dataPackage, event)).thenReturn(response)

        val actual = handler.handle(event)

        assertThat(actual).isEqualTo(response)

        verify(dataPackageService).handleDataPackage(dataPackage, event)
        verifyNoMoreInteractions(dataPackageService)
    }
}
