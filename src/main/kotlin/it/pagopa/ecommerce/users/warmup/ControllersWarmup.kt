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
                mapOf("restControllers.size" to restControllers.size)
            ) {
                logger.debug("Found controllers: [{}]", restControllers.size)
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
                                                    "warmingFunction" to it.toString(),
                                                )
                                            ) {
                                                logger.debug(
                                                    "Invoking function: [{}]",
                                                    it.toString()
                                                )
                                            }
                                        }
                                        it.call(controllerToWarmUpInstance)
                                    }
                                }
                                LogTracingUtils.withContextDetailsMdc(
                                    mapOf(
                                        "warmingFunction" to it.toString(),
                                        "elsapsedTime" to intertime
                                    ),
                                    mapOf(
                                        LogTracingUtils.TracingEntry.EVENT_OUTCOME.key to
                                            result.isSuccess
                                    )
                                ) {
                                    logger.info(
                                        "Warmup function: [{}] -> elapsed time: [{}]. Is ok: [{}] ",
                                        it.toString(),
                                        intertime,
                                        result.isSuccess
                                    )
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
                                        logger.error(
                                            "Error performing warmup method: [$it]",
                                            result.exceptionOrNull()
                                        )
                                    }
                                }
                                1
                            }
                            .sum()
                    }
                    .getOrElse {
                        LogTracingUtils.withErrorMdc(
                            it,
                            mapOf(
                                LogTracingUtils.TracingEntry.EVENT_OUTCOME.key to 0,
                                LogTracingUtils.TracingEntry.ERROR_MESSAGE.key to
                                    "Error performing warmup method"
                            )
                        ) {
                            logger.error("Exception performing controller warm up ", it)
                        }
                        0
                    }
        }
        LogTracingUtils.withContextDetailsMdc(
            mapOf(
                "controller" to controllerToWarmUpKClass,
                "warmUpMethods" to warmUpMethods,
                "elsapsedTime" to elapsedTime
            )
        ) {
            logger.info(
                "Controller: [{}] warm-up executed functions: [{}], elapsed time: [{}] ms",
                controllerToWarmUpKClass,
                warmUpMethods,
                elapsedTime
            )
        }
    }
}
