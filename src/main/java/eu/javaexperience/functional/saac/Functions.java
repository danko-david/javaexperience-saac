package eu.javaexperience.functional.saac;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;

import eu.javaexperience.annotation.FunctionDescription;
import eu.javaexperience.annotation.FunctionVariableDescription;
import eu.javaexperience.arrays.ArrayTools;
import eu.javaexperience.datareprez.DataObject;
import eu.javaexperience.datareprez.DataReprezTools;
import eu.javaexperience.datareprez.convertFrom.DataWrapper;
import eu.javaexperience.datareprez.jsonImpl.DataObjectJsonImpl;
import eu.javaexperience.generic.annotations.Ignore;
import eu.javaexperience.interfaces.ObjectWithProperty;
import eu.javaexperience.interfaces.ObjectWithPropertyStorage;
import eu.javaexperience.interfaces.simple.SimpleCall;
import eu.javaexperience.interfaces.simple.SimpleGet;
import eu.javaexperience.interfaces.simple.getBy.GetBy1;
import eu.javaexperience.interfaces.simple.getBy.GetBy2;
import eu.javaexperience.interfaces.simple.getBy.GetBy3;
import eu.javaexperience.interfaces.simple.publish.SimplePublish1;
import eu.javaexperience.reflect.Mirror;
import eu.javaexperience.rpc.bidirectional.BidirectionalRpcDefaultProtocol;
import eu.javaexperience.saac.SaacIgnore;
import eu.javaexperience.saac.SaacServerUnit;
import eu.javaexperience.saac.exceptions.SaacFunctionCreationException;
import eu.javaexperience.text.StringTools;

public class Functions
{
	public static class Param implements ObjectWithProperty
	{
		protected String name;
		protected Type type;
		protected String description;
		
		public String getName()
		{
			return name;
		}
		
		public String getDescription()
		{
			return description;
		}
		
		public Type getType()
		{
			return type;
		}
		
		public Param(Parameter at, FunctionDescription d, int i)
		{
			name = at.getName();
			type = at.getParameterizedType();
			
			description = name;
			if(null != d)
			{
				try
				{
					FunctionVariableDescription[] pds = d.parameters();
					if(null != pds)
					{
						FunctionVariableDescription pd = ArrayTools.accessIndexSafe(pds, i);
						if(null != pd)
						{
							name = pd.paramName();
							description = pd.description();
						}
					}
				}
				catch(Exception e)
				{
					throw new RuntimeException("Error in FunctionVariableDescription at parameter index: "+i);
				}
			}
		}
		
		public Param
		(
			AnnotatedType annotatedReturnType,
			FunctionDescription d
		)
		{
			//ret type
			name = "ret";
			type = annotatedReturnType.getType();
			description = "return";
			if(null != d)
			{
				FunctionVariableDescription f = d.returning();
				name = f.paramName();
				description = f.description();
			}
		}

		public static final Param[] emptyParamArray = new Param[0];
		public static Param[] wrap(Parameter[] params, FunctionDescription d)
		{
			ArrayList<Param> ret = new ArrayList<>();
			int i=0;
			
			for(Parameter p:params)
			{
				ret.add(new Param(p, d, i++));
			}
			
			return ret.toArray(emptyParamArray);
		}

		public AutocompleteProvider getAutocompleteProvider()
		{
			return null;
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
		
		protected static ObjectWithPropertyStorage<Param> PROPS = new ObjectWithPropertyStorage<>();
		
		static
		{
			PROPS.addExaminer("name", (e)-> e.name);
			PROPS.addExaminer("description", (e)-> e.description);
			PROPS.addExaminer("type", (e)-> serializeType(e.type));
		}
	}
	
	public static final DataWrapper TYPE_SERIALIZER = DataReprezTools.combineWrappers
	(
		SaacServerUnit.reflectTypeDataWrapper,
		BidirectionalRpcDefaultProtocol.DEFAULT_RPC_DATA_WRAPPER_WITH_CLASS
	);
	
	protected static DataObject serializeType(Type t)
	{
		return (DataObject) TYPE_SERIALIZER.wrap
		(
			TYPE_SERIALIZER,
			DataObjectJsonImpl.instane,
			t
		);
	}
	
	/*
	@FunctionDescription
	(
		functionDescription = "A küldő nevének vizsgálata.",
		parameters = {@FunctionVariableDescription(description = "Vizsgáló függvény", mayNull = false, paramName = "functionName", type = GetBy1.class)},
		returning = @FunctionVariableDescription(description="Megállapító függvény",mayNull=false,paramName="",type=GetBy1.class) 
	)
	*/
	public static class PreparedFunction
	{
		protected Method m;
		public PreparedFunction(Method m)
		{
			try
			{
				this.m = m;
				id = m.getDeclaringClass().getName()+"."+m.getName();
				name = StringTools.getSubstringAfterLastString(m.getName(), ".");
				
				description = name;
				
				FunctionDescription d = m.getAnnotation(FunctionDescription.class);
				if(null != d)
				{
					description = d.functionDescription();
				}
				
				returning = new Param(m.getAnnotatedReturnType(), d);
				
				
				args = Param.wrap(m.getParameters(), d);
			}
			catch(Exception e)
			{
				throw new RuntimeException("Error in FunctionDescription: "+m+"");
			}
		}
		
		protected String id;
		protected String name;
		protected String description;
		protected Param returning;
		protected Param[] args;
		
		public String getName()
		{
			return name;
		}
		
		public String getDescription()
		{
			return description;
		}
		
		public Param getReturning()
		{
			return returning;
		}
		
		public Param[] getArgs()
		{
			return ArrayTools.copy(args);
		}
		
		public Method getMethod()
		{
			return m;
		}
		
		public Object create(Object... arguments)
		{
			try
			{
				return m.invoke(null, arguments);
			}
			catch (Exception e)
			{
				SaacFunctionCreationException ex = new SaacFunctionCreationException("Can't create function",e);
				ex.function = this;
				ex.functionName = id;
				ex.arguments = arguments;
				throw ex;
			}
		}

		public String getId()
		{
			return id;
		}
		
		@Override
		public String toString()
		{
			return "PreparedFunction: "+m;
		}
	}
	
	protected static final Collection<Class<?>> FUNCTION_CLASS = new ArrayList<>();
	static
	{
		FUNCTION_CLASS.add(SimpleGet.class);
		FUNCTION_CLASS.add(SimplePublish1.class);
		FUNCTION_CLASS.add(SimpleCall.class);
		FUNCTION_CLASS.add(GetBy1.class);
		FUNCTION_CLASS.add(GetBy2.class);
		FUNCTION_CLASS.add(GetBy3.class);
	}
	
	public static void collectFunctions
	(
		Collection<PreparedFunction> funcs,
		Class cls
	)
	{
		final int req_mod = Modifier.STATIC | Modifier.PUBLIC;
		for(Method m:cls.getMethods())
		{
			if(0 != m.getAnnotationsByType(SaacIgnore.class).length || 0 != m.getAnnotationsByType(Ignore.class).length)
			{
				continue;
			}
			int mod = m.getModifiers();
			if((mod & req_mod) == req_mod)
			//if(FUNCTION_CLASS.contains(m.getReturnType()))
			{
				funcs.add(new PreparedFunction(m));
			}
		}
	}
}
