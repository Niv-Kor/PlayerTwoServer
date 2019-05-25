import com.hit.server_side.connection.GeneralService;
import com.hit.server_side.game_controlling.ServerSideController;

public class Main
{
	public static boolean debug = false;

	public static void main(String[] args) {
		try {
			GeneralService.init();
			ServerSideController.init();
			CLI cli = new CLI();
			cli.run();
		}
		catch(Exception e) {
			System.err.println("Server is unavailable.");
			e.printStackTrace();
		}
	}
}