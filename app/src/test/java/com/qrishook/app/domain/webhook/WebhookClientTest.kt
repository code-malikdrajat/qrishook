package com.qrishook.app.domain.webhook

import com.qrishook.app.data.model.HttpMethod
import com.qrishook.app.data.model.KeyValue
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WebhookClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpWebhookClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = OkHttpWebhookClient(
            OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.SECONDS)
                .readTimeout(1, TimeUnit.SECONDS)
                .build(),
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun success200() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"ok":true}"""))

        val result = client.send(
            url = server.url("/hook").toString(),
            method = HttpMethod.POST,
            headers = listOf(KeyValue("Content-Type", "application/json")),
            body = """{"event":"transaction"}""",
        )

        assertTrue(result is WebhookResult.Success)
        result as WebhookResult.Success
        assertEquals(200, result.httpCode)
        assertEquals("""{"ok":true}""", result.body)
    }

    @Test
    fun failed500IsRetriedAndReported() = runTest {
        repeat(4) { server.enqueue(MockResponse().setResponseCode(500).setBody("boom")) }

        val result = client.send(
            url = server.url("/hook").toString(),
            method = HttpMethod.POST,
            headers = emptyList(),
            body = "{}",
        )

        assertTrue(result is WebhookResult.Failed)
        assertEquals(4, server.requestCount) // 1 percobaan + 3 retry
    }

    @Test
    fun networkError() = runTest {
        repeat(4) { server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)) }

        val result = client.send(
            url = server.url("/hook").toString(),
            method = HttpMethod.POST,
            headers = emptyList(),
            body = "{}",
        )

        assertTrue(result is WebhookResult.NetworkError)
    }

    @Test
    fun headersAreSent() = runTest {
        server.enqueue(MockResponse().setResponseCode(200))

        client.send(
            url = server.url("/hook").toString(),
            method = HttpMethod.POST,
            headers = listOf(KeyValue("X-Token", "rahasia"), KeyValue("X-App", "QRISHook")),
            body = "{}",
        )

        val recorded = server.takeRequest()
        assertEquals("rahasia", recorded.headers["X-Token"])
        assertEquals("QRISHook", recorded.headers["X-App"])
    }

    @Test
    fun postBodyIsSent() = runTest {
        server.enqueue(MockResponse().setResponseCode(200))

        client.send(
            url = server.url("/hook").toString(),
            method = HttpMethod.POST,
            headers = emptyList(),
            body = """{"event":"heartbeat"}""",
        )

        assertEquals("""{"event":"heartbeat"}""", server.takeRequest().body.readUtf8())
    }

    @Test
    fun getSendsBodyAsQuery() = runTest {
        server.enqueue(MockResponse().setResponseCode(200))

        client.send(
            url = server.url("/hook").toString(),
            method = HttpMethod.GET,
            headers = emptyList(),
            body = "event=heartbeat&device=Pixel7",
        )

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("heartbeat", recorded.requestUrl?.queryParameter("event"))
        assertEquals("Pixel7", recorded.requestUrl?.queryParameter("device"))
    }
}
