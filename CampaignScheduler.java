package management.system.sale.Scheduler;

import jakarta.transaction.Transactional;
import management.system.sale.Model.Campaign;
import management.system.sale.Model.CampaignProduct;
import management.system.sale.Model.LogTable;
import management.system.sale.Model.Product;
import management.system.sale.Repository.CampaignProductRepository;
import management.system.sale.Repository.CampaignRepository;
import management.system.sale.Repository.LogTableRepository;
import management.system.sale.Repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class CampaignScheduler {

    @Autowired
    CampaignRepository campaignRepository;

    @Autowired
    CampaignProductRepository campaignProductRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    LogTableRepository logTableRepository;


    @Scheduled(cron = "0 56 10 * * *")
    @Transactional
    public void checkCampaign() {

        LocalDate today = LocalDate.now();

        List<Campaign> campaigns = campaignRepository.findAll();

        Thread startThread = new Thread(() -> {

            try {

                for (Campaign campaign : campaigns) {

                    if (campaign.getStartDate().equals(today)) {

                        List<CampaignProduct> campaignProducts =
                                campaignProductRepository
                                        .findByCampaignId(campaign.getId());

                        for (CampaignProduct cp : campaignProducts) {

                            // Prevent applying same campaign again
                            if ("ACTIVE".equals(cp.getStatus())
                                    || "ENDED".equals(cp.getStatus())) {
                                continue;
                            }

                            Product product = productRepository
                                    .findById(cp.getProductId())
                                    .orElse(null);

                            if (product != null) {

                                double oldPrice = product.getCurrentPrice();

                                double campaignPrice =
                                        oldPrice - (oldPrice * cp.getDiscount() / 100);

                                cp.setOldPrice(oldPrice);
                                cp.setCampaignPrice(campaignPrice);
                                cp.setStatus("ACTIVE");

                                campaignProductRepository.save(cp);

                                product.setCurrentPrice(campaignPrice);

                                product.setDiscount(cp.getDiscount());

                                productRepository.save(product);

                                LogTable logTable = new LogTable();

                                logTable.setProduct_id(product.getId());
                                logTable.setCampaign_id(campaign.getId());
                                logTable.setOldPrice(oldPrice);
                                logTable.setNewPrice(campaignPrice);
                                logTable.setChangeType("CAMPAIGN_STARTED");

                                logTableRepository.save(logTable);
                            }
                        }
                    }
                }
            }catch (Exception e) {
                System.out.println("Start Thread Got Error " + e.getMessage());
            }
        });

        Thread endThread = new Thread(() -> {

            try {
                for (Campaign campaign : campaigns) {

                    if (campaign.getEndDate().equals(today)) {

                        List<CampaignProduct> campaignProducts =
                                campaignProductRepository
                                        .findByCampaignId(campaign.getId());

                        for (CampaignProduct cp : campaignProducts) {

                            // Only ACTIVE campaign should be ended
                            if (!"ACTIVE".equals(cp.getStatus())) {
                                continue;
                            }

                            Product product = productRepository
                                    .findById(cp.getProductId())
                                    .orElse(null);

                            if (product != null) {

                                double priceBeforeEnding =
                                        product.getCurrentPrice();

                                cp.setStatus("ENDED");

                                campaignProductRepository.save(cp);

                                List<CampaignProduct> allProductCampaigns =
                                        campaignProductRepository.findAll();

                                double originalPrice = cp.getOldPrice();

                                CampaignProduct firstCampaignProduct = null;
                                LocalDate firstStartDate = null;

                                for (CampaignProduct productCp : allProductCampaigns) {

                                    if (productCp.getProductId() != product.getId()) {
                                        continue;
                                    }

                                    Campaign productCampaign =
                                            campaignRepository
                                                    .findById(productCp.getCampaignId())
                                                    .orElse(null);

                                    if (productCampaign == null) {
                                        continue;
                                    }

                                    if (firstStartDate == null
                                            || productCampaign.getStartDate()
                                            .isBefore(firstStartDate)) {

                                        firstStartDate =
                                                productCampaign.getStartDate();

                                        firstCampaignProduct = productCp;
                                    }
                                }


                                if (firstCampaignProduct != null) {
                                    originalPrice =
                                            firstCampaignProduct.getOldPrice();
                                }

                                double newPrice = originalPrice;

                                int activeCampaignCount = 0;

                                double totalDiscount = 0;


                                for (CampaignProduct productCp : allProductCampaigns) {

                                    if (productCp.getProductId() != product.getId()) {
                                        continue;
                                    }

                                    if (!"ACTIVE".equals(productCp.getStatus())) {
                                        continue;
                                    }

                                    newPrice =
                                            newPrice
                                                    - (newPrice
                                                    * productCp.getDiscount()
                                                    / 100);

                                    activeCampaignCount++;
                                }

                                product.setCurrentPrice(newPrice);


                                if (activeCampaignCount == 0) {

                                    // No campaign remaining
                                    product.setCurrentPrice(originalPrice);

                                    product.setDiscount(0);

                                } else {

                                    totalDiscount =
                                            ((originalPrice - newPrice)
                                                    / originalPrice) * 100;

                                    product.setDiscount(totalDiscount);
                                }

                                productRepository.save(product);

                                LogTable logTable = new LogTable();

                                logTable.setProduct_id(product.getId());
                                logTable.setCampaign_id(campaign.getId());

                                logTable.setOldPrice(priceBeforeEnding);
                                logTable.setNewPrice(newPrice);

                                logTable.setChangeType("CAMPAIGN_ENDED");

                                logTableRepository.save(logTable);
                            }
                        }
                    }
                }
            }catch (Exception e) {
                System.out.println("End Thread Got Error" + e.getMessage());
            }
        });

        startThread.start();
        endThread.start();

        try {

            startThread.join();

            endThread.join();
        }catch (InterruptedException ex) {
            System.out.println(
                    "Main scheduler thread interrupted: " + ex.getMessage());
        }

        System.out.println(
                "START and END campaign processing completed."
        );
    }
}