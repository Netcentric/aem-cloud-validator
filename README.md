![](https://github.com/Netcentric/aem-cloud-validator/workflows/Java%20CI/badge.svg) [![License](https://img.shields.io/badge/License-EPL%201.0-red.svg)](https://opensource.org/licenses/EPL-1.0)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/biz.netcentric.filevault.validator/aem-cloud-validator/badge.svg)](https://search.maven.org/artifact/biz.netcentric.filevault.validator/aem-cloud-validator)

# Overview

Validates content packages for invalid usage patterns described in [Debugging AEM as a Cloud Service](https://experienceleague.adobe.com/docs/experience-manager-learn/cloud-service/debugging/debugging-aem-as-a-cloud-service/build-and-deployment.html?lang=en#build-images). It is a validator implementation for the [FileVault Validation Module][2] and can be used for example with the [filevault-package-maven-plugin][3].

*This validator only includes checks which are not yet covered by the [aem-analyser-maven-plugin][aem-analyser-maven-plugin] so it is strongly recommended to also enable the [aem-analyser-maven-plugin][aem-analyser-maven-plugin] in your build.*

# Included Checks

## Including `/var` in content package

Including `/var` in content packages being deployed to publish instances must be prevented, as it causes deployment failures. Further details at <https://experienceleague.adobe.com/docs/experience-manager-learn/cloud-service/debugging/debugging-aem-as-a-cloud-service/build-and-deployment.html?lang=en#including-%2Fvar-in-content-package>.

# Usage with Maven

You can use this validator with the [FileVault Package Maven Plugin][3] in version 1.1.0 or higher like this

```
<plugin>
  <groupId>org.apache.jackrabbit</groupId>
  <artifactId>filevault-package-maven-plugin</artifactId>
  <version>1.1.0</version>
  <dependencies>
    <dependency>
      <groupId>biz.netcentric.filevault.validator</groupId>
      <artifactId>aem-cloud-validator</artifactId>
      <version>1.0.0</version>
    </dependency>
  </dependencies>
</plugin>
```


[aem-analyser-maven-plugin]: https://github.com/adobe/aemanalyser-maven-plugin/tree/main/aemanalyser-maven-plugin
[2]: https://jackrabbit.apache.org/filevault/validation.html
[3]: https://jackrabbit.apache.org/filevault-package-maven-plugin/index.html
