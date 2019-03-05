package eu.javaexperience.saac.client;

import java.util.ArrayList;
import java.util.List;

import eu.javaexperience.collection.CollectionTools;
import eu.javaexperience.interfaces.ObjectWithProperty;
import eu.javaexperience.interfaces.ObjectWithPropertyStorage;

public class SaacContainer implements ObjectWithProperty
{
	protected String id;
	
	protected String content;
	
	protected List<SaacContainer> args = new ArrayList<>();
	
	public static SaacContainer create(Class cls, String functionName, SaacContainer... args)
	{
		SaacContainer ret = new SaacContainer();
		ret.id = cls.getCanonicalName()+"."+functionName;
		ret.content = functionName;
		CollectionTools.inlineAdd(ret.args, args);
		return ret;
	}
	
	public static SaacContainer create(String str)
	{
		SaacContainer ret = new SaacContainer();
		ret.id = "";
		ret.content = str;
		return ret;
	}
	
	public static SaacContainer create(Enum e)
	{
		return create(e.name());
	}
	
	public static SaacContainer create(Object str)
	{
		SaacContainer ret = new SaacContainer();
		ret.id = "";
		ret.content = str.toString();
		return ret;
	}
	
	
	protected static final ObjectWithPropertyStorage<SaacContainer> PROPS = new ObjectWithPropertyStorage<>();
	static
	{
		PROPS.addExaminer("id", (e)->e.id);
		PROPS.addExaminer("content", (e)->e.content);
		PROPS.addExaminer("args", (e)->e.args);
	}
	
	@Override
	public Object get(String key)
	{
		return PROPS.get(this, key);
	}
	
	@Override
	public String[] keys()
	{
		return PROPS.keys();
	}

	public String getId()
	{
		return id;
	}
	
	public String getContent()
	{
		return content;
	}
	
	public List<SaacContainer> getContainers()
	{
		return args;
	}
}
