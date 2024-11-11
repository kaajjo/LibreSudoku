package com.kaajjo.libresudoku.core.update

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import com.kaajjo.libresudoku.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import java.io.File

object UpdateUtil {
    private const val OWNER = "kaajjo"
    private const val REPO = "LibreSudoku"
    private const val GITHUB_URL = "https://github.com/kaajjo/LibreSudoku"

    private val requestLatestRelease =
        Request.Builder()
            .url("https://api.github.com/repos/$OWNER/$REPO/releases/latest")
            .build()

    private val requestReleases =
        Request.Builder()
            .url("https://api.github.com/repos/$OWNER/$REPO/releases")
            .build()

    private val okHttpClient = OkHttpClient()

    private val jsonFormat = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private fun getLatestRelease(allowBetas: Boolean): Release {
        okHttpClient
            .newCall(requestReleases)
            .execute()
            .body.use { releasesResponse ->
                val releases = jsonFormat.decodeFromString<List<Release>>(releasesResponse.string())
                return releases
                    .filter {
                        Log.d("UpdateUtil:filter", it.name?.toString() ?: "null")
                        if (!allowBetas && it.name != null)
                            it.name.toVersion() is Version.Stable
                        else
                            true
                    }
                    .maxBy { it.name?.toVersion()?.toVersionNumber() ?: 0 }
                    .also {
                        Log.d("UpdateUtil:maxBy", it.name?.toVersion()?.toVersionString() ?: "null")
                    }
            }
    }

    fun checkForUpdate(allowBetas: Boolean = true): Release? {
        val currentVersion = BuildConfig.VERSION_NAME.toVersion()

        val latestRelease = getLatestRelease(allowBetas)
        val latestVersion = latestRelease.name?.toVersion() ?: Version.Stable(0, 0, 0)
        return if (latestVersion > currentVersion) {
            latestRelease
        } else {
            null
        }
    }

    suspend fun downloadApk(
        context: Context,
        release: Release,
    ): Flow<DownloadStatus> =
        withContext(Dispatchers.IO) {
            val downloadUrl = release.assets
                ?.first()
                ?.browserDownloadUrl ?: return@withContext emptyFlow()

            val request = Request.Builder()
                .url(downloadUrl)
                .build()

            try {
                val response = okHttpClient.newCall(request).execute()
                val responseBody = response.body
                return@withContext responseBody.downloadFileWithProgress(context.getLatestApk())
            } catch (e: Exception) {
                Log.e("UpdateUtil", "Failed to download the apk", e)
            }
            emptyFlow()
        }

    private fun ResponseBody.downloadFileWithProgress(saveFile: File): Flow<DownloadStatus> =
        flow {
            emit(DownloadStatus.Progress(0))

            var deleteFile = true

            try {
                byteStream().use { inputStream ->
                    saveFile.outputStream().use { outputStream ->
                        val totalBytes = contentLength()
                        val data = ByteArray(8_192)
                        var progressBytes = 0L

                        while (true) {
                            val bytes = inputStream.read(data)

                            if (bytes == -1) {
                                break
                            }

                            outputStream.channel
                            outputStream.write(data, 0, bytes)
                            progressBytes += bytes
                            emit(
                                DownloadStatus.Progress(
                                    percent = ((progressBytes * 100) / totalBytes).toInt()
                                )
                            )
                        }

                        when {
                            progressBytes < totalBytes -> throw Exception("missing bytes")
                            progressBytes > totalBytes -> throw Exception("too many bytes")
                            else -> deleteFile = false
                        }
                    }
                }

                emit(DownloadStatus.Finished(saveFile))
            } finally {
                if (deleteFile) {
                    saveFile.delete()
                }
            }
        }
            .flowOn(Dispatchers.IO)
            .distinctUntilChanged()

    fun installLatestApk(context: Context) =
        context.run {
            kotlin
                .runCatching {
                    val contentUri =
                        FileProvider.getUriForFile(
                            this,
                            "${context.packageName}.provider",
                            getLatestApk()
                        )
                    val intent =
                        Intent(Intent.ACTION_VIEW).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            setDataAndType(contentUri, "application/vnd.android.package-archive")
                        }
                    startActivity(intent)
                }
                .onFailure { throwable: Throwable ->
                    throwable.printStackTrace()
                }
        }
}

@Serializable
data class Release(
    @SerialName("html_url") val htmlUrl: String? = null,
    @SerialName("tag_name") val tagName: String? = null,
    val name: String? = null,
    val draft: Boolean? = null,
    @SerialName("prerelease") val preRelease: Boolean? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("published_at") val publishedAt: String? = null,
    val assets: List<AssetsItem>? = null,
    val body: String? = null
)

@Serializable
data class AssetsItem(
    val name: String? = null,
    @SerialName("content_type") val contentType: String? = null,
    val size: Int? = null,
    @SerialName("download_count") val downloadCount: Int? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("browser_download_url") val browserDownloadUrl: String? = null,
)

sealed class DownloadStatus {
    object NotStarted : DownloadStatus()

    data class Progress(val percent: Int) : DownloadStatus()

    data class Finished(val file: File) : DownloadStatus()
}

fun String.toVersion(): Version {
    this.replace("v", "").run {
        val regex = Regex("""(\d+)\.(\d+)\.(\d+)(?:-(?:alpha|beta)?(\d+))?""")
        val matchResult = regex.matchEntire(this)

        val (major, minor, patch, build) = matchResult!!.destructured
        try {
            major.toInt()
            minor.toInt()
            patch.toInt()
        } catch (e: Exception) {
            Log.e("UpdateUtil", "Failed to parse version name: $this")
        }
        return if (this.contains("beta")) {
            Version.Beta(major.toInt(), minor.toInt(), patch.toInt(), build.toInt())
        } else {
            Version.Stable(major.toInt(), minor.toInt(), patch.toInt())
        }
    }
}

sealed class Version(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val build: Int = 0
) : Comparable<Version> {
    companion object {
        private const val MAJOR = 10_000_000L
        private const val MINOR = 100_000L
        private const val PATCH = 1000L
        private const val BUILD = 10L

        private const val STABLE = 100L
    }

    abstract fun toVersionString(): String
    abstract fun toVersionNumber(): Long

    override fun compareTo(other: Version): Int {
        return this.toVersionNumber().compareTo(other.toVersionNumber())
    }

    class Stable(major: Int, minor: Int, patch: Int) : Version(major, minor, patch) {
        override fun toVersionString(): String {
            return "$major.$minor.$patch"
        }

        override fun toVersionNumber(): Long =
            (major * MAJOR) + (minor * MINOR) + (patch * PATCH) + STABLE
    }

    class Beta(major: Int, minor: Int, patch: Int, build: Int) :
        Version(major, minor, patch, build) {
        override fun toVersionString(): String {
            return "$major.$minor.$patch-beta$build"
        }

        override fun toVersionNumber(): Long =
            (major * MAJOR) + (minor * MINOR) + (patch * PATCH) + (build * BUILD)
    }
}

private fun Context.getLatestApk() = File(getExternalFilesDir("apk"), "latest.apk")
