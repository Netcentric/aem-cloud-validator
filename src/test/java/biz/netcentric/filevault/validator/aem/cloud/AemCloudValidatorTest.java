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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AemCloudValidatorTest {

    @Test
    void testIsPackagePathInstalledConditionally() {
        Assertions.assertFalse(AemCloudValidator.isPackagePathInstalledConditionally("author", Paths.get("/etc/package/container/container.zip")));
        Assertions.assertFalse(AemCloudValidator.isPackagePathInstalledConditionally("author", Paths.get("/apps/install.publish/container.zip")));
        Assertions.assertFalse(AemCloudValidator.isPackagePathInstalledConditionally("author", Paths.get("/apps/install/container.author.zip")));
        Assertions.assertTrue(AemCloudValidator.isPackagePathInstalledConditionally("author", Paths.get("/apps/install.author/container.zip")));
        Assertions.assertTrue(AemCloudValidator.isPackagePathInstalledConditionally("author", Paths.get("/apps/install.author/subfolder/container.zip")));
    }
}
