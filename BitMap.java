public class BitMap{
	public int[] bm;
	public int[] mask;


	// BitMap Constructor. Construct the mask
	public BitMap(){
		// BM Size: # of bits needed = 64 = # of ldisk blocks
		// Represent as an array of int (32 bits each)
		// bitmap = new int[2] because 2 * 32 = 64
		this.bm = new int[2];
		this.mask = new int[32];


		// MASK[15] = 1;
		// MASK[i] = MASK[i+1] <<
		this.mask[31] = 1;
		for (int i = 30; i >= 0; i--){
			this.mask[i] = this.mask[i+1] << 1;
		}


	}



	public void set_one(int index){
		// BM[j] = BM[j] | mask[i]
		// int rounds down
		// index 54 % 32 = index 22
		this.bm[index/32] = this.bm[index/32] | this.mask[index%32];
	}

	// Set a bit at index to zero
	public void set_zero(int index){
		// MASK2[i] = ~MASK[i]
		int[] mask2 = new int[32];
		for (int i = 0; i < this.mask.length; i++){
			mask2[i] = ~mask[i];
		}
		// int rounds down
		this.bm[index/32] = this.bm[index/32] & mask2[index%32];
	}



	public int search_zero(){
		// for (i=0; …           search BM from the beginning
		// 	for (j=0; …     check each bit in BM[i] for “0”
		// 		test = BM[i] & MASK[j])
		// 		if (test == 0) then 
		// 			bit j of BM[i] is “0”; 
		// 			stop search
		

		for (int i = 0; i < 2; i++){
			for (int j = 0; j < 32; j++){
				int test = this.bm[i] & this.mask[j];
				if (test == 0){
					return ((i * 32) + j);
				}
			}
		}
		// Did not find an open bit, so return -1.
		return -1;
	}

	public static void main(String[] args){
		
		BitMap b = new BitMap();
		System.out.println("first zero is at" + b.search_zero() + "\n");
		b.set_one(0);
		b.set_one(1);
		b.set_one(2);
		b.set_one(3);
		b.set_one(4);
		b.set_one(5);
		b.set_one(5);
		b.set_one(5);
		b.set_one(6);
		b.set_one(7);
		b.set_one(8);
		b.set_one(9);
		/*
		b.set_one(10);
		b.set_one(11);
		b.set_one(12);
		b.set_one(13);
		b.set_one(14);
		b.set_one(15);
		b.set_one(16);
		b.set_one(17);
		b.set_one(18);
		b.set_one(19);
		b.set_one(20);
		b.set_one(21);
		b.set_one(22);
		b.set_one(23);
		b.set_one(24);
		b.set_one(25);
		b.set_one(26);
		b.set_one(27);
		b.set_one(28);
		b.set_one(29);
		b.set_one(30);
		b.set_one(31);
		b.set_one(32);
		b.set_one(33);
		*/
		System.out.println("first zero is at" + b.search_zero() + "\n");

		
		for(int i = 0; i<b.bm.length; i++){
			System.out.println(b.bm[i]);
		}
		
	
	}


}