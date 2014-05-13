package edu.upc.eetac.dsa.ifrago.books.api;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import edu.upc.eetac.dsa.ifrago.books.api.DataSourceSPA;
import edu.upc.eetac.dsa.ifrago.books.api.MediaType;
import edu.upc.eetac.dsa.ifrago.books.api.model.Books;
import edu.upc.eetac.dsa.ifrago.books.api.model.Reviews;

@Path("/books")
public class BooksResource {
	private DataSource ds = DataSourceSPA.getInstance().getDataSource();
	@Context
	SecurityContext security;

	@GET
	public String getBooks() {
		return "getBooks()";
	}

	@POST
	public String createBook() {
		if (!security.isUserInRole("admin"))
			throw new ForbiddenException("You are not allowed to create a book");
		return "createBook()";
	}

	@GET
	@Path("/{bookid}")
	@Produces(MediaType.BOOKS_API_BOOKS)
	public Response getBook(@PathParam("bookid") String bookid,@Context Request request) {
		
		//Creamos CacheControl
		CacheControl cc= new CacheControl();
		
		//Sacamos un book de la base de datos
		Books book = getBookFromDatabase(bookid);
		
		//Calculamos ETag de la ultima modificación de la reseña
		
		String s= book.getReviews()+book.getAuthor()+book.getEdition()+"21";
		
		System.out.println("Construimos el string para el hash");
		
		EntityTag eTag = new EntityTag(Long.toString(s.hashCode()));
		
		System.out.println("Ya hemos hecho el hash");
		
		//Comparamos el eTag creado con el que viene de la peticiOn HTTP
		Response.ResponseBuilder rb = request.evaluatePreconditions(eTag);// comparamos
		
		System.out.println("Ya hemos hecho el eTag");
		if (rb != null) {// Si el resultado no es nulo, significa que no ha sido modificado el contenido ( o es la 1º vez )
				return rb.cacheControl(cc).tag(eTag).build();
		}

		System.out.println("Ya hemos hmirado el cache");
		
		// Si es nulo construimos la respuesta de cero.
		rb = Response.ok(book).cacheControl(cc).tag(eTag);
		System.out.println("Ya hemos hmirado el cache2");
		
		return rb.build();
		
	}
	private Books getBookFromDatabase(String bookid){
		
		Books book=new Books();
		
		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}
		
		PreparedStatement stmt=null;
		PreparedStatement stmtr=null;
		
		try{
			stmt=conn.prepareStatement(buildGetBookByIdQuery());
			stmt.setInt(1, Integer.valueOf(bookid));
			ResultSet rs = stmt.executeQuery();
			
			if(rs.next()) {
				book.setId(rs.getInt("bookid"));
				book.setTitle(rs.getString("title"));
				book.setAuthor(rs.getString("author"));
				book.setLanguage(rs.getString("language"));
				book.setEdition(rs.getString("edition"));
				book.setEditiondate(rs.getDate("editiondate"));
				book.setPrintdate(rs.getDate("printdate"));
				book.setEditorial(rs.getString("editorial"));									
			}else{
				throw new NotFoundException("There's no sting with stingid ="
						+ bookid);
			}
			
			//Cogemos las reviews de un libro
			stmtr=conn.prepareStatement(buildGetReviewBookByIdQuery());
			stmtr.setInt(1, Integer.valueOf(bookid));
			ResultSet rsr = stmtr.executeQuery();
			
			if(rsr.next()) {							
				Reviews review = new Reviews();
				review.setDateupdate(rsr.getDate("dateupdate"));
				review.setText(rsr.getString("text"));
				review.setUsername(rsr.getString("username"));
				
				book.addReviews(review);
				
					
			}/*else{
				throw new NotFoundException("There's no sting with stingid ="
						+ bookid);
			}*/
			
			
			
		}catch (SQLException e) {
			throw new ServerErrorException(e.getMessage(),
					Response.Status.INTERNAL_SERVER_ERROR);
		} finally {
			try {
				if (stmt != null)
					stmt.close();
				conn.close();
			} catch (SQLException e) {
			}
		}
		

		
		return book;
	}
	
	private String buildGetReviewBookByIdQuery() {
		return "select username, text, dateupdate from reviews  where bookid=?;";
	}

	private String buildGetBookByIdQuery() {
		return "select * from books  where  bookid=?;";
	}

	@POST
	@Path("/{bookid}/reviews")
	public String createReview(@PathParam("bookid") String bookid) {
		if (!security.isUserInRole("registered"))
			throw new ForbiddenException("You are not allowed to create reviews for a book");
		return "create review for bookid = " + bookid;
	}
}
