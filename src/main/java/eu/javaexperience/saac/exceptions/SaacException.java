package eu.javaexperience.saac.exceptions;

import eu.javaexperience.reflect.Mirror;

public class SaacException extends RuntimeException
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public SaacException(){}

	public SaacException(String message)
	{
		super(message);
	}

	public SaacException(Throwable cause)
	{
		super(cause);
	}

	public SaacException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public SaacException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
	{
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public String toDetailedMessage()
	{
		return toString();
	}
}
