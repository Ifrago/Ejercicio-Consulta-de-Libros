package edu.upc.eetac.dsa.ifrago.books.api.model;

import java.util.List;

import javax.ws.rs.core.Link;

import org.glassfish.jersey.linking.InjectLink;
import org.glassfish.jersey.linking.InjectLinks;
import org.glassfish.jersey.linking.InjectLink.Style;

import edu.upc.eetac.dsa.ifrago.books.api.BookRootAPIResource;
import edu.upc.eetac.dsa.ifrago.books.api.BooksResource;
import edu.upc.eetac.dsa.ifrago.books.api.MediaType;

public class BookRootAPI {

	@InjectLinks({
		@InjectLink(resource = BookRootAPIResource.class, style = Style.ABSOLUTE, rel = "self bookmark home", title = "Book Root API", method = "getBookAPI"),
		@InjectLink(resource = BooksResource.class, style = Style.ABSOLUTE, rel = "books", title = "Latest books", type = MediaType.BOOKS_API_BOOKS_COLLECTION) 
	})
	private List<Link> links;
 
	public List<Link> getLinks() {
		return links;
	}
 
	public void setLinks(List<Link> links) {
		this.links = links;
	}
	
	
}
