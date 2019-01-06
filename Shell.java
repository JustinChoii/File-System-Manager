import java.io.*;
import java.util.*;

public class Shell{

	public static void main(String[] args) throws Exception{

	
		File file = new File("input.txt");
		BufferedReader br = new BufferedReader(new FileReader("input.txt"));
		byte[] b = new byte[4096];
		FileReader fr = null;
		FileWriter fw = new FileWriter("40532889.txt");
		BufferedWriter bw = new BufferedWriter(fw);
		FileSystem fs = new FileSystem();

		fr = new FileReader(file);
		String line;
		while ((line = br.readLine()) != null){

			String[] arguments = line.split(" ");
			String target = arguments[0];

			if(target.equals("cr")){
				String temp = arguments[1];
				fs.create(temp);
				bw.write(temp + " created\n");
			}
			else if(target.equals("de")){
				String temp = arguments[1];
				fs.destroy(temp);
				bw.write(temp + " destroyed\n");
			}
			else if (target.equals("op")){
				String temp = arguments[1];
				int ret = fs.open(temp);
				bw.write(temp + " opened " + ret + "\n");
			}
			else if (target.equals("cl")){
				String temp = arguments[1];
				int ret = Integer.parseInt(temp);
				fs.close(ret);
				bw.write(ret + ". closed\n");
			}
			else if(target.equals("rd")){
				String rets = "";
				try{
					int temp = Integer.parseInt(arguments[1]);
					int temp2 = Integer.parseInt(arguments[2]);
					rets = fs.read(temp, temp2);
					bw.write(rets + "\n");
				}
				catch(ArrayIndexOutOfBoundsException e){
					bw.write(rets + "\n");
				}
			}
			else if (target.equals("wr")){
				int reta = 0;
				try{
					int temp = Integer.parseInt(arguments[1]);
					char c = arguments[2].charAt(0);
					int temp2 = Integer.parseInt(arguments[3]);
					reta = fs.write(temp, c, temp2);
					bw.write(reta + " bytes written\n");
				}
				catch (ArrayIndexOutOfBoundsException e){
					bw.write(reta + " bytes written\n");
				}
				//ArrayIndexOutOfBoundsException

			}
			else if(target.equals("sk")){
				int temp = Integer.parseInt(arguments[1]);
				int temp2 = Integer.parseInt(arguments[2]);
				fs.seek(temp, temp2);
				bw.write("position is " + temp2 + "\n");
			}
			else if(target.equals("dr")){
				ArrayList<String> ret = fs.list();
				for (int i = 0; i < ret.size(); i++){
					bw.write(ret.get(i) + " ");
				}
				bw.write("\n");
			}

			else if(target.equals("in") && arguments.length == 1){
				System.out.println("in");
				fs.ldisk = new IOSystem();
				fs.bitmap = new BitMap();
				fs.bm = fs.bitmap.bm;
				fs.initialize_ldisk();
				fs.oft = new OFT[4];
				for (int i = 0; i < 4; i++){
					byte[] temp = new byte[64];
					fs.oft[i] = new OFT(temp, -1, -1, 0);
				}
				fs.oft[0].fdIndex = 0;
				bw.write("disk initialized\n");
			}
			else if(target.equals("in") && arguments.length >1){
				System.out.println("in");
				// fs.restoreDisk(b);
				fs.ldisk = new IOSystem();
				fs.bitmap = new BitMap();
				fs.bm = fs.bitmap.bm;
				fs.initialize_ldisk();
				fs.oft = new OFT[4];
				for (int i = 0; i < 4; i++){
					byte[] temp = new byte[64];
					fs.oft[i] = new OFT(temp, -1, -1, 0);
				}
				fs.oft[0].fdIndex = 0;
				bw.write("disk restored\n");
			}
			else if(target.equals("sv")){
				String temp = arguments[1];
				fs.save(temp);
				bw.write("disk saved\n");
			}
			else{
				bw.write("\n");
			}

		}

		br.close();
		bw.close();
		
	}
}