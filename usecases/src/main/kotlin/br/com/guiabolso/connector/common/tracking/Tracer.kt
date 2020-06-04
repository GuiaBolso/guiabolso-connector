package br.com.guiabolso.connector.common.tracking

import br.com.guiabolso.tracing.factory.TracerFactory

object Tracer : br.com.guiabolso.tracing.Tracer by TracerFactory.createTracer()
