package eu.javaexprerience.saac.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import eu.javaexperience.functional.BoolFunctions;
import eu.javaexperience.functional.ComparableFunctions;
import eu.javaexperience.saac.client.SaacContainer;
import eu.javaexperience.text.StringFunctions;

//TODO serialiazation: Type, GenericArrayType, ParameterizedType, TypeVariable, WildcardType

public class TestSaacOffer
{
	@Test
	public void test_simple_offer_type()
	{
		SaacContainer root = SaacContainer.create(BoolFunctions.class, "and");
		
		Object[] off = SaacTestTools.offer(root, 0);
		
		assertTrue(SaacTestTools.constainsFunction(off, BoolFunctions.class, "or"));
		assertTrue(SaacTestTools.constainsFunction(off, ComparableFunctions.class, "isBetween"));
		
		assertFalse(SaacTestTools.constainsFunction(off, StringFunctions.class, "generatePassMatchingFilter"));
	}
	
	@Test
	public void testOffer1()
	{
		SaacContainer root = SaacContainer.create(StringFunctions.class, "swapExactString");
		
		Object[] off = SaacTestTools.offer(root, 0);
		
		assertTrue(SaacTestTools.constainsFunction(off, SaacFunctionsForTest.class, "castTo"));
		assertTrue(SaacTestTools.constainsFunction(off, SaacFunctionsForTest.class, "createMap"));
		
		assertFalse(SaacTestTools.constainsFunction(off, BoolFunctions.class, "or"));
	}
	
	@Test
	public void testOffer2()
	{
		SaacContainer root = SaacContainer.create(SaacFunctionsForTest.class, "__fixtureAssert");
		
		Object[] off = SaacTestTools.offer(root, 0);
		
		assertTrue(SaacTestTools.constainsFunction(off, SaacFunctionsForTest.class, "isSValue"));
		assertTrue(SaacTestTools.constainsFunction(off, SaacFunctionsForTest.class, "isIValue"));
		assertTrue(SaacTestTools.constainsFunction(off, SaacFunctionsForTest.class, "isDValue"));
		assertTrue(SaacTestTools.constainsFunction(off, BoolFunctions.class, "or"));
		assertTrue(SaacTestTools.constainsFunction(off, BoolFunctions.class, "and"));
		assertTrue(SaacTestTools.constainsFunction(off, BoolFunctions.class, "always"));
		assertTrue(SaacTestTools.constainsFunction(off, BoolFunctions.class, "never"));
	}
	
	@Test
	public void testOffer3()
	{
		SaacContainer root = SaacContainer.create(SaacFunctionsForTest.class, "isSValue");
		
		Object[] off = SaacTestTools.offer(root, 0);
		
		assertTrue(SaacTestTools.constainsFunction(off, StringFunctions.class, "isEndsWith"));
		assertTrue(SaacTestTools.constainsFunction(off, BoolFunctions.class, "not"));
		assertTrue(SaacTestTools.constainsFunction(off, StringFunctions.class, "isMatches"));
		
		assertFalse(SaacTestTools.constainsFunction(off, SaacFunctionsForTest.class, "isSValue"));
	}
	
	@Test
	public void testOffer4()
	{
		SaacContainer root = SaacContainer.create(SaacFunctionsForTest.class, "__fixtureAct");
		
		Object[] off = SaacTestTools.offer(root, 0);
		
		assertTrue(SaacTestTools.constainsFunction(off, SaacFunctionsForTest.class, "deleteByDescision"));
		assertTrue(SaacTestTools.constainsFunction(off, SaacFunctionsForTest.class, "updateByDescision"));
		
		assertFalse(SaacTestTools.constainsFunction(off, SaacFunctionsForTest.class, "isSValue"));
	}
	@Test
	public void testOffer5()
	{
		SaacContainer root = SaacContainer.create(SaacFunctionsForTest.class, "deleteByDescision");
		
		Object[] off = SaacTestTools.offer(root, 0);
		
		assertTrue(SaacTestTools.constainsFunction(off, SaacFunctionsForTest.class, "isSValue"));
		assertTrue(SaacTestTools.constainsFunction(off, SaacFunctionsForTest.class, "isIValue"));
		
		assertFalse(SaacTestTools.constainsFunction(off, SaacFunctionsForTest.class, "isEtc"));
	}
	
	@Test
	public void testOffer6()
	{
		SaacContainer root = SaacContainer.create(SaacFunctionsForTest.class, "__fixtureActExt");
		
		SaacFunctionsForTest.__fixtureActExt(SaacFunctionsForTest.deleteByExtDescision(null));
		
		Object[] off = SaacTestTools.offer(root, 0);
		
		assertTrue(SaacTestTools.constainsFunction(off, SaacFunctionsForTest.class, "deleteByExtDescision"));
		
		assertFalse(SaacTestTools.constainsFunction(off, SaacFunctionsForTest.class, "updateByDescision"));
		assertFalse(SaacTestTools.constainsFunction(off, SaacFunctionsForTest.class, "deleteByDescision"));
	}
	
	@Test
	public void testOffer7()
	{
		SaacContainer root = SaacContainer.create(SaacFunctionsForTest.class, "__fixtureActMod");
		
		SaacFunctionsForTest.__fixtureActExt(SaacFunctionsForTest.deleteByExtDescision(null));
		
		Object[] off = SaacTestTools.offer(root, 0);
		
		SaacTestTools.printOffers(off);
		
		SaacFunctionsForTest.__fixtureActMod(SaacFunctionsForTest.modifyByDescision(null));
		
		assertTrue(SaacTestTools.constainsFunction(off, SaacFunctionsForTest.class, "modifyByDescision"));
		
		assertFalse(SaacTestTools.constainsFunction(off, SaacFunctionsForTest.class, "updateByDescision"));
		assertFalse(SaacTestTools.constainsFunction(off, SaacFunctionsForTest.class, "deleteByDescision"));
	}
	
}
