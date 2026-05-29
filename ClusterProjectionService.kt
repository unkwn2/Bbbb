package com.sr.openbyd.adb

import android.content.Context
import android.os.Build
import android.util.Base64
import io.github.muntashirakon.adb.AbsAdbConnectionManager
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.*
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.*
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Date
import kotlin.random.Random

class AdbConnectionManager private constructor(context: Context) : AbsAdbConnectionManager() {

    private var mPrivateKey: PrivateKey? = null
    private var mCertificate: Certificate? = null

    init {
        api = Build.VERSION.SDK_INT
        mPrivateKey = readPrivateKeyFromFile(context)
        mCertificate = readCertificateFromFile(context)

        if (mPrivateKey == null || mCertificate == null) {
            generateKeysAndCert(context)
        }
    }

    private fun generateKeysAndCert(context: Context) {
        val keySize = 2048
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(keySize, SecureRandom())
        val generateKeyPair = keyPairGenerator.generateKeyPair()
        val publicKey = generateKeyPair.public
        val privateKey = generateKeyPair.private

        mPrivateKey = privateKey

        // Generate a new certificate using BouncyCastle
        val subject = X500Name("CN=Open BYD Assistant")
        val issuer = subject
        val serial = BigInteger.valueOf(Random.nextLong().coerceAtLeast(1L))
        val notBefore = Date()
        val notAfter = Date(System.currentTimeMillis() + 86400000L * 3650) // 10 years

        val certBuilder = JcaX509v3CertificateBuilder(
            issuer, serial, notBefore, notAfter, subject, publicKey
        )

        val signer = JcaContentSignerBuilder("SHA256withRSA").build(privateKey)
        val certHolder = certBuilder.build(signer)
        val certificate = JcaX509CertificateConverter().getCertificate(certHolder)

        mCertificate = certificate

        writePrivateKeyToFile(context, privateKey)
        writeCertificateToFile(context, certificate)
    }

    override fun getPrivateKey(): PrivateKey {
        return mPrivateKey!!
    }

    override fun getCertificate(): Certificate {
        return mCertificate!!
    }

    override fun getDeviceName(): String {
        return "OpenBYD"
    }

    private fun readCertificateFromFile(context: Context): Certificate? {
        val certFile = File(context.filesDir, "cert.pem")
        if (!certFile.exists()) return null
        return try {
            FileInputStream(certFile).use { cert ->
                CertificateFactory.getInstance("X.509").generateCertificate(cert)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun writeCertificateToFile(context: Context, certificate: Certificate) {
        val certFile = File(context.filesDir, "cert.pem")
        try {
            FileOutputStream(certFile).use { os ->
                os.write("-----BEGIN CERTIFICATE-----\n".toByteArray(StandardCharsets.UTF_8))
                val base64 = Base64.encodeToString(certificate.encoded, Base64.NO_WRAP)
                os.write(base64.toByteArray(StandardCharsets.UTF_8))
                os.write("\n-----END CERTIFICATE-----\n".toByteArray(StandardCharsets.UTF_8))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun readPrivateKeyFromFile(context: Context): PrivateKey? {
        val privateKeyFile = File(context.filesDir, "private.key")
        if (!privateKeyFile.exists()) return null
        val privKeyBytes = ByteArray(privateKeyFile.length().toInt())
        return try {
            FileInputStream(privateKeyFile).use { `is` ->
                `is`.read(privKeyBytes)
            }
            val keyFactory = KeyFactory.getInstance("RSA")
            val privateKeySpec = PKCS8EncodedKeySpec(privKeyBytes)
            keyFactory.generatePrivate(privateKeySpec)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun writePrivateKeyToFile(context: Context, privateKey: PrivateKey) {
        val privateKeyFile = File(context.filesDir, "private.key")
        try {
            FileOutputStream(privateKeyFile).use { os ->
                os.write(privateKey.encoded)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AbsAdbConnectionManager? = null

        @JvmStatic
        fun getInstance(context: Context): AbsAdbConnectionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AdbConnectionManager(context).also { INSTANCE = it }
            }
        }
    }
}
