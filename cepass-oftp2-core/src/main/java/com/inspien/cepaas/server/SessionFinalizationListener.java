package com.inspien.cepaas.server;

import static java.util.concurrent.TimeUnit.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.neociclo.odetteftp.OdetteFtpException;
import org.neociclo.odetteftp.OdetteFtpSession;
import org.neociclo.odetteftp.support.OftpletEventListenerAdapter;

/**
 * @author Rafael Marins
 */
public class SessionFinalizationListener extends OftpletEventListenerAdapter {

	private final Object lock = new Object();

	private final AtomicInteger noInits = new AtomicInteger();
	private final AtomicInteger noDestroys = new AtomicInteger();

	private int noOfSessions;

	public SessionFinalizationListener(int noOfSessions) {
		super();
		this.noOfSessions = noOfSessions;
	}

	@Override
	public void init(OdetteFtpSession session) throws OdetteFtpException {
		synchronized (noInits) {
			noInits.incrementAndGet();
		}
	}

	@Override
	public void destroy() {
		synchronized (noDestroys) {
			int deltaStarts = noInits.get();
			int deltaEnds = noDestroys.incrementAndGet();
			if (deltaStarts >= noOfSessions && deltaEnds >= noOfSessions) {
				synchronized (lock) {
					lock.notifyAll();
				}
			}
		}
	}

	public void waitFinalization() throws InterruptedException {
		synchronized (lock) {
			lock.wait(MINUTES.toMillis(5));
		}
	}
}
