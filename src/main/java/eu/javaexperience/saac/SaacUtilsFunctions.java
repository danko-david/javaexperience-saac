package eu.javaexperience.saac;

import eu.javaexperience.interfaces.simple.getBy.GetBy1;
import eu.javaexperience.reflect.CastTo;
import eu.javaexperience.reflect.Mirror;

public class SaacUtilsFunctions
{
	public static <T> GetBy1<Boolean, T> isEqualsWith(T object)
	{
		return new GetBy1<Boolean, T>()
		{
			@Override
			public Boolean getBy(T a)
			{
				if(Mirror.equals(a, object))
				{
					return true;
				}
				
				return Mirror.equals(castTo(a, object), a);
			}
		};
	}
	
	public static <T, R> GetBy1<Boolean, T> isResultsEquals(GetBy1<R, T> a, GetBy1<R, T> b)
	{
		return new GetBy1<Boolean, T>()
		{
			@Override
			public Boolean getBy(T p)
			{
				R A = a.getBy(p);
				R B = b.getBy(p);
				return isEquals(A, B);
			}
		};
	}
	
	public static <T> boolean isEquals(T a, T b)
	{
		return Mirror.equals(a, b)
			|| 
				Mirror.equals(castTo(b, a), b)
			||
				Mirror.equals(a, castTo(a, b))
			;
	}
	
	protected static Object castTo(Object to, Object object)
	{
		CastTo t = CastTo.getCasterRestrictlyForTargetClass(to.getClass());
		if(null != t)
		{
			return t.cast(object);
		}
		return object;
	}
}
