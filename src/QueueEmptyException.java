
public class QueueEmptyException extends RuntimeException{
	
	    public QueueEmptyException(){
	        super();
	    }
	     
	    public QueueEmptyException(String message){
	        super(message);
	    }
	     
	    public QueueEmptyException(String message, Throwable cause){
	        super(message, cause);
	    }
	
}
