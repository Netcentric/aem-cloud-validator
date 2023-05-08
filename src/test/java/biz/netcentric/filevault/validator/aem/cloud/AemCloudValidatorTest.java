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
import java.util.Collection;
import java.util.Optional;

import org.apache.jackrabbit.vault.packaging.PackageType;
import org.apache.jackrabbit.vault.validation.spi.ValidationMessage;
import org.apache.jackrabbit.vault.validation.spi.ValidationMessageSeverity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static biz.netcentric.filevault.validator.aem.cloud.AemCloudValidator.VIOLATION_MESSAGE_INSTALL_HOOK_IN_MUTABLE_PACKAGE;

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
        Assertions.assertTrue(messages.stream().anyMatch(message -> message.getMessage().equals(VIOLATION_MESSAGE_INSTALL_HOOK_IN_MUTABLE_PACKAGE)));

        validator = new AemCloudValidator(true, false, true, PackageType.CONTENT, null, ValidationMessageSeverity.ERROR);
        messages = new ArrayList<>();
        Optional.ofNullable(validator.validateMetaInfPath(Paths.get("vault/hooks/install-hook.jar"))).ifPresent(messages::addAll);
        Optional.ofNullable(validator.done()).ifPresent(messages::addAll);
        Assertions.assertTrue(messages.isEmpty());
    }

}
