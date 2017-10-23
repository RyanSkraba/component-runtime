/**
 *  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.talend.components.runtime.manager.reflect.parameterenricher;

import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.talend.component.api.configuration.action.Discoverable;
import org.talend.component.api.configuration.action.meta.ActionRef;
import org.talend.component.api.service.ActionType;
import org.talend.components.spi.parameter.ParameterExtensionEnricher;

public class ActionParameterEnricher implements ParameterExtensionEnricher {

    public static final String META_PREFIX = "tcomp::action::";

    @Override
    public Map<String, String> onParameterAnnotation(final String parameterName, final Type parameterType,
            final Annotation annotation) {
        final ActionRef ref = annotation.annotationType().getAnnotation(ActionRef.class);
        if (ref == null) {
            return emptyMap();
        }
        final String type = ref.value().getAnnotation(ActionType.class).value();
        return new HashMap<String, String>() {

            {
                put(META_PREFIX + type, getValueString(annotation));
                ofNullable(getParametersString(annotation)).ifPresent(v -> put(META_PREFIX + type + "::parameters", v));
                ofNullable(getBinding(annotation)).ifPresent(v -> put(META_PREFIX + type + "::binding", v));
            }
        };
    }

    private String getValueString(final Annotation annotation) {
        try {
            return String.valueOf(annotation.annotationType().getMethod("value").invoke(annotation));
        } catch (final IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalArgumentException("No value for " + annotation);
        }
    }

    private String getBinding(final Annotation annotation) {
        try {
            return Discoverable.Binding.class.cast(annotation.annotationType().getMethod("binding").invoke(annotation)).name();
        } catch (final IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            return null;
        }
    }

    private String getParametersString(final Annotation annotation) {
        try {
            return Stream.of(String[].class.cast(annotation.annotationType().getMethod("parameters").invoke(annotation)))
                    .collect(Collectors.joining(","));
        } catch (final IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            return null;
        }
    }
}
