\conninfo
\d book
\d book_tag
SELECT id,title,publication_year,genre,author_id,publisher_id,version,archived FROM book ORDER BY id;
SELECT book_id,tag_id FROM book_tag ORDER BY book_id,tag_id;
