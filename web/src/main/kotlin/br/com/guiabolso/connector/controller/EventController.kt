package br.com.guiabolso.connector.controller

import br.com.guiabolso.events.server.EventProcessor
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import ro.pippo.controller.Controller
import ro.pippo.controller.POST
import ro.pippo.controller.Path
import ro.pippo.controller.Produces

@Path("/")
@Component
class EventController(
    @Qualifier("partnerEventProcessor") private val partnerEventProcessor: EventProcessor,
    @Qualifier("userEventProcessor") private val userEventProcessor: EventProcessor
) : Controller() {

    @POST("/partner/events/")
    @Produces(Produces.JSON)
    fun partnerEventHandler(): String {
        return partnerEventProcessor.processEvent(request.body)
    }

    @POST("/gbConnect/events/")
    @Produces(Produces.JSON)
    fun oAuthEventHandler(): String {
        return userEventProcessor.processEvent(request.body)
    }
}
