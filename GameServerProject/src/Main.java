public class Main
{
	public static boolean debug = false;

	public static void main(String[] args) {
			//ServerLogger.print("Server: started...");
			//GeneralService.init();
			//ServerSideController.init();
			//ServerSideController.find();
			CLI cli = new CLI();
			cli.run();
	}
}