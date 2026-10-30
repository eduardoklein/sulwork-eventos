package br.com.sulwork.events;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Runs some work in another transaction and keeps it uncommitted for a while, like a second person in the middle
 * of a request. The caller continues as soon as the work is done, and joins the thread at the end.
 */
public final class UncommittedTransaction {

	private static final long HOLD_MILLIS = 800;

	private UncommittedTransaction() {
	}

	public static Thread hold(PlatformTransactionManager transactionManager, Runnable work)
			throws InterruptedException {
		CountDownLatch done = new CountDownLatch(1);
		Thread thread = new Thread(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
			work.run();
			done.countDown();
			try {
				Thread.sleep(HOLD_MILLIS);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}));
		thread.start();
		if (!done.await(10, TimeUnit.SECONDS)) {
			throw new IllegalStateException("The other transaction did not start in time.");
		}
		return thread;
	}

}
