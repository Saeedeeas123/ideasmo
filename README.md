# Template App — ready-to-build on-device starter

Frozen verified config (AGP 8.2.2 / Kotlin 1.9.22 / Compose 2024.02.00 / SDK 35 / buildTools 35.0.1 / JDK 17).
See ../SKILL.md for the full workflow, routing, failure modes and provenance.

Setup (from SKILL.md §3):
1. Copy to /root/projects/<app_name>
2. Rename package com.example.templateapp → com.saeed.<app> (app/build.gradle.kts namespace+applicationId, MainActivity.kt package, AndroidManifest unchanged)
3. Copy wrapper JAR: cp -r /root/projects/air-mouse/gradle/wrapper/* gradle/wrapper/
4. git config user.email/user.name
5. ./gradlew assembleDebug --no-daemon
6. Ship: cp app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/<app>-debug.apk && chmod 644
7. GitHub private repo per SKILL.md §5 (mandatory)