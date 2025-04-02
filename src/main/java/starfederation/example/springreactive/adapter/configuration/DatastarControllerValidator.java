package starfederation.example.springreactive.adapter.configuration;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.NonNull;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import starfederation.datastar.utils.DataStore;
import starfederation.example.springreactive.adapter.annotations.DatastarController;

import java.lang.reflect.Method;
import java.util.Arrays;

@Configuration(proxyBeanMethods = false)
public class DatastarControllerValidator implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(@NonNull  Object bean, @NonNull String beanName) throws BeansException {
        Class<?> targetClass = AopUtils.getTargetClass(bean);

        if (AnnotationUtils.findAnnotation(targetClass, DatastarController.class) != null) {
            validateControllerMethods(targetClass);
        }
        return bean;
    }

    private void validateControllerMethods(Class<?> controllerClass) {
        ReflectionUtils.doWithMethods(controllerClass, method -> {
            if (isSignalHandler(method) && !hasDataStoreParameter(method)) {
                throw new IllegalStateException(
                        String.format("Method %s in controller %s must declare a DataStore parameter",
                                method.getName(),
                                controllerClass.getSimpleName()
                        )
                );
            }
        });
    }

    private boolean isSignalHandler(Method method) {
        RequestMapping mapping = AnnotationUtils.getAnnotation(method, RequestMapping.class);
        return mapping != null && Arrays.stream(mapping.method())
                .anyMatch(m -> m == RequestMethod.GET
                        || m == RequestMethod.POST
                        || m == RequestMethod.PUT
                        || m == RequestMethod.PATCH);
    }

    private boolean hasDataStoreParameter(Method method) {
        return Arrays.stream(method.getParameterTypes())
                .anyMatch(DataStore.class::isAssignableFrom);
    }
}