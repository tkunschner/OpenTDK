package RegressionTest.Dispatcher;

import RegressionTest.BaseRegression;

public class RT_noFile_Default_values extends BaseRegression {

	public static void main(String[] args) {
		new RT_noFile_Default_values();
	}
	
	@Override
	public void runTest() {
		
		// Check default values defined in E_PropertiesFile_Dispatcher_values.java
		testResult(E_PropertiesFile_Dispatcher_values.COUNTRY.getValue(), "Country", "Germany");
		testResult(E_PropertiesFile_Dispatcher_values.LANGUAGE.getValue(), "Language", "german");
		testResult(E_PropertiesFile_Dispatcher_values.CAPITAL_CITY.getValue(), "CapitalCity", "Berlin");
		
		// Change one value and check the values of E_PropertiesFile_Dispatcher_values
		E_PropertiesFile_Dispatcher_values.COUNTRY.setValue("France");
		testResult(E_PropertiesFile_Dispatcher_values.COUNTRY.getValue(), "Country", "France");
		testResult(E_PropertiesFile_Dispatcher_values.LANGUAGE.getValue(), "Language", "german");
		testResult(E_PropertiesFile_Dispatcher_values.CAPITAL_CITY.getValue(), "City", "Berlin");

		// Change the other values and check the values of E_PropertiesFile_Dispatcher_values
		E_PropertiesFile_Dispatcher_values.LANGUAGE.setValue("french");
		testResult(E_PropertiesFile_Dispatcher_values.LANGUAGE.getValue(), "Language", "french");
		E_PropertiesFile_Dispatcher_values.CAPITAL_CITY.setValue("Paris");
		testResult(E_PropertiesFile_Dispatcher_values.CAPITAL_CITY.getValue(), "City", "Paris");
		
		// Overwrite values in properties file
		E_PropertiesFile_Dispatcher_values.COUNTRY.setValue("England");
		E_PropertiesFile_Dispatcher_values.LANGUAGE.setValue("english");
		E_PropertiesFile_Dispatcher_values.CAPITAL_CITY.setValue("London");
		
		// Check count of each variable within properties file is 1
		testResult(Integer.toString(E_PropertiesFile_Dispatcher_values.COUNTRY.getValues().length), "Country", "1");
		testResult(Integer.toString(E_PropertiesFile_Dispatcher_values.LANGUAGE.getValues().length), "Language", "1");
		testResult(Integer.toString(E_PropertiesFile_Dispatcher_values.CAPITAL_CITY.getValues().length), "City", "1");
		
		// Check changed values of properties file
		testResult(E_PropertiesFile_Dispatcher_values.COUNTRY.getValue(), "Country", "England");
		testResult(E_PropertiesFile_Dispatcher_values.LANGUAGE.getValue(), "Language", "english");
		testResult(E_PropertiesFile_Dispatcher_values.CAPITAL_CITY.getValue(), "City", "London");
		
		// Delete the values from properties file
		E_PropertiesFile_Dispatcher_values.COUNTRY.delete();
		E_PropertiesFile_Dispatcher_values.LANGUAGE.delete();
		E_PropertiesFile_Dispatcher_values.CAPITAL_CITY.delete();

		// Check if default values defined in E_PropertiesFile_Dispatcher_values.java will be used again
		testResult(E_PropertiesFile_Dispatcher_values.COUNTRY.getValue(), "Country", "Germany");
		testResult(E_PropertiesFile_Dispatcher_values.LANGUAGE.getValue(), "Language", "german");
		testResult(E_PropertiesFile_Dispatcher_values.CAPITAL_CITY.getValue(), "CapitalCity", "Berlin");
		
//		BaseDispatcher.clearDataContainer();
	}

}
