/**
 * Copyright 2022 (c) Ur Nan
 *
 * This code is distributed under the GNU GPL Version 2.
 * For details, please read the LICENSE file.
 *
 */
package cc.telepath.rhizome

import java.security.*
import java.security.spec.AlgorithmParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 *  Return a private key read from a b64 string.
 */
fun readPrivkey(b64Key: String): PrivateKey{
    val keySpec = PKCS8EncodedKeySpec(Base64.getDecoder().decode(b64Key))
    return KeyFactory.getInstance("RSA").generatePrivate(keySpec)
}

/**
 * Create a public key from a base 64 encoded public key
 */
fun readPubkey(b64Key: String): PublicKey{
    val keySpec = X509EncodedKeySpec(Base64.getDecoder().decode(b64Key))
    return KeyFactory.getInstance("RSA").generatePublic(keySpec)
}

/**
 * Generate an RSA Keypair
 */
fun generateKeypair(): KeyPair{
    val kpg: KeyPairGenerator = KeyPairGenerator.getInstance("RSA")
    kpg.initialize(2048)
    val newKeyPair: KeyPair = kpg.generateKeyPair()
    return newKeyPair
}

/**
 * We're using CBC with randomly generated IVs because it's safer than ECB and easier than GCM
 * Pass the plaintext and let me do the rest.
 * Takes: Base64 Encoded Key
 */
fun CBCEncrypt(key: String, data: ByteArray, IV: ByteArray): String{
    val IVSpec: AlgorithmParameterSpec = IvParameterSpec(IV)
    val key: SecretKeySpec = SecretKeySpec(Base64.getDecoder().decode(key), "AES")
    val c: Cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    c.init(Cipher.ENCRYPT_MODE, key, IVSpec)
    return String(Base64.getEncoder().encode(c.doFinal(Base64.getEncoder().encode(data))))
}

/**
 * Generate a random 256 bit AES key, return Base64 encoded string
 */
fun generateAESKey(): String{
    var keyBytes: ByteArray = ByteArray(32)
    var random: SecureRandom = SecureRandom()
    random.nextBytes(keyBytes)
    return String(Base64.getEncoder().encode(keyBytes))
}

/**
 * Generate a random IV
 */
fun generateIV(): ByteArray{
    var IV: ByteArray = ByteArray(16)
    val rand: SecureRandom = SecureRandom()
    rand.nextBytes(IV)
    return IV
}

/**
 * Decrypt and return plaintext
 */
fun CBCDecrypt(b64key: String, data:String, IV: ByteArray): String{
    var c: Cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    var IVSpec: AlgorithmParameterSpec = IvParameterSpec(IV)
    var key: SecretKeySpec = SecretKeySpec(Base64.getDecoder().decode(b64key), "AES")
    c.init(Cipher.DECRYPT_MODE, key, IVSpec)
    var spoop = c.doFinal(Base64.getDecoder().decode(data))
    return String(Base64.getDecoder().decode(spoop))
}


/**
 * Sign the base64 encoding of a message. Return Base64 representation of signature.
 */
fun signMessage(signingKey: PrivateKey, message: String): String{
    val sig: Signature = Signature.getInstance("SHA512withRSA")
    sig.initSign(signingKey)
    val b64Encoder: Base64.Encoder = Base64.getEncoder()
    sig.update(b64Encoder.encode(message.toByteArray()))
    return String(b64Encoder.encode(sig.sign()))
}

/**
 * Takes a base64 encoded signature and verifies that it was created by a given public key
 */
fun verifyMessage(signingKey: PublicKey, message: String,  signature: String) : Boolean{
    val sig: Signature = Signature.getInstance("SHA512withRSA")
    val b64Decoder: Base64.Decoder = Base64.getDecoder()
    val base64Encoder: Base64.Encoder = Base64.getEncoder()
    sig.initVerify(signingKey)
    sig.update(base64Encoder.encode(message.toByteArray()))
    val result = sig.verify(b64Decoder.decode(signature))
    return result
}