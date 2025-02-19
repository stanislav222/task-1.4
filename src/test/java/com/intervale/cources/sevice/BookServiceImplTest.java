package com.intervale.cources.sevice;

import com.intervale.cources.dao.BookDaoWithJdbcTemplate;
import com.intervale.cources.dto.NationalRateDto;
import com.intervale.cources.external.alfabank.model.Currency;
import com.intervale.cources.external.alfabank.service.AlfaBankExchangeWithWebClient;
import com.intervale.cources.external.openlibrary.dto.AuthorFromOpenLibDto;
import com.intervale.cources.external.openlibrary.dto.BookFromOpenLibraryDto;
import com.intervale.cources.external.openlibrary.service.OpenLibraryExchangeClientWithWebClient;
import com.intervale.cources.model.Book;
import com.intervale.cources.model.dto.BookDto;
import com.intervale.cources.model.dto.SimpleBankCurrencyExchangeRateDto;
import com.intervale.cources.sevice.impl.BookServiceImpl;
import com.intervale.cources.util.ModelMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

    @Mock
    private AlfaBankExchangeWithWebClient alfaBankExchangeClient;

    @Mock
    private BookDaoWithJdbcTemplate bookDaoWithJdbcTemplate;

    @Mock
    private OpenLibraryExchangeClientWithWebClient openLibraryExchangeClient;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private BookServiceImpl bookServiceImpl;

    private final BookDto dtoForTest = new BookDto("isbn", "title", "author", "sheets", "weight", new BigDecimal("0.0"));
    private final Book bookForTest = new Book("isbn", "title", "author", "sheets", "weight", new BigDecimal("0.0"));
    private final BookFromOpenLibraryDto libraryDtoForTest = new BookFromOpenLibraryDto(
            new AuthorFromOpenLibDto("key", "name"),
            "title",
            "key");

    @BeforeEach
    public void init() {
        lenient().when(modelMapper.bookDTOConvertToBookModel(dtoForTest)).thenReturn(bookForTest);
        lenient().when(modelMapper.bookModelConvertToBookDTO(bookForTest)).thenReturn(dtoForTest);
    }

    @Test
    void create() {
       bookServiceImpl.create(dtoForTest);
       verify(bookDaoWithJdbcTemplate).createBook(bookForTest);
    }

    @Test
    void readAll() {
        when(bookDaoWithJdbcTemplate.readAll()).thenReturn(List.of(bookForTest));
        List<BookDto> dtoList = bookServiceImpl.readAll();
        Assertions.assertEquals(dtoList, List.of(dtoForTest));
    }

    @Test
    void update() {
        when(bookDaoWithJdbcTemplate.update(bookForTest, 1)).thenReturn(true);
        boolean update = bookServiceImpl.update(dtoForTest, 1);
        Assertions.assertTrue(update);
    }

    @Test
    void delete() {
        when(bookDaoWithJdbcTemplate.delete(anyInt())).thenReturn(true);
        boolean delete = bookServiceImpl.delete(anyInt());
        Assertions.assertTrue(delete);
    }

    @Test
    void readFromOpenLibrary() {
        when(openLibraryExchangeClient.getBookFromOpenLibraryByAuthor(anyString())).thenReturn(List.of(libraryDtoForTest));
        List<BookDto> dtoList = bookServiceImpl.readFromOpenLibrary(anyString());
        Assertions.assertEquals(dtoList, List.of(libraryDtoForTest).stream()
                .map(modelMapper::openLibraryDtoConvertToBookDTO).collect(Collectors.toList()));
    }

    @Test
    void readBookByAuthorFromDbAndOL() {
        when(bookDaoWithJdbcTemplate.getBookByAuthor(anyString())).thenReturn(List.of(bookForTest));
        when(openLibraryExchangeClient.getBookFromOpenLibraryByAuthor(anyString())).thenReturn(List.of(libraryDtoForTest));
        List<BookDto> dtoList = bookServiceImpl.readBookByAuthorFromDbAndOL(anyString());
        List<BookDto> dtoList2 = bookServiceImpl.readFromOpenLibrary(anyString());
        Assertions.assertEquals(dtoList, List.of(bookForTest).stream().map(modelMapper::bookModelConvertToBookDTO)
                .collect(Collectors.toCollection(() -> dtoList2)));
    }

     @Test
    void bookDTOConvertToBookModel() {
        Book book = modelMapper.bookDTOConvertToBookModel(dtoForTest);
        assertThat(book.getIsbn(), equalTo("isbn"));
        assertThat(book.getTitle(), equalTo("title"));
        assertThat(book.getAuthor(), equalTo("author"));
        assertThat(book.getSheets(), equalTo("sheets"));
        assertThat(book.getWeight(), equalTo("weight"));
        assertThat(book.getCost(), comparesEqualTo(new BigDecimal("0.0")));
    }

   @Test
    void openLibraryDtoConvertToBookDTO() {
        String info = "info missing in openLibrary";
       lenient().when(modelMapper.openLibraryDtoConvertToBookDTO(libraryDtoForTest))
               .thenReturn(new BookDto(info, "title", "name", info, info, new BigDecimal("0.0")));
       BookDto bookDto = modelMapper.openLibraryDtoConvertToBookDTO(libraryDtoForTest);
        assertThat(bookDto.getIsbn(), equalTo(info));
        assertThat(bookDto.getTitle(), equalTo("title"));
        assertThat(bookDto.getAuthor(), equalTo("name"));
        assertThat(bookDto.getSheets(), equalTo(info));
        assertThat(bookDto.getWeight(), equalTo(info));
        assertThat(bookDto.getCost(), comparesEqualTo(new BigDecimal("0.0")));
    }

    @Test
    void bookModelConvertToBookDTO() {
        BookDto bookDto = modelMapper.bookModelConvertToBookDTO(bookForTest);
        assertThat(bookDto.getIsbn(), equalTo("isbn"));
        assertThat(bookDto.getTitle(), equalTo("title"));
        assertThat(bookDto.getAuthor(), equalTo("author"));
        assertThat(bookDto.getSheets(), equalTo("sheets"));
        assertThat(bookDto.getWeight(), equalTo("weight"));
        assertThat(bookDto.getCost(), comparesEqualTo(new BigDecimal("0.0")));
    }

    @SneakyThrows
    @Test
    void getPriceByTitle() {
        when(bookDaoWithJdbcTemplate.getPriceByTitle(anyString())).thenReturn(bookForTest);
        BigDecimal price = bookServiceImpl.getPriceByTitle("title");
        Assertions.assertEquals(new BigDecimal("0.0"), price);
    }

    @SneakyThrows
    @Test
    void getPriceByTitleWithCostInDifferentCurrencies() {
        when(bookDaoWithJdbcTemplate.getPriceByTitle(anyString())).thenReturn(bookForTest);
        when(alfaBankExchangeClient.getTheCurrentCurrencySaleRate(List.of(Currency.RUB)))
                .thenReturn(Collections.singletonList(new NationalRateDto() {{
                    setRate(new BigDecimal("3.405200"));
                    setCode(643);
                    //setDate(LocalDate.of(2022, 02, 17));
                    setDate("17.02.2022");
                    setIso("RUB");
                    setName("российский рубль");
                    setQuantity(100);
                }}));
        SimpleBankCurrencyExchangeRateDto dto = bookServiceImpl.getPriceByTitleWithCostInDifferentCurrencies("title",
                List.of(Currency.RUB));
        Assertions.assertEquals("title", dto.getTitle());
    }
}