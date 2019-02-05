package eu.javaexperience.saac;

import java.io.File;
import java.io.IOException;

import eu.javaexperience.dispatch.Dispatcher;
import eu.javaexperience.io.IOTools;
import eu.javaexperience.web.Context;
import eu.javaexperience.web.HttpTools;
import eu.javaexperience.web.facility.SiteFacilityTools;

public class SaacWebTools
{
	/**
	 * ?file=path will be saved(on POST) and send(on GET). 
	 * */
	public static Dispatcher<Context> createSaveRestoreDispatcher(final File directory) throws IOException
	{
		final String strRoot = directory.getCanonicalFile().toString();
		return new Dispatcher<Context>()
		{
			@Override
			public boolean dispatch(Context ctx)
			{
				try
				{
					String file = ctx.getRequest().getParameter("file");
					if(null == file)
					{
						SiteFacilityTools.finishWithElementSend(ctx, "{\"result\":\"No 'file' given.\"}");
						ctx.finishOperation();
					}
					
					File dst = new File(directory+"/"+file);
					dst = dst.getCanonicalFile();
					if(!dst.toString().startsWith(strRoot))
					{
						SiteFacilityTools.finishWithElementSend(ctx, "{\"result\":\"access denied\"}");
						ctx.finishOperation();
					}
					
					
					if(HttpTools.isPost(ctx.getRequest()))
					{
						byte[] data = (byte[]) ctx.getRequest().getAttribute("data");
						IOTools.createPathBeforeFile(dst);
						IOTools.putFileContent(dst.toString(), data);
						SiteFacilityTools.finishWithElementSend(ctx, "{\"result\":\"ok\"}");
						
						ctx.finishOperation();
					}
					else
					{
						String ret = "";
						try
						{
							ret = IOTools.getFileContents(dst);
						}
						catch (Exception e)
						{
							e.printStackTrace();
						}
						
						SiteFacilityTools.finishWithElementSend(ctx, ret);
						
						ctx.finishOperation();
					}
				}
				catch (IOException e)
				{
					SiteFacilityTools.finishWithElementSend(ctx, "{\"result\":\"IOError: "+e.getMessage()+"\"}");
				}
				return true;
			}
		};
	}
}
