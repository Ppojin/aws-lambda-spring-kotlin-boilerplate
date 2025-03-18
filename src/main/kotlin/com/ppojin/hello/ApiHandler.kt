package com.ppojin.hello

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import java.util.*

@Component
class ApiHandler(
    restClientBuilder: RestClient.Builder,
    @Value("\${cognito.client.id}") private val clientId: String,
    @Value("\${cognito.client.secret}") clientSecret: String,
) {
    private final val oauthTokenRestClient: RestClient

    init {
        val basicAuth = Base64.getEncoder()
            .encodeToString("$clientId:$clientSecret".toByteArray())

        oauthTokenRestClient = restClientBuilder
            .baseUrl("https://cognito.ppojin.com/oauth2/token")
            .defaultHeader("Authorization", "Basic $basicAuth")
            .requestFactory(HttpComponentsClientHttpRequestFactory())
            .build()
    }

    @Bean
    fun apiGatewayEventHandler(): (APIGatewayProxyRequestEvent) -> APIGatewayProxyResponseEvent = {
        when (it.path) {
            "/oauth2" -> oauth2(it)
            else -> APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(it.path)
        }
    }

    fun oauth2(req: APIGatewayProxyRequestEvent): APIGatewayProxyResponseEvent {
        println("${req.path} ${req.queryStringParameters}")

        val authorizationCode = req.queryStringParameters["code"]

        val body = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "authorization_code")
            add("client_id", clientId)
            add("code", authorizationCode ?: "")
            add("redirect_uri", "https://www.ppojin.com/api/oauth2")
        }

        val token = oauthTokenRestClient.post()
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(body)
            .retrieve()
            .body(AuthToken::class.java)
            ?: throw IllegalStateException("Failed to retrieve token")

        return APIGatewayProxyResponseEvent()
            .withStatusCode(302)
            .withMultiValueHeaders(mapOf(
                "Location" to listOf("https://www.ppojin.com/api/test"),
                "Set-Cookie" to listOf(
                    buildString {
                        append("Authorization=Bearer ${token.access_token}; ")
                        append("Max-Age=${token.expires_in}; ")
                        append("SameSite=Lax; ")
                        append("HttpOnly; ")
                        append("Secure; ")
                    },
                    buildString {
                        append("Refresh_Token=${token.refresh_token}; ")
                        append("Max-Age=300; ")
                        append("SameSite=Lax; ")
                        append("Secure; ")
                    }
                ),
            ))
    }
}