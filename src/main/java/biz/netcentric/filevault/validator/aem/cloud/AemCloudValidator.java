package biz.netcentric.filevault.validator.aem.cloud;

import java.nio.file.Path;

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

import java.util.Collection;
import java.util.Collections;

import org.apache.jackrabbit.vault.validation.spi.NodePathValidator;
import org.apache.jackrabbit.vault.validation.spi.ValidationContext;
import org.apache.jackrabbit.vault.validation.spi.ValidationMessage;
import org.apache.jackrabbit.vault.validation.spi.ValidationMessageSeverity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AemCloudValidator implements NodePathValidator {

    static final String VIOLATION_MESSAGE_STRING = "Using nodes below /var is only allowed in author-specific packages. Further details at https://experienceleague.adobe.com/docs/experience-manager-learn/cloud-service/debugging/debugging-aem-as-a-cloud-service/build-and-deployment.html?lang=en#including-%2Fvar-in-content-package";

    private final @NotNull ValidationMessageSeverity defaultSeverity;
    private final ValidationContext containerValidationContext;
    private boolean foundViolation;

    public AemCloudValidator(@Nullable ValidationContext containerValidationContext, @NotNull ValidationMessageSeverity defaultSeverity) {
        super();
        this.containerValidationContext = containerValidationContext;
        this.defaultSeverity = defaultSeverity;
        this.foundViolation = false;
    }

    @Override
    public Collection<ValidationMessage> validate(@NotNull String path) {
        // only check if within a container
        if (containerValidationContext != null && !foundViolation) {
            // check if package itself is only used on author
            if (path.startsWith("/var/") && !isContainedInAuthorOnlyPackage(containerValidationContext)) {
                // only emit once per package
                foundViolation = true;
                return Collections.singleton(new ValidationMessage(defaultSeverity, VIOLATION_MESSAGE_STRING));
            }
        }
        return null;
    }

    /** 
     * 
     * @param containerValidationContext the container validation context of the package to be validated.
     * @return {@code true} in case this package has been included in a "author" run mode specific folder, otherwise {@code false}
     */
    private boolean isContainedInAuthorOnlyPackage(ValidationContext containerValidationContext) {
        if (containerValidationContext ==  null) {
            return false;
        } else {
            // assume that run mode specific folder
            if (isPackagePathInstalledConditionally("author", containerValidationContext.getPackageRootPath())) {
                return true;
            } else {
                return isContainedInAuthorOnlyPackage(containerValidationContext.getContainerValidationContext());
            }
        }
    }
    
    static boolean isPackagePathInstalledConditionally(String runMode, Path packageRootPath) {
        // https://experienceleague.adobe.com/docs/experience-manager-cloud-service/implementing/developing/aem-project-content-package-structure.html?lang=en#embeddeds
        for (int i = packageRootPath.getNameCount() - 1; i-- > 0; ) {
            if (packageRootPath.getName(i).toString().equals("install." + runMode)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public @Nullable Collection<ValidationMessage> done() {
        return null;
    }

}
