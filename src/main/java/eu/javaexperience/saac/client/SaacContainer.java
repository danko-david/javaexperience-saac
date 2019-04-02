package eu.javaexperience.saac.client;

import java.util.ArrayList;
import java.util.List;

import eu.javaexperience.collection.CollectionTools;
import eu.javaexperience.datareprez.DataCommon;
import eu.javaexperience.datareprez.DataObject;
import eu.javaexperience.datareprez.DataReprezDialectTools;
import eu.javaexperience.datareprez.DataReprezTools;
import eu.javaexperience.datareprez.convertFrom.DataWrapper;
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
	
	public static SaacContainer createArray()
	{
		SaacContainer ret = new SaacContainer();
		ret.id = "";
		ret.content = "";
		return ret;
	}
	
	public SaacContainer addArgument(SaacContainer... args)
	{
		for(SaacContainer a:args)
		{
			this.args.add(a);
		}
		
		return this;
	}
	
	protected static final ObjectWithPropertyStorage<SaacContainer> PROPS = new ObjectWithPropertyStorage<>();
	static
	{
		PROPS.addExaminer("id", (e)->e.id);
		PROPS.addExaminer("content", (e)->e.content);
		PROPS.addExaminer("args", (e)->e.args);
	}
	
	protected static final DataWrapper DATA_WRAPPER = DataReprezTools.combineWrappers
	(
		DataReprezTools.WRAP_ARRAY_COLLECTION_MAP,
		DataReprezTools.WRAP_CLASS__OBJECT_WITH_PROPERTY
	);
	
	public DataObject serialize(DataCommon comm)
	{
		return (DataObject) DATA_WRAPPER.wrap(DATA_WRAPPER, comm, this);
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
