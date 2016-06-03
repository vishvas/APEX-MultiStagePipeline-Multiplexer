COA APEX Simulation Project Part 2

Make Command:
make

Execute Command:
java -cp target/prj1.jar ApexSimulator Input.txt

Assumption:
1)Reorder Buffer (ROB):
-ROB works as physical register where register is renamed.

2)Load-Store-Queue(LSQ):
-Instruction of LSQ are always dequeued from the Head.

 
Team Member Work:
1) Vishvas Patel
-Created PushForward method which transfer one instruction into another
-Created Execution Unit which perform Operations according to the instruction opcode.
-Testing of Instruction sequence.

2) Gaurav Shankur 
-Created Instruction forwarding mechanism. Which send the execution data to the IQ, LSQ, Decode 2 stage.
-Created Priority and Function Unit selection Logic.
-Testing of Instruction sequence.

3) Akshay Shah
-Created Branch Prediction terminology.
-Created ROB Mechanism and Printing of Instruction sequence.
-Testing of Instruction sequence.
