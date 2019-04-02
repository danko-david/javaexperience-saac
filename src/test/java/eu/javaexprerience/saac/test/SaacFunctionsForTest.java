package eu.javaexprerience.saac.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import eu.javaexperience.arrays.ArrayTools;
import eu.javaexperience.collection.CollectionTools;
import eu.javaexperience.collection.map.SmallMap;
import eu.javaexperience.interfaces.simple.SimpleCall;
import eu.javaexperience.interfaces.simple.SimpleGet;
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
	
	public enum WellKnownAttributes
	{
		CAPACITY,
		DIMENSION,
		COLOR,
	}
	
	public static interface EntityAttribute
	{
		public String getName();
		public Object getValue();
		
	};
	
	public static class WellKnownEntityAttribute implements EntityAttribute
	{
		public WellKnownAttributes attr;
		public Object value;
		
		@Override
		public String getName()
		{
			return attr.name();
		}

		@Override
		public Object getValue()
		{
			return value;
		}
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
	
	public static interface ModificationCommand{}
	
	public static class ActorDescriptor implements ModificationCommand
	{
		public boolean createEntity;
		public boolean deleteEntity;
		public boolean updateEntity;
		
		public String newKey;
		public String newValue;
		
		public List<EntityAttribute> attributes = new ArrayList<>();
		
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
	
	public static class ModificationInstructions<T extends ModificationCommand>
	{
		public List<T> mod = new ArrayList<>();
		
		public ModificationInstructions(T... mod)
		{
			CollectionTools.inlineAdd(this.mod, mod);
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
	
	public static GetBy1<ModificationCommand, EvalContext> doWhen
	(
		GetBy1<Boolean, EvalContext> check,
		GetBy1<ModificationCommand, EvalContext> act
	)
	{
		return new GetBy1<ModificationCommand, EvalContext>()
		{
			@Override
			public ModificationCommand getBy(EvalContext a)
			{
				if(Boolean.TRUE == check.getBy(a))
				{
					return act.getBy(a);
				}
				return null;
			}
		};
	}

/***************************** AttributeCreators ******************************/
	public static WellKnownEntityAttribute knownAttribute
	(
		WellKnownAttributes attr,
		Object value
	)
	{
		WellKnownEntityAttribute ret = new WellKnownEntityAttribute();
		ret.attr = attr;
		ret.value = value;
		return ret;
	}
	
	public static EntityAttribute newFreeAttribute
	(
		String attr,
		Object value
	)
	{
		return new EntityAttribute()
		{
			@Override
			public String getName()
			{
				return attr;
			}

			@Override
			public Object getValue()
			{
				return value;
			}
		};
	}
	
	public static ModificationCommand newEntryWithAttributes(EntityAttribute... atts)
	{
		ActorDescriptor ret = new ActorDescriptor();
		ret.createEntity = true;
		Collections.addAll(ret.attributes, atts);
		
		return ret;
	}
	
	public static GetBy1<ModificationCommand, ?> funcNewEntryWithAttributes(EntityAttribute... atts)
	{
		return (a)->newEntryWithAttributes(atts);
	}
	
	public static ModificationCommand newEntryKeys(WellKnownAttributes... atts)
	{
		ActorDescriptor ret = new ActorDescriptor();
		ret.createEntity = true;
		for(WellKnownAttributes a:atts)
		{
			ret.attributes.add(newFreeAttribute(a.name(), "placeholder"));
		}
		
		return ret;
	}
	
	public static GetBy1<ModificationCommand, ?> funcNewEntryKeys(WellKnownAttributes... atts)
	{
		return (a)->newEntryKeys(atts);
	}
	
	public static GetBy1<ModificationInstructions, EvalContext> doAllWhen
	(
		GetBy1<Boolean, EvalContext> check,
		GetBy1<ModificationCommand, EvalContext>... acts
	)
	{
		return new GetBy1<ModificationInstructions, EvalContext>()
		{
			@Override
			public ModificationInstructions getBy(EvalContext a)
			{
				if(Boolean.TRUE == check.getBy(a))
				{
					ModificationInstructions<ModificationCommand> ret = new ModificationInstructions<>();
					for(GetBy1<ModificationCommand, EvalContext> act:acts)
					{
						ModificationCommand r = act.getBy(a);
						if(null != r)
						{
							ret.mod.add(r);
						}
					}
					
					return ret;
				}
				return null;
			}
		};
	}
	
	public static SimpleCall justRunEnv(GetBy1<ModificationCommand, EvalContext> cmd)
	{
		return new SimpleCall()
		{
			@Override
			public void call()
			{
				cmd.getBy(new EvalContext());
			}
		};
	}
	
	public static SimpleGet<ModificationCommand> evalWithEnv(GetBy1<ModificationCommand, EvalContext> cmd)
	{
		return new SimpleGet<SaacFunctionsForTest.ModificationCommand>()
		{
			@Override
			public ModificationCommand get()
			{
				//in practice you may read thread local variable to acquire the context
				return cmd.getBy(new EvalContext());
			}
		};
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
	
/****************************** CheckerFunctions ******************************/
	
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
	
/************************* Unbound checker functions **************************/
	
	public static GetBy1<Boolean, ?> isAfterDate(Date d)
	{
		return o->System.currentTimeMillis() >= d.getTime();
	}
	
	public static GetBy1<Boolean, ?> isBeforeDate(Date d)
	{
		return o->System.currentTimeMillis() < d.getTime();
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
