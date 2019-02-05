package eu.javaexprerience.saac.test;

import eu.javaexperience.dispatch.DispatcherFunctions;
import eu.javaexperience.functional.BoolFunctions;
import eu.javaexperience.functional.ComparableFunctions;
import eu.javaexperience.functional.GeneralFunctions;
import eu.javaexperience.patterns.behavioral.cor.CorFunctions;
import eu.javaexperience.rpc.SimpleRpcRequest;
import eu.javaexperience.rpc.SimpleRpcSession;
import eu.javaexperience.rpc.bidirectional.BidirectionalRpcDefaultProtocol;
import eu.javaexperience.saac.SaacFunctionCollection;
import eu.javaexperience.saac.SaacFunctions;
import eu.javaexperience.saac.SaacRpc;
import eu.javaexperience.text.StringFunctions;
import eu.javaexperience.web.spider.SpiderFunctions;

public class TestSaacTools
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
		
		/*public void use()
		{
			
			
		}*/
	}
	
	public static SaacTestComponents createDefaultSaacTestComponents()
	{
		SaacTestComponents ret = new SaacTestComponents();
		
		ret.coll = new SaacFunctionCollection
		(
			BoolFunctions.class,
			GeneralFunctions.class,
			StringFunctions.class,
			CorFunctions.class,
			DispatcherFunctions.class,
			SaacFunctions.class,
			ComparableFunctions.class
		);
		
		
		return ret;
	}
}
