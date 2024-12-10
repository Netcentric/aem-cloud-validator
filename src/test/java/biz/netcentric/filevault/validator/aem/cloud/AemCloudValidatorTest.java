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

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.vault.packaging.PackageType;
import org.apache.jackrabbit.vault.util.DocViewNode2;
import org.apache.jackrabbit.vault.util.DocViewProperty2;
import org.apache.jackrabbit.vault.validation.spi.NodeContext;
import org.apache.jackrabbit.vault.validation.spi.ValidationMessage;
import org.apache.jackrabbit.vault.validation.spi.ValidationMessageSeverity;
import org.apache.jackrabbit.vault.validation.spi.util.NodeContextImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AemCloudValidatorTest {

    @Test
    void testIsPackagePathInstalledConditionally() {
        Assertions.assertFalse(AemCloudValidator.isPackagePathInstalledConditionally("author", Paths.get("/etc/package/container/container.zip")));
        Assertions.assertFalse(AemCloudValidator.isPackagePathInstalledConditionally("author", Paths.get("/apps/install.publish/container.zip")));
        Assertions.assertFalse(AemCloudValidator.isPackagePathInstalledConditionally("author", Paths.get("/apps/install/container.author.zip")));
        Assertions.assertTrue(AemCloudValidator.isPackagePathInstalledConditionally("author", Paths.get("/apps/install.author/container.zip")));
        Assertions.assertTrue(AemCloudValidator.isPackagePathInstalledConditionally("author", Paths.get("/apps/install.author/subfolder/container.zip")));
    }

    @Test
    void testIsMutablePath() {
        Assertions.assertTrue(AemCloudValidator.isMutablePath("/"));
        Assertions.assertTrue(AemCloudValidator.isMutablePath("/conf"));
        Assertions.assertTrue(AemCloudValidator.isMutablePath("/conf/test"));
        Assertions.assertFalse(AemCloudValidator.isMutablePath("/libs"));
        Assertions.assertFalse(AemCloudValidator.isMutablePath("/libs/test"));
    }

    @Test
    void testIsPathWritableByDistributionJournalImporter() {
        Assertions.assertTrue(AemCloudValidator.isPathWritableByDistributionJournalImporter("/content"));
        Assertions.assertTrue(AemCloudValidator.isPathWritableByDistributionJournalImporter("/content/test"));
        Assertions.assertFalse(AemCloudValidator.isPathWritableByDistributionJournalImporter("/tmp"));
        Assertions.assertFalse(AemCloudValidator.isPathWritableByDistributionJournalImporter("/var"));
        Assertions.assertFalse(AemCloudValidator.isPathWritableByDistributionJournalImporter("/var/subnode/myfile"));
    }

    @Test
    void testMutablePaths(){
        AemCloudValidator validator = new AemCloudValidator(false, false, false, PackageType.CONTENT, null, ValidationMessageSeverity.ERROR);
        Collection<ValidationMessage> messages = validator.validate("/var/subnode");
        Assertions.assertFalse(messages.isEmpty());
    }

    @Test
    void testAllowReadOnlyMutablePaths(){
        AemCloudValidator validator = new AemCloudValidator(true, false, false, PackageType.CONTENT, null, ValidationMessageSeverity.ERROR);
        Collection<ValidationMessage> messages = validator.validate("/var/subnode");
        Assertions.assertTrue(messages.isEmpty());
    }

    @Test
    void testAllowHooksInMutableContent() {
        AemCloudValidator validator = new AemCloudValidator(true, false, false, PackageType.CONTENT, null, ValidationMessageSeverity.ERROR);
        Collection<ValidationMessage> messages = new ArrayList<>();
        Optional.ofNullable(validator.validateMetaInfPath(Paths.get("vault/hooks/install-hook.jar"))).ifPresent(messages::addAll);
        Optional.ofNullable(validator.done()).ifPresent(messages::addAll);
        Assertions.assertTrue(messages.stream().anyMatch(message -> message.getMessage().equals(AemCloudValidator.VIOLATION_MESSAGE_INSTALL_HOOK_IN_MUTABLE_PACKAGE)));

        validator = new AemCloudValidator(true, false, true, PackageType.CONTENT, null, ValidationMessageSeverity.ERROR);
        messages = new ArrayList<>();
        Optional.ofNullable(validator.validateMetaInfPath(Paths.get("vault/hooks/install-hook.jar"))).ifPresent(messages::addAll);
        Optional.ofNullable(validator.done()).ifPresent(messages::addAll);
        Assertions.assertTrue(messages.isEmpty());
    }

    @Test
    void testValidIndexDefinitions() {
        AemCloudValidator validator = new AemCloudValidator(true, false, false, PackageType.CONTENT, null, ValidationMessageSeverity.ERROR);
        Collection<ValidationMessage> messages = new ArrayList<>();
        List<DocViewProperty2> properties = Arrays.asList(
                new DocViewProperty2(NameConstants.JCR_PRIMARYTYPE, "oak:QueryIndexDefinition"),
                new DocViewProperty2(NameFactoryImpl.getInstance().create(Name.NS_DEFAULT_URI, "type"), "lucene"));
        // valid lucene index definition
        NodeContext context = new NodeContextImpl("/oak:index/prefix.myindex-1-custom-1", Paths.get("_oak_index/test"),Paths.get("./jcr_root"));
        DocViewNode2 node = new DocViewNode2(NameConstants.JCR_ROOT, properties);
        Optional.ofNullable(validator.validate(node, context, true)).ifPresent(messages::addAll);
        Assertions.assertTrue(messages.isEmpty());
        context = new NodeContextImpl("/oak:index/productindex-1-custom-1", Paths.get("_oak_index/test"),Paths.get("./jcr_root"));
        Optional.ofNullable(validator.validate(node, context, true)).ifPresent(messages::addAll);
        Assertions.assertTrue(messages.isEmpty());
    }

    @Test
    void testInvalidLuceneIndexDefinitions() {
        AemCloudValidator validator = new AemCloudValidator(true, false, false, PackageType.CONTENT, null, ValidationMessageSeverity.ERROR);
        Collection<ValidationMessage> messages = new ArrayList<>();
        List<DocViewProperty2> properties = Arrays.asList(
                new DocViewProperty2(NameConstants.JCR_PRIMARYTYPE, "oak:QueryIndexDefinition"),
                new DocViewProperty2(NameFactoryImpl.getInstance().create(Name.NS_DEFAULT_URI, "type"), "lucene"));
        NodeContext context = new NodeContextImpl("/oak:index/myindex", Paths.get("_oak_index/test"),Paths.get("./jcr_root"));
        DocViewNode2 node = new DocViewNode2(NameConstants.JCR_ROOT, properties);
        Optional.ofNullable(validator.validate(node, context, true)).ifPresent(messages::addAll);
        assertEquals(1, messages.size());
        assertEquals(String.format(AemCloudValidator.VIOLATION_MESSAGE_INVALID_INDEX_DEFINITION_NODE_NAME, "myindex"), messages.iterator().next().getMessage());
    }

    @Test
    void testInvalidPropertyIndexDefinition() {
        AemCloudValidator validator = new AemCloudValidator(true, false, false, PackageType.CONTENT, null, ValidationMessageSeverity.ERROR);
        Collection<ValidationMessage> messages = new ArrayList<>();
        List<DocViewProperty2> properties =  Arrays.asList(
                new DocViewProperty2(NameConstants.JCR_PRIMARYTYPE, "oak:QueryIndexDefinition"),
                new DocViewProperty2(NameFactoryImpl.getInstance().create(Name.NS_DEFAULT_URI, "type"), "property"));
        // invalid property index definition
        DocViewNode2 node = new DocViewNode2(NameConstants.JCR_ROOT, properties);
        NodeContext context = new NodeContextImpl("/oak:index/prefix.myindex-1-custom-1", Paths.get("_oak_index/test"),Paths.get("./jcr_root"));
        Optional.ofNullable(validator.validate(node, context, true)).ifPresent(messages::addAll);
        assertEquals(1, messages.size());
        assertEquals(String.format(AemCloudValidator.VIOLATION_MESSAGE_NON_LUCENE_TYPE_INDEX_DEFINITION, "property"), messages.iterator().next().getMessage());
    }
}
