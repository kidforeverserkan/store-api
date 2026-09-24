package com.kidforeverserkan.store.web;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Page shells for the storefront. Every route here just returns a Thymeleaf
 * view name (or, for the product page, the id the template needs) — all
 * product/cart/order data is fetched client-side from the existing REST API.
 */
@Controller
public class StoreController {

    @GetMapping("/shop")
    public String shop() {
        return "shop";
    }

    @GetMapping("/shop/{id}")
    public String product(@PathVariable Long id, Model model) {
        model.addAttribute("productId", id);
        return "product";
    }

    @GetMapping("/cart")
    public String cart() {
        return "cart";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @GetMapping("/checkout-success")
    public String checkoutSuccess() {
        return "checkout-success";
    }

    @GetMapping("/checkout-cancel")
    public String checkoutCancel() {
        return "checkout-cancel";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }

    @GetMapping("/contact")
    public String contact() {
        return "contact";
    }

    // produces=HTML disambiguates this from OrderController's "/orders" API
    // endpoint — see the comment there for why they intentionally share a path.
    @GetMapping(value = "/orders", produces = MediaType.TEXT_HTML_VALUE)
    public String orders() {
        return "orders";
    }
}
