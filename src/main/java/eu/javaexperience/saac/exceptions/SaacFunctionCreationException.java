package eu.javaexperience.saac.exceptions;

import java.util.Arrays;

import eu.javaexperience.functional.saac.Functions.PreparedFunction;

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
	
	public PreparedFunction function;
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

	public String toDetailedMessage()
	{
		String args = "";
		if(null == arguments)
		{
			args = "null";
		}
		else if(arguments.getClass().isArray())
		{
			try
			{
			args = Arrays.toString((Object[])arguments);
			}
			catch(Exception e)
			{}
		}
		else
		{
			args = arguments.toString();
		}
		return getMessage()+" function: "+function+", functionName: "+functionName+", arguments: "+args;
	}
	
}
