package kz.tamur.or4.data;

import java.util.List;

import kz.tamur.or4.data.bind.CompRefType;
import kz.tamur.or4.data.bind.FieldType;
import kz.tamur.or4.data.bind.QueryType;

public interface QueryComponet {

	String getId();
	String getKey();
	String getSrc();
	List<FieldType> getFields();
	List<CompRefType> getCompRef();
	QueryType getQuery();
}
