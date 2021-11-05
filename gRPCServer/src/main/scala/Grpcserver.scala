package com.grpcserver

import com.typesafe.config.{Config, ConfigFactory}
import com.example.protos.logTime.{CheckerGrpc, Messages, TimeReply, TimeRequest}

import akka.actor.ActorSystem

import scala.concurrent.Await
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import akka.stream.ActorMaterializer
import io.grpc.{Server, ServerBuilder}

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.concurrent.{ExecutionContext, Future}

// This file contains all the bootstrapping code for the gRPC server which calls the lambda function by a basic HTTP call at the request of a client.
// The reference for me for the skeleton gRPC code was scalaPB, as I used that to write my server and client.
// https://scalapb.github.io/docs/grpc here are the official docs which give an explanation for the server.
// https://github.com/xuwei-k/grpc-scala-sample/blob/master/grpc-scala/src/main/scala/io/grpc/examples/helloworld/HelloWorldServer.scala Reference skeleton code

object Grpcserver {

	val config: Config = ConfigFactory.load("application.conf")
	val serverConfig = config.getConfig("serverSettings")

	def main(args: Array[String]): Unit = {
		val server = new Grpcserver(ExecutionContext.global)
		server.start()
		server.blockUntilShutdown()
	}

	private val port = serverConfig.getInt("port")
}

class Grpcserver(executionContext: ExecutionContext) { self =>

	val config: Config = ConfigFactory.load("application.conf")
	val serverConfig = config.getConfig("serverSettings")

	// Bootstrap the server and add a binding service implementation to it.
	private[this] val server: Server = ServerBuilder.forPort(Grpcserver.port).addService(CheckerGrpc.bindService(new CheckerImpl, executionContext)).build

	// Procedure to start the server
	private def start(): Unit = {
		server.start
		sys.addShutdownHook {
			System.err.println("*** shutting down gRPC server since JVM is shutting down")
			self.stop()
			System.err.println("*** server shut down")
		}
	}

	// Procedure to stop the server
	private def stop(): Unit = {
		if (server != null) {
			server.shutdown()
		}
	}

	// Procedure to keep the server running until shutdown signal received
	private def blockUntilShutdown(): Unit = {
		if (server != null) {
			server.awaitTermination()
		}
	}

	private class CheckerImpl extends CheckerGrpc.Checker {

		// Akka HTTP actors
		implicit val system = ActorSystem()
		implicit val materializer = ActorMaterializer()
		import system.dispatcher

		// Helper function to send HTTP requests to the configured lambda functions API gateway endpoints
		def sendRequest(req: HttpRequest): Future[String] = {
			val responseFuture = Http().singleRequest(req)
			val response = responseFuture.flatMap(_.entity.toStrict(5 seconds)).map(_.data.utf8String)
			response
		}


		// Function which queries the lambda to find if log messages do indeed occur for given timestamp
		override def checkExists(request: TimeRequest): Future[TimeReply] = {

			val apiEndpoint = serverConfig.getString("lambdaEndpoints.logIntervalExists")

			// Form the request string
			val getrequest = HttpRequest(
				method = HttpMethods.GET,
				uri = s"${apiEndpoint}?T=${request.t}&dT=${request.dT}"
			)

			val response = Await.result(sendRequest(getrequest), 6 seconds)
			Future.successful(TimeReply(response.toBoolean))
		}

		// Function which queries the lambda function to actually retrieve the MD5 hashes of the log messages
		override def getMessages(request: TimeRequest): Future[Messages] = {

			val apiEndpoint = serverConfig.getString("lambdaEndpoints.getMessages")

			// For the request string
			val messagesRequest = HttpRequest(
				method = HttpMethods.GET,
				uri = s"${apiEndpoint}?T=${request.t}&dT=${request.dT}"
			)

			val response = Await.result(sendRequest(messagesRequest), 6 seconds)
			Future.successful(Messages(response))
		}
	}
}