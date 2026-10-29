package org.telegram.transforms

import org.gradle.api.artifacts.transform.CacheableTransform
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.HexFormat
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@CacheableTransform
abstract class CoreFingerprintTransform : TransformAction<CoreFingerprintTransform.Parameters> {
    interface Parameters : TransformParameters {
        @get:InputFile
        @get:PathSensitive(PathSensitivity.NONE)
        val legacyCoreAar: RegularFileProperty
    }

    @get:InputArtifact
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val stockBytes = inputArtifact.get().asFile.readBytes()
        val donorBytes = parameters.legacyCoreAar.get().asFile.readBytes()
        check(sha256(stockBytes) == STOCK_SHA256) {
            "Core stock AAR changed; re-audit the 1.19.1 fingerprint transform"
        }
        check(sha256(donorBytes) == DONOR_SHA256) {
            "Core donor AAR changed; expected the audited official 1.17.0 artifact"
        }

        val stockAar = readZip(stockBytes.inputStream())
        val donorAar = readZip(donorBytes.inputStream())
        val stockClasses = readZip(stockAar.getValue("classes.jar").inputStream())
        val donorClasses = readZip(donorAar.getValue("classes.jar").inputStream())
        check(stockClasses.getValue(VERSION_ENTRY).toString(Charsets.UTF_8).trim() == "1.19.1")
        check(donorClasses.getValue(VERSION_ENTRY).toString(Charsets.UTF_8).trim() == "1.17.0")
        val stockFamily = stockClasses.keys.filter { it.startsWith(PREFIX) }.toSet()
        val donorFamily = donorClasses.keys.filter { it.startsWith(PREFIX) }.toSet()
        check(stockFamily == STOCK_FAMILY) { "Unexpected Core stock fingerprint family" }
        check(donorFamily == DONOR_FAMILY) { "Unexpected Core donor fingerprint family" }

        stockFamily.forEach { stockClasses.remove(it) }
        DONOR_FAMILY.forEach { stockClasses[it] = donorClasses.getValue(it) }
        val patchedClasses = ByteArrayOutputStream()
        writeZip(stockClasses, patchedClasses)
        stockAar["classes.jar"] = patchedClasses.toByteArray()
        writeZip(
            stockAar,
            outputs.file("core-1.19.1-native-fingerprint.aar").outputStream().buffered(),
        )
    }

    private fun readZip(input: InputStream): LinkedHashMap<String, ByteArray> {
        val entries = linkedMapOf<String, ByteArray>()
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                check(entries.put(entry.name, zip.readBytes()) == null) {
                    "Duplicate ZIP entry: ${entry.name}"
                }
            }
        }
        return entries
    }

    private fun writeZip(entries: Map<String, ByteArray>, output: OutputStream) {
        ZipOutputStream(output).use { zip ->
            entries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name).apply { time = 0 })
                zip.write(bytes)
                zip.closeEntry()
            }
        }
    }

    private fun sha256(bytes: ByteArray): String =
        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))

    private companion object {
        const val STOCK_SHA256 = "08108ea780205bd54603088a62a6dd5378f63bd25542674be24cd4cdb8191a09"
        const val DONOR_SHA256 = "e1bd22bbcf5cee3a992dedb7d2d3c3b31c9f761c01e56d00ea443c8aaa981182"
        const val PREFIX = "androidx/core/hardware/fingerprint/FingerprintManagerCompat"
        const val VERSION_ENTRY = "META-INF/androidx.core_core.version"
        val DONOR_FAMILY = linkedSetOf(
            "$PREFIX.class",
            "$PREFIX\$1.class",
            "$PREFIX\$Api23Impl.class",
            "$PREFIX\$AuthenticationCallback.class",
            "$PREFIX\$AuthenticationResult.class",
            "$PREFIX\$CryptoObject.class",
        )
        val STOCK_FAMILY = DONOR_FAMILY - setOf("$PREFIX\$1.class", "$PREFIX\$Api23Impl.class")
    }
}
