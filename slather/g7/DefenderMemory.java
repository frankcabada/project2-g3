package slather.g7;

import java.util.Random;

// TODO This is very inefficient. Need to use bit operations to improve performance
public class DefenderMemory implements Memory {
	public String memStr;
	private int circleBits;
	private int defOrExp;

	private DefenderMemory() {
	}

	public byte getByte() {

		if (memStr.length() != 8) {
			System.out.println("The memory is converted to " + memStr.length() + " bits: " + memStr);
			return Byte.parseByte("0", 2);
		}

		int integer = Integer.parseInt(memStr, 2);
		if (integer > 127) {
			integer = integer - 256;
		}
		byte b = (byte) integer;
		return b;
	}

	public char getMemoryAt(int index) {
		if (index > memStr.length())
			return ' ';
		return memStr.charAt(index);
	}

	/*
	 * Retrieving a block of memory from index start inclusively to end
	 * exclusively
	 */
	public int getMemoryBlock(int start, int end) {
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
		this.memStr = blockToString(defOrExp, 1) + blockToString(circleBits, 7);
	}

	@Override
	public String getMemoryString() {
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
	public DefenderMemory generateNextMoveMemory() {
		DefenderMemory memoryObject = getNewObject();
		int currentCircle = this.circleBits;
		if (currentCircle == Player.num_def_sides) {
			memoryObject.initialize(0);
		} else {
			memoryObject.initialize(this.circleBits + 1);
		}
		
		return memoryObject;

	}

	@Override
	public Memory generateFirstChildMemory() {
		Random rand = new Random();
		Memory childMem;
		double num = rand.nextDouble();
		
		if (num > ToolBox.EXPLORER_PROBABILITY) {
			childMem = DefenderMemory.getNewObject();
			((DefenderMemory)childMem).initialize(0);
		} else {
			childMem = ExplorerMemory.getNewObject();
			((ExplorerMemory)childMem).initialize(0, 15, 1);
		}
		return childMem;
	}

	@Override
	public Memory generateSecondChildMemory(Memory firstChildMemory) {

		Random rand = new Random();
		Memory childMem;
		double num = rand.nextDouble();
		
		if (num > ToolBox.EXPLORER_PROBABILITY) {
			childMem = DefenderMemory.getNewObject();
			((DefenderMemory)childMem).initialize(0);
		} else {
			childMem = ExplorerMemory.getNewObject();
			((ExplorerMemory)childMem).initialize(0,15,1);
		}
		
		return childMem;
	}

	public int getCircleBits() {
		return this.circleBits;
	}
}