// Top-level build file where you can add configuration options common to all sub-projects/modules.
@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.kapt) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.androidLibrary) apply false
}
true // Needed to make the Suppress annotation work for the plugins block

//plugins {
//    id 'com.android.application' version '8.0.0-rc01' apply false
//    id 'com.android.library' version '8.0.0-rc01' apply false
//    id 'org.jetbrains.kotlin.android' version '1.8.10' apply false
//    id 'com.google.dagger.hilt.android' version '2.45' apply false
//    id 'com.mikepenz.aboutlibraries.plugin' version "$latestAboutLibsRelease" apply false
//}