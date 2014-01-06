package org.tue.students.patents.landscape;

public class LOG {
	
	public static final boolean displayLogs = true;
	private static boolean waiting = false;
	
	/** Logs a message. */
	public static void m(String tag, String message){
		if (displayLogs){
			System.out.println("["+tag+"] "+message);
		}
	}
	
	/** Log a process. Must be ended with a call to pe. */
	public static void pb(String tag, String message){
		if (displayLogs){
			if (waiting){
				System.out.println();
			}
			System.out.print("["+tag+"] PROCESS: "+message+"...");
			waiting = true;
		}
	}
	
	/** Ends a process. */
	public static void pe(){
		if (displayLogs){
			System.out.println(" done!");
			waiting = false;
		}
	}
	
	/** Logs an error. */
	public static void e(String tag, String message){
		if (displayLogs)
			System.out.println("["+tag+"] ERROR: "+message);
	}
}
