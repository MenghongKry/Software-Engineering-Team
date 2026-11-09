package edu.itc.se.shop_jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import edu.itc.se.shop_jpa.domain.Tag;
import edu.itc.se.shop_jpa.repository.TagRepository;

@SpringBootTest
class TagFlushOrderTest {

    @Autowired TagRepository tags;

    @Test
    @Transactional
    void replacingTagWithoutFlushingDeleteViolatesUniqueConstraint() {
        Tag saleTag = tags.saveAndFlush(new Tag("sale"));

        tags.delete(saleTag);
        tags.save(new Tag("sale"));

        assertThatThrownBy(tags::flush)
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void flushingDeleteBeforeInsertAllowsReplacingTag() {
        Tag saleTag = tags.saveAndFlush(new Tag("sale"));

        tags.delete(saleTag);
        tags.flush();
        tags.save(new Tag("sale"));
        tags.flush();

        assertThat(tags.findByName("sale")).isPresent();
    }
}
