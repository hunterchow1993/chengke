package com.chengke.chengkecrmbackend.modules.auth.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 固化登录模块 controller/application/domain/infrastructure 的依赖方向。
 */
@AnalyzeClasses(packages = "com.chengke.chengkecrmbackend", importOptions = ImportOption.DoNotIncludeTests.class)
class AuthArchitectureTest {

    @ArchTest
    static final ArchRule CONTROLLER_MUST_NOT_USE_INFRASTRUCTURE =
            noClasses().that().resideInAPackage("..auth.controller..")
                    .should().dependOnClassesThat().resideInAPackage("..auth.infrastructure..");

    @ArchTest
    static final ArchRule DOMAIN_MUST_NOT_USE_FRAMEWORKS =
            noClasses().that().resideInAPackage("..auth.domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework..", "org.apache.ibatis..", "org.springframework.data.redis..");
}
