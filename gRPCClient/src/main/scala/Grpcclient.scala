package com.grpcclient

import java.util.concurrent.TimeUnit
import java.util.logging.{Level, Logger}
import com.typesafe.config.{Config, ConfigFactory}
import com.example.protos.logTime.{CheckerGrpc, TimeReply, TimeRequest}
import com.example.protos.logTime.CheckerGrpc.CheckerBlockingStub
import io.grpc.{StatusRuntimeException, ManagedChannelBuilder, ManagedChannel}


// This file contains all the bootstrapping code for the gRPC client which requests the gRPC server for data.
// The reference for me was scalaPB, as I used that to write my server and client.
// https://scalapb.github.io/docs/grpc here are the official docs which give an explanation for the client.
// https://github.com/xuwei-k/grpc-scala-sample/blob/master/grpc-scala/src/main/scala/io/grpc/examples/helloworld/HelloWorldClient.scala Reference skeleton code

object Grpcclient {

	val config: Config = ConfigFactory.load("application.conf")
	val clientConfig = config.getConfig("clientSettings")

	def apply(host: String, port: Int): Grpcclient = {

		// Creating the channel
		val channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build

		// Creating the blocking call to server
		val blockingStub = CheckerGrpc.blockingStub(channel)
		new Grpcclient(channel, blockingStub)
	}

	def main(args: Array[String]): Unit = {

		// Configure server details for client
		val client = Grpcclient(clientConfig.getString("serverUrl"), clientConfig.getInt("serverPort"))

		// "Business" logic for determining if messages are to be fetched (if they exist)
		try {
			val time = clientConfig.getString("requestBody.T")
			val interval = clientConfig.getString("requestBody.dT")

			if(client.check(time, interval))
				client.getmd5(time, interval)
		} finally {
			client.shutdown()
		}
	}
}

class Grpcclient private(private val channel: ManagedChannel, private val blockingStub: CheckerBlockingStub) {

	private[this] val logger = Logger.getLogger(classOf[Grpcclient].getName)

	def shutdown(): Unit = {
		channel.shutdown.awaitTermination(5, TimeUnit.SECONDS)
	}

	// Query server if log messages exist for the parameterized timeframe
	def check(time: String, interval: String): Boolean = {

		logger.info("Sending request to check if log messages occur at configured timeframe")
		val request = TimeRequest(time, interval)

		try {
			val response = blockingStub.checkExists(request)
			logger.info("******************************************\n")
			logger.info("Log messages for the time interval exist- " + response.message.toString)
			logger.info("\n******************************************")
			response.message
		}
		catch {
			case e: StatusRuntimeException =>
				logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus)
				false
		}
	}

	// Query server for the actual log messages in the given timeframe
	def getmd5(time: String, interval: String): Unit = {

		logger.info("Since messages exist, fetching log messages fitting the required pattern")
		val request = TimeRequest(time, interval)

		try {
			val response = blockingStub.getMessages(request)
			logger.info("******************************************\n")
			logger.info("MD5 Hashes of the log messages:-\n" + response.messages.toString)
			logger.info("\n******************************************")
		}
		catch {
			case e: StatusRuntimeException =>
				logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus)
		}
	}
}