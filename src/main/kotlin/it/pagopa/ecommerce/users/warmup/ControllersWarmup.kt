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
            LogTracingUtils.withContextDetailsMdc(
                mapOf("rest_controllers_size" to restControllers.size)
            ) {
                logger.debug("Found controllers")
            }
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
                                            LogTracingUtils.withContextDetailsMdc(
                                                mapOf(
                                                    "warmup_function" to it.toString(),
                                                )
                                            ) {
                                                logger.debug("Perform warmup function")
                                            }
                                        }
                                        it.call(controllerToWarmUpInstance)
                                    }
                                }
                                LogTracingUtils.withContextDetailsMdc(
                                    mapOf(
                                        "warmup_function" to it.toString(),
                                        "elapsed_time" to intertime
                                    ),
                                    mapOf(
                                        LogTracingUtils.TracingEntry.EVENT_OUTCOME.key to
                                            result.isSuccess
                                    )
                                ) {
                                    logger.info("Warmup function")
                                }

                                if (result.isFailure) {
                                    LogTracingUtils.withErrorMdc(
                                        result.exceptionOrNull(),
                                        mapOf(
                                            LogTracingUtils.TracingEntry.EVENT_OUTCOME.key to
                                                result.isFailure,
                                            LogTracingUtils.TracingEntry.EVENT_ACTION.key to
                                                it.toString()
                                        )
                                    ) {
                                        logger.error("Error performing warmup method")
                                    }
                                }
                                1
                            }
                            .sum()
                    }
                    .getOrElse {
                        LogTracingUtils.withErrorMdc(it) {
                            logger.error("Exception performing controller warm up")
                        }
                        0
                    }
        }
        LogTracingUtils.withContextDetailsMdc(
            mapOf(
                "controller" to controllerToWarmUpKClass,
                "warmup_methods" to warmUpMethods,
                "elapsed_time" to elapsedTime
            )
        ) {
            logger.info("Controller: warm-up executed functions")
        }
    }
}
