package eu.javaexprerience.saac.test;

import java.util.Map;
import java.util.Map.Entry;

import eu.javaexperience.collection.map.SmallMap;
import eu.javaexperience.interfaces.simple.getBy.GetBy1;

/**
 * Some monkey business class used to test offering, compilation and 
 * execution of user defined saac functions.
 * */
public class SaacFunctionsForTest
{
	public enum TestEntityType
	{
		BOOL,
		NUMBER,
		OBJECT,
		ARRAY
	}
	
	public static class EvalContext
	{
		public int iVal;
		public String sVal;
		public double dVal;
	}
	
	public static class ExtendedEvalContext extends EvalContext
	{
		public Object etc;
	}
	
	public static interface ModificationCommand
	{
		
	}
	
	public static class ActorDescriptor implements ModificationCommand
	{
		public boolean deleteEntity;
		public boolean updateEntity;
		
		public String newKey;
		public String newValue;
		
		public ActorDescriptor delete()
		{
			this.deleteEntity = true;
			return this;
		}
		
		public ActorDescriptor update()
		{
			this.updateEntity = true;
			return this;
		}
	}
	
	public static class ModificationInstruction<T extends ModificationCommand>
	{
		public T mod;
		
		public ModificationInstruction(T mod)
		{
			this.mod = mod;
		}
	}
	
/******************************** Fixtures ************************************/
	
	public static void __fixtureAssert(GetBy1<Boolean, EvalContext> eval){}
	
	public static void __fixtureAct(GetBy1<ActorDescriptor, EvalContext> eval){}
	
	public static void __fixtureActExt(GetBy1<ActorDescriptor, ExtendedEvalContext> eval){}
	
	public static void __fixtureActMod(GetBy1<? extends ModificationInstruction, ExtendedEvalContext> eval){}
	
/******************************* ActByDecision ********************************/
	
	public static GetBy1<ActorDescriptor, EvalContext> deleteByDescision(GetBy1<Boolean, EvalContext> eval)
	{
		return (ctx)-> Boolean.TRUE == eval.getBy(ctx)?new ActorDescriptor().delete():null;
	}
	
	public static GetBy1<ActorDescriptor, EvalContext> updateByDescision(GetBy1<Boolean, EvalContext> eval)
	{
		return (ctx)-> Boolean.TRUE == eval.getBy(ctx)?new ActorDescriptor().update():null;
	}
	
/****************************** ActByDecision2 ********************************/
	
	public static GetBy1<ActorDescriptor, ExtendedEvalContext> deleteByExtDescision(GetBy1<Boolean, ExtendedEvalContext> eval)
	{
		return (ctx)-> Boolean.TRUE == eval.getBy(ctx)?new ActorDescriptor().update():null;
	}
	
	
	public static GetBy1<ModificationInstruction<ActorDescriptor>, ExtendedEvalContext> modifyByDescision(GetBy1<Boolean, ExtendedEvalContext> eval)
	{
		return (ctx)-> Boolean.TRUE == eval.getBy(ctx)?new ModificationInstruction<>(new ActorDescriptor()):null;
	}
	
/************************** AssertExtendedFunctions ***************************/

	public static GetBy1<Boolean, ExtendedEvalContext> isEtc(GetBy1<Boolean, Object> check)
	{
		return (ctx)->check.getBy(ctx.etc);
	}
	
/****************************** AssertFunctions *******************************/
	
	public static GetBy1<Boolean, EvalContext> isIValue(GetBy1<Boolean, Integer> eval)
	{
		return (ctx)->eval.getBy(ctx.iVal);
	}
	
	public static GetBy1<Boolean, EvalContext> isSValue(GetBy1<Boolean, String> eval)
	{
		return (ctx)->eval.getBy(ctx.sVal);
	}
	
	public static GetBy1<Boolean, EvalContext> isDValue(GetBy1<Boolean, Double> eval)
	{
		return (ctx)->eval.getBy(ctx.dVal);
	}
	
/************************** 0th level functions *******************************/
	public static Object castTo(Object o, TestEntityType toType)
	{
		return o;
	}
	
	public static boolean isNotNull(Object o)
	{
		return null != o;
	}
	
	public static <K,V> Map<K,V> createMap(Entry<K,V>... ent)
	{
		Map<K, V> ret = new SmallMap<>();
		for(Entry<K, V> kv:ent)
		{
			ret.put(kv.getKey(), kv.getValue());
		}
		
		return ret;
	}
	
}
