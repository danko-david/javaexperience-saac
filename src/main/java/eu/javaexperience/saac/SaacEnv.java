package eu.javaexperience.saac;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import eu.javaexperience.collection.enumerations.EnumTools;
import eu.javaexperience.datareprez.DataArray;
import eu.javaexperience.datareprez.DataCommon;
import eu.javaexperience.datareprez.DataObject;
import eu.javaexperience.datareprez.convertFrom.DataLike;
import eu.javaexperience.exceptions.UnimplementedCaseException;
import eu.javaexperience.functional.saac.Functions.Param;
import eu.javaexperience.functional.saac.Functions.PreparedFunction;
import eu.javaexperience.interfaces.simple.SimpleGet;
import eu.javaexperience.interfaces.simple.getBy.GetBy1;
import eu.javaexperience.interfaces.simple.getBy.GetBy2;
import eu.javaexperience.interfaces.simple.publish.SimplePublish1;
import eu.javaexperience.reflect.CastTo;
import eu.javaexperience.reflect.Mirror;
import eu.javaexperience.reflect.PrimitiveTools;
import eu.javaexperience.saac.exceptions.SaacFunctionCreationException;
import eu.javaexperience.text.StringTools;

public class SaacEnv
{
	protected Map<String, PreparedFunction> functionSet;
	protected Class<?> rootAcceptType;
	
	
	public SaacEnv(Map<String, PreparedFunction> functionSet, Class<?> accept)
	{
		this.functionSet = functionSet;
		this.rootAcceptType = accept;
	}

	protected Map<Object, Object[]> path = new IdentityHashMap<>();
	
	public static interface SaacClosureInfo
	{
		public Object[] getArgs();
	}
	
	public static interface EnvAdapter
	{
		public Map<String, Object> subEnv(Map<String, Object> env);
	}
	
	public static interface SaacGetByWrapper<R, A> extends SaacClosureInfo, GetBy1<R, A>{}
	public static interface SaacSimplePublishWrapper<R> extends SaacClosureInfo, SimplePublish1<R>{}

	
	protected Object root;
	
	//TODO encapsule
	protected static ThreadLocal<Map<String, PreparedFunction>> THREAD_CONTEXT = new ThreadLocal<>();
	
	protected static class ContextFunctionSet
	{
		protected Map<String, PreparedFunction> functionSet;
		protected boolean master = false;
		
		public ContextFunctionSet(Map<String, PreparedFunction> functionSet, boolean master)
		{
			this.functionSet = functionSet;
			this.master = false;
		}
		
	}
	
	/**
	 * Getting/setting the thread function set
	 * @param functionSet2 
	 * */
	protected static ContextFunctionSet getOrAccumulateProperFunctionSet(Map<String, PreparedFunction> functionSet)
	{
		//trying to get a previous setted function set
		if(null == functionSet)
		{
			Map<String, PreparedFunction> ret = THREAD_CONTEXT.get();
			if(null == ret)
			{
				throw new RuntimeException("There's no function set available for this context.");
			}
			else
			{
				return new ContextFunctionSet(ret, false);
			}
		}
		else
		{
			//trying to accumulate
			
			if(null == THREAD_CONTEXT.get())
			{
				THREAD_CONTEXT.set(functionSet);
				
				//this will be cleaned up if the root parser returned.
				return new ContextFunctionSet(functionSet, true);
			}
			else
			{
				//returning the original one.
				return new ContextFunctionSet(functionSet, false);
			}
		}
	}
	
	protected static void cleanupFunctionSet(ContextFunctionSet funcs)
	{
		if(null != funcs)
		{
			if(funcs.master)
			{
				THREAD_CONTEXT.set(null);
			}
		}
	}
	
	public static SaacEnv create
	(
		Map<String, PreparedFunction> functionSet,
		DataObject obj,
		Class<?> accept
	)
	{
		SaacEnv env = new SaacEnv(functionSet, accept);
		env.parse(obj);
		return env;
	}
	
	public void parse
	(
		DataObject obj
	)
	{
		ContextFunctionSet fset = getOrAccumulateProperFunctionSet(functionSet);
		functionSet = fset.functionSet;
		try
		{
			root = parse(obj, rootAcceptType);
		}
		finally
		{
			cleanupFunctionSet(fset);
		}
	}

	protected Object parse(DataObject obj, Type acceptType)
	{
		Class accept = Mirror.extracClass(acceptType);
		//{"id":"","content":"","parent":null,"args":[]}
		{
			String id = extractString(obj, SaacTools.SAAC_FIELD_ID);
			if(null != id)
			{
				PreparedFunction pp = functionSet.get(id);
				if(null == pp)
				{
					SaacFunctionCreationException ex = new SaacFunctionCreationException("Function doesn't exists: "+id);
					ex.functionName = id;
					throw ex;
				}
				
				DataArray args = obj.getArray("args");
				Object[] call = new Object[pp.getArgs().length];
				Param[] ps = pp.getArgs();
				
				boolean[] paramWraps = new boolean[pp.getArgs().length];
				boolean wrapRet = false;
				
				Class retClass = Mirror.extracClass(pp.getReturning().getType());
				
				if(null != accept && null != retClass)
				{
					//if method "returning" void we don't have to cast to SimplePublish
					
					
					//if method returns anything, we need to cast with GetBy1
					//even if it's accetable, because only in that case will be wrapped with GetBy1
					
					//from the other hand this function might be wrapped if any of it's argument evaluated in runtime.
					wrapRet = !Mirror.isVoid(retClass) && !SimplePublish1.class.isAssignableFrom(accept) 
							//!accept.isAssignableFrom(retClass)
					;
				}
				
				List<Object> varargs = null;
				if(ps.length > 0 && Mirror.extracClass(ps[ps.length-1].getType()).isArray())
				{
					varargs = new ArrayList<>();
				}
				
				for(int i=0;i<args.size();++i)
				{
					DataLike dc = (DataLike) args.get(i);
					
					int acc = i;
					
					boolean inVaridaicRange = false;
					
					//is there a better way to check variadic?
					if(i >= ps.length-1 && null != varargs)
					{
						inVaridaicRange = true;
						acc = ps.length-1;
					}
					
					Class reqType = Mirror.extracClass(ps[acc].getType());
					
					Class varType = reqType;
					if(inVaridaicRange)
					{
						varType = reqType.getComponentType();
					}
					
					if(reqType.isArray() && dc instanceof DataObject)
					{
						DataObject d = (DataObject) dc;
						if(!isUseful(d.opt(SaacTools.SAAC_FIELD_ID)) && !isUseful(d.opt(SaacTools.SAAC_FIELD_CONTENT)))
						{
							dc = d.getArray("args");
						}
					}
					
					Object add = null; 
					switch (dc.getDataReprezType())
					{
						case ARRAY:
							add = parseArray
							(
								functionSet,
								(DataArray) dc,
								reqType.getComponentType()
							);
							break;
						
						case OBJECT:
						case CLASS_OBJECT:
						case RESOURCE:
							
							add = create
							(
								functionSet,
								(DataObject) dc,
								varType
							).root;
							
							break;
							
						case NULL:
						case PRIMITIVE:
						default:
							throw new UnimplementedCaseException(dc.getDataReprezType());
					}
					
					add = postWrapFilter(varType, add);
					
					if(null != varargs)
					{
						if(i >= ps.length-1)
						{
							varargs.add(add);
						}
					}
					else
					{
						call[i] = add;
					}
				}
				
				if(null != varargs)
				{
					if
					(
						varargs.size() == 1 &&
						null != varargs.get(0) &&
						Mirror.extracClass(ps[ps.length-1].getType()).isAssignableFrom(varargs.get(0).getClass())
					)
					{
						
						call[ps.length-1] = varargs.get(0);
					}
					else
					{
						Class<?> vr = Mirror.extracClass(ps[ps.length-1].getType());
						Object[] as = tryExtractAsRequested(vr, varargs);
						
						call[ps.length-1] = postWrapFilter(vr, as);
					}
				}
				
				boolean needWrap = 
						false;
						//wrapRet;
				
				if(!needWrap)
				{
					for(int i=0;i<paramWraps.length;++i)
					{
						needWrap |= paramWraps[i];
						if(needWrap)
						{
							break;
						}
					}
				}
				
				//accept is null only int the case of the root element
				if(needWrap && null != accept && !Mirror.isVoid(accept))
				{
					//wrapping the return value if we have to wrap any of input argument and caller requires return value
					wrapRet = true;
				}
				
				
				if(needWrap)
				{
					return wrapForRuntime(pp, wrapRet, paramWraps, call);
				}
				else
				{
					return pp.create(call);
				}
			}
		}
		
		if(null != accept)
		{
			final String content = extractString(obj, SaacTools.SAAC_FIELD_CONTENT);
			if(null != content)
			{
				if(accept.isEnum())
				{
					return EnumTools.getByName((Class) accept, content);
				}
	
				CastTo to = CastTo.getCasterRestrictlyForTargetClass(accept);
				if(null != to)
				{
					return to.cast(content);
				}
				
				if(content.startsWith("$"))
				{
					return new SaacGetByWrapper<Object, Map<String,Object>>()
					{
						@Override
						public Object getBy(Map<String, Object> a)
						{
							return a.get(content);
						}
						
						@Override
						public String toString()
						{
							return "Scope getter: "+super.toString();
						}
	
						@Override
						public Object[] getArgs()
						{
							return new Object[]{content};
						}
					};
				}
				else
				{
					return new SaacGetByWrapper<Object, Object>()
					{
						@Override
						public Object getBy(Object a)
						{
							return content;
						}
						
						@Override
						public String toString()
						{
							return "Scope getter: "+super.toString();
						}
	
						@Override
						public Object[] getArgs()
						{
							return new Object[]{content};
						}
					};
				}
			}
		}
		
		return null;
	}
	
	public static Object[] tryExtractAsRequested(Class req, List obj)
	{
		try
		{
			return obj.toArray((Object[]) Array.newInstance(req.getComponentType(), obj.size()));
		}
		catch(Exception e)
		{
			return obj.toArray();
		}
	}
	
	public static Object postWrapFilter(Class reqType, Object add)
	{
		//comaptible?
		if(null != reqType && null != add)
		{
			if(PrimitiveTools.isPrimitiveClass(reqType))
			{
				reqType = PrimitiveTools.toObjectClassType(reqType, reqType);
			}
			
			//add direct wrap: GetBy1<T,?> (or SimplePublish<T>)required and a T given
			boolean wrapParam = !reqType.isAssignableFrom(add.getClass()) || SaacSimplePublishWrapper.class.isAssignableFrom(add.getClass());
			wrap:if(wrapParam)
			{
				Object o = tryWrapSameOfArray(reqType, add);
				if(null != o)
				{
					add = o;
					break wrap;
				}
				
				o = tryWrapConstantAsSourceFunction(reqType, add);
				if(null != o)
				{
					add = o;
					//not needed to wrap in runtime, we have been done that.
					break wrap;
				}
				
				//need to extact or evaluate in runtime, or unamingously: it will be extracted in runtime
			}
		}
		
		return add;
	}
	
	
	public static Object tryWrapSameOfArray(Class reqType, Object object)
	{
		if(reqType.isArray())
		{
			Class cls = reqType.getComponentType();
			if(cls.isAssignableFrom(object.getClass()))
			{
				Object[] ret = (Object[]) Array.newInstance(cls, 1);
				ret[0] = object;
				return ret;
			}
		}
		return null;
	}

	public static Object tryWrapConstantAsSourceFunction(Type reqType, Object value)
	{
		Class req = Mirror.extracClass(reqType);
		if(req.isAssignableFrom(SimpleGet.class))
		{
			//TODO when it's raw just let them go, if Generic bounding specified
			// check them, on mismatch throw exception
			return new SimpleGet()
			{
				@Override
				public Object get()
				{
					return value;
				}
			};
		}
		else if(req.isAssignableFrom(GetBy1.class))
		{
			return new GetBy1()
			{
				@Override
				public Object getBy(Object o)
				{
					return value;
				}
			};
		}
		else if(req.isAssignableFrom(GetBy2.class))
		{
			return new GetBy2()
			{
				@Override
				public Object getBy(Object o, Object p)
				{
					return value;
				}
			};
		}
		//getBy3 so on
		
		return null;
		
	}
	
	protected static boolean isUseful(Object o)
	{
		return null != o && !StringTools.isNullOrTrimEmpty(o.toString());
	}
	
	protected static <T> Object processArray
	(
		PreparedFunction pp,
		Param p,
		Object[] at,
		Map<String, Object> env
	)
	{
		Class<T> rt = (Class<T>) Mirror.extracClass(p.getType()).getComponentType();
		ArrayList<Object> ret = new ArrayList<>();
		boolean allFits = true;
		
		for(int i=0;i<at.length;++i)
		{
			Object add = processSingleParam(pp, p, at[i], env);
			ret.add(add);
			if(null != add)
			{
				allFits &= rt.isAssignableFrom(add.getClass());
			}
		}
		
		
		if(allFits)
		{
			return ret.toArray((T[]) Array.newInstance(rt, 0));
		}
		
		return ret.toArray();
		
	}
	
	protected static Object processSingleParam
	(
		PreparedFunction pp,
		Param p,
		Object at,
		Map<String, Object> env
	)
	{
		if(null == at)
		{
			return null;
		}
		
		if(at instanceof EnvAdapter)
		{
			env = ((EnvAdapter) at).subEnv(env);
		}
		
		Class cls = Mirror.extracClass(p.getType());
		if(cls.isAssignableFrom(at.getClass()))
		{
			return at;
		}
		
		if(at instanceof SimpleGet)
		{
			at = ((SimpleGet) at).get();
		}
		else if(at instanceof	//SaacGetByWrapper)
								GetBy1)
		{
			at = ((GetBy1) at).getBy(env);
		}
		else if(at instanceof	//SaacSimplePublishWrapper)
								SimplePublish1)
		{
			((SimplePublish1) at).publish(env);
			at = null;
		}
		
		if(null == at)
		{
			return null;
		}
		
		if(at instanceof String)
		{
			String str = (String) at;
			
			if(cls.isEnum())
			{
				return EnumTools.getByName(cls, str);
			}
			else
			{
				CastTo ct = CastTo.getCasterRestrictlyForTargetClass(cls);
				if(null != ct)
				{
					return ct.cast(str);
				}
				else
				{
					return env.get(str);
				}
			}
		}
		//arrays
		else if(at.getClass().isArray())
		{
			return processArray(pp, p, (Object[]) at, env);
		}
		else
		{
			return at;
		}
	}

	protected static String extractString(DataObject obj, String key)
	{
		if(obj.has(key))
		{
			//can be a number or boolean
			Object o = obj.get(key);
			
			if(null == o)
			{
				return null;
			}
			
			String ret = null;
			
			if(o instanceof DataCommon)
			{
				byte[] b = ((DataCommon)o).toBlob();
				if(null != b)
				{
					ret = new String(b);
				}
			}
			else
			{
				ret = o.toString();
			}
			
			if(!StringTools.isNullOrTrimEmpty(ret))
			{
				return ret;
			}
		}
		return null;
	}

	
	protected static Object processArgs
	(
		final PreparedFunction pp,
		boolean wrapRet,
		boolean[] paramWraps,
		final Object[] args,
		final Map<String,Object> env
	)
	{
		Object[] cre = new Object[args.length];
		
		for(int i=0;i<args.length;++i)
		{
			if(!paramWraps[i])
			{
				cre[i] = args[i];
				continue;
			}
			
			cre[i] = processSingleParam(pp, pp.getArgs()[i], args[i], env);
		}
		
		try
		{
			return pp.create(cre);
		}
		catch(Exception e)
		{
			SaacFunctionCreationException t = new SaacFunctionCreationException("Can't create function: "+pp.getName(), e);
			t.functionName = pp.getName();
			t.arguments = cre;
			t.function = pp;
			throw t;
		}
	}
	
	public static <T> Object wrapForRuntime
	(
		final PreparedFunction pp,
		boolean wrapRet,
		boolean[] paramWraps,
		final Object[] args
	)
	{
		if(wrapRet)
		{
			return new SaacGetByWrapper<Object, Map<String, Object>>()
			{
				@Override
				public Object getBy(Map<String, Object> env)
				{
					return processArgs(pp, wrapRet, paramWraps, args, env);
				}
				
				@Override
				public String toString()
				{
					return "Runtime Scope getter (GetBy1): "+super.toString();
				}

				@Override
				public Object[] getArgs()
				{
					return new Object[]{pp, wrapRet, paramWraps, args};
				}
			};
		}
		else
		{
			return new SaacSimplePublishWrapper<Map<String,Object>>()
			{
				@Override
				public void publish(Map<String, Object> env)
				{
					processArgs(pp, wrapRet, paramWraps, args, env);
				}
				
				@Override
				public String toString()
				{
					return "Runtime Wrap (SimplePublish): "+super.toString();
				}
				
				@Override
				public Object[] getArgs()
				{
					return new Object[]{pp, wrapRet, paramWraps, args};
				}
			};
		}
	}
	
	protected static <T> Object parseArray
	(
		Map<String, PreparedFunction> functionSet,
		DataArray arr,
		Class<?> accept
	)
	{
		int size = arr.size();
		ArrayList<Object> coll = new ArrayList<>();
		
		boolean allFits = true;
		
		for(int i=0;i<size;++i)
		{
			Object add = create(functionSet, arr.getObject(i), accept).root;
			if(null != add)
			{
				allFits &= accept.isAssignableFrom(add.getClass());
			}
			coll.add(add);
		}
		
		if(allFits)
		{
			return coll.toArray((T[]) Array.newInstance(accept, 0));
		}
		
		return coll.toArray();
	}

	//TODO encapsule
	protected static ThreadLocal<Stack<Map<String, Object>>> ENV = new ThreadLocal<Stack<Map<String, Object>>>()
	{
		protected java.util.Stack<java.util.Map<String,Object>> initialValue()
		{
			return new Stack<>();
		};
	};
	
	public static synchronized Map<String, Object> getCurrentEnv()
	{
		Stack<Map<String, Object>> stack = ENV.get();
		return stack.peek();
	}
	
	public static synchronized void pushEnv(Map<String, Object> env)
	{
		Stack<Map<String, Object>> stack = ENV.get();
		stack.push(env);
	}

	public static synchronized Map<String, Object> popEnv(Map<String, Object> env)
	{
		Stack<Map<String, Object>> stack = ENV.get();
		return stack.pop();
	}

	public Object getRoot()
	{
		return root;
	}
}
