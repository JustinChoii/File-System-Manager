import java.util.*;
import java.io.*;

public class OFT{
	public byte[] buffer = new byte[64];
	public int current_position = -1;
	public int fdIndex = -1;
	public int length = 0;

	public OFT(byte[] buff, int currentPos, int descriptor_index, int len){
		this.buffer = buff;
		this.current_position = currentPos;
		this.fdIndex = descriptor_index;
		this.length = len;
	}
	public void set_buffer(byte[] buff){
		this.buffer = buff;
	}

	public byte[] get_buffer(){
		return this.buffer;
	}

	public void set_position(int pos){
		this.current_position = pos;
	}

	public int get_position(){
		return this.current_position;
	}

	public void set_fdIndex(int index){
		this.fdIndex = index;
	}

	public int get_fdIndex(){
		return this.fdIndex;
	}

	public void set_length(int len){
		this.length = len;
	}

	public int get_length(){
		return this.length;
	}



}