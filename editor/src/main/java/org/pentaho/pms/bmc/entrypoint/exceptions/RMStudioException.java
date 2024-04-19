package org.pentaho.pms.bmc.entrypoint.exceptions;

public class RMStudioException extends RuntimeException {
	private static final long serialVersionUID = -2409865471231L;
	public static final String RS_PREFIX = "RS-";

	public RMStudioException() {
		super();
	}
	
	public RMStudioException(final String msg) {
		super(msg);
	}
	
	public RMStudioException(final String code, final String msg) {
		super(RS_PREFIX + code + ":" + msg);
	}
	
	public RMStudioException(final String msg, final Throwable cause) {
		super(msg);
		initCause(cause);
	}
	
	public RMStudioException(final String code, final String msg, final Throwable cause) {
		super(RS_PREFIX + code + ":" + msg);
		initCause(cause);
	}
}
