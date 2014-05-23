package edu.upc.eetac.dsa.ifrago.books.api.model;

import java.util.Date;
import java.util.List;

import javax.ws.rs.core.Link;

import org.glassfish.jersey.linking.Binding;
import org.glassfish.jersey.linking.InjectLink;
import org.glassfish.jersey.linking.InjectLinks;
import org.glassfish.jersey.linking.InjectLink.Style;

import edu.upc.eetac.dsa.ifrago.books.api.BooksResource;
import edu.upc.eetac.dsa.ifrago.books.api.MediaType;

public class Reviews {
	@InjectLinks({
		@InjectLink(resource = BooksResource.class, rel = "create-review", title = "Book", type = MediaType.BOOKS_API_BOOKS, method = "createReview"),
		@InjectLink(resource = BooksResource.class, style = Style.ABSOLUTE, rel = "create-review", title = "Book", type = MediaType.BOOKS_API_BOOKS, method = "createReview"),
		@InjectLink(resource = BooksResource.class, style = Style.ABSOLUTE, rel = "update-review", title = "updateBook", type = MediaType.BOOKS_API_BOOKS, method = "updateReview", bindings ={ @Binding(name = "reviewid", value = "${instance.reviewid}"),@Binding(name = "bookid", value = "${instance.bookid}")}),
		@InjectLink(resource = BooksResource.class, style = Style.ABSOLUTE, rel = "delete-review", title = "deleteBook", type = MediaType.BOOKS_API_BOOKS, method = "deleteReview", bindings ={ @Binding(name = "reviewid", value = "${instance.reviewid}"),@Binding(name = "bookid", value = "${instance.bookid}")})

	})
	private List<Link> links;
	//condition="${resource.registered}",
	int reviewid= 0;
	String username=null;
	Date dateupdate=null;
	String text=null;
	int bookid=0;
	

	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public Date getDateupdate() {
		return dateupdate;
	}
	public void setDateupdate(Date dateupdate) {
		this.dateupdate = dateupdate;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public int getBookid() {
		return bookid;
	}
	public void setBookid(int bookid) {
		this.bookid = bookid;
	}
	public int getReviewid() {
		return reviewid;
	}
	public void setReviewid(int reviewid) {
		this.reviewid = reviewid;
	}
	public List<Link> getLinks() {
		return links;
	}
	public void setLinks(List<Link> links) {
		this.links = links;
	}
	
	

}
