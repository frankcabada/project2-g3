package slather.g7;

public interface Memory {
	
	/*
	 * Get the byte from the memory fields
	 */
	byte getByte();
	
	/*
	 * Get the bit at a particular index
	 */
	char getMemoryAt(int index);
	
	/*
	 * Get a block of this memory object
	 */
	int getMemoryBlock(int start, int end);
	
	
	
	/*
	 * Get string version of the memory byte
	 */
	String getMemoryString();
	
	/*
	 * Generating new memory objects for the future based on current memory
	 */
	public Memory generateNextMoveMemory();
	public Memory generateFirstChildMemory();
	public Memory generateSecondChildMemory(Memory firstChildMemory);
}
