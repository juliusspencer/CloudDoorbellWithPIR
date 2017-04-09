package co.jsaltd.doorimage;

public class DoorbellEntry {

	Long timestamp;
	String image;

	public DoorbellEntry() {
	}

	public DoorbellEntry(Long timestamp, String image) {
		this.timestamp = timestamp;
		this.image = image;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public String getImage() {
		return image;
	}

}