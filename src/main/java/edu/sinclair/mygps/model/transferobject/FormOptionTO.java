package edu.sinclair.mygps.model.transferobject;

import java.util.UUID;

public class FormOptionTO {

	private UUID id;
	private String label;
	private String value;

	public FormOptionTO() {}

	public FormOptionTO(UUID id, String label, String value) {
		this.id = id;
		this.label = label;
		this.value = value;
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}

}
