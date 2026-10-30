package edu.itc.se.shop.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import edu.itc.se.shop.domain.Product;
import edu.itc.se.shop.service.ProductNotFoundException;
import edu.itc.se.shop.service.ProductService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
class ProductControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean ProductService service;

    @Test
    void listReturns200() throws Exception {
        when(service.findAll(null)).thenReturn(List.of(
                new Product(1L, "Notebook", new BigDecimal("2.50"), 3)));

        mvc.perform(get("/api/products"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].id").value(1))
           .andExpect(jsonPath("$[0].name").value("Notebook"));
    }

    @Test
    void getMissingProductReturns404() throws Exception {
        when(service.findById(42)).thenThrow(new ProductNotFoundException(42));

        mvc.perform(get("/api/products/42"))
           .andExpect(status().isNotFound());
    }

    @Test
    void createWithInvalidBodyReturns400WithTwoFieldErrors() throws Exception {
        mvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "", "price": 0, "stock": 1}
                        """))
           .andExpect(status().isBadRequest())
           .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
           .andExpect(jsonPath("$.errors.length()").value(2));
    }

    @Test
    void createReturns201WithLocationHeader() throws Exception {
        when(service.create(eq("Notebook"), eq(new BigDecimal("2.50")), eq(3)))
                .thenReturn(new Product(7L, "Notebook", new BigDecimal("2.50"), 3));

        mvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "Notebook", "price": 2.50, "stock": 3}
                        """))
           .andExpect(status().isCreated())
           .andExpect(header().string("Location", "/api/products/7"))
           .andExpect(jsonPath("$.id").value(7))
           .andExpect(jsonPath("$.name").value("Notebook"));
    }
}