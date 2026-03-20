package com.blackboxpro.runtime.bindings

import com.blackboxpro.common.runtime.LogHandler
import com.blackboxpro.common.runtime.LoggerSupplier
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * NeoForge platform binding for LoggerSupplier.
 * Converts SLF4J Logger to the common LogHandler interface.
 */
object LoggerSupplierBinding : LoggerSupplier {
    override fun getLogger(name: String): LogHandler = NeoForgeSlf4jLogHandler(LoggerFactory.getLogger(name))
}

private class NeoForgeSlf4jLogHandler(private val logger: Logger) : LogHandler {
    override fun info(message: String, vararg args: Any?) = logger.info(message, *args)
    override fun debug(message: String, vararg args: Any?) = logger.debug(message, *args)
    override fun warn(message: String, vararg args: Any?) = logger.warn(message, *args)
    override fun error(message: String, vararg args: Any?) = logger.error(message, *args)
}
