package edu.itc.se.shop.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import edu.itc.se.shop.web.dto.ProductForm;

@ControllerAdvice(assignableTypes = ProductPageController.class)
public class PageExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(PageExceptionHandler.class);

    @ExceptionHandler(DuplicateKeyException.class)
    public ModelAndView duplicateName(DuplicateKeyException exception) {
        log.warn("product form rejected: duplicate name");
        ModelAndView mav = new ModelAndView("products/form");
        mav.addObject("productForm", new ProductForm());
        mav.addObject("error", "A product with this name already exists.");
        return mav;
    }
}
