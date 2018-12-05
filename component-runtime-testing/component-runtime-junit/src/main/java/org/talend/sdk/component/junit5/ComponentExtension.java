/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.junit5;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.talend.sdk.component.junit.BaseComponentsHandler;
import org.talend.sdk.component.junit.base.junit5.JUnit5InjectionSupport;
import org.talend.sdk.component.junit.environment.Environment;

/**
 * Extension allowing the test to use a {@link org.talend.sdk.component.junit.ComponentsHandler}
 * and auto register components from current project.
 */
public class ComponentExtension extends BaseComponentsHandler
        implements BeforeAllCallback, AfterAllCallback, JUnit5InjectionSupport, BeforeEachCallback, AfterEachCallback {

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(ComponentExtension.class.getName());

    private static final String USE_EACH_KEY = ComponentExtension.class.getName() + ".useEach";

    @Override
    public void beforeAll(final ExtensionContext extensionContext) {
        final WithComponents element = extensionContext
                .getElement()
                .map(e -> e.getAnnotation(WithComponents.class))
                .orElseThrow(() -> new IllegalArgumentException(
                        "No annotation @WithComponents on " + extensionContext.getRequiredTestClass()));
        this.packageName = element.value();
        final ExtensionContext.Store store = extensionContext.getStore(NAMESPACE);
        if (element.isolatedPackages().length > 0) {
            withIsolatedPackage(null, element.isolatedPackages());
        }

        final boolean shouldUseEach = shouldIgnore(extensionContext.getElement());
        if (!shouldUseEach) {
            store.put(EmbeddedComponentManager.class.getName(), start());
        } else if (!extensionContext.getElement().map(AnnotatedElement::getAnnotations).map(annotations -> {
            int componentIndex = -1;
            for (int i = 0; i < annotations.length; i++) {
                final Class<? extends Annotation> type = annotations[i].annotationType();
                if (type == WithComponents.class) {
                    componentIndex = i;
                } else if (type == Environment.class && componentIndex >= 0) {
                    return false;
                }
            }
            return true;
        }).orElse(false)) {
            // check the ordering, if environments are put after this then the context is likely wrong
            // this condition is a simple heuristic but enough for most cases
            throw new IllegalArgumentException("If you combine @WithComponents and @Environment, you must ensure "
                    + "environment annotations are becoming before the component one otherwise you will run in an "
                    + "unexpected context and will not reproduce real execution.");
        }
        store.put(USE_EACH_KEY, shouldUseEach);
    }

    @Override
    public void afterAll(final ExtensionContext extensionContext) {
        if (!shouldUseEach(extensionContext)) {
            doClose(extensionContext);
        }
    }

    @Override
    public Class<? extends Annotation> injectionMarker() {
        return Injected.class;
    }

    @Override
    public void beforeEach(final ExtensionContext extensionContext) {
        if (shouldUseEach(extensionContext)) {
            extensionContext.getStore(NAMESPACE).put(EmbeddedComponentManager.class.getName(), start());
        }
        extensionContext.getTestInstance().ifPresent(this::injectServices);
    }

    @Override
    public void afterEach(final ExtensionContext extensionContext) {
        resetState();
        if (shouldUseEach(extensionContext)) {
            doClose(extensionContext);
        }
    }

    private Boolean shouldUseEach(final ExtensionContext extensionContext) {
        return extensionContext.getStore(NAMESPACE).get(USE_EACH_KEY, boolean.class);
    }

    private void doClose(final ExtensionContext extensionContext) {
        EmbeddedComponentManager.class
                .cast(extensionContext.getStore(NAMESPACE).get(EmbeddedComponentManager.class.getName()))
                .close();
    }

    private boolean shouldIgnore(final Optional<AnnotatedElement> element) {
        return element.map(e -> e.getDeclaredAnnotationsByType(Environment.class).length > 0).orElse(false);
    }
}
