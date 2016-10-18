package slather.g5;

public class MemByte {

	private byte _memory;

	public MemByte(byte memory) {
		_memory = memory;
	}

	public byte getRawByte() {
		return _memory;
	}

	public int getGeneration() {
		int gen = (_memory >> (byte)4) & 0xF;
		return gen;
	}

	public byte getDirection() {
		return (byte)(_memory & 0x0F);
	}

	public byte nextGeneration() {
		int nextGen = getGeneration() + 1;
		if(nextGen > 15) {
			nextGen = 15;
		}

		return (byte)((byte)(nextGen << 4) | getDirection());
		//return (byte)((byte)(_memory & 0x0F) | (byte)((getGeneration() + 1) << 4));
	}

	public MemByte packerByte() {
		return new MemByte((byte)((byte)(15 << 4) | getDirection()));
	}

	public byte withNewDirection(byte newDir) {
		if(newDir > 15) {
			newDir = 15;
		}
		return (byte)((newDir & 0xF) | (byte)(getGeneration() << 4));
		//return (byte)((_memory & ((byte) 0xF0)) | newDir);
	}

}