package br.com.guiabolso.connector.handlers

import br.com.guiabolso.connector.datapackage.DataPackageService
import br.com.guiabolso.connector.datapackage.model.DataPackage
import br.com.guiabolso.events.model.RequestEvent
import br.com.guiabolso.events.model.ResponseEvent
import br.com.guiabolso.events.server.handler.EventHandler

class MergingEventHandler(
    private val dataPackage: DataPackage,
    private val dataPackageService: DataPackageService
) : EventHandler {

    override val eventName = dataPackage.publish.name
    override val eventVersion = dataPackage.publish.version

    override fun handle(event: RequestEvent): ResponseEvent {
        return dataPackageService.handleDataPackage(dataPackage, event)
    }
}
