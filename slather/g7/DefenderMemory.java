package slather.g7;

import java.util.Random;

// TODO This is very inefficient. Need to use bit operations to improve performance
public class DefenderMemory implements Memory {
	public String memStr;
	public int circleBits;
	public int defOrExp;

	private DefenderMemory() {
	}

	/*
	 * Call this function at the beginning of every other function to account
	 * for all changes to object fields.
	 */
	private void updateMemStr() {
		this.memStr = blockToString(defOrExp, 1) + blockToString(circleBits, 7);
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

	public static DefenderMemory getNewObject() {
		return new DefenderMemory();
	}

	public void initialize(byte memory) {
		this.memStr = byteToString(memory);
		this.defOrExp = getMemoryBlock(0, 1);
		this.circleBits = getMemoryBlock(1, 8);
	}

	public void initialize(int circleDefinition) {
		this.defOrExp = 1; // 1 impies def
		this.circleBits = circleDefinition;
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

		DefenderMemory memoryObject = getNewObject();
		memoryObject.initialize(this.getByte());
		int currentCircle = this.circleBits;
		if (currentCircle == 0) {
			memoryObject.circleBits = 127;
		} else {
			memoryObject.circleBits = currentCircle--;
		}
		memoryObject.defOrExp = 1;
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