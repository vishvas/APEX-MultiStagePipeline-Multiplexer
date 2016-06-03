public class Instruction {
	private String opcode;
	private String destination;
	private String source1;
	private String source2;
	private int literal;
	private int instrNumber;
	private int result;
	private String stage;
	private String thisInstruction;
	private boolean writeResult;
	private int src1Val;
	private int src2Val;
	private int src1ROBRef = -1;
	private int src2ROBRef = -1;
	private String typeOfQueue;// Modified by
	private String typeOfFU;// Modified by
	private boolean detsinationValidBit = false;// Modified by
	private String stageBit;// Modified by
	private String source1Result;// Modified by
	private String source2Result;// Modified by

	public Instruction() {

		this.thisInstruction = "";
	}

	public int getInstrNumber() {
		return instrNumber;
	}

	public void setInstrNumber(int instrNumber) {
		this.instrNumber = instrNumber;
		this.thisInstruction += "" + this.instrNumber + "" + ":";
	}

	public String getOpcode() {
		return opcode;
	}

	public void setOpcode(String opcode) {
		this.opcode = opcode;
		this.thisInstruction += " " + this.opcode;
	}

	public String getDestination() {
		return destination;
	}

	public void setDestination(String destinationReg) {
		this.destination = destinationReg;
		this.thisInstruction += " " + this.destination;

	}

	public String getSource1() {
		return source1;
	}

	public void setSource1(String sourceReg1) {
		this.source1 = sourceReg1;
		if (!opcode.equals("BNZ") && !opcode.equals("BZ") && !opcode.equals("BAL") && !opcode.equals("JUMP")) {
			this.thisInstruction += " " + this.source1;
		}

	}

	public String getSource2() {
		return source2;
	}

	public void setSource2(String sourceReg2) {
		this.source2 = sourceReg2;
		this.thisInstruction += " " + this.source2;
	}

	public int getLiteral() {
		return literal;
	}

	public void setLiteral(int literal) {
		this.literal = literal;
		this.thisInstruction += " " + this.literal;
	}

	public int getResult() {
		return result;
	}

	public void setResult(int tempResult) {
		this.result = tempResult;
	}

	public String getStage() {
		return stage;
	}

	public void setStage(String stage) {
		this.stage = stage;
	}

	@Override
	public String toString() {

		// System.out.println(this.thisInstruction);
		return this.thisInstruction;
	}

	public boolean isWriteResult() {
		return writeResult;
	}

	public void setWriteResult(boolean writeResult) {
		this.writeResult = writeResult;
	}

	public String getTypeOfQueue() {
		return typeOfQueue;
	}

	public void setTypeOfQueue(String typeOfQueue) {
		this.typeOfQueue = typeOfQueue;
	}

	public String getTypeOfFU() {
		return typeOfFU;
	}

	public void setTypeOfFU(String typeOfFU) {
		this.typeOfFU = typeOfFU;
	}

	public boolean isDetsinationValidBit() {
		return detsinationValidBit;
	}

	public void setDetsinationValidBit(boolean detsinationValidBit) {
		this.detsinationValidBit = detsinationValidBit;
	}

	public String getStageBit() {
		return stageBit;
	}

	public void setStageBit(String stageBit) {
		this.stageBit = stageBit;
	}

	public String getSource1Result() {
		return source1Result;
	}

	public void setSource1Result(String source1Result) {
		this.source1Result = source1Result;
	}

	public String getSource2Result() {
		return source2Result;
	}

	public void setSource2Result(String source2Result) {
		this.source2Result = source2Result;
	}

	public int getSrc1Val() {
		return src1Val;
	}

	public void setSrc1Val(int src1Val) {
		this.src1Val = src1Val;
	}

	public int getSrc2Val() {
		return src2Val;
	}

	public void setSrc2Val(int src2Val) {
		this.src2Val = src2Val;
	}

	public int getSrc1ROBRef() {
		return src1ROBRef;
	}

	public void setSrc1ROBRef(int src1robRef) {
		src1ROBRef = src1robRef;
	}

	public int getSrc2ROBRef() {
		return src2ROBRef;
	}

	public void setSrc2ROBRef(int src2robRef) {
		src2ROBRef = src2robRef;
	}
	
	

}
