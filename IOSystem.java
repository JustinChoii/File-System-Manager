import java.util.*;
import java.io.*;

public class IOSystem{
	private static final int L = 64;
	private static final int B = 64;


	public byte ldisk[][];
	// ldisk[0] = bitmap
	// ldisk[1-6] = Descriptors
	// ldisk[1][0] = Directory
	// ldisk[7-63] = Blocks for files

	public IOSystem(){
		this.ldisk = new byte[L][B];
		// Pack negative 1s into ldisk to indicate a free block.

		for(int i = 0;i < 64; i++){
			this.ldisk[i] = new byte[B];
			for (int j = 0; j < B; j++){
				this.ldisk[i][j] = -1;
			}
		}




		
	}

	public byte[] read_block(int index){
		return ldisk[index];
	}

	public int[] read_block1(int index){
        // Code to read a block, and turn it into an int array.
        PackableMemory pm = new PackableMemory(64);
        pm.mem = this.read_block(index);
        int[] temp = new int[16];
        for (int i = 0; i < 16; i++){
        	temp[i] = pm.unpack(i * 4);
        	System.out.println(temp[i]);
        }
        return temp;
	}

	public void write_block(int index, byte[] buffer){
		if (index == 0){
			return;
		}
		if (buffer.length == 64){
			this.ldisk[index] = buffer;
		}
	}

	public void write_block1(int index, int[] buffer){
		if (buffer.length == 16){
			PackableMemory pm = new PackableMemory(64);
			pm.mem = this.ldisk[index];
			for (int i = 0; i < buffer.length; i++){
				pm.pack(buffer[i], i * 4);
				int temp = pm.unpack(i * 4);
				System.out.print(temp + " ");
				pm.pack(buffer[i], i * 4);

			}
			this.ldisk[index] = pm.mem;
		}
	}




	public static void main(String args[]){
		IOSystem io = new IOSystem();

        int[] temp = new int[16];
        for (int i= 0; i < 16; i++){
        	temp[i] = i;
        }

        io.write_block1(63, temp);
        

        PackableMemory pm = new PackableMemory(64);

        
        // Code to read a block, and turn it into an int array.
        io.read_block1(63);

        



	}
}
