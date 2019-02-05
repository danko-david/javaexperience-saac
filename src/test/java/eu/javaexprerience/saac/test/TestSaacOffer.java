package eu.javaexprerience.saac.test;

import org.junit.Test;

import eu.javaexperience.functional.BoolFunctions;
import eu.javaexperience.rpc.SimpleRpcRequest;
import eu.javaexperience.saac.SaacRpc;
import eu.javaexprerience.saac.test.TestSaacTools.SaacTestComponents;


//TODO serialiazation: Type, GenericArrayType, ParameterizedType, TypeVariable, WildcardType

public class TestSaacOffer
{
	@Test
	public void test_simple_offer_type()
	{
		SaacTestComponents def = TestSaacTools.createDefaultSaacTestComponents();
		
		SimpleRpcRequest req = def.createRpcRequest();

		Object[] off = (Object[]) SaacRpc.offerForType
		(
			req,
			//StringFunctions.class.getCanonicalName()+".withPostfix",
			BoolFunctions.class.getCanonicalName()+".and",
			0,
			null,
			null,
			null,
			null
		);
		
		for(Object o:off)
		{
			System.out.println(o);
		}
	}
	
	
}
