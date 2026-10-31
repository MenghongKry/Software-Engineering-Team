package edu.itc.se.shop.web;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import edu.itc.se.shop.config.ShopProperties;
import edu.itc.se.shop.service.PricingService;
import edu.itc.se.shop.service.ProductService;
import edu.itc.se.shop.web.dto.ProductForm;
import edu.itc.se.shop.web.dto.ProductRow;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/products")
public class ProductPageController {
    private static final Logger log = LoggerFactory.getLogger(ProductPageController.class);

    private final ProductService service;
    private final PricingService pricing;
    private final ShopProperties props;

    public ProductPageController(ProductService service, PricingService pricing, ShopProperties props) {
        this.service = service;
        this.pricing = pricing;
        this.props = props;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q, Model model) {
        List<ProductRow> rows = service.findAll(q).stream()
                .map(p -> new ProductRow(p, pricing.gross(p.price())))
                .toList();
        model.addAttribute("products", rows);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("currency", props.currency());
        model.addAttribute("threshold", props.lowStockThreshold());
        return "products/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("productForm", new ProductForm());
        return "products/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("productForm") ProductForm form,
                         BindingResult result,
                         RedirectAttributes redirect) {
        if (result.hasErrors()) {
            log.warn("product form rejected: {} error(s) {}", result.getErrorCount(), result.getFieldErrors());
            return "products/form";
        }
        service.create(form.getName(), form.getPrice(), form.getStock());
        redirect.addFlashAttribute("message", "Product '" + form.getName() + "' created.");
        return "redirect:/products";
    }
}
