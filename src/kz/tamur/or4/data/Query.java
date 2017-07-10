package kz.tamur.or4.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.ws.rs.core.MultivaluedMap;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;

import kz.tamur.or4.data.bind.CompRefType;
import kz.tamur.or4.data.bind.FieldType;
import kz.tamur.or4.data.bind.QueryType;

public class Query {
	
	protected static Pattern paramPattern = Pattern.compile(":(\\w+)");
	
	protected InterfaceConfig config;
	protected String sql;
	protected String whereSql;
	protected Map<String, Integer> paramIndexes = new HashMap<>();
	protected Map<String, FieldType> fields = new HashMap<>();
	
	protected List<Query> parents = new ArrayList<>();
	protected List<Query> children = new ArrayList<>();

	private QueryComponet comp;

	public Query(InterfaceConfig config, QueryComponet comp) {
		this.config = config;
		this.comp = comp;
		
		StringBuilder sql = new StringBuilder("SELECT ");
		sql.append(comp.getKey());
		
		List<FieldType> children = comp.getFields();
		for (FieldType child : children) {
			FieldType field = (FieldType)child;
			sql.append(',').append(field.getSrc());
			fields.put(field.getId(), field);
		}
		sql.append(" FROM ").append(config.getDefaultSchema()).append('.').append(comp.getSrc());
		
		List<CompRefType> compRefs = comp.getCompRef();
		for (CompRefType compRef : compRefs) {
			Query query = config.getQuery(compRef.getId());
			parents.add(query);
			query.children.add(this);
		}
		
		QueryType query = comp.getQuery();
		if (query != null) {
			Matcher m = paramPattern.matcher(query.getValue());
			int i = 1;
			while (m.find()) {
				paramIndexes.put(m.group(1), i++);
			}
			StringBuilder whereSql = new StringBuilder(" WHERE ");
			whereSql.append(query.getValue().replaceAll(":\\w+", "?").replaceAll("FROM (\\S+)", "FROM " + config.getDefaultSchema() + ".$1"));
			sql.append(whereSql);
			this.whereSql = whereSql.toString();
		}
		this.sql = sql.toString();
	}
	
	public String getId() {
		return comp.getId();
	}
	
	public void getData(MultivaluedMap<String, String> queryParams, JsonNode stateJson, Map<String, Map<String, List<Object>>> res) throws Exception {

		Map<String, List<Object>> resPanel = new HashMap<>();
		resPanel.put("__key", new ArrayList<>());

		if (stateJson != null && comp instanceof TableComponent) {
			JsonNode node = stateJson.get(comp.getId());
			if (node != null) {
				List<Object> selList = new ArrayList<>();
				selList.add(node.asInt());
				resPanel.put("__sel", selList);
			}
		}
		
		List<FieldType> children = comp.getFields();
		for (FieldType child : children) {
				resPanel.put(child.getId(), new ArrayList<>());
		}
		
		try (
				Connection conn = config.getConnection();
				PreparedStatement pst = conn.prepareStatement(sql);
				) {
			
			for (String param : queryParams.keySet()) {
				Integer paramIndex = paramIndexes.get(param);
				if (paramIndex != null) {
					pst.setObject(paramIndex, queryParams.getFirst(param), config.getParameterSqlType(param));
				}
			}
			
			List<CompRefType> compRefs = comp.getCompRef();
			for (CompRefType compRef : compRefs) {
				Integer paramIndex = paramIndexes.get(compRef.getId());
				if (paramIndex != null) {
					Query query = config.getQuery(compRef.getId());
					Object value = null;
					if (query.comp instanceof TableComponent) {
						value = stateJson != null ? stateJson.get(compRef.getId()).asInt() : null;
					} else {
						value = res.get(query.comp.getId()).get(compRef.getId()).get(0);
					}
					pst.setObject(paramIndex, value, query.getFieldSqlType(compRef.getId()));
				}
			}

			ResultSet rs = pst.executeQuery();
			while (rs.next()) {
				resPanel.get("__key").add(rs.getObject(1));
				int i = 0;
				for (Object child : children) {
					FieldType field = (FieldType)child;
					resPanel.get(field.getId()).add(rs.getObject(i++ + 2));
				}
			}
			rs.close();
		}
		
		res.put(comp.getId(), resPanel);
		
		for (Query query : this.children) {
			query.getData(queryParams, stateJson, res);
		}
	}

	public void setData(JsonNode node, MultivaluedMap<String, String> queryParams, ObjectNode stateJson, List<Query> queriesToRefresh) throws Exception {

		StringBuilder sql = new StringBuilder("UPDATE ").append(config.getDefaultSchema()).append('.').append(comp.getSrc()).append(" SET ");
		int i = 0;
		for (Iterator<String> nameIt = node.getFieldNames(); nameIt.hasNext();) {
			String name = nameIt.next();
			if ("__sel".equals(name)) {
				stateJson.put(comp.getId(), node.get(name));
				queriesToRefresh.addAll(children);
				continue;
			}
			if ("__key".equals(name)) {
				continue;
			}
			FieldType field = fields.get(name);
			if (i++ > 0) {
				sql.append(',');
			}
			sql.append(field.getSrc()).append("=?");
		}
		
		if (i > 0) {
			sql.append(" WHERE ").append(comp.getKey()).append("=?");
	
			try (
					Connection conn = config.getConnection();
					PreparedStatement pst = conn.prepareStatement(sql.toString());
					) {
				
				i = 1;
				for (Iterator<String> nameIt = node.getFieldNames(); nameIt.hasNext();) {
					String name = nameIt.next();
					if (name.startsWith("__")) {
						continue;
					}
					setStatementParam(name, pst, i++, node.get(name));
				}
				pst.setObject(i++, node.get("__key").asInt(), Types.INTEGER);
				
				pst.executeUpdate();
			}
		}
	}
	
	public int getFieldSqlType(final String fieldId) {
		FieldType field = fields.get(fieldId);
		return field != null ? config.getSqlType(field.getType()) : Types.INTEGER;
	}
	
	private void setStatementParam(final String fieldId, final PreparedStatement pst, final int index, JsonNode valueNode) throws SQLException {
		final FieldType field = fields.get(fieldId);
		final String type = field.getType();
		if (InterfaceConfig.FT_INTEGER.equals(type) || InterfaceConfig.FT_OBJECT.equals(type)) {
			pst.setInt(index, valueNode.asInt());
		} else if (InterfaceConfig.FT_STRING.equals(type)) {
			pst.setString(index, valueNode.asText());
		}
	}
}
