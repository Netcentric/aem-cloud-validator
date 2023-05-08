package biz.netcentric.filevault.validator.aem.cloud;

/*-
 * #%L
 * AEM Cloud Validator
 * %%
 * Copyright (C) 2021 Netcentric - A Cognizant Digital Business
 * %%
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * #L%
 */

import org.apache.jackrabbit.vault.validation.spi.ValidationContext;
import org.apache.jackrabbit.vault.validation.spi.Validator;
import org.apache.jackrabbit.vault.validation.spi.ValidatorFactory;
import org.apache.jackrabbit.vault.validation.spi.ValidatorSettings;
import org.jetbrains.annotations.NotNull;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@MetaInfServices
public class AemCloudValidatorFactory implements ValidatorFactory {

    private static final String OPTION_ALLOW_VAR_NODE_OUTSIDE_CONTAINERS = "allowVarNodeOutsideContainer";
    private static final String OPTION_ALLOW_READONLY_MUTABLE_PATHS = "allowReadOnlyMutablePaths";
    private static final String OPTION_ALLOW_LIBS_NODE = "allowLibsNode";
    private static final String OPTION_ALLOW_HOOKS_IN_MUTABLE_CONTENT = "allowHooksInMutableContent";

    private static final Logger LOGGER = LoggerFactory.getLogger(AemCloudValidatorFactory.class);

    @Override
    public Validator createValidator(@NotNull ValidationContext context, @NotNull ValidatorSettings settings) {
        boolean allowReadOnlyMutablePaths = false;
        if (settings.getOptions().containsKey(OPTION_ALLOW_READONLY_MUTABLE_PATHS)) {
            allowReadOnlyMutablePaths = Boolean.parseBoolean(settings.getOptions().get(OPTION_ALLOW_READONLY_MUTABLE_PATHS));
            // deprecated option
        } else if (settings.getOptions().containsKey(OPTION_ALLOW_VAR_NODE_OUTSIDE_CONTAINERS)) {
            allowReadOnlyMutablePaths = Boolean.parseBoolean(settings.getOptions().get(OPTION_ALLOW_VAR_NODE_OUTSIDE_CONTAINERS));
            LOGGER.warn("Using deprecated option 'allowVarNodeOutsideContainer', please use 'allowReadOnlyMutablePaths' instead");
        }
        boolean allowLibsNode = false;
        boolean allowHooksInMutableContent = false;
        if (settings.getOptions().containsKey(OPTION_ALLOW_LIBS_NODE)) {
            allowLibsNode = Boolean.parseBoolean(settings.getOptions().get(OPTION_ALLOW_LIBS_NODE));
        }
        if (settings.getOptions().containsKey(OPTION_ALLOW_HOOKS_IN_MUTABLE_CONTENT)) {
            allowHooksInMutableContent = Boolean.parseBoolean(settings.getOptions().get(OPTION_ALLOW_HOOKS_IN_MUTABLE_CONTENT));
        }
        return new AemCloudValidator(allowReadOnlyMutablePaths, allowLibsNode, allowHooksInMutableContent, context.getProperties().getPackageType(), context.getContainerValidationContext(), settings.getDefaultSeverity());
    }

    @Override
    public boolean shouldValidateSubpackages() {
        return true;
    }

    @Override
    public @NotNull String getId() {
        return "netcentric-aem-cloud";
    }

    @Override
    public int getServiceRanking() {
        return 0;
    }
}
