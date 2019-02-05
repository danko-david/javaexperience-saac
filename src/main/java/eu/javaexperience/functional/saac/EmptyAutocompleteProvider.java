package eu.javaexperience.functional.saac;

import eu.javaexperience.reflect.Mirror;

public class EmptyAutocompleteProvider implements AutocompleteProvider
{
	@Override
	public Object[] offerElement(String term, int from, int to)
	{
		return Mirror.emptyObjectArray;
	}
}
