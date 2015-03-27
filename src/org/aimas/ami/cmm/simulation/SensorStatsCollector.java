package org.aimas.ami.cmm.simulation;

import java.util.List;

public interface SensorStatsCollector {
	public void markSensing(long timestamp, String sensedAssertionType);
	
	public void markSensingUpdateMessage(long timestamp, int messageId, String sensedAssertionType);
	
	public SensorStats collectSensingStats();
	
	public static class SensorStats {
		List<SensingEvent> sensingEventHistory;
		List<SensingMessageEvent> sensingMessageEventHistory;
		
		public SensorStats(List<SensingEvent> sensingEventHistory,
                List<SensingMessageEvent> sensingMessageEventHistory) {
	        this.sensingEventHistory = sensingEventHistory;
	        this.sensingMessageEventHistory = sensingMessageEventHistory;
        }

		public List<SensingEvent> getSensingEventHistory() {
			return sensingEventHistory;
		}
		
		public List<SensingMessageEvent> getSensingMessageEventHistory() {
			return sensingMessageEventHistory;
		}
	}
	
	public static class SensingEvent implements Comparable<SensingEvent> {
		
		long timestamp;
		String sensedAssertionType;
		
		public SensingEvent(long timestamp, String sensedAssertionType) {
	        this.timestamp = timestamp;
	        this.sensedAssertionType = sensedAssertionType;
        }

		public long getTimestamp() {
			return timestamp;
		}

		public String getSensedAssertionType() {
			return sensedAssertionType;
		}

		@Override
        public int compareTo(SensingEvent o) {
	        if (timestamp < o.getTimestamp()) {
	        	return -1;
	        }
	        else if (timestamp > o.getTimestamp()) {
	        	return 1;
	        }
	        
	        return 0;
        }
	}
	
	public static class SensingMessageEvent implements Comparable<SensingMessageEvent> {
		
		long timestamp;
		String sensedAssertionType;
		int messageId;
		
		public SensingMessageEvent(long timestamp, String sensedAssertionType,
                int messageId) {
	        this.timestamp = timestamp;
	        this.sensedAssertionType = sensedAssertionType;
	        this.messageId = messageId;
        }

		public long getTimestamp() {
			return timestamp;
		}

		public String getSensedAssertionType() {
			return sensedAssertionType;
		}

		public int getMessageId() {
			return messageId;
		}

		@Override
        public int compareTo(SensingMessageEvent o) {
			if (timestamp < o.getTimestamp()) {
	        	return -1;
	        }
	        else if (timestamp > o.getTimestamp()) {
	        	return 1;
	        }
	        
	        return 0;
        }
	}
}
