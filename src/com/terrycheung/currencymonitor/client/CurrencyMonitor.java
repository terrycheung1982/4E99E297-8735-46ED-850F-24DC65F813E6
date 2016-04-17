package com.terrycheung.currencymonitor.client;

import com.terrycheung.currencymonitor.model.Currency;
import com.terrycheung.currencymonitor.shared.FieldVerifier;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTMLTable.Cell;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class CurrencyMonitor implements EntryPoint {
	private static final int REFRESH_INTERVAL = 10000; // ms
	private VerticalPanel mainPanel = new VerticalPanel();
	private FlexTable currenciesFlexTable = new FlexTable();
	private HorizontalPanel addPanel = new HorizontalPanel();
	private TextBox newSymbolTextBox = new TextBox();
	private Button addCurrencyButton = new Button("Add");
	private Label lastUpdatedLabel = new Label();
	private HashMap<String, Currency> dollarMap = new HashMap<String, Currency>();
	private static final String JSON_URL1 = "https://query.yahooapis.com/v1/public/yql?q=";
	private static final String JSON_URL2 = "&format=json&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys";
	private static String sql_hist = "SELECT * FROM yahoo.finance.historicaldata  WHERE symbol=\"$targetDollar=X\" AND startDate=\"$startDate\"  AND endDate=\"$endDate\"";
	private static final String sql_exchangerate = "SELECT * FROM yahoo.finance.xchange WHERE pair in ($dollars)";

	private String baseDollar = "HKD";
	private StringBuilder strBuidler = new StringBuilder();
	private Label errorMsgLabel = new Label();
	private String tmpStr=null;
	// private static final String JSON_URL = GWT.getModuleBaseURL() +
	// "stockPrices?q=";

	/**
	 * Entry point method.
	 */
	public void onModuleLoad() {
		// Create table for currency data.
		currenciesFlexTable.setText(0, 0, "Symbol");
		currenciesFlexTable.setText(0, 1, "Price");
		currenciesFlexTable.setText(0, 2, "Change");
		currenciesFlexTable.setText(0, 3, "Remove");
		// Add styles to elements in the currency list table.
		currenciesFlexTable.setCellPadding(6);
		currenciesFlexTable.getRowFormatter().addStyleName(0, "monitorListHeader");
		currenciesFlexTable.addStyleName("monitorList");
		currenciesFlexTable.getCellFormatter().addStyleName(0, 1, "monitorListNumericColumn");
		currenciesFlexTable.getCellFormatter().addStyleName(0, 2, "monitorListNumericColumn");
		currenciesFlexTable.getCellFormatter().addStyleName(0, 3, "monitorListRemoveColumn");
		// Assemble Add currency.
		addPanel.add(newSymbolTextBox);
		addPanel.add(addCurrencyButton);
		addPanel.addStyleName("addPanel");
		// Assemble Main panel.
		errorMsgLabel.setStyleName("errorMessage");
		errorMsgLabel.setVisible(false);
		mainPanel.add(errorMsgLabel);
		mainPanel.add(currenciesFlexTable);
		mainPanel.add(addPanel);
		mainPanel.add(lastUpdatedLabel);
		// Associate the Main panel with the HTML host page.
		RootPanel.get("currencyMonitorList").add(mainPanel);

		// Move cursor focus to the input box.
		newSymbolTextBox.setFocus(true);
		newSymbolTextBox.setMaxLength(5);
		// Setup timer to refresh list automatically.
		Timer refreshTimer = new Timer() {
			@Override
			public void run() {
				refreshMonitorList();
			}
		};
		refreshTimer.scheduleRepeating(REFRESH_INTERVAL);
		currenciesFlexTable.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				Cell cell = currenciesFlexTable.getCellForEvent(event);
	            if(cell.getCellIndex()==0) Window.open("CurrencyHistory.html?currency="+currenciesFlexTable.getFlexCellFormatter().getElement(cell.getRowIndex(),0).getInnerText(), "_blank", "");
	        }
	    });
		// Listen for mouse events on the Add button.
		addCurrencyButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) { addCurrency(); }
		});
		// Listen for keyboard events in the input box.
		newSymbolTextBox.addKeyDownHandler(new KeyDownHandler() {
			public void onKeyDown(KeyDownEvent event) {
				if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER)
					addCurrency();
			}
		});
	}

	private void addCurrency() {
		tmpStr = newSymbolTextBox.getText();
		if(tmpStr==null) return;
		final String symbol = tmpStr.toUpperCase().trim();
		newSymbolTextBox.setFocus(true);

		if(!symbol.matches("[a-zA-Z]{1,5}") || symbol.equals(baseDollar)) {
			Window.alert("'" + symbol + "' is not a valid symbol.");
			newSymbolTextBox.selectAll(); return;
		}
		newSymbolTextBox.setText("");
		// Add the currency to the table.
		if(dollarMap.containsKey(symbol)) {
			Window.alert(symbol+" exists."); return ;
		}
		int row = currenciesFlexTable.getRowCount();
		dollarMap.put(symbol, new Currency());
		currenciesFlexTable.setText(row, 0, symbol);
		currenciesFlexTable.getFlexCellFormatter().getElement(row,0 ).getStyle().setCursor(Cursor.POINTER);
		currenciesFlexTable.getCellFormatter().addStyleName(row, 1, "monitorListNumericColumn");
		currenciesFlexTable.getCellFormatter().addStyleName(row, 2, "monitorListNumericColumn");
		currenciesFlexTable.getCellFormatter().addStyleName(row, 3, "monitorListRemoveColumn");
		// Add a button to remove this currency from the table.
		Button removeCurrencyButton = new Button("x");
		removeCurrencyButton.addStyleDependentName("remove");
		removeCurrencyButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				Iterator<String> keys = dollarMap.keySet().iterator();
				int x = 0;
				while(keys.hasNext()) {
					x++; if(symbol.equals(keys.next())) break;
				}
				dollarMap.remove(symbol);
				currenciesFlexTable.removeRow(x);
			}
		});
		currenciesFlexTable.setWidget(row, 3, removeCurrencyButton);
		// Get the currency price.
		refreshMonitorList();
	}

	private void refreshMonitorList() {
		if(dollarMap.size()==0) return;
		String url = JSON_URL1.concat(sql_exchangerate).concat(JSON_URL2);
		strBuidler.delete(0, strBuidler.length());
		Iterator<String> keys = dollarMap.keySet().iterator();
		String[] symbols = new String[dollarMap.size()];
		int x = 0;
		while(keys.hasNext()) {
			symbols[x] = keys.next();
			strBuidler.append("\"").append(baseDollar).append(symbols[x]).append("\"").append(",");
			x++;
		}
		// dollarList.forEach(item -> {
		// strBuidler.append("\"").append(baseDollar).append(item).append("\"").append(",");
		// });
		strBuidler.delete(strBuidler.length() - 1, strBuidler.length());
		url = url.replace("$dollars", strBuidler.toString());
//		Window.alert(url);
		RequestBuilder reqBuilder = new RequestBuilder(RequestBuilder.GET, url);
		// CurrencyPrice[] prices=null;
		try {
			Request request = reqBuilder.sendRequest(null, new RequestCallback() {
				public void onError(Request request, Throwable exception) {
					displayError("Couldn't retrieve JSON");
				}

				public void onResponseReceived(Request request, Response response) {
					if (200 == response.getStatusCode()) {
						JSONObject[] queryObj = new JSONObject[4];
						JSONValue value = JSONParser.parseStrict(response.getText());
						queryObj[0] = value.isObject();
						queryObj[1] = queryObj[0].get("query").isObject();
						queryObj[2] = queryObj[1].get("results").isObject();
						value = queryObj[2].get("rate");
						JSONArray queryObjs=null;
						if ((queryObj[3]=value.isObject()) != null) {
							setCurrency(1, queryObj[3]);
						} else if((queryObjs=value.isArray()) != null) {
							for(int i=0; i<=queryObjs.size()-1; i++) {
								if((queryObj[3] = queryObjs.get(i).isObject()) != null) setCurrency(i+1, queryObj[3]);
							}
						}
						// updateTable(JsonUtils.<JsArray<CurrencyPrice>>
						// safeEval(response.getText()));
					} else {
						displayError("Couldn't retrieve JSON (" + response.getStatusText() + ")");
					}
				}
			});
		} catch (RequestException re) {
			displayError("Couldn't retrieve JSON");
		} catch (Exception e) {
			displayError(e.toString());
		}
		DateTimeFormat dateFormat = DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_TIME_MEDIUM);
		lastUpdatedLabel.setText("Last update : " + dateFormat.format(new Date()));
		errorMsgLabel.setVisible(false);
	}
	private void setCurrency(int row, JSONObject obj){
		Currency currency=null;
		try{ 
			tmpStr=obj.get("id").isString().stringValue().replace(baseDollar, "").toUpperCase().trim();
			currency = dollarMap.get(tmpStr);
			currency.setRate(Double.parseDouble(obj.get("Rate").isString().stringValue()));
			currency.setDate(obj.get("Date").isString().stringValue());
			currency.setTime(obj.get("Time").isString().stringValue());
			setRateChangeEffect(row,currency);
		} catch (Exception e) {
			dollarMap.remove(tmpStr); currenciesFlexTable.removeRow(row);
			Window.alert("No data for " + tmpStr);
		}
	}
	private void setRateChangeEffect(int row, Currency currency){
		// Format the data in the Price and Change fields.
		double change = currency.getRate() - currency.getPreviousRate();
		double changePercentage = 100.00 * change / currency.getRate();
		NumberFormat changeFormat = NumberFormat.getFormat("+#,##0.00;-#,##0.00");
		// Populate the Price and Change fields with new data.
		currenciesFlexTable.setText(row, 1, NumberFormat.getFormat("#,##0.0000").format(currency.getRate()));
		Label changePercentLabel = new Label("0.00");
		// Change the color of text in the Change field based on its value.
		String changeStyleName = "noChange";
		if(change!=1.00){
			changeStyleName = ((change<-0.01f)?"negativeChange":"positiveChange");
			changePercentLabel.setText(changeFormat.format(change) + " (" + changeFormat.format(changePercentage) + "%)");
		}
		changePercentLabel.setStyleName(changeStyleName);
		currenciesFlexTable.setWidget(row, 2, changePercentLabel);
	}
	private void displayError(String error) {
		errorMsgLabel.setText("Error: " + error);
		errorMsgLabel.setVisible(true);
	}
}
