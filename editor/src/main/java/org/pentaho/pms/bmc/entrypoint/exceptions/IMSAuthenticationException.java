/*
 * ©Copyright 2024 BMC Software. Inc.
 *
 */
package org.pentaho.pms.bmc.entrypoint.exceptions;

public class IMSAuthenticationException extends RuntimeException {
	private static final long serialVersionUID = -6709865471231L;
	public static final String IM_PREFIX = "IM-";

	public IMSAuthenticationException() {
		super();
	}

	public IMSAuthenticationException(final String msg) {
		super(msg);
	}

	public IMSAuthenticationException(final String code, final String msg) {
		super(IM_PREFIX + code + ":" + msg);
	}

	public IMSAuthenticationException(final String msg, final Throwable cause) {
		super(msg);
		initCause(cause);
	}

	public IMSAuthenticationException(final String code, final String msg, final Throwable cause) {
		super(IM_PREFIX + code + ":" + msg);
		initCause(cause);
	}

}