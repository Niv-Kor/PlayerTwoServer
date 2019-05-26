import com.hit.server_side.connection.GeneralService;
import com.hit.server_side.connection.PortGenerator;
import com.hit.server_side.connection.Server;
import com.hit.server_side.game_controlling.ServerSideController;

public class GameServerDriver
{
	public static void main(String[] args) {
		CLI cli = new CLI(System.in, System.out);
		Server server = new Server(PortGenerator.CLIENT_FINDER);
		cli.addPropertyChangeListener(server);
		new Thread(cli).start();
		
		//TODO move
		try {
			GeneralService.init();
			ServerSideController.init();
		}
		catch(Exception e) {
			System.err.println("Server is unavailable.");
			e.printStackTrace();
		}
	}
}