package com.terrycheung.currencymonitor.client;

import com.terrycheung.currencymonitor.model.Currency;
//import com.terrycheung.currencymonitor.shared.FieldVerifier;
import java.util.Date;
import java.util.Iterator;
import java.util.HashMap;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
//import com.google.gwt.http.client.UrlBuilder;
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
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.datepicker.client.DatePicker;

public class CurrencyMonitor implements EntryPoint {
	private static final int REFRESH_INTERVAL = 2000; // ms
	private VerticalPanel mainPanel = new VerticalPanel();
	private FlexTable currenciesFlexTable = new FlexTable();
	private HorizontalPanel addPanel = new HorizontalPanel(),datePanel= new HorizontalPanel();
	private TextBox newSymbolTextBox = new TextBox(), startDateText=new TextBox(),endDateText=new TextBox() ;
	private Button addCurrencyButton = new Button("Add");
	private Label lastUpdatedLabel = new Label();
	private HashMap<String, Currency> dollarMap = new HashMap<String, Currency>();
	private static final String JSON_URL1 = "https://query.yahooapis.com/v1/public/yql?q=";
	private static final String JSON_URL2 = "&format=json&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys";
	private static final String YQL_Hist = "SELECT * FROM yahoo.finance.historicaldata WHERE symbol=\"$targetDollar=X\" AND startDate=\"$startDate\" AND endDate=\"$endDate\"";
	private static final String YQL_ExchangeRate = "SELECT * FROM yahoo.finance.xchange WHERE pair in ($dollars)";
	private JSONValue HJson=null;
	private String baseDollar = "HKD";
	private Label errorMsgLabel = new Label();
	private String tmpStr=null, removedSymbol;
	private boolean lock=false, addClicked=false;
	// private static final String JSON_URL = GWT.getModuleBaseURL() +
	// "stockPrices?q=";

	public static native void showCurrencyHistory(String value) /*-{
		$doc.drawChart(value);
	}-*/;
	public void onModuleLoad() {
		// Create table for currency data.
		currenciesFlexTable.setText(0, 0, "Symbol");
		currenciesFlexTable.setText(0, 1, "Price");
		currenciesFlexTable.setText(0, 2, "Change");
		currenciesFlexTable.setText(0, 3, "View");
		currenciesFlexTable.setText(0, 4, "Remove");
		// Add styles to elements in the currency list table.
		currenciesFlexTable.setCellPadding(6);
		currenciesFlexTable.getRowFormatter().addStyleName(0, "monitorListHeader");
		currenciesFlexTable.addStyleName("monitorList");
		currenciesFlexTable.getCellFormatter().addStyleName(0, 1, "monitorListNumericColumn");
		currenciesFlexTable.getCellFormatter().addStyleName(0, 2, "monitorListNumericColumn");
		currenciesFlexTable.getCellFormatter().addStyleName(0, 3, "monitorListRemoveColumn");
		currenciesFlexTable.getCellFormatter().addStyleName(0, 4, "monitorListRemoveColumn");
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
		
		Label startDateLabel = new Label("Start Date: ");
		startDateText.setReadOnly(true);
        final PopupPanel startDatePopup=new PopupPanel(true);
        DatePicker startDatePicker=new DatePicker();
        startDatePicker.addValueChangeHandler(new ValueChangeHandler<Date>() {
            public void onValueChange(ValueChangeEvent<Date> event) {
                Date date=event.getValue();
                startDateText.setText(DateTimeFormat.getFormat("yyyy-MM-dd").format(date));
                startDatePopup.hide();
            }
        });
        startDatePopup.setWidget(startDatePicker);
        datePanel.add(startDateLabel);
        datePanel.add(startDateText);
        datePanel.add(startDatePopup);
        Label endDateLabel = new Label("End Date: ");
		endDateText.setReadOnly(true);
		final PopupPanel endDatePopup=new PopupPanel(true);
        DatePicker endDatePicker=new DatePicker();
        endDatePicker.addValueChangeHandler(new ValueChangeHandler<Date>() {
            public void onValueChange(ValueChangeEvent<Date> event) {
                Date date=event.getValue();
                endDateText.setText(DateTimeFormat.getFormat("yyyy-MM-dd").format(date));
                endDatePopup.hide();
            }
        });
        endDatePopup.setWidget(endDatePicker);
        datePanel.add(endDateLabel);
        datePanel.add(endDateText);
        datePanel.add(endDatePopup);
        mainPanel.add(datePanel);
		// Associate the Main panel with the HTML host page.
		RootPanel.get("currencyMonitorList").add(mainPanel);

		// Move cursor focus to the input box.
		newSymbolTextBox.setFocus(true);
		newSymbolTextBox.setMaxLength(5);
		// Setup timer to refresh list automatically.
		Timer refreshTimer = new Timer() {
			@Override
			public void run() { refreshMonitorList(); }
		};
		refreshTimer.scheduleRepeating(REFRESH_INTERVAL);
		
		// Listen for mouse events on the Add button.
		addCurrencyButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) { if(!addClicked) addCurrency(); }
		});
		// Listen for keyboard events in the input box.
		newSymbolTextBox.addKeyDownHandler(new KeyDownHandler() {
			public void onKeyDown(KeyDownEvent event) {
				if(!addClicked && event.getNativeKeyCode() == KeyCodes.KEY_ENTER) addCurrency();
			}
		});
	}
	private void addCurrency() {
		addClicked=true; if(lock) return;
		tmpStr = newSymbolTextBox.getText();
		if(tmpStr==null) return;
		final String symbol = tmpStr.toUpperCase().trim();
		newSymbolTextBox.setFocus(true);

		if(!symbol.matches("[a-zA-Z]{1,5}") || symbol.equals(baseDollar)) {
			Window.alert("'" + symbol + "' is not a valid symbol.");
			newSymbolTextBox.selectAll(); return;
		}
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
		currenciesFlexTable.getCellFormatter().addStyleName(row, 4, "monitorListRemoveColumn");
		// Get the currency price.
		refreshMonitorList();
	}
	synchronized private void refreshMonitorList() {
		String url = JSON_URL1.concat(YQL_ExchangeRate).concat(JSON_URL2);
		StringBuilder strBuidler = new StringBuilder();
		String[] symbols = new String[dollarMap.size()];
		Iterator<String> keys = dollarMap.keySet().iterator();
		int x = 0;
		while(keys.hasNext()) {
			symbols[x] = keys.next();
			strBuidler.append("\"").append(baseDollar).append(symbols[x]).append("\"").append(",");
			x++;
		}
		// dollarList.forEach(item -> { strBuidler.append("\"").append(baseDollar).append(item).append("\"").append(","); });
		strBuidler.delete(strBuidler.length() - 1, strBuidler.length());
//		Window.alert(url);
		extractJsonData(false,url.replace("$dollars", tmpStr));
		strBuidler.delete(0, strBuidler.length()); strBuidler=null;
		DateTimeFormat dateFormat = DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_TIME_MEDIUM);
		lastUpdatedLabel.setText("Last update : " + dateFormat.format(new Date()));
		errorMsgLabel.setVisible(false); addClicked=false;
	}
	private void setCurrency(int row, JSONObject obj){
		if(dollarMap.size()==0 && dollarMap.size()!=(currenciesFlexTable.getRowCount()-1)) return;
		Currency currency=null;
		try{
			tmpStr=obj.get("id").isString().stringValue().replace(baseDollar, "").toUpperCase().trim();
			currency = dollarMap.get(tmpStr);
			currency.setRate(Double.parseDouble(obj.get("Rate").isString().stringValue()));
			currency.setDate(obj.get("Date").isString().stringValue());
			currency.setTime(obj.get("Time").isString().stringValue());
			setRateChangeEffect(row,currency);
		} catch (Exception e) {
			if(removedSymbol==null){dollarMap.remove(tmpStr); currenciesFlexTable.removeRow(row);}
			removedSymbol=null;
			Window.alert("No data for " + tmpStr + " OR Web Server in busy.");
		}
	}
	private void setRateChangeEffect(int row, Currency currency){
		// Format the data in the Price and Change fields.
		double change = currency.getRate() - currency.getPreviousRate();
		double changePercentage = 100.00 * (change / currency.getRate());
		NumberFormat changeFormat = NumberFormat.getFormat("+#,##0.0000;-#,##0.0000");
		// Populate the Price and Change fields with new data.
		currenciesFlexTable.setText(row, 1, NumberFormat.getFormat("#,##0.0000").format(currency.getRate()));
		Label changePercentLabel = new Label("0.0000 (0.00%)");
		// Change the color of text in the Change field based on its value.
		String changeStyleName = "noChange";
		//Should not use change!=100.00
		if(currency.getPreviousRate()!=0 && currency.getRate()!=currency.getPreviousRate()){
			changeStyleName = ((change<-0.01f)?"negativeChange":"positiveChange");
			changePercentLabel.setText(changeFormat.format(change) + " (" + changeFormat.format(changePercentage) + "%)");
		}
		changePercentLabel.setStyleName(changeStyleName);
		currenciesFlexTable.setWidget(row, 2, changePercentLabel);
		appendButtons(row);
		currency.setPreviousRate(currency.getRate());
	}
	synchronized private void extractJsonData(final boolean isHistorical,String url){
		lock = true;
		RequestBuilder reqBuilder = new RequestBuilder(RequestBuilder.GET, url);
		reqBuilder.setTimeoutMillis(REFRESH_INTERVAL+2000);
		try {
			Request request = reqBuilder.sendRequest(null, new RequestCallback() {
				public void onError(Request request, Throwable exception) {
					dollarMap.clear(); lock = false; displayError("Couldn't retrieve JSON");
				}
				public void onResponseReceived(Request request, Response response) {
					if(dollarMap.size()==0 && dollarMap.size()!=(currenciesFlexTable.getRowCount()-1)) return;
					if (200 == response.getStatusCode()) {
						JSONObject[] queryObj = new JSONObject[4];
						try{
							JSONValue value = JSONParser.parseStrict(response.getText());
							queryObj[0] = value.isObject();
							queryObj[1] = queryObj[0].get("query").isObject();
							queryObj[2] = queryObj[1].get("results").isObject();
							JSONArray queryArray=null;
							if(isHistorical){ HJson=queryObj[2].get("quote"); showCurrencyHistory(HJson.toString());}
							else{
								value=queryObj[2].get("rate");
								if((queryObj[3]=value.isObject()) != null) {
									setCurrency(1, queryObj[3]);
								} else if((queryArray=value.isArray()) != null) {
									for(int i=0; i<=queryArray.size()-1; i++) {
										if((queryObj[3] = queryArray.get(i).isObject()) != null) setCurrency(i+1, queryObj[3]);
									}
								}
							}
						}catch(Exception e){ dollarMap.clear(); displayError("Data source is corrupted. "+e.getMessage()); }
					} else { displayError("Couldn't retrieve data source (" + response.getStatusText() + ")"); }
					lock = false;
				}
			});
		} catch (Exception e) { dollarMap.clear(); lock = false; displayError("Couldn't retrieve JSON");}
	}
	private void appendButtons(final int row){
//		Add a button to remove this currency from the table.
		Button removeCurrencyButton = new Button("x"), chartButton = new Button("View Chart");
		removeCurrencyButton.addStyleDependentName("remove");
		removeCurrencyButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				if(lock || dollarMap.size()==0) return;
				try{
					String targetSymbol=currenciesFlexTable.getFlexCellFormatter().getElement(row,0).getInnerText();
					dollarMap.remove(targetSymbol.trim());
					currenciesFlexTable.removeRow(row);
					removedSymbol=targetSymbol;
				}catch(Exception e){displayError(e.getMessage());}
			}
		});
		chartButton.addStyleDependentName("chart");
		chartButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				if(lock) return;
//				UrlBuilder builder = Window.Location.createUrlBuilder();
//				builder.setHash("ddasd"); builder.setParameter("currency", "");
//				Window.Location.assign(builder.buildString());
//	            if(cell.getCellIndex()==0) Window.open("CurrencyHistory.html?currency="+currenciesFlexTable.getFlexCellFormatter().getElement(cell.getRowIndex(),0).getInnerText(), "_blank", "");
				String symbol=null;
				try{
					symbol=currenciesFlexTable.getFlexCellFormatter().getElement(row,0).getInnerText();
					if(!symbol.equals("") && !symbol.equals("USD")){
						if(startDateText.getText()!=null && endDateText.getText()!=null && !startDateText.getText().equals("") && !endDateText.getText().equals("")){
							String url = JSON_URL1.concat(YQL_Hist).concat(JSON_URL2);
							url = url.replace("$targetDollar", symbol);
							url = url.replace("$startDate",startDateText.getText());
							url = url.replace("$endDate",endDateText.getText());
							extractJsonData(true,url);
						} else { Window.alert("Please select the date range."); }
					}
				}catch(Exception e){displayError(e.getMessage());}
			}
		});
		currenciesFlexTable.setWidget(row, 3, chartButton);
		currenciesFlexTable.setWidget(row, 4, removeCurrencyButton);
	}
	private void displayError(String error) {
		errorMsgLabel.setText("Error: " + error);
		errorMsgLabel.setVisible(true);
	}
}
