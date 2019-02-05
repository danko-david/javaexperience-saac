package eu.javaexperience.functional.saac;

public interface FunctionCreator<T>
{
	
	/**
	 * Short name of function: isEquals
	 * */
	public String getName();
	
	/**
	 * Description of the function: checks that the given value is equals with the specified one.
	 * */
	public String getDescription();
	
	/**
	 * Numbers of accepted parameters:
	 * 	-1 varargs,
	 *  0: no arguments
	 *  n: n args
	 * */
	public int getParamCount();
	
	/**
	 * returns the description of the parameter: -1 for varargs
	 * */
	public String getParamDescription(int n);
	
	/**
	 * Creates the function.
	 * */
	public T createFunction(Object... arguments);
	
	/**
	 * Ordinal of the creator in the creator set
	 * */
	public int ordinal();
}
