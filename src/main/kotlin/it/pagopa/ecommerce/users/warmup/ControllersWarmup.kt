package it.pagopa.ecommerce.users.warmup

import it.pagopa.ecommerce.users.mdcutilities.LogTracingUtils
import it.pagopa.ecommerce.users.warmup.annotations.WarmupFunction
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.hasAnnotation
import kotlin.system.measureTimeMillis
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.getBeansWithAnnotation
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Component
import org.springframework.util.ClassUtils
import org.springframework.web.bind.annotation.RestController

@Component
class ControllersWarmup : ApplicationListener<ContextRefreshedEvent> {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    override fun onApplicationEvent(event: ContextRefreshedEvent) {
        val restControllers =
            event.applicationContext.getBeansWithAnnotation<RestController>().map { it.value }
        if (logger.isDebugEnabled) {
            LogTracingUtils.loggerTracingUtils()
                .success()
                .details(mapOf("rest_controllers_size" to restControllers.size.toString()))
                .logDebug(logger, "Found controllers")
        }
        restControllers.forEach(this::warmUpController)
    }

    private fun warmUpController(controllerToWarmUpInstance: Any) {
        val controllerToWarmUpKClass = ClassUtils.getUserClass(controllerToWarmUpInstance).kotlin
        var warmUpMethods: Int
        val elapsedTime = measureTimeMillis {
            warmUpMethods =
                runCatching {
                        controllerToWarmUpKClass.declaredMemberFunctions
                            .filter { it.hasAnnotation<WarmupFunction>() }
                            .parallelStream()
                            .mapToInt {
                                val result: Result<*>
                                val intertime = measureTimeMillis {
                                    result = runCatching {
                                        if (logger.isDebugEnabled) {
                                            LogTracingUtils.loggerTracingUtils()
                                                .success()
                                                .details(
                                                    mapOf(
                                                        "warmup_function" to it.toString(),
                                                    )
                                                )
                                                .logDebug(logger, "Perform warmup function")
                                        }
                                        it.call(controllerToWarmUpInstance)
                                    }
                                }
                                LogTracingUtils.loggerTracingUtils()
                                    .success()
                                    .details(
                                        mapOf(
                                            "warmup_function" to it.toString(),
                                            "elapsed_time" to intertime.toString()
                                        )
                                    )
                                    .logInfo(logger, "Warmup function")

                                if (result.isFailure) {
                                    LogTracingUtils.loggerTracingUtils()
                                        .failure()
                                        .logError(
                                            logger,
                                            result.exceptionOrNull(),
                                            "Error performing warmup method"
                                        )
                                }
                                1
                            }
                            .sum()
                    }
                    .getOrElse {
                        LogTracingUtils.loggerTracingUtils()
                            .failure()
                            .logError(logger, it, "Exception performing controller warm up")
                        0
                    }
        }
        LogTracingUtils.loggerTracingUtils()
            .success()
            .details(
                mapOf(
                    "controller" to controllerToWarmUpKClass.toString(),
                    "warmup_methods" to warmUpMethods.toString(),
                    "elapsed_time" to elapsedTime.toString()
                )
            )
            .logInfo(logger, "Controller: warm-up executed functions")
    }
}
