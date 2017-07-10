package kz.tamur.or4.data.action;

public class FieldState extends ComponentState {

	public String compId;
	public String oldValue;
	public String newValue;
	
	public FieldState(String id) {
		super(id);
	}
	
	public String getOldValue() {
		return oldValue;
	}
	
	public String getNewValue() {
		return newValue;
	}
}
