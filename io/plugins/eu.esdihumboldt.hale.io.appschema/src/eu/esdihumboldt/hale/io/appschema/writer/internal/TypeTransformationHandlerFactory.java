package eu.esdihumboldt.hale.io.appschema.writer.internal;

import eu.esdihumboldt.hale.common.align.model.functions.JoinFunction;
import eu.esdihumboldt.hale.common.align.model.functions.RetypeFunction;

public class TypeTransformationHandlerFactory {

	private static TypeTransformationHandlerFactory instance;

	private TypeTransformationHandlerFactory() {

	}

	public static TypeTransformationHandlerFactory getInstance() {
		if (instance == null) {
			instance = new TypeTransformationHandlerFactory();
		}

		return instance;
	}

	public TypeTransformationHandler createTypeTransformationHandler(
			String typeTransformationIdentifier) throws UnsupportedTransformationException {
		if (typeTransformationIdentifier == null || typeTransformationIdentifier.trim().isEmpty()) {
			throw new IllegalArgumentException("typeTransformationIdentifier must be set");
		}

		if (typeTransformationIdentifier.equals(RetypeFunction.ID)) {
			return new RetypeHandler();
		}
		else if (typeTransformationIdentifier.equals(JoinFunction.ID)) {
			return new JoinHandler();
		}
		else {
			String errMsg = String.format("Unsupported type transformation %s",
					typeTransformationIdentifier);
			throw new UnsupportedTransformationException(errMsg, typeTransformationIdentifier);
		}
	}

}
