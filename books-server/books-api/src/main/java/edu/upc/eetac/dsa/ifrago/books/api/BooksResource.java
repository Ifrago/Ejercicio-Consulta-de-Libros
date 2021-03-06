package edu.upc.eetac.dsa.ifrago.books.api;

import java.awt.print.Book;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import javax.sql.DataSource;
import javax.validation.constraints.Null;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
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

	private boolean admin, registered;

	@GET
	@Produces(MediaType.BOOKS_API_BOOKS_COLLECTION)
	public BooksCollection getBooks(@QueryParam("length") int length,
			@QueryParam("after") int after) {

		setAdministrator(security.isUserInRole("admin"));
		
		System.out.println("Dentro el metodo getBooks con length: "+length+" y after: "+after);
		BooksCollection books = new BooksCollection();

		Connection conn = null;
		try {
			conn = ds.getConnection();// Conectamos con la base de datos
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}

		PreparedStatement stmt = null;
		System.out.println("Conexion BD establecida");

		try {
			boolean updateFromLast = after > 0;
			stmt = conn.prepareStatement(buildGetBooksQuery(updateFromLast));
			if (updateFromLast) {
				if (length == 0) {
					stmt.setInt(1, after);
					stmt.setInt(2, 5);
				} else {
					stmt.setInt(1, after);
					stmt.setInt(2, length);
				}
			} else {

				if (length == 0)
					stmt.setInt(1, 5);
				else
					stmt.setInt(1, length);
			}
			
			System.out.println("La query es: "+ stmt);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				System.out.println("libro cogido");
				Books book = new Books();

				book.setId(rs.getInt("bookid"));
				book.setTitle(rs.getString("title"));
				book.setAuthor(rs.getString("author"));
				book.setLanguage(rs.getString("language"));
				book.setEdition(rs.getString("edition"));
				book.setEditiondate(rs.getDate("editiondate"));
				book.setPrintdate(rs.getDate("printdate"));
				book.setEditorial(rs.getString("editorial"));

				// Nos encargamos de las reviews de cada libro
				PreparedStatement stmtr = null;
				stmtr = conn.prepareStatement(buildGetReviewBookByIdQuery());
				stmtr.setInt(1, book.getId());

				ResultSet rsr = stmtr.executeQuery();

				while (rsr.next()) {
					System.out.println("Review cogida");
					Reviews review = new Reviews();
					review.setReviewid(rsr.getInt("reviewid"));
					review.setDateupdate(rsr.getDate("dateupdate"));
					review.setText(rsr.getString("text"));
					review.setUsername(rsr.getString("username"));
					review.setBookid(rsr.getInt("bookid"));

					book.addReviews(review);

				}

				books.addBook(book);// Añadimos toda la info dellibro (
									// inclusive sus reviews ).

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

		if (updateFromLast)
			return "select * from books where bookid>? limit ?;";
		else
			return "select *from books limit ?;";
	}

	@POST
	@Consumes(MediaType.BOOKS_API_BOOKS)
	@Produces(MediaType.BOOKS_API_BOOKS)
	public Books createBook(Books book) {// Dates can are null
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

		try {
			String sql = buildInsertBook(book.getEditiondate(),
					book.getPrintdate());
			stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

			System.out.println("Printdate: " + book.getPrintdate());
			System.out.println("Editiondate: " + book.getEditiondate());

			if (book.getEditiondate() != null && book.getPrintdate() != null) {
				stmt.setString(1, book.getTitle());
				stmt.setString(2, book.getAuthor());
				stmt.setString(3, book.getLanguage());
				stmt.setString(4, book.getEdition());
				stmt.setString(5, book.getEditorial());
				stmt.setDate(6, (Date) book.getPrintdate());
				stmt.setDate(7, (Date) book.getEditiondate());
			}

			if (book.getEditiondate() == null && book.getPrintdate() != null) {
				stmt.setString(1, book.getTitle());
				stmt.setString(2, book.getAuthor());
				stmt.setString(3, book.getLanguage());
				stmt.setString(4, book.getEdition());
				stmt.setString(5, book.getEditorial());
				stmt.setDate(6, (Date) book.getPrintdate());
			}

			if (book.getEditiondate() != null && book.getPrintdate() == null) {
				stmt.setString(1, book.getTitle());
				stmt.setString(2, book.getAuthor());
				stmt.setString(3, book.getLanguage());
				stmt.setString(4, book.getEdition());
				stmt.setString(5, book.getEditorial());
				stmt.setDate(7, (Date) book.getEditiondate());
			}

			if (book.getEditiondate() == null && book.getPrintdate() == null) {
				stmt.setString(1, book.getTitle());
				stmt.setString(2, book.getAuthor());
				stmt.setString(3, book.getLanguage());
				stmt.setString(4, book.getEdition());
				stmt.setString(5, book.getEditorial());
			}

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

	private String buildInsertBook(java.util.Date editiondate,
			java.util.Date printdate) {

		if (editiondate != null && printdate != null)
			return "insert into books (title,author,language,edition,editorial,printdate,editiondate) values(?,?,?,?,?,?,?); ";
		if (editiondate != null && printdate == null)
			return "insert into books (title,author,language,edition,editorial,editiondate) values(?,?,?,?,?,?); ";
		if (editiondate == null && printdate != null)
			return "insert into books (title,author,language,edition,editorial,printdate) values(?,?,?,?,?,?); ";
		else
			return "insert into books (title,author,language,edition,editorial) values(?,?,?,?,?); ";

	}

	private void ValidateBook(Books book) {// condiciones para poder ingresar
											// enla BD
		if (book.getTitle() == null)
			throw new BadRequestException("Title can't be null.");
		if (book.getAuthor() == null)
			throw new BadRequestException("Author can't be null.");
		if (book.getEdition() == null)
			throw new BadRequestException("Edition can't be null.");
		// if (book.getEditiondate() == null)
		// throw new BadRequestException("Editionale can't be null.");
		if (book.getLanguage() == null)
			throw new BadRequestException("Language can't be null.");
		if (book.getEditorial() == null)
			throw new BadRequestException("Language can't be null.");
		// if (book.getPrintdate() == null)
		// throw new BadRequestException("Printdate can't be null.");
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
	public Response getBook(@PathParam("bookid") String bookid,
			@Context Request request) {

		// Creamos CacheControl
		CacheControl cc = new CacheControl();

		// Sacamos un book de la base de datos
		Books book = getBookFromDatabase(bookid);

		// Calculamos ETag de la ultima modificación de la reseña

		String s = book.getReviews() + book.getAuthor() + book.getEdition()
				+ "21";

		EntityTag eTag = new EntityTag(Long.toString(s.hashCode()));

		// Comparamos el eTag creado con el que viene de la peticiOn HTTP
		Response.ResponseBuilder rb = request.evaluatePreconditions(eTag);// comparamos

		if (rb != null) {// Si el resultado no es nulo, significa que no ha sido
							// modificado el contenido ( o es la 1º vez )
			return rb.cacheControl(cc).tag(eTag).build();
		}

		// Si es nulo construimos la respuesta de cero.
		rb = Response.ok(book).cacheControl(cc).tag(eTag);

		return rb.build();

	}

	private Books getBookFromDatabase(String bookid) {

		Books book = new Books();

		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}

		PreparedStatement stmt = null;
		PreparedStatement stmtr = null;

		try {
			stmt = conn.prepareStatement(buildGetBookByIdQuery());
			stmt.setInt(1, Integer.valueOf(bookid));
			System.out.println("Query completa: " + stmt);
			ResultSet rs = stmt.executeQuery();

			if (rs.next()) {
				book.setId(rs.getInt("bookid"));
				book.setTitle(rs.getString("title"));
				book.setAuthor(rs.getString("author"));
				book.setLanguage(rs.getString("language"));
				book.setEdition(rs.getString("edition"));
				book.setEditiondate(rs.getDate("editiondate"));
				book.setPrintdate(rs.getDate("printdate"));
				book.setEditorial(rs.getString("editorial"));

			} else {
				throw new NotFoundException("There's no sting with stingid ="
						+ bookid);
			}

			// Cogemos las reviews de un libro
			stmtr = conn.prepareStatement(buildGetReviewBookByIdQuery());
			stmtr.setInt(1, Integer.valueOf(bookid));
			ResultSet rsr = stmtr.executeQuery();

			while (rsr.next()) {
				Reviews review = new Reviews();
				review.setReviewid(rsr.getInt("reviewid"));
				review.setDateupdate(rsr.getDate("dateupdate"));
				review.setText(rsr.getString("text"));
				review.setUsername(rsr.getString("username"));
				review.setBookid(rsr.getInt("bookid"));

				book.addReviews(review);

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

	private String buildGetReviewBookByIdQuery() {
		return "select * from reviews  where bookid=?;";
	}

	private String buildGetBookByIdQuery() {
		return "select * from books  where  bookid=?;";
	}

	@GET
	@Path("/search")
	@Produces(MediaType.BOOKS_API_BOOKS_COLLECTION)
	public BooksCollection searchByAuthorBook(
			@QueryParam("author") String author,
			@QueryParam("tittle") String tittle,
			@QueryParam("length") int length) {
		BooksCollection books = new BooksCollection();

		Connection conn = null;
		try {
			conn = ds.getConnection();// Conectamos con la base de datos
		} catch (SQLException e) {
			throw new ServerErrorException(
					"Could not connect to the d ( linea 413 )atabase",
					Response.Status.SERVICE_UNAVAILABLE);
		}

		String sql = buildSearchByAhutor(author, tittle);
		PreparedStatement stmt = null;

		try {
			System.out.println("Query a construir: " + sql);
			stmt = conn.prepareStatement(sql);
			if (length != 0) {
				if (author != null && tittle != null) {
					stmt.setString(1, "%" + author + "%");
					stmt.setString(2, "%" + tittle + "%");
					stmt.setInt(3, length);// Limitamos el numero de resultados,
											// es el parametro 3
				} else if (author == null && tittle != null) {
					System.out
							.println("Estamos en Sub=null, Cont!= null, length!=0");
					stmt.setString(1, "%" + tittle + "%");
					stmt.setInt(2, length);// Limitamos el numero de resultados,
											// es el parametro 2
				} else if (author != null && tittle == null) {
					System.out
							.println("Estamos en Sub=null, Cont= null, length!=0");
					stmt.setString(1, "%" + author + "%");
					stmt.setInt(2, length);// Limitamos el numero de resultados,
											// es el parametro 2
				}
			} else if (length == 0) {
				if (author != null && tittle != null) {
					System.out
							.println("Estamos en Sub!=null, Cont!= null, length=0-> 5");
					stmt.setString(1, "%" + author + "%");
					stmt.setString(2, "%" + tittle + "%");
					stmt.setInt(3, 5);// Limitamos el numero de resultados a 5,
										// es el parametro 3
				} else if (author == null && tittle != null) {
					System.out
							.println("Estamos en Sub=null, Cont!= null, length=0-> 5");
					stmt.setString(1, "%" + tittle + "%");
					stmt.setInt(2, 5);// Limitamos el numero de resultados,
										// es el parametro 2
				} else if (author != null && tittle == null) {
					System.out
							.println("Estamos en Sub!=null, Cont= null, length=0-> 5");
					stmt.setString(1, "%" + author + "%");
					stmt.setInt(2, 5);// Limitamos el numero de resultados,
										// es el parametro 2
				}
			}

			System.out.println("Query salida: " + stmt);
			ResultSet rs = stmt.executeQuery();// mandamos la query
			System.out.println("Resultado: " + rs);
			while (rs.next()) {
				Books book = new Books();

				book.setId(rs.getInt("bookid"));
				book.setTitle(rs.getString("title"));
				book.setAuthor(rs.getString("author"));
				book.setLanguage(rs.getString("language"));
				book.setEdition(rs.getString("edition"));
				book.setEditiondate(rs.getDate("editiondate"));
				book.setPrintdate(rs.getDate("printdate"));
				book.setEditorial(rs.getString("editorial"));

				// Nos encargamos de las reviews de cada libro
				PreparedStatement stmtr = null;
				stmtr = conn.prepareStatement(buildGetReviewBookByIdQuery());
				stmtr.setInt(1, book.getId());

				ResultSet rsr = stmtr.executeQuery();

				while (rsr.next()) {
					Reviews review = new Reviews();
					review.setReviewid(rsr.getInt("reviewid"));
					review.setDateupdate(rsr.getDate("dateupdate"));
					review.setText(rsr.getString("text"));
					review.setUsername(rsr.getString("username"));
					review.setBookid(rsr.getInt("bookid"));

					book.addReviews(review);

				}

				books.addBook(book);// Añadimos toda la info dellibro (
									// inclusive sus reviews ).

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

	private String buildSearchByAhutor(String author, String tittle) {

		System.out.println("Author: " + author + " Tittle: " + tittle);

		String query = null;
		if (author != null && tittle != null)
			// QUERY: seleccionar toda la tabala sting y columna name de tabal
			// user donde aparezca algo del subject y del content que nos pasan.
			return "SELECT  * FROM books  WHERE  author LIKE ? OR title LIKE ? LIMIT ? ;";

		if (author == null && tittle != null)
			// QUERY: seleccionar toda la tabala sting y columna name de tabal
			// user donde aparezca algo del content que nos pasan.
			return "SELECT  * FROM books  WHERE   title LIKE ? LIMIT ? ;";

		if (author != null && tittle == null)
			// QUERY: seleccionar toda la tabala sting y columna name de tabal
			// user donde aparezca algo del subject que nos pasan.
			return "SELECT  * FROM books  WHERE  author LIKE ? LIMIT ? ;";

		if (author == null && tittle == null)// En este caso no se puede
												// buscar nada ya que nos han
												// devuelto los dos paremotros
												// nulos.
			throw new BadRequestException(
					"Se tiene que poner algo en el subject o context para poder buscar.");

		return query;
	}

	@PUT
	@Path("/{bookid}")
	@Consumes(MediaType.BOOKS_API_BOOKS)
	@Produces(MediaType.BOOKS_API_BOOKS)
	public Books updateBook(@PathParam("bookid") int bookid, Books book) {

		if (!security.isUserInRole("admin"))
			throw new ForbiddenException("You are not allowed to delete a book");
		System.out.println("Eres admin");

		setAdministrator(security.isUserInRole("admin"));

		ValidateBookforUpdate(book);
		System.out.println("Book validado");
		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}
		System.out.println("BD establecida");
		PreparedStatement stmt = null;

		try {
			String sql = buildUpdateBook();
			System.out.println("Query escrita");
			stmt = conn.prepareStatement(sql);
			System.out.println("Query cargada");
			stmt.setString(1, book.getTitle());
			stmt.setString(2, book.getAuthor());
			stmt.setString(3, book.getLanguage());
			stmt.setString(4, book.getEdition());
			stmt.setDate(5, (Date) book.getEditiondate());
			stmt.setDate(6, (Date) book.getPrintdate());
			stmt.setString(7, book.getEditorial());
			stmt.setInt(8, bookid);
			System.out.println("Query lista");
			int rows = stmt.executeUpdate();
			System.out.println("Query ejecutada");
			String sbookid = Integer.toString(bookid);
			System.out.println("Miramos si hay contestación row=" + sbookid);
			if (rows == 1) {
				System.out.println("Cogemos el book modificado");
				book = getBookFromDatabase(sbookid);
				System.out.println("Hemos cogido el book modificado");
			} else {
				throw new NotFoundException("There's no book with bookid="
						+ bookid);// Updating inexistent sting
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

	private void ValidateBookforUpdate(Books book) {
		System.out.println("Dentro del validate");
		if (book.getTitle() != null && book.getTitle().length() > 80)
			throw new BadRequestException(
					"Title can't be greater than 80 characters.");
		if (book.getAuthor() != null && book.getAuthor().length() > 20)
			throw new BadRequestException(
					"Author can't be greater than 20 characters.");
		if (book.getLanguage() != null && book.getLanguage().length() > 15)
			throw new BadRequestException(
					"Language can't be greater than 15 characters.");
		if (book.getEdition() != null && book.getEdition().length() > 20)
			throw new BadRequestException(
					"Edition can't be greater than 20 characters.");
		if (book.getEditorial() != null && book.getEditorial().length() > 20)
			throw new BadRequestException(
					"Editorial can't be greater than 20 characters.");
		System.out.println("Fuera del validate");
	}

	private String buildUpdateBook() {
		return "update books set title=ifnull(?, title), author=ifnull(?, author), language=ifnull(?, language), edition=ifnull(?, edition), editiondate=ifnull(?, editiondate), printdate=ifnull(?, printdate), editorial=ifnull(?, editorial) where bookid=?;";
	}

	@DELETE
	@Path("/{bookid}")
	public void deleteBook(@PathParam("bookid") String bookid) {

		if (!security.isUserInRole("admin"))
			throw new ForbiddenException("You are not allowed to delete a book");

		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}

		PreparedStatement stmt = null;

		try {
			String sql = buildDeleteBook();
			stmt = conn.prepareStatement(sql);
			stmt.setInt(1, Integer.valueOf(bookid));

			int rows = stmt.executeUpdate();

			if (rows == 0)
				throw new NotFoundException("There's no sting with book="
						+ bookid);

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

	}

	private String buildDeleteBook() {
		return "delete from books where bookid=?;";
	}

	@POST
	@Path("/{bookid}/reviews")
	@Consumes(MediaType.BOOKS_API_REVIEWS)
	@Produces(MediaType.BOOKS_API_REVIEWS)
	public Reviews createReview(@PathParam("bookid") String bookid,
			Reviews review) {
		if (!security.isUserInRole("registered"))
			throw new ForbiddenException(
					"You are not allowed to create reviews for a book");
		String tmp = security.getUserPrincipal().getName();
		System.out.println("Eres el registred");

		setRegistered(security.isUserInRole("registered"));

		ValidateReview(review);
		System.out.println("Review validada");

		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}
		System.out.println("Connexion BD establecida");

		PreparedStatement stmt = null;
		PreparedStatement stmt2 = null;

		System.out.println("Conociemiento de Dateupdate");

		try {
			// Para hacer que un usuario no haga dos reseñas en un mismo libro
			stmt = conn.prepareStatement(locateReview());
			stmt.setInt(1, Integer.valueOf(bookid));
			stmt.setString(2, security.getUserPrincipal().getName());
			ResultSet rsV = stmt.executeQuery();
			if (rsV != null) {
				stmt.close();
				throw new BadRequestException("Can't create other Review "
						+ tmp);
			}

			String sql = buildInsertReview();
			stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

			// stmt.setString(1, security.getUserPrincipal().getName());
			stmt.setString(1, tmp);
			stmt.setString(2, review.getText());
			stmt.setInt(3, Integer.parseInt(bookid));

			stmt.executeUpdate();// Ejecuto la actualización
			System.out.println("Query ejecutada");

			/*
			 * //Cogemos las reviews de un libro PreparedStatement stmtr =
			 * conn.prepareStatement(buildGetReviewBookByIdQuery());
			 * stmtr.setInt(1, Integer.valueOf(bookid)); ResultSet rsr =
			 * stmtr.executeQuery();
			 * 
			 * while(rsr.next()) { Reviews review = new Reviews();
			 * review.setDateupdate(rsr.getDate("dateupdate"));
			 * review.setText(rsr.getString("text"));
			 * review.setUsername(rsr.getString("username"));
			 * review.setBookid(rsr.getInt("bookid"));
			 * 
			 * book.addReviews(review);
			 */

			// }

			sql = locateReview();
			System.out.println("Query para ver la review: " + sql);
			stmt2 = conn.prepareStatement(sql);
			stmt2.setInt(1, Integer.valueOf(bookid));
			stmt2.setString(2, tmp);
			System.out.println("QLe metemos bookid: " + bookid);
			System.out.println("Lemetemos Username: " + tmp);

			ResultSet rs = stmt2.executeQuery();

			System.out.println("Query ejecutada");

			if (rs.next()) {
				System.out.println("Miramos contestacion query");

				review.setBookid(rs.getInt("bookid"));
				review.setReviewid(rs.getInt("reviewid"));
				review.setDateupdate(rs.getDate("dateupdate"));
				review.setText(rs.getString("text"));
				review.setUsername(rs.getString("username"));
			} else {
				throw new BadRequestException("Can't view the Review");
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

		return review;
	}

	private boolean reviseDuplicateReview(Reviews review) {// miramos que no hay
															// ninguna reseña
															// con el mismo
															// bookid y username

		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}

		PreparedStatement stmt = null;

		try {
			stmt = conn.prepareStatement(locateReview());
			stmt.setInt(1, review.getBookid());
			stmt.setString(2, review.getUsername());

			ResultSet rs = stmt.executeQuery();

			if (rs.next()) {
				review.setBookid(rs.getInt("bookid"));
				review.setDateupdate(rs.getDate("dateupdate"));
				review.setText(rs.getString("text"));
				review.setUsername(rs.getString("username"));

				return false;

			} else {
				return true;
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
	}

	private String buildInsertReview() {
		return "insert into reviews (username,text,bookid) values (?,?,?);";
	}

	private Reviews getReviewFromDatabase(String reviewid, String bookid) {

		Reviews review = new Reviews();

		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}

		PreparedStatement stmt = null;

		try {
			stmt = conn.prepareStatement(locateReviewWithReviewID());
			stmt.setInt(1, Integer.valueOf(bookid));
			stmt.setString(2, reviewid);

			ResultSet rs = stmt.executeQuery();
			System.out.println("Query: " + stmt);
			if (rs.next()) {
				review.setReviewid(rs.getInt("reviewid"));
				review.setDateupdate(rs.getDate("dateupdate"));
				review.setText(rs.getString("text"));
				review.setUsername(rs.getString("username"));
				review.setBookid(rs.getInt("bookid"));

			} else {
				throw new NotFoundException("There's no review with bookid ="
						+ bookid + "and reviewid = " + reviewid);
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

		return review;
	}

	private String locateReviewWithReviewID() {
		return "select * from reviews where bookid=? and reviewid=?;";
	}

	private String locateReview() {
		return "select * from reviews where bookid=? and username=?;";
	}

	private void ValidateReview(Reviews review) {// Para poder crear una reseña
													// tiene que aporbar todos
													// los if
		if (review.getText() == null)
			throw new BadRequestException("Text can't be null.");
		if (review.getText().length() > 500)
			throw new BadRequestException(
					"Text can't be greater than 500 characters.");

	}

	@PUT
	@Path("/{bookid}/reviews/{reviewid}")
	@Consumes(MediaType.BOOKS_API_REVIEWS)
	@Produces(MediaType.BOOKS_API_REVIEWS)
	public Reviews updateReview(@PathParam("bookid") String bookid,
			@PathParam("reviewid") String reviewid, Reviews review) {

		if (!security.isUserInRole("registered"))
			throw new ForbiddenException(
					"You are not allowed to update a review");

		setRegistered(security.isUserInRole("registered"));

		validateReviewfroUpdate(review, bookid, reviewid);

		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}

		PreparedStatement stmt = null;
		PreparedStatement stmtV = null;
		System.out.println("bookid: " + bookid + " Reviewid: " + reviewid
				+ " Text: " + review.getText());
		try {

			// Para hacer que un usuario no haga dos reseñas en un mismo libro
			System.out.println("Es elpropietario");
			String sql = buildUpdateReview();
			stmt = conn.prepareStatement(sql);

			stmt.setString(1, review.getText());
			stmt.setString(2, bookid);
			stmt.setString(3, reviewid);

			int rows = stmt.executeUpdate();
			if (rows == 1) {
				System.out.println("Intentamos sacar la review");
				review = getReviewFromDatabase(reviewid, bookid);
			} else {
				System.out.println("No hay nada de row");
				throw new NotFoundException("There's no review with bookid="
						+ bookid + "and reviewid= " + reviewid);// Updating
																// inexistent
																// sting
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

		return review;
	}

	private String buildUpdateReview() {
		return "update reviews set  text=ifnull(?, text) where bookid=? and reviewid=?;";
	}

	private void validateReviewfroUpdate(Reviews review, String bookid,
			String reviewid) {

		if (review.getText().length() > 500)
			throw new BadRequestException(
					"Text can't be greater than 500 characters.");
		Reviews rev = getReviewFromDatabase(reviewid, bookid);
		System.out.println("Miramos que sea la autora del review");
		if (!security.getUserPrincipal().getName().equals(rev.getUsername()))
			throw new ForbiddenException(
					"You are not allowed to modify this review.");
	}

	@DELETE
	@Path("/{bookid}/reviews/{reviewid}")
	public void deleteReview(@PathParam("bookid") int bookid,
			@PathParam("reviewid") int reviewid) {

		/*if (security.isUserInRole("registered")
				|| security.isUserInRole("admin"))
			throw new ForbiddenException("You are not allowed to delete a book");*/

		setRegistered(security.isUserInRole("registered"));

		validateReviewfroDelete(bookid, reviewid);

		System.out.println("Eres el admin");
		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException e) {
			throw new ServerErrorException("Could not connect to the database",
					Response.Status.SERVICE_UNAVAILABLE);
		}
		System.out.println("Conectado a la BD");

		PreparedStatement stmt = null;

		try {
			System.out.println("query haciendose");
			String sql = buildDeleteReview();
			stmt = conn.prepareStatement(sql);
			System.out.println("query casi hecha");
			stmt.setInt(1, reviewid);
			stmt.setInt(2, bookid);

			System.out.println("query rellena");
			int rows = stmt.executeUpdate();
			System.out.println("query enviada");
			if (rows == 0)
				throw new NotFoundException("There's no review with bookid="
						+ bookid + "and reviewid= " + reviewid);

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

	}

	private void validateReviewfroDelete(int bookid, int reviewid) {

		Reviews rev = getReviewFromDatabase(Integer.toString(reviewid),
				Integer.toString(bookid));
		System.out.println("Miramos que sea la autora del review");

		if (!security.isUserInRole("admin")) {
			if (!security.getUserPrincipal().getName()
					.equals(rev.getUsername()))
				throw new ForbiddenException(
						"You are not allowed to delete this review, "+security.getUserPrincipal().getName());
		}

	}

	private String buildDeleteReview() {
		return "delete from reviews where reviewid=? and bookid=?;";
	}

	public boolean isAdministrator() {
		return admin;
	}

	public void setAdministrator(boolean administrator) {
		this.admin = administrator;
	}

	public boolean isRegistered() {
		return registered;
	}

	public void setRegistered(boolean registered) {
		this.registered = registered;
	}

}
