package biz.netcentric.filevault.validator.aem.cloud;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

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
import java.util.Optional;
import java.util.regex.Pattern;

import javax.jcr.PropertyType;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.vault.packaging.PackageProperties;
import org.apache.jackrabbit.vault.packaging.PackageType;
import org.apache.jackrabbit.vault.util.Constants;
import org.apache.jackrabbit.vault.util.DocViewNode2;
import org.apache.jackrabbit.vault.util.DocViewProperty2;
import org.apache.jackrabbit.vault.validation.spi.DocumentViewXmlValidator;
import org.apache.jackrabbit.vault.validation.spi.MetaInfPathValidator;
import org.apache.jackrabbit.vault.validation.spi.NodeContext;
import org.apache.jackrabbit.vault.validation.spi.NodePathValidator;
import org.apache.jackrabbit.vault.validation.spi.PropertiesValidator;
import org.apache.jackrabbit.vault.validation.spi.ValidationContext;
import org.apache.jackrabbit.vault.validation.spi.ValidationMessage;
import org.apache.jackrabbit.vault.validation.spi.ValidationMessageSeverity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AemCloudValidator implements NodePathValidator, MetaInfPathValidator, DocumentViewXmlValidator, PropertiesValidator {

    static final String VIOLATION_MESSAGE_READONLY_MUTABLE_PATH = "Using mutable nodes in this repository location is only allowed in author-specific packages as it is not writable by the underlying service user on a publish instance. Consider to use repoinit scripts instead or move that content to another location. Further details at https://experienceleague.adobe.com/docs/experience-manager-learn/cloud-service/debugging/debugging-aem-as-a-cloud-service/build-and-deployment.html?lang=en#including-%2Fvar-in-content-package";
    static final String VIOLATION_MESSAGE_INSTALL_HOOK_IN_MUTABLE_PACKAGE = "Using install hooks in mutable content packages leads to deployment failures as the underlying service user on the publish does not have the right to execute those.";
    static final String VIOLATION_MESSAGE_INVALID_INDEX_DEFINITION_NODE_NAME = "All Oak index definition node names must follow scheme '<indexName>-<productVersion>-custom-<customVersion>' (for a customized OOTB index) or '<prefix>.<indexName>-<productVersion>-custom-<customVersion>' (for a fully customized index) but found name '%s'. Further details at https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/operations/indexing#preparing-the-new-index-definition";
    static final String VIOLATION_MESSAGE_LIBS_NODES = "Nodes below '/libs' may be overwritten by future product upgrades. Rather use '/apps'. Further details at https://experienceleague.adobe.com/docs/experience-manager-cloud-service/implementing/developing/full-stack/overlays.html?lang=en#developing";
    static final String VIOLATION_MESSAGE_MUTABLE_NODES_IN_MIXED_PACKAGE = "Mutable nodes in mixed package types are not installed!";
    static final String VIOLATION_MESSAGE_MUTABLE_NODES_AND_IMMUTABLE_NODES_IN_SAME_PACKAGE = "Mutable and immutable nodes must not be mixed in the same package. You must separate those into two packages and give them both a dedicated package type!";
    static final String VIOLATION_MESSAGE_NON_LUCENE_TYPE_INDEX_DEFINITION = "Only oak:QueryIndexDefinitions of type='lucene' are supported in AEMaaCS but found type='%s'. Compare with https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/operations/indexing#current-limitations";
    static final String VIOLATION_MESSAGE_INVALID_COMPAT_VERSION_IN_INDEX_DEFINITION = "The compatVersion property of an oak:QueryIndexDefinition must be set to the Long value '2' but found '%s'. Compare with https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/operations/indexing#current-limitations";
    
    // this path is relative to META-INF
    private static final Path INSTALL_HOOK_PATH = Paths.get(Constants.VAULT_DIR, Constants.HOOKS_DIR);
    /**
     * The allowed patterns are defined in https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/operations/indexing#preparing-the-new-index-definition
     */
    private static final Pattern INDEX_DEFINITION_NAME_PATTERN = Pattern.compile(".*-\\d++-custom-\\d++");
    
    private static final Collection<String> IMMUTABLE_PATH_PREFIXES = Arrays.asList("/apps", "/libs", "/oak:index");
    private static final Collection<String> WRITABLE_PATHS_BY_DISTRIBUTION_IMPORTER = Arrays.asList(
            "/content",     // access provided by system user content-writer-service and sling-distribution-importer
            "/etc",         // access provided by system user version-manager-service and sling-distribution-importer
            "/conf",        // access provided by system user version-manager-service and sling-distribution-importer
            "/home/users",  // access provided by system user user-administration-service
            "/home/groups"  // access provided by system user group-administration-service
            );

    private final @NotNull ValidationMessageSeverity defaultSeverity;
    private final ValidationContext containerValidationContext;
    private final PackageType packageType;
    private boolean hasMutableNodes;
    private final boolean allowReadOnlyMutablePaths;
    private final boolean allowLibsNode;
    private final boolean allowHooksInMutableContent;
    private boolean hasInstallHooks;
    private boolean hasImmutableNodes;

    private static final int MAX_NUM_VIOLATIONS_PER_TYPE = 5;
    private static final Name PN_TYPE = NameFactoryImpl.getInstance().create(Name.NS_DEFAULT_URI, "type");
    private static final Name PN_COMPAT_VERSION = NameFactoryImpl.getInstance().create(Name.NS_DEFAULT_URI, "compatVersion");
    private int numVarNodeViolations = 0;
    private int numLibNodeViolations = 0;
    private int numMutableNodeViolations = 0;

    public AemCloudValidator(boolean allowReadOnlyMutablePaths, boolean allowLibsNode, boolean allowHooksInMutableContent, @Nullable PackageType packageType,
                             @Nullable ValidationContext containerValidationContext, @NotNull ValidationMessageSeverity defaultSeverity) {
        super();
        this.allowReadOnlyMutablePaths = allowReadOnlyMutablePaths;
        this.allowLibsNode = allowLibsNode;
        this.allowHooksInMutableContent = allowHooksInMutableContent;
        this.packageType = packageType;
        this.containerValidationContext = containerValidationContext;
        this.defaultSeverity = defaultSeverity;
        this.hasMutableNodes = false;
        this.hasImmutableNodes = false;
        this.hasInstallHooks = false;
    }

    @Override
    public Collection<ValidationMessage> validate(@NotNull String path) {
        Collection<ValidationMessage> messages = new ArrayList<>();
        // skip root node for mutable/immutable path classification
        if (!"/".equals(path)) {
            if (isMutablePath(path)) {
                hasMutableNodes = true;
                if (numVarNodeViolations < MAX_NUM_VIOLATIONS_PER_TYPE && !isPathWritableByDistributionJournalImporter(path)) {
                    // check if package itself is only used on author
                    if (!allowReadOnlyMutablePaths && !isContainedInAuthorOnlyPackage(containerValidationContext)) {
                        // only emit once per package
                        messages.add(new ValidationMessage(defaultSeverity, VIOLATION_MESSAGE_READONLY_MUTABLE_PATH));
                        numVarNodeViolations++;
                    }
                }
                if (numMutableNodeViolations < MAX_NUM_VIOLATIONS_PER_TYPE && PackageType.MIXED.equals(packageType)) {
                    messages.add(new ValidationMessage(defaultSeverity, VIOLATION_MESSAGE_MUTABLE_NODES_IN_MIXED_PACKAGE));
                    numMutableNodeViolations++;
                }
            } else {
                hasImmutableNodes = true;
            }
        }
        if (numLibNodeViolations < MAX_NUM_VIOLATIONS_PER_TYPE && !allowLibsNode && path.startsWith("/libs")) {
            messages.add(new ValidationMessage(defaultSeverity, VIOLATION_MESSAGE_LIBS_NODES));
            numLibNodeViolations++;
        }
        return messages;
    }

    static boolean isMutablePath(String path) {
        for (String immutablePathPrefix : IMMUTABLE_PATH_PREFIXES) {
            if (path.startsWith(immutablePathPrefix+"/") || path.equals(immutablePathPrefix)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 
     * @return {@code true} in case the given mutable path is writable by the service user used by the Content Distribution Journal Importer used on AEMaaCS publish
     */
    static boolean isPathWritableByDistributionJournalImporter(String path) {
        for (String writablePath : WRITABLE_PATHS_BY_DISTRIBUTION_IMPORTER) {
            if (path.startsWith(writablePath + "/") || path.equals(writablePath)) {
                return true;
            }
        }
        return false;
    }

    /** @param containerValidationContext the container validation context of the package to be validated.
     * @return {@code true} in case this package has been included in a "author" run mode specific folder, otherwise {@code false} */
    private boolean isContainedInAuthorOnlyPackage(ValidationContext containerValidationContext) {
        if (containerValidationContext == null) {
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
        for (int i = packageRootPath.getNameCount() - 1; i-- > 0;) {
            if (packageRootPath.getName(i).toString().equals("install." + runMode)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public @Nullable Collection<ValidationMessage> done() {
        Collection<ValidationMessage> messages = new ArrayList<>();
        if (hasInstallHooks && hasMutableNodes && !allowHooksInMutableContent) {
            messages.add(new ValidationMessage(defaultSeverity, VIOLATION_MESSAGE_INSTALL_HOOK_IN_MUTABLE_PACKAGE));
        }
        // for non-set package types usually the package type is determined by cp2fm, but it will be MIXED in case both mutable and immutable nodes are contained
        if (packageType == null && hasMutableNodes && hasImmutableNodes) {
            messages.add(new ValidationMessage(defaultSeverity, VIOLATION_MESSAGE_MUTABLE_NODES_AND_IMMUTABLE_NODES_IN_SAME_PACKAGE));
        }
        return messages;
    }

    @Override
    public @Nullable Collection<ValidationMessage> validateMetaInfPath(@NotNull Path filePath) {
        // is it install hook?
        if (filePath.startsWith(INSTALL_HOOK_PATH) && filePath.toString().endsWith(".jar")) {
            // is it mutable content package?
            if (PackageType.CONTENT.equals(packageType)) {
                hasInstallHooks = true;
                if (!allowHooksInMutableContent) {
                    return Collections.singleton(new ValidationMessage(defaultSeverity, VIOLATION_MESSAGE_INSTALL_HOOK_IN_MUTABLE_PACKAGE));
                }
            } else if (packageType == null) {
                // defer checking until one is sure that the package has mutable content
                hasInstallHooks = true;
            }
        }
        return null;
    }

    @Override
    public @Nullable Collection<ValidationMessage> validate(@NotNull DocViewNode2 node, @NotNull NodeContext nodeContext, boolean isRoot) {
        if ("oak:QueryIndexDefinition".equals(node.getPrimaryType().orElse(""))) {
            Collection<ValidationMessage> messages = new ArrayList<>();
            String indexType = node.getPropertyValue(PN_TYPE).orElse("");
            if (!"lucene".equals(indexType)) {
                messages.add(new ValidationMessage(defaultSeverity,
                        String.format(VIOLATION_MESSAGE_NON_LUCENE_TYPE_INDEX_DEFINITION, indexType)));
            } else {
                Optional<DocViewProperty2> compatVersionProperty = node.getProperty(PN_COMPAT_VERSION);
                if (!compatVersionProperty.isPresent() || compatVersionProperty.get().getType() != PropertyType.LONG || !compatVersionProperty.get().getStringValue().orElse("").equals("2")) {
                    messages.add(new ValidationMessage(defaultSeverity,
                            String.format(VIOLATION_MESSAGE_INVALID_COMPAT_VERSION_IN_INDEX_DEFINITION, compatVersionProperty.map(p -> p.formatValue()).orElse("not set"))));
                }
            }
            // check node name (jcr qualified name as contained in the path)
            String qualifiedName = Text.getName(nodeContext.getNodePath());
            if (!INDEX_DEFINITION_NAME_PATTERN.matcher(qualifiedName).matches()) {
                messages.add(new ValidationMessage(defaultSeverity,
                        String.format(VIOLATION_MESSAGE_INVALID_INDEX_DEFINITION_NODE_NAME, qualifiedName)));
            }
            return messages;
        }
        return null;
    }

    @Override
    public @Nullable Collection<ValidationMessage> validate(@NotNull PackageProperties properties) {
        if (!properties.getExternalHooks().isEmpty()) {
            if (PackageType.CONTENT.equals(packageType)) {
                return Collections.singleton(new ValidationMessage(defaultSeverity, VIOLATION_MESSAGE_INSTALL_HOOK_IN_MUTABLE_PACKAGE));
            }
        }
        return null;
    }

}
