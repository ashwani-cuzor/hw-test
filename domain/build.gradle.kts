plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.ksp)
    `java-test-fixtures`
}

dependencies {
    implementation(project(":utils"))
    implementation(libs.arrow.core)
    implementation(libs.dagger)
    implementation(libs.kotlinxCoroutines)
    implementation(libs.kotlinxDatetime)
    ksp(libs.dagger.compiler)
    testImplementation(libs.kotlinxCoroutinesTest)
    testFixturesImplementation(libs.arrow.core)
}
