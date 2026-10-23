package io.github.townyadvanced.iconomy.util;

import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class AsyncReadExecutor implements AutoCloseable {

	private final Set<Object> pending = ConcurrentHashMap.newKeySet();
	private final ThreadPoolExecutor worker;
	private volatile boolean closed;

	public AsyncReadExecutor() {
		this(32);
	}

	AsyncReadExecutor(int queueCapacity) {
		worker = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.SECONDS,
			new ArrayBlockingQueue<>(queueCapacity), runnable -> {
				Thread thread = new Thread(runnable, "iConomyUnlocked-read-commands");
				thread.setDaemon(true);
				return thread;
			}, new ThreadPoolExecutor.AbortPolicy());
	}

	public <T> boolean submit(Object key, Callable<T> query,
			BiConsumer<Runnable, Runnable> dispatch, Consumer<T> success,
			Consumer<Exception> failure) {
		if (closed || !pending.add(key))
			return false;
		Runnable complete = () -> pending.remove(key);
		try {
			worker.execute(() -> {
				if (closed) {
					complete.run();
					return;
				}
				Runnable reply;
				try {
					T result = query.call();
					reply = () -> success.accept(result);
				} catch (Exception error) {
					reply = () -> failure.accept(error);
				}
				if (closed) {
					complete.run();
					return;
				}
				Runnable delivery = reply;
				try {
					dispatch.accept(() -> {
						try {
							if (!closed)
								delivery.run();
						} finally {
							complete.run();
						}
					}, complete);
				} catch (RuntimeException schedulingFailure) {
					complete.run();
				}
			});
			return true;
		} catch (RejectedExecutionException fullOrStopped) {
			complete.run();
			return false;
		}
	}

	@Override
	public void close() {
		closed = true;
		worker.shutdownNow();
		pending.clear();
	}
}
