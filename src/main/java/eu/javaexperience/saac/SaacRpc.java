package eu.javaexperience.saac;

import static eu.javaexperience.log.LogLevel.*;
import static eu.javaexperience.log.LoggingTools.*;

import java.io.PrintWriter;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import eu.javaexperience.annotation.FunctionDescription;
import eu.javaexperience.annotation.FunctionVariableDescription;
import eu.javaexperience.collection.map.SmallMap;
import eu.javaexperience.datareprez.DataObject;
import eu.javaexperience.functional.saac.AutocompleteProvider;
import eu.javaexperience.functional.saac.FunctionCreator;
import eu.javaexperience.functional.saac.Functions.Param;
import eu.javaexperience.functional.saac.Functions.PreparedFunction;
import eu.javaexperience.interfaces.simple.SimpleCall;
import eu.javaexperience.interfaces.simple.publish.SimplePublish1;
import eu.javaexperience.log.JavaExperienceLoggingFacility;
import eu.javaexperience.log.LogLevel;
import eu.javaexperience.log.Loggable;
import eu.javaexperience.log.Logger;
import eu.javaexperience.log.LoggingTools;
import eu.javaexperience.log.ThreadLocalHookableLogFacility;
import eu.javaexperience.reflect.Mirror;
import eu.javaexperience.resource.ReferenceCounted;
import eu.javaexperience.rpc.JavaClassRpcFunctions;
import eu.javaexperience.rpc.SimpleRpcRequest;
import eu.javaexperience.rpc.SimpleRpcSession;
import eu.javaexperience.verify.LanguageTranslatableValidationEntry;
/**
 * TODO:
 * 		- nice UI:
 * 			//- horizontal/vertical agruments
 * 			- close function containers, minimize content AND expand on hover
 * 
 * 		- autocomplete:
 * 			- Enum
 * 			- Varaible
 * 		
 * 		- validation:
 * 			- assemble_time/runtime/error
 * 			- trace function position in hierarchy for error reporting/debug purposes
 * 		
 * 		- project mode usage:
 * 			- specify project dir (browse JS API)
 * 			- module version tracking
 * 			- include functions
 * 		
 * 		- macro support:
 * 			define small blocks and make appear during autocomplete
 * 			(Store macros in JSON, beacuse we must be capable to restore editor state from json)
 * */
public class SaacRpc
{
	protected static final Logger LOG = JavaExperienceLoggingFacility.getLogger(new Loggable("SaacRpc"));
	
	public static final JavaClassRpcFunctions<SimpleRpcRequest> DISPATCH = new JavaClassRpcFunctions<SimpleRpcRequest>(SaacRpc.class);
	
/******************************* RPC functions ********************************/
	
	@FunctionDescription
    (
    	functionDescription = "Sets the given parameters on the current Saac session, hence this values can be used int the PARAM variable."
    						+ "\n This feature has effect only in execution mode, not affects the stored functions.",
    	parameters =
    	{
    		@FunctionVariableDescription(description="Parameters in js object/map/associative array style.",mayNull=false,paramName="PARAM",type=Map.class)
    	},
    	returning = @FunctionVariableDescription(description="",mayNull=false,paramName="",type=FunctionDescriptor.class) 
    )
	public static void setQueryParams(SimpleRpcRequest req, Map<String, Object> ps)
	{
		SimpleRpcSession sess = (SimpleRpcSession) req.getRpcSession();
		
		if(null != ps)
		{
			sess.put("PARAM", ps);
		}
	}

	@FunctionDescription
	(
		functionDescription = "List all available functions.",
		parameters = {},
		returning = @FunctionVariableDescription(description="Collection of function descriptors.",mayNull=false,paramName="",type=FunctionDescriptor[].class) 
    )
	public static FunctionDescriptor[] listFunctions(SimpleRpcRequest req)
	{
		Map<String, PreparedFunction> fset = getSessionFunctionSet((SimpleRpcSession) req.getRpcSession());
		Collection<PreparedFunction> fs = fset.values();
		
		FunctionDescriptor[] ret = new FunctionDescriptor[fs.size()];
		int i = 0;
		for(PreparedFunction f:fs)
		{
			ret[i++] = new FunctionDescriptor(f);
		}
		
		return ret;
	}
	
	@FunctionDescription
	(
		functionDescription = "Offers functions/enums that can be accepted by the specified function's specified argument (by index).\n"
							+ "Currently Saac RPC not provide type specifications from the returning and parameter types, and can't provide \n"
							+ "type bounding instructions (eg is KeyVal<String, HashMap<Boolean,GetBy1<String,Object>>> suitable for ? extends Entry<String, ? extends Map<Boolean,GetBy1>>)\n"
							+ "So in this version, you should get an offer. Eg.: if user wants to edit the 0th argument of function.aggregates.countUsers(GetBy1<Boolean,User> criteria)\n you might\n"
							+ "call this function: offerForType(\\\"function.aggregates.countUsers\\\", 0, false, \\\"\\\", 0,0) to get the list of suitable functions that returns GetBy1<Boolean, User> functionObject.\n"
							+ "Not that if the target type is an enum, enumeration elements and functions that return that class of enum is returned.\n"
							+ "Also note that an argument might have custom AutocompleteProvider, which make possible to promt application specific strings, eg:\n"
							+ "getRedisInteger(String key) [key might be prompted as string]", 
		parameters =
		{
			@FunctionVariableDescription(description="Id of the receiver function.",mayNull=false,paramName="functionId",type=String.class),
			@FunctionVariableDescription(description="Index of the parent's paramter count.",mayNull=false,paramName="index",type=int.class),
			@FunctionVariableDescription(description="Is the function variadic?",mayNull=false,paramName="isVariadic",type=boolean.class),
			@FunctionVariableDescription(description="Search term, result may be filtered by this fraction of string.",mayNull=true,paramName="",type=String.class),
			@FunctionVariableDescription(description="Where the user's cursor stands in the given term.",mayNull=true,paramName="cursorStartIndex",type=Integer.class),
			@FunctionVariableDescription(description="",mayNull=true,paramName="cursorEndIndex",type=Integer.class)
		},
		returning = @FunctionVariableDescription(description="Collection of function descriptors.",mayNull=false,paramName="",type=FunctionDescriptor[].class) 
    )
	public static Object offerForType
	(
		SimpleRpcRequest req,
		String functionId,
		Integer index,
		Boolean variadic,//TODO determine variadic if null given
		String term,
		Integer cursorStartIndex,
		Integer cursorEndIndex
	)
	{
		ArrayList<Object> ret = new ArrayList<>();
		
		Map<String, PreparedFunction> INDEX = getSessionFunctionSet((SimpleRpcSession) req.getRpcSession());
		
		PreparedFunction parentFunction = INDEX.get(functionId);
		if(null == parentFunction)
		{
			return null;
		}
		
		Param[] ps = parentFunction.getArgs();
		if(0 == ps.length)
		{
			return Mirror.emptyObjectArray;
		}
		
		if(ps.length <= index)
		{
			index = ps.length-1;
		}
		

		Param p = ps[index];
		AutocompleteProvider acp = p.getAutocompleteProvider();
		if(null != acp)
		{
			if(null == cursorStartIndex)
			{
				cursorStartIndex = 0;
			}
			
			if(null == cursorEndIndex)
			{
				cursorEndIndex = 0;
			}
			
			if(cursorEndIndex < cursorStartIndex)
			{
				cursorEndIndex = cursorStartIndex;
			}
			
			acp.offerElement(term, cursorStartIndex, cursorEndIndex);
		}
		
		offerTypeV1(ret, INDEX, p, Boolean.TRUE == variadic);
		return ret.toArray();
	}
	
	protected static void offerTypeV1
	(
		List<Object> offers,
		Map<String, PreparedFunction> INDEX,
		Param p,
		boolean variadic
	)
	{
		offerTypeV1(offers, INDEX, p.getType(), variadic);
	}
	
	protected static void offerTypeV1
	(
		List<Object> offers,
		Map<String, PreparedFunction> INDEX,
		Type req,
		boolean variadic
	)
	{
		Class reqType = Mirror.extracClass(req);
		if(variadic)
		{
			req = reqType.getComponentType();
			reqType = reqType.getComponentType();
		}
		
		/** TODO why
		if(SimplePublish1.class == reqType)
		{
			return;
		}
		*/
		if(reqType.isEnum())
		{
			for(Enum e:((Class<Enum>)reqType).getEnumConstants())
			{
				offers.add(new _enum(e));
			}
		}
		
		if(req instanceof GenericArrayType)
		{
			req = ((GenericArrayType) req).getGenericComponentType();
		}
		
		//if(isUnbounded(toRet))
		{
		//	return null;
		}
		//if parent function accepts SimplePublish, we can offer any function
		//(done in client side)
		//the offered function will be wrapped at assemble time
		/*if(SimplePublish1.class.isAssignableFrom(toRet))
		{
			return null;
		}*/
		
		Collection<PreparedFunction> pps = INDEX.values();
		
		for(PreparedFunction pp:pps)
		{
			Type fret = pp.getReturning().getType();
			
			if(isAcceptable(req, fret))
			{
				offers.add(new FunctionDescriptor(pp));
			}
		}
	}
	
	@FunctionDescription
	(
		functionDescription = "Offers functions/enums that can be accepted by the specified function's specified argument (by index).\n"
							+ "This version is take care about type "
							+ "Also note that an argument might have custom AutocompleteProvider, which make possible to promt application specific strings, eg:\n"
							+ "getRedisInteger(String key) [key might be prompted as string]", 
		parameters =
		{
			@FunctionVariableDescription(description="The function tree from the context.",mayNull=false,paramName="function",type=String.class),
			@FunctionVariableDescription(description="UUID of the receiver function in the function tree.",mayNull=false,paramName="uuid",type=String.class),
			@FunctionVariableDescription(description="Index of the parent's paramter count.",mayNull=false,paramName="index",type=int.class)
		},
		returning = @FunctionVariableDescription(description="Collection of function descriptors.",mayNull=false,paramName="",type=FunctionDescriptor[].class) 
    )
	public static Object[] accurateOfferV1
	(
		String tree,
		String functionUUID,
		int index
	)
	{
		//TODO parse tree
		//TODO propagate types
		//TODO select function
		
		//TODO return original offerType
		
		
		return null;
	}
	
	@FunctionDescription
	(
		functionDescription = "Validates every function connections (This function not implmented yet.). Because users can give any kind of functions as\n"
				+ " an argument of other function, they can broke the \"connections\". \n"
				+ "Warning levels:\n"
				+ "\t- NOTICE: If parameter matcher exacly the requested type.\n"
				+ "\t- INFO: If parameter is wrapped automatically eg.: requested function is a `void function(T p1, T p2)` and the given method is `void function()` which can be assembled by relaying the call to the requested type without parameters\n"
				+ "\t- WARNING: Saac supports environment variables that can be given anywhere as a given parameter, and will be fetched and used in runtime."
				+ "\t- ERROR: On incompatible function composition (Requested and given functions are incompatible types.).",
		parameters =
		{
			@FunctionVariableDescription(description="Serialized function.",mayNull=false,paramName="function",type=DataObject.class),
		},
		returning = @FunctionVariableDescription(description="Validation results for every entry.",mayNull=false,paramName="",type=FunctionDescriptor[].class) 
    )
	public static Map<String, List<LanguageTranslatableValidationEntry>> compileAndCheck
	(
		SimpleRpcRequest req,
		DataObject function
	)
	{
		//createUnit((DataObject) o);
		return new SmallMap<>();
	}
	
	@FunctionDescription
	(
		functionDescription = "Executes the constructed function.",
		parameters =
		{
			@FunctionVariableDescription(description="Serialized function.",mayNull=false,paramName="function",type=DataObject.class),
		},
		returning = @FunctionVariableDescription(description="Return value of the root function",mayNull=false,paramName="",type=FunctionDescriptor[].class) 
    )
	public static Object execute(SimpleRpcRequest req, DataObject fs)
	{
		SimpleRpcSession rpcSess = (SimpleRpcSession) req.getRpcSession();
		SaacSession saac_sess = getOrCreateSaacSession(rpcSess);
		
		ReferenceCounted<PrintWriter> log = saac_sess.setContextLogger();
		
		Map<String, Object> env = new SmallMap<>();
		
		env.put("PARAM", rpcSess.get("PARAM"));

		env.put("SAAC_SESSION", saac_sess);
		Map<String, PreparedFunction> INDEX = (Map<String, PreparedFunction>)((SimpleRpcSession) req.getRpcSession()).get(SAAC_FUNCTION_SET_KEY);
		
		long t0 = System.currentTimeMillis();
		try
		{
			LoggingTools.tryLogFormat
			(
				LOG,
				LogLevel.INFO,
				"Saac execution starting"
			);
			
			if(!execute(INDEX, fs, env, log))
			{
				throw new RuntimeException("The root element is not runnable.");
			}
		}
		catch(Throwable e)
		{
			Throwable t = e;
			while(t instanceof InvocationTargetException || t instanceof RuntimeException)
			{
				t = t.getCause();
			}
			
			LoggingTools.tryLogFormatException
			(
				LOG,
				LogLevel.ERROR,
				t,
				"Saac execution exception:\n"
			);
			
			if(null == t)
			{
				throw e;
			}
			else
			{
				Mirror.throwSoftOrHardButAnyway(e);
			}
		}
		finally
		{
			LoggingTools.tryLogFormat
			(
				LOG,
				LogLevel.INFO,
				"Saac execution ended after %d ms",
				System.currentTimeMillis() - t0
			);
		}
		return true;
	}
	
/********************************* RPC classes *****************************************/
	
	public static class FunctionDescriptor
	{
		public static FunctionDescriptor[] emptyFuncArray = new FunctionDescriptor[0];
		public FunctionDescriptor(FunctionCreator cre)
		{
			this.name = cre.getName();
			this.description = cre.getDescription();
		}
		
		public String id;
		public String name;
		public String description;
		public Param returning;
		public Param[] arguments;
		
		public FunctionDescriptor(PreparedFunction f)
		{
			id = f.getMethod().getDeclaringClass().getName()+"."+f.getMethod().getName();
			name = f.getName();
			description = f.getDescription();
			this.returning = f.getReturning();
			this.arguments = f.getArgs();
		}
		
		@Override
		public String toString()
		{
			return "FunctionDescriptor: "+id;
		}
	}
	
	protected static class _enum
	{
		public String name;
		public int ordinal;
		public String enumClass;
		public _enum(Enum e)
		{
			this.name = e.name();
			this.ordinal = e.ordinal();
			this.enumClass = e.getClass().getName();
		}
	}
	
/********************************* helper functions *****************************************/
	
	public static boolean isAcceptable(Type targetType, Type sourceType)
	{
		if(isUnbounded(targetType))
		{
			return true;
		}
		
		if(isUnbounded(sourceType))
		{
			return true;
		}
	
		if
		(
			targetType instanceof Class
			&&
			sourceType instanceof Class
		)
		{
			return targetType == sourceType;
			//return ((Class) targetType).isAssignableFrom(((Class)sourceType));
		}
		
		if
		(
			targetType instanceof ParameterizedType
			&&
			sourceType instanceof ParameterizedType
		)
		{
			ParameterizedType src = ((ParameterizedType)sourceType);
			ParameterizedType target = ((ParameterizedType)targetType);
			
			if(!isAcceptable(target.getRawType(), src.getRawType()))
			{
				return false;
			}
			
			Type[] sa = src.getActualTypeArguments();
			Type[] ta = target.getActualTypeArguments();
			if(sa.length != ta.length)
			{
				return false;
			}
			
			for(int i=0;i<sa.length;++i)
			{
				if(!isAcceptable(ta[i], sa[i]))
				{
					return false;
				}
			}
			
			return true;
		}
		
		
		if
		(
			targetType instanceof GenericArrayType
			&&
			sourceType instanceof GenericArrayType
		)
		{
			return isAcceptable
			(
				((GenericArrayType) targetType).getGenericComponentType(),
				((GenericArrayType) sourceType).getGenericComponentType()
			);
		}
		
		//type(CTX) && type(CTX)
		if
		(
			targetType instanceof TypeVariable
		)
		{
			Type[] ts = ((TypeVariable) targetType).getBounds();
			if(ts.length == 1)
			{
				return isAcceptable(ts[0], sourceType);
			}
			else
			{
				//TODO foreach 
			}
		}
		
		if(targetType instanceof ParameterizedType)
		{
			Type act = ((ParameterizedType) targetType).getRawType();
			return isAcceptable(act, sourceType);
		}
		
		if(targetType instanceof WildcardType)
		{
			WildcardType wt = (WildcardType) targetType;
			Type[] b = null;
			if(null != (b = wt.getLowerBounds()))
			{
				for(Type t:b)
				{
					if(!isAcceptable(t, sourceType))
					{
						return false;
					}
				}
			}
			
			if(null != (b = wt.getUpperBounds()))
			{
				for(Type t:b)
				{
					if(!isAcceptable(sourceType, t))
					{
						return false;
					}
				}
			}
			
			return true;
		}
		
		/*if(targetType instanceof TypeVariable)
		{
			((TypeVariable) targetType).getBounds();
		}
		
		if(sourceType instanceof TypeVariable)
		{
			((TypeVariable) sourceType).getBounds();
		}
		
		
		
		if(targetType instanceof WildcardType)
		{
			((WildcardType)targetType).getLowerBounds();
		}
		
		if(sourceType instanceof WildcardType)
		{
			((WildcardType)targetType).getLowerBounds();
		}
		*/
		//((WildcardType)targetType).
		
		//GenericArrayTypeImpl ParameterizedTypeimpl, WildcardTypeimpl
		
		return false;
	}
	
	public static boolean isUnbounded(Type[] tt)
	{
		for(int i=0;i<tt.length;++i)
		{
			if(!isUnbounded(tt[i]))
			{
				return false;
			}
		}
		return true;
	}
	
	public static boolean isUnbounded(Type t)
	{
		if(t instanceof Class)
		{
			return ((Class)t).isAssignableFrom(Object.class);
		}
		
		if(t instanceof ParameterizedType)
		{
			return
					isUnbounded(((ParameterizedType)t).getRawType())
					&&
					isUnbounded(((ParameterizedType)t).getActualTypeArguments());
		}
		
		
		if(t instanceof GenericArrayType)
		{
			return isUnbounded(((GenericArrayType) t).getGenericComponentType());
		}
		
		if(t instanceof TypeVariable)
		{
			return isUnbounded(((TypeVariable) t).getBounds());
		}
		
		if(t instanceof WildcardType)
		{
			if(!isUnbounded(((WildcardType)t).getLowerBounds()))
			{
				return false;
			}
			
			if(!isUnbounded(((WildcardType)t).getUpperBounds()))
			{
				return false;
			}
			return true;
		}
		
		return false;
	}
	
	public static final String SAAC_FUNCTION_SET_KEY = "SAAC_FUNCTIONSET";
	
	public static Map<String, PreparedFunction> getSessionFunctionSet(SimpleRpcSession sess)
	{
		Map<String, PreparedFunction> ret = (Map<String, PreparedFunction>)sess.get(SAAC_FUNCTION_SET_KEY);
		if(null == ret)
		{
			throw new RuntimeException("SAAC functionSet not set in the session. Use SaacRpc.setSessionFunctionSet to setup the session");
		}
		return ret;
	}
	
	public static void setSessionFunctionSet(SimpleRpcSession sess, Map<String, PreparedFunction> map)
	{
		sess.put(SAAC_FUNCTION_SET_KEY, map);
	}
	
	protected static final String SAAC_SESSION_KEY = "SAAC_EXECUTION";
	
	public static SaacSession getOrCreateSaacSession(SimpleRpcSession sess)
	{
		SaacSession saac_sess = (SaacSession) sess.get(SAAC_SESSION_KEY);
		if(null == saac_sess)
		{
			sess.put(SAAC_SESSION_KEY, saac_sess = new SaacSession(sess));
		}
		return saac_sess;
	}
	
	public static SaacEnv parse(Map<String, PreparedFunction> functions, DataObject data)
	{
		SaacEnv se = new SaacEnv(functions, null);
		se.parse(data);
		return se;
	}
	
	public static boolean execute
	(
		SaacEnv se,
		Map<String, Object> env,
		ReferenceCounted<PrintWriter> LOGGER
	)
	{
		LoggingTools.tryLogFormat
		(
			LOG,
			LogLevel.INFO,
			"Saac Parse started"
		);
		
		se.pushEnv(env);
		
		try
		{
			if(null != LOGGER)
			{
				ThreadLocalHookableLogFacility.setLocalOutput(LOGGER);
			}
			
			//TODO add to session and monitor
			//SaacExecution exec = new SaacExecution(se, saac_sess);
			
			Object stage = se.root;
			
			if(stage instanceof SimpleCall)
			{
				((SimpleCall)stage).call();
				return true;
			}
			
			if(stage instanceof SimplePublish1)
			{
				((SimplePublish1)stage).publish(env);
				return true;
			}
		}
		finally
		{
			se.popEnv(env);
			if(null != LOGGER)
			{
				ThreadLocalHookableLogFacility.setLocalOutput(null);
			}
		}
		
		return false;
	}
	
	public static boolean execute
	(
		Map<String, PreparedFunction> functions,
		DataObject data,
		Map<String, Object> env,
		ReferenceCounted<PrintWriter> LOGGER
	)
	{
		return execute(parse(functions, data), env, LOGGER);
	}
}
