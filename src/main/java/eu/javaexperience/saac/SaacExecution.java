package eu.javaexperience.saac;

public class SaacExecution
{
	protected final SaacEnv env;
	protected SaacSession session; 
	
	
	public SaacExecution(SaacEnv env, SaacSession session)
	{
		this.env = env;
		this.session = session;
	}
	
	public void asserValid()
	{
		
	}

	protected Thread runner;
	
	protected long startedOn = 0;
	
	public void runAsync()
	{
		runner = new Thread()
		{
			@Override
			public void run()
			{
				startedOn = System.currentTimeMillis();
			}
		};
		runner.run();
	}
}
