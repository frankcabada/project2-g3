package slather.g7;

import java.util.Random;

// TODO This is very inefficient. Need to use bit operations to improve performance
public class ExplorerMemory implements Memory {
	public String memStr;
	public int opposite;
	public int moveDirectionCountdown;
	public int offsetCountDown;
	public int defOrExp;
	
	private ExplorerMemory() {}

	/*
	 * Call this function at the beginning of every other function to
	 * account for all changes to object fields.
	 */
	private void updateMemStr() {
		this.memStr = blockToString(defOrExp, 1) + 
				blockToString(offsetCountDown, 2) + 
				blockToString(moveDirectionCountdown, 4) +
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
		return memStr.charAt(index);
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

	
	public static  ExplorerMemory getNewObject() {
		return new ExplorerMemory();
	}
	
	public void initialize(byte memory) {
		this.memStr = byteToString(memory);
		this.defOrExp = getMemoryBlock(0, 1);
		this.opposite = getMemoryBlock(7, 8);
		this.moveDirectionCountdown = getMemoryBlock(3, 7);
		this.offsetCountDown = getMemoryBlock(1, 3);	
	}
	
	public void initialize(int offset, int dirCnt, int opposite) {
		this.defOrExp = 0; // 0 implies Exp
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
		
		ExplorerMemory memoryObject = getNewObject();
		memoryObject.initialize(this.getByte());
		if (memoryObject.opposite == 1){
			if (memoryObject.moveDirectionCountdown == 0){
				memoryObject.opposite = 0;
				memoryObject.offsetCountDown = 3;
				memoryObject.moveDirectionCountdown = 15;
			}
			else{
				memoryObject.moveDirectionCountdown--;
			}
		}
		else{
			if (memoryObject.moveDirectionCountdown == 0){
				memoryObject.opposite = 1;
				memoryObject.offsetCountDown = 0;
				memoryObject.moveDirectionCountdown = 15;
			}
			else{
				if (memoryObject.offsetCountDown == 0){
					memoryObject.offsetCountDown = 3;
					memoryObject.moveDirectionCountdown--;
				}
				else{
					memoryObject.offsetCountDown--;
				}

			}
		}
		memoryObject.defOrExp = 0;
		return memoryObject;
		
	}

	@Override
	public Memory generateFirstChildMemory() {
		Random rand = new Random();
		Memory childMem;
		double num = rand.nextDouble();
		
		if (num > 0.7) {
			childMem = DefenderMemory.getNewObject();
			((DefenderMemory)childMem).initialize(127);
		} else {
			childMem = ExplorerMemory.getNewObject();
			((ExplorerMemory)childMem).initialize(1, 0, 15);
		}
		
		return childMem;
	}

	@Override
	public Memory generateSecondChildMemory(Memory currentMemory) {

		Random rand = new Random();
		Memory childMem;
		double num = rand.nextDouble();
		
		if (num > 0.7) {
			childMem = DefenderMemory.getNewObject();
			((DefenderMemory)childMem).initialize(127);
		} else {
			childMem = ExplorerMemory.getNewObject();
			((ExplorerMemory)childMem).initialize(1, 0, 15);
		}
		
		return childMem;
	}
}