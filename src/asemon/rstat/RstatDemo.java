package asemon.rstat;

import java.net.InetAddress;

import org.acplt.oncrpc.OncRpcProtocols;


public class RstatDemo {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Starting");
		rstatClient client = null;
		double interval = 0;
		long sleepTime = 1000;
		
		try
		{
//			client = new rstatClient(InetAddress.getByName("sekax010.epk.ericsson.se"), rstat.RSTATPROG, rstat.RSTATVERS_TIME, 32780,  OncRpcProtocols.ONCRPC_UDP);
			client = new rstatClient(InetAddress.getByName("sekax010.epk.ericsson.se"), rstat.RSTATPROG, rstat.RSTATVERS_TIME, OncRpcProtocols.ONCRPC_UDP);
		}
		catch(Exception e)
		{
			System.out.println("rstatDemo: oops when creating RPC client:");
	    	e.printStackTrace(System.out);
		}
        client.getClient().setTimeout(300*1000);

        try {
        	statsvar sample1 = client.RSTATPROC_STATS_4();
    		
    		double calcCpu1 = ((sample1.cp_time[0]+ sample1.cp_time[1])*100.0/(sample1.cp_time[0]+ sample1.cp_time[1] + sample1.cp_time[2] + sample1.cp_time[3]));
 
    		System.out.println("load       : " + calcCpu1);
    		
        } catch ( Exception e ) {
            e.printStackTrace(System.out);
        }

		System.out.println("End");
	}

}
