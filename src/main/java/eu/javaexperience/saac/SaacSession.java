package eu.javaexperience.saac;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;

import eu.javaexperience.interfaces.simple.publish.SimplePublish1;
import eu.javaexperience.io.AppendableLocklessWriter;
import eu.javaexperience.log.ThreadLocalHookableLogFacility;
import eu.javaexperience.resource.ReferenceCounted;
import eu.javaexperience.rpc.RpcRequest;
import eu.javaexperience.rpc.RpcTools;
import eu.javaexperience.rpc.SimpleRpcSession;
import eu.javaexperience.rpc.bidirectional.BidirectionalRpcProtocolHandler;
import eu.javaexperience.web.features.WebSocket;

public class SaacSession
{
	protected SimpleRpcSession rpcSession;
	
	public SaacSession(SimpleRpcSession sess)
	{
		this.rpcSession = sess;
	}
	
	protected Vector<SaacExecution> processes = new Vector<>();
	
	
	public void addProcess(SaacExecution env)
	{
		processes.add(env);
	}
	
	public void removeProcess(SaacExecution env)
	{
		processes.remove(env);
	}
	
	public void sendServerEvent(String _this, String method, Object... arguments) throws IOException
	{
		BidirectionalRpcProtocolHandler PROTO = (BidirectionalRpcProtocolHandler) rpcSession.get("PROTOCOL");
		WebSocket SEND = (WebSocket) rpcSession.get("SEND");
		
		RpcRequest req = RpcTools.createClientInvocation(rpcSession, 0, _this, method, arguments);
		
		SEND.send(req.getRequestData().getImpl().toString().getBytes());
	}

	public ReferenceCounted<PrintWriter> setContextLogger()
	{
		ReferenceCounted<PrintWriter> wsLog = new ReferenceCounted<PrintWriter>(AppendableLocklessWriter.asNewLineDispatcher(new SimplePublish1<String>()
		{
			@Override
			public void publish(String a)
			{
				try
				{
					sendServerEvent(null ,"newLogLine", a);
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}), 1)
		{
			@Override
			protected void onFree()
			{
				ThreadLocalHookableLogFacility.setLocalOutput(null);
			}
		};
		
		ThreadLocalHookableLogFacility.setLocalOutput(wsLog);
		return wsLog;
	}
}
