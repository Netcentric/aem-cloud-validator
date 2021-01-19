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

    private static final Logger LOGGER = LoggerFactory.getLogger(AemCloudValidatorFactory.class);

    private static final String OPTION_ALLOW_VAR_NODE_OUTSIDE_CONTAINERS = "allowVarNodeOutsideContainer";
    
    @Override
    public Validator createValidator(@NotNull ValidationContext context, @NotNull ValidatorSettings settings) {
        boolean allowVarNodesOutsideContainers = true;
        if (settings.getOptions().containsKey(OPTION_ALLOW_VAR_NODE_OUTSIDE_CONTAINERS)) {
            allowVarNodesOutsideContainers = Boolean.parseBoolean(settings.getOptions().get(OPTION_ALLOW_VAR_NODE_OUTSIDE_CONTAINERS));
        }
        LOGGER.debug("netcentric-aem-cloud: Allow /var nodes outside containers: {}", allowVarNodesOutsideContainers);
        return new AemCloudValidator(allowVarNodesOutsideContainers, context.getContainerValidationContext(), settings.getDefaultSeverity());
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
