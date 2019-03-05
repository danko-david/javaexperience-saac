package eu.javaexperience.saac;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletOutputStream;

import org.json.JSONObject;

import eu.javaexperience.collection.iterator.IteratorTools;
import eu.javaexperience.collection.map.NullMap;
import eu.javaexperience.collection.map.OneShotMap;
import eu.javaexperience.datareprez.DataArray;
import eu.javaexperience.datareprez.DataCommon;
import eu.javaexperience.datareprez.DataObject;
import eu.javaexperience.datareprez.DataReprezTools;
import eu.javaexperience.datareprez.DataSender;
import eu.javaexperience.datareprez.convertFrom.DataWrapper;
import eu.javaexperience.datareprez.jsonImpl.DataObjectJsonImpl;
import eu.javaexperience.dispatch.Dispatcher;
import eu.javaexperience.functional.saac.Functions;
import eu.javaexperience.functional.saac.Functions.PreparedFunction;
import eu.javaexperience.interfaces.simple.getBy.GetBy1;
import eu.javaexperience.patterns.behavioral.cor.CorDispatcher;
import eu.javaexperience.reflect.Mirror;
import eu.javaexperience.reflect.Mirror.BelongTo;
import eu.javaexperience.reflect.Mirror.FieldSelector;
import eu.javaexperience.reflect.Mirror.Select;
import eu.javaexperience.reflect.Mirror.Visibility;
import eu.javaexperience.rpc.RpcFacility;
import eu.javaexperience.rpc.RpcFunction;
import eu.javaexperience.rpc.RpcRequest;
import eu.javaexperience.rpc.SimpleRpcRequest;
import eu.javaexperience.rpc.SimpleRpcSession;
import eu.javaexperience.rpc.bidirectional.BidirectionalRpcDefaultProtocol;
import eu.javaexperience.rpc.bidirectional.BidirectionalRpcProtocolHandler;
import eu.javaexperience.rpc.codegen.JavascriptRpcSourceGenerator;
import eu.javaexperience.rpc.function.RpcFunctionParameter;
import eu.javaexperience.rpc.http.RpcHttpTools;
import eu.javaexperience.semantic.references.MayNull;
import eu.javaexperience.text.StringFunctions;
import eu.javaexperience.url.UrlPart;
import eu.javaexperience.web.Context;
import eu.javaexperience.web.dispatch.WebDispatchTools;
import eu.javaexperience.web.dispatch.url.UrlNodePatternTools;
import eu.javaexperience.web.features.WebSocket;
import eu.javaexperience.web.features.WebSocketEndpoint;

public class SaacServerUnit
{
	public static final DataWrapper reflectTypeDataWrapper = new DataWrapper()
	{
		@Override
		public DataCommon wrap
		(
			DataWrapper topWrapper,
			DataCommon prototype,
			Object o
		)
		{
			/*if(o instanceof AnnotatedType)
			{
				DataObject ret = prototype.newObjectInstance();
				AnnotatedType at = (AnnotatedType)o;
				
				return ret;
			}*/
			
			if(o instanceof Type)
			{
				DataObject ret = prototype.newObjectInstance();
				ret.putString("toString", o.toString());
				
				if(o instanceof Class)
				{
					ret.putString("type", "class");
					ret.putString("class", ((Class)o).getSimpleName());
				}
				else if(o instanceof GenericArrayType)
				{
					GenericArrayType gat = (GenericArrayType) o;
					ret.putString("type", "genericArray");
					ret.putObject("genericComponentType", (DataObject) topWrapper.wrap(topWrapper, prototype, gat.getGenericComponentType()));
				}
				else if(o instanceof ParameterizedType)
				{
					ParameterizedType pt = (ParameterizedType) o;
					ret.putString("type", "parameterized");
					ret.putArray("actualTypeArguments", (DataArray) topWrapper.wrap(topWrapper, prototype, pt.getActualTypeArguments()));
					ret.putObject("rawType", (DataObject) wrap(topWrapper, prototype, pt.getRawType()));
				}
				else if(o instanceof TypeVariable)
				{
					TypeVariable tv = (TypeVariable) o;
					ret.putString("type", "typeVariable");
					//ret.putObject("genericDeclaration", (DataObject) topWrapper.wrap(topWrapper, prototype, tv.getGenericDeclaration()));
					//ret.putArray("annotatedBounds", (DataArray) topWrapper.wrap(topWrapper, prototype, tv.getAnnotatedBounds()));
					ret.putArray("bounds", (DataArray) topWrapper.wrap(topWrapper, prototype, tv.getBounds()));
					ret.putString("name", tv.getName());
				}
				else if(o instanceof WildcardType)
				{
					WildcardType wt = (WildcardType) o;
					ret.putString("type", "wildcard");
					ret.putArray("lowerBounds" , (DataArray) topWrapper.wrap(topWrapper, prototype, wt.getLowerBounds()));
					ret.putArray("upperBounds" , (DataArray) topWrapper.wrap(topWrapper, prototype, wt.getUpperBounds()));
				}
				
				return ret;
			}
			return null;
		}
	};  
	
	public static final BidirectionalRpcProtocolHandler<SimpleRpcSession> DEFAULT_PROTOCOL = new BidirectionalRpcDefaultProtocol<SimpleRpcSession>
	(
		new DataObjectJsonImpl(),
		DataReprezTools.combineWrappers
		(
			reflectTypeDataWrapper,
			DataReprezTools.WRAP_ARRAY_COLLECTION_MAP,
			DataReprezTools.WRAP_DATA_LIKE,
			DataReprezTools.createClassInstanceWrapper
			(
				new FieldSelector
				(
					true,
					Visibility.Public,
					BelongTo.Instance,
					Select.All,
					Select.IsNot,
					Select.All
				)
			)
		)
	);
	
	public static SaacServerUnit create
	(
		String apiName,
		Iterable<Class<?>> clss
	)
	{
		ArrayList<PreparedFunction> funcs = new ArrayList<>();
		
		for(Class<?> cls:clss)
		{
			Functions.collectFunctions(funcs, cls);
		}
		
		HashMap<String, PreparedFunction> pf = new HashMap<>();
		
		for(PreparedFunction f:funcs)
		{
			pf.put(f.getId(), f);
		}
		
		return new SaacServerUnit(apiName, pf);
	}
	
	public static SaacServerUnit create
	(
		String apiName,
		Class<?>... clss
	)
	{
		return create(apiName, IteratorTools.asList(clss));
	}

	protected final String apiName;
	protected final Map<String, PreparedFunction> saacFunctions;
	
	protected final BidirectionalRpcProtocolHandler<SimpleRpcSession> protocol;
	
	public SaacServerUnit(String apiName, Map<String, PreparedFunction> funcs)
	{
		this.protocol = DEFAULT_PROTOCOL;
		this.apiName = apiName;
		this.saacFunctions = Collections.unmodifiableMap(new HashMap<String, PreparedFunction>(funcs));
	}
	
	public Map<String, PreparedFunction> getFunctions()
	{
		return saacFunctions;
	}
	
	public String getJsApiCall(boolean async)
	{
		return JavascriptRpcSourceGenerator.BASIC_JAVASCRIPT_SOURCE_BUILDER.buildRpcClientSource
		(
			"SaacApi",
			(Collection<RpcFunction<RpcRequest, RpcFunctionParameter>>) (Object) SaacRpc.DISPATCH.getWrappedMethods(),
			async?new OneShotMap<>("using_callback_return", "true"):NullMap.instance
		);
	}
	
	public SimpleRpcSession sessionStart(@MayNull WebSocket ws)
	{
		final SimpleRpcSession session = new SimpleRpcSession(protocol);
		SaacRpc.setSessionFunctionSet(session, saacFunctions);
		if(null != ws)
		{
			session.put("SEND", ws);
		}
		session.put("PROTOCOL", protocol);
		return session;
	}
	
	public void sessionStart(SimpleRpcSession session, @MayNull WebSocket ws)
	{
		SaacRpc.setSessionFunctionSet(session, saacFunctions);
		if(null != ws)
		{
			session.put("SEND", ws);
		}
		session.put("PROTOCOL", protocol);
	}
	
	public void serveRestCall(Context ctx) throws IOException
	{
		RpcHttpTools.serveRpcAjaxRequest(ctx, RpcHttpTools.WRAP_SIMPLE_RPC_REQUEST, GET_SESSION, protocol, SaacRpc.DISPATCH);
	}
	
	public final GetBy1<SimpleRpcSession, Context> GET_SESSION = new GetBy1<SimpleRpcSession, Context>()
	{
		@Override
		public SimpleRpcSession getBy(Context a)
		{
			return sessionStart(null);
		}
	};
	
	public void handleWebSocket(Context ctx) throws NoSuchAlgorithmException, IOException
	{
		//HttpRequest h = (HttpRequest) ctx.getRequest();
		WebSocket ws = WebSocketEndpoint.upgradeRequest(ctx);//h.getQuery().upgradeWebsocket();
		SimpleRpcSession session = sessionStart(ws);
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataSender ds = protocol.getDefaultCommunicationProtocolPrototype().newDataSender(baos);
		
		
		while(true)
		{
			String get = null;
			try
			{
				baos.reset();
				get = null;
				byte[] rec = ws.receive();
				get = new String(rec);
				DataObject rpcReq = new DataObjectJsonImpl(new JSONObject(get));
				ds.send(SaacRpc.DISPATCH.dispatch(new SimpleRpcRequest(session, rpcReq)));
			}
			catch(Exception e)
			{
				//System.out.println(get);
				//throw e;
				Mirror.propagateAnyway(e);
				//ds.send(RpcTools.wrapException(new SimpleRpcRequest(session), e));
			}
			finally
			{
				ws.send(baos.toByteArray());
			}
		}
	}
	
	public Dispatcher<Context> generateApiAjaxDispatcher()
	{
		return new Dispatcher<Context>()
		{
			@Override
			public boolean dispatch(Context ctx)
			{
				try
				{
					serveRestCall(ctx);
				}
				catch (Exception e)
				{
					Mirror.propagateAnyway(e);
				}
				ctx.finishOperation();
				return true;
			}
		};
	}
	
	public Dispatcher<Context> generateApiWebsocketDispatcher()
	{
		return new Dispatcher<Context>()
		{

			@Override
			public boolean dispatch(Context ctx)
			{
				try
				{
					handleWebSocket(ctx);
				}
				catch (Exception e)
				{
					Mirror.propagateAnyway(e);
				}
				ctx.finishOperation();
				return false;
			}
		};
	}

	public void registerApiDispacher(CorDispatcher<Context> dds, final String chain, final  String restPath, final  String websocketPath)
	{
		dds.getChainByName(chain).addLink
		(
			WebDispatchTools.exactPath
			(
				UrlNodePatternTools.assemble(UrlPart.PATH, StringFunctions.isEquals(restPath)),
				generateApiAjaxDispatcher()
			)
		);
		
		dds.getChainByName(chain).addLink
		(
			WebDispatchTools.exactPath
			(
				UrlNodePatternTools.assemble(UrlPart.PATH, StringFunctions.isEquals(websocketPath)),
				generateApiWebsocketDispatcher()
			)
		);
	}

	public static void registerSaveDispatch
	(
		CorDispatcher<Context> dispatcher,
		String chain,
		File rootDir,
		final String getPath,
		final String savePath
	)
		throws IOException
	{

		final Dispatcher<Context> SAVE_RESTORE = SaacWebTools.createSaveRestoreDispatcher(rootDir);
		
		dispatcher.getChainByName(chain).addLink
		(
			WebDispatchTools.createWith(new Dispatcher<Context>()
			{
				@Override
				public boolean dispatch(Context ctx)
				{
					if(getPath.equals(ctx.getRequestUrl().getPath()))
					{
						SAVE_RESTORE.dispatch(ctx);
					}
					
					if(savePath.equals(ctx.getRequestUrl().getPath()))
					{
						SAVE_RESTORE.dispatch(ctx);
						
					}
					
					return false;
				}
			})
		);
	}

	public Dispatcher<Context> generateSourceApiDispatcher()
	{
		final byte[] RPC_SORUCE = getJsApiCall(true).getBytes();
		return new Dispatcher<Context>()
		{
			@Override
			public boolean dispatch(Context ctx)
			{
				try
				{
					ctx.getResponse().setContentType("text/javascript");
					ServletOutputStream os = ctx.getResponse().getOutputStream();
					os.write(RPC_SORUCE);
					os.flush();
				}
				catch(Exception e)
				{
					Mirror.propagateAnyway(e);
				}
				
				ctx.finishOperation();
				return true;
			}
		};
	}
	
	public void registerApiSourceJs(CorDispatcher<Context> dispatcher, String chain, String path)
	{
		dispatcher.getChainByName(chain).addLink
		(
			WebDispatchTools.exactPath
			(
				UrlNodePatternTools.assemble(UrlPart.PATH, StringFunctions.isEquals(path)),
				generateSourceApiDispatcher()
			)
		);
		
	}

	public void registerFullstack(CorDispatcher<Context> dispatcher, String chain, String apiSourcePath, String restPath, String websocketPath)
	{
		registerApiSourceJs(dispatcher, "static", apiSourcePath);
		registerApiDispacher(dispatcher, "static", restPath, websocketPath);
	}

	public RpcFacility<SimpleRpcRequest> getApi()
	{
		return new RpcFacility<SimpleRpcRequest>()
		{
			@Override
			public DataObject getBy(SimpleRpcRequest a)
			{
				return dispatch(a);
			}

			@Override
			public String getRpcName()
			{
				return apiName;
			}

			@Override
			public DataObject dispatch(SimpleRpcRequest req)
			{
				((SimpleRpcSession)req.getRpcSession()).setProtocolHandler(DEFAULT_PROTOCOL);
				sessionStart((SimpleRpcSession) req.getRpcSession(), null);
				return SaacRpc.DISPATCH.dispatch(req);
			}

			@Override
			public Collection<? extends RpcFunction<SimpleRpcRequest, ?>> getWrappedFunctions()
			{
				return SaacRpc.DISPATCH.getWrappedFunctions();
			}
		};
	}
}
