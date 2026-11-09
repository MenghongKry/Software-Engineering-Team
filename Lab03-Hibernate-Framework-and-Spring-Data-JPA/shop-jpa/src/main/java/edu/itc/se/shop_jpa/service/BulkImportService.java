package edu.itc.se.shop_jpa.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.itc.se.shop_jpa.domain.Category;
import edu.itc.se.shop_jpa.domain.Money;
import edu.itc.se.shop_jpa.domain.Product;
import edu.itc.se.shop_jpa.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;

@Service
public class BulkImportService {

    public static final int CHUNK = 500;

    private final SessionFactory sessionFactory;
    private final ProductRepository products;

    @PersistenceContext
    private EntityManager em;

    public BulkImportService(EntityManagerFactory emf, ProductRepository products) {
        this.sessionFactory = emf.unwrap(SessionFactory.class);
        this.products = products;
    }

    public int importCsv(Path csv) throws IOException {
        int imported = 0;
        try (StatelessSession ss = sessionFactory.openStatelessSession();
             Stream<String> lines = Files.lines(csv).skip(1)) {
            Transaction tx = ss.beginTransaction();
            try {
                ss.setJdbcBatchSize(CHUNK);
                List<Product> chunk = new ArrayList<>(CHUNK);
                for (Iterator<String> it = lines.iterator(); it.hasNext(); ) {
                    chunk.add(parse(it.next()));
                    if (chunk.size() == CHUNK) {
                        ss.insertMultiple(chunk);
                        imported += chunk.size();
                        chunk.clear();
                    }
                }
                if (!chunk.isEmpty()) {
                    ss.insertMultiple(chunk);
                    imported += chunk.size();
                }
                tx.commit();
            } catch (RuntimeException e) {
                tx.rollback();
                throw e;
            }
        }
        return imported;
    }
    @Transactional
    public int importCsvWithSaveAll(Path csv, boolean flushAndClear) throws IOException {
        int imported = 0;
        try (Stream<String> lines = Files.lines(csv).skip(1)) {
            List<Product> chunk = new ArrayList<>(CHUNK);
            for (Iterator<String> it = lines.iterator(); it.hasNext(); ) {
                chunk.add(parse(it.next()));
                if (chunk.size() == CHUNK) {
                    imported += saveChunk(chunk, flushAndClear);
                }
            }
            if (!chunk.isEmpty()) {
                imported += saveChunk(chunk, flushAndClear);
            }
        }
        return imported;
    }

    private int saveChunk(List<Product> chunk, boolean flushAndClear) {
        products.saveAll(chunk);
        int n = chunk.size();
        chunk.clear();
        if (flushAndClear) {
            em.flush();     
            em.clear();
        }
        return n;
    }

    // sku; name; amount; currency; category; stock
    private Product parse(String line) {
        String[] f = line.split(";", -1);
        Product p = new Product(f[0], f[1], new Money(new BigDecimal(f[2]), f[3]), Category.valueOf(f[4]));
        p.setStock(Integer.parseInt(f[5]));
        return p;
    }
}
