package management.system.sale.Service;

import management.system.sale.Model.Campaign;
import management.system.sale.Model.CampaignProduct;
import management.system.sale.Model.LogTable;
import management.system.sale.Model.Product;
import management.system.sale.Repository.CampaignProductRepository;
import management.system.sale.Repository.CampaignRepository;
import management.system.sale.Repository.LogTableRepository;
import management.system.sale.Repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class CampaignProductService {
    private static final Logger log = LoggerFactory.getLogger(CampaignProductService.class);
    @Autowired
    CampaignProductRepository campaignProductRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    CampaignRepository campaignRepository;

    @Autowired
    LogTableRepository logTableRepository;

    public void save_campaign_product(CampaignProduct campaignProduct) {

        Product product = productRepository.findById(campaignProduct.getProductId()).orElseThrow(() -> new RuntimeException("Product Not Found"));

        Campaign campaign = campaignRepository
                .findById(campaignProduct.getCampaignId())
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        double oldPrice = product.getCurrentPrice();

        double discount = campaignProduct.getDiscount();

        double campaignPrice = oldPrice - (oldPrice * discount / 100);

        campaignProduct.setOldPrice(oldPrice);
        campaignProduct.setCampaignPrice(campaignPrice);

        LocalDate today = LocalDate.now();
        if (today.isBefore(campaign.getStartDate())) {
            campaignProduct.setStatus("UPCOMING");
        } else if (!today.isAfter(campaign.getEndDate())) {
            campaignProduct.setStatus("ACTIVE");
        } else {
            campaignProduct.setStatus("END");
        }

        campaignProductRepository.save(campaignProduct);

        product.setCurrentPrice(campaignPrice);

        product.setDiscount(campaignProduct.getDiscount());

        productRepository.save(product);

        LogTable logtable = new LogTable();

        logtable.setProduct_id(product.getId());
        logtable.setCampaign_id(campaign.getId());
        logtable.setOldPrice(oldPrice);
        logtable.setNewPrice(campaignPrice);
        logtable.setChangeType("CAMPAIGN_CREATED");

        logTableRepository.save(logtable);
    }
}
