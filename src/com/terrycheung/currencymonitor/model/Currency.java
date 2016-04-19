package com.terrycheung.currencymonitor.model;

//{
// "query": {
//  "count": 1,
//  "created": "2016-04-16T11:52:01Z",
//  "lang": "zh-TW",
//  "results": {
//   "rate": {
//    "id": "HKDUSD",
//    "Name": "HKD/USD",
//    "Rate": "0.1289",
//    "Date": "4/16/2016",
//    "Time": "1:35am",
//    "Ask": "0.1290",
//    "Bid": "0.1289"
//   }
//  }
// }
//}
public class Currency {
	private String name,date,time;
	private double previousRate=0.0000, rate=0.0000;
	
	public Currency() { }
	
	public double getPreviousRate() {
		return previousRate;
	}
	public void setPreviousRate(double previousRate) {
		this.previousRate = previousRate;
	}

	public double getRate() {
		return rate;
	}
	public void setRate(double rate) {
		this.rate = rate;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
	public String getTime() {
		return time;
	}
	public void setTime(String time) {
		this.time = time;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((date == null) ? 0 : date.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		long temp;
		temp = Double.doubleToLongBits(previousRate);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(rate);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((time == null) ? 0 : time.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Currency other = (Currency) obj;
		if (date == null) {
			if (other.date != null)
				return false;
		} else if (!date.equals(other.date))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (Double.doubleToLongBits(previousRate) != Double.doubleToLongBits(other.previousRate))
			return false;
		if (Double.doubleToLongBits(rate) != Double.doubleToLongBits(other.rate))
			return false;
		if (time == null) {
			if (other.time != null)
				return false;
		} else if (!time.equals(other.time))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Currency [name=" + name + ", date=" + date + ", time=" + time + ", previousRate=" + previousRate
				+ ", rate=" + rate + "]";
	}
}