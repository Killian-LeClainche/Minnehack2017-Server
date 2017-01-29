package com.nebby.server;

import java.util.Date;

public class Medication {

		private String name;
		private String time;
		public long timer = 0;
		public boolean checked = false;
		
		public Medication() {
			name = null;
			time= null;
		}
		
		public Medication(String n, String t)
		{
			name = n;
			time = t;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getTime() {
			return time;
		}

		public void setTime(String date) {
			this.time = date;
		}
		
}