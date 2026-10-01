package management.system.sale.Service;

import management.system.sale.DTO.ProductPageResponse;
import management.system.sale.Model.LogTable;
import management.system.sale.Model.Product;
import management.system.sale.Repository.LogTableRepository;
import management.system.sale.Repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {

    @Autowired
    ProductRepository productRepository;

    @Autowired
    LogTableRepository logTableRepository;

    public List<Product> save_product(List<Product> products) {

        // First save products so IDs are generated
        List<Product> savedProducts = productRepository.saveAll(products);

        List<LogTable> logs = new ArrayList<>();

        for (Product product : savedProducts) {

            LogTable log = new LogTable();

            // Generated product ID
            log.setProduct_id(product.getId());

            // New product has no previous price
            log.setOldPrice(product.getCurrentPrice());

            // Product's initial/current price
            log.setNewPrice(product.getCurrentPrice());

            log.setChangeType("PRODUCT_CREATED");

            logs.add(log);
        }

        // Save all logs
        logTableRepository.saveAll(logs);

        return savedProducts;
    }

    public Product update_product(Product newProduct, int id) {

        Product oldProduct = productRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Product Not Found")
                );

        oldProduct.setTitle(newProduct.getTitle());
        oldProduct.setMrp(newProduct.getMrp());
        oldProduct.setCurrentPrice(newProduct.getCurrentPrice());
        oldProduct.setDiscount(newProduct.getDiscount());
        oldProduct.setInventoryStock(newProduct.getInventoryStock());

        Product updatedProduct = productRepository.save(oldProduct);

        return updatedProduct;
    }

    public ProductPageResponse getProducts(Pageable pageable) {
        Page<Product> productPage = productRepository.findAll(pageable);

        return new ProductPageResponse(
                productPage.getContent(),
                productPage.getSize(),
                productPage.getNumberOfElements(),
                productPage.getTotalElements(),
                productPage.getTotalPages()
        );
    }
}
