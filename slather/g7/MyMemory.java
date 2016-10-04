package slather.g7;

// TODO This is very inefficient. Need to use bit operations to improve performance
public class MyMemory implements Memory {
	public String memStr;
	public int opposite;
	public int moveDirectionCountdown;
	public int offsetCountDown;
	public int moveDirectionCountSetter;
	
	private MyMemory() {}

	public static MyMemory getNewObject() {
		return new MyMemory();
	}

	/*
	 * Call this function at the beginning of every other function to
	 * account for all changes to object fields.
	 */
	private void updateMemStr() {
		this.memStr = blockToString(moveDirectionCountSetter, 2) + 
				blockToString(offsetCountDown, 2) + 
				blockToString(moveDirectionCountdown, 3) +
				blockToString(opposite, 1);
	}

	public byte getByte() {
		updateMemStr();
		
		if (memStr.length() != 8) {
			System.out.println("The memory is converted to " + memStr.length() + " bits: " + memStr);
			return Byte.parseByte("0", 2);
		}
		
		int integer = Integer.parseInt(memStr, 2);
		if (integer > 127) {
			integer = 256 - integer;
		}
		byte b = (byte) integer;
		return b;
	}

	public char getMemoryAt(int index) {
		updateMemStr();
		if (index > memStr.length())
			return ' ';
		return memStr.charAt(index - 1);
	}

	/*
	 * Retrieving a block of memory from index start inclusively to end
	 * exclusively
	 */
	public int getMemoryBlock(int start, int end) {
		updateMemStr();
		String sub = memStr.substring(start, end);
		int val = Integer.parseInt(sub, 2);
		return val;
	}
	
	public void initialize(byte memory) {
		this.memStr = byteToString(memory);
		this.moveDirectionCountSetter = getMemoryBlock(0, 2);
		this.opposite = getMemoryBlock(7, 8);
		this.moveDirectionCountdown = getMemoryBlock(4, 7);
		this.offsetCountDown = getMemoryBlock(2, 4);	
	}
	
	public void initialize(int offset, int dirCnt, int opposite) {
		this.moveDirectionCountSetter = 0; // Initializes to 0 to start off
		this.offsetCountDown = offset;
		this.moveDirectionCountdown = dirCnt;
		this.opposite = opposite;
	}

	@Override
	public String getMemoryString() {
		updateMemStr();
		return this.memStr;
	}

	public static String byteToString(byte memory) {
		String s = String.format("%8s", Integer.toBinaryString(memory & 0xFF)).replace(' ', '0');
		return s;
	}

	public static String blockToString(int block, int length) {
		String target = Integer.toBinaryString(block);
		if (target.length() > length) {
			System.out.println("The information " + block + " is too big to fit in memory of length " + length);
			return target.substring(0, length);
		} else if (target.length() < length) {
			StringBuilder sb = new StringBuilder(target);
			for (int i = target.length(); i < length; i++) {
				sb.insert(0, '0');
			}
			return sb.toString();
		} else {
			return target;
		}
	}

	@Override
	public Memory generateNextMoveMemory() {
		MyMemory memoryObject = this.createCopy();
		
		if (memoryObject.opposite == 1) {

			if (memoryObject.moveDirectionCountdown == 0) {
				memoryObject.opposite = 0;
				memoryObject.offsetCountDown = 3;

				// Setting count-down based on cell life
				if (memoryObject.moveDirectionCountSetter >= 2) {
					memoryObject.moveDirectionCountSetter = 3;
					memoryObject.moveDirectionCountdown = 7;
				} else {
					(memoryObject.moveDirectionCountSetter)++;
					memoryObject.moveDirectionCountdown = 2 * memoryObject.moveDirectionCountSetter;
				}

			} else {
				memoryObject.moveDirectionCountdown--;
			}

		} else {
			if (memoryObject.moveDirectionCountdown == 0) {
				memoryObject.opposite = 1;
				memoryObject.offsetCountDown = 0;

				// Setting count-down based on cell life
				if (memoryObject.moveDirectionCountSetter >= 2) {
					memoryObject.moveDirectionCountSetter = 3;
					memoryObject.moveDirectionCountdown = 7;
				} else {
					(memoryObject.moveDirectionCountSetter)++;
					memoryObject.moveDirectionCountdown = 2 * memoryObject.moveDirectionCountSetter;
				}

			} else {
				if (memoryObject.offsetCountDown == 0) {
					memoryObject.moveDirectionCountdown--;
				} else {
					memoryObject.offsetCountDown--;
				}
			}
		}

		return memoryObject;
	}

	@Override
	public Memory generateFirstChildMemory() {
		MyMemory child = MyMemory.getNewObject();
		child.initialize(3, 2, 0);
		return child;
	}

	@Override
	public Memory generateSecondChildMemory(Memory firstChildMemory) {
		MyMemory firstChild = (MyMemory) firstChildMemory;
		MyMemory child = MyMemory.getNewObject();
		child.initialize(firstChild.offsetCountDown, 
							firstChild.moveDirectionCountdown, 
							1 - firstChild.opposite);
		return child;
	}

	private MyMemory createCopy() {
		MyMemory copy = MyMemory.getNewObject();
		copy.memStr = this.memStr;
		copy.opposite = this.opposite;
		copy.moveDirectionCountdown = this.moveDirectionCountdown;
		copy.offsetCountDown = this.offsetCountDown;
		copy.moveDirectionCountSetter = this.moveDirectionCountSetter;
		
		return copy;
	}
	
	public String toString() {
		return this.getMemoryString();
	}
}