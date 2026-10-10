package mx.cec.crm.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import java.time.Clock;
import java.util.concurrent.Executor;

@Slf4j
@Configuration
@EnableAsync
@EnableConfigurationProperties(PasswordResetSettings.class)
public class PasswordResetConfig implements AsyncConfigurer {
    @Bean
    public Clock clock() { return Clock.systemUTC(); }
    @Bean(name = "passwordResetExecutor")
    public Executor passwordResetExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("password-reset-");
        return executor;
    }
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        // SMTP errors can include recipients or credentials: omit messages and arguments.
        return (exception, method, arguments) -> log.error(
                "Falló el envío de recuperación. Revisar SMTP. Tipo: {}", exception.getClass().getSimpleName());
    }
}

