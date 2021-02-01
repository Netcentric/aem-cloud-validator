package biz.netcentric.filevault.validator.aem.cloud;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

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
import java.util.regex.Pattern;

import org.apache.jackrabbit.vault.packaging.PackageType;
import org.apache.jackrabbit.vault.util.Constants;
import org.apache.jackrabbit.vault.util.DocViewNode;
import org.apache.jackrabbit.vault.validation.spi.DocumentViewXmlValidator;
import org.apache.jackrabbit.vault.validation.spi.MetaInfPathValidator;
import org.apache.jackrabbit.vault.validation.spi.NodePathValidator;
import org.apache.jackrabbit.vault.validation.spi.ValidationContext;
import org.apache.jackrabbit.vault.validation.spi.ValidationMessage;
import org.apache.jackrabbit.vault.validation.spi.ValidationMessageSeverity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AemCloudValidator implements NodePathValidator, MetaInfPathValidator, DocumentViewXmlValidator {

    static final String VIOLATION_MESSAGE_STRING_VAR_NODES_CONDITION_CONTAINER = "only allowed in author-specific packages";
    static final String VIOLATION_MESSAGE_STRING_VAR_NODES_CONDITION_OVERALL = "not allowed";
    static final String VIOLATION_MESSAGE_STRING_VAR_NODES = "Using nodes below /var is %s. Consider to use repoinit scripts instead or move that content to another location. Further details at https://experienceleague.adobe.com/docs/experience-manager-learn/cloud-service/debugging/debugging-aem-as-a-cloud-service/build-and-deployment.html?lang=en#including-%%2Fvar-in-content-package";
    static final String VIOLATION_MESSAGE_INSTALL_HOOK_IN_MUTABLE_PACKAGE = "Using install hooks in mutable content packages leads to deployment failures as the underlying service user on the publish does not have the right to execute those.";
    static final String VIOLATION_MESSAGE_INVALID_INDEX_DEFINITION_NODE_NAME = "All Oak index definition node names must end with '-custom-<integer>' but found name '%s'. Further details at https://experienceleague.adobe.com/docs/experience-manager-cloud-service/operations/indexing.html?lang=en#how-to-use";

    // this path is relative to META-INF
    private static final Path INSTALL_HOOK_PATH = Paths.get(Constants.VAULT_DIR, Constants.HOOKS_DIR);
    private static final @NotNull String VIOLATION_MESSAGE_NON_LUCENE_TYPE_INDEX_DEFINITION = "Only oak:QueryIndexDefinitions of type='lucene' are supported in AEMaaCS but found type='%s'. Compare with https://experienceleague.adobe.com/docs/experience-manager-cloud-service/operations/indexing.html?lang=en#changes-in-aem-as-a-cloud-service";

    private static final Pattern INDEX_DEFINITION_NAME_PATTERN = Pattern.compile(".*-custom-\\d++");
    
    private final @NotNull ValidationMessageSeverity defaultSeverity;
    private final ValidationContext containerValidationContext;
    private final PackageType packageType;
    private boolean foundViolation;
    private boolean hasMutableNodes;
    private boolean allowVarNodesOutsideContainers;
    private boolean hasInstallHooks;

    public AemCloudValidator(boolean allowVarNodesOutsideContainers, @Nullable PackageType packageType, @Nullable ValidationContext containerValidationContext, @NotNull ValidationMessageSeverity defaultSeverity) {
        super();
        this.packageType = packageType;
        this.containerValidationContext = containerValidationContext;
        this.defaultSeverity = defaultSeverity;
        this.foundViolation = false;
        this.allowVarNodesOutsideContainers = allowVarNodesOutsideContainers;
        this.hasMutableNodes = false;
        this.hasInstallHooks = false;
    }

    @Override
    public Collection<ValidationMessage> validate(@NotNull String path) {
        if (!foundViolation && path.startsWith("/var/")) {
            // check if package itself is only used on author
            if (!allowVarNodesOutsideContainers || !isContainedInAuthorOnlyPackage(containerValidationContext)) {
                // only emit once per package
                foundViolation = true;
                return Collections.singleton(new ValidationMessage(defaultSeverity, String.format(
                        VIOLATION_MESSAGE_STRING_VAR_NODES, allowVarNodesOutsideContainers ? VIOLATION_MESSAGE_STRING_VAR_NODES_CONDITION_CONTAINER : VIOLATION_MESSAGE_STRING_VAR_NODES_CONDITION_OVERALL)));
            }
        }
        if (!path.startsWith("/apps/") && !path.startsWith("/libs")) {
            hasMutableNodes = true;
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
        if (hasInstallHooks && hasMutableNodes) {
            return Collections.singleton(new ValidationMessage(defaultSeverity, VIOLATION_MESSAGE_INSTALL_HOOK_IN_MUTABLE_PACKAGE));
        }
        return null;
    }

    @Override
    public @Nullable Collection<ValidationMessage> validateMetaInfPath(@NotNull Path filePath) {
        // is it install hook?
        if (filePath.startsWith(INSTALL_HOOK_PATH) && filePath.toString().endsWith(".jar")) {
            // is it mutable content package?
            if (PackageType.CONTENT.equals(packageType) || PackageType.MIXED.equals(packageType)) {
                return Collections.singleton(new ValidationMessage(defaultSeverity, VIOLATION_MESSAGE_INSTALL_HOOK_IN_MUTABLE_PACKAGE));
            } else if (packageType == null) {
                // defer checking until one is sure that the package has mutable content
                hasInstallHooks = true;
            }
        }
        return null;
    }

    @Override
    public @Nullable Collection<ValidationMessage> validate(@NotNull DocViewNode node, @NotNull String nodePath,
            @NotNull Path filePath, boolean isRoot) {
        if ("oak:QueryIndexDefinition".equals(node.primary)) {
            Collection<ValidationMessage> messages = new ArrayList<>();
            String indexType = node.getValue("{}type");
            if (!"lucene".equals(indexType)) {
                messages.add(new ValidationMessage(defaultSeverity, String.format(VIOLATION_MESSAGE_NON_LUCENE_TYPE_INDEX_DEFINITION, indexType)));
            }
            // check node name
            if (!INDEX_DEFINITION_NAME_PATTERN.matcher(node.name).matches()) {
                messages.add(new ValidationMessage(defaultSeverity, String.format(VIOLATION_MESSAGE_INVALID_INDEX_DEFINITION_NODE_NAME, node.name)));
            }
            return messages;
        }
        return null;
    }

}
