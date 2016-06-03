import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class ApexSimulator {

	//public static String FILENAME = "Input 2.txt";
	public static String FILENAME;
	private ArrayList<Instruction> instructions;
	private Map<String, Instruction> apexPipeline = new HashMap<String, Instruction>();
	private Map<String, RegisterState> memory = new HashMap<String, RegisterState>();
	int progCounter;
	int cycles;
	int addr;
//	private int haltFlag;
	
	BufferedReader aBufferedReader;
	// private Object instrInIntFU;
	private int counterMul = 0;
	private Queue robQueue;

	// Method to get instructions from the file
	public void initialize() {
		// System.out.println("No is "+(16+1)%16);
		apexPipeline.put("fetchOne", null);
		apexPipeline.put("fetchTwo", null);
		apexPipeline.put("decodeOne", null);
		apexPipeline.put("decodeTwo", null);
		apexPipeline.put("lsqOne", null);// Lsq 4 stages
		apexPipeline.put("lsqTwo", null);
		apexPipeline.put("lsqThree", null);
		apexPipeline.put("lsqFour", null);
		apexPipeline.put("iqOne", null);// Iq 8 satges
		apexPipeline.put("iqTwo", null);
		apexPipeline.put("iqThree", null);
		apexPipeline.put("iqFour", null);
		apexPipeline.put("iqFive", null);
		apexPipeline.put("iqSix", null);
		apexPipeline.put("iqSeven", null);
		apexPipeline.put("iqEight", null);
		apexPipeline.put("lsExecuteStageOne", null); // LS execute 3 stages
		apexPipeline.put("lsExecuteStageTwo", null);
		apexPipeline.put("lsExecuteStageThree", null);
		apexPipeline.put("multipleFU", null); // Multiply FU
		apexPipeline.put("intFU", null); // Int FU
		// apexPipeline.put("memory", null); // Memory
		// apexPipeline.put("writeback", null);// Writeback

		memory.put("R0", new RegisterState(0, null));
		memory.put("R1", new RegisterState(0, null));
		memory.put("R2", new RegisterState(0, null));
		memory.put("R3", new RegisterState(0, null));
		memory.put("R4", new RegisterState(0, null));
		memory.put("R5", new RegisterState(0, null));
		memory.put("R6", new RegisterState(0, null));
		memory.put("R7", new RegisterState(0, null));
		memory.put("X", new RegisterState(0, null));

		for (int i = 8; i < 10000; i++) {

			memory.put("R" + i, new RegisterState(0, null));
		}

		// lsqList=new ArrayList<>();
		// iqList=new ArrayList<>();

		// Initialize rob queue
		robQueue = new CircularArrayQueue();

		progCounter = 0;
		cycles = 0;

	}

	private boolean allAreExecuted() {

		for (Map.Entry<String, Instruction> entry : apexPipeline.entrySet()) {
			if (entry.getValue() != null) {
				return false;
			}
		}
		return true;
	}

	private void executeInstructions(int executionCycles) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(FILENAME));
			String newLine;
			int instrNum = 0;
			instructions = new ArrayList<>();

			// Only contains the destination register for each instruction
			while ((newLine = reader.readLine()) != null) {
				String[] instParts = newLine.split(" ");
				Instruction anInst = new Instruction();

				anInst.setInstrNumber(instrNum);
				instructions.add(anInst);
				instrNum++;
				anInst.setOpcode(instParts[0]); // HALT
				anInst.setStage("Read");
				if (instParts.length < 2)
					continue;
				if (isInteger(instParts[1]))
					anInst.setLiteral(Integer.parseInt(instParts[1])); // JUMP
																		// 65
				else
					anInst.setDestination(instParts[1]);

				if (instParts.length < 3) // MOV R1 65
					continue;
				if (isInteger(instParts[2]))
					anInst.setLiteral(Integer.parseInt(instParts[2]));
				else
					anInst.setSource1(instParts[2]);

				if (instParts.length < 4) // ADD R1 R2 R3
					continue;
				if (isInteger(instParts[3]))
					anInst.setLiteral(Integer.parseInt(instParts[3]));
				else
					anInst.setSource2(instParts[3]);
			}
			reader.close();

			do {
				this.cycles++;
//				if(haltFlag==1){
//					this.cycles--;
//				}
				pushInstructionForward();
				// // Writeback
				// if (apexPipeline.get("writeback") != null) {
				// writeInstruction(apexPipeline.get("writeback"));
				// }
				// Memory
				// if (apexPipeline.get("memory") != null) {
				// memoryInstruction(apexPipeline.get("memory"));
				// }
				// Execute
				/*
				 * if (apexPipeline.get("execute") != null) {
				 * executeInstruction(apexPipeline.get("execute")); }
				 */

				commitROB();

				selectPriorityFU();

				if (apexPipeline.get("lsExecuteStageOne") != null) {
					executeLSInstructionONE(apexPipeline.get("lsExecuteStageOne"));
					forwardResults();
				}
				if (apexPipeline.get("lsExecuteStageTwo") != null) {
					executeLSInstructionTWO(apexPipeline.get("lsExecuteStageTwo"));
					forwardResults();
				}

				if (apexPipeline.get("lsExecuteStageThree") != null) {
					// executeInstruction(apexPipeline.get("lsExecuteStageThree"));
					executeLSInstructionTHREE(apexPipeline.get("lsExecuteStageThree"));
					forwardResults();
				}
				if (apexPipeline.get("multiplyFU") != null) {
					if (counterMul == 0) {
						executeInstruction(apexPipeline.get("multiplyFU"));
						forwardResults();
						counterMul++;
					} else {
						if (counterMul < 3) {
							counterMul++;
						} else {
							counterMul = 0;
							apexPipeline.get("multiplyFU").setDetsinationValidBit(true);
						}

						// counterMul++;
					}
				}
				if (apexPipeline.get("intFU") != null) {
					executeInstruction(apexPipeline.get("intFU"));
					forwardResults();
				}

				// Decode
				if (apexPipeline.get("lsqOne") != null) {
					decodeInstruction(apexPipeline.get("lsqOne"));
				}
				if (apexPipeline.get("iqOne") != null) {
					decodeInstruction(apexPipeline.get("iqOne"));
				}
				
				//Used for forwarding
				forwardResults();
				
				//Print all the values
				printApexState();
			}while (executionCycles != this.cycles);//(!allAreExecuted());//(executionCycles != this.cycles);
			printMemoryState();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void printMemoryState() {
		System.out.println("MemoryStatus:");
		for (int i = 0; i < 100; i++) {
			String register = "R" + i;
			if (i == 50) {
				System.out.println();
			}
			System.out.print(register + ": " + memory.get(register).getValue() + "   ");
		}
	}

	public void printApexState() {

		Instruction instrInFetchOne = this.apexPipeline.get("fetchOne");
		Instruction instrInFetchTwo = this.apexPipeline.get("fetchTwo");
		Instruction instrInDecodeOne = this.apexPipeline.get("decodeOne");
		Instruction instrInDecodeTwo = this.apexPipeline.get("decodeTwo");
		// Instruction instrInExecute = this.apexPipeline.get("execute");

		Instruction instrInLsqOne = this.apexPipeline.get("lsqOne");
		Instruction instrInLsqTwo = this.apexPipeline.get("lsqTwo");

		Instruction instrInLsqThree = this.apexPipeline.get("lsqThree");
		Instruction instrInLsqFour = this.apexPipeline.get("lsqFour");

		Instruction instrInIqOne = this.apexPipeline.get("iqOne");

		Instruction instrInIqTwo = this.apexPipeline.get("iqTwo");
		Instruction instrInIqThree = this.apexPipeline.get("iqThree");

		Instruction instrInIqFour = this.apexPipeline.get("iqFour");
		Instruction instrInIqFive = this.apexPipeline.get("iqFive");
		Instruction instrInIqSix = this.apexPipeline.get("iqSix");
		Instruction instrInIqSeven = this.apexPipeline.get("iqSeven");
		Instruction instrInIqEight = this.apexPipeline.get("iqEight");

		Instruction lsExecuteOne = this.apexPipeline.get("lsExecuteStageOne");
		Instruction lsExecuteTwo = this.apexPipeline.get("lsExecuteStageTwo");
		Instruction lsExecuteThree = this.apexPipeline.get("lsExecuteStageThree");

		Instruction instrInMul = this.apexPipeline.get("multiplyFU");
		Instruction instrIntFU = this.apexPipeline.get("intFU");

		/*
		 * Instruction instrInMemory = this.apexPipeline.get("memory");
		 * Instruction instrInWriteback = this.apexPipeline.get("writeback");
		 */

		System.out.println("Status:: Cycle - " + this.cycles);
		System.out.format(
				"In Fetch One    -> %-17s    State: " + (instrInFetchOne == null ? "NA" : instrInFetchOne.getStage()),
				(instrInFetchOne == null ? "Empty" : instrInFetchOne));
		System.out.println();
		System.out.format(
				"In Fetch Two   -> %-17s    State: " + (instrInFetchTwo == null ? "NA" : instrInFetchTwo.getStage()),
				(instrInFetchTwo == null ? "Empty" : instrInFetchTwo));
		System.out.println();
		System.out.format(
				"In Decode One   -> %-17s    State: " + (instrInDecodeOne == null ? "NA" : instrInDecodeOne.getStage()),
				(instrInDecodeOne == null ? "Empty" : instrInDecodeOne));
		System.out.println();
		System.out.format(
				"In Decode Two   -> %-17s    State: " + (instrInDecodeTwo == null ? "NA" : instrInDecodeTwo.getStage()),
				(instrInDecodeTwo == null ? "Empty" : instrInDecodeTwo));
		System.out.println();
		// System.out.format("In -> %-17s State: " + (instrIn == null ? "NA" :
		// instrIn.getStage()), (instrIn == null ? "Empty": instrIn));

		System.out.format(
				"In LSQONE    -> %-17s    State: " + (instrInLsqOne == null ? "NA" : instrInLsqOne.getStage()),
				(instrInLsqOne == null ? "Empty" : instrInLsqOne));
		System.out.println();
		System.out.format("In LSQTWO   -> %-17s    State: " + (instrInLsqTwo == null ? "NA" : instrInLsqTwo.getStage()),
				(instrInLsqTwo == null ? "Empty" : instrInLsqTwo));
		System.out.println();
		System.out.format(
				"In LSQTHREE   -> %-17s    State: " + (instrInLsqThree == null ? "NA" : instrInLsqThree.getStage()),
				(instrInLsqThree == null ? "Empty" : instrInLsqThree));
		System.out.println();
		System.out.format(
				"In LSQFOUR   -> %-17s    State: " + (instrInLsqFour == null ? "NA" : instrInLsqFour.getStage()),
				(instrInLsqFour == null ? "Empty" : instrInLsqFour));
		System.out.println();

		System.out.format("In IQONE   -> %-17s    State: " + (instrInIqOne == null ? "NA" : instrInIqOne.getStage()),
				(instrInIqOne == null ? "Empty" : instrInIqOne));
		System.out.println();
		System.out.format("In IQTWO  -> %-17s    State: " + (instrInIqTwo == null ? "NA" : instrInIqTwo.getStage()),
				(instrInIqTwo == null ? "Empty" : instrInIqTwo));
		System.out.println();
		System.out.format(
				"In IQthree   -> %-17s    State: " + (instrInIqThree == null ? "NA" : instrInIqThree.getStage()),
				(instrInIqThree == null ? "Empty" : instrInIqThree));
		System.out.println();
		System.out.format("In  IQFOUR  -> %-17s    State: " + (instrInIqFour == null ? "NA" : instrInIqFour.getStage()),
				(instrInIqFour == null ? "Empty" : instrInIqFour));
		System.out.println();
		System.out.format("In IQFiVE   -> %-17s    State: " + (instrInIqFive == null ? "NA" : instrInIqFive.getStage()),
				(instrInIqFive == null ? "Empty" : instrInIqFive));
		System.out.println();
		System.out.format("In IQSIX  -> %-17s    State: " + (instrInIqSix == null ? "NA" : instrInIqSix.getStage()),
				(instrInIqSix == null ? "Empty" : instrInIqSix));
		System.out.println();
		System.out.format(
				"In IQSEVEN   -> %-17s    State: " + (instrInIqSeven == null ? "NA" : instrInIqSeven.getStage()),
				(instrInIqSeven == null ? "Empty" : instrInIqSeven));
		System.out.println();
		System.out.format(
				"In  IQEIGHT  -> %-17s    State: " + (instrInIqEight == null ? "NA" : instrInIqEight.getStage()),
				(instrInIqEight == null ? "Empty" : instrInIqEight));
		System.out.println();

		System.out.format("In  LSEOne  -> %-17s    State: " + (lsExecuteOne == null ? "NA" : lsExecuteOne.getStage()),
				(lsExecuteOne == null ? "Empty" : lsExecuteOne));
		System.out.println();
		System.out.format("In LSETWO   -> %-17s    State: " + (lsExecuteTwo == null ? "NA" : lsExecuteTwo.getStage()),
				(lsExecuteTwo == null ? "Empty" : lsExecuteTwo));
		System.out.println();
		System.out.format(
				"In LSETHREE  -> %-17s    State: " + (lsExecuteThree == null ? "NA" : lsExecuteThree.getStage()),
				(lsExecuteThree == null ? "Empty" : lsExecuteThree));
		System.out.println();
		System.out.format("In MULFU   -> %-17s    State: " + (instrInMul == null ? "NA" : instrInMul.getStage()),
				(instrInMul == null ? "Empty" : instrInMul));
		System.out.println();
		System.out.format("In  INTFU  -> %-17s    State: " + (instrIntFU == null ? "NA" : instrIntFU.getStage()),
				(instrIntFU == null ? "Empty" : instrIntFU));
		System.out.println();

		/*
		 * System.out.format("In Memory    -> %-17s    State: " + (instrInMemory
		 * == null ? "NA" : instrInMemory.getStage()), (instrInMemory == null ?
		 * "Empty" : instrInMemory)); System.out.println(); System.out.format(
		 * "In Writeback -> %-17s    State: " + (instrInWriteback == null ? "NA"
		 * : instrInWriteback.getStage()), (instrInWriteback == null ? "Empty" :
		 * instrInWriteback)); System.out.println();
		 */
		System.out.println();
	}

	// private void writeInstruction(Instruction inst) {
	//
	// String destRegister = inst.getDestination();
	// RegisterState registerState = memory.get(destRegister);
	// if (registerState != null) {
	//
	// registerState.setUnderOperation(false);
	// }
	// if (inst.isWriteResult()) {
	//
	// registerState.setValue(inst.getResult());
	// // registerState.setValue(value);
	// }
	//
	// }

	private void commitROB() {
		int index = robQueue.getHeadIndex();
		Instruction anInstruction = robQueue.getROBFromIndex(index);
		if (anInstruction != null && anInstruction.getStage().equals("completed")) {
			dequeue();
		}
	}

	/*private void memoryInstruction(Instruction inst) {
		// System.out.println("Destination "+inst.getDestination());
		if (inst.getDestination() != null) {
			RegisterState destination = memory.get(inst.getDestination());
			switch (inst.getOpcode()) {
			case "LOAD": {

				String regAddress = inst.getSource1().replaceAll("[^0-9]", "");
				int loadAddress = Integer.parseInt(regAddress) + inst.getLiteral();
				RegisterState loadFrom = memory.get("R" + loadAddress);
				RegisterState loadHere = memory.get(inst.getDestination());
				loadHere.setValue(loadFrom.getValue());
				break;
			}
			case "STORE": {

				String regAddress = inst.getSource1().replaceAll("[^0-9]", "");
				int storeAddress = Integer.parseInt(regAddress) + inst.getLiteral();
				RegisterState storeHere = memory.get("R" + storeAddress);
				RegisterState storeFrom = memory.get(inst.getDestination());
				storeHere.setValue(storeFrom.getValue());
				break;
			}
			}
		}
	}*/

	private void executeInstruction(Instruction inst) {

		if (inst.getOpcode().equals("STORE")) {
			return;
		}
		if (inst.getDestination() != null) {

			@SuppressWarnings("unused")
			RegisterState destination = memory.get(inst.getDestination());
			// destination.setUnderOperation(true);
		}
		switch (inst.getOpcode()) {
		case "MOVC": {
			inst.setResult(inst.getLiteral());
			inst.setWriteResult(true);
			inst.setDetsinationValidBit(true);
			break;
		}
		case "ADD": {
			//RegisterState source1 = memory.get(inst.getSrc1Val());
			//RegisterState source2 = memory.get(inst.getSource2());
			inst.setResult(inst.getSrc1Val() + inst.getSrc2Val());
			inst.setWriteResult(true);
			inst.setDetsinationValidBit(true);
			break;
		}
		case "SUB": {
			//RegisterState source1 = memory.get(inst.getSource1());
			//RegisterState source2 = memory.get(inst.getSource2());
			// inst.setResult(source1.getValue() - source2.getValue());
			inst.setResult(inst.getSrc1Val() - inst.getSrc2Val());
			inst.setWriteResult(true);
			inst.setDetsinationValidBit(true);
			break;
		}
		case "MUL": {

			//RegisterState source1 = memory.get(inst.getSource1());
			//RegisterState source2 = memory.get(inst.getSource2());
			// inst.setResult(source1.getValue() * source2.getValue());
			inst.setResult(inst.getSrc1Val() * inst.getSrc2Val());
			inst.setWriteResult(true);
			break;
		}
		case "AND": {
			//RegisterState source1 = memory.get(inst.getSource1());
			//RegisterState source2 = memory.get(inst.getSource2());
			// inst.setResult((source1.getValue()) & (source2.getValue()));
			inst.setResult(inst.getSrc1Val() & inst.getSrc2Val());
			inst.setWriteResult(true);
			inst.setDetsinationValidBit(true);
			break;
		}
		case "OR": {
			//RegisterState source1 = memory.get(inst.getSource1());
			//RegisterState source2 = memory.get(inst.getSource2());
			// inst.setResult((source1.getValue()) | (source2.getValue()));
			inst.setResult(inst.getSrc1Val() | inst.getSrc2Val());
			inst.setWriteResult(true);
			inst.setDetsinationValidBit(true);
			break;
		}
		case "EX-OR": {
			//RegisterState source1 = memory.get(inst.getSource1());
			//RegisterState source2 = memory.get(inst.getSource2());
			// inst.setResult((source1.getValue()) ^ (source2.getValue()));
			inst.setResult(inst.getSrc1Val() ^ inst.getSrc2Val());
			inst.setWriteResult(true);
			inst.setDetsinationValidBit(true);
			break;
		}
		case "HALT": {
			this.setProgCounter(instructions.size());
			//flush();
			squashInstructions(inst, this.getProgCounter());
			break;
		}
		case "BZ": {
			RegisterState source1 = memory.get(inst.getSource1());
			if (source1.getValue() == 0) {
				//int tempProgCounter = inst.getInstrNumber()+1;
				//tempProgCounter += inst.getLiteral();
				int tempProgCounter; 
				if(inst.getLiteral()<0){
					tempProgCounter= inst.getInstrNumber()-1;
				}else{
					tempProgCounter = inst.getInstrNumber()+1;
				}
				tempProgCounter += inst.getLiteral();
				this.setProgCounter(tempProgCounter);
				inst.setDetsinationValidBit(true);
				//flush();
				squashInstructions(inst, this.getProgCounter());
			}
			break;
		}
		case "BNZ": {
			RegisterState source1 = memory.get(inst.getSource1());
			if (source1.getValue() != 0) {
				//int tempProgCounter = inst.getInstrNumber()+1;
				//tempProgCounter += inst.getLiteral();
				int tempProgCounter; 
				if(inst.getLiteral()<0){
					tempProgCounter= inst.getInstrNumber()-1;
				}else{
					tempProgCounter = inst.getInstrNumber()+1;
				}
				tempProgCounter += inst.getLiteral();
				
				this.setProgCounter(tempProgCounter);
				inst.setDetsinationValidBit(true);
				//flush();
				squashInstructions(inst, this.getProgCounter());
			}
			break;
		}
		case "JUMP": {
			//int instDestVal = memory.get(inst.getDestination()).getValue();
			int instDestVal = memory.get(inst.getSource1()).getValue();
			int instLiteral = inst.getLiteral();
			this.setProgCounter((instDestVal + instLiteral) - 20000);
			inst.setDetsinationValidBit(true);
			//flush();
			squashInstructions(inst,this.getProgCounter());
			break;
		}
		case "NOP": {
			break;
		}
		case "BAL": {

			addr = 20000 + this.getProgCounter() - 3;
			addr += 1;
			memory.get("X").setValue(addr);
			RegisterState instDest = memory.get(inst.getDestination());
			int literal = inst.getLiteral();
			this.setProgCounter((instDest.getValue() + literal) - 20000);
			inst.setDetsinationValidBit(true);
			// System.out.println(jmp);
			//flush();
			break;
		}
		}
	}

	private void executeLSInstructionONE(Instruction inst) {
		if (inst.getDestination() != null) {
			// RegisterState destination = memory.get(inst.getDestination());
			switch (inst.getOpcode()) {
			case "LOAD": {

				// String regAddress = inst.getSource1().replaceAll("[^0-9]",
				// "");
				int loadAddress = inst.getSrc1Val() + inst.getLiteral();
				inst.setResult(loadAddress);
				//System.out.println("INst " + inst.getResult());
				/*
				 * RegisterState loadFrom = memory.get("R" + loadAddress);
				 * RegisterState loadHere = memory.get(inst.getDestination());
				 * loadHere.setValue(loadFrom.getValue());
				 */
				break;
			}
			case "STORE": {

				// String regAddress = inst.getSource1().replaceAll("[^0-9]",
				// "");
				int storeAddress = inst.getSrc1Val() +  inst.getLiteral();//memory.get(inst.getSource1()).getValue() + inst.getLiteral();
				inst.setResult(storeAddress);
				//System.out.println("INst " + inst.getResult());
				/*
				 * RegisterState storeHere = memory.get("R" + storeAddress);
				 * RegisterState storeFrom = memory.get(inst.getDestination());
				 * storeHere.setValue(storeFrom.getValue());
				 */
				break;
			}
			}
		}
	}

	private void executeLSInstructionTWO(Instruction inst) {
		/*
		 * if (inst.getDestination() != null) { RegisterState destination =
		 * memory.get(inst.getDestination()); switch (inst.getOpcode()) { case
		 * "LOAD": { RegisterState loadFrom = memory.get("R" +
		 * inst.getResult()); RegisterState loadHere =
		 * memory.get(inst.getDestination());
		 * loadHere.setValue(loadFrom.getValue()); break; } case "STORE": {
		 * RegisterState storeHere = memory.get("R" + inst.getResult());
		 * RegisterState storeFrom = memory.get(inst.getDestination());
		 * storeHere.setValue(storeFrom.getValue()); break; } } }
		 */
	}

	private void executeLSInstructionTHREE(Instruction inst) {
		if (inst.getDestination() != null) {
			// RegisterState destination = memory.get(inst.getDestination());
			switch (inst.getOpcode()) {
			case "LOAD": {
				RegisterState loadFrom = memory.get("R" + inst.getResult());
				RegisterState loadHere = memory.get(inst.getDestination());
				loadHere.setValue(loadFrom.getValue());
				inst.setDetsinationValidBit(true);
				break;
			}
			case "STORE": {
				RegisterState storeHere = memory.get("R" + inst.getResult());
				RegisterState storeFrom = memory.get(inst.getDestination()); // get from mem or instr.getDestVal()
				storeHere.setValue(storeFrom.getValue());
				inst.setDetsinationValidBit(true);
				break;
			}
			}
		}
	}

	private void flush() {
		apexPipeline.put("decode", null);
		apexPipeline.put("fetch", null);
	}

	//14 December
	private void squashInstructions(Instruction instObj, int destPC) {
		//System.out.println("HELLO!!");
		
		//Squash LSQ and IQ
		if(apexPipeline.get("iqEight")!=null){
			if(instObj.getInstrNumber()<apexPipeline.get("iqEight").getInstrNumber()){ //&&  (destPC-20000)>apexPipeline.get("iqEight").getInstrNumber()){
				apexPipeline.put("iqEight", null);
			}
		}
		if(apexPipeline.get("iqSeven")!=null){
			if(instObj.getInstrNumber()<apexPipeline.get("iqSeven").getInstrNumber() ){//&&  (destPC-20000)>apexPipeline.get("iqSeven").getInstrNumber()){
				apexPipeline.put("iqSeven", null);
			}
		}
		if(apexPipeline.get("iqSix")!=null){
			if(instObj.getInstrNumber()<apexPipeline.get("iqSix").getInstrNumber() ){//&&  (destPC-20000)>apexPipeline.get("iqSix").getInstrNumber()){
				apexPipeline.put("iqSix", null);
			}
		}
		if(apexPipeline.get("iqFive")!=null){
			if(instObj.getInstrNumber()<apexPipeline.get("iqFive").getInstrNumber() ){//&&  (destPC-20000)>apexPipeline.get("iqFive").getInstrNumber()){
				apexPipeline.put("iqFive", null);
			}
		}
		if(apexPipeline.get("iqFour")!=null){
			if(instObj.getInstrNumber()<apexPipeline.get("iqFour").getInstrNumber() ){//&&  (destPC-20000)>apexPipeline.get("iqFour").getInstrNumber()){
				apexPipeline.put("iqFour", null);
			}
		}
		if(apexPipeline.get("iqThree")!=null){
			if(instObj.getInstrNumber()<apexPipeline.get("iqThree").getInstrNumber() ){//&&  (destPC-20000)>apexPipeline.get("iqThree").getInstrNumber()){
				apexPipeline.put("iqThree", null);
			}
		}
		if(apexPipeline.get("iqTwo")!=null){
			if(instObj.getInstrNumber()<apexPipeline.get("iqTwo").getInstrNumber() ){//&&  (destPC-20000)>apexPipeline.get("iqTwo").getInstrNumber()){
				apexPipeline.put("iqTwo", null);
			}
		}
		if(apexPipeline.get("iqOne")!=null){
			if(instObj.getInstrNumber()<apexPipeline.get("iqOne").getInstrNumber() ){//&&  (destPC-20000)>apexPipeline.get("iqOne").getInstrNumber()){
				apexPipeline.put("iqOne", null);
			}
		}
		if(apexPipeline.get("lsqFour")!=null){
			if(instObj.getInstrNumber()<apexPipeline.get("lsqFour").getInstrNumber() ){//&&  (destPC-20000)>apexPipeline.get("lsqFour").getInstrNumber()){
				apexPipeline.put("lsqFour", null);
			}
		}
		if(apexPipeline.get("lsqThree")!=null){
			if(instObj.getInstrNumber()<apexPipeline.get("lsqThree").getInstrNumber() ){//&&  (destPC-20000)>apexPipeline.get("lsqThree").getInstrNumber()){
				apexPipeline.put("lsqThree", null);
			}
		}
		if(apexPipeline.get("lsqTwo")!=null){
			if(instObj.getInstrNumber()<apexPipeline.get("lsqTwo").getInstrNumber() ){//&&  (destPC-20000)>apexPipeline.get("lsqTwo").getInstrNumber()){
				apexPipeline.put("lsqTwo", null);
			}
		}
		if(apexPipeline.get("lsqOne")!=null){
			if(instObj.getInstrNumber()<apexPipeline.get("lsqOne").getInstrNumber() ){//&&  (destPC-20000)>apexPipeline.get("lsqOne").getInstrNumber()){
				apexPipeline.put("lsqOne", null);
			}
		}
		
		
		
		if(apexPipeline.get("decodeTwo")!=null){
			//if(instObj.getInstrNumber()<apexPipeline.get("decodeTwo").getInstrNumber() ){//&&  (destPC-20000)>apexPipeline.get("decodeTwo").getInstrNumber()){
				apexPipeline.put("decodeTwo", null);
			//}
		}
		if(apexPipeline.get("decodeOne")!=null){
			//if(instObj.getInstrNumber()<apexPipeline.get("decodeOne").getInstrNumber() ){//&&  (destPC-20000)>apexPipeline.get("decodeOne").getInstrNumber()){
				apexPipeline.put("decodeOne", null);
			//}
		}
		if(apexPipeline.get("fetchTwo")!=null){
			//if(instObj.getInstrNumber()<apexPipeline.get("fetchTwo").getInstrNumber() ){//&&  (destPC-20000)>apexPipeline.get("fetchTwo").getInstrNumber()){
				apexPipeline.put("fetchTwo", null);
			//}
		}
		if(apexPipeline.get("fetchOne")!=null){
			//if(instObj.getInstrNumber()<apexPipeline.get("fetchOne").getInstrNumber() ){//&&  (destPC-20000)>apexPipeline.get("fetchOne").getInstrNumber()){
				apexPipeline.put("fetchOne", null);
			//}
		}
		
		//Squash ROB
		//But first copy rob into another array list
		ArrayList<Instruction> tempInstRob=new ArrayList<>();
		
		//for(int i=0;i<(robQueue.getTailIndex()-1);i++){
		for(int i=0;i<=(robQueue.size()-1);i++){
			//Get the dequeued instruction and check the instruction number
			Instruction tempDequeuedInstr= (Instruction) robQueue.dequeue();
			//If the instruction number is 
			if(tempDequeuedInstr.getInstrNumber()<instObj.getInstrNumber()){
				tempInstRob.add(tempDequeuedInstr);
			}
		}
		
		//For testing
		robQueue.printList();
		
		
		
		//Re-populate rob with new data
		for(int i=0; i<tempInstRob.size();i++){
			robQueue.enqueue(tempInstRob.get(i));
		}
		
		//Print temp List
		for(int i=0; i<tempInstRob.size();i++){
			System.out.println("TEMP 1 "+tempInstRob.get(i).getInstrNumber());
		}
		
	}

	/*private void decodeInstruction(Instruction inst) {

		// Resolve Dependencies
		String source1 = inst.getSource1();
//		if (inst.getOpcode().equals("STORE")) { // Special handling for STORE,
//												// since an Instruction next to
//												// store can be dependent on
//												// STORE result.
//			source1 = inst.getDestination();
//		}

		if (source1 == null) {

			inst.setSrc1ROBRef(-1);
			inst.setSrc1Val(0);
			// If source1 is null then source2 will also be null
			inst.setSrc2ROBRef(-1);
			inst.setSrc2Val(0);
			inst.setStage("ready");

		} else {

			if (memory.get(source1).getLatestUsedAsDestBy() == null) { // No
																		// true
																		// dependency
																		// for
																		// Src1

				inst.setSrc1ROBRef(-1);
				inst.setSrc1Val(memory.get(source1).getValue());

				String source2 = inst.getSource2();
				if (source2 == null) { // No Src2
					inst.setSrc2ROBRef(-1);
					inst.setSrc2Val(0);
					inst.setStage("ready");
					return;
				}
				if (memory.get(source2).getLatestUsedAsDestBy() == null) { // No
																			// true
																			// dependency
																			// for
																			// Src2

					inst.setSrc2ROBRef(-1);
					inst.setSrc2Val(memory.get(source2).getValue());
					inst.setStage("ready");
				} else { // true dependency for Src2

					inst.setSrc2ROBRef(memory.get(source2).getLatestUsedAsDestBy());
					inst.setSrc2Val(-1);
					inst.setStage("stalled");
				}
			} else { // True dependency for Src 1

				inst.setSrc1ROBRef(memory.get(source1).getLatestUsedAsDestBy());
				inst.setSrc1Val(-1);
				inst.setStage("stalled");

				String source2 = inst.getSource2();
				if (source2 == null) { // No Src2
					inst.setSrc2ROBRef(-1);
					inst.setSrc2Val(0);
					return;
				}
				if (memory.get(source2).getLatestUsedAsDestBy() == null) { // No
																			// true
																			// dependency
																			// for
																			// Src2

					inst.setSrc2ROBRef(-1);
					inst.setSrc2Val(memory.get(source2).getValue());
				} else { // true dependency for Src2

					inst.setSrc2ROBRef(memory.get(source2).getLatestUsedAsDestBy());
					inst.setSrc2Val(-1);
				}
			}

			// if (!memory.get(source1).isUnderOperation()) {
			//
			// String source2 = inst.getSource2();
			// if (source2 == null) {
			//
			// inst.setStage("ready");
			// } else {
			// if (!memory.get(source2).isUnderOperation()) {
			//
			// inst.setStage("ready");
			// } else {
			//
			// inst.setStage("stalled");
			// }
			// }
			// } else {
			//
			// inst.setStage("stalled");
			// }
		}

	}*/

	private void decodeInstruction(Instruction inst) {

		// Resolve Dependencies
		String source1 = inst.getSource1();
		if (inst.getOpcode().equals("STORE")) { // Special handling for STORE,
												// since an Instruction next to
												// store can be dependent on
												// STORE result.
			source1 = inst.getDestination();
		}
		
		//Changes for halt made on 17 December
//		if(inst.getOpcode().equals("HALT")){
//			haltFlag=1;
//			this.setProgCounter(instructions.size());
//			squashInstructions(inst, this.getProgCounter());
//		}
		
		
		//Prediction taken
		/*if(inst.getOpcode().equals("BZ") || inst.getOpcode().equals("BNZ") ){
			if(inst.getLiteral()<0){
				//Prediction branch taken
				if(apexPipeline.get("decodeOne")!=null){
					apexPipeline.put("decodeOne", null);
				}
				if(apexPipeline.get("fetchTwo")!=null){
					apexPipeline.put("fetchTwo", null);
				}
				if(apexPipeline.get("fetchOne")!=null){
					apexPipeline.put("fetchOne", null);
				}
			}
		}*/
		
		if (source1 == null) {

			inst.setSrc1ROBRef(-1);
			inst.setSrc1Val(0);
			// If source1 is null then source2 will also be null
			inst.setSrc2ROBRef(-1);
			inst.setSrc2Val(0);
			inst.setStage("ready");

		} else {

			if (memory.get(source1).getLatestUsedAsDestBy() == null || robQueue.getROBFromIndex(memory.get(source1).getLatestUsedAsDestBy()) == inst) { // No
																		// true
																		// dependency
																		// for
																		// Src1

				inst.setSrc1ROBRef(-1);
				inst.setSrc1Val(memory.get(source1).getValue());

				String source2 = inst.getSource2();
				if (source2 == null) { // No Src2
					inst.setSrc2ROBRef(-1);
					inst.setSrc2Val(0);
					inst.setStage("ready");
					return;
				}
				if (memory.get(source2).getLatestUsedAsDestBy() == null || robQueue.getROBFromIndex(memory.get(source2).getLatestUsedAsDestBy()) == inst) { // No
																			// true
																			// dependency
																			// for
																			// Src2

					inst.setSrc2ROBRef(-1);
					inst.setSrc2Val(memory.get(source2).getValue());
					inst.setStage("ready");
				} else { // true dependency for Src2

					inst.setSrc2ROBRef(memory.get(source2).getLatestUsedAsDestBy());
					inst.setSrc2Val(-1);
					inst.setStage("stalled");
				}
			} else { // True dependency for Src 1

				inst.setSrc1ROBRef(memory.get(source1).getLatestUsedAsDestBy());
				inst.setSrc1Val(-1);
				inst.setStage("stalled");

				String source2 = inst.getSource2();
				if (source2 == null) { // No Src2
					inst.setSrc2ROBRef(-1);
					inst.setSrc2Val(0);
					return;
				}
				if (memory.get(source2).getLatestUsedAsDestBy() == null || robQueue.getROBFromIndex(memory.get(source2).getLatestUsedAsDestBy()) == inst) { // No
																			// true
																			// dependency
																			// for
																			// Src2

					inst.setSrc2ROBRef(-1);
					inst.setSrc2Val(memory.get(source2).getValue());
				} else { // true dependency for Src2

					inst.setSrc2ROBRef(memory.get(source2).getLatestUsedAsDestBy());
					inst.setSrc2Val(-1);
				}
			}

			// if (!memory.get(source1).isUnderOperation()) {
			//
			// String source2 = inst.getSource2();
			// if (source2 == null) {
			//
			// inst.setStage("ready");
			// } else {
			// if (!memory.get(source2).isUnderOperation()) {
			//
			// inst.setStage("ready");
			// } else {
			//
			// inst.setStage("stalled");
			// }
			// }
			// } else {
			//
			// inst.setStage("stalled");
			// }
		}

	}
	
	private void pushInstructionForward() {
		// Writeback
		if (apexPipeline.get("writeback") != null) { // Retire

			Instruction anInstruction = apexPipeline.get("writeback");
			anInstruction.setStage("completed");
			apexPipeline.put("writeback", null);
		}
		/*
		 * if (apexPipeline.get("writeback") == null) {
		 * 
		 * Instruction anInstruction = apexPipeline.get("memory");
		 * 
		 * if (anInstruction != null) {
		 * 
		 * if (robQueue.getHeadOfROB().getStage().equals("completed")) {
		 * System.out.println("ROB dequeue value for  " +
		 * robQueue.getHeadOfROB()); dequeue(); }
		 * anInstruction.setStage("writeback"); apexPipeline.put("writeback",
		 * anInstruction); apexPipeline.put("memory", null);
		 * robQueue.printList(); }
		 * 
		 * }
		 */
		// Memory Stage
		/*
		 * if (apexPipeline.get("memory") == null) { // Pripority Logic needed
		 * // Logic required for which FU to get the instruction from //
		 * Instruction anInstruction = apexPipeline.get("intFU"); String fuName
		 * = selectPriorityFU(); Instruction anInstruction =
		 * apexPipeline.get(fuName); if (anInstruction != null) { if
		 * (fuName.equals("multiplyFU") && counterMul == 3) {
		 * anInstruction.setStage("memory"); apexPipeline.put("memory",
		 * anInstruction); apexPipeline.put(fuName, null);
		 * robQueue.updateInstructionInROBQ(anInstruction); counterMul = 0; }
		 * else if (!fuName.equals("multiplyFU")) {
		 * anInstruction.setStage("memory"); apexPipeline.put("memory",
		 * anInstruction); apexPipeline.put(fuName, null);
		 * robQueue.updateInstructionInROBQ(anInstruction); }
		 * 
		 * } }
		 */

		if (apexPipeline.get("intFU") != null) { // Retire

			Instruction anInstruction = apexPipeline.get("intFU");
			if (anInstruction.getStage().equals("completed")) {
				apexPipeline.put("intFU", null);
			}
		}

		if (apexPipeline.get("multiplyFU") != null) { // Retire

			Instruction anInstruction = apexPipeline.get("multiplyFU");
			if (anInstruction.getStage().equals("completed")) {
				apexPipeline.put("multiplyFU", null);
			}
		}

		if (apexPipeline.get("lsExecuteStageThree") != null) { // Retire

			Instruction anInstruction = apexPipeline.get("lsExecuteStageThree");
			if (anInstruction.getStage().equals("completed")) {
				apexPipeline.put("lsExecuteStageThree", null);
			}
		}

		// Execute stage
		// Integer FU
		if (apexPipeline.get("intFU") == null) {
			// Logic required for which instruction to get from IQ
			// Instruction anInstruction = apexPipeline.get("iqOne");
			// Selects an insdtruction only if all sources are available and
			// corresponding FU is free
			Instruction anInstruction = iQSelection("intFU");
			if (anInstruction != null) {
				if (!anInstruction.getOpcode().equals("MUL")) {
					anInstruction.setStage("intFU");
					apexPipeline.put("intFU", anInstruction);
					// apexPipeline.put("iqOne", null);
				}
			}
		}
		if (apexPipeline.get("multiplyFU") == null) {
			// Logic required for which instruction to get from IQ
			// Instruction anInstruction = apexPipeline.get("iqOne");
			// mulCounter 4 Cycle Latency

			Instruction anInstruction = iQSelection("multiplyFU");
			if (anInstruction != null) {
				if (anInstruction.getOpcode().equals("MUL")) {
					anInstruction.setStage("multiplyFU");
					apexPipeline.put("multiplyFU", anInstruction);
					// apexPipeline.put("iqOne", null);
				}
			}

		}

		// 3 Stages of execute for Load & store
		if (apexPipeline.get("lsExecuteStageThree") == null) {
			Instruction anInstruction = apexPipeline.get("lsExecuteStageTwo");
			if (anInstruction != null) {
				anInstruction.setStage("lsExecuteStageThree");
				apexPipeline.put("lsExecuteStageThree", anInstruction);
				apexPipeline.put("lsExecuteStageTwo", null);
			}
		}
		if (apexPipeline.get("lsExecuteStageTwo") == null) {
			Instruction anInstruction = apexPipeline.get("lsExecuteStageOne");
			if (anInstruction != null) {
				anInstruction.setStage("lsExecuteStageTwo");
				apexPipeline.put("lsExecuteStageTwo", anInstruction);
				apexPipeline.put("lsExecuteStageOne", null);
			}
		}
		if (apexPipeline.get("lsExecuteStageOne") == null) {
			//Instruction anInstruction = apexPipeline.get("lsqOne");
			Instruction anInstruction = apexPipeline.get("lsqFour");
			/*anInstruction.setStage("lsExecuteStageOne");
			apexPipeline.put("lsExecuteStageOne", anInstruction);
			apexPipeline.put("lsqOne", null);*/
			
			//Changes made on 14 December begin
			if (anInstruction != null) {
				//Check if instruction load or store
				if(anInstruction.getOpcode().equals("LOAD")){
					if(anInstruction.getStage().equals("ready")){
						pushLoadStoreForward(anInstruction);
					}/*else{
						anInstruction.setStage("stalled");
					}*/
				}else if(anInstruction.getOpcode().equals("STORE")){
					//Do something for STORE
					if(anInstruction.getStage().equals("ready")){
						if(robQueue.getROBFromIndex(robQueue.getHeadIndex()).getInstrNumber()==anInstruction.getInstrNumber()){
							pushLoadStoreForward(anInstruction);
						}/*else{
							anInstruction.setStage("stalled");
						}*/
					}
				}
				
			}
			//Changes made on 14 December end
		}
		// 8 IQ stages
		if (apexPipeline.get("iqEight") == null) {

			Instruction anInstruction = apexPipeline.get("iqSeven");
			if (anInstruction != null) {
				// anInstruction.setStage("iqEight");
				apexPipeline.put("iqEight", anInstruction);
				apexPipeline.put("iqSeven", null);
			}
		}
		if (apexPipeline.get("iqSeven") == null) {

			Instruction anInstruction = apexPipeline.get("iqSix");
			if (anInstruction != null) {
				// anInstruction.setStage("iqSeven");
				apexPipeline.put("iqSeven", anInstruction);
				apexPipeline.put("iqSix", null);
			}
		}
		if (apexPipeline.get("iqSix") == null) {

			Instruction anInstruction = apexPipeline.get("iqFive");
			if (anInstruction != null) {
				// anInstruction.setStage("iqSix");
				apexPipeline.put("iqSix", anInstruction);
				apexPipeline.put("iqFive", null);
			}
		}
		if (apexPipeline.get("iqFive") == null) {

			Instruction anInstruction = apexPipeline.get("iqFour");
			if (anInstruction != null) {
				// anInstruction.setStage("iqFive");
				apexPipeline.put("iqFive", anInstruction);
				apexPipeline.put("iqFour", null);
			}
		}
		if (apexPipeline.get("iqFour") == null) {

			Instruction anInstruction = apexPipeline.get("iqThree");
			if (anInstruction != null) {
				// anInstruction.setStage("iqFour");
				apexPipeline.put("iqFour", anInstruction);
				apexPipeline.put("iqThree", null);
			}
		}
		if (apexPipeline.get("iqThree") == null) {

			Instruction anInstruction = apexPipeline.get("iqTwo");
			if (anInstruction != null) {
				// anInstruction.setStage("iqThree");
				apexPipeline.put("iqThree", anInstruction);
				apexPipeline.put("iqTwo", null);
			}
		}
		if (apexPipeline.get("iqTwo") == null) {

			Instruction anInstruction = apexPipeline.get("iqOne");
			if (anInstruction != null) {
				// anInstruction.setStage("iqTwo");
				apexPipeline.put("iqTwo", anInstruction);
				apexPipeline.put("iqOne", null);
			}
		}
		if (apexPipeline.get("iqOne") == null) {
			Instruction anInstruction = apexPipeline.get("decodeTwo");
			if (anInstruction != null) {
				String typeOfInst = apexPipeline.get("decodeTwo").getOpcode();
				if (!typeOfInst.equals("LOAD") && !typeOfInst.equals("STORE")) {
					// Insert into ROB
					// anInstruction.setStage("iqOne");
					apexPipeline.put("iqOne", anInstruction);
					apexPipeline.put("decodeTwo", null);
					// Insert into ROB
					enqueue(anInstruction);
					// decodeInstruction(apexPipeline.get("iqOne"));
				}
			}
		}
		// 4 stages of LSQ
		if (apexPipeline.get("lsqFour") == null) {
			Instruction anInstruction = apexPipeline.get("lsqThree");
			if (anInstruction != null) {
				//anInstruction.setStage("lsqFour");
				
				apexPipeline.put("lsqFour", anInstruction);
				apexPipeline.put("lsqThree", null);
			}
		}
		if (apexPipeline.get("lsqThree") == null) {

			Instruction anInstruction = apexPipeline.get("lsqTwo");
			if (anInstruction != null) {
				//anInstruction.setStage("lsqThree");
				
				apexPipeline.put("lsqThree", anInstruction);
				apexPipeline.put("lsqTwo", null);
			}
		}
		if (apexPipeline.get("lsqTwo") == null) {

			Instruction anInstruction = apexPipeline.get("lsqOne");
			if (anInstruction != null) {
				//anInstruction.setStage("lsqTwo");
				
				 apexPipeline.put("lsqTwo", anInstruction);
				 apexPipeline.put("lsqOne", null);
			}
		}
		if (apexPipeline.get("lsqOne") == null) {
			Instruction anInstruction = apexPipeline.get("decodeTwo");
			if (anInstruction != null) {
				String typeOfInst = apexPipeline.get("decodeTwo").getOpcode();
				if (typeOfInst.equals("LOAD") || typeOfInst.equals("STORE")) {
					// anInstruction.setStage("lsqOne");
					// Insert into ROB
					// insertIntoROB(anInstruction);
					apexPipeline.put("lsqOne", anInstruction);
					apexPipeline.put("decodeTwo", null);
					// Insert into ROB
					enqueue(anInstruction);
					// decodeInstruction(apexPipeline.get("lsqOne"));

				}
			}
		}

		// Decode Stage
		if (apexPipeline.get("decodeTwo") == null) {

			Instruction anInstruction = apexPipeline.get("decodeOne");
			if (anInstruction != null) {
				anInstruction.setStage("decodeTwo");

				apexPipeline.put("decodeTwo", anInstruction);
				apexPipeline.put("decodeOne", null);

			}
		}
		// Decode Stage
		if (apexPipeline.get("decodeOne") == null) {

			Instruction anInstruction = apexPipeline.get("fetchTwo");
			if (anInstruction != null) {
				anInstruction.setStage("decodeOne");
				apexPipeline.put("decodeOne", anInstruction);
				apexPipeline.put("fetchTwo", null);
			}
		}

		if (apexPipeline.get("fetchTwo") == null) {
			Instruction anInstruction = apexPipeline.get("fetchOne");
			if (anInstruction != null) {
				anInstruction.setStage("fetchTwo");
				apexPipeline.put("fetchTwo", anInstruction);
				apexPipeline.put("fetchOne", null);
			}
		}

		// Fetch Stage
		if (apexPipeline.get("fetchOne") == null) {

			if (this.progCounter < instructions.size()) {

				Instruction anInstruction = instructions.get(this.progCounter);
				anInstruction.setStage("fetchOne");
				apexPipeline.put("fetchOne", anInstruction);

				if (anInstruction.getOpcode().equals("BZ") || anInstruction.getOpcode().equals("BNZ")) {

					Instruction prevInstruction = instructions.get(this.progCounter - 1);
					anInstruction.setSource1(prevInstruction.getDestination());
				}
				if (anInstruction.getOpcode().equals("BAL") || anInstruction.getOpcode().equals("JUMP")) {
					anInstruction.setSource1(anInstruction.getDestination());
					anInstruction.setDestination(null);//changes on 16 december
				}

				this.progCounter++;

			}

		}
	}
	
	
	private void pushLoadStoreForward(Instruction anInstruction){
		anInstruction.setStage("lsExecuteStageOne");
		apexPipeline.put("lsExecuteStageOne", anInstruction);
		apexPipeline.put("lsqFour", null);
	}

	/*
	 * private void insertIntoROB(Instruction instrObj){ ROBDetails
	 * robDetails=new ROBDetails();
	 * robDetails.setDestAddress(instrObj.getDestination());
	 * robDetails.setResult(0); robDetails.setStatusBit(0);
	 * robDetails.setInstructionType(instrObj.getOpcode());
	 * robDetails.setProgROBCounter(getProgCounter()); }
	 */

	private void enqueue(Instruction anInstruction) {

		int robIndex = robQueue.enqueue(anInstruction);
		if (!anInstruction.getOpcode().equals("STORE") && !anInstruction.getOpcode().equals("JUMP") && !anInstruction.getOpcode().equals("BNZ") && !anInstruction.getOpcode().equals("BZ") && !anInstruction.getOpcode().equals("HALT")) {

			RegisterState aRegisterState = this.memory.get(anInstruction.getDestination());
			aRegisterState.setLatestUsedAsDestBy(robIndex);
//			aRegisterState.setValue(null);
			//aRegisterState.setValue(0);
		}

	}

	private void dequeue() {

		int completedInstrIndex = robQueue.getHeadIndex();
		Instruction completedInsrt = (Instruction) robQueue.dequeue();
		if(completedInsrt.getOpcode().equals("JUMP")){//16 December
			completedInsrt.setDestination(completedInsrt.getSource1());
		}
		RegisterState registerState = null;
		if(!completedInsrt.getOpcode().equals("BZ") && !completedInsrt.getOpcode().equals("BNZ")){
			 registerState= memory.get(completedInsrt.getDestination());
		}
		//This check is required for BZ and BNZ
		if(registerState!=null){
			if (registerState.getLatestUsedAsDestBy()!=null && registerState.getLatestUsedAsDestBy() == completedInstrIndex) { // Operations
																				// when
																				// instr
																				// leaves
																				// queue
				//System.out.println("--TEST--IN DQ---"+registerState.getValue());
				
				registerState.setLatestUsedAsDestBy(null);
				if(completedInsrt.isWriteResult()){ //write only for instruction whose result is to be written
					
					registerState.setValue(completedInsrt.getResult());
				}
				
				System.out.println("Commited:" + completedInsrt.getDestination() + " Value: " + completedInsrt.getResult());
			} else {
	
				if(completedInsrt.isWriteResult()){ //write only for instruction whose result is to be written
					registerState.setValue(completedInsrt.getResult());
				}
				System.out.println("Commited:" + completedInsrt.getDestination() + " Value: " + completedInsrt.getResult());
			}
		}

	}

	// Select an instruction from an FU
	private String selectPriorityFU() {
		String functionalUnitName = "lsExecuteStageThree";
		for (int i = 1; i <= 3; i++) {
			// if (i == 1) {
			if (apexPipeline.get("lsExecuteStageThree") != null) {
				apexPipeline.get("lsExecuteStageThree").setStage("completed");
				break;
			}
			// } else if (i == 2) {
			if (apexPipeline.get("intFU") != null) {
				apexPipeline.get("intFU").setStage("completed");
				break;
			}
			// } else {
			if (apexPipeline.get("multiplyFU") != null) {
				if (counterMul == 3) {
					apexPipeline.get("multiplyFU").setStage("completed");
				}
			}
			// }
		}
		return functionalUnitName;
	}

	private Instruction iQSelection(String instrExeFUType) {
		Instruction returnInstrObj = null;
		boolean breakForLoop = false;
		for (int i = 8; i >= 1; i--) {
			switch (i) {
			case 1:
				returnInstrObj = apexPipeline.get("iqOne");
				if (returnInstrObj != null) {
					// Checks if the instruction opcode corresponds to type of
					// FU
					if (isInstrOfCorrectType(instrExeFUType, returnInstrObj)) {
						// Check for dependency
						resolveDependency(returnInstrObj);
						if (returnInstrObj.getStage().equals("ready")) {
							breakForLoop = true;
							apexPipeline.put("iqOne", null);
						} else {
							returnInstrObj = null;
						}
					}
				}
				break;
			case 2:
				returnInstrObj = apexPipeline.get("iqTwo");
				if (returnInstrObj != null) {
					// Checks if the instruction opcode corresponds to type of
					// FU
					if (isInstrOfCorrectType(instrExeFUType, returnInstrObj)) {
						// Check for dependency
						resolveDependency(returnInstrObj);
						if (returnInstrObj.getStage().equals("ready")) {
							breakForLoop = true;
							apexPipeline.put("iqTwo", null);
						} else {
							returnInstrObj = null;
						}
					}
				}
				break;
			case 3:
				returnInstrObj = apexPipeline.get("iqThree");
				if (returnInstrObj != null) {
					// Checks if the instruction opcode corresponds to type of
					// FU
					if (isInstrOfCorrectType(instrExeFUType, returnInstrObj)) {
						// Check for dependency
						resolveDependency(returnInstrObj);
						if (returnInstrObj.getStage().equals("ready")) {
							breakForLoop = true;
							apexPipeline.put("iqThree", null);
						} else {
							returnInstrObj = null;
						}
					}
				}
				break;

			case 4:
				returnInstrObj = apexPipeline.get("iqFour");
				if (returnInstrObj != null) {
					// Checks if the instruction opcode corresponds to type of
					// FU
					if (isInstrOfCorrectType(instrExeFUType, returnInstrObj)) {
						// Check for dependency
						resolveDependency(returnInstrObj);
						if (returnInstrObj.getStage().equals("ready")) {
							breakForLoop = true;
							apexPipeline.put("iqFour", null);
						} else {
							returnInstrObj = null;
						}
					}
				}
				break;
			case 5:
				returnInstrObj = apexPipeline.get("iqFive");
				if (returnInstrObj != null) {
					// Checks if the instruction opcode corresponds to type of
					// FU
					if (isInstrOfCorrectType(instrExeFUType, returnInstrObj)) {
						// Check for dependency
						resolveDependency(returnInstrObj);
						if (returnInstrObj.getStage().equals("ready")) {
							breakForLoop = true;
							apexPipeline.put("iqFive", null);
						} else {
							returnInstrObj = null;
						}
					}
				}
				break;
			case 6:
				returnInstrObj = apexPipeline.get("iqSix");
				if (returnInstrObj != null) {
					// Checks if the instruction opcode corresponds to type of
					// FU
					if (isInstrOfCorrectType(instrExeFUType, returnInstrObj)) {
						// Check for dependency
						resolveDependency(returnInstrObj);
						if (returnInstrObj.getStage().equals("ready")) {
							breakForLoop = true;
							apexPipeline.put("iqSix", null);
						} else {
							returnInstrObj = null;
						}
					}
				}
				break;

			case 7:
				returnInstrObj = apexPipeline.get("iqSeven");
				if (returnInstrObj != null) {
					// Checks if the instruction opcode corresponds to type of
					// FU
					if (isInstrOfCorrectType(instrExeFUType, returnInstrObj)) {
						// Check for dependency
						resolveDependency(returnInstrObj);
						if (returnInstrObj.getStage().equals("ready")) {
							breakForLoop = true;
							apexPipeline.put("iqSeven", null);
						} else {
							returnInstrObj = null;
						}
					}
				}
				break;
			case 8:
				returnInstrObj = apexPipeline.get("iqEight");
				if (returnInstrObj != null) {
					// Checks if the instruction opcode corresponds to type of
					// FU
					if (isInstrOfCorrectType(instrExeFUType, returnInstrObj)) {
						// Check for dependency
						resolveDependency(returnInstrObj);
						if (returnInstrObj.getStage().equals("ready")) {
							breakForLoop = true;
							apexPipeline.put("iqEight", null);
						} else {
							returnInstrObj = null;
						}
					}
				}
				break;
			default:
				break;
			}

			// break the for if we find the instruction
			if (breakForLoop) {
				break;
			}
		}
		return returnInstrObj;
	}

	private void forwardResults() {

		Instruction returnInstrObj = null;
		for (int i = 8; i >= 1; i--) {
			switch (i) {
			case 1:
				returnInstrObj = apexPipeline.get("iqOne");
				if (returnInstrObj != null) {
					resolveDependency(returnInstrObj);
				}

				break;
			case 2:

				returnInstrObj = apexPipeline.get("iqTwo");
				if (returnInstrObj != null) {
					resolveDependency(returnInstrObj);
				}
				break;
			case 3:
				returnInstrObj = apexPipeline.get("iqThree");
				if (returnInstrObj != null) {
					resolveDependency(returnInstrObj);
				}
				break;

			case 4:
				returnInstrObj = apexPipeline.get("iqFour");
				if (returnInstrObj != null) {
					resolveDependency(returnInstrObj);
				}
				break;
			case 5:
				returnInstrObj = apexPipeline.get("iqFive");
				if (returnInstrObj != null) {
					resolveDependency(returnInstrObj);
				}
				break;
			case 6:
				returnInstrObj = apexPipeline.get("iqSix");
				if (returnInstrObj != null) {
					resolveDependency(returnInstrObj);
				}
				break;

			case 7:
				returnInstrObj = apexPipeline.get("iqSeven");
				if (returnInstrObj != null) {
					resolveDependency(returnInstrObj);
				}
				break;
			case 8:
				returnInstrObj = apexPipeline.get("iqEight");
				if (returnInstrObj != null) {
					resolveDependency(returnInstrObj);
				}
				break;
			default:
				break;
			}
		}

		for (int i = 4; i >= 1; i--) {
			switch (i) {
			case 1:
				returnInstrObj = apexPipeline.get("lsqOne");
				if (returnInstrObj != null) {
					resolveDependency(returnInstrObj);
				}
				break;
			case 2:
				returnInstrObj = apexPipeline.get("lsqTwo");
				if (returnInstrObj != null) {
					resolveDependency(returnInstrObj);
				}
				break;
			case 3:
				returnInstrObj = apexPipeline.get("lsqThree");
				if (returnInstrObj != null) {
					resolveDependency(returnInstrObj);
				}
				break;
			case 4:
				returnInstrObj = apexPipeline.get("lsqFour");
				if (returnInstrObj != null) {
					resolveDependency(returnInstrObj);
				}
				break;
			default:
				break;
			}
		}

	}

	private void resolveDependency(Instruction anInstruction) {

		int src1RobRefernce = anInstruction.getSrc1ROBRef();
		int src2RobRefernce = anInstruction.getSrc2ROBRef();

		if (src1RobRefernce == -1 && src2RobRefernce == -1) {

			anInstruction.setStage("ready");
			return;
		}

		if (src1RobRefernce != -1) {

			// Get instruction dependent on from ROB
			Instruction src1RefInst = robQueue.getROBFromIndex(src1RobRefernce);
			if (!src1RefInst.isDetsinationValidBit()) { // Check if the target
														// instruction is
														// executed
				anInstruction.setStage("stalled");
			} else {
				anInstruction.setStage("ready");
				// Dependency resolved...read values
				anInstruction.setSrc1ROBRef(-1);
				anInstruction.setSrc1Val(src1RefInst.getResult());
				// Values read so ROB instruction is completed
				// src1RefInst.setStage("completed");
			}
		}

		if (src2RobRefernce != -1) {

			// Get instruction dependent on from ROB
			Instruction src2RefInst = robQueue.getROBFromIndex(src2RobRefernce);
			if (!src2RefInst.isDetsinationValidBit()) { // Check if the target
														// instruction is
														// executed
				anInstruction.setStage("stalled");
			} else {
				anInstruction.setStage("ready");
				// Dependency resolved...read values
				anInstruction.setSrc2ROBRef(-1);
				anInstruction.setSrc2Val(src2RefInst.getResult());
				// Values read so ROB instruction is completed
				// src2RefInst.setStage("completed");
			}
		}

	}

	private boolean isInstrOfCorrectType(String instrExeFUType, Instruction returnInstrObj) {
		if (instrExeFUType.equals("multiplyFU")) {
			if (returnInstrObj.getOpcode().equals("MUL")) {
				return true;
			} else {
				return false;
			}
		} else {
			if (!returnInstrObj.getOpcode().equals("MUL")) {
				return true;
			} else {
				return false;
			}
		}
	}

	public static boolean isInteger(String s) {
		try {
			Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return false;
		} catch (NullPointerException e) {
			return false;
		}
		return true;
	}

	// Main method
	public static void main(String args[]) {

		ApexSimulator asm = new ApexSimulator();
		
		//Get the filename from command line
		FILENAME=args[0];
		
//		asm.initialize();
//		asm.executeInstructions(30);

		Scanner scanner = new Scanner(System.in);

		while (true) {

			String command = scanner.next();

			if (command.equals("exit")) {
				break;
			}
			switch (command) {
			case "initialize":
				asm.initialize();
				System.out.println("Initialized.");
				break;
			case "simulate":
				int executionCyles = Integer.parseInt(scanner.next());
				asm.executeInstructions(executionCyles);
				System.out.println("Executed.");
				break;
			case "display":
				asm.printApexState();
				asm.printMemoryState();
				break;
			}
		}
		scanner.close();
	}

	public int getProgCounter() {
		return progCounter;
	}

	public void setProgCounter(int progCounter) {
		this.progCounter = progCounter;
	}

}
