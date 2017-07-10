package kz.tamur.or4.data.action;

public abstract class ComponentState {
	
	private final String id;
	
	protected ComponentState(final String id) {
		this.id = id;
	}
	
	public String getId() {
		return id;
	}

}
