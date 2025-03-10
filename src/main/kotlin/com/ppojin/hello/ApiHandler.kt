package com.ppojin.hello

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import java.util.*

@Service
class ApiHandler(
    restClientBuilder: RestClient.Builder,
    @Value("\${cognito.client.id}") private val clientId: String,
    @Value("\${cognito.client.secret}") clientSecret: String,
) {
    private final val restClient: RestClient

    init {
        val basicAuth = Base64.getEncoder()
            .encodeToString("$clientId:$clientSecret".toByteArray())

        restClient = restClientBuilder
            .baseUrl("https://cognito.ppojin.com/oauth2/token")
            .defaultHeader("Authorization", "Basic $basicAuth")
            .requestFactory(JdkClientHttpRequestFactory())
            .build()
    }

    @Bean
    fun api(): (APIGatewayProxyRequestEvent) -> APIGatewayProxyResponseEvent = { it: APIGatewayProxyRequestEvent ->
        if (it.path == "/oauth2") {
            val authorizationCode = it.queryStringParameters["code"]

            val body = LinkedMultiValueMap<String, String>().apply {
                add("grant_type", "authorization_code")
                add("client_id", clientId)
                add("code", authorizationCode ?: "")
                add("redirect_uri", "https://www.ppojin.com/api/oauth2")
            }

            val token = restClient.post()
                .uri { it.path("/oauth2/token").build() }
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(AuthToken::class.java)
                ?: throw IllegalStateException("Failed to retrieve token")

            APIGatewayProxyResponseEvent()
                .withStatusCode(302)
                .withHeaders(mapOf(
                    "Location" to "https://www.ppojin.com/api/test",
                    "Set-Cookie" to buildString {
                        append("Authorization=\"Bearer ${token.access_token}\"; ")
                        append("Max-Age=${token.expires_in}; ")
                        append("SameSite=Lax; ")
                        append("HttpOnly; ")
                        append("Secure; ")
                    },
                    "Set-Cookie" to buildString {
                        append("Refresh_Token=\"Bearer ${token.refresh_token}\"; ")
                        append("SameSite=Lax")
                        append("Max-Age=300; ")
                        append("Secure; ")
                    },
                ))
        } else {
            APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withBody(it.path)
        }
    }
}