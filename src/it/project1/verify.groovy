String buildLog = new File(basedir, 'build.log').text

assert buildLog.contains("[WARNING] ValidationViolation: \"netcentric-aem-cloud: Nodes below '/libs' may be overwritten by future product upgrades. Rather use '/apps'. Further details at https://experienceleague.adobe.com/docs/experience-manager-cloud-service/implementing/developing/full-stack/overlays.html?lang=en#developing\", filePath=jcr_root${File.separator}libs${File.separator}cq${File.separator}test${File.separator}test.jsp, nodePath=/libs/cq/test/test.jsp") : 'libs violation not found'
assert buildLog.contains("[WARNING] ValidationViolation: \"netcentric-aem-cloud: Using mutable nodes in this repository location is only allowed in author-specific packages as it is not writable by the underlying service user on a publish instance. Consider to use repoinit scripts instead or move that content to another location. Further details at https://experienceleague.adobe.com/docs/experience-manager-learn/cloud-service/debugging/debugging-aem-as-a-cloud-service/build-and-deployment.html?lang=en#including-%2Fvar-in-content-package\", filePath=jcr_root${File.separator}var${File.separator}example${File.separator}test.txt, nodePath=/var/example/test.txt") : 'read only path in mutable package not detected'
assert buildLog.contains("[WARNING] ValidationViolation: \"netcentric-aem-cloud: Using install hooks in mutable content packages leads to deployment failures as the underlying service user on the publish does not have the right to execute those.\", filePath=META-INF${File.separator}vault${File.separator}hooks${File.separator}vault-hook-example-3.0.0.jar") : 'internal hook violation not found'
assert buildLog.contains("[WARNING] ValidationViolation: \"netcentric-aem-cloud: Using install hooks in mutable content packages leads to deployment failures as the underlying service user on the publish does not have the right to execute those.\", filePath=META-INF${File.separator}vault${File.separator}properties") : 'external hook violation not found'
assert buildLog.contains("[WARNING] ValidationViolation: \"netcentric-aem-cloud: Mutable nodes in mixed package types are not installed!\", filePath=jcr_root${File.separator}content${File.separator}example${File.separator}test.txt, nodePath=/content/example/test.txt") : 'mutable content in mixed package not found'
