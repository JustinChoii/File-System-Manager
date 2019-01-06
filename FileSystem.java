import java.util.*;
import java.io.*;
import java.lang.*;


// ldisk[0] = Bitmap
// ldisk[1-6] = File Descriptors
// 		ldisk[1][0] = Descriptor for Directory
// ldisk[7-9] = Directory
// ldisk[10-63] = Data blocks

		
public class FileSystem{

	public IOSystem ldisk;
	public BitMap bitmap;
	public OFT[] oft;
	private PackableMemory pm;
	public int[] bm;
	private byte[] directory;
	private int file_count = 0;
	private int open_file_count = 0;

	public FileSystem(){
		// Initialize ldisk and bitmap
		this.ldisk = new IOSystem();
		this.bitmap = new BitMap();
		this.bm = bitmap.bm;
		initialize_ldisk();
		
		// Initilize OFT
		this.oft = new OFT[4];
		for (int i = 0; i < 4; i++){
			byte[] temp = new byte[64];
			oft[i] = new OFT(temp, -1, -1, 0);
		}

		// OFT[0] is used for the Directory. The directory will be inside fd[0] of block 1.
		oft[0].fdIndex = 0;

		// Initialize PackableMemory

	}

	public int create(String name){
		// Once works, test to see if it is fine with more than 5 files (goes into fd2). 
		// Check to see if directory is set up properly.

		//byte[] dir = this.ldisk.read_block(1);

		// Find a free file descriptor

		int current_fd_block = 1;
		// keep track of the fd_index, so we can label the directory.
		int current_fd_index = 1;
		// Reference starts at 4 because 0-3 is the fd for directory.
		int current_position = 4;
		int curr = -1;
		int free_descriptor = -1;
		int free_descriptor_index = 0;
		int free_block = -1;

		int free_directory_index = 0;

		//byte[] fd_block = ldisk.read_block(1);
        byte[] fd_block = new byte[64];

        int done = 0;
        for (int i = 1; i <= 6; i++){
        	if (free_descriptor_index > 23){
        		System.out.println("descriptor_index out of bounds");
        		break;
        	}
	        fd_block = this.ldisk.read_block(i);


        	for (int j = 0; j < 64; j+= 16){
		        if (i == 1 && free_descriptor_index == 0){
		        	fd_block[0] = 0;
		        	fd_block[4] = 7;
		        	fd_block[8] = 8;
		        	fd_block[12] = 9;
		        	j = 16;
		        	free_descriptor_index++;
		        }
        		if  (fd_block[j] == -1){
        			// find open block
        			int free = this.bitmap.search_zero();
        			this.set_bitmap_one(free);

        			curr = j;
        			fd_block[j] = 0;
        			fd_block[j + 4] = (byte) free;
        			fd_block[j + 8] = -1;
        			fd_block[j + 12] = -1;
        			// Check to see if fd_index is being saved properly.
        			// It is good.
        			free_block = i;
        			done = 0;
        			free_descriptor_index++;
        			break;

        		}

        	}
    		if (free_block != -1){
    			break;
    		}
        }

        // Find a free directory entry

        // Directory is in ldisk[7] to ldisk[9];
        int dir_done = -1;
        for (int i = 7; i<=9;i++){
        	if (free_directory_index > 24){
        		System.out.println("descriptor_index out of bounds");
        		break;
        	}
        	// dir_block 

            byte[] dir_block = new byte[64];
        	dir_block = this.ldisk.read_block(i);

        	// slots are 2 ints each, 8 bytes each.
        	for (int j = 0; j < 64; j+=8){

        		// If directory slot is free
        		if (dir_block[j] == -1){
        			this.file_count += 1;
        			dir_done = 1;
        			// char = 1 byte
        			// we can put the name into 4 bytes (1 int)
        			for (int k = 0; k < name.length(); k++){
        				dir_block[j + k] = (byte) name.charAt(k);
        			}
        			if (name.length() == 1){
        				dir_block[j] = (byte) name.charAt(0);
        			}
        			else if (name.length() == 2){
        				dir_block[j] = (byte) name.charAt(0);
        				dir_block[j + 1] = (byte) name.charAt(1);
        			}
        			else if (name.length() == 3){
	        			dir_block[j] = (byte) name.charAt(0);
	        			dir_block[j + 1] = (byte) name.charAt(1);
	        			dir_block[j + 2] = (byte) name.charAt(2);
        			}

        			// set the descriptor
        			dir_block[j + 4] = (byte) (free_descriptor_index - 1);

        			break;
        		}
        		free_descriptor_index++;
        	}
        	if (dir_done != -1){
        		break;
        	}
        }



        // Need to update ldisk[1][0] -> length must be equal to this.file_count;
        byte[] temp1 = new byte[64];
        temp1 = this.ldisk.read_block(1);
        temp1[0] = (byte) this.file_count;


        return 1;

	}


	public boolean destroy(String filename) {
		byte[] dir_block = new byte[64];
		boolean found = false;
		// search directory to find file descriptor
		int found_descriptor_index = -1;
		int found_map1 = -1;
		int found_map2 = -1;
		int found_map3 = -1;
		for (int i = 7; i<= 9; i++){
			dir_block = this.ldisk.read_block(i);
			for (int j = 0; j < 64; j += 8){
				// read one slot at a time (8 bytes, 2 ints)
				// first int is a char (name)
				byte[] name = new byte[4];
				name[0] = dir_block[j];
				name[1] = dir_block[j + 1];
				name[2] = dir_block[j + 2];
				name[3] = dir_block[j + 3];
				String combine = new String (name);
				if (combine.contentEquals(filename)) {
					// System.out.println("line 323: match!");
					found_descriptor_index = (int) dir_block[j + 4];
					// System.out.println("line 330: found descriptor: " + found_descriptor_index);

					found = true;
					// Remove the entries from the directory
					for (int k = 0; k < 8; k++){
						dir_block[j + k] = -1;	
					}

					break;
				}
			}
		}
		if (found == false){
			return false;
		}


		// Update bitmap from the locations from descriptor.

		int fd_block_num = found_descriptor_index/4 + 1;
		byte[] fd_block = new byte[64];
		fd_block = this.ldisk.read_block(fd_block_num);

		int desc_index_in_block = 0;
		// If first block, we should only be looking at 1/2/3
		boolean p = false;
		if (fd_block_num == 1 && found_descriptor_index < 4){
			desc_index_in_block = found_descriptor_index;
			for (int j = 16; j < 64; j+= 16){
				if (fd_block[j + 4] != -1 && p == false){
					int temp = (int) fd_block[j+4];
					if (temp > 9){
						p = true;
						this.set_bitmap_zero(temp);
						fd_block[j + 4] = -1;
					}
				}
				else if (fd_block[j + 8] != -1 && p == false){
					int temp = (int) fd_block[j+8];
					if (temp > 9){
						p = true;
						this.set_bitmap_zero(temp);
						fd_block[j + 8] = -1;
					}	
				}
				else if (fd_block[j + 12] != -1 && p == false){
					int temp = (int) fd_block[j+12];
					if (temp > 9){
						p = true;
						this.set_bitmap_zero(temp);					
					

						fd_block[j + 12] = -1;
					}
				}
			}

		}
		// Else, convert the descriptor index to 0, 1, 2 or 3

		else{
			desc_index_in_block = found_descriptor_index % 4;
			for (int j = 0; j < 64; j+= 16){
				if (fd_block[j + 4] != -1 && p == false){
					int temp = (int) fd_block[j+4];
					if (temp > 9){
						p = true;

						this.set_bitmap_zero(temp);
						fd_block[j + 4] = -1;
					}
				}
				else if (fd_block[j + 8] != -1 && p == false){
					int temp = (int) fd_block[j+8];
					if (temp > 9){
						p = true;
						this.set_bitmap_zero(temp);
						fd_block[j + 8] = -1;
					}
				}
				else if (fd_block[j + 12] != -1 && p == false){
					int temp = (int) fd_block[j+12];
					if (temp > 9){
						p = true;
						this.set_bitmap_zero(temp);					
						fd_block[j + 12] = -1;
					}
				}
			}

		}

		// Removed directory entry properly, have not updated bit map or freed
		// file descriptor.
		
		// Free File Descriptor
		// found_descriptor_index = the file descriptor index starting from 1 - 23 
		// fd_block_num = the block number that the file descriptor resides in
		// Need to read in the block, and free the contents (set it to -1)
		byte[] fd_block_2 = new byte[64];
		fd_block_2 = this.ldisk.read_block(fd_block_num);
		int desc_index_in_block_2 = found_descriptor_index % 4;
		fd_block_2[desc_index_in_block_2 * 16] = -1;
		fd_block_2[desc_index_in_block_2 * 16 + 4] = -1;
		fd_block_2[desc_index_in_block_2 * 16 + 8] = -1;
		fd_block_2[desc_index_in_block_2 * 16 + 12] = -1;
		this.ldisk.write_block(fd_block_num, fd_block_2);

		if(found == true){
			this.file_count = this.file_count - 1;
	        // Need to update ldisk[1][0] -> length must be equal to this.file_count;
	        byte[] temp1 = new byte[64];
	        temp1 = this.ldisk.read_block(1);
	        temp1[0] = (byte) this.file_count;

			return true;
		}
		else{
			return false;
		}
	}

	public int open(String filename){  
		if (this.open_file_count >= 3){
			// System.out.println("Can't open more than 3 files. file: " + filename + " could not be opened.");
			return -1;
		}
		int oft_index = -1;

		byte[] dir_block = new byte[64];
		boolean found = false;

		// search directory to find file descriptor
		int found_descriptor_index = -1;
		int found_map1 = 0;
		int found_map2 = 0;
		int found_map3 = 0;
		for (int i = 7; i<= 9; i++){
			dir_block = this.ldisk.read_block(i);
			for (int j = 0; j < 64; j += 8){
				// read one slot at a time (8 bytes, 2 ints)
				// first int is a char (name)
				byte[] name = new byte[3];
				name[0] = dir_block[j];
				name[1] = dir_block[j + 1];
				name[2] = dir_block[j + 2];
				String combine = new String (name);
				if (combine.contentEquals(filename)) {
					// System.out.println("line 467: match!");
					found_descriptor_index = (int) dir_block[j + 4];
					// System.out.println("line 469: found descriptor: " + found_descriptor_index);

					found = true;

					break;
				}
			}
		}
		if (found == false){
			// System.out.println("Open(" + filename + "): file not found.");
			return -1;
		}


		boolean oft_found = false;
		// Allocate a free OFT entry (reuse deleted entries)
		for (int i = 1; i < 4; i++){

			// If the index is equal to -1, then it is free.
			// Set oft_index to i
			if (this.oft[i].fdIndex == -1 && oft_found != true){
				oft_index = i;
				oft_found = true;
				this.open_file_count += 1;
			}
		}
		// Set the OFT properties
		this.oft[oft_index].current_position = 0;
		this.oft[oft_index].fdIndex= found_descriptor_index;
		this.oft[oft_index].length = 0;

		// Read block 0 of file into the read/write buffer (read-ahead)

		int fd_block_num = found_descriptor_index/4 + 1;
		byte[] fd_block = new byte[64];
		fd_block = this.ldisk.read_block(fd_block_num);
		int fd_index_in_block = found_descriptor_index % 4;

		int block_index = (int) fd_block[fd_index_in_block * 16 + 4];
		// System.out.println("line 510: " + block_index);
		// if 22nd fd, then it must be in block 6, index 3 from (0, 1, 2, 3)

		byte[] oft_buffer = new byte[64];
		if (block_index == -1){
			// do nothing for now as block doesn't exist.
		}
		else{
			// block exists, so load it in to the buffer.
			oft_buffer = this.ldisk.read_block(block_index);
		}
		

		// Free File Descriptor
		// found_descriptor_index = the file descriptor index starting from 1 - 23 
		// fd_block_num = the block number that the file descriptor resides in
		// Need to read in the block, and free the contents (set it to -1)

		// byte[] fd_block = new byte[64];
		// fd_block_2 = this.ldisk.read_block(fd_block_num);

		// this.ldisk.write_block(fd_block_num, fd_block_2);

		return oft_index;
	}


	public int close(int index){
		if (this.oft[index].fdIndex == -1){
			return -1;
		}
		int closed = -1;

		// Write the buffer to disk
		byte[]  oft_buffer = new byte[64]; 
		oft_buffer = this.oft[index].buffer;

		// Find the data block from the descriptor:
		int fd_index = this.oft[index].get_fdIndex();
		
		// Get the block number in the disk map. Block 0 or 1 or 2?
		int disk_map = this.oft[index].get_position() / 64;

		// If block 0
		// If block 1
		// if block 2

		int block_num = (fd_index/4) + 1;
		byte[] fd_block = this.ldisk.read_block(block_num);
		int fd_index_in_block = fd_index % 4;

		if (disk_map == 0){
			closed = 0;
			int block_numb = (int) fd_block[fd_index_in_block * 16 + 4];
			if (block_numb == -1){
				// do nothing for now as there is nothing in the buffer to write.
			}
			else{
				this.ldisk.write_block(block_numb, oft_buffer);
			}

		}
		else if(disk_map == 1){
			closed = 0;
			int block_numb = (int) fd_block[fd_index_in_block * 16 + 8];
			if (block_numb == -1){
				// do nothing for now as there is nothing in the buffer to write.
			}
			else{
				this.ldisk.write_block(block_numb, oft_buffer);
			}
		}
		else if(disk_map == 2){
			closed = 0;
			int block_numb = (int) fd_block[fd_index_in_block * 16 + 12];
			if (block_numb == -1){
				// do nothing for now as there is nothing in the buffer to write.
			}
			else{
				this.ldisk.write_block(block_numb, oft_buffer);
			}
		}	


		// Update file length in descriptor
		byte[] fd_block2 = this.ldisk.read_block(fd_index/4 + 1);
		fd_block2[fd_index_in_block * 16] = (byte) this.oft[index].get_length();
		this.ldisk.write_block(fd_index/4 + 1, fd_block2);


		// Free OFT entry
		this.oft[index].fdIndex = -1;
		byte[] z = new byte[64];
		this.oft[index].buffer = z;
		this.oft[index].current_position = -1;
		this.oft[index].length = 0; 

		// Return the status
		if (closed == -1){
			// System.out.println("Error on closing file.");
			return -1;
		}
		else{
			this.open_file_count = open_file_count - 1;
			return index;
		}


	}

	public int seek(int oft_index, int position){
		this.oft[oft_index].set_position(position);
		// System.out.println("Current position is: " + this.oft[oft_index].get_position());

		return this.oft[oft_index].get_position();
	}
	// Read needs seek to work properly.
	public String read(int oft_index, int count){
		//
		String output = "";

		byte[] oft_buff = new byte[64];
		oft_buff = this.oft[oft_index].get_buffer();
		int pos1 = this.oft[oft_index].current_position % 64;		


		for (int i = 0; i < count; i++){
			int pos = this.oft[oft_index].current_position % 64;	
			int diskmap = this.oft[oft_index].current_position/64;
			int temp = (64 * (diskmap + 1));
			int fd_index = this.oft[oft_index].get_fdIndex();
			int block_num = (fd_index/4) + 1;
			byte[] fd_block = this.ldisk.read_block(block_num);
			int fd_index_in_block = fd_index % 4;
			int length = (int) fd_block[fd_index_in_block * 16];
			int map0 = (int) fd_block[fd_index_in_block * 16 + 4];
			int map1 = (int) fd_block[fd_index_in_block * 16 + 8];
			int map2 = (int) fd_block[fd_index_in_block * 16 + 12];

			if (this.oft[oft_index].current_position < temp){
				output += (char) this.oft[oft_index].buffer[pos];
				this.oft[oft_index].current_position++;
			}

			else{
				if (diskmap == 0){
					this.ldisk.write_block(map0, this.oft[oft_index].get_buffer());

					if (map1 == -1){
						// System.out.println("End of file!");
						break;

					}
					else{
						this.oft[oft_index].buffer = this.ldisk.read_block(map1);
					}

				}
				if (diskmap == 1){
					this.ldisk.write_block(map1, this.oft[oft_index].get_buffer());

					if (map2 == -1){
						// System.out.println("End of file!");
						break;

					}
					else{
						this.oft[oft_index].buffer = this.ldisk.read_block(map2);
					}
				}
				if (diskmap == 2){
					this.ldisk.write_block(map2, this.oft[oft_index].get_buffer());

					if (map2 == -1){
						// System.out.println("End of file!");
						break;

					}
					else{
						this.oft[oft_index].buffer = this.ldisk.read_block(map2);
					}
				}
			}


		}
		/*
		for (int i = 0; i < count; i++){
			int pos = this.oft[oft_index].current_position % 64;	
			int diskmap = this.oft[oft_index].current_position/64;

			int fd_index = this.oft[oft_index].get_fdIndex();
			int block_num = (fd_index/4) + 1;
			byte[] fd_block = this.ldisk.read_block(block_num);
			int fd_index_in_block = fd_index % 4;
			// End of file
			if (this.oft[oft_index].current_position >= 192) {
				System.out.println("End of file!");
				return output;
			}	

			// IF we are in the current block
			if (this.oft[oft_index].get_position() <= 63){
				output += (char) this.oft[oft_index].buffer[this.oft[oft_index].current_position];
			}
			// If current position is now in the next block.
			if (this.oft[oft_index].current_position >= (64 * (diskmap + 1))) {
				byte[] new_buff = new byte[64];


				if (diskmap == 1){
					int block_numb = (int) fd_block[fd_index_in_block * 16 + 8];
					new_buff = this.ldisk.read_block(block_numb);
					this.oft[oft_index].set_buffer(new_buff);
				}

				else if (diskmap == 2){
					int block_numb = (int) fd_block[fd_index_in_block * 16 + 12];
					new_buff = this.ldisk.read_block(block_numb);
					this.oft[oft_index].set_buffer(new_buff);

				}
			}
			else{
				
			}
			this.oft[oft_index].current_position += 1;
			
		}


		*/

		return output;
		
	}

	public int write(int oft_index, char c, int count){
		int free;
		byte lol = (byte) c;
		byte[] oft_buff = new byte[64];
		oft_buff = this.oft[oft_index].get_buffer();


		// Compute position in the read/write buffer.
		

		// Copy from memory into buffer until:
		//		Desired count or end of file is reached:
		//			Update current position, return status.

		// 		End of buffer is reached
		//			If block does not exist yet (file is expanding):
		//				Allocate new block (search and update bitmap)
		//				Update file descriptor with new block number

		//			Write the buffer to disk block
		//			Continue Copying


		//	Update file length in descriptor
		int pos1 = this.oft[oft_index].current_position % 64;		

		
		
		for (int i = 0; i < count; i++){
			int pos = this.oft[oft_index].current_position % 64;	
			int diskmap = this.oft[oft_index].current_position/64;
			if (this.oft[oft_index].current_position >= 192){
				System.out.println("End of File!");
				return -1;
			}
			int fd_index = this.oft[oft_index].get_fdIndex();
			int block_num = (fd_index/4) + 1;
			byte[] fd_block = this.ldisk.read_block(block_num);
			int fd_index_in_block = fd_index % 4;

			// Write here
			if (pos1 + i >= 64){
				if (diskmap == 1){
					free = this.bitmap.search_zero();
        			this.set_bitmap_one(free);
        			fd_block[fd_index_in_block * 16 + 8] = (byte) free;
        			this.oft[oft_index].buffer = this.ldisk.read_block(free);
				}

				if (diskmap == 2){
					free = this.bitmap.search_zero();
        			this.set_bitmap_one(free);
        			fd_block[fd_index_in_block * 16 + 12] = (byte) free;
        			this.oft[oft_index].buffer = this.ldisk.read_block(free);
				}
			}
			else {

				this.oft[oft_index].buffer[pos1 + i] = lol;

				this.oft[oft_index].set_position(this.oft[oft_index].current_position + 1);
				this.oft[oft_index].set_length(this.oft[oft_index].length + 1);
				int block_numb = (int) fd_block[fd_index_in_block * 16 + 4];
				fd_block[fd_index_in_block * 16] = (byte) this.oft[oft_index].get_length();
				this.ldisk.write_block(block_numb, this.oft[oft_index].buffer);
			}



		}
		// System.out.println(this.oft[oft_index].get_position() + "line 762");
		

		return count;
		
	}

	public void initialize_ldisk(){
		// Initialize the bitmap so bm[0-10] are set to 1 (not free).
		initialize_bitmap();
		// Need to convert bitmap into a byte array, so it can be placed into ldisk[0]

		byte[] temp = new byte[64];
		String bitmap_block = Integer.toBinaryString(this.bm[0]) + Integer.toBinaryString(this.bm[1]);

		for (int i= 0; i < 31; i++){
			bitmap_block += "0";
		}
		for (int i = 0; i < 64; i++){
			char a = bitmap_block.charAt(i);
			int b = a - '0';
			byte c = (byte) b;
			temp[i] = c;
		}


		// Write the bitmap into the first block of ldisk.
		ldisk.write_block(0, temp);

		// Write the file descriptors into ldisk[1-6]
		// Write the directory descriptor into ldisk[1][0]

		



		// Write the directory blocks into ldisk[7-9]



	}

	// Initialize Bitmap so bits 0-9 are listed as not free.
	public void initialize_bitmap(){
		for (int i = 0; i < 10; i++){
			this.bitmap.set_one(i);			
		}
	}
	public void set_bitmap_one(int index){
		byte[] temp = new byte[64];
		if (index > 9){
			this.bitmap.set_one(index);
			temp = this.ldisk.read_block(0);
			temp[index] = 1;
			this.ldisk.write_block(0, temp);
		
		}

		//for (int i = 0; i < )
	
	}


	public void set_bitmap_zero (int index){
		byte[] temp = new byte[64];
		if (index > 9){
			this.bitmap.set_zero(index);
			temp = this.ldisk.read_block(0);
			temp[index] = 0;
			this.ldisk.write_block(0, temp);
		}
		
	}

	public ArrayList<String> list(){
		ArrayList<String> ret = new ArrayList<>();
		String output = "";
		byte[] dir = this.ldisk.read_block(7);
		int dir_done = -1;

		for (int i = 7; i <=9; i++){
			dir = this.ldisk.read_block(i);
			for (int j = 0; j < 64; j += 8){
				String temp = "";
				if (dir[j] != -1){
					for (int k = 0; k < 3; k++){
						temp += (char) dir[j + k];
					}
					output+= temp;
					output += " ";
				}
			
				else{

				}

			}
		}
		String[] a = output.split(" ");
		for (int i = 0; i < a.length;i++){
			ret.add(a[i]);
		}
		Set<String> ret_2 = new LinkedHashSet<String>(ret);
		ret.clear();
		ret.addAll(ret_2);

		return ret;
	}

	public void save(String name) throws FileNotFoundException, UnsupportedEncodingException{
		PrintWriter writer = new PrintWriter(name, "UTF-8");
		for (int i = 0; i < 4; i++){
			this.oft[i].fdIndex = -1;
				close(i);
			
		}
		byte[] output2 = new byte[4096];
		for (int i = 0; i < 64; i++){
			for (int j = 0; j < 64; j++){
				output2[i * 64 + j] = this.ldisk.ldisk[i][j];
				writer.print(this.ldisk.ldisk[i][j] + ",");
			}
			writer.print("\n");
		}
		writer.close();
	}

	public void restoreDisk(byte[] input){
		for (int i = 0; i < 64; i++){
			for (int j = 0; j < 64; j++){
				this.ldisk.ldisk[i][j] = input[i * 64 + j];
			}
		}
	}




	public static void main(String[] args){
		FileSystem fs = new FileSystem();

		// // 1-1
		// fs.create("aaaa");
		
		// // 1-2
		// fs.create("bbbb");

		// // 1-3
		// fs.create("cccc");	

		// // 2-1	
		// fs.create("dddd");

		// // 2-2	
		// fs.create("eeee");

		// // 2-3.

		// // 2-4
		// fs.create("gggg");

		// // 3-1	
		// fs.create("hhhh");

		// // 3-2

		// fs.create("iiii");
		// fs.destroy("iiii");
		// fs.create("oooo");

        
  //       fs.destroy("cccc");
		// fs.destroy("bbbb");
		// fs.destroy("dddd");

		// System.out.println(fs.destroy("eeee"));
		// System.out.println(fs.destroy("aabb"));
		// fs.destroy("abce");
		
		// // System.out.println(fs.open("aaaa"));
		// System.out.println(fs.open("bbbb"));
		// System.out.println(fs.open("cccc"));


		// System.out.println(fs.write(1, 'c', 50));
		// System.out.println(fs.write(1, 'g', 10));
		// System.out.println(fs.write(2, 'a', 192));
		// System.out.println(fs.write(3, '4', 78));
		// System.out.println(fs.write(2, 'a', 60));
		// System.out.println(fs.write(3, 'b', 48));
		// //System.out.println(fs.write(1, 'h', 20));
		// System.out.println(fs.seek(3, 60));
		// System.out.println("test read");
		// System.out.println(fs.read(3, 15));

		// System.out.println("test list");
		// System.out.println(fs.list());
		// fs.write(1, 'h', 20)
		// System.out.println(fs.open("dddd"));
		// System.out.println(fs.close(1));
		// System.out.println(fs.open("dddd"));	
		// System.out.println(fs.open("hhhh"));
		// System.out.println(fs.close(3));
		// System.out.println(fs.open("hhhh"));
		// System.out.println(fs.open("ffff"));				
		// System.out.println(fs.close(1));
		// System.out.println(fs.close(2));
		// System.out.println(fs.close(3));
		// System.out.println(fs.open("aaaa"));
		// System.out.println(fs.open("eeee"));
		// System.out.println(fs.open("cccc"));
		// System.out.println(fs.open("dddd"));
		// fs.set_bitmap_one(15);
		// fs.set_bitmap_one(16);
		// fs.set_bitmap_one(32);
		// fs.set_bitmap_one(63);

		// for(int i = 0; i<fs.bitmap.bm.length; i++){
		// 	System.out.println(fs.bitmap.bm[i]);
		// }


  //       }
  //       byte[] x = fs.save();
  //       fs.restoreDisk(x);
		// System.out.println(fs.seek(3, 60));
		// System.out.println("test read");
		// System.out.println(fs.read(3, 15));

	}

}