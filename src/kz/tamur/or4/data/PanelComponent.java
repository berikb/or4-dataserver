package kz.tamur.or4.data;

import java.util.List;

import kz.tamur.or4.data.bind.CompRefType;
import kz.tamur.or4.data.bind.FieldType;
import kz.tamur.or4.data.bind.PanelType;
import kz.tamur.or4.data.bind.QueryType;

public class PanelComponent implements QueryComponet {
	
	private PanelType panel;
	
	public PanelComponent(PanelType panel) {
		this.panel = panel;
	}

	@Override
	public String getId() {
		return panel.getId();
	}

	@Override
	public String getKey() {
		return panel.getKey();
	}

	@Override
	public String getSrc() {
		return panel.getSrc();
	}

	@Override
	public List<FieldType> getFields() {
		return panel.getField();
	}

	@Override
	public List<CompRefType> getCompRef() {
		return panel.getCompRef();
	}

	@Override
	public QueryType getQuery() {
		return panel.getQuery();
	}

}
