// 순수 Kotlin/JVM 모듈. android.* 를 쓰지 않는다 — 나중에 KMP commonMain 으로 옮긴다.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    // 모델이 LocalDate 를 드러내므로 api 로 노출한다
    api(libs.kotlinx.datetime)
    testImplementation(libs.junit)
}
