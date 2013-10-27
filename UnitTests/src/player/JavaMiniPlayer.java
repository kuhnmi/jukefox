/* 
 * Copyright 2008-2013, ETH ZÃ¼rich, Samuel Welten, Michael Kuhn, Tobias Langner,
 * Sandro Affentranger, Lukas Bossard, Michael Grob, Rahul Jain, 
 * Dominic Langenegger, Sonia Mayor Alonso, Roger Odermatt, Tobias Schlueter,
 * Yannick Stucki, Sebastian Wendland, Samuel Zehnder, Samuel Zihlmann,       
 * Samuel Zweifel
 *
 * This file is part of Jukefox.
 *
 * Jukefox is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software 
 * Foundation, either version 3 of the License, or any later version. Jukefox is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Jukefox. If not, see <http://www.gnu.org/licenses/>.
 */
package player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import maryb.player.Player;
import maryb.player.PlayerEventListener;
import maryb.player.PlayerState;

import org.naturalcli.Command;
import org.naturalcli.ExecutionException;
import org.naturalcli.ICommandExecutor;
import org.naturalcli.InvalidSyntaxException;
import org.naturalcli.NaturalCLI;
import org.naturalcli.ParseResult;
import org.naturalcli.commands.HelpCommand;

public class JavaMiniPlayer implements PlayerEventListener, Runnable {
	private static Player player;
	private int currentIndex;
	private ArrayList<String> songs; 
	
	public JavaMiniPlayer() {
		player = new Player();
        player.setCurrentVolume( 0.9f );
        player.setListener(this);
        currentIndex = 0;
        songs = new ArrayList<String>();
        songs.add("res\\testlibrary\\Die Ärzte\\BäST OF\\Die Ärzte - 17 - Yoko Ono.mp3");
        songs.add("C:\\Users\\Public\\Music\\BäST OF\\Die Ärzte - 01 - Schrei nach Liebe.mp3");
        songs.add("C:\\Users\\Public\\Music\\BäST OF\\Die Ärzte - 03 - Friedenspanzer.mp3");
	}
    
    @Override
	public void run() {
	    try {
	    	System.out.println("command 'play' starts the player.");
			player.setSourceLocation( songs.get(currentIndex) );
			
			Scanner scanner = new Scanner(System.in);
			Set<Command> commands = initCli();
			NaturalCLI cli = new NaturalCLI(commands);

			while (true) {
				String next = scanner.nextLine();
				if (next.isEmpty()) {
					continue;
				}
				try {
					cli.execute(next);
				} catch (ExecutionException e) {
					System.out.println("Invalid input, please use following commands: ");
					cli.execute("help");
				}
				if (next.equals("exit")){
					break;
				}
				
			}

			player.stop();
		} catch (InvalidSyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private Set<Command> initCli() throws InvalidSyntaxException {
    	  // Create the commands
    	  Command exit =
    	    new Command(
    	    "exit", 
    	    "shut down", 
    	    new ICommandExecutor ()
    	    {
    	       @Override
			public void execute(ParseResult pr){
    	    	 System.out.println("good bye...");
    	       }
    	    }		
    	  );
    	  
    	  Command play =
    	    new Command(
    	    "play", 
    	    "Plays the song.", 
    	    new ICommandExecutor ()
    	    {
    	      @Override
			public void execute(ParseResult pr ) 
    	      {  player.play();  }
    	    }		
    	  );
    	  
    	  Command stop =
    	    	    new Command(
    	    	    "stop", 
    	    	    "Stops the song.", 
    	    	    new ICommandExecutor ()
    	    	    {
    	    	      @Override
    				public void execute(ParseResult pr ) 
    	    	      {  player.stop();  }
    	    	    }		
    	    	  );
    	  
    	  Command pause =
  	    	    new Command(
  	    	    "pause", 
  	    	    "Pause the song.", 
  	    	    new ICommandExecutor ()
  	    	    {
  	    	      @Override
  				public void execute(ParseResult pr ) 
  	    	      {  player.pause();  }
  	    	    }		
  	    	  );
    	  
    	  Command next =
  	    	    new Command(
  	    	    "next", 
  	    	    "Play next song.", 
  	    	    new ICommandExecutor ()
  	    	    {
  	    	      @Override
  				public void execute(ParseResult pr ) {
//  	    	    	try {
//						player.stopSync();
//					} catch (InterruptedException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
  	    	    	player.setSourceLocation(songs.get(nextIndex()));
  	    	    	try {
						player.stopSync();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
  	    	    	player.play();
  	    	      }
  	    	    }		
  	    	  );
    	  
    	  Command state =
    	    	    new Command(
    	    	    "state", 
    	    	    "Print state of the player.", 
    	    	    new ICommandExecutor ()
    	    	    {
    	    	      @Override
    				public void execute(ParseResult pr ) {
  						PlayerState playerState = player.getState();
    	    	    	System.out.println(playerState.name());
    	    	    	System.out.println(player.getCurrentBufferedTimeMcsec());
    	    	      }
    	    	    }		
    	    	  );
    	   
    	  // Create the set of commands
    	  Set<Command> cs = new HashSet<Command>();
    	  cs.add(new HelpCommand(cs));
    	  cs.add(exit);
    	  cs.add(play);
    	  cs.add(stop);
    	  cs.add(pause);
    	  cs.add(next);
    	  cs.add(state);
    	   
    	  // Execute
    	  return cs;
    }

	@Override
	public void buffer() {
		System.out.println("buffer");
		// TODO Auto-generated method stub
	}

	@Override
	public void endOfMedia() {
		player.setSourceLocation(songs.get(nextIndex()));
		player.play();
	}

	@Override
	public void stateChanged() {
		// TODO Auto-generated method stub
	}
	
	public int nextIndex() {
		currentIndex = (currentIndex + 1) % songs.size();
		return currentIndex;
	}
	
	 public static void waiting (int n){
	        
	        long t0, t1;

	        t0 =  System.currentTimeMillis();

	        do{
	            t1 = System.currentTimeMillis();
	        }
	        while (t1 - t0 < n);
	    }

}

