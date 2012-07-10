package org.infinispan.dataplacement.c50;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.clearspring.analytics.util.Pair;

public class NameSplitter {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws NumberFormatException 
	 */
	public static void main(String[] args) throws NumberFormatException, IOException {
		// TODO Auto-generated method stub
        String path = args[0];
        if(path.endsWith("/")==false)
        	path += "/";
		readFileAndWrite(path);
	}
	
	public static void readFileAndWrite(String folder) throws NumberFormatException, IOException{
		List<String> files = listFile(folder);
		List<String> keys = new ArrayList<String>();

		String outputFolder = folder +"splits/";
		File f = new File(outputFolder);
		if(!f.exists()){   
		  f.mkdir();
		}
		int flag = 0, counter = 0;
		for (String file : files) {
			Reader.getInstance().StartReading(folder + file);
			FileWriter fw = new FileWriter(outputFolder + file + "_splitted");
			BufferedWriter bw = new BufferedWriter(fw);
			while (true) {			
				keys = new ArrayList<String>();
				flag = Reader.getInstance().GetElements(keys);
				if(flag==0)
					break;
				else{
				 List<String> splittedKeys = splitKeys(keys);
				 writeKeys(bw,splittedKeys, counter);
				}
			}
			bw.close();
			fw.close();
			Reader.getInstance().StopReading();
			++counter;
		}
	}
	
	public static List<String> splitKeys(List<String> keys){
		int size = keys.size();
		List<String> splittedKeys = new ArrayList<String>();
		String key = null;
		for(int i = 0; i< size; ++i){
			key = keys.get(i);
			if(key.startsWith("O")){
				String[] parts = key.split("_");
				String num1, num2, num3;
				num1 = parts[1];
				num2 = parts[2];
				num3 = parts[3];
				splittedKeys.add(num1 + ","+num2 + ","+num3 +",N/A,N/A,N/A,N/A,N/A");
				//keys.set(i,num1 + ","+num2 + ","+num3 +",N/A,N/A,N/A,N/A,N/A");
			}
			else if(key.startsWith("W")){
				String num = key.split("_")[1];
				splittedKeys.add("N/A,N/A,N/A,"+ num+",N/A,N/A,N/A,N/A");
				//keys.set(i,"N/A,N/A,N/A,"+ num+",N/A,N/A,N/A,N/A");
			}
			else if(key.startsWith("D")){
				String[] parts = key.split("_");
				String num1, num2;
				num1 = parts[1];
				num2 = parts[2];
				splittedKeys.add("N/A,N/A,N/A,N/A,"+num1 + ","+num2 +",N/A,N/A");
				//keys.set(i,"N/A,N/A,N/A,N/A,"+num1 + ","+num2 +",N/A,N/A");
			}
			else if(key.startsWith("S")){
				String[] parts = key.split("_");
				String num1, num2;
				num1 = parts[1];
				num2 = parts[2];
				splittedKeys.add("N/A,N/A,N/A,N/A,N/A,N/A,"+num1 + ","+num2);
				//keys.set(i,"N/A,N/A,N/A,N/A,N/A,N/A,"+num1 + ","+num2);
			}
//			else{
//				keys.remove(i);
//				--size;
//			}
		}
		return splittedKeys;
	}
	
	public static Integer[] splitSingleKey(String key){
            Integer[] fullKey = {null,null,null,null,null,null,null,null};
			if(key.startsWith("O")){
				String[] parts = key.split("_");
				String num1, num2, num3;
				fullKey[0] = Integer.parseInt(parts[1]);
				fullKey[1] = Integer.parseInt(parts[2]);
				fullKey[2] = Integer.parseInt(parts[3]);
				return fullKey;
			}
			else if(key.startsWith("W")){
				String num = key.split("_")[1];
				fullKey[3] = Integer.parseInt(num);
				//splittedKeys.add("N/A,N/A,N/A,"+ num+",N/A,N/A,N/A,N/A");
				return fullKey;
				//keys.set(i,"N/A,N/A,N/A,"+ num+",N/A,N/A,N/A,N/A");
			}
			else if(key.startsWith("D")){
				String[] parts = key.split("_");
				String num1, num2;
				fullKey[4] = Integer.parseInt(parts[1]); //num1 = parts[1];
				fullKey[5] = Integer.parseInt(parts[2]);//num2 = parts[2];
				//splittedKeys.add("N/A,N/A,N/A,N/A,"+num1 + ","+num2 +",N/A,N/A");
				return fullKey;
				//keys.set(i,"N/A,N/A,N/A,N/A,"+num1 + ","+num2 +",N/A,N/A");
			}
			else if(key.startsWith("S")){
				String[] parts = key.split("_");
				String num1, num2;
				fullKey[6] = Integer.parseInt(parts[1]);
				fullKey[7] = Integer.parseInt(parts[2]);//num1 = parts[1];
				//num2 = parts[2];
				return fullKey;
				//splittedKeys.add("N/A,N/A,N/A,N/A,N/A,N/A,"+num1 + ","+num2);
				//keys.set(i,"N/A,N/A,N/A,N/A,N/A,N/A,"+num1 + ","+num2);
			}
	    return fullKey;
	}
	
	
	public static List<Pair<String, Integer>> splitPairKeys(List<Pair<String, Integer>> keys){
		int size = keys.size();
		List<Pair<String, Integer>> splittedKeys = new ArrayList<Pair<String, Integer>>();
		String key = null;
		for(int i = 0; i< size; ++i){
			key = keys.get(i).left;
			if(key.startsWith("O")){
				String[] parts = key.split("_");
				String num1, num2, num3;
				num1 = parts[1];
				num2 = parts[2];
				num3 = parts[3];
				splittedKeys.add(new Pair<String, Integer>(num1 + ","+num2 + ","+num3 +",N/A,N/A,N/A,N/A,N/A", keys.get(i).right));
				//keys.set(i,num1 + ","+num2 + ","+num3 +",N/A,N/A,N/A,N/A,N/A");
			}
			else if(key.startsWith("W")){
				String num = key.split("_")[1];
				splittedKeys.add(new Pair<String, Integer>("N/A,N/A,N/A,"+ num+",N/A,N/A,N/A,N/A", keys.get(i).right));
				//keys.set(i,"N/A,N/A,N/A,"+ num+",N/A,N/A,N/A,N/A");
			}
			else if(key.startsWith("D")){
				String[] parts = key.split("_");
				String num1, num2;
				num1 = parts[1];
				num2 = parts[2];
				splittedKeys.add(new Pair<String, Integer>("N/A,N/A,N/A,N/A,"+num1 + ","+num2 +",N/A,N/A", keys.get(i).right));
				//keys.set(i,"N/A,N/A,N/A,N/A,"+num1 + ","+num2 +",N/A,N/A");
			}
			else if(key.startsWith("S")){
				String[] parts = key.split("_");
				String num1, num2;
				num1 = parts[1];
				num2 = parts[2];
				splittedKeys.add(new Pair<String, Integer>("N/A,N/A,N/A,N/A,N/A,N/A,"+num1 + ","+num2, keys.get(i).right));
				//keys.set(i,"N/A,N/A,N/A,N/A,N/A,N/A,"+num1 + ","+num2);
			}
		}
		return splittedKeys;
	}
	
	
	public static void writeKeys(BufferedWriter bw, List<String> keys, int counter) throws IOException{

		for(String key : keys){
			bw.write(key+" 2"+ counter+"\n");
		}
	}
	
	public static void writePairKeys(BufferedWriter bw, List<Pair<String, Integer>> keys) throws IOException{
		for(Pair<String, Integer> key : keys){
			bw.write(key.left+","+key.right+"\n");
		}
	}
	
	public static List<String> listFile(String folder) {
	String[] lsfiles = new File(folder).list();
	String namePrefix = new String();
//	if (isReadingTop == true)
//		namePrefix = "top-keys";
//	else
//		namePrefix = "all-keys"; 
	//file2.startsWith(namePrefix)&&
	List<String> files = new ArrayList<String>();
	Integer fileOrder = 0;
	for (String file : lsfiles) {
		for (String file2 : lsfiles) {
			if (file2.length()>16 && file2.substring(14, 15).equals(fileOrder.toString()) &&file2.endsWith("splitted")== false) {
				++fileOrder;
				files.add(file2);
				break;
			}
		}
	}
	return files;
}

}