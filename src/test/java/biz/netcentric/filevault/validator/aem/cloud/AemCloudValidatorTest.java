package biz.netcentric.filevault.validator.aem.cloud;

import java.nio.file.Paths;
import java.util.Collection;

import org.apache.jackrabbit.vault.packaging.PackageType;
import org.apache.jackrabbit.vault.validation.spi.ValidationMessage;
import org.apache.jackrabbit.vault.validation.spi.ValidationMessageSeverity;
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
        AemCloudValidator validator = new AemCloudValidator(false, false, PackageType.CONTENT, null, ValidationMessageSeverity.ERROR);
        Collection<ValidationMessage> messages = validator.validate("/var/subnode");
        Assertions.assertFalse(messages.isEmpty());
    }

    @Test
    void testAllowReadOnlyMutablePaths(){
        AemCloudValidator validator = new AemCloudValidator(true, false, PackageType.CONTENT, null, ValidationMessageSeverity.ERROR);
        Collection<ValidationMessage> messages = validator.validate("/var/subnode");
        Assertions.assertTrue(messages.isEmpty());
    }

}
