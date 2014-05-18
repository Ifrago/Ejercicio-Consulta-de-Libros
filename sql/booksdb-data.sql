source booksdb-schema.sql;

insert into users values('alicia', MD5('alicia'), 'Alicia Jackson', 'alicia@acme.com');
insert into user_roles values ('alicia', 'registered');

insert into users values('blas', MD5('blas'), 'Blas Guillermo', 'blas@acme.com');
insert into user_roles values ('blas', 'registered');

insert into users values('ivan', MD5('ivan'), 'Iván Frago', 'ivan@acme.com');
insert into user_roles values ('ivan', 'admin');

insert into books (title,author,language,edition,editiondate,printdate,editorial) values('Peter Jackson: El ladron del rayo', 'Rick Riordan','Castellano','quinta', '2008-10-15','2009-01-19','Salamandra');
insert into books (title,author,language,edition,editiondate,printdate,editorial) values('Peter Jackson: Mar de monstruos', 'Rick Riordan','Castellano','quinta', '2009-10-15','2010-01-19','Salamandra');
insert into books (title,author,language,edition,editiondate,printdate,editorial) values('Peter Jackson: La maldicion del titan', 'Rick Riordan','Castellano','quinta', '2010-10-15','2011-01-19','Salamandra');
insert into books (title,author,language,edition,editiondate,printdate,editorial) values('Peter Jackson: La batalla del laberinto', 'Rick Riordan','Castellano','quinta', '2011-10-15','2012-01-19','Salamandra');
insert into books (title,author,language,edition,editiondate,printdate,editorial) values('Peter Jackson: El ultimo heroe del Olimpo', 'Rick Riordan','Castellano','quinta', '2013-10-15','2014-01-19','Salamandra');

insert into reviews (username,dateupdate,text,bookid) values ('alicia', '2013-12-09','Buena saga, se lee muy rapido',5);
insert into reviews (username,dateupdate,text,bookid) values ('alicia', '2012-10-09','Cada libro termina mejor',3);
insert into reviews (username,dateupdate,text,bookid) values ('blas', '2013-12-09','La saga es entretenida, no dificil de leer',5);
insert into reviews (username,dateupdate,text,bookid) values ('blas', '2012-12-09','De los tres que he leido, es el mas flojo, pero no significa que no este muy bien',2);