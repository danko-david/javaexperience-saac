package eu.javaexperience.saac.exceptions;

public class SaacFunctionCreationException extends SaacException
{
	public SaacFunctionCreationException() {}

	public SaacFunctionCreationException(String message)
	{
		super(message);
	}

	public SaacFunctionCreationException(Throwable cause)
	{
		super(cause);
	}
	
	public String functionName;
	public Object arguments;
	
	public SaacFunctionCreationException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public SaacFunctionCreationException
	(
		String message,
		Throwable cause,
		boolean enableSuppression,
		boolean writableStackTrace
	)
	{
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
