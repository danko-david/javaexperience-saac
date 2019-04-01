package eu.javaexperience.saac;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import eu.javaexperience.collection.map.NullMap;
import eu.javaexperience.datareprez.DataObject;
import eu.javaexperience.datareprez.javaImpl.DataObjectJavaImpl;
import eu.javaexperience.functional.saac.Functions;
import eu.javaexperience.functional.saac.Functions.PreparedFunction;
import eu.javaexperience.saac.SaacEnv.SaacGetByWrapper;

public class SaacFunctionCollection
{
	protected Map<String, PreparedFunction> functionSet = new HashMap<>(); 
	
	protected void registerClasses(Class<?>... clss)
	{
		ArrayList<PreparedFunction> funcs = new ArrayList<>();
		
		for(Class<?> cls:clss)
		{
			Functions.collectFunctions(funcs, cls);
		}
		
		for(PreparedFunction f:funcs)
		{
			functionSet.put(f.getId(), f);
		}
	}
	
	public SaacFunctionCollection(Class... functionClasses)
	{
		registerClasses(functionClasses);
	}
	
	public Map<String, PreparedFunction> getFunctions()
	{
		return functionSet;
	}
	
	public <T> T createFunction(Class<T> ret, Map<String, Object> func)
	{
		return createFunction(ret, new DataObjectJavaImpl(func));
	}
	
	public <T> T createFunction(Class<T> ret, DataObject func)
	{
		return createFunction(ret, false, func);
	}
	
	public <T> T createFunction(Class<T> ret, boolean envDepend, Map<String, Object> func)
	{
		return createFunction(ret, envDepend, new DataObjectJavaImpl(func));
	}
	
	public <T> T createFunction(Class<T> ret, boolean envDepend, DataObject func)
	{
		SaacEnv env = SaacEnv.create(functionSet, func, ret);
		Object root = env.getRoot();
		if(!envDepend)
		{
			if(root instanceof SaacGetByWrapper)
			{
				root = ((SaacGetByWrapper)root).getBy(NullMap.instance);
			}
		}
		return (T) root;
	}

	public <T> T createEnvFunction(Class<T> cls, Map<String, Object> impl)
	{
		return createFunction(cls, true, impl);
	}
	
	public <T> T createEnvFunction(Class<T> cls, DataObject impl)
	{
		return createFunction(cls, true, impl);
	}
}
