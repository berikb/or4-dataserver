package kz.tamur.or4.data;

import java.util.List;

import kz.tamur.or4.data.bind.CompRefType;
import kz.tamur.or4.data.bind.FieldType;
import kz.tamur.or4.data.bind.QueryType;
import kz.tamur.or4.data.bind.TableType;

public class TableComponent implements QueryComponet {
	
	private TableType table;

	public TableComponent(TableType table) {
		this.table = table;
	}

	@Override
	public String getId() {
		return table.getId();
	}

	@Override
	public String getKey() {
		return table.getKey();
	}

	@Override
	public String getSrc() {
		return table.getSrc();
	}

	@Override
	public List<FieldType> getFields() {
		return table.getColumn();
	}

	@Override
	public List<CompRefType> getCompRef() {
		return table.getCompRef();
	}

	@Override
	public QueryType getQuery() {
		return table.getQuery();
	}
}
