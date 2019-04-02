package eu.javaexprerience.saac.test;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import eu.javaexperience.functional.BoolFunctions;
import eu.javaexperience.interfaces.simple.getBy.GetBy1;
import eu.javaexperience.saac.SaacEnv;
import eu.javaexperience.saac.client.SaacContainer;
import eu.javaexprerience.saac.test.SaacFunctionsForTest.ActorDescriptor;
import eu.javaexprerience.saac.test.SaacFunctionsForTest.EvalContext;
import eu.javaexprerience.saac.test.SaacFunctionsForTest.ModificationCommand;
import eu.javaexprerience.saac.test.SaacFunctionsForTest.ModificationInstructions;
import eu.javaexprerience.saac.test.SaacFunctionsForTest.WellKnownAttributes;
import eu.javaexprerience.saac.test.SaacFunctionsForTest.WellKnownEntityAttribute;
import eu.javaexprerience.saac.test.SaacTestTools.SaacTestComponents;

import static eu.javaexperience.saac.client.SaacContainer.*;

public class TestSaacAssemble
{
	public static SaacContainer c(String name, SaacContainer... cnts)
	{
		return SaacContainer.create
		(
			SaacFunctionsForTest.class,
			name,
			cnts
		);
	}
	
	@Test
	public void testWrapConstAsGetFunction_and_singleVarargsArrayWrap()
	{
		SaacContainer f = c
		(
			"doWhen",
			c("isAfterDate", create(System.currentTimeMillis()-15000)),
			
			//direct values being wrapped to getter function
			c
			(
				"newEntryWithAttributes",
				createArray().addArgument
				(
					//using single parameter for for vararg params
					c("newFreeAttribute", create("startValue"), create("myValue"))
				)
			)
		);
		
		SaacTestComponents saac = SaacTestTools.createSaacTestDefaultComponents();
		SaacEnv env = saac.compile(f);
		GetBy1<ModificationCommand, EvalContext> root = (GetBy1<ModificationCommand, EvalContext>) env.getRoot();
		
		EvalContext ctx = new EvalContext();
		ModificationCommand cmd = root.getBy(ctx);
		assertNotNull(cmd);
		ActorDescriptor ad = (ActorDescriptor) cmd;
		
		assertEquals(1, ad.attributes.size());
		assertEquals("startValue", ad.attributes.get(0).getName());
		assertEquals("myValue", ad.attributes.get(0).getValue());
	}
	
	@Test
	public void testWrapConstAsGetFunction_and_multiVarargUsingArrayWrappedAsVaragaArray()
	{
		SaacContainer f = c
		(
			"doWhen",
			c("isAfterDate", create(System.currentTimeMillis()-15000)),
			
			//direct values being wrapped to getter function
			c
			(
				"newEntryWithAttributes",
				//using an array declaration for vararg params
				createArray().addArgument
				(
					c("knownAttribute", create(WellKnownAttributes.COLOR.name()), create("red")),
					c("newFreeAttribute", create("startValue"), create("myValue"))
				)
			)
		);
		
		SaacTestComponents saac = SaacTestTools.createSaacTestDefaultComponents();
		SaacEnv env = saac.compile(f);
		GetBy1<ModificationCommand, EvalContext> root = (GetBy1<ModificationCommand, EvalContext>) env.getRoot();
		
		EvalContext ctx = new EvalContext();
		ModificationCommand cmd = root.getBy(ctx);
		assertNotNull(cmd);
		ActorDescriptor ad = (ActorDescriptor) cmd;
		
		assertEquals(2, ad.attributes.size());
		assertEquals("COLOR", ad.attributes.get(0).getName());
		assertEquals("red", ad.attributes.get(0).getValue());
		
		assertEquals("startValue", ad.attributes.get(1).getName());
		assertEquals("myValue", ad.attributes.get(1).getValue());
	}
	
	@Test
	public void testStringToEnum()
	{
		SaacContainer f = c
		(
			"doWhen",
			c("isAfterDate", create(System.currentTimeMillis()-15000)),
			c
			(
				"newEntryWithAttributes",
				//also test that name being casted to enum type
				c("knownAttribute", create(WellKnownAttributes.COLOR.name()), create("red"))
			)
		);
		
		SaacTestComponents saac = SaacTestTools.createSaacTestDefaultComponents();
		SaacEnv env = saac.compile(f);
		GetBy1<ModificationCommand, EvalContext> root = (GetBy1<ModificationCommand, EvalContext>) env.getRoot();
		
		EvalContext ctx = new EvalContext();
		ModificationCommand cmd = root.getBy(ctx);
		assertNotNull(cmd);
		ActorDescriptor ad = (ActorDescriptor) cmd;
		
		assertEquals(1, ad.attributes.size());
		assertEquals(WellKnownAttributes.COLOR, ((WellKnownEntityAttribute)ad.attributes.get(0)).attr);
		assertEquals("COLOR", ad.attributes.get(0).getName());
		assertEquals("red", ad.attributes.get(0).getValue());
	}
	
	@Test
	public void testUsingCreateFunctionVarargsMultiParam()
	{
		SaacContainer f = c
		(
			"doWhen",
			c("isAfterDate", create(System.currentTimeMillis()-15000)),
			c
			(
				"funcNewEntryWithAttributes",
				createArray().addArgument
				(
					c("knownAttribute", create(WellKnownAttributes.COLOR.name()), create("red")),
					c("newFreeAttribute", create("startValue"), create("myValue"))
				)
			)
		);
		
		SaacTestComponents saac = SaacTestTools.createSaacTestDefaultComponents();
		SaacEnv env = saac.compile(f);
		GetBy1<ModificationCommand, EvalContext> root = (GetBy1<ModificationCommand, EvalContext>) env.getRoot();
		
		EvalContext ctx = new EvalContext();
		ModificationCommand cmd = root.getBy(ctx);
		assertNotNull(cmd);
		ActorDescriptor ad = (ActorDescriptor) cmd;
		
		assertEquals(2, ad.attributes.size());
		assertEquals(WellKnownAttributes.COLOR, ((WellKnownEntityAttribute)ad.attributes.get(0)).attr);
		assertEquals("COLOR", ad.attributes.get(0).getName());
		assertEquals("red", ad.attributes.get(0).getValue());
		
		assertEquals("startValue", ad.attributes.get(1).getName());
		assertEquals("myValue", ad.attributes.get(1).getValue());
		
	}
	
	@Test
	public void testUsingCreateFunctionVarargsSingleParam()
	{
		SaacContainer f = c
		(
			"doWhen",
			c("isAfterDate", create(System.currentTimeMillis()-15000)),
			c
			(
				"funcNewEntryWithAttributes",
				c("knownAttribute", create(WellKnownAttributes.COLOR.name()), create("red"))
			)
		); 
		
		SaacTestComponents saac = SaacTestTools.createSaacTestDefaultComponents();
		SaacEnv env = saac.compile(f);
		GetBy1<ModificationCommand, EvalContext> root = (GetBy1<ModificationCommand, EvalContext>) env.getRoot();
		
		EvalContext ctx = new EvalContext();
		ModificationCommand cmd = root.getBy(ctx);
		assertNotNull(cmd);
		ActorDescriptor ad = (ActorDescriptor) cmd;
		
		assertEquals(1, ad.attributes.size());
		assertEquals(WellKnownAttributes.COLOR, ((WellKnownEntityAttribute)ad.attributes.get(0)).attr);
		assertEquals("COLOR", ad.attributes.get(0).getName());
		assertEquals("red", ad.attributes.get(0).getValue());
	}
	

	@Test
	public void testEvalNoRet()
	{
		SaacContainer f = c
		(
			"doWhen",
			//not the current time +15 secounds modification
			c("isAfterDate", create(System.currentTimeMillis()+15000)),
			c
			(
				"funcNewEntryWithAttributes",
				c("knownAttribute", create(WellKnownAttributes.COLOR.name()), create("red"))
			)
		);
		
		SaacTestComponents saac = SaacTestTools.createSaacTestDefaultComponents();
		SaacEnv env = saac.compile(f);
		GetBy1<ModificationCommand, EvalContext> root = (GetBy1<ModificationCommand, EvalContext>) env.getRoot();
		
		EvalContext ctx = new EvalContext();
		ModificationCommand cmd = root.getBy(ctx);
		assertNull(cmd);
	}
	
	@Test
	public void testConditionAnd()
	{
		SaacContainer f = c
		(
			"doWhen",
			SaacContainer.create
			(
				BoolFunctions.class,
				"and", 
				c("isAfterDate", create(System.currentTimeMillis()-15000)),
				c("isBeforeDate", create(System.currentTimeMillis()+15000))
			),
			
			//direct values being wrapped to getter function
			c
			(
				"newEntryWithAttributes",
				//using an array declaration for vararg params
				createArray().addArgument
				(
					c("knownAttribute", create(WellKnownAttributes.COLOR.name()), create("red")),
					c("newFreeAttribute", create("startValue"), create("myValue"))
				)
			)
		);
		
		SaacTestComponents saac = SaacTestTools.createSaacTestDefaultComponents();
		SaacEnv env = saac.compile(f);
		GetBy1<ModificationCommand, EvalContext> root = (GetBy1<ModificationCommand, EvalContext>) env.getRoot();
		
		EvalContext ctx = new EvalContext();
		ModificationCommand cmd = root.getBy(ctx);
		assertNotNull(cmd);
		ActorDescriptor ad = (ActorDescriptor) cmd;
		
		assertEquals(2, ad.attributes.size());
		assertEquals("COLOR", ad.attributes.get(0).getName());
		assertEquals("red", ad.attributes.get(0).getValue());
		
		assertEquals("startValue", ad.attributes.get(1).getName());
		assertEquals("myValue", ad.attributes.get(1).getValue());
	}
	
	@Test
	public void testEnumVarargs1()
	{
		SaacContainer f = c
		(
			"doWhen",
			SaacContainer.create
			(
				BoolFunctions.class,
				"and", 
				c("isAfterDate", create(System.currentTimeMillis()-15000)),
				c("isBeforeDate", create(System.currentTimeMillis()+15000))
			),
			
			//direct values being wrapped to getter function
			c
			(
				"newEntryKeys",
				//using an array declaration for vararg params
				create(WellKnownAttributes.COLOR),
				create(WellKnownAttributes.CAPACITY),
				create(WellKnownAttributes.DIMENSION)
			)
		);
		
		SaacTestComponents saac = SaacTestTools.createSaacTestDefaultComponents();
		SaacEnv env = saac.compile(f);
		GetBy1<ModificationCommand, EvalContext> root = (GetBy1<ModificationCommand, EvalContext>) env.getRoot();
		
		EvalContext ctx = new EvalContext();
		ModificationCommand cmd = root.getBy(ctx);
		assertNotNull(cmd);
		ActorDescriptor ad = (ActorDescriptor) cmd;
		
		assertEquals(3, ad.attributes.size());
		
		assertEquals("COLOR", ad.attributes.get(0).getName());
		assertEquals("placeholder", ad.attributes.get(0).getValue());
		
		assertEquals("CAPACITY", ad.attributes.get(1).getName());
		assertEquals("placeholder", ad.attributes.get(1).getValue());
		
		assertEquals("DIMENSION", ad.attributes.get(2).getName());
		assertEquals("placeholder", ad.attributes.get(2).getValue());
	}
	
	@Test
	public void testEnumVarargs2()
	{
		SaacContainer f = c
		(
			"doWhen",
			SaacContainer.create
			(
				BoolFunctions.class,
				"and", 
				c("isAfterDate", create(System.currentTimeMillis()-15000)),
				c("isBeforeDate", create(System.currentTimeMillis()+15000))
			),
			
			//direct values being wrapped to getter function
			c
			(
				"newEntryKeys",
				//using an array declaration for vararg params
				createArray().addArgument
				(
					create(WellKnownAttributes.COLOR),
					create(WellKnownAttributes.CAPACITY),
					create(WellKnownAttributes.DIMENSION)
				)
			)
		);
		
		SaacTestComponents saac = SaacTestTools.createSaacTestDefaultComponents();
		SaacEnv env = saac.compile(f);
		GetBy1<ModificationCommand, EvalContext> root = (GetBy1<ModificationCommand, EvalContext>) env.getRoot();
		
		EvalContext ctx = new EvalContext();
		ModificationCommand cmd = root.getBy(ctx);
		assertNotNull(cmd);
		ActorDescriptor ad = (ActorDescriptor) cmd;
		
		assertEquals(3, ad.attributes.size());
		
		assertEquals("COLOR", ad.attributes.get(0).getName());
		assertEquals("placeholder", ad.attributes.get(0).getValue());
		
		assertEquals("CAPACITY", ad.attributes.get(1).getName());
		assertEquals("placeholder", ad.attributes.get(1).getValue());
		
		assertEquals("DIMENSION", ad.attributes.get(2).getName());
		assertEquals("placeholder", ad.attributes.get(2).getValue());
	}
	
	@Test
	public void testBugVararg()
	{
		SaacContainer f = c
		(
			"doAllWhen",
			SaacContainer.create
			(
				BoolFunctions.class,
				"and", 
				c("isAfterDate", create(System.currentTimeMillis()-15000)),
				c("isBeforeDate", create(System.currentTimeMillis()+15000))
			),
			
			//direct values being wrapped to getter function
			c
			(
				"newEntryWithAttributes",
				c("knownAttribute", create(WellKnownAttributes.COLOR.name()), create("placeholder")),
				c("knownAttribute", create(WellKnownAttributes.CAPACITY.name()), create("placeholder"))
			),
			c
			(
				"funcNewEntryWithAttributes",
				c("knownAttribute", create(WellKnownAttributes.COLOR.name()), create("placeholder"))
				
			),
			c
			(
				"funcNewEntryKeys",
				create(WellKnownAttributes.DIMENSION.name())
			)
		);
		
		SaacTestComponents saac = SaacTestTools.createSaacTestDefaultComponents();
		SaacEnv env = saac.compile(f);
		GetBy1<ModificationInstructions<ModificationCommand>, EvalContext> root = (GetBy1<ModificationInstructions<ModificationCommand>, EvalContext>) env.getRoot();
		
		EvalContext ctx = new EvalContext();
		ModificationInstructions<ModificationCommand> cmd = root.getBy(ctx);
		assertNotNull(cmd);
		List<ActorDescriptor> ad = (List) cmd.mod;
		
		
		
		assertEquals(3, ad.size());
		
		assertEquals("COLOR", ad.get(0).attributes.get(0).getName());
		assertEquals("placeholder", ad.get(0).attributes.get(0).getValue());
		
		assertEquals("CAPACITY", ad.get(0).attributes.get(1).getName());
		assertEquals("placeholder", ad.get(0).attributes.get(1).getValue());
		
		assertEquals("COLOR", ad.get(1).attributes.get(0).getName());
		assertEquals("placeholder", ad.get(1).attributes.get(0).getValue());
		
		assertEquals("DIMENSION", ad.get(2).attributes.get(0).getName());
		assertEquals("placeholder", ad.get(2).attributes.get(0).getValue());
		
	}
	
	//TODO test for errors: not enought argument provided, user compileAndCheckFor that.
	//TODO check for illegal argument exception
}
