package kz.tamur.or4.data.action;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;

import kz.tamur.or4.data.InterfaceConfig;
import kz.tamur.or4.data.Query;

public abstract class ActionHandler {
	
	private InterfaceConfig config;
	private JsonNode inputData;
	private Map<String, Map<String, List<Object>>> outputData = new HashMap<>();;

	public void setInterfaceConfig(InterfaceConfig config) {
		this.config = config;
	}
	
	public void setInputData(JsonNode data) {
		this.inputData = data;
	}

	public Map<String, Map<String, List<Object>>> getOutputData() {
		return outputData;
	}
	
	public Object getFieldValue(final String fieldId) {
		String panelId = config.getQuery(fieldId).getId();
		JsonNode panelValues = inputData.get(panelId);
		if (panelValues != null) {
			JsonNode value = panelValues.get(fieldId);
			if (value != null) {
				return value.asText();
			}
		}
		return null;
	}
	
	public void setField(final String fieldId, Object value) {
		String panelId = config.getQuery(fieldId).getId();
		Map<String, List<Object>> resPanel = outputData.get(panelId);
		if (resPanel == null) {
			resPanel = new HashMap<>();
			outputData.put(panelId, resPanel);
		}
		List<Object> values = resPanel.get(fieldId);
		if (values == null) {
			values = new ArrayList<>();
			resPanel.put(fieldId, values);
		}
		values.add(value);
	}

	protected FieldState getState(final String compId) {
		return null;
	}
	
	protected Connection getConnection() throws Exception {
		return config.getConnection();
	}
}
