package slather.g7;

public class DummyMemory implements Memory {

	@Override
	public byte getByte() {
		return 0;
	}

	@Override
	public char getMemoryAt(int index) {
		return '0';
	}

	@Override
	public int getMemoryBlock(int start, int end) {
		return 0;
	}

	@Override
	public String getMemoryString() {
		return "00000000";
	}

	@Override
	public Memory generateNextMoveMemory() {
		return new DummyMemory();
	}

	@Override
	public Memory generateFirstChildMemory() {
		// TODO Auto-generated method stub
		return new DummyMemory();
	}

	@Override
	public Memory generateSecondChildMemory(Memory firstChildMemory) {
		return new DummyMemory();
	}

}
