/*
 * ©Copyright 2024 BMC Software. Inc.
 *
 */
package org.pentaho.pms.bmc.entrypoint.exceptions;

public class RMSAuthorizationException extends RuntimeException {

	private static final long serialVersionUID = -7709865471231L;
	public static final String RM_PREFIX = "RM-";

	public RMSAuthorizationException() {
		super();
	}

	public RMSAuthorizationException(final String msg) {
		super(msg);
	}

	public RMSAuthorizationException(final String code, final String msg) {
		super(RM_PREFIX + code + ":" + msg);
	}

	public RMSAuthorizationException(final String msg, final Throwable cause) {
		super(msg);
		initCause(cause);
	}

	public RMSAuthorizationException(final String code, final String msg, final Throwable cause) {
		super(RM_PREFIX + code + ":" + msg);
		initCause(cause);
	}
}
