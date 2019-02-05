package eu.javaexperience.saac;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

import org.json.JSONObject;

import eu.javaexperience.datareprez.jsonImpl.DataObjectJsonImpl;
import eu.javaexperience.interfaces.simple.SimpleCall;
import eu.javaexperience.interfaces.simple.getBy.GetBy1;
import eu.javaexperience.interfaces.simple.publish.SimplePublish1;
import eu.javaexperience.io.IOTools;
import eu.javaexperience.log.JavaExperienceLoggingFacility;
import eu.javaexperience.log.LogLevel;
import eu.javaexperience.log.Loggable;
import eu.javaexperience.log.Logger;
import eu.javaexperience.log.LoggingTools;
import eu.javaexperience.reflect.Mirror;
import eu.javaexperience.saac.SaacEnv.SaacClosureInfo;

public class SaacTools
{
	protected static final Logger LOG = JavaExperienceLoggingFacility.getLogger(new Loggable("SaacTools", LogLevel.INFO));
	
	public static void execScript(String file, Map<String, Object> env) throws FileNotFoundException, IOException
	{
		Object o = loadScriptRoot(file);
		if(o instanceof SimplePublish1)
		{
			((SimplePublish1<Map<String,Object>>)o).publish(env);
		}
		else if(o instanceof GetBy1)
		{
			((GetBy1<?, Map<String,Object>>)o).getBy(env);
		}
		else if(o instanceof SimpleCall)
		{
			((SimpleCall)o).call();
		}
	}
	
	public static Object loadScriptRoot(String file) throws FileNotFoundException, IOException
	{
		String str = IOTools.getFileContents(file);
		SaacEnv env = SaacEnv.create(null, new DataObjectJsonImpl(new JSONObject(str)), null);
		return env.root;
	}
	
	public static void assertNotRuntimeClosure(Object ret)
	{
		if(ret instanceof SaacClosureInfo)
		{
			throw new RuntimeException("Closure is wrapped for runtime execution");
		}
	}
	
	public static void assertTypeOf(Type t, Object ret)
	{
		if(!(ret instanceof GetBy1))
		{
			throw new RuntimeException("Wrong root type in the script file: "+t.getTypeName()+" required, "+ret.getClass().getTypeName()+" given");
		}
	}
	
	public static GetBy1<Boolean, ?> loadScriptFile(String file)
	{
		try
		{
			Object ret = loadScriptRoot(file);
			assertNotRuntimeClosure(ret);
			assertTypeOf(GetBy1.class, ret);
			
			return (GetBy1<Boolean, ?>) ret;
		}
		catch(Exception e)
		{
			LoggingTools.tryLogFormatException
			(
				LOG,
				LogLevel.ERROR,
				e,
				"Can't parse script file: %s\n",
				file
			);
			Mirror.propagateAnyway(e);
		}
		return null;
	}
}
