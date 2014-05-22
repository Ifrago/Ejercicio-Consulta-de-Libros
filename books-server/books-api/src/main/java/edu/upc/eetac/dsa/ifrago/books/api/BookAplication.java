package edu.upc.eetac.dsa.ifrago.books.api;

import org.glassfish.jersey.linking.DeclarativeLinkingFeature;
import org.glassfish.jersey.server.ResourceConfig;

public class BookAplication extends ResourceConfig{
	public BookAplication() {
		super();
		register(DeclarativeLinkingFeature.class);
	}

}
