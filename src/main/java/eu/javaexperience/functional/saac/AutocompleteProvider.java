package eu.javaexperience.functional.saac;

public interface AutocompleteProvider
{
	public Object[] offerElement(String term, int from, int to); 
}
