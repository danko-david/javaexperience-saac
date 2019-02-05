package eu.javaexprerience.saac.test;

import java.util.Map;
import java.util.Map.Entry;

import eu.javaexperience.collection.map.KeyVal;
import eu.javaexperience.interfaces.simple.getBy.GetBy1;
import eu.javaexperience.interfaces.simple.publish.SimplePublish1;
import eu.javaexperience.saac.SaacEnv;

public class SaacTestFunctions
{
	public static void execAll(SimplePublish1<Map<String,Object>>... executors)
	{
		Map<String, Object> env = SaacEnv.getCurrentEnv();
		for(SimplePublish1<Map<String,Object>> e:executors)
		{
			e.publish(env);
		}
	}
	
	public static <K, V> Entry<K, V> entry(K key, V value)
	{
		return new KeyVal(key, value);
	}
	
	public static <R, P> GetBy1<R, P> switchCase
	(
		GetBy1<R, P> defaultCase,
		Entry<GetBy1<Boolean,P>, GetBy1<R,P>>... cases
	)
	{
		return new GetBy1<R, P>()
		{
			@Override
			public R getBy(P a)
			{
				for(Entry<GetBy1<Boolean, P>, GetBy1<R, P>> c:cases)
				{
					if(Boolean.TRUE == c.getKey().getBy(a))
					{
						return c.getValue().getBy(a);
					}
				}
				
				if(null != defaultCase)
				{
					return defaultCase.getBy(a);
				}
				return  null;
			}
		};
	}
}
