package com.sr.openbyd.patcher

import android.util.Log
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.Security
import java.security.cert.X509Certificate
import java.util.Date
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.security.auth.x500.X500Principal

object ApkPatcher {
    private const val TAG = "ApkPatcher"
    private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"

    init {
        // Initialize BouncyCastle Provider
        if (Security.getProvider("BC") == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    /**
     * Patches an installed APK to support native DiLink 6 split screen.
     * Extracts AndroidManifest.xml, modifies AXML binary elements, places it back,
     * and signs the resulting APK using BouncyCastle.
     */
    data class SigningKeys(val privateKey: PrivateKey, val certificate: X509Certificate)

    private fun generateSigningKeys(): SigningKeys {
        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(2048)
        val keyPair = keyGen.generateKeyPair()
        
        val issuer = X500Principal("CN=OpenBYD, O=OpenBYD Project, C=US")
        val serial = BigInteger.valueOf(System.currentTimeMillis())
        val notBefore = Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L)
        val notAfter = Date(System.currentTimeMillis() + 3650 * 24 * 60 * 60 * 1000L) // 10 years
        
        val certBuilder = JcaX509v3CertificateBuilder(
            issuer,
            serial,
            notBefore,
            notAfter,
            issuer,
            keyPair.public
        )
        
        val signer = JcaContentSignerBuilder("SHA256withRSA").build(keyPair.private)
        val holder = certBuilder.build(signer)
        val cert = JcaX509CertificateConverter().getCertificate(holder)
        
        return SigningKeys(keyPair.private, cert)
    }

    fun patchApk(
        inputApk: File,
        outputApk: File,
        splitApks: List<File> = emptyList(),
        outputSplitApks: List<File> = emptyList(),
        logCallback: (String) -> Unit
    ) {
        try {
            logCallback("Generating cryptographic keys...")
            val keys = generateSigningKeys()
            
            logCallback("Patching base APK: ${inputApk.name}...")
            patchOrAlignApk(inputApk, outputApk, keys.privateKey, keys.certificate, shouldPatchManifest = true, logCallback)
            
            if (splitApks.isNotEmpty() && splitApks.size == outputSplitApks.size) {
                for (i in splitApks.indices) {
                    val splitApk = splitApks[i]
                    val outSplitApk = outputSplitApks[i]
                    logCallback("Aligning and re-signing split APK: ${splitApk.name}...")
                    patchOrAlignApk(splitApk, outSplitApk, keys.privateKey, keys.certificate, shouldPatchManifest = false, logCallback)
                }
            }
            
            logCallback("All APKs patched, aligned, and signed successfully!")
        } catch (e: Exception) {
            Log.e(TAG, "Full patch pipeline failed", e)
            logCallback("ERROR during patching: ${e.localizedMessage}")
            throw e
        }
    }

    private fun patchOrAlignApk(
        inputApk: File,
        outputApk: File,
        privateKey: PrivateKey,
        cert: X509Certificate,
        shouldPatchManifest: Boolean,
        logCallback: (String) -> Unit
    ) {
        val tempApk = File(outputApk.parentFile, "temp_${inputApk.name}")
        if (tempApk.exists()) tempApk.delete()
        if (outputApk.exists()) outputApk.delete()

        try {
            ZipFile(inputApk).use { zip ->
                val cos = CountingOutputStream(FileOutputStream(tempApk).buffered())
                ZipOutputStream(cos).use { zos ->
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        if (entry.name == "AndroidManifest.xml" && shouldPatchManifest) {
                            logCallback("Extracting and patching...")
                            val manifestBytes = zip.getInputStream(entry).use { it.readBytes() }
                            val patchedManifest = patchManifestBytes(manifestBytes, logCallback)
                            
                            val newEntry = ZipEntry("AndroidManifest.xml")
                            zos.putNextEntry(newEntry)
                            zos.write(patchedManifest)
                            zos.closeEntry()
                        } else if (!entry.name.startsWith("META-INF/")) {
                            val newEntry = ZipEntry(entry.name).apply {
                                method = entry.method
                                if (entry.method == ZipEntry.STORED) {
                                    size = entry.size
                                    compressedSize = entry.compressedSize
                                    crc = entry.crc
                                    
                                    val alignment = if (entry.name.endsWith(".so")) 4096 else 4
                                    val currentOffset = cos.getCount()
                                    val nameBytesSize = entry.name.toByteArray(Charsets.UTF_8).size
                                    
                                    val dataStartWithoutPadding = currentOffset + 36 + nameBytesSize
                                    val rem = (dataStartWithoutPadding % alignment).toInt()
                                    val paddingSize = if (rem == 0) 0 else (alignment - rem)
                                    
                                    val extraField = createAlignmentExtraField(alignment, paddingSize)
                                    setExtra(extraField)
                                }
                            }
                            zos.putNextEntry(newEntry)
                            zip.getInputStream(entry).use { it.copyTo(zos) }
                            zos.closeEntry()
                        }
                    }
                }
            }

            signApkWithBc(tempApk, outputApk, privateKey, cert)
        } catch (e: Exception) {
            Log.e(TAG, "Patch/Align failed for ${inputApk.name}", e)
            throw e
        } finally {
            if (tempApk.exists()) tempApk.delete()
        }
    }

    /**
     * Parses the binary AXML Manifest and injects BYD_SUPPORT_SPLIT_ACTIVITY metadata
     * and resizeableActivity=true.
     */
    private fun patchManifestBytes(bytes: ByteArray, log: (String) -> Unit): ByteArray {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        
        // 1. Parse File Header
        val fileType = buffer.short
        val headerSize = buffer.short
        val fileSize = buffer.int
        if (fileType.toInt() != 0x0003 && headerSize.toInt() != 0x0008) {
            throw IllegalArgumentException("Invalid AndroidManifest.xml binary format.")
        }

        // 2. Parse Chunks
        val output = ByteArrayOutputStream()
        var stringPoolChunk: ByteArray? = null
        val remainingChunks = mutableListOf<ByteArray>()
        
        var stringPoolOffset = 8
        buffer.position(stringPoolOffset)
        
        val spType = buffer.short
        val spHeaderSize = buffer.short
        val spChunkSize = buffer.int
        
        if (spType.toInt() != 0x0001) {
            throw IllegalArgumentException("Expected String Pool chunk at offset 8.")
        }
        
        buffer.position(stringPoolOffset)
        val spBytes = ByteArray(spChunkSize)
        buffer.get(spBytes)
        stringPoolChunk = spBytes
        
        // Read remaining chunks
        while (buffer.hasRemaining()) {
            val type = buffer.short
            val hSize = buffer.short
            val cSize = buffer.int
            
            buffer.position(buffer.position() - 8)
            val chunkBytes = ByteArray(cSize)
            buffer.get(chunkBytes)
            remainingChunks.add(chunkBytes)
        }

        log("Parsing string pool...")
        // 3. Parse and append to String Pool
        val spBuffer = ByteBuffer.wrap(stringPoolChunk).order(ByteOrder.LITTLE_ENDIAN)
        spBuffer.position(8)
        val stringCount = spBuffer.int
        val styleCount = spBuffer.int
        val flags = spBuffer.int
        val stringsOffset = spBuffer.int
        val stylesOffset = spBuffer.int
        
        val isUtf8 = (flags and 0x00000100) != 0
        
        // Parse original strings
        val strings = mutableListOf<String>()
        for (i in 0 until stringCount) {
            val offset = spBuffer.getInt(28 + i * 4)
            val strPos = stringsOffset + offset
            spBuffer.position(strPos)
            
            if (isUtf8) {
                // UTF-8 length decoding (character count + byte count)
                var len = spBuffer.get().toInt() and 0xFF
                if ((len and 0x80) != 0) {
                    len = ((len and 0x7F) shl 8) or (spBuffer.get().toInt() and 0xFF)
                }
                var bytesLen = spBuffer.get().toInt() and 0xFF
                if ((bytesLen and 0x80) != 0) {
                    bytesLen = ((bytesLen and 0x7F) shl 8) or (spBuffer.get().toInt() and 0xFF)
                }
                val stringBytes = ByteArray(bytesLen)
                spBuffer.get(stringBytes)
                strings.add(String(stringBytes, Charsets.UTF_8))
            } else {
                // UTF-16 length decoding
                var len = spBuffer.short.toInt() and 0xFFFF
                if ((len and 0x8000) != 0) {
                    len = ((len and 0x7FFF) shl 16) or (spBuffer.short.toInt() and 0xFFFF)
                }
                val stringBytes = ByteArray(len * 2)
                spBuffer.get(stringBytes)
                strings.add(String(stringBytes, Charsets.UTF_16LE))
            }
        }

        // Search for namespace or append it
        var nsIndex = strings.indexOf(ANDROID_NS)
        if (nsIndex == -1) {
            nsIndex = strings.size
            strings.add(ANDROID_NS)
        }

        // Add new strings needed for injection only if they don't exist
        val newStrings = mutableListOf<String>()
        fun getOrAddString(str: String): Int {
            val idx = strings.indexOf(str)
            if (idx != -1) return idx
            val newIdx = strings.size + newStrings.size
            newStrings.add(str)
            return newIdx
        }
        
        val metaDataIdx = getOrAddString("meta-data")
        val nameIdx = getOrAddString("name")
        val splitValIdx = getOrAddString("BYD_SUPPORT_SPLIT_ACTIVITY")
        val valueIdx = getOrAddString("value")
        val resizeableIdx = getOrAddString("resizeableActivity")
        val extractNativeLibsIdx = getOrAddString("extractNativeLibs")
        
        val startIndex = strings.size
        strings.addAll(newStrings)

        log("Rebuilding modified string pool...")
        // Re-serialize modified string pool
        val spOut = ByteArrayOutputStream()
        val spOutBuffer = ByteBuffer.allocate(spChunkSize + 4096).order(ByteOrder.LITTLE_ENDIAN)
        
        spOutBuffer.putShort(0x0001.toShort()) // Chunk Type
        spOutBuffer.putShort(28.toShort())     // Header Size
        spOutBuffer.putInt(0)                  // Placeholder for total chunk size
        spOutBuffer.putInt(strings.size)       // New string count
        spOutBuffer.putInt(styleCount)
        spOutBuffer.putInt(flags)
        spOutBuffer.putInt(0)                  // Placeholder for strings offset
        spOutBuffer.putInt(0)                  // Placeholder for styles offset
        
        // Write offsets
        val offsetPos = spOutBuffer.position()
        for (i in 0 until strings.size) {
            spOutBuffer.putInt(0) // Placeholder for string offsets
        }
        
        // Write styles offsets placeholder if styleCount > 0
        val styleOffsetsPos = spOutBuffer.position()
        for (i in 0 until styleCount) {
            spOutBuffer.putInt(0)
        }
        
        // Write string data
        val dataStartPos = spOutBuffer.position()
        val newOffsets = mutableListOf<Int>()
        
        for (str in strings) {
            newOffsets.add(spOutBuffer.position() - dataStartPos)
            if (isUtf8) {
                val strBytes = str.toByteArray(Charsets.UTF_8)
                val len = str.length
                val bytesLen = strBytes.size
                
                // Write character count
                if (len > 0x7F) {
                    spOutBuffer.put(((len ushr 8) or 0x80).toByte())
                    spOutBuffer.put((len and 0xFF).toByte())
                } else {
                    spOutBuffer.put(len.toByte())
                }
                
                // Write byte count
                if (bytesLen > 0x7F) {
                    spOutBuffer.put(((bytesLen ushr 8) or 0x80).toByte())
                    spOutBuffer.put((bytesLen and 0xFF).toByte())
                } else {
                    spOutBuffer.put(bytesLen.toByte())
                }
                
                spOutBuffer.put(strBytes)
                spOutBuffer.put(0.toByte()) // Null terminator
            } else {
                val strBytes = str.toByteArray(Charsets.UTF_16LE)
                val len = str.length
                
                if (len > 0x7FFF) {
                    spOutBuffer.putShort(((len ushr 16) or 0x8000).toShort())
                    spOutBuffer.putShort((len and 0xFFFF).toShort())
                } else {
                    spOutBuffer.putShort(len.toShort())
                }
                
                spOutBuffer.put(strBytes)
                spOutBuffer.putShort(0.toShort()) // Null terminator
            }
        }
        
        // Padding to 4 bytes
        var padding = (4 - (spOutBuffer.position() % 4)) % 4
        for (p in 0 until padding) {
            spOutBuffer.put(0.toByte())
        }
        
        val styleStartPos = spOutBuffer.position()
        // Copy original styles if present
        if (styleCount > 0) {
            val originalStylesOffset = stylesOffset
            val stylesSize = spChunkSize - originalStylesOffset
            spOutBuffer.put(stringPoolChunk, originalStylesOffset, stylesSize)
        }
        
        // Re-write headers
        val totalSpSize = spOutBuffer.position()
        spOutBuffer.putInt(4, totalSpSize) // Chunk Size
        spOutBuffer.putInt(20, dataStartPos) // Strings Offset
        spOutBuffer.putInt(24, if (styleCount > 0) styleStartPos else 0) // Styles Offset
        
        // Write real offsets
        spOutBuffer.position(offsetPos)
        for (off in newOffsets) {
            spOutBuffer.putInt(off)
        }
        
        val spChunkPatched = ByteArray(totalSpSize)
        spOutBuffer.position(0)
        spOutBuffer.get(spChunkPatched)

        log("Modifying chunks to inject elements...")
        // 4. Modify remaining chunks and insert <meta-data> inside <application>
        val patchedChunks = mutableListOf<ByteArray>()
        
        val appIndex = strings.indexOf("application")
        if (appIndex == -1) {
            throw IllegalArgumentException("Could not find '<application>' element in Manifest.")
        }
        
        var injected = false
        for (chunk in remainingChunks) {
            val chType = getShort(chunk, 0).toInt() and 0xFFFF
            val chSize = getInt(chunk, 4)
            
            if (chType == 0x0180) { // Resource Map
                val rebuiltMap = rebuildResourceMap(chunk, strings.size, startIndex, nameIdx, valueIdx, resizeableIdx, extractNativeLibsIdx)
                patchedChunks.add(rebuiltMap)
                continue
            }
            
            if (chType == 0x0102) { // Start Element
                val nameIndex = getInt(chunk, 20)
                if (nameIndex == appIndex) {
                    // This is <application> start tag! Let's inject attributes
                    val patchedAppChunk = injectApplicationAttributes(chunk, nsIndex, resizeableIdx, extractNativeLibsIdx)
                    patchedChunks.add(patchedAppChunk)
                    
                    // Immediately inject <meta-data android:name="BYD_SUPPORT_SPLIT_ACTIVITY" android:value="1"/>
                    log("Injecting metadata...")
                    val metadataChunk = buildMetadataChunk(nsIndex, metaDataIdx, nameIdx, splitValIdx, valueIdx)
                    patchedChunks.add(metadataChunk)
                    
                    injected = true
                    continue
                }
            }
            patchedChunks.add(chunk)
        }
        
        if (!injected) {
            throw IllegalArgumentException("Failed to inject split-screen metadata into '<application>' tag.")
        }

        // 5. Re-assemble Axml Binary File
        val resultStream = ByteArrayOutputStream()
        val fileHeader = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
        
        // Total final file size calculation
        var finalSize = 8 + spChunkPatched.size
        for (ch in patchedChunks) {
            finalSize += ch.size
        }
        
        fileHeader.putShort(0x0003.toShort()) // File Type Magic
        fileHeader.putShort(0x0008.toShort()) // Header Size
        fileHeader.putInt(finalSize)
        
        resultStream.write(fileHeader.array())
        resultStream.write(spChunkPatched)
        for (ch in patchedChunks) {
            resultStream.write(ch)
        }
        
        return resultStream.toByteArray()
    }

    /**
     * Injects or overrides 'android:resizeableActivity="true"' into <application> tag
     */
    private fun injectApplicationAttributes(chunk: ByteArray, nsIdx: Int, resizeableIdx: Int, extractNativeLibsIdx: Int): ByteArray {
        val buffer = ByteBuffer.wrap(chunk).order(ByteOrder.LITTLE_ENDIAN)
        val type = buffer.short
        val headerSize = buffer.short
        val chunkSize = buffer.int
        val line = buffer.int
        val comment = buffer.int
        val ns = buffer.int
        val name = buffer.int
        val attrStart = buffer.short.toInt() and 0xFFFF
        val attrSize = buffer.short.toInt() and 0xFFFF
        var attrCount = buffer.short.toInt() and 0xFFFF
        val classAttr = buffer.short
        val idAttr = buffer.short
        val styleAttr = buffer.short

        // Extract original attributes safely
        val attributesList = mutableListOf<ByteArray>()
        for (i in 0 until attrCount) {
            val attrBytes = ByteArray(20)
            System.arraycopy(chunk, headerSize.toInt() + attrStart + i * 20, attrBytes, 0, 20)
            attributesList.add(attrBytes)
        }

        // 1. Process resizeableActivity
        var resizeableExists = false
        for (attr in attributesList) {
            val attrBuf = ByteBuffer.wrap(attr).order(ByteOrder.LITTLE_ENDIAN)
            val aNs = attrBuf.getInt(0)
            val aName = attrBuf.getInt(4)
            if (aNs == nsIdx && aName == resizeableIdx) {
                resizeableExists = true
                attrBuf.putInt(8, -1) // raw value index (-1)
                attrBuf.putShort(12, 8.toShort()) // size
                attrBuf.put(14, 0.toByte()) // res
                attrBuf.put(15, 0x12.toByte()) // Boolean DataType
                attrBuf.putInt(16, 1) // Data (1 = true)
                break
            }
        }

        if (!resizeableExists) {
            val newAttrBytes = ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN)
            newAttrBytes.putInt(nsIdx)         // Namespace URI index
            newAttrBytes.putInt(resizeableIdx) // Name index
            newAttrBytes.putInt(-1)            // Raw Value index (-1 for boolean)
            newAttrBytes.putShort(8.toShort()) // size (8)
            newAttrBytes.put(0.toByte())       // res
            newAttrBytes.put(0x12.toByte())    // Type (Boolean)
            newAttrBytes.putInt(1)             // Value (1 = true)
            attributesList.add(newAttrBytes.array())
        }

        // 2. Process extractNativeLibs
        var extractNativeExists = false
        for (attr in attributesList) {
            val attrBuf = ByteBuffer.wrap(attr).order(ByteOrder.LITTLE_ENDIAN)
            val aNs = attrBuf.getInt(0)
            val aName = attrBuf.getInt(4)
            if (aNs == nsIdx && aName == extractNativeLibsIdx) {
                extractNativeExists = true
                attrBuf.putInt(8, -1) // raw value index (-1)
                attrBuf.putShort(12, 8.toShort()) // size
                attrBuf.put(14, 0.toByte()) // res
                attrBuf.put(15, 0x12.toByte()) // Boolean DataType
                attrBuf.putInt(16, 1) // Data (1 = true)
                break
            }
        }

        if (!extractNativeExists) {
            val newAttrBytes = ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN)
            newAttrBytes.putInt(nsIdx)         // Namespace URI index
            newAttrBytes.putInt(extractNativeLibsIdx) // Name index
            newAttrBytes.putInt(-1)            // Raw Value index (-1 for boolean)
            newAttrBytes.putShort(8.toShort()) // size (8)
            newAttrBytes.put(0.toByte())       // res
            newAttrBytes.put(0x12.toByte())    // Type (Boolean)
            newAttrBytes.putInt(1)             // Value (1 = true)
            attributesList.add(newAttrBytes.array())
        }

        // Rebuild attributes array
        val finalAttributes = ByteArray(attributesList.size * 20)
        for (i in 0 until attributesList.size) {
            System.arraycopy(attributesList[i], 0, finalAttributes, i * 20, 20)
        }
        val finalAttrCount = attributesList.size

        // Rebuild Start Element Tag Node safely with updated size and attributes
        val finalChunkSize = 36 + finalAttributes.size
        val rebuiltChunk = ByteBuffer.allocate(finalChunkSize).order(ByteOrder.LITTLE_ENDIAN)
        rebuiltChunk.putShort(type)
        rebuiltChunk.putShort(headerSize)
        rebuiltChunk.putInt(finalChunkSize)
        rebuiltChunk.putInt(line)
        rebuiltChunk.putInt(comment)
        rebuiltChunk.putInt(ns)
        rebuiltChunk.putInt(name)
        rebuiltChunk.putShort(attrStart.toShort())
        rebuiltChunk.putShort(attrSize.toShort())
        rebuiltChunk.putShort(finalAttrCount.toShort())
        rebuiltChunk.putShort(classAttr)
        rebuiltChunk.putShort(idAttr)
        rebuiltChunk.putShort(styleAttr)
        rebuiltChunk.put(finalAttributes)

        return rebuiltChunk.array()
    }

    private fun rebuildResourceMap(
        chunk: ByteArray,
        newSize: Int,
        startIndex: Int,
        nameIdx: Int,
        valueIdx: Int,
        resizeableIdx: Int,
        extractNativeLibsIdx: Int
    ): ByteArray {
        val originalBuffer = ByteBuffer.wrap(chunk).order(ByteOrder.LITTLE_ENDIAN)
        val type = originalBuffer.short
        val headerSize = originalBuffer.short
        val chunkSize = originalBuffer.int
        
        val originalCount = (chunkSize - headerSize.toInt()) / 4
        val originalIds = IntArray(originalCount)
        for (i in 0 until originalCount) {
            originalIds[i] = originalBuffer.int
        }
        
        val newIds = IntArray(newSize)
        for (i in 0 until newSize) {
            if (i < originalCount) {
                newIds[i] = originalIds[i]
            } else {
                newIds[i] = 0
            }
        }
        
        // Map new attributes to their official Android Resource IDs if they are newly added
        if (nameIdx >= originalCount) {
            newIds[nameIdx] = 0x01010003
        }
        if (valueIdx >= originalCount) {
            newIds[valueIdx] = 0x01010024
        }
        if (resizeableIdx >= originalCount) {
            newIds[resizeableIdx] = 0x010104f6
        }
        if (extractNativeLibsIdx >= originalCount) {
            newIds[extractNativeLibsIdx] = 0x010104ea
        }
        
        val newChunkSize = headerSize.toInt() + newSize * 4
        val outBuffer = ByteBuffer.allocate(newChunkSize).order(ByteOrder.LITTLE_ENDIAN)
        outBuffer.putShort(type)
        outBuffer.putShort(headerSize)
        outBuffer.putInt(newChunkSize)
        for (id in newIds) {
            outBuffer.putInt(id)
        }
        
        return outBuffer.array()
    }

    /**
     * Builds the entire binary XML chunks for <meta-data android:name="..." android:value="..."/>
     */
    private fun buildMetadataChunk(nsIdx: Int, tagIdx: Int, nameIdx: Int, splitValIdx: Int, valueIdx: Int): ByteArray {
        val out = ByteArrayOutputStream()
        
        // 1. Build Start Element Chunk (76 bytes)
        val se = ByteBuffer.allocate(76).order(ByteOrder.LITTLE_ENDIAN)
        se.putShort(0x0102.toShort()) // Chunk Type (Start Element)
        se.putShort(16.toShort())     // Header Size (16)
        se.putInt(76)                      // Chunk Size (76)
        se.putInt(1)                       // Line Number
        se.putInt(-1)                      // Comment (-1)
        se.putInt(-1)                      // Namespace (-1)
        se.putInt(tagIdx)                  // Tag Name ("meta-data")
        se.putShort(20.toShort())          // Attribute Start Offset (20)
        se.putShort(20.toShort())          // Attribute Size (20)
        se.putShort(2.toShort())           // Attribute Count (2)
        se.putShort(0.toShort())           // Class Attribute
        se.putShort(0.toShort())           // Id Attribute
        se.putShort(0.toShort())           // Style Attribute
        
        // Attribute 1: android:name="BYD_SUPPORT_SPLIT_ACTIVITY"
        se.putInt(nsIdx)                   // Namespace URI
        se.putInt(nameIdx)                 // Name ("name")
        se.putInt(splitValIdx)             // Raw String Index
        se.putShort(8.toShort())           // size (8)
        se.put(0.toByte())                 // res (0)
        se.put(0x03.toByte())              // DataType (String = 3)
        se.putInt(splitValIdx)             // Data (string index)
        
        // Attribute 2: android:value="1" (Integer decimal = 0x10)
        se.putInt(nsIdx)                   // Namespace URI
        se.putInt(valueIdx)                // Name ("value")
        se.putInt(-1)                      // Raw String Index (-1 for numeric/integer)
        se.putShort(8.toShort())           // size (8)
        se.put(0.toByte())                 // res (0)
        se.put(0x10.toByte())              // DataType (Integer decimal = 0x10)
        se.putInt(1)                       // Data (1)
        
        out.write(se.array())
        
        // 2. Build End Element Chunk (24 bytes)
        val ee = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
        ee.putShort(0x0103.toShort()) // Chunk Type (End Element)
        ee.putShort(16.toShort())     // Header Size (16)
        ee.putInt(24)                      // Chunk Size (24)
        ee.putInt(1)                       // Line Number
        ee.putInt(-1)                      // Comment (-1)
        ee.putInt(-1)                      // Namespace (-1)
        ee.putInt(tagIdx)                  // Tag Name ("meta-data")
        
        out.write(ee.array())
        return out.toByteArray()
    }

    private fun getShort(bytes: ByteArray, offset: Int): Short {
        return ByteBuffer.wrap(bytes, offset, 2).order(ByteOrder.LITTLE_ENDIAN).short
    }

    private fun getInt(bytes: ByteArray, offset: Int): Int {
        return ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).int
    }

    private fun createAlignmentExtraField(alignment: Int, paddingSize: Int): ByteArray {
        val size = 2 + 2 + 2 + paddingSize
        val buf = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)
        buf.putShort(0xd935.toShort()) // Header ID
        buf.putShort((2 + paddingSize).toShort()) // Data Size
        buf.putShort(alignment.toShort()) // Alignment
        // Remaining bytes are initialized to 0
        return buf.array()
    }

    /**
     * Signs an APK file using BouncyCastle (V1/JAR Signing)
     */
    private fun signApkWithBc(unsignedApk: File, signedApk: File, privateKey: PrivateKey, cert: X509Certificate) {
        val signerConfig = com.android.apksig.ApkSigner.SignerConfig.Builder(
            "cert",
            privateKey,
            listOf(cert)
        ).build()
        
        com.android.apksig.ApkSigner.Builder(listOf(signerConfig))
            .setInputApk(unsignedApk)
            .setOutputApk(signedApk)
            .build()
            .sign()
    }
}

private class CountingOutputStream(private val out: java.io.OutputStream) : java.io.FilterOutputStream(out) {
    private var bytesWritten = 0L

    fun getCount(): Long = bytesWritten

    override fun write(b: Int) {
        out.write(b)
        bytesWritten++
    }

    override fun write(b: ByteArray) {
        out.write(b)
        bytesWritten += b.size
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        out.write(b, off, len)
        bytesWritten += len
    }

    override fun flush() {
        out.flush()
    }
}
