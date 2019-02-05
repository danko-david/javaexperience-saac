package eu.javaexperience.saac;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONObject;

import eu.javaexperience.collection.CollectionTools;
import eu.javaexperience.collection.PublisherCollection;
import eu.javaexperience.collection.map.MapTools;
import eu.javaexperience.datareprez.DataAccessorTools;
import eu.javaexperience.datareprez.PropertyAccessTools;
import eu.javaexperience.datareprez.WellKnownDataAccessors;
import eu.javaexperience.datareprez.jsonImpl.DataObjectJsonImpl;
import eu.javaexperience.interfaces.simple.SimpleGet;
import eu.javaexperience.interfaces.simple.getBy.GetBy1;
import eu.javaexperience.interfaces.simple.getBy.GetBy2;
import eu.javaexperience.interfaces.simple.publish.SimplePublish1;
import eu.javaexperience.io.IOTools;
import eu.javaexperience.io.file.FileTools;
import eu.javaexperience.log.ExtraLogLevel;
import eu.javaexperience.log.JavaExperienceLoggingFacility;
import eu.javaexperience.log.LogLevel;
import eu.javaexperience.log.Loggable;
import eu.javaexperience.log.Logger;
import eu.javaexperience.log.LoggingTools;
import eu.javaexperience.patterns.behavioral.cor.CorChain;
import eu.javaexperience.patterns.behavioral.cor.link.CorChainLink;
import eu.javaexperience.reflect.Mirror;
import eu.javaexperience.saac.SaacEnv.EnvAdapter;
import eu.javaexperience.saac.SaacEnv.SaacClosureInfo;
import eu.javaexperience.annotation.FunctionDescription;
import eu.javaexperience.annotation.FunctionVariableDescription;

public class SaacFunctions
{
	protected static final Logger LOG = JavaExperienceLoggingFacility.getLogger(new Loggable("SaacFunctions", LogLevel.INFO));
	
	@FunctionDescription
	(
		functionDescription = "Az útvonalat végigkeresve, a glob kifejezésre illeszkedő modulokat betölti, burkolja ha az meg lett adva.",
		parameters =
		{
			@FunctionVariableDescription(description = "Keresési útvonal", mayNull = false, paramName = "searchPath", type = Object.class),
			@FunctionVariableDescription(description = "Kifejezés", mayNull = false, paramName = "glob", type = Object.class),
			@FunctionVariableDescription(description = "Illesztés relatív útvonalként", mayNull = false, paramName = "relativeGlob", type = Object.class),
			@FunctionVariableDescription(description = "Burkoló", mayNull = false, paramName = "wrapper", type = Object.class)
		},
		returning = @FunctionVariableDescription(description="Betöltő művelet",mayNull=false,paramName="",type=Object.class) 
	)
	public static <T> T[] loadUnitsGlob
	(
		String startDir,
		String glob,
		boolean relativeGlob,
		final GetBy2<T, String, Object> wrapper
	)
		throws IOException
	{
		final ArrayList<T> ret = new ArrayList<>();
		FileTools.globFiles
		(
			new PublisherCollection<File>()
			{
				@Override
				public boolean add(File obj)
				{
					try
					{
						String str = IOTools.getFileContents(obj);
						
						SaacEnv env = SaacEnv.create(null, new DataObjectJsonImpl(new JSONObject(str)), null);
						T add = (T) env.root;
						if(null != wrapper)
						{
							add = (T) wrapper.getBy(obj.toString(), add);
						}
						
						ret.add(add);
					}
					catch(Exception e)
					{
						LoggingTools.tryLogFormatException
						(
							LOG,
							LogLevel.ERROR,
							e,
							"Error during loadUnitsGlob: file: %s",
							obj
						);
					}
					return true;
				}
			},
			new File(startDir),
			relativeGlob,
			glob
		);
		
		return (T[]) ret.toArray();
	}
	
	public static interface ModGet2<R, A, B> extends GetBy2<R, A, B>, EnvAdapter{};
	
	public static <R, A, B> GetBy2<R, A, B> wrap_f2
	(
		final String p1Name,
		final String p2Name,
		final SimpleGet<R> process
	)
	{
		return new GetBy2<R, A, B>()
		{
			@Override
			public R getBy(A a, B b)
			{
				return process.get();
			}
		};
	}
	
	protected static Object loadScriptRoot(String file) throws FileNotFoundException, IOException
	{
		String str = IOTools.getFileContents(file);
		SaacEnv env = SaacEnv.create(null, new DataObjectJsonImpl(new JSONObject(str)), null);
		return env.root;
	}
	
	protected static void assertNotRuntimeClosure(Object ret)
	{
		if(ret instanceof SaacClosureInfo)
		{
			throw new RuntimeException("Closure is wrapped for runtime execution");
		}
	}
	
	protected static void assertTypeOf(Type t, Object ret)
	{
		if(!(ret instanceof GetBy1))
		{
			throw new RuntimeException("Wrong root type in the script file: "+t.getTypeName()+" required, "+ret.getClass().getTypeName()+" given");
		}
	}
	
	protected static <T> GetBy1<Boolean, T> loadScriptFile(String file)
	{
		try
		{
			Object ret = loadScriptRoot(file);
			assertNotRuntimeClosure(ret);
			assertTypeOf(GetBy1.class, ret);
			
			return (GetBy1<Boolean, T>) ret;
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
	
	protected static <T> void addLinksToChain(CorChain<T> cc, Object o)
	{
		if(null == o)
		{
			return;
		}
		
		if(o instanceof CorChainLink)
		{
			cc.addLink((CorChainLink<T>) o);
		}
		else if(o.getClass().isArray())
		{
			for(Object c:(Object[])o)
			{
				addLinksToChain(cc, c);
			}
		}
		else
		{
			throw new RuntimeException("Given object is not a chain link: chain:"+cc.getChainName()+", link: "+o);
		}
	}
	
	public static <T> GetBy1<Boolean, T> wrapLocation(GetBy1<Boolean, T> func, String file)
	{
		return new GetBy1<Boolean, T>()
		{
			@Override
			public Boolean getBy(T a)
			{
				try
				{
					return func.getBy(a);
				}
				catch(Exception e)
				{
					throw new RuntimeException("Exception ocurred in an included file: "+file, e);
				}
			}
		};
	}
	

	/*public static CorDispatcher<Message> loadChains(String directory) throws IOException
	{
		Object[] re = loadUnitsGlob(directory, "*.partner" , true, new GetBy2<CorChain, String, Object>()
		{
			@Override
			public CorChain getBy(String a, Object b)
			{
				String name = Strings.getLastBetween(a, "/", ".partner", a);
				CorChain<Message> ret = new CorChain<>(name);
				
				//ret.setDefaultAction(DispatcherTools.toDispatcher(ImapSorterTools.postMoveToFolder(name+" Unsorted")));
				
				Object[] os = (Object[]) b;
				for(Object o:os)
				{
					CorChainLink<Message> lnk = (CorChainLink<Message>) o;
					ret.addLink(lnk);
				}
				return ret;
			}
		});
		
		CorDispatcher<Message> dispatcher = new CorDispatcher<>();
		
		for(Object o:re)
		{
			dispatcher.addChain((CorChain<Message>) o);
		}
		
		return dispatcher;
	}*/
	
	public static <T> T[] array(T... elems)
	{
		return elems;
	}
	
	
	public static void log(Object o)
	{
		LoggingTools.tryLogFormat
		(
			LOG,
			ExtraLogLevel.USER,
			"log: %s",
			o
		);
	}
	
	public static <K extends Comparable<K>, V> List<Entry<K,V>> mapSortByKeys(Map<K,V> map)
	{
		return MapTools.sortByKeys(map);
	}
	
	public static <K, V extends Comparable<V>> List<Entry<K,V>> mapSortByValues(Map<K,V> map)
	{
		return MapTools.sortByValues((Map<K, V>) map);
	}
	
	public static <T> String collectionToMultilineString(Collection<T> coll)
	{
		return CollectionTools.toStringMultiline(coll);
	}
	
	public static Object propGet(String path)
	{
		Map<String, Object> env = SaacEnv.getCurrentEnv();
		if(null != env)
		{
			Map<String, Object> access = 
				PropertyAccessTools.dotAccessWrap
				(
					DataAccessorTools.mixedAccessWrap
					(
						env,
						WellKnownDataAccessors.ARRAY,
						WellKnownDataAccessors.LIST,
						WellKnownDataAccessors.MAP,
						WellKnownDataAccessors.OBJECT_PUBLIC_FIELDS
					)
				);
			
			return access.get(path);
		}
		return null;
	}
	
	@FunctionDescription
	(
		functionDescription = "A jelenlegi Saac session-nek kiszállít egy javascript kérést.",
		parameters =
		{
			@FunctionVariableDescription(description = "Kliens oldalon a this neve", mayNull = false, paramName = "thisName", type = Object.class),
			@FunctionVariableDescription(description = "Függvény neve", mayNull = false, paramName = "function", type = Object.class),
			@FunctionVariableDescription(description = "Params", mayNull = false, paramName = "A függvény paraméterei", type = Object.class),
		},
		returning = @FunctionVariableDescription(description="Betöltő művelet",mayNull=false,paramName="",type=Object.class) 
	)
	public static void tryDispatchAsyncServerEvent(String _this, String function, Object... args)
	{
		Map<String,Object> env = SaacEnv.getCurrentEnv();
		try
		{
			SaacSession ss = (SaacSession) env.get("SAAC_SESSION");
			ss.sendServerEvent(_this, function, args);
		}
		catch(Exception e)
		{
			
		}
	}
	
	public static void forceRuntime(SimplePublish1<Map<String,Object>>... runs)
	{
		Map<String, Object> env = SaacEnv.getCurrentEnv();
		for(SimplePublish1<Map<String, Object>> run:runs)
		{
			if(null != run)
			{
				run.publish(env);
			}
		}
	}
	
	public static Object loadFunctionFile(String file) throws FileNotFoundException, IOException
	{
		return loadScriptRoot(file);
	}
}
