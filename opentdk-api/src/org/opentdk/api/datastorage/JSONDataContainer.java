package org.opentdk.api.datastorage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opentdk.api.datastorage.BaseContainer.EContainerFormat;
import org.opentdk.api.filter.Filter;
import org.opentdk.api.filter.FilterRule;
import org.opentdk.api.io.FileUtil;
import org.opentdk.api.logger.MLogger;
import org.opentdk.api.util.ListUtil;
import org.yaml.snakeyaml.Yaml;

/**
 * Specific data container class for the JSON format. In its read and write methods the
 * {@link org.json.JSONObject} class gets used to handle sources from an {@link java.io.InputStream}
 * or a JSON file.
 * 
 * @author LK Test Solutions
 * @see org.opentdk.api.datastorage.DataContainer
 */
public class JSONDataContainer implements CustomContainer {

	/**
	 * The instance of the {@link DataContainer} that the JSONDataContainer works with.
	 */
	private final DataContainer dc;

	/**
	 * Container object for the JSON data. Supports several read and write methods. Gets initialized in
	 * {@link #readData(Filter)}.
	 */
	private JSONObject json;

	/**
	 * Construct a new specific container for JSON files. This gets done by the {@link DataContainer}
	 * class in its adaptContainer method. After initialization, it gets passed to this class.
	 *
	 * @param dCont {@link #dc}
	 */
	JSONDataContainer(DataContainer dCont) {
		dc = dCont;
		dc.containerFormat = EContainerFormat.JSON;
		dc.getImplicitHeaders().add("XPath");
	}

	@Override
	public void readData(Filter filter) {
		String content = null;
		if (!dc.getFileName().isEmpty()) {
			content = FileUtil.getContent(dc.getFileName());
		} else if (dc.getInputStream() != null) {
			InputStreamReader inputStreamReader = new InputStreamReader(dc.getInputStream());
			Stream<String> streamOfString = new BufferedReader(inputStreamReader).lines();
			content = streamOfString.collect(Collectors.joining());

			streamOfString.close();
			try {
				inputStreamReader.close();
			} catch (IOException e) {
				MLogger.getInstance().log(Level.SEVERE, e);
			}
		}
		if (content != null) {
			json = new JSONObject(content);
			fillDc();
		}
	}

	private void fillDc() {
		if (!json.isEmpty()) {
			String[] names = JSONObject.getNames(json);
			if (names.length > 0) {
				dc.setHeaders(names);
				for (String header : names) {
					dc.setColumn(header, new String[] { json.get(header).toString() });
				}
			}
		}
	}
	
	/**
	 * @return {@link #json} object as map to access from other containers.
	 */
	Map<String, Object> getJsonAsMap() {
		return json.toMap();
	}

	/**
	 * Possibility to set the content of the {@link #json} object from other containers.
	 * 
	 * @param content map that has the whole content of the JSON source
	 */
	void setJsonWithMap(Map<String, Object> content) {
		if(content == null) {
			MLogger.getInstance().log(Level.WARNING, "Map object is not initialized ==> No JSON content to read", getClass().getSimpleName(), "setJsonWithMap");
		} else {
			json = new JSONObject(content);
			fillDc();
		}
	}

	@Override
	public void writeData(String srcFileName) {
		if (json == null || json.isEmpty()) {
			MLogger.getInstance().log(Level.WARNING, "JSON object is not initialized or empty ==> No JSON content to write", getClass().getSimpleName(), "writeData");
		} else {
			try {
				FileUtil.createFile(srcFileName, true);
				FileUtil.writeOutputFile(json.toString(1), srcFileName);
			} catch (JSONException | IOException e) {
				MLogger.getInstance().log(Level.SEVERE, e);
				throw new RuntimeException(e);
			}
		}

	}

	@Override
	public Object[] getColumn(String headerName, Filter fltr) {
		String[] ret = null;
		List<FilterRule> implFilterRules = dc.getImplFilterRules(fltr);
		List<String> filteredValues = new ArrayList<>();

		if (implFilterRules.size() > 0) {
			for (FilterRule frImpl : fltr.getFilterRules()) {
				if (frImpl.getHeaderName().equalsIgnoreCase("XPath")) {

					String searchExp = "/" + frImpl.getValue().replace(";", "/") + "/" + headerName;
					Object result = json.query(searchExp);
					JSONObject.testValidity(result);

					if (result == null) {
						throw new JSONException("JSON key not present");
					} else {
						String sResult = result.toString();
						if (result instanceof JSONArray) {
							JSONArray values = (JSONArray) result;
							for (int i = 0; i < values.length(); i++) {
								filteredValues.add(values.get(i).toString());
							}
						} else {
							filteredValues.add(sResult);
						}
					}
				}
			}
			ret = ListUtil.asStringArr(filteredValues);
		} else {
			Object result = json.get(headerName);
			String sResult = result.toString();

			if (result instanceof JSONArray) {
				JSONArray values = (JSONArray) result;
				ret = new String[values.length()];
				for (int i = 0; i < values.length(); i++) {
					ret[i] = values.get(i).toString();
				}
			} else {
				ret = new String[] { sResult };
			}
		}
		return ret;
	}

	@Override
	public void addField(String headerName, String value, Filter fltr) {
		this.addField(headerName, "", value, fltr);
	}

	@Override
	public void addField(String headerName, String fieldName, String newFieldValue, Filter fltr) {
		this.addField(headerName, fieldName, "", newFieldValue, fltr);
	}

	@Override
	public void addField(String headerName, String fieldName, String oldFieldValue, String newFieldValue, Filter fltr) {
		if (fltr.getFilterRules().isEmpty()) {
			Object newValue = this.getDataType(newFieldValue);
			json.append(headerName, newValue);
		}
		for (FilterRule fltrRule : fltr.getFilterRules()) {
			if (fltrRule.getHeaderName().equalsIgnoreCase("XPath") && StringUtils.isNotBlank(fltrRule.getValue())) {
				String searchExp = fltrRule.getValue().replace(";", "/") + "/" + headerName;
				Object newValue = this.getDataType(newFieldValue);
				Object result = null;

				List<String> keyList = ListUtil.asList(searchExp.split("/"));
				int i = 0;
				for (String key : keyList) {
					if (i == 0) {
						result = json.get(key);
					} else {
						if (result instanceof JSONObject && i < keyList.size() - 1) {
							result = ((JSONObject) result).get(key);
						} else if (result instanceof JSONArray && i < keyList.size() - 1) {
							result = ((JSONArray) result).get(Integer.parseInt(key));
						} else {
							break;
						}
					}
					i++;
				}
				((JSONObject) result).append(headerName, newValue);
				break;
			}
		}
	}

	@Override
	public void deleteField(String headerName, String fieldName, String fieldValue, Filter fltr) {
		if (fltr.getFilterRules().isEmpty()) {
			json.remove(headerName);
		}
		for (FilterRule fltrRule : fltr.getFilterRules()) {
			if (fltrRule.getHeaderName().equalsIgnoreCase("XPath") && StringUtils.isNotBlank(fltrRule.getValue())) {
				String searchExp = fltrRule.getValue().replace(";", "/") + "/" + headerName;
				Object result = null;

				List<String> keyList = ListUtil.asList(searchExp.split("/"));
				int i = 0;
				for (String key : keyList) {
					if (i == 0) {
						result = json.get(key);
					} else {
						if (result instanceof JSONObject && i < keyList.size() - 1) {
							result = ((JSONObject) result).get(key);
						} else if (result instanceof JSONArray && i < keyList.size() - 1) {
							result = ((JSONArray) result).get(Integer.parseInt(key));
						} else {
							break;
						}
					}
					i++;
				}
				((JSONObject) result).remove(headerName);
				break;
			}
		}
	}

	/**
	 * Called when a value, array or object should be changed or created.
	 * 
	 * @param headerName JSON key
	 * @param occurences unused
	 * @param value      data to set {@literal -} the JSON data type gets parsed e.g. 'true' becomes a
	 *                   boolean or '"Test"' becomes a string or '[value1,value2]' an array
	 * @param fltr       filter object to find the correct field
	 */
	@Override
	public void setFieldValues(String headerName, int[] occurences, String value, Filter fltr) {
		occurences = null;
		if (fltr.getFilterRules().isEmpty()) {
			Object newValue = this.getDataType(value);
			json.put(headerName, newValue);
		}
		for (FilterRule fltrRule : fltr.getFilterRules()) {
			if (fltrRule.getHeaderName().equalsIgnoreCase("XPath") && StringUtils.isNotBlank(fltrRule.getValue())) {
				String searchExp = fltrRule.getValue().replace(";", "/") + "/" + headerName;
				Object newValue = this.getDataType(value);
				Object result = null;

				List<String> keyList = ListUtil.asList(searchExp.split("/"));
				int i = 0;
				for (String key : keyList) {
					if (i == 0) {
						result = json.get(key);
					} else {
						if (result instanceof JSONObject && i < keyList.size() - 1) {
							result = ((JSONObject) result).get(key);
						} else if (result instanceof JSONArray && i < keyList.size() - 1) {
							result = ((JSONArray) result).get(Integer.parseInt(key));
						} else {
							break;
						}
					}
					i++;
				}
				if (result instanceof JSONArray) {
					((JSONArray) result).put(newValue);
				} else {
					((JSONObject) result).put(headerName, newValue);
				}
				break;
			}
		}
	}

	/**
	 * Retrieves the data type out of the committed value. This can be a JSONObject, JSONArray or any
	 * other primitive data type like string, integer or boolean. E.g. if the input string has leading
	 * and trailing '"' it gets interpreted as JSONString. If not it gets wrapped into a number or
	 * boolean. If it has the syntax '[value1,value2]' it gets wrapped as JSONarray. And if it has the
	 * syntax '{...}' it gets wrapped as JSONObject. A 'null' value is allowed, too, and gets a
	 * JSON.NULL object.
	 * 
	 * @param newValue the value to add or put that should be checked
	 * @return the detected object type depending on the syntax of the committed string
	 */
	private Object getDataType(String newValue) {
		JSONObject.testValidity(newValue);

		Object ret = null;
		if (newValue.startsWith("\"") && newValue.endsWith("\"")) {
			ret = newValue.substring(newValue.indexOf("\"") + 1, newValue.indexOf("\""));
		} else {
			try {
				ret = new JSONArray(newValue);
			} catch (JSONException e) {
				ret = null;
			}
			if (ret == null) {
				try {
					ret = new JSONObject(newValue);
				} catch (JSONException e) {
					ret = null;
				}
				if (ret == null) {
					ret = JSONObject.stringToValue(newValue);
				}
			}
		}
		return ret;
	}

	@Override
	public String asString() {
		String ret = "";
		if (json == null || json.isEmpty()) {
			MLogger.getInstance().log(Level.WARNING, "JSON object is not initialized or empty ==> No JSON content to return", getClass().getSimpleName(), "asString");
		} else {
			ret = json.toString(1);
		}
		return ret;
	}
	
	@Override
	public String asString(EContainerFormat format) {
		switch(format) {
		case JSON:
			MLogger.getInstance().log(Level.INFO, "Format already is JSON. Call asString", getClass().getSimpleName(), "asString(format)");
			return asString();
		case YAML:
			Yaml yaml = new Yaml();
			return yaml.dump(yaml.dumpAsMap(getJsonAsMap()));
		default:
			MLogger.getInstance().log(Level.WARNING, "Format not supported to export from YAML", getClass().getSimpleName(), "asString(format)");
			return "";
		}
	}

}
