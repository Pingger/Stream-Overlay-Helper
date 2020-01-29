package pingger.obsAutomation;

import java.net.InetSocketAddress;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import pingger.obsAutomation.monitors.MouseMonitor;
import pingger.obsAutomation.monitors.PixelMonitor;
import pingger.obsAutomation.monitors.OverlayManager;

public class Main extends WebSocketServer
{
	public static Main				main;
	public static OverlayManager	pm;
	private static MouseMonitor		mm;

	public static void main(String[] args) throws Exception
	{
		pm = new OverlayManager();
		new Thread(new PixelMonitor()).start();
		mm = new MouseMonitor();
		Thread mmt = new Thread(mm);
		mmt.setDaemon(true);
		mmt.setName("MouseMonitorThread");
		mmt.start();
		main = new Main();
		main.start();
		try
		{
			while (System.in.available() == 0)
			{
				//main.broadcast(mm.shouldShow() ? "ow show" : "ow hide");
				Thread.sleep(50);
			}
			System.exit(0);
		}
		finally
		{
			System.exit(1);
			main.stop();
		}
	}

	public Main()
	{
		super(new InetSocketAddress(1248));
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote)
	{
		System.out.println("Closed: " + conn.getRemoteSocketAddress() + " Code:" + code + " Reason:" + reason);
	}

	@Override
	public void onError(WebSocket conn, Exception ex)
	{
		System.err.println("Error: " + conn.getRemoteSocketAddress());
		ex.printStackTrace();
	}

	@Override
	public void onMessage(WebSocket conn, String message)
	{
		System.out.println("Recv: " + conn.getRemoteSocketAddress() + " - " + message);
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake)
	{
		System.out.println("Open: " + conn.getRemoteSocketAddress());
		conn.send("ow " + (mm.shouldShow ? "show" : "hide"));
		pm.onConnect();
	}

	@Override
	public void onStart()
	{
		System.out.println("Start!");
	}

}
