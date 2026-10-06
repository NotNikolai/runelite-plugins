package com.nhfc;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

// one websocket to the nhfc server, on runelite's own okhttp client. reconnects with a growing wait
@Slf4j
class NhfcConnection
{
	static final int PROTOCOL = 1;
	private static final int MAX_BACKOFF_SECONDS = 60;

	private final OkHttpClient okHttpClient;
	private final Gson gson;
	private final ScheduledExecutorService executor;
	private final Consumer<JsonObject> onMessage;
	private final Consumer<String> onClosed;

	private WebSocket socket;
	private ScheduledFuture<?> retry;
	private int failures;
	private boolean wanted;
	private String url;
	private JsonObject hello;

	NhfcConnection(OkHttpClient okHttpClient, Gson gson, ScheduledExecutorService executor, Consumer<JsonObject> onMessage, Consumer<String> onClosed)
	{
		this.okHttpClient = okHttpClient;
		this.gson = gson;
		this.executor = executor;
		this.onMessage = onMessage;
		this.onClosed = onClosed;
	}

	synchronized void open(String serverUrl, JsonObject hello)
	{
		this.url = serverUrl.replaceFirst("^http", "ws").replaceAll("/+$", "") + "/plugin/ws";
		this.hello = hello;
		wanted = true;
		connect();
	}

	synchronized void close()
	{
		wanted = false;
		if (retry != null)
		{
			retry.cancel(false);
			retry = null;
		}
		if (socket != null)
		{
			socket.close(1000, "bye");
			socket = null;
		}
	}

	synchronized boolean isOpen()
	{
		return socket != null;
	}

	// okhttp queues it and sends it on its own thread, so this is fine to call from the client thread
	synchronized void send(JsonObject msg)
	{
		if (socket != null)
		{
			socket.send(gson.toJson(msg));
		}
	}

	private void connect()
	{
		Request request = new Request.Builder().url(url).build();
		socket = okHttpClient.newWebSocket(request, new WebSocketListener()
		{
			@Override
			public void onOpen(WebSocket ws, Response response)
			{
				failures = 0;
				ws.send(gson.toJson(hello));
			}

			@Override
			public void onMessage(WebSocket ws, String text)
			{
				try
				{
					onMessage.accept(gson.fromJson(text, JsonObject.class));
				}
				catch (RuntimeException e)
				{
					log.debug("bad message from nhfc", e);
				}
			}

			@Override
			public void onClosed(WebSocket ws, int code, String reason)
			{
				dropped(ws, reason);
			}

			@Override
			public void onFailure(WebSocket ws, Throwable t, Response response)
			{
				log.debug("nhfc connection failed", t);
				dropped(ws, "Can't reach NHFC");
			}
		});
	}

	private synchronized void dropped(WebSocket ws, String reason)
	{
		if (ws != socket)
		{
			return;
		}
		socket = null;
		onClosed.accept(reason);
		if (!wanted)
		{
			return;
		}
		failures++;
		long wait = Math.min(MAX_BACKOFF_SECONDS, 1L << Math.min(failures, 6));
		retry = executor.schedule(() ->
		{
			synchronized (NhfcConnection.this)
			{
				if (wanted && socket == null)
				{
					connect();
				}
			}
		}, wait, TimeUnit.SECONDS);
	}
}
