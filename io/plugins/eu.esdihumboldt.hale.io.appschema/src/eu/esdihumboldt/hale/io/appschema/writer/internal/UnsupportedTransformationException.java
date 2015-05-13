package eu.esdihumboldt.hale.io.appschema.writer.internal;

public class UnsupportedTransformationException extends Exception {

	private String transformationIdentifier;

	public UnsupportedTransformationException(String message) {
		super(message);
	}

	public UnsupportedTransformationException(String message, String transformationIdentifier) {
		super(message);
		this.transformationIdentifier = transformationIdentifier;
	}

	public String getTransformationIdentifier() {
		return transformationIdentifier;
	}

	public void setTransformationIdentifier(String transformationIdentifier) {
		this.transformationIdentifier = transformationIdentifier;
	}

}
