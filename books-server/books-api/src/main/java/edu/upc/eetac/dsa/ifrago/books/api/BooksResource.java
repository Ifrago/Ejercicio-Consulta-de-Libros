package edu.upc.eetac.dsa.ifrago.books.api;


import java.awt.print.Book;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import javax.sql.DataSource;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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
import edu.upc.eetac.dsa.ifrago.books.api.model.BooksCollection;
import edu.upc.eetac.dsa.ifrago.books.api.model.Reviews;

@Path("/books")
public class BooksResource {
	private DataSource ds = DataSourceSPA.getInstance().getDataSource();
	@Context
	SecurityContext security;

	@GET
	@Produces(MediaType.BOOKS_API_BOOKS_COLLECTION)
	public BooksCollection getBooks(@QueryParam("length") int length, @QueryParam("after") int after) {
		
		BooksCollection books = new BooksCollection ();
		
		Connection conn = null;
		try {
			conn = ds.getConnection();// Conectamos con la base de datos
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}
		
		PreparedStatement stmt=null;
		
		try{
			boolean updateFromLast= after>0;
			stmt = conn.prepareStatement(buildGetBooksQuery(updateFromLast));// Para preparar la query con el metodo buildGetStingsQuery ( metodo de abajo )
			if (updateFromLast) {
				if(length==0){
					stmt.setInt(1, after);
					stmt.setInt(2,5);
				}else{
					stmt.setInt(1, after);
					stmt.setInt(2,length);
				}
			}else{
				
				if(length==0)
					stmt.setInt(1,5);
				else
					stmt.setInt(1,length);
			}
			
			ResultSet rs = stmt.executeQuery();
			while (rs.next()){
				Books book = new Books();
				
				book.setId(rs.getInt("bookid"));
				book.setTitle(rs.getString("title"));
				book.setAuthor(rs.getString("author"));
				book.setLanguage(rs.getString("language"));
				book.setEdition(rs.getString("edition"));
				book.setEditiondate(rs.getDate("editiondate"));
				book.setPrintdate(rs.getDate("printdate"));
				book.setEditorial(rs.getString("editorial"));
				
				//Nos encargamos de las reviews de cada libro
				PreparedStatement stmtr=null;
				stmtr=conn.prepareStatement(buildGetReviewBookByIdQuery());
				stmtr.setInt(1, book.getId());
				
				ResultSet rsr = stmtr.executeQuery();
				
				while(rsr.next()) {							
					Reviews review = new Reviews();
					review.setDateupdate(rsr.getDate("dateupdate"));
					review.setText(rsr.getString("text"));
					review.setUsername(rsr.getString("username"));
					review.setBookid(rsr.getInt("bookid"));
					review.setReviewsid(rsr.getInt("reviewsid"));
					
					book.addReviews(review);
			
				}
				
				books.addBook(book);//Añadimos toda la info dellibro ( inclusive sus reviews ).
							
			}
			
		} catch (SQLException e) {
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

		return books;
	}

	private String buildGetBooksQuery(boolean updateFromLast) {
		
		if(updateFromLast)
			return "select * from books where bookid>? limit ?;";
		else
			return "select *from books limit ?;";
	}

	@POST
	@Consumes(MediaType.BOOKS_API_BOOKS)
	@Produces(MediaType.BOOKS_API_BOOKS)
	public Books createBook(Books book) {//Dates can are null
		if (!security.isUserInRole("admin"))
			throw new ForbiddenException("You are not allowed to create a book");
		
		ValidateBook(book);
		
		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}
		
		PreparedStatement stmt = null;
		
		try{
			String sql= buildInsertBook();
			stmt=conn.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);
			
			stmt.setString(1, book.getTitle());
			stmt.setString(2, book.getAuthor());
			stmt.setString(3, book.getLanguage());
			stmt.setString(4, book.getEdition());
			//stmt.setDate(5, book.getEditiondate());
			//stmt.setDate(6, book.getPrintdate());
			stmt.setString(5, book.getEditorial());
			
			
			stmt.executeUpdate();// Ejecuto la actualización
			ResultSet rs = stmt.getGeneratedKeys();// query para saber si ha ido
													// bien la inserción
			if (rs.next()) {// Si ha ido bien me da la id del sting
				int bookid = rs.getInt(1);

				book = getBookFromDatabase(Integer.toString(bookid));
			} else {
				throw new BadRequestException("Can't create a Book");
			}
			
			
		} catch (SQLException e) {
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

	private String buildInsertBook() {
		return "insert into books (title,author,language,edition,editorial) values(?,?,?,?,?); ";
	}

	private void ValidateBook(Books book) {//condiciones para poder ingresar enla BD
		if (book.getTitle() == null)
			throw new BadRequestException("Title can't be null.");
		if (book.getAuthor() == null)
			throw new BadRequestException("Author can't be null.");
		if (book.getEdition() == null)
			throw new BadRequestException("Edition can't be null.");
		//if (book.getEditiondate() == null)
		//	throw new BadRequestException("Editionale can't be null.");
		if (book.getLanguage() == null)
			throw new BadRequestException("Language can't be null.");
		if (book.getEditorial() == null)
			throw new BadRequestException("Language can't be null.");
		//if (book.getPrintdate() == null)
			//throw new BadRequestException("Printdate can't be null.");
		if (book.getTitle().length() > 80)
			throw new BadRequestException(
					"Title can't be greater than 80 characters.");
		if (book.getAuthor().length() > 20)
			throw new BadRequestException(
					"Author can't be greater than 20 characters.");
		if (book.getLanguage().length() > 15)
			throw new BadRequestException(
					"Language can't be greater than 15 characters.");
		if (book.getEdition().length() > 20)
			throw new BadRequestException(
					"Edition can't be greater than 20 characters.");
		if (book.getEditorial().length() > 20)
			throw new BadRequestException(
					"Editorial can't be greater than 20 characters.");
		
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

		
		EntityTag eTag = new EntityTag(Long.toString(s.hashCode()));
		
		
		//Comparamos el eTag creado con el que viene de la peticiOn HTTP
		Response.ResponseBuilder rb = request.evaluatePreconditions(eTag);// comparamos
		
		if (rb != null) {// Si el resultado no es nulo, significa que no ha sido modificado el contenido ( o es la 1º vez )
				return rb.cacheControl(cc).tag(eTag).build();
		}

		
		// Si es nulo construimos la respuesta de cero.
		rb = Response.ok(book).cacheControl(cc).tag(eTag);
		
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
			
			while(rsr.next()) {							
				Reviews review = new Reviews();
				review.setDateupdate(rsr.getDate("dateupdate"));
				review.setText(rsr.getString("text"));
				review.setUsername(rsr.getString("username"));
				review.setBookid(rsr.getInt("bookid"));
				review.setReviewsid(rsr.getInt("reviewsid"));
				
				book.addReviews(review);
		
			}
			
			
			
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
		return "select * from reviews  where bookid=?;";
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
