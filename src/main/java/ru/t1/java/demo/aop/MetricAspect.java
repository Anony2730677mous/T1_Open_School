package ru.t1.java.demo.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import ru.t1.java.demo.kafka.KafkaAspectMessageProducer;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Async
@Slf4j
@Aspect
@Component
public class MetricAspect {

    private static final AtomicLong START_TIME = new AtomicLong();

    @Value("${track.time-limit-exceed}")
    private long timeLimitExceed;
    @Value("${t1.kafka.topic.time-limit-exceed-topic}")
    private String kafkaTopic;

    @Autowired
    private KafkaAspectMessageProducer kafkaAspectMessageProducer;

    @Before("@annotation(ru.t1.java.demo.aop.Track)")
    public void logExecTime(JoinPoint joinPoint) throws Throwable {
        log.info("Старт метода: {}", joinPoint.getSignature().toShortString());
        START_TIME.addAndGet(System.currentTimeMillis());
    }

    @After("@annotation(ru.t1.java.demo.aop.Track)")
    public void calculateTime(JoinPoint joinPoint) {
        long afterTime = System.currentTimeMillis();
        log.info("Время исполнения: {} ms", (afterTime - START_TIME.get()));
        START_TIME.set(0L);
    }

    @Around("@annotation(ru.t1.java.demo.aop.Track)")
    public Object logExecTime(ProceedingJoinPoint pJoinPoint) throws Throwable {
        log.info("Вызов метода: {}", pJoinPoint.getSignature().toShortString());
        long beforeTime = System.currentTimeMillis();
        Object result = null;
        try {
            result = pJoinPoint.proceed();//Important
        } finally {
            long afterTime = System.currentTimeMillis();
            log.info("Время исполнения: {} ms", (afterTime - beforeTime));
        }

        return result;
    }
    @Around("@annotation(ru.t1.java.demo.aop.Track)")
    public Object logExecTimeLimitExceed(ProceedingJoinPoint pJoinPoint) throws Throwable {
        log.info("Вызов метода: {}", pJoinPoint.getSignature().toShortString());
        long beforeTime = System.currentTimeMillis();
        Object result;
        try {
            result = pJoinPoint.proceed();
        } finally {
            long afterTime = System.currentTimeMillis();
            long executionTime = afterTime - beforeTime;

            if (executionTime > timeLimitExceed) {
                log.warn("Время исполнения метода {} превысило лимит: {} ms", pJoinPoint.getSignature().toShortString(), executionTime);
                String methodName = pJoinPoint.getSignature().getName();
                Object[] methodArguments = pJoinPoint.getArgs();
                String methodArgumentsStrings = (methodArguments !=null) ? Arrays.stream(methodArguments)
                        .map(Object::toString)
                        .collect(Collectors.joining(", ")): "Аргументы метода отсутствуют";
                String kafkaMessage = String.format("Method: %s, Execution time: %d, Method arguments: %s",
                        methodName, executionTime, methodArgumentsStrings);
                try {
                    CompletableFuture<SendResult<String,String>> future = kafkaAspectMessageProducer.sendAspectMessage(kafkaTopic, kafkaMessage);
                    future.whenCompleteAsync((futureResult, error) ->{
                        if(error != null){
                            log.error("Не удается отправить сообщение в Kafka: " + error.getMessage());
                        }
                        else {
                            log.info("Сообщение в Kafka отправлено успешно {}", kafkaMessage);
                        }
                    });

                } catch (Exception e) {
                    log.error("Ошибка при отправке сообщения в Kafka в топик {}: {}", kafkaTopic, e.getMessage());
                }
            }
        }
        return result;
    }

}
