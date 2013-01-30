package popjava.scripts;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ScriptUtils {
	
	private static final String NEWLINE = System.getProperty("line.separator");
	
	public static boolean containsOption(String [] args, String option){
		for(String argument: args){
			if(argument.equals(option)){
				return true;
			}
		}
		
		return false;
	}
	
	public static boolean removeOption(List<String> parameters, String ... options){
		for(String argument: parameters){
			for(String option: options){
				if(option.equals(argument)){
					parameters.remove(option);
					return true;
				}
			}
		}
		return false;
	}
	
	public static String getOption(List<String> parameters, String defaultValue, String ... options){
        for(int i = 0; i < parameters.size(); i++){
        	for(String option: options){
        		if(parameters.get(i).equals(option)){
                    if(parameters.size() > i + 1){
                    	String value = parameters.get(i+1);
                    	parameters.remove(i);
                    	parameters.remove(i);
                        return value;
                    }
                }
        	}            
        }
        
        return defaultValue;
    }
	
	public static List<String> arrayToList(String ... args){
		ArrayList<String> arguments = new ArrayList<String>();
		
		for(String arg: args){
			arguments.add(arg);
		}
		return arguments;
	}
	
	public static String [] listToArray(List<String> list){
		String [] array = new String[list.size()];
		
		int index = 0;
		for(String item: list){
			array[index++] = item;
		}
		
		return array;
	}
	
	public static void runNativeApplication(String [] arguments, String notFoundError, BufferedWriter out, boolean verbose){
		if(verbose){
			for(String arg: arguments){
				System.out.print(arg+" ");
			}
			System.out.println();
		}
		
		
		try {
			Process p = Runtime.getRuntime().exec(arguments);
			if(p.waitFor() == 2){
				System.err.println(notFoundError);
			}
			
			BufferedReader ok = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			while((line = ok.readLine()) != null){
				if(out != null){
					out.write(line + NEWLINE);
				}else if(verbose){
					System.out.println(line);
				}
			}
			if(out != null){
				out.close();
			}
			
			BufferedReader error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			while((line = error.readLine()) != null){
				System.out.println(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
