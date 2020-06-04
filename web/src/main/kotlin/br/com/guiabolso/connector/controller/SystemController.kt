package br.com.guiabolso.connector.controller

import br.com.guiabolso.connector.common.tracking.Tracer
import org.springframework.stereotype.Component
import ro.pippo.controller.Controller
import ro.pippo.controller.GET
import ro.pippo.controller.Path
import ro.pippo.controller.Produces

@Path("/")
@Component
class SystemController : Controller() {

    @GET("/")
    @Produces(Produces.JSON)
    fun index(): Map<String, Boolean> {
        Tracer.setOperationName("/")
        return mapOf("status" to true)
    }

    @GET("/health")
    @Produces(Produces.JSON)
    fun health(): Map<String, Boolean> {
        Tracer.setOperationName("/health")
        return mapOf("status" to true)
    }
}
