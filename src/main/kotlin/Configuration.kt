/**
 * Copyright 2022 (c) Ur Nan
 *
 * This code is distributed under the GNU GPL Version 2.
 * For details, please read the LICENSE file.
 *
 */
package cc.telepath.rhizome

import java.io.File
import java.io.StringReader
import java.io.StringWriter
import java.util.Properties
import java.util.Base64

/**
 * Get the base64 ciphertext and base64 IV from the config file.
 * Then decrypt with our key, read as a Properties object and return the result.
 */
fun readConfig(filepath: String, key: String): Properties {
    val file =  File(filepath)
    when{
        file.exists() -> {
            val lines = file.readLines()
            val cipherText = lines[0].split(":")[0]
            val IV = Base64.getDecoder().decode(lines[0].split(":")[1])
            if(IV.size == 16){
                val props = Properties()
                var crupty = CBCDecrypt(key, cipherText, IV)
                props.load(StringReader(crupty))
                return props
            }
            else return Properties()
        }
    }
    return Properties()
}

/**
 * key must be a Base64 encoded 256 bit AES key.
 */
fun saveConfig(filepath:String, key: String, props: Properties){
    val IV = generateIV()
    val sw = StringWriter()
    props.store(sw, "This is el commento")
    val debug = sw.toString()
    val cipherText = CBCEncrypt(key, sw.toString().toByteArray(), IV)
    val file = File(filepath)
    file.writeText(cipherText + ":" + String(Base64.getEncoder().encode(IV)))
}