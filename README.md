[![Build Status](https://img.shields.io/github/workflow/status/Netcentric/aem-cloud-validator/maven-cicd)](https://github.com/Netcentric/aem-cloud-validator/actions)
[![License](https://img.shields.io/badge/License-EPL%201.0-red.svg)](https://opensource.org/licenses/EPL-1.0)
[![Maven Central](https://img.shields.io/maven-central/v/biz.netcentric.filevault.validator/aem-cloud-validator)](https://search.maven.org/artifact/biz.netcentric.filevault.validator/aem-cloud-validator)


# Overview

Validates content packages to prevent invalid usage patterns for AEM as a Cloud Service (AEMaaCS) described in [Debugging AEM as a Cloud Service build and deployments](https://experienceleague.adobe.com/docs/experience-manager-learn/cloud-service/debugging/debugging-aem-as-a-cloud-service/build-and-deployment.html?lang=en#build-images) as those might lead to Build or Deployment errors in CloudManager. It is a validator implementation for the [FileVault Validation Module][2] and can be used for example with the [filevault-package-maven-plugin][3].

*This validator only includes checks which are not covered by the [aem-analyser-maven-plugin][aem-analyser-maven-plugin] so it is strongly recommended to also enable the [aem-analyser-maven-plugin][aem-analyser-maven-plugin] in your build.*

# Settings

The following options are supported apart from the default settings mentioned in [FileVault validation][2].

Option | Mandatory | Description | Default Value
--- | --- | --- | ---
`allowVarNodeOutsideContainer` | no | `true` in case `/var` nodes should be allowed in content packages which do not contain other packages (i.e. are no containers). Otherwise `var` nodes are not even allowed in standalone packages. | `true`

# Included Checks

## Prevent using `/var` in content package

Including `/var` in content packages being deployed to publish instances must be prevented, as it causes deployment failures. The system user which takes care of installing the packages on publish (named `sling-distribution-importer`) does not have `write` permission in `/var`. Further details at <https://experienceleague.adobe.com/docs/experience-manager-learn/cloud-service/debugging/debugging-aem-as-a-cloud-service/build-and-deployment.html?lang=en#including-%2Fvar-in-content-package>.

As this restriction technically only affects publish instances it is still valid to have `/var` nodes in author-only containers.

## Prevent using install hooks in mutable content packages

The usage of [install hooks](http://jackrabbit.apache.org/filevault/installhooks.html) is not allowed to the system user which is installing the package on the AEMaaCS publish instances (named `sling-distribution-importer`) and leads to a `PackageException`. Subsequently the deployment will fail as the exception on publish will block the replication queue on author. Further details at [JCRVLT-427](https://issues.apache.org/jira/browse/JCRVLT-427). As AEMaaCS currently (version 2021.1.4738.20210107T143101Z) still ships with the old FileVault 3.4.0, you cannot circumvent this limitation with OSGi configuration (only possible since FileVault 3.4.6).

Usage of install hooks in immutable content packages works, as those are installed by an [admin user](https://github.com/apache/sling-org-apache-sling-jcr-packageinit/blob/7424e1b1f47758c12b6161e8689d6f9022257ce0/src/main/java/org/apache/sling/jcr/packageinit/impl/ExecutionPlanRepoInitializer.java#L157).

## Enforce Oak index definitions of type `lucene`

Currently only Oak index definitions of type `lucene` are supported in AEMaaCS. Further details in <https://experienceleague.adobe.com/docs/experience-manager-cloud-service/operations/indexing.html?lang=en#changes-in-aem-as-a-cloud-service>.

## Follow naming policy for Oak index definition node names

There is a mandatory naming policy for Oak index definition node names which enforces them to end with `-custom-<version-as-integer>`. The format is used in [`IndexName`](https://github.com/apache/jackrabbit-oak/blob/08c7b20e0676739d9c445b5249c3f71004b6b894/oak-search/src/main/java/org/apache/jackrabbit/oak/plugins/index/search/spi/query/IndexName.java#L36) and allows for upgrades of existing index definitions in blue/green deployments.

Further details in <https://experienceleague.adobe.com/docs/experience-manager-cloud-service/operations/indexing.html?lang=en#changes-in-aem-as-a-cloud-service>.

# Usage with Maven

You can use this validator with the [FileVault Package Maven Plugin][3] in version 1.1.0 or higher like this

```
<plugin>
  <groupId>org.apache.jackrabbit</groupId>
  <artifactId>filevault-package-maven-plugin</artifactId>
  <version>1.1.0</version>
  <configuration>
    <validatorsSettings>
      <netcentric-aem-cloud>
        <options>
          <allowVarNodeOutsideContainer>false</allowVarNodeOutsideContainer><!-- default value is true, as it is allowed to have /var nodes inside author-only container -->
        </options>
      </netcentric-aem-cloud>
    </validatorsSettings>
  </configuration>
  <dependencies>
    <dependency>
      <groupId>biz.netcentric.filevault.validator</groupId>
      <artifactId>aem-cloud-validator</artifactId>
      <version>1.1.0</version>
    </dependency>
  </dependencies>
</plugin>
```


[aem-analyser-maven-plugin]: https://github.com/adobe/aemanalyser-maven-plugin/tree/main/aemanalyser-maven-plugin
[2]: https://jackrabbit.apache.org/filevault/validation.html
[3]: https://jackrabbit.apache.org/filevault-package-maven-plugin/index.html
