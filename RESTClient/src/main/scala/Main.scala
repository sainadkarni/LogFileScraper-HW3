package com.restclient

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpEntity, HttpMethods, HttpRequest}
import akka.stream.ActorMaterializer
import net.liftweb.json.DefaultFormats
import net.liftweb.json.Serialization.write
import com.typesafe.config.{Config, ConfigFactory}

import java.util.logging.Logger
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

// Model for our data structure required to make the request
final case class Details(T: String, dT: String)

object Main extends App {

	// Akka HTTP required actors
	implicit val system = ActorSystem()
	implicit val materializer = ActorMaterializer()
	import system.dispatcher

	val logger = Logger.getLogger(classOf[App].getName)

	val config: Config = ConfigFactory.load("application.conf")
	val clientConfig = config.getConfig("clientSettings")

	// Initialize model object
	val timeDetails: Details = Details(clientConfig.getString("requestBody.T"), clientConfig.getString("requestBody.dT"))

	// Use model object to create JSON and subsequent JSON string (equivalent to JSON.stringify()) for parcelling in request body
	implicit val formats = DefaultFormats
	val jsonTimestring = write(timeDetails)

	// Endpoints from configuration
	val apiEndpointExists = clientConfig.getString("lambdaEndpoints.logIntervalExists")
	val apiEndpointMessages = clientConfig.getString("lambdaEndpoints.getMessages")

	// Create HttpRequest's for the different combinations. Creating val refuses me to update the URI later, which is the reason for multiple requests here
	val getrequest = HttpRequest(
		method = HttpMethods.GET,
		uri = s"${apiEndpointExists}?T=${timeDetails.T}&dT=${timeDetails.dT}"
	)

	val messagesRequest = HttpRequest(
		method = HttpMethods.GET,
		uri = s"${apiEndpointMessages}?T=${timeDetails.T}&dT=${timeDetails.dT}"
	)

	val postrequest = HttpRequest(
		method = HttpMethods.POST,
		uri = s"${apiEndpointExists}",
		entity = HttpEntity(jsonTimestring)
	)

	val postmessagesrequest = HttpRequest(
		method = HttpMethods.POST,
		uri = s"${apiEndpointMessages}",
		entity = HttpEntity(jsonTimestring)
	)

	// Method to make request according to configured request method
	def requester(): Unit = {
		val requestType = clientConfig.getString("requestType")
		logger.info("Chosen request method type: " + requestType)
		val requests: List[HttpRequest] = {
			if(requestType == "GET") {
				List(getrequest, messagesRequest)
			}
			else
				List(postrequest, postmessagesrequest)
		}

		logger.info("Sending request to check if log messages exist at configured timeframe")

		// Reference- https://dzone.com/articles/sending-http-requests-in-5-mins-with-scala-and-akk
		val responseFuture = Http().singleRequest(requests(0))
		responseFuture.flatMap(_.entity.toStrict(5 seconds)).map(_.data.utf8String).foreach { value =>
			if(value.toBoolean) {
				logger.info("Log messages found, fetching their MD5 hashes...")
				val messageresponseFuture = Http().singleRequest(requests(1))
				messageresponseFuture.flatMap(_.entity.toStrict(5 seconds)).map(_.data.utf8String).foreach(println)
			}
			else
				logger.info("Log messages not found for given timeframe.")
		}
	}

	requester()
}