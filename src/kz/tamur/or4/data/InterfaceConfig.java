package kz.tamur.or4.data;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;

import kz.tamur.or4.data.action.ActionHandler;
import kz.tamur.or4.data.bind.ActionType;
import kz.tamur.or4.data.bind.FieldType;
import kz.tamur.or4.data.bind.Interface;
import kz.tamur.or4.data.bind.PanelType;
import kz.tamur.or4.data.bind.ParamType;
import kz.tamur.or4.data.bind.TableType;

public class InterfaceConfig {
	
	public static final String FT_STRING = "string";
	public static final String FT_INTEGER = "integer";
	public static final String FT_OBJECT = "object";

	private static DataSource ds;

	private final InterfaceManager interfaceManager;
	private Interface xml;
	private List<Query> queries = new ArrayList<>();
	private Map<String, Query> queryById = new HashMap<>();
	private Map<String, ActionType> actionById = new HashMap<>();
	private Map<String, Integer> paramTypes = new HashMap<>();

	public InterfaceConfig(File configFile, InterfaceManager interfaceManager) throws Exception {
		this.interfaceManager = interfaceManager;
		JAXBContext jc = JAXBContext.newInstance("kz.tamur.or4.data.bind");
		Unmarshaller u = jc.createUnmarshaller();
		this.xml = (Interface)u.unmarshal(new FileInputStream(configFile));
		
		for (ParamType param : xml.getParam()) {
			paramTypes.put(param.getId(), getSqlType(param.getType()));
		}
		
		createQueries(this.xml);
		
		for (ActionType action : xml.getAction()) {
			actionById.put(action.getId(), action);
		}
	}
	
	public String getId() {
		return xml.getId();
	}
	
	public Interface getInterface() {
		return xml;
	}
	
	public String getDefaultSchema() {
		return interfaceManager.getDefaultSchema();
	}
	
	public InterfaceManager getInterfaceManager() {
		return interfaceManager;
	}
	
	public Query getQuery(final String id) {
		return queryById.get(id);
	}
	
	public Object getData(String compId, MultivaluedMap<String, String> params) throws Exception {
		Query query = queryById.get(compId);
		Map<String, Map<String, List<Object>>> res = new HashMap<>();
		query.getData(params, null, res);
		return res;
	}
	
	public Object getData(MultivaluedMap<String, String> params, JsonNode stateJson) throws Exception {
		Map<String, Map<String, List<Object>>> res = new HashMap<>();
		Set<String> evaluatedQueryIds = new HashSet<>();
		for (Query query : queries) {
			String queryId = query.getId();
			if (!evaluatedQueryIds.contains(queryId)) {
				query.getData(params, stateJson, res);
				evaluatedQueryIds.add(queryId);
			}
		}
		return res;
	}

	public Object setData(JsonNode root, MultivaluedMap<String, String> params, ObjectNode stateJson) throws Exception {
		List<Query> queriesToRefresh = new ArrayList<>();
		for (Iterator<String> nameIt = root.getFieldNames(); nameIt.hasNext();) {
			String name = nameIt.next();
			Query query = queryById.get(name);
			query.setData(root.get(name), params, stateJson, queriesToRefresh);
		}
		Map<String, Map<String, List<Object>>> res = new HashMap<>();
		for (Query query : queriesToRefresh) {
			query.getData(params, stateJson, res);
		}
		return res;
	}
	
	public Object execute(final String actionId, JsonNode data) throws Exception {
		ActionType action = actionById.get(actionId);
		ActionClassLoader classLoader = new ActionClassLoader(InterfaceConfig.class.getClassLoader());
		Class<?> clazz = classLoader.loadClass(xml.getClazz());
		Method method = clazz.getMethod(action.getMethod());
		ActionHandler inst = (ActionHandler)clazz.newInstance();
		inst.setInterfaceConfig(this);
		inst.setInputData(data);
		method.invoke(inst);
		return inst.getOutputData();
	}

	public int getParameterSqlType(String paramId) {
		return paramTypes.get(paramId);
	}
	
	private void createQueries(Object comp) {
		if (comp instanceof Interface) {
			for (Object child : ((Interface)comp).getChildren()) {
				createQueries(child);
			}
		
		} else if (comp instanceof TableType) {
			TableType table = (TableType)comp;
			QueryComponet qcomp = new TableComponent(table);
			Query query = new Query(this, qcomp);
			queryById.put(qcomp.getId(), query);
			for (FieldType field : qcomp.getFields()) {
				queryById.put(field.getId(), query);
			}
			queries.add(query);

		} else if (comp instanceof PanelType) {
			PanelType panel = (PanelType)comp;
			QueryComponet qcomp = new PanelComponent(panel);
			Query query = new Query(this, qcomp);
			queryById.put(qcomp.getId(), query);
			for (FieldType field : qcomp.getFields()) {
				queryById.put(field.getId(), query);
			}
			queries.add(query);
		}
	}
	
	public int getSqlType(String type) {
		if (FT_INTEGER.equals(type)) {
			return Types.INTEGER;
		} else if (FT_OBJECT.equals(type)) {
			return Types.INTEGER;
		} else if (FT_STRING.equals(type)) {
			return Types.VARCHAR;
		}
		return 0;
	}
	
	public Connection getConnection() throws Exception {
		if (ds == null) {
			InitialContext ic = new InitialContext();
			ds = (DataSource)ic.lookup("/PostgreSqlDS");
		}
		return ds.getConnection();
	}
}
