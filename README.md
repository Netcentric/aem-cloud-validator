[![Build Status](https://img.shields.io/github/workflow/status/Netcentric/aem-cloud-validator/maven-cicd)](https://github.com/Netcentric/aem-cloud-validator/actions)
[![License](https://img.shields.io/badge/License-EPL%201.0-red.svg)](https://opensource.org/licenses/EPL-1.0)
[![Maven Central](https://img.shields.io/maven-central/v/biz.netcentric.filevault.validator/aem-cloud-validator)](https://search.maven.org/artifact/biz.netcentric.filevault.validator/aem-cloud-validator)


# Overview

Validates content packages to prevent invalid usage patterns for AEM as a Cloud Service (AEMaaCS) described in [Debugging AEM as a Cloud Service build and deployments](https://experienceleague.adobe.com/docs/experience-manager-learn/cloud-service/debugging/debugging-aem-as-a-cloud-service/build-and-deployment.html?lang=en#build-images) as those might lead to Build or Deployment errors in CloudManager. It is a validator implementation for the [FileVault Validation Module][2] and can be used for example with the [filevault-package-maven-plugin][3].

*This validator only includes checks which are not covered by the [aemanalyser-maven-plugin][aemanalyser-maven-plugin] so it is strongly recommended to also enable the [aemanalyser-maven-plugin][aemanalyser-maven-plugin] in your build.*

# Settings

The following options are supported apart from the default settings mentioned in [FileVault validation][2].

Option | Mandatory | Description | Default Value | Since Version
--- | --- | --- | --- | ---
`allowReadOnlyMutablePathsOutsideContainers` (or `allowVarNodeOutsideContainer` deprecated) | no | `true` means read-only paths (i.e. paths to which the service session used for mutable package installation on publish does not have write permission) should be allowed. Otherwise those won't only be allowed in author-only container packages. | `true` | 1.2.0 
`allowLibsNode` | no | `true` means that `libs` nodes are allowed in content packages. *Only set this to `true` when building packages which are part of the AEM product.* | `false` | 1.2.0

# Included Checks

## Prevent using certain paths in mutable content packages

Including `/var` and `tmp` and some others in content packages being deployed to publish instances must be prevented, as it causes deployment failures. The [system session](https://sling.apache.org/documentation/the-sling-engine/service-authentication.html#slingrepository) which takes care of installing the packages on publish does not have `jcr:write` permission to those locations. Further details at <https://experienceleague.adobe.com/docs/experience-manager-learn/cloud-service/debugging/debugging-aem-as-a-cloud-service/build-and-deployment.html?lang=en#including-%2Fvar-in-content-package>.

As this restriction technically only affects publish instances it is still valid to have those nodes in [author-only containers](https://experienceleague.adobe.com/docs/experience-manager-cloud-service/implementing/developing/aem-project-content-package-structure.html#embeddeds).
As a *temporary workaround* you can also [extend the privileges of the `sling-distribution-importer` user via a custom repoinit configuration](https://helpx.adobe.com/in/experience-manager/kb/cm/cloudmanager-deploy-fails-due-to-sling-distribution-aem.html). Here is the full list of default permissions of the system session extracted from AEM 2021.2.4887.20210204T154817Z.
All the following principals are mapped via the service user mapping for `org.apache.sling.distribution.journal:importer` on publish

Principal | Permissions
--- | ---
`sling-distribution-importer` | allow `jcr:modifyAccessControl,jcr:readAccessControl` on `/content`<br/>allow `jcr:modifyAccessControl,jcr:readAccessControl` on `/conf`<br/>allow `jcr:modifyAccessControl,jcr:readAccessControl` on `/etc`<br/>allow `jcr:nodeTypeDefinitionManagement,rep:privilegeManagement` on `:repository`
`sling-distribution` | allow `jcr:read,rep:write` on `/var/sling/distribution`
`content-writer-service` | allow `jcr:read,rep:write,jcr:versionManagement` on `/content`
`repository-reader-service` | allow `jcr:read` on `/`
`version-manager-service` | allow `jcr:read,rep:write,jcr:versionManagement` on `/conf`<br/>allow `jcr:read,rep:write,jcr:versionManagement` on `/etc`
`group-administration-service` | allow `jcr:all` on `/home/groups`
`user-administration-service` | allow `jcr:all` on `/home/users`
`namespace-mgmt-service` | allow `jcr:namespaceManagement` on `:repository`


## Prevent using `/libs` in content package

Changes below `/libs` may be overwritten by AEM product upgrades (applied regularly). Further details at <https://experienceleague.adobe.com/docs/experience-manager-cloud-service/implementing/developing/full-stack/overlays.html?lang=en#developing>. Instead put overlays in `/apps`.

## Prevent using install hooks in content packages

The usage of [install hooks](http://jackrabbit.apache.org/filevault/installhooks.html) is not allowed to the system user which is installing the package on the AEMaaCS publish instances (named `sling-distribution-importer`) and leads to a `PackageException`. The code for that can be found in [ContentPackageExtractor](https://github.com/apache/sling-org-apache-sling-distribution-journal/blob/ba075183c374a09b86ca6fa4755a05b26e74866d/src/main/java/org/apache/sling/distribution/journal/bookkeeper/ContentPackageExtractor.java#L93). Subsequently the deployment will fail as the exception on publish will block the replication queue on author. Further details at [JCRVLT-427](https://issues.apache.org/jira/browse/JCRVLT-427). As AEMaaCS currently (version 2021.1.4738.20210107T143101Z) still ships with the old FileVault 3.4.0, you cannot circumvent this limitation with OSGi configuration (only possible since FileVault 3.4.6).

Usage of install hooks in immutable content packages does not work either as the transformation of the [cp2fm converter](https://github.com/apache/sling-org-apache-sling-feature-cpconverter) strips those out in transformed packages. Further details in [SLING-10205](https://issues.apache.org/jira/browse/SLING-10205).

## Prevent using mutable content in "mixed" content packages

Content packages of type `mixed` are allowed to have both mutable and immutable nodes. AEMaaCS will only ever install the immutable part of it. The mutable part won't be installed as that cannot be successful (due to missing write access at the time of installation).
Further details at <https://experienceleague.adobe.com/docs/experience-manager-cloud-service/implementing/deploying/overview.html?lang=en#deploying-content-packages-via-cloud-manager-and-package-manager>.

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


[aemanalyser-maven-plugin]: https://github.com/adobe/aemanalyser-maven-plugin/tree/main/aemanalyser-maven-plugin
[2]: https://jackrabbit.apache.org/filevault/validation.html
[3]: https://jackrabbit.apache.org/filevault-package-maven-plugin/index.html
