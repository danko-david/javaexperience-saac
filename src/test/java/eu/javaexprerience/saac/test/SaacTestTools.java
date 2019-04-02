package eu.javaexprerience.saac.test;

import eu.javaexperience.datareprez.DataObject;
import eu.javaexperience.datareprez.javaImpl.DataObjectJavaImpl;
import eu.javaexperience.datareprez.jsonImpl.DataObjectJsonImpl;
import eu.javaexperience.functional.BoolFunctions;
import eu.javaexperience.functional.ComparableFunctions;
import eu.javaexperience.functional.GeneralFunctions;
import eu.javaexperience.rpc.SimpleRpcRequest;
import eu.javaexperience.rpc.SimpleRpcSession;
import eu.javaexperience.rpc.bidirectional.BidirectionalRpcDefaultProtocol;
import eu.javaexperience.saac.SaacEnv;
import eu.javaexperience.saac.SaacFunctionCollection;
import eu.javaexperience.saac.SaacRpc;
import eu.javaexperience.saac.SaacRpc.FunctionDescriptor;
import eu.javaexperience.saac.client.SaacContainer;
import eu.javaexperience.text.StringFunctions;

public class SaacTestTools
{
	public static class SaacTestComponents
	{
		public SaacFunctionCollection coll;
		
		public SimpleRpcSession createRpcSession()
		{
			SimpleRpcSession ret = new SimpleRpcSession(BidirectionalRpcDefaultProtocol.DEFAULT_PROTOCOL_HANDLER_WITH_CLASS);
			SaacRpc.setSessionFunctionSet(ret, coll.getFunctions());
			return ret;
		}

		public SimpleRpcRequest createRpcRequest()
		{
			return new SimpleRpcRequest(createRpcSession());
		}

		public SaacEnv compile(SaacContainer root)
		{
			DataObject ser = root.serialize(DataObjectJsonImpl.instane);
			return SaacEnv.create(coll.getFunctions(), ser, null);
		}
	}
	
	public static SaacTestComponents createSaacTestDefaultComponents()
	{
		return createSaacWithComponents
		(
			BoolFunctions.class,
			GeneralFunctions.class,
			StringFunctions.class,
			//CorFunctions.class,
			//DispatcherFunctions.class,
			//SaacFunctions.class,
			ComparableFunctions.class,
			SaacFunctionsForTest.class
		);
	}
	
	public static SaacTestComponents createSaacWithComponents(Class... clss)
	{
		SaacTestComponents ret = new SaacTestComponents();
		ret.coll = new SaacFunctionCollection(clss);
		return ret;
	}
	
	public static Object[] offerFromDefaults(SaacContainer cnt, int arg)
	{
		SaacTestComponents def = SaacTestTools.createSaacTestDefaultComponents();
		
		SimpleRpcRequest req = def.createRpcRequest();
		
		return (Object[]) SaacRpc.offerForType
		(
			req,
			cnt.getId(),
			0,
			null,
			null,
			null,
			null
		);
	}
	
	public static Object[] offerFrom(SaacContainer cnt, int arg, Class... components)
	{
		SaacTestComponents def = SaacTestTools.createSaacWithComponents(components);
		
		SimpleRpcRequest req = def.createRpcRequest();
		
		return (Object[]) SaacRpc.offerForType
		(
			req,
			cnt.getId(),
			0,
			null,
			null,
			null,
			null
		);
	}
	
	public static boolean constainsFunction(Object[] ret, Class cls, String functionName)
	{
		String search = cls.getCanonicalName()+"."+functionName;
		for(Object o:ret)
		{
			if(o instanceof FunctionDescriptor)
			{
				if(((FunctionDescriptor)o).id.equals(search))
				{
					return true;
				}
			}
		}
		
		return false;
	}

	public static void printOffers(Object[] off)
	{
		System.out.println("Number of offers: "+off.length);
		for(Object o:off)
		{
			System.out.println(o);
		}
	}
}
