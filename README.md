[![Build Status](https://img.shields.io/github/workflow/status/Netcentric/aem-cloud-validator/maven-cicd)](https://github.com/Netcentric/aem-cloud-validator/actions)
[![License](https://img.shields.io/badge/License-EPL%201.0-red.svg)](https://opensource.org/licenses/EPL-1.0)
[![Maven Central](https://img.shields.io/maven-central/v/biz.netcentric.filevault.validator/aem-cloud-validator)](https://search.maven.org/artifact/biz.netcentric.filevault.validator/aem-cloud-validator)


# Overview

Validates content packages for invalid usage patterns described in [Debugging AEM as a Cloud Service build and deployments](https://experienceleague.adobe.com/docs/experience-manager-learn/cloud-service/debugging/debugging-aem-as-a-cloud-service/build-and-deployment.html?lang=en#build-images). It is a validator implementation for the [FileVault Validation Module][2] and can be used for example with the [filevault-package-maven-plugin][3].

*This validator only includes checks which are not covered by the [aem-analyser-maven-plugin][aem-analyser-maven-plugin] so it is strongly recommended to also enable the [aem-analyser-maven-plugin][aem-analyser-maven-plugin] in your build.*

# Settings

The following options are supported apart from the default settings mentioned in [FileVault validation][2].

Option | Mandatory | Description
--- | --- | ---
allowVarNodeOutsideContainer | no | `true` in case `/var` nodes should be allowed in content packages which do not contain other packages (i.e. are no containers). Otherwise `var` nodes are not even allowed in standalone packages. Default value `true`.

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
