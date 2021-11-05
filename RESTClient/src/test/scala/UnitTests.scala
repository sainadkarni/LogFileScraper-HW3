package com.restclient

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import akka.stream.ActorMaterializer
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers.{a, convertToAnyShouldWrapper}

import scala.concurrent.duration.DurationInt
import java.util
import java.util.List
import java.util.regex.Pattern
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class UnitTests extends AnyFunSuite {

	test("Unit test to check successful loading of configuration") {
		val config: Config = ConfigFactory.load("application.conf")
		val clientConfig = config.getConfig("clientSettings.requestBody")
		val time = clientConfig.getString("T")
		time shouldBe a [String]
	}

	test("Unit test to check pattern matching") {
		val config: Config = ConfigFactory.load("application.conf")
		val testingString = "20:54:56.986 [scala-execution-context-global-181] ERROR HelperUtils.Parameters$ - NT1#6aT6sY6uB9wG7vbe0F8wL9m\"Irsj,"
		val keywordPatternMatcher = Pattern.compile(s"([a-c][e-g][0-3]|[A-Z][5-9][f-w]){5,15}").matcher(testingString)
		assert(keywordPatternMatcher.find())
	}

	test("Unit test to check pattern matching negative case") {
		val config: Config = ConfigFactory.load("application.conf")
		val testingString = "20:54:56.986 [scala-execution-context-global-181] RANDOM HelperUtils.Parameters$ - T1#6aT6s6G7vbeF8wL9\"Irsj,"
		val keywordPatternMatcher = Pattern.compile(s"([a-c][e-g][0-3]|[A-Z][5-9][f-w]){5,15}").matcher(testingString)
		assert(!keywordPatternMatcher.find())
	}

	test("Unit test to check correct Future resolution") {

		val config: Config = ConfigFactory.load("application.conf")
		val clientConfig = config.getConfig("clientSettings")
		val apiEndpointExists = clientConfig.getString("lambdaEndpoints.logIntervalExists")

		// Akka HTTP actors
		implicit val system = ActorSystem()
		implicit val materializer = ActorMaterializer()
		import system.dispatcher

		val getrequest = HttpRequest(
			method = HttpMethods.GET,
			uri = s"${apiEndpointExists}?T=15:55:00&dT=00:01:00"
		)

		val testResponse = {
			val responseFuture = Http().singleRequest(getrequest)
			val response = responseFuture.flatMap(_.entity.toStrict(5 seconds)).map(_.data.utf8String)
			response
		}
		testResponse shouldBe a [Future[String]]
	}

	test("Unit test to check correst response type, which should be boolean") {

		val config: Config = ConfigFactory.load("application.conf")
		val clientConfig = config.getConfig("clientSettings")
		val apiEndpointExists = clientConfig.getString("lambdaEndpoints.logIntervalExists")

		// Akka HTTP actors
		implicit val system = ActorSystem()
		implicit val materializer = ActorMaterializer()
		import system.dispatcher

		val getrequest = HttpRequest(
			method = HttpMethods.GET,
			uri = s"${apiEndpointExists}?T=15:55:00&dT=00:01:00"
		)

		def sendRequest(req: HttpRequest): Future[String] = {
			val responseFuture = Http().singleRequest(getrequest)
			val response = responseFuture.flatMap(_.entity.toStrict(8 seconds)).map(_.data.utf8String)
			response
		}

		val response = Await.result(sendRequest(getrequest), 8 seconds).toBoolean
		response shouldBe a [Boolean]
	}

	test("Unit test to check MD5 hash match") {

		// Reference for MD5 hasher- https://alvinalexander.com/source-code/scala-method-create-md5-hash-of-string/

		def md5HashString(s: String): String = {
			import java.security.MessageDigest
			import java.math.BigInteger
			val md = MessageDigest.getInstance("MD5")
			val digest = md.digest(s.getBytes)
			val bigInt = new BigInteger(1,digest)
			val hashedString = bigInt.toString(16)
			hashedString
		}

		val testingString = "17:54:01.101 [scala-execution-context-global-179] DEBUG HelperUtils.Parameters$ - <(#>5Y`{QSNx9E*+4c+3Hs}hX9faf2ae3be1cg0cf2cg2T5sag1L9wW7hS7pce3ce2cg3'j+?ty9HZ^*i6`7r22gHx<}K"
		val hashReceived = "bcdb9ddfab7b83882b04b8099d2e09d3"
		val hashComputed = md5HashString(testingString)

		assert(hashComputed == hashReceived)
	}

}